package presentacio.controladors;

import domini.Llibre;
import herramienta.config.Configuracio;
import herramienta.config.ConfiguracioUi;
import herramienta.i18n.I18n;
import herramienta.ui.DialegError;
import herramienta.ui.UITheme;
import java.util.Comparator;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import presentacio.detalles.control.ControladorPanellDetallsLlibre;
import presentacio.panells.PanelMarcPrincipal;
import presentacio.util.AmfitrioPantallaBiblioteca;
import presentacio.util.EstatVistaBiblioteca;
import presentacio.util.UtilitatsLlibre;


/** Vista galeria vs taula, agrupació per sèrie i barra de títol. */
public class ControladorModeVista {

    private final EstatVistaBiblioteca state;
    private final AmfitrioPantallaBiblioteca host;
    private final ControladorMenuContextual contextMenuCtrl;

    ControladorModeVista(EstatVistaBiblioteca state, AmfitrioPantallaBiblioteca host,
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
                ControladorPanellDetallsLlibre detalles = new ControladorPanellDetallsLlibre(l, state.enActualizarBBDD, state.cd);
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
        if ("galeria".equals(Configuracio.obtenirViewMode())) activarGaleria();
    }

    private void activarGaleria() {
        state.vista.obtenirGaleria().actualitzarLlibres(UtilitatsLlibre.asArrayList(state.biblio));
        state.vista.mostrarGaleria();
        state.vista.obtenirBtnToggleVista().setText(I18n.t("btn_table_view"));
    }

    void restaurarSort() {
        int savedSortCol = Configuracio.obtenirSortColumn();
        if (savedSortCol >= 0 && savedSortCol < state.vista.obtenirTaulaLlibres().getColumnCount()) {
            javax.swing.RowSorter<?> sorter = state.vista.obtenirTaulaLlibres().getRowSorter();
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
            activarGaleria();
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
        JTable t = state.vista.obtenirTaulaLlibres();
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



