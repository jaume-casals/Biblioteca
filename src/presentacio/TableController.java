package presentacio;

import domini.Llibre;
import herramienta.Config;
import herramienta.I18n;
import interficie.BibliotecaWriter;
import presentacio.renderers.TableCellComponents;

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

    static final int COL_COVER = BibliotecaTableModel.COL_COVER;
    static final int COL_ISBN = BibliotecaTableModel.COL_ISBN;
    static final int COL_NOM = BibliotecaTableModel.COL_NOM;
    static final int COL_AUTOR = BibliotecaTableModel.COL_AUTOR;
    static final int COL_ANY = BibliotecaTableModel.COL_ANY;
    static final int COL_VALORACIO = BibliotecaTableModel.COL_VALORACIO;
    static final int COL_PREU = BibliotecaTableModel.COL_PREU;
    static final int COL_LLEGIT = BibliotecaTableModel.COL_LLEGIT;
    static final int COL_PROGRES = BibliotecaTableModel.COL_PROGRES;
    static final int COL_DETALLS = BibliotecaTableModel.COL_DETALLS;

    private static final boolean[] COL_TOGGLEABLE =
        {true, false, false, true, true, true, true, true, true, false};

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
    private TableCellComponents.SearchHighlightRenderer highlightRenderer;
    private final java.util.TreeMap<Integer, TableColumn> hiddenCols = new java.util.TreeMap<>();

    TableController(MostrarBibliotecaPanel vista) {
        this.vista = vista;
    }

    BibliotecaTableModel model() { return tableModel; }
    TableCellComponents.SearchHighlightRenderer highlightRenderer() { return highlightRenderer; }

    void setBooks(List<Llibre> books, BibliotecaWriter cd, JButton detallesBtn,
                  Map<Long, ImageIcon> coverCache, Set<Long> coverLoading,
                  Supplier<Set<Long>> loanedIsbns, Consumer<Llibre> onRowUpdated) {
        tableModel.setBooks(books);
        JTable t = vista.getjTableBilio();
        if (t.getModel() != tableModel) {
            t.setModel(tableModel);
            columnsInstalled = false;
            sortListenerAttached = false;
        }
        attachSortPersistenceListener(t);
        if (!columnsInstalled) installColumns(t, cd, detallesBtn, coverCache, coverLoading, loanedIsbns, onRowUpdated);
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
                if (table.convertColumnIndexToModel(col) != COL_AUTOR) return;
                int modelRow = table.convertRowIndexToModel(row);
                Object val = table.getModel().getValueAt(modelRow, COL_AUTOR);
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
            Config.setColWidths(widths);
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
            boolean shouldBeVisible = Config.getColVisible(col);
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
            Config.setColVisible(modelIndex, true);
        } else {
            TableColumn tc = columnByModelIndex(t, modelIndex);
            if (tc == null) return;
            hiddenCols.put(modelIndex, tc);
            t.removeColumn(tc);
            Config.setColVisible(modelIndex, false);
        }
    }

    private void installColumns(JTable t, BibliotecaWriter cd, JButton detallesBtn,
                                Map<Long, ImageIcon> coverCache, Set<Long> coverLoading,
                                Supplier<Set<Long>> loanedIsbns, Consumer<Llibre> onRowUpdated) {
        t.setRowHeight(50);
        setWidth(t, COL_COVER, 48, 48, 56);
        setWidth(t, COL_ISBN, 130, 80, Integer.MAX_VALUE);
        setWidth(t, COL_NOM, 220, 80, Integer.MAX_VALUE);
        setWidth(t, COL_AUTOR, 180, 80, Integer.MAX_VALUE);
        setWidth(t, COL_ANY, 55, 40, Integer.MAX_VALUE);
        setWidth(t, COL_VALORACIO, 75, 50, Integer.MAX_VALUE);
        setWidth(t, COL_PREU, 60, 40, Integer.MAX_VALUE);
        setWidth(t, COL_LLEGIT, 80, 55, Integer.MAX_VALUE);
        setWidth(t, COL_PROGRES, 90, 50, Integer.MAX_VALUE);
        t.getColumnModel().getColumn(COL_COVER).setCellRenderer(
            new TableCellComponents.CoverCellRenderer(t, coverCache, coverLoading, cd));
        // Details column hidden — double-click row or Enter opens details (see installInteractionListeners)
        TableColumn detallsCol = t.getColumnModel().getColumn(COL_DETALLS);
        hiddenCols.put(COL_DETALLS, detallsCol);
        t.removeColumn(detallsCol);
        Config.setColVisible(COL_DETALLS, false);
        t.getColumnModel().getColumn(COL_LLEGIT).setCellRenderer(new TableCellComponents.LlegitCheckBoxRenderer());
        t.getColumnModel().getColumn(COL_LLEGIT).setCellEditor(
            new TableCellComponents.LlegitCheckBoxEditor(cd, onRowUpdated));
        t.getColumnModel().getColumn(COL_PROGRES).setCellRenderer(new TableCellComponents.ProgressBarRenderer());
        highlightRenderer = new TableCellComponents.SearchHighlightRenderer(loanedIsbns);
        for (int v = 0; v < t.getColumnCount(); v++) {
            int modelIndex = t.getColumnModel().getColumn(v).getModelIndex();
            if (modelIndex != COL_COVER && modelIndex != COL_LLEGIT && modelIndex != COL_PROGRES)
                t.getColumnModel().getColumn(v).setCellRenderer(highlightRenderer);
        }
        int[] defaults = {48, 130, 220, 180, 55, 75, 60, 80, 90, 85};
        for (int i = 0; i < defaults.length; i++) {
            int saved = Config.getColWidth(i, -1);
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
            if (keys.isEmpty()) Config.setSortColumn(-1);
            else {
                Config.setSortColumn(keys.get(0).getColumn());
                Config.setSortOrder(keys.get(0).getSortOrder().name());
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
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Llibre b = tableModel.getBookAt(i);
            if (b != null && b.getISBN() == isbn) return i;
        }
        return -1;
    }

    void refreshRow(int row, Llibre l) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            tableModel.getBooks().set(row, l);
            tableModel.fireTableRowsUpdated(row, row);
        }
    }

    void removeRowByIsbn(long isbn) {
        int idx = indexOfIsbn(isbn);
        if (idx >= 0) {
            tableModel.getBooks().remove(idx);
            tableModel.fireTableRowsDeleted(idx, idx);
        }
    }

    void appendBook(Llibre l) {
        tableModel.getBooks().add(l);
        tableModel.fireTableRowsInserted(tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
    }
}
