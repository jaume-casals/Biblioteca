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
import interficie.BookReader;
import presentacio.BibliotecaTableModel;
import presentacio.CoverImageCache;

public class CoverCellRenderer extends JLabel implements TableCellRenderer {
    private static final ExecutorService LOADER = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    private static final java.util.concurrent.atomic.AtomicBoolean SHUTDOWN_HOOK_REGISTERED =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    static {
        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            main.ShutdownHooks.register(() -> {
                try { LOADER.shutdownNow(); } catch (Exception ignored) {}
            });
        }
    }
    private final Map<Long, ImageIcon> coverCache;
    private final Set<Long> coverLoading;
    private final BookReader cd;
    private final JTable table;
    /**
     * ISBN → model row index. Caller-provided so the renderer can repaint
     * the right cell after a background load completes without walking
     * the whole table (O(n) per repaint was a hotspot on 1k+ row libraries).
     * May be {@code null} for backward compatibility — the renderer falls
     * back to a full-table walk in that case.
     */
    private final java.util.function.LongFunction<Integer> isbnToRow;

    public CoverCellRenderer(JTable table, Map<Long, ImageIcon> coverCache,
            Set<Long> coverLoading, BookReader cd) {
        this(table, coverCache, coverLoading, cd, null);
    }

    public CoverCellRenderer(JTable table, Map<Long, ImageIcon> coverCache,
            Set<Long> coverLoading, BookReader cd,
            java.util.function.LongFunction<Integer> isbnToRow) {
        this.table = table;
        this.coverCache = coverCache;
        this.coverLoading = coverLoading;
        this.cd = cd;
        this.isbnToRow = isbnToRow;
        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        setBackground(selected ? UITheme.palette().accent() : UITheme.palette().bgPanel());
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
                        ImageIcon img = presentacio.galeria.CoverImageService.scaledCover(
                            presentacio.galeria.CoverImageService.loadCoverBytes(book, cd));
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
        // Fast path: caller gave us a direct ISBN → model-row map.
        if (isbnToRow != null) {
            Integer modelRow = isbnToRow.apply(isbn);
            if (modelRow != null) {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.repaint(table.getCellRect(viewRow, viewCol, false));
                    return;
                }
            }
        }
        // Fallback: walk the table (caller did not supply isbnToRow, or
        // the row was filtered out between the background submit and the
        // repaint — harmless to repaint the whole table in that case).
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            Llibre rowBook = model.getBookAt(table.convertRowIndexToModel(viewRow));
            if (rowBook != null && rowBook.getISBN() == isbn) {
                table.repaint(table.getCellRect(viewRow, viewCol, false));
                return;
            }
        }
    }
}
