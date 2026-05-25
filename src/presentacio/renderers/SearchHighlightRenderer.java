package presentacio.renderers;

import java.awt.Component;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.*;

import herramienta.UITheme;

public class SearchHighlightRenderer extends DefaultTableCellRenderer {
    private static final int COLUMNA_ISBN = 1;
    private String searchText = "";
    private final Supplier<Set<Long>> loanedISBNs;

    public SearchHighlightRenderer(Supplier<Set<Long>> loanedISBNs) {
        this.loanedISBNs = loanedISBNs;
    }

    public void setSearchText(String text) { this.searchText = text != null ? text : ""; }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(t, value, selected, focus, row, col);
        if (!selected) {
            try {
                long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
                if (loanedISBNs.get().contains(isbn)) {
                    setBackground(UITheme.isDark() ? new java.awt.Color(0x5C3A00) : new java.awt.Color(0xFFF3CD));
                }
            } catch (Exception ignored) {}
        }
        String query = searchText.trim();
        String text = value != null ? value.toString() : "";
        if (!query.isEmpty() && !selected) {
            String escaped = text
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            String escapedQ = java.util.regex.Pattern.quote(query);
            String highlighted = escaped.replaceAll(
                "(?i)(" + escapedQ + ")",
                "<span style='background:#F39C12;color:#000'>$1</span>");
            if (!highlighted.equals(escaped))
                setText("<html>" + highlighted + "</html>");
        }
        return this;
    }
}