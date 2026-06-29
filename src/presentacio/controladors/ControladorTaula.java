package presentacio.controladors;

import domini.Llibre;
import herramienta.config.ConfiguracioFinestra;
import herramienta.config.ConfiguracioUi;
import herramienta.i18n.I18n;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.TableColumn;
import persistencia.contract.EscritorBiblioteca;
import persistencia.contract.EscritorLlibre;
import presentacio.models.ModelTaulaBiblioteca;
import presentacio.panells.PanelMostrarBiblioteca;
import presentacio.renderers.EditorCasellaLlegit;
import presentacio.renderers.RenderitzadorBarraProgres;
import presentacio.renderers.RenderitzadorCasellaLlegit;
import presentacio.renderers.RenderitzadorCellaCoberta;
import presentacio.renderers.RenderitzadorDestacatCerca;
import presentacio.util.AmfitrioPantallaBiblioteca;


/** Vista de taula: model, renderitzadors, visibilitat de columnes i listeners d'experiència d'usuari. */
public class ControladorTaula {

    private static final boolean[] COL_TOGGLEABLE =
        {true, false, false, true, true, true, true, true, true, false};

    private static final int ROW_HEIGHT = 50;
    private static final int[][] COL_WIDTHS = {
        {48, 48, 56},
        {130, 80, Integer.MAX_VALUE},
        {220, 80, Integer.MAX_VALUE},
        {180, 80, Integer.MAX_VALUE},
        {55, 40, Integer.MAX_VALUE},
        {75, 50, Integer.MAX_VALUE},
        {60, 40, Integer.MAX_VALUE},
        {80, 55, Integer.MAX_VALUE},
        {90, 50, Integer.MAX_VALUE},
        {85, 85, Integer.MAX_VALUE},
    };

    private static String[] colNames() { return ColNamesHolder.NAMES; }
    private static final class ColNamesHolder {
        static final String[] NAMES = {
            I18n.t("col_cover"), I18n.t("col_isbn"), I18n.t("col_title"),
            I18n.t("col_author"), I18n.t("col_year"), I18n.t("col_rating"),
            I18n.t("col_price"), I18n.t("col_read"), I18n.t("col_progress"),
            I18n.t("col_details")
        };
    }

    private final PanelMostrarBiblioteca vista;
    private final ModelTaulaBiblioteca tableModel = new ModelTaulaBiblioteca();
    private boolean columnsInstalled;
    private boolean ordenarListenerAttached;
    private RenderitzadorDestacatCerca highlightRenderer;
    private final java.util.TreeMap<Integer, TableColumn> hiddenCols = new java.util.TreeMap<>();
    /** Cerca O(1) des de l'ISBN a l'índex de fila del model. Es reconstrueix a {@link #setBooks}; s'actualitza incrementalment en mutacions de fila. */
    private final java.util.HashMap<Long, Integer> isbnToRow = new java.util.HashMap<>();

    ControladorTaula(PanelMostrarBiblioteca vista) {
        this.vista = vista;
    }

    ModelTaulaBiblioteca model() { return tableModel; }
    RenderitzadorDestacatCerca highlightRenderer() { return highlightRenderer; }

    void posarBooks(List<Llibre> books, EscritorBiblioteca cd, JButton detallesBtn,
                  Map<Long, ImageIcon> coverCache, Set<Long> coverLoading,
                  Set<Long> loanedIsbns, Consumer<Llibre> onRowUpdated) {
        tableModel.posarBooks(books);
        rebuildIsbnIndex();
        JTable t = vista.obtenirTaulaLlibres();
        if (t.getModel() != tableModel) {
            t.setModel(tableModel);
            columnsInstalled = false;
            ordenarListenerAttached = false;
        }
        adjuntarSortPersistenceListener(t);
        if (!columnsInstalled) installColumns(t, cd, detallesBtn, coverCache, coverLoading, loanedIsbns, onRowUpdated);
    }

    /** Actualitza el conjunt d'ISBN prestats en caché al renderer. La
     *  criden {@code MostrarBibliotecaControl} i {@code ContextMenuController}
     *  cada vegada que es reassigna el {@code state.loanedISBNs} del host.
     *  Evita el dispatch per supplier de cel·la que el finding MEDIUM
     *  de tot.txt va assenyalar. */
    public void posarLoanedISBNs(Set<Long> isbns) {
        if (highlightRenderer != null) highlightRenderer.posarLoanedISBNs(isbns);
    }

    private void rebuildIsbnIndex() {
        isbnToRow.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Llibre b = tableModel.obtenirBookAt(i);
            if (b != null) isbnToRow.put(b.obtenirISBN(), i);
        }
    }

    void installInteractionListeners(AmfitrioPantallaBiblioteca host, Runnable onOpenDetails,
                                     Runnable onFilterByAuthor, MouseAdapter contextMenu) {
        JTable table = vista.obtenirTaulaLlibres();
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) table.setRowSelectionInterval(row, row);
                if (e.getClickCount() == 2 && row >= 0) onOpenDetails.run();
            }
        });
        table.addMouseListener(contextMenu);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!e.isControlDown() || e.getClickCount() != 1) return;
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || col < 0) return;
                if (table.convertColumnIndexToModel(col) != ModelTaulaBiblioteca.COL_AUTOR) return;
                int modelRow = table.convertRowIndexToModel(row);
                Object val = table.getModel().getValueAt(modelRow, ModelTaulaBiblioteca.COL_AUTOR);
                if (val == null) return;
                String autor = val.toString().trim();
                if (autor.isEmpty()) return;
                vista.obtenirTextAutor().setText(autor);
                onFilterByAuthor.run();
            }
        });

        table.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "obrirDetalls");
        table.getActionMap().put("obrirDetalls", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (table.getSelectedRow() >= 0) onOpenDetails.run();
            }
        });

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShowColMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowColMenu(e); }
            private void maybeShowColMenu(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                JPopupMenu menu = new JPopupMenu();
                for (int col = 0; col < colNames().length; col++) {
                    if (!COL_TOGGLEABLE[col]) continue;
                    final int c = col;
                    boolean visible = !hiddenCols.containsKey(c);
                    JCheckBoxMenuItem item = new JCheckBoxMenuItem(colNames()[c], visible);
                    item.addActionListener(ev -> toggleColumn(c));
                    menu.add(item);
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        Timer desarWidthsTimer = new Timer(800, e -> {
            if (table.getColumnCount() == 0) return;
            int total = colNames().length;
            int[] widths = new int[total];
            for (int i = 0; i < table.getColumnCount(); i++) {
                TableColumn tc = table.getColumnModel().getColumn(i);
                widths[tc.getModelIndex()] = tc.getWidth();
            }
            for (var entry : hiddenCols.entrySet())
                widths[entry.getKey()] = entry.getValue().getWidth();
            ConfiguracioFinestra.posarColWidths(widths);
        });
        desarWidthsTimer.setRepeats(false);
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) { desarWidthsTimer.restart(); }
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });
    }

    void aplicarColumnVisibility() {
        for (int col = 0; col < colNames().length; col++) {
            if (!COL_TOGGLEABLE[col]) continue;
            boolean hauriaBeVisible = ConfiguracioFinestra.colVisible(col);
            boolean isVisible = !hiddenCols.containsKey(col);
            if (isVisible != hauriaBeVisible) toggleColumn(col);
        }
    }

    private void toggleColumn(int modelIndex) {
        JTable t = vista.obtenirTaulaLlibres();
        if (hiddenCols.containsKey(modelIndex)) {
            TableColumn tc = hiddenCols.remove(modelIndex);
            t.addColumn(tc);
            int targetView = 0;
            for (int i = 0; i < modelIndex; i++)
                if (!hiddenCols.containsKey(i)) targetView++;
            int currentView = t.getColumnCount() - 1;
            if (currentView != targetView) t.moveColumn(currentView, targetView);
            ConfiguracioFinestra.posarColVisible(modelIndex, true);
        } else {
            TableColumn tc = columnByModelIndex(t, modelIndex);
            if (tc == null) return;
            hiddenCols.put(modelIndex, tc);
            t.removeColumn(tc);
            ConfiguracioFinestra.posarColVisible(modelIndex, false);
        }
    }

    private void installColumns(JTable t, EscritorBiblioteca cd, JButton detallesBtn,
                                Map<Long, ImageIcon> coverCache, Set<Long> coverLoading,
                                Set<Long> loanedIsbns, Consumer<Llibre> onRowUpdated) {
        t.setRowHeight(ROW_HEIGHT);
        for (int i = 0; i < 9; i++) {
            int[] w = COL_WIDTHS[i];
            setWidth(t, i, w[0], w[1], w[2]);
        }
        column(t, ModelTaulaBiblioteca.COL_COVER).setCellRenderer(
            new RenderitzadorCellaCoberta(t, coverCache, coverLoading, cd, isbnToRow::get));
        // Columna de detalls oculta — doble clic a la fila o Enter obre
        // els detalls (veure installInteractionListeners)
        TableColumn detallsCol = column(t, ModelTaulaBiblioteca.COL_DETALLS);
        hiddenCols.put(ModelTaulaBiblioteca.COL_DETALLS, detallsCol);
        t.removeColumn(detallsCol);
        ConfiguracioFinestra.posarColVisible(ModelTaulaBiblioteca.COL_DETALLS, false);
        column(t, ModelTaulaBiblioteca.COL_LLEGIT).setCellRenderer(new RenderitzadorCasellaLlegit());
        column(t, ModelTaulaBiblioteca.COL_LLEGIT).setCellEditor(
            new EditorCasellaLlegit((EscritorLlibre) cd, onRowUpdated));
        column(t, ModelTaulaBiblioteca.COL_PROGRES).setCellRenderer(new RenderitzadorBarraProgres());
        highlightRenderer = new RenderitzadorDestacatCerca(loanedIsbns);
        for (int v = 0; v < t.getColumnCount(); v++) {
            int modelIndex = t.getColumnModel().getColumn(v).getModelIndex();
            if (modelIndex != ModelTaulaBiblioteca.COL_COVER && modelIndex != ModelTaulaBiblioteca.COL_LLEGIT && modelIndex != ModelTaulaBiblioteca.COL_PROGRES)
                t.getColumnModel().getColumn(v).setCellRenderer(highlightRenderer);
        }
        for (int i = 0; i < COL_WIDTHS.length; i++) {
            int saved = ConfiguracioFinestra.colWidth(i, -1);
            if (saved > 0) {
                TableColumn tc = columnByModelIndex(t, i);
                if (tc != null) tc.setPreferredWidth(saved);
            }
        }
        columnsInstalled = true;
    }

    private void adjuntarSortPersistenceListener(JTable table) {
        if (ordenarListenerAttached) return;
        javax.swing.RowSorter<?> sorter = table.getRowSorter();
        if (sorter == null) return;
        sorter.addRowSorterListener(e -> {
            var keys = sorter.getSortKeys();
            if (keys.isEmpty()) ConfiguracioUi.posarSortColumn(-1);
            else {
                ConfiguracioUi.posarSortColumn(keys.get(0).getColumn());
                ConfiguracioUi.posarSortOrder(keys.get(0).getSortOrder().name());
            }
        });
        ordenarListenerAttached = true;
    }

    private static TableColumn columnByModelIndex(JTable t, int modelIndex) {
        for (int v = 0; v < t.getColumnCount(); v++) {
            TableColumn c = t.getColumnModel().getColumn(v);
            if (c.getModelIndex() == modelIndex) return c;
        }
        return null;
    }

    private static TableColumn column(JTable t, int idx) {
        return t.getColumnModel().getColumn(idx);
    }

    private static void setWidth(JTable t, int col, int pref, int min, int max) {
        TableColumn c = column(t, col);
        c.setPreferredWidth(pref);
        c.setMinWidth(min);
        if (max < Integer.MAX_VALUE) c.setMaxWidth(max);
    }

    int indexOfIsbn(long isbn) {
        Integer row = isbnToRow.get(isbn);
        return row == null ? -1 : row;
    }

    void refreshRow(int row, Llibre l) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            Long oldIsbn = tableModel.obtenirBookAt(row) != null ? tableModel.obtenirBookAt(row).obtenirISBN() : null;
            tableModel.obtenirBooks().set(row, l);
            if (oldIsbn != null) isbnToRow.remove(oldIsbn);
            isbnToRow.put(l.obtenirISBN(), row);
            tableModel.fireTableRowsUpdated(row, row);
        }
    }

    void eliminarRowByIsbn(long isbn) {
        int idx = indexOfIsbn(isbn);
        if (idx >= 0) {
            tableModel.obtenirBooks().remove(idx);
            // Reconstrueix l'índex de files en una sola passada — el
            // desplaçament O(n) per eliminació era el finding MEDIUM
            // de tot.txt (1M d'actualitzacions d'entrades per a una
            // biblioteca de 10k llibres eliminant-ne 100). rebuildIsbnIndex
            // és O(n) un sol cop independentment del nombre d'eliminacions.
            rebuildIsbnIndex();
            tableModel.fireTableRowsDeleted(idx, idx);
        }
    }

    /** Eliminació per lots. O(llibres + eliminacions) en lloc d'O(llibres * eliminacions)
     *  del patró de desplaçament d'entrades. */
    void eliminarRowsByIsbn(java.util.Collection<Long> isbns) {
        if (isbns == null || isbns.isEmpty()) return;
        // Ordena per índex de fila descendent perquè cada eliminació
        // no invalidi els índexs que encara no hem processat.
        java.util.List<Integer> rows = new java.util.ArrayList<>(isbns.size());
        for (Long isbn : isbns) {
            Integer r = isbnToRow.get(isbn);
            if (r != null) rows.add(r);
        }
        rows.sort(java.util.Collections.reverseOrder());
        for (int r : rows) {
            tableModel.obtenirBooks().remove(r);
            tableModel.fireTableRowsDeleted(r, r);
        }
        rebuildIsbnIndex();
    }

    void afegirBook(Llibre l) {
        int row = tableModel.getRowCount();
        tableModel.obtenirBooks().add(l);
        isbnToRow.put(l.obtenirISBN(), row);
        tableModel.fireTableRowsInserted(row, row);
    }
}



