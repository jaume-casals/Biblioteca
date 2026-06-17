package presentacio;

import domini.Llibre;
import herramienta.Configuracio;
import herramienta.DialegError;
import herramienta.I18n;
import herramienta.UITheme;
import herramienta.ConfiguracioUi;
import presentacio.detalles.control.DetallesLlibrePanelControl;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.Comparator;

/** Gallery vs table view, series grouping, and title bar. */
class ControladorModeVista {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final ControladorMenuContextual contextMenuCtrl;

    ControladorModeVista(LibraryViewState state, LibraryScreenHost host,
                       ControladorAccionsLlibre bookActionsCtrl, ControladorMenuContextual contextMenuCtrl) {
        this.state = state;
        this.host = host;
        this.contextMenuCtrl = contextMenuCtrl;
    }

    void wireListeners() {
        state.vista.obtenirBtnToggleVista().addActionListener(e -> toggleVista());
        state.vista.obtenirBtnGroupSeries().addActionListener(e -> toggleGroupBySeries());
    }

    void inicialitzarGaleria() {
        state.vista.obtenirGaleria().posarCd(state.cd);
        state.vista.obtenirGaleria().posarOnCardClick(l -> {
            try {
                DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, state.enActualizarBBDD, state.cd);
                detalles.obtenirDetallesLlibrePanel().setLocationRelativeTo(state.vista);
                detalles.obtenirDetallesLlibrePanel().setVisible(true);
            } catch (Exception e) {
                new DialegError(e).mostrarErrorMessage();
            }
        });
        state.vista.obtenirGaleria().posarOnRightClick((e, sel) -> contextMenuCtrl.mostrarGaleriaContextMenu(e, sel));
        state.vista.obtenirGaleria().posarOnDeleteSelected(sel -> contextMenuCtrl.eliminarLlibresGaleria(sel));
    }

    void restaurarViewMode() {
        if ("galeria".equals(Configuracio.obtenirViewMode())) {
            state.vista.obtenirGaleria().actualitzarLlibres(
                state.biblio instanceof java.util.ArrayList<Llibre> a ? a : new java.util.ArrayList<>(state.biblio));
            state.vista.mostrarGaleria();
            state.vista.obtenirBtnToggleVista().setText(I18n.t("btn_table_view"));
        }
    }

    void restaurarSort() {
        int savedSortCol = Configuracio.obtenirSortColumn();
        if (savedSortCol >= 0 && savedSortCol < state.vista.getjTableBilio().getColumnCount()) {
            javax.swing.RowSorter<?> sorter = state.vista.getjTableBilio().getRowSorter();
            if (sorter != null) {
                javax.swing.SortOrder order = "DESCENDING".equals(Configuracio.getSortOrder())
                    ? javax.swing.SortOrder.DESCENDING : javax.swing.SortOrder.ASCENDING;
                sorter.setSortKeys(java.util.Collections.singletonList(
                    new javax.swing.RowSorter.SortKey(savedSortCol, order)));
            }
        }
    }

    void toggleVista() {
        if (!state.vista.esGaleriaMode()) {
            state.vista.obtenirGaleria().actualitzarLlibres(
                state.biblio instanceof java.util.ArrayList<Llibre> a ? a : new java.util.ArrayList<>(state.biblio));
            state.vista.mostrarGaleria();
            state.vista.obtenirBtnToggleVista().setText(I18n.t("btn_table_view"));
            ConfiguracioUi.posarViewMode("galeria");
        } else {
            state.vista.mostrarTaula();
            state.vista.obtenirBtnToggleVista().setText(I18n.t("btn_gallery_view"));
            ConfiguracioUi.posarViewMode("taula");
        }
    }

    void toggleGroupBySeries() {
        state.groupBySeries = !state.groupBySeries;
        state.vista.obtenirBtnGroupSeries().setFont(state.groupBySeries
            ? state.vista.obtenirBtnGroupSeries().getFont().deriveFont(java.awt.Font.BOLD)
            : UITheme.fontBold());
        if (state.groupBySeries && state.biblio != null) {
            java.util.List<Llibre> sorted = new java.util.ArrayList<>(state.biblio);
            sorted.sort(Comparator
                .comparing((Llibre l) -> l.obtenirSerie() == null || l.obtenirSerie().isBlank() ? "￿" : l.obtenirSerie())
                .thenComparingInt(Llibre::obtenirVolum));
            state.biblio = sorted;
        }
        host.pageCtrl().posarCurrentPage(0);
        host.mostrarPage(0);
    }

    void actualitzarTitleBar() {
        JTable t = state.vista.getjTableBilio();
        int shown = t.getRowCount();
        int total = state.biblio != null ? state.biblio.size() : 0;
        java.awt.Window w = SwingUtilities.getWindowAncestor(state.vista);
        if (w instanceof JFrame frame && frame.getContentPane() instanceof PanelMarcPrincipal mfp) {
            frame.setTitle(I18n.t("app_title"));
            String shelf = state.currentLlistaId == null ? I18n.t("lbl_all_lists") : currentShelfName();
            String count = shown == total
                ? total + " llibres"
                : shown + " / " + total + " llibres";
            mfp.obtenirStatusBar().setText(shelf + "  —  " + count);
        }
    }

    private String currentShelfName() {
        Object sel = state.vista.obtenirComboLlistes().getSelectedItem();
        if (sel instanceof domini.Llista ll) return ll.obtenirNom();
        return I18n.t("lbl_all_lists");
    }
}
