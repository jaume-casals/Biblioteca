package presentacio;


import presentacio.config.ConfiguracioDialogListener;
import domini.Llibre;
import domini.Llista;
import herramienta.Config;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;
import presentacio.detalles.control.DetallesLlibrePanelControl;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Book CRUD, details dialogs, stats, quick views, and undo. */
class BookActionsController {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final FilterController filterCtrl;
    private final ShelfController shelfCtrl;

    BookActionsController(LibraryViewState state, LibraryScreenHost host,
                          FilterController filterCtrl, ShelfController shelfCtrl) {
        this.state = state;
        this.host = host;
        this.filterCtrl = filterCtrl;
        this.shelfCtrl = shelfCtrl;
    }

    void wireListeners() {
        state.vista.getBtnEstadistiques().addActionListener(e -> mostrarEstadistiques());
        state.vista.getBtnLlibreAleatori().addActionListener(e -> mostrarLlibreAleatori());
        state.vista.getBtnConfiguracio().addActionListener(e -> obrirConfiguracio());
        state.vista.getBtnAfegitsRecentment().addActionListener(e -> mostrarAfegitsRecentment());
        state.vista.getBtnLlegitsRecentment().addActionListener(e -> mostrarLlegitsRecentment());
        state.vista.getBtnDesitjats().addActionListener(e -> mostrarDesitjats());
        state.vista.getBtnEnCurs().addActionListener(e -> mostrarEnCurs());
    }

    void abrirDetallesLlibres() { abrirDetalles(false); }

    void abrirDetallesEnEdicio() {
        if (state.vista.isGaleriaMode()) {
            List<Llibre> sel = state.vista.getGaleria().getSelectedLlibres();
            if (!sel.isEmpty()) abrirDetallesDeLlibre(sel.get(0), true);
        } else {
            abrirDetalles(true);
        }
    }

    private void abrirDetalles(boolean editMode) {
        try {
            javax.swing.JTable table = state.vista.getjTableBilio();
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return;
            int modelRow = table.convertRowIndexToModel(viewRow);
            javax.swing.table.TableModel model = table.getModel();
            if (!(model instanceof BibliotecaTableModel bt)) return;
            Llibre l = bt.getBookAt(modelRow);
            if (l == null) return;
            abrirDetallesDeLlibre(l, editMode);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    void abrirDetallesDeLlibre(Llibre l, boolean editMode) {
        if (l == null) return;
        try {
            DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, state.enActualizarBBDD, state.cd);
            detalles.getDetallesLlibrePanel().setLocationRelativeTo(state.vista);
            if (editMode) detalles.getDetallesLlibrePanel().getBtnEditar().doClick();
            detalles.getDetallesLlibrePanel().setVisible(true);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    void refreshLlibre(Llibre l, boolean nuevo) {
        if (!nuevo) host.refreshRow(l);
        else {
            host.appendRow(l);
            host.updateTitleBar();
        }
    }

    void eliminarFilaSeleccionada() {
        JTable t = state.vista.getjTableBilio();
        int[] rows = t.getSelectedRows();
        if (rows.length == 0) return;
        String msg = rows.length == 1
            ? I18n.t("dlg_confirm_delete_one", t.getValueAt(rows[0], BibliotecaTableModel.COL_NOM))
            : I18n.t("dlg_confirm_delete_n", rows.length);
        if (JOptionPane.showConfirmDialog(state.vista, msg, I18n.t("dlg_confirm_delete_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        List<Long> isbns = new ArrayList<>();
        for (int row : rows) {
            Object cell = t.getValueAt(row, BibliotecaTableModel.COL_ISBN);
            if (!(cell instanceof String) && !(cell instanceof Number)) continue;
            try { isbns.add(Long.parseLong(cell.toString())); }
            catch (NumberFormatException nfe) { /* skip non-numeric cell */ }
        }
        for (long isbn : isbns) {
            try {
                state.cd.findLlibre(isbn).ifPresent(l -> {
                    state.undoBuffer.push(l);
                    if (state.undoBuffer.size() > LibraryViewState.UNDO_MAX) state.undoBuffer.removeLast();
                    state.cd.deleteLlibre(l);
                    eliminarFila(l);
                });
            } catch (Exception e) {
                new DialogoError(e).showErrorMessage();
            }
        }
    }

    void eliminarFila(Llibre l) {
        host.removeRow(l);
        if (state.biblio != null) state.biblio.removeIf(b -> java.util.Objects.equals(b.getISBN(), l.getISBN()));
        host.updateTitleBar();
    }

    void undoDelete() {
        if (state.undoBuffer.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_undo_empty"), I18n.t("dlg_undo_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llibre l = state.undoBuffer.pop();
        try {
            state.cd.addLlibre(l);
            refreshLlibre(l, true);
            JOptionPane.showMessageDialog(state.vista,
                I18n.t("dlg_undo_done", l.getNom()), I18n.t("dlg_undo_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    void mostrarEstadistiques() {
        List<Llibre> global = state.cd.getAllLlibres();
        if (global.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_empty_library"), I18n.t("dlg_stats_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        EstadistiquesHelper.BookStats globalStats = EstadistiquesHelper.computeStats(global);
        javax.swing.JPanel tab1 = EstadistiquesHelper.buildGeneralTab(global, globalStats, state.cd);

        javax.swing.JPanel tab2 = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        tab2.setBackground(UITheme.palette().bgPanel());
        tab2.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tab2.add(EstadistiquesHelper.buildReadingChart(globalStats.booksByReadYear));
        tab2.add(EstadistiquesHelper.buildPublisherChart(global));

        javax.swing.JPanel tab3 = EstadistiquesHelper.buildTagCloud(global, state.cd);
        javax.swing.JPanel tab4 = EstadistiquesHelper.buildReadingPacePanel(global, globalStats.booksByReadYear);

        EstadistiquesHelper.showDialog(SwingUtilities.getWindowAncestor(state.vista),
            new javax.swing.JComponent[] { tab1, tab2, tab3, tab4 },
            new String[] { I18n.t("stats_tab_general"), I18n.t("stats_tab_charts"),
                I18n.t("stats_tab_tags"), I18n.t("stats_tab_pace") });
    }

    void mostrarLlibreAleatori() {
        if (state.biblio == null || state.biblio.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_books_view"), I18n.t("dlg_aleatori_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Llibre> noLlegits = state.biblio.stream()
            .filter(l -> !Boolean.TRUE.equals(l.getLlegit()))
            .collect(Collectors.toList());
        if (noLlegits.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_all_read"), I18n.t("dlg_aleatori_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llibre aleatori = noLlegits.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(noLlegits.size()));
        JTable t = state.vista.getjTableBilio();
        for (int row = 0; row < t.getRowCount(); row++) {
            if (String.valueOf(aleatori.getISBN()).equals(t.getValueAt(row, BibliotecaTableModel.COL_ISBN))) {
                t.setRowSelectionInterval(row, row);
                t.scrollRectToVisible(t.getCellRect(row, 0, true));
                break;
            }
        }
        DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(aleatori, state.enActualizarBBDD, state.cd);
        detalles.getDetallesLlibrePanel().setLocationRelativeTo(state.vista);
        detalles.getDetallesLlibrePanel().setVisible(true);
    }

    private void mostrarAfegitsRecentment() {
        ArrayList<Llibre> recents = new ArrayList<>(state.cd.getRecentlyAdded());
        if (recents.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_books_recent"),
                I18n.t("dlg_recently_added_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        state.biblio = recents;
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    private void mostrarLlegitsRecentment() {
        List<Llibre> llegits = state.cd.getAllLlibres().stream()
            .filter(l -> Boolean.TRUE.equals(l.getLlegit()))
            .collect(Collectors.toList());
        if (llegits.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_read"), I18n.t("dlg_read_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        state.biblio = llegits;
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    private void mostrarDesitjats() {
        state.biblio = state.cd.getAllLlibres().stream()
            .filter(l -> Boolean.TRUE.equals(l.isDesitjat()))
            .collect(Collectors.toList());
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    private void mostrarEnCurs() {
        state.biblio = state.cd.getAllLlibres().stream()
            .filter(l -> l.getPaginesLlegides() > 0 && !Boolean.TRUE.equals(l.getLlegit()))
            .collect(Collectors.toList());
        host.pageCtrl().setUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    void obrirConfiguracio() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(state.vista);
        new ConfiguracioDialog(
            w instanceof JFrame ? (JFrame) w : null,
            new ConfiguracioDialogListener() {
                @Override public void onThemeChange() { state.vista.applyTheme(); }
                @Override public void onRefreshData() {
                    state.biblio = new ArrayList<>(state.cd.getAllLlibres());
                    host.pageCtrl().setUseDBPagination(state.cd.isLargeLibrary());
                    state.currentLlistaId = null;
                    filterCtrl.quitarFiltros();
                    shelfCtrl.refreshComboLlistes();
                }
            },
            state.cd
        ).setVisible(true);
    }
}
