package presentacio.renderers;

import java.awt.Component;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.UITheme;
import interficie.BibliotecaWriter;
import presentacio.BibliotecaTableModel;
import presentacio.CoverImageCache;

public class CoverCellRenderer extends JLabel implements TableCellRenderer {
    private static final ExecutorService LOADER = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
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
            if (!(t.getModel() instanceof BibliotecaTableModel model)) return this;
            int modelRow = t.convertRowIndexToModel(row);
            Llibre l = model.getBookAt(modelRow);
            if (l == null) return this;
            long isbn = l.getISBN();
            if (coverCache.containsKey(isbn)) {
                ImageIcon cached = coverCache.get(isbn);
                if (cached != CoverImageCache.NO_COVER) setIcon(cached);
                return this;
            }
            if (!coverLoading.contains(isbn)) {
                coverLoading.add(isbn);
                final Llibre book = l;
                LOADER.submit(() -> {
                    try {
                        ImageIcon img = TableCellComponents.scaledCover(
                            TableCellComponents.loadCoverBytes(book, cd));
                        coverCache.put(isbn, img != null ? img : CoverImageCache.NO_COVER);
                    } finally {
                        coverLoading.remove(isbn);
                        SwingUtilities.invokeLater(() -> repaintCoverCell(isbn));
                    }
                });
            }
        } catch (Exception ignored) {}
        return this;
    }

    private void repaintCoverCell(long isbn) {
        if (!(table.getModel() instanceof BibliotecaTableModel model)) {
            table.repaint();
            return;
        }
        int viewCol = table.convertColumnIndexToView(BibliotecaTableModel.COL_COVER);
        if (viewCol < 0) {
            table.repaint();
            return;
        }
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            Llibre rowBook = model.getBookAt(table.convertRowIndexToModel(viewRow));
            if (rowBook != null && rowBook.getISBN() == isbn) {
                table.repaint(table.getCellRect(viewRow, viewCol, false));
                return;
            }
        }
    }
}
