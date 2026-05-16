package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;
import interficie.BibliotecaWriter;
import presentacio.MainFrameControl;

public class TableCellComponents {

    private static final int COLUMNA_ISBN = 1;

    // ── Cover thumbnail renderer ──────────────────────────────────────────────

    public static class CoverCellRenderer extends JLabel implements TableCellRenderer {
        private final Map<Long, ImageIcon> coverCache;
        private final Set<Long> coverLoading;
        private final BibliotecaWriter cd;
        private final JTable table;

        public CoverCellRenderer(JTable table, Map<Long, ImageIcon> coverCache,
                Set<Long> coverLoading, BibliotecaWriter cd) {
            this.table = table;
            this.coverCache = coverCache;
            this.coverLoading = coverLoading;
            this.cd = cd;
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focus, int row, int col) {
            setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
            setIcon(null);
            try {
                long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
                ImageIcon icon = coverCache.get(isbn);
                if (icon != null) {
                    setIcon(icon);
                } else if (!coverLoading.contains(isbn)) {
                    coverLoading.add(isbn);
                    final int r = row, c = col;
                    new SwingWorker<ImageIcon, Void>() {
                        @Override protected ImageIcon doInBackground() {
                            Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
                            return l != null ? scaledCover(loadCoverBytes(l, cd)) : null;
                        }
                        @Override protected void done() {
                            try {
                                ImageIcon img = get();
                                if (img != null) coverCache.put(isbn, img);
                            } catch (Exception ignored) {}
                            coverLoading.remove(isbn);
                            if (r < table.getRowCount())
                                table.repaint(table.getCellRect(r, c, false));
                        }
                    }.execute();
                }
            } catch (Exception ignored) {}
            return this;
        }
    }

    public static byte[] loadCoverBytes(Llibre l, BibliotecaWriter cd) {
        byte[] blob = l.getImatgeBlob();
        if (blob == null && l.hasBlob())
            blob = cd.getLlibreBlob(l.getISBN());
        if (blob != null) return blob;
        String path = l.getImatge();
        if (path != null && !path.isEmpty()) {
            try { return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)); } catch (Exception ignored) {}
        }
        return null;
    }

    public static ImageIcon scaledCover(byte[] data) {
        if (data == null) return null;
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img == null) return null;
            int h = 46;
            int w = Math.max(1, (int)(img.getWidth() * (h / (double) img.getHeight())));
            return new ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
        } catch (Exception ignored) { return null; }
    }

    // ── Search highlight renderer ─────────────────────────────────────────────

    public static class SearchHighlightRenderer extends DefaultTableCellRenderer {
        private String searchText = "";
        // Supplier is called on every getTableCellRendererComponent() invocation (i.e. every paint),
        // not once at construction. The Set it returns must reflect current loaned state.
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
                        setBackground(UITheme.isDark() ? new Color(0x5C3A00) : new Color(0xFFF3CD));
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

    // ── Progress bar renderer ─────────────────────────────────────────────────

    public static class ProgressBarRenderer extends JProgressBar implements TableCellRenderer {
        public ProgressBarRenderer() {
            setMinimum(0);
            setStringPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focus, int row, int col) {
            try {
                long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
                Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
                if (l != null && l.getPagines() > 0) {
                    setMaximum(l.getPagines());
                    setValue(l.getPaginesLlegides());
                    setString(l.getPaginesLlegides() + " / " + l.getPagines());
                } else {
                    setMaximum(1); setValue(0); setString("—");
                }
            } catch (Exception ignored) {
                setMaximum(1); setValue(0); setString("—");
            }
            setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
            setForeground(selected ? Color.WHITE : UITheme.TEXT_DARK);
            return this;
        }
    }

    // ── Llegit column: checkbox renderer + in-place toggle editor ─────────────

    public static class LlegitCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public LlegitCheckBoxRenderer() {
            setHorizontalAlignment(JCheckBox.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean selected,
                boolean focus, int row, int col) {
            setSelected(I18n.t("filter_read").equals(val));
            setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
            setForeground(selected ? Color.WHITE : UITheme.TEXT_DARK);
            return this;
        }
    }

    public static class LlegitCheckBoxEditor extends AbstractCellEditor implements TableCellEditor {
        private final JCheckBox cb = new JCheckBox();
        private int editingRow = -1;
        private JTable editingTable = null;
        private final BibliotecaWriter cd;
        private final Consumer<Llibre> onUpdated;

        public LlegitCheckBoxEditor(BibliotecaWriter cd, Consumer<Llibre> onUpdated) {
            this.cd = cd;
            this.onUpdated = onUpdated;
            cb.setHorizontalAlignment(JCheckBox.CENTER);
            cb.setOpaque(true);
            cb.addActionListener(e -> {
                boolean newLlegit = cb.isSelected();
                int row = editingRow;
                JTable tbl = editingTable;
                String isbnStr = tbl != null && row >= 0 && row < tbl.getRowCount()
                    ? (String) tbl.getValueAt(row, COLUMNA_ISBN) : null;
                fireEditingStopped();
                if (isbnStr != null) {
                    String isbn = isbnStr;
                    Thread t = new Thread(() -> {
                        try {
                            Llibre l = MainFrameControl.getInstance().getLlibreIsbn(Long.parseLong(isbn));
                            if (l == null) return;
                            l.setLlegit(newLlegit);
                            cd.updateLlibre(l);
                            SwingUtilities.invokeLater(() -> onUpdated.accept(l));
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> new DialogoError(ex).showErrorMessage());
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int col) {
            editingRow = row;
            editingTable = table;
            cb.setSelected(I18n.t("filter_read").equals(value));
            cb.setBackground(UITheme.ACCENT);
            cb.setForeground(Color.WHITE);
            return cb;
        }

        @Override
        public Object getCellEditorValue() {
            return cb.isSelected() ? I18n.t("filter_read") : I18n.t("filter_unread");
        }
    }

    // ── Details button renderer / editor ──────────────────────────────────────

    public static class BotonDetallesEditor extends DefaultCellEditor {
        private final JButton botonDetalles;

        public BotonDetallesEditor(JCheckBox checkbox, JButton botonDetalles) {
            super(checkbox);
            this.botonDetalles = botonDetalles;
            UITheme.styleAccentButton(botonDetalles);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int col) {
            botonDetalles.setText(I18n.t("col_details"));
            return botonDetalles;
        }
    }

    public static class BotonDetallesRenderer extends JButton implements TableCellRenderer {
        public BotonDetallesRenderer() { UITheme.styleAccentButton(this); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(isSelected ? UITheme.ACCENT_ALT : UITheme.ACCENT);
            setForeground(Color.WHITE);
            setText(I18n.t("col_details"));
            return this;
        }
    }
}
