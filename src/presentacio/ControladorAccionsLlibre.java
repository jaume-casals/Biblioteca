package presentacio;


import presentacio.config.ConfiguracioDialogListener;
import domini.Llibre;
import domini.Llista;
import herramienta.Configuracio;
import herramienta.DialegError;
import herramienta.I18n;
import herramienta.UITheme;
import presentacio.detalles.control.ControladorPanellDetallsLlibre;
import presentacio.listener.EnEliminarLlibre;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** CRUD de llibres, diàlegs de detalls, estadístiques, vistes ràpides i desfer. */
class ControladorAccionsLlibre {

    private final EstatVistaBiblioteca state;
    private final AmfitrioPantallaBiblioteca host;
    private final ControladorFiltre filtrarCtrl;
    private final ControladorPrestatgeria shelfCtrl;

    ControladorAccionsLlibre(EstatVistaBiblioteca state, AmfitrioPantallaBiblioteca host,
                          ControladorFiltre filtrarCtrl, ControladorPrestatgeria shelfCtrl) {
        this.state = state;
        this.host = host;
        this.filtrarCtrl = filtrarCtrl;
        this.shelfCtrl = shelfCtrl;
    }

    void wireListeners() {
        state.vista.obtenirBtnEstadistiques().addActionListener(e -> mostrarEstadistiques());
        state.vista.obtenirBtnLlibreAleatori().addActionListener(e -> mostrarLlibreAleatori());
        state.vista.obtenirBtnConfiguracio().addActionListener(e -> obrirConfiguracio());
        state.vista.obtenirBtnAfegitsRecentment().addActionListener(e -> mostrarAfegitsRecentment());
        state.vista.obtenirBtnLlegitsRecentment().addActionListener(e -> mostrarLlegitsRecentment());
        state.vista.obtenirBtnDesitjats().addActionListener(e -> mostrarDesitjats());
        state.vista.obtenirBtnEnCurs().addActionListener(e -> mostrarEnCurs());
    }

    void abrirDetallesLlibres() { abrirDetalles(false); }

    void abrirDetallesEnEdicio() {
        if (state.vista.esGaleriaMode()) {
            List<Llibre> sel = state.vista.obtenirGaleria().obtenirSelectedLlibres();
            if (!sel.isEmpty()) abrirDetallesDeLlibre(sel.get(0), true);
        } else {
            abrirDetalles(true);
        }
    }

    private void abrirDetalles(boolean editMode) {
        try {
            javax.swing.JTable table = state.vista.obtenirTaulaLlibres();
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return;
            int modelRow = table.convertRowIndexToModel(viewRow);
            javax.swing.table.TableModel model = table.getModel();
            if (!(model instanceof ModelTaulaBiblioteca bt)) return;
            Llibre l = bt.obtenirBookAt(modelRow);
            if (l == null) return;
            abrirDetallesDeLlibre(l, editMode);
        } catch (Exception e) {
            new DialegError(e).mostrarErrorMessage();
        }
    }

    void abrirDetallesDeLlibre(Llibre l, boolean editMode) {
        if (l == null) return;
        try {
            ControladorPanellDetallsLlibre detalles = new ControladorPanellDetallsLlibre(l, state.enActualizarBBDD, state.cd);
            detalles.obtenirDetallesLlibrePanel().setLocationRelativeTo(state.vista);
            if (editMode) detalles.obtenirDetallesLlibrePanel().obtenirBtnEditar().doClick();
            detalles.obtenirDetallesLlibrePanel().setVisible(true);
        } catch (Exception e) {
            new DialegError(e).mostrarErrorMessage();
        }
    }

    void refrescarLlibre(Llibre l, boolean nuevo) {
        if (!nuevo) host.refreshRow(l);
        else {
            host.afegirRow(l);
            host.actualitzarTitleBar();
        }
    }

    void eliminarFilaSeleccionada() {
        JTable t = state.vista.obtenirTaulaLlibres();
        int[] rows = t.getSelectedRows();
        if (rows.length == 0) return;
        String msg = rows.length == 1
            ? I18n.t("dlg_confirm_delete_one", t.getValueAt(rows[0], ModelTaulaBiblioteca.COL_NOM))
            : I18n.t("dlg_confirm_delete_n", rows.length);
        if (JOptionPane.showConfirmDialog(state.vista, msg, I18n.t("dlg_confirm_delete_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        List<Long> isbns = new ArrayList<>();
        for (int row : rows) {
            Object cell = t.getValueAt(row, ModelTaulaBiblioteca.COL_ISBN);
            if (!(cell instanceof String) && !(cell instanceof Number)) continue;
            try { isbns.add(Long.parseLong(cell.toString())); }
            catch (NumberFormatException nfe) { /* salta cel·les no numèriques */ }
        }
        for (long isbn : isbns) {
            try {
                state.cd.cercarLlibre(isbn).ifPresent(l -> {
                    EnEliminarLlibre.EsborrarEvent ev = new EnEliminarLlibre.EsborrarEvent(l, true);
                    state.enActualizarBBDD.enEliminantLlibre(ev);
                    if (!EnEliminarLlibre.hauriaProceed(ev)) return;
                    state.undoBuffer.push(l);
                    // Limita la cua d'undoing a UNDO_MAX (20). Quan
                    // l'usuari elimina més de UNDO_MAX llibres en un sol
                    // lot, les entrades més antigues es descarten en
                    // silenci — el límit és un topall de memòria, no un
                    // contracte de "només els 20 primers". La pila
                    // undo() n'és l'única consumidora.
                    if (state.undoBuffer.size() > EstatVistaBiblioteca.UNDO_MAX) state.undoBuffer.removeLast();
                    state.cd.eliminarLlibre(l);
                    eliminarFila(l);
                });
            } catch (Exception e) {
                new DialegError(e).mostrarErrorMessage();
            }
        }
    }

    void eliminarFila(Llibre l) {
        host.removeRow(l);
        if (state.biblio != null) state.biblio.removeIf(b -> java.util.Objects.equals(b.obtenirISBN(), l.obtenirISBN()));
        host.actualitzarTitleBar();
    }

    void undoDelete() {
        if (state.undoBuffer.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_undo_empty"), I18n.t("dlg_undo_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llibre l = state.undoBuffer.pop();
        try {
            state.cd.afegirLlibre(l);
            refrescarLlibre(l, true);
            JOptionPane.showMessageDialog(state.vista,
                I18n.t("dlg_undo_done", l.obtenirNom()), I18n.t("dlg_undo_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            new DialegError(e).mostrarErrorMessage();
        }
    }

    void mostrarEstadistiques() {
        List<Llibre> global = state.cd.obtenirAllLlibres();
        if (global.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_empty_library"), I18n.t("dlg_stats_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        AjudaEstadistiques.EstadistiquesLlibre globalStats = AjudaEstadistiques.computeStats(global);
        javax.swing.JPanel tab1 = AjudaEstadistiques.buildGeneralTab(global, globalStats, state.cd);

        javax.swing.JPanel tab2 = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        tab2.setBackground(UITheme.palette().bgPanel());
        tab2.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tab2.add(AjudaEstadistiques.buildReadingChart(globalStats.booksByReadYear));
        tab2.add(AjudaEstadistiques.buildPublisherChart(global));

        javax.swing.JPanel tab3 = AjudaEstadistiques.buildTagCloud(global, state.cd);
        javax.swing.JPanel tab4 = AjudaEstadistiques.buildReadingPacePanel(global, globalStats.booksByReadYear);

        AjudaEstadistiques.showDialog(SwingUtilities.getWindowAncestor(state.vista),
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
            .filter(l -> !Boolean.TRUE.equals(l.obtenirLlegit()))
            .collect(Collectors.toList());
        if (noLlegits.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_all_read"), I18n.t("dlg_aleatori_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Llibre aleatori = noLlegits.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(noLlegits.size()));
        JTable t = state.vista.obtenirTaulaLlibres();
        for (int row = 0; row < t.getRowCount(); row++) {
            if (String.valueOf(aleatori.obtenirISBN()).equals(t.getValueAt(row, ModelTaulaBiblioteca.COL_ISBN))) {
                t.setRowSelectionInterval(row, row);
                t.scrollRectToVisible(t.getCellRect(row, 0, true));
                break;
            }
        }
        ControladorPanellDetallsLlibre detalles = new ControladorPanellDetallsLlibre(aleatori, state.enActualizarBBDD, state.cd);
        detalles.obtenirDetallesLlibrePanel().setLocationRelativeTo(state.vista);
        detalles.obtenirDetallesLlibrePanel().setVisible(true);
    }

    private void mostrarAfegitsRecentment() {
        ArrayList<Llibre> recents = new ArrayList<>(state.cd.obtenirRecentlyAdded());
        if (recents.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_books_recent"),
                I18n.t("dlg_recently_added_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        state.biblio = recents;
        host.pageCtrl().posarUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().posarCurrentPage(0);
        host.mostrarPage(0);
    }

    private void mostrarLlegitsRecentment() {
        List<Llibre> llegits = state.cd.obtenirAllLlibres().stream()
            .filter(l -> Boolean.TRUE.equals(l.obtenirLlegit()))
            .collect(Collectors.toList());
        if (llegits.isEmpty()) {
            JOptionPane.showMessageDialog(state.vista, I18n.t("dlg_no_read"), I18n.t("dlg_read_title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        state.biblio = llegits;
        host.pageCtrl().posarUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().posarCurrentPage(0);
        host.mostrarPage(0);
    }

    private void mostrarDesitjats() {
        state.biblio = state.cd.obtenirAllLlibres().stream()
            .filter(l -> Boolean.TRUE.equals(l.esDesitjat()))
            .collect(Collectors.toList());
        host.pageCtrl().posarUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().posarCurrentPage(0);
        host.mostrarPage(0);
    }

    private void mostrarEnCurs() {
        state.biblio = state.cd.obtenirAllLlibres().stream()
            .filter(l -> l.obtenirPaginesLlegides() > 0 && !Boolean.TRUE.equals(l.obtenirLlegit()))
            .collect(Collectors.toList());
        host.pageCtrl().posarUseDBPagination(false);
        state.currentLlistaId = null;
        host.pageCtrl().posarCurrentPage(0);
        host.mostrarPage(0);
    }

    void obrirConfiguracio() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(state.vista);
        new ConfiguracioDialog(
            w instanceof JFrame ? (JFrame) w : null,
            new ConfiguracioDialogListener() {
                @Override public void enCanviarTema() { state.vista.aplicarTheme(); }
                @Override public void enRefrescarDades() {
                    state.biblio = new ArrayList<>(state.cd.obtenirAllLlibres());
                    host.pageCtrl().posarUseDBPagination(state.cd.esLargeLibrary());
                    state.currentLlistaId = null;
                    filtrarCtrl.quitarFiltros();
                    shelfCtrl.refrescarComboLlistes();
                }
            },
            state.cd
        ).setVisible(true);
    }
}
