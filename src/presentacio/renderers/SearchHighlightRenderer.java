package presentacio.renderers;

import java.awt.Component;
import java.util.Collections;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.UITheme;
import presentacio.BibliotecaTableModel;

public class SearchHighlightRenderer extends DefaultTableCellRenderer {
    private String cercarText = "";
    /** Cached set reference — refreshed via {@link #setLoanedISBNs(Set)} from
     *  {@link presentacio.TableController} whenever the host's
     *  {@code state.loanedISBNs} is reassigned. Avoids the per-cell
     *  {@code Supplier.get()} dispatch the previous API required (the
     *  tot.txt MEDIUM finding flagged 10 000 lambda calls per repaint). */
    private Set<Long> loanedISBNs = Collections.emptySet();

    public SearchHighlightRenderer() {
        this(Collections.emptySet());
    }

    public SearchHighlightRenderer(Set<Long> loanedISBNs) {
        this.loanedISBNs = loanedISBNs != null ? loanedISBNs : Collections.emptySet();
    }

    public void posarSearchText(String text) { this.cercarText = text != null ? text : ""; }
    public void posarLoanedISBNs(Set<Long> isbns) { this.loanedISBNs = isbns != null ? isbns : Collections.emptySet(); }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(t, value, selected, focus, row, col);
        if (!selected && t.getModel() instanceof BibliotecaTableModel model) {
            try {
                int modelRow = t.convertRowIndexToModel(row);
                Llibre l = model.obtenirBookAt(modelRow);
                if (l != null && loanedISBNs.contains(l.obtenirISBN())) {
                    setBackground(UITheme.esDark() ? new java.awt.Color(0x5C3A00) : new java.awt.Color(0xFFF3CD));
                }
            } catch (Exception ignored) {}
        }
        String query = cercarText.trim();
        String text = value != null ? value.toString() : "";
        if (!query.isEmpty() && !selected) {
            String escaped = text
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            String escapedQ = java.util.regex.Pattern.quote(query);
            String bg = hexColor(UITheme.palette().cercarHighlightBg());
            String fg = hexColor(UITheme.palette().cercarHighlightFg());
            String highlighted = escaped.replaceAll(
                "(?i)(" + escapedQ + ")",
                "<span style='background:" + bg + ";color:" + fg + "'>$1</span>");
            if (!highlighted.equals(escaped))
                setText("<html>" + highlighted + "</html>");
        }
        return this;
    }

    private static String hexColor(java.awt.Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
