package presentacio.renderers;

import java.awt.Component;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.ui.UITheme;
import persistencia.contract.LectorLlibre;
import presentacio.ModelTaulaBiblioteca;
import presentacio.MemoriaImatgesCoberta;

public class RenderitzadorCellaCoberta extends JLabel implements TableCellRenderer {
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
    private final LectorLlibre cd;
    private final JTable table;
    /**
     * ISBN → índex de fila del model. Subministrat pel consumidor perquè el
     * renderer pugui repintar la cel·la correcta quan acaba una càrrega en
     * segon pla sense recórrer tota la taula (O(n) per repaint era un
     * punt calent en biblioteques de més de 1k files). Pot ser
     * {@code null} per compatibilitat enrere — el renderer cau a un
     * recorregut complet de la taula en aquest cas.
     */
    private final java.util.function.LongFunction<Integer> isbnToRow;

    public RenderitzadorCellaCoberta(JTable table, Map<Long, ImageIcon> coverCache,
            Set<Long> coverLoading, LectorLlibre cd) {
        this(table, coverCache, coverLoading, cd, null);
    }

    public RenderitzadorCellaCoberta(JTable table, Map<Long, ImageIcon> coverCache,
            Set<Long> coverLoading, LectorLlibre cd,
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
            if (!(t.getModel() instanceof ModelTaulaBiblioteca model)) return this;
            int modelRow = t.convertRowIndexToModel(row);
            Llibre l = model.obtenirBookAt(modelRow);
            if (l == null) return this;
            long isbn = l.obtenirISBN();
            ImageIcon cached = coverCache.get(isbn);
            if (cached != null) {
                if (cached != MemoriaImatgesCoberta.NO_COVER) setIcon(cached);
                return this;
            }
            if (!coverLoading.contains(isbn)) {
                coverLoading.add(isbn);
                final Llibre book = l;
                LOADER.submit(() -> {
                    try {
                        ImageIcon img = presentacio.galeria.ServeiImatgesCoberta.scaledCover(
                            presentacio.galeria.ServeiImatgesCoberta.carregarCoverBytes(book, cd));
                        coverCache.put(isbn, img != null ? img : MemoriaImatgesCoberta.NO_COVER);
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
        if (!(table.getModel() instanceof ModelTaulaBiblioteca model)) {
            table.repaint();
            return;
        }
        int viewCol = table.convertColumnIndexToView(ModelTaulaBiblioteca.COL_COVER);
        if (viewCol < 0) {
            table.repaint();
            return;
        }
        // Camí ràpid: el consumidor ens ha donat un mapa directe
        // ISBN → fila del model.
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
        // Caiguda: recorre la taula (el consumidor no ha subministrat
        // isbnToRow, o la fila s'ha filtrat entre el submit en segon
        // pla i el repaint — no passa res per repintar tota la taula
        // en aquest cas).
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            Llibre rowBook = model.obtenirBookAt(table.convertRowIndexToModel(viewRow));
            if (rowBook != null && rowBook.obtenirISBN() == isbn) {
                table.repaint(table.getCellRect(viewRow, viewCol, false));
                return;
            }
        }
    }
}
