package presentacio;

import domini.Llibre;
import herramienta.I18n;
import herramienta.UiConfig;
import herramienta.WindowConfig;
import interficie.BibliotecaWriter;
import interficie.BookWriter;
import presentacio.renderers.CoverCellRenderer;
import presentacio.renderers.LlegitCheckBoxEditor;
import presentacio.renderers.LlegitCheckBoxRenderer;
import presentacio.renderers.ProgressBarRenderer;
import presentacio.renderers.SearchHighlightRenderer;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Table view: model, renderers, column visibility, table UX listeners. */
class TableController {

    private static final boolean[] COL_TOGGLEABLE =
        {true, false, false, true, true, true, true, true, true, false};

    private static final int ROW_HEIGHT = 50;
    private static final int[] DEFAULT_COL_WIDTHS = {48, 130, 220, 180, 55, 75, 60, 80, 90, 85};

    private static String[] colNames() {
        return new String[]{
            I18n.t("col_cover"), I18n.t("col_isbn"), I18n.t("col_title"),
            I18n.t("col_author"), I18n.t("col_year"), I18n.t("col_rating"),
            I18n.t("col_price"), I18n.t("col_read"), I18n.t("col_progress"),
            I18n.t("col_details")
        };
    }

    private final MostrarBibliotecaPanel vista;
    private final BibliotecaTableModel tableModel = new BibliotecaTableModel();
    private boolean columnsInstalled;
    private boolean sortListenerAttached;
    private SearchHighlightRenderer highlightRenderer;
    private final java.util.TreeMap<Integer, TableColumn> hiddenCols = new java.util.TreeMap<>();
    /** O(1) lookup from ISBN to model row index. Rebuilt on {@link #setBooks}; updated incrementally on row mutations. */
    private final java.util.HashMap<Long, Integer> isbnToRow = new java.util.HashMap<>();

    TableController(MostrarBibliotecaPanel vista) {
        this.vista = vista;
    }

    BibliotecaTableModel model() { return tableModel; }
    SearchHighlightRenderer highlightRenderer() { return highlightRenderer; }

    void setBooks(List<Llibre> books, BibliotecaWriter cd, JButton detallesBtn,
                  Map<Long, ImageIcon> coverCache, Set<Long> coverLoading,
                  Set<Long> loanedIsbns, Consumer<Llibre> onRowUpdated) {
        tableModel.setBooks(books);
        rebuildIsbnIndex();
        JTable t = vista.getjTableBilio();
        if (t.getModel() != tableModel) {
            t.setModel(tableModel);
            columnsInstalled = false;
            sortListenerAttached = false;
        }
        attachSortPersistenceListener(t);
        if (!columnsInstalled) installColumns(t, cd, detallesBtn, coverCache, coverLoading, loanedIsbns, onRowUpdated);
    }

    /** Update the renderer's cached loaned-ISBN set. Called by
     *  {@code MostrarBibliotecaControl} / {@code ContextMenuController}
     *  whenever the host's {@code state.loanedISBNs} is reassigned.
     *  Avoids the per-cell supplier dispatch the tot.txt MEDIUM
     *  finding flagged. */
    public void setLoanedISBNs(Set<Long> isbns) {
        if (highlightRenderer != null) highlightRenderer.setLoanedISBNs(isbns);
    }

    private void rebuildIsbnIndex() {
        isbnToRow.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Llibre b = tableModel.getBookAt(i);
            if (b != null) isbnToRow.put(b.getISBN(), i);
        }
    }

    void installInteractionListeners(LibraryScreenHost host, Runnable onOpenDetails,
                                     Runnable onFilterByAuthor, MouseAdapter contextMenu) {
        JTable table = vista.getjTableBilio();
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
                if (table.convertColumnIndexToModel(col) != BibliotecaTableModel.COL_AUTOR) return;
                int modelRow = table.convertRowIndexToModel(row);
                Object val = table.getModel().getValueAt(modelRow, BibliotecaTableModel.COL_AUTOR);
                if (val == null) return;
                String autor = val.toString().trim();
                if (autor.isEmpty()) return;
                vista.getTextAutor().setText(autor);
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

        Timer saveWidthsTimer = new Timer(800, e -> {
            if (table.getColumnCount() == 0) return;
            int total = colNames().length;
            int[] widths = new int[total];
            for (int i = 0; i < table.getColumnCount(); i++) {
                TableColumn tc = table.getColumnModel().getColumn(i);
                widths[tc.getModelIndex()] = tc.getWidth();
            }
            for (var entry : hiddenCols.entrySet())
                widths[entry.getKey()] = entry.getValue().getWidth();
            WindowConfig.setColWidths(widths);
        });
        saveWidthsTimer.setRepeats(false);
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) { saveWidthsTimer.restart(); }
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });
    }

    void applyColumnVisibility() {
        for (int col = 0; col < colNames().length; col++) {
            if (!COL_TOGGLEABLE[col]) continue;
            boolean shouldBeVisible = WindowConfig.colVisible(col);
            boolean isVisible = !hiddenCols.containsKey(col);
            if (isVisible != shouldBeVisible) toggleColumn(col);
        }
    }

    private void toggleColumn(int modelIndex) {
        JTable t = vista.getjTableBilio();
        if (hiddenCols.containsKey(modelIndex)) {
            TableColumn tc = hiddenCols.remove(modelIndex);
            t.addColumn(tc);
            int targetView = 0;
            for (int i = 0; i < modelIndex; i++)
                if (!hiddenCols.containsKey(i)) targetView++;
            int currentView = t.getColumnCount() - 1;
            if (currentView != targetView) t.moveColumn(currentView, targetView);
            WindowConfig.setColVisible(modelIndex, true);
        } else {
            TableColumn tc = columnByModelIndex(t, modelIndex);
            if (tc == null) return;
            hiddenCols.put(modelIndex, tc);
            t.removeColumn(tc);
            WindowConfig.setColVisible(modelIndex, false);
        }
    }

    private void installColumns(JTable t, BibliotecaWriter cd, JButton detallesBtn,
                                Map<Long, ImageIcon> coverCache, Set<Long> coverLoading,
                                Set<Long> loanedIsbns, Consumer<Llibre> onRowUpdated) {
        t.setRowHeight(ROW_HEIGHT);
        setWidth(t, BibliotecaTableModel.COL_COVER, 48, 48, 56);
        setWidth(t, BibliotecaTableModel.COL_ISBN, 130, 80, Integer.MAX_VALUE);
        setWidth(t, BibliotecaTableModel.COL_NOM, 220, 80, Integer.MAX_VALUE);
        setWidth(t, BibliotecaTableModel.COL_AUTOR, 180, 80, Integer.MAX_VALUE);
        setWidth(t, BibliotecaTableModel.COL_ANY, 55, 40, Integer.MAX_VALUE);
        setWidth(t, BibliotecaTableModel.COL_VALORACIO, 75, 50, Integer.MAX_VALUE);
        setWidth(t, BibliotecaTableModel.COL_PREU, 60, 40, Integer.MAX_VALUE);
        setWidth(t, BibliotecaTableModel.COL_LLEGIT, 80, 55, Integer.MAX_VALUE);
        setWidth(t, BibliotecaTableModel.COL_PROGRES, 90, 50, Integer.MAX_VALUE);
        t.getColumnModel().getColumn(BibliotecaTableModel.COL_COVER).setCellRenderer(
            new CoverCellRenderer(t, coverCache, coverLoading, cd, isbnToRow::get));
        // Details column hidden — double-click row or Enter opens details (see installInteractionListeners)
        TableColumn detallsCol = t.getColumnModel().getColumn(BibliotecaTableModel.COL_DETALLS);
        hiddenCols.put(BibliotecaTableModel.COL_DETALLS, detallsCol);
        t.removeColumn(detallsCol);
        WindowConfig.setColVisible(BibliotecaTableModel.COL_DETALLS, false);
        t.getColumnModel().getColumn(BibliotecaTableModel.COL_LLEGIT).setCellRenderer(new LlegitCheckBoxRenderer());
        t.getColumnModel().getColumn(BibliotecaTableModel.COL_LLEGIT).setCellEditor(
            new LlegitCheckBoxEditor((BookWriter) cd, onRowUpdated));
        t.getColumnModel().getColumn(BibliotecaTableModel.COL_PROGRES).setCellRenderer(new ProgressBarRenderer());
        highlightRenderer = new SearchHighlightRenderer(loanedIsbns);
        for (int v = 0; v < t.getColumnCount(); v++) {
            int modelIndex = t.getColumnModel().getColumn(v).getModelIndex();
            if (modelIndex != BibliotecaTableModel.COL_COVER && modelIndex != BibliotecaTableModel.COL_LLEGIT && modelIndex != BibliotecaTableModel.COL_PROGRES)
                t.getColumnModel().getColumn(v).setCellRenderer(highlightRenderer);
        }
        for (int i = 0; i < DEFAULT_COL_WIDTHS.length; i++) {
            int saved = WindowConfig.colWidth(i, -1);
            if (saved > 0) {
                TableColumn tc = columnByModelIndex(t, i);
                if (tc != null) tc.setPreferredWidth(saved);
            }
        }
        columnsInstalled = true;
    }

    private void attachSortPersistenceListener(JTable table) {
        if (sortListenerAttached) return;
        javax.swing.RowSorter<?> sorter = table.getRowSorter();
        if (sorter == null) return;
        sorter.addRowSorterListener(e -> {
            var keys = sorter.getSortKeys();
            if (keys.isEmpty()) UiConfig.setSortColumn(-1);
            else {
                UiConfig.setSortColumn(keys.get(0).getColumn());
                UiConfig.setSortOrder(keys.get(0).getSortOrder().name());
            }
        });
        sortListenerAttached = true;
    }

    private static TableColumn columnByModelIndex(JTable t, int modelIndex) {
        for (int v = 0; v < t.getColumnCount(); v++) {
            TableColumn c = t.getColumnModel().getColumn(v);
            if (c.getModelIndex() == modelIndex) return c;
        }
        return null;
    }

    private static void setWidth(JTable t, int col, int pref, int min, int max) {
        TableColumn c = t.getColumnModel().getColumn(col);
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
            Long oldIsbn = tableModel.getBookAt(row) != null ? tableModel.getBookAt(row).getISBN() : null;
            tableModel.getBooks().set(row, l);
            if (oldIsbn != null) isbnToRow.remove(oldIsbn);
            isbnToRow.put(l.getISBN(), row);
            tableModel.fireTableRowsUpdated(row, row);
        }
    }

    void removeRowByIsbn(long isbn) {
        int idx = indexOfIsbn(isbn);
        if (idx >= 0) {
            tableModel.getBooks().remove(idx);
            // Rebuild the row index in one pass — the O(n) per-delete
            // shift was the tot.txt MEDIUM finding (1M entry updates
            // for a 10k library deleting 100 books). rebuildIsbnIndex
            // is O(n) once regardless of delete count.
            rebuildIsbnIndex();
            tableModel.fireTableRowsDeleted(idx, idx);
        }
    }

    /** Batch delete. O(books + deletes) instead of O(books * deletes)
     *  for the entry-shift pattern. */
    void removeRowsByIsbn(java.util.Collection<Long> isbns) {
        if (isbns == null || isbns.isEmpty()) return;
        // Sort by descending row index so each removal does not invalidate
        // the indexes we haven't processed yet.
        java.util.List<Integer> rows = new java.util.ArrayList<>(isbns.size());
        for (Long isbn : isbns) {
            Integer r = isbnToRow.get(isbn);
            if (r != null) rows.add(r);
        }
        rows.sort(java.util.Collections.reverseOrder());
        for (int r : rows) {
            tableModel.getBooks().remove(r);
            tableModel.fireTableRowsDeleted(r, r);
        }
        rebuildIsbnIndex();
    }

    void appendBook(Llibre l) {
        int row = tableModel.getRowCount();
        tableModel.getBooks().add(l);
        isbnToRow.put(l.getISBN(), row);
        tableModel.fireTableRowsInserted(row, row);
    }
}
