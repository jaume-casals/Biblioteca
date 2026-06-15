package presentacio;

import domini.Llibre;
import herramienta.Config;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;
import herramienta.UiConfig;
import presentacio.detalles.control.DetallesLlibrePanelControl;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.Comparator;

/** Gallery vs table view, series grouping, and title bar. */
class ViewModeController {

    private final LibraryViewState state;
    private final LibraryScreenHost host;
    private final ContextMenuController contextMenuCtrl;

    ViewModeController(LibraryViewState state, LibraryScreenHost host,
                       BookActionsController bookActionsCtrl, ContextMenuController contextMenuCtrl) {
        this.state = state;
        this.host = host;
        this.contextMenuCtrl = contextMenuCtrl;
    }

    void wireListeners() {
        state.vista.getBtnToggleVista().addActionListener(e -> toggleVista());
        state.vista.getBtnGroupSeries().addActionListener(e -> toggleGroupBySeries());
    }

    void initGaleria() {
        state.vista.getGaleria().setCd(state.cd);
        state.vista.getGaleria().setOnCardClick(l -> {
            try {
                DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, state.enActualizarBBDD, state.cd);
                detalles.getDetallesLlibrePanel().setLocationRelativeTo(state.vista);
                detalles.getDetallesLlibrePanel().setVisible(true);
            } catch (Exception e) {
                new DialogoError(e).showErrorMessage();
            }
        });
        state.vista.getGaleria().setOnRightClick((e, sel) -> contextMenuCtrl.showGaleriaContextMenu(e, sel));
        state.vista.getGaleria().setOnDeleteSelected(sel -> contextMenuCtrl.eliminarLlibresGaleria(sel));
    }

    void restoreViewMode() {
        if ("galeria".equals(Config.getViewMode())) {
            state.vista.getGaleria().updateLlibres(
                state.biblio instanceof java.util.ArrayList<Llibre> a ? a : new java.util.ArrayList<>(state.biblio));
            state.vista.showGaleria();
            state.vista.getBtnToggleVista().setText(I18n.t("btn_table_view"));
        }
    }

    void restoreSort() {
        int savedSortCol = Config.getSortColumn();
        if (savedSortCol >= 0 && savedSortCol < state.vista.getjTableBilio().getColumnCount()) {
            javax.swing.RowSorter<?> sorter = state.vista.getjTableBilio().getRowSorter();
            if (sorter != null) {
                javax.swing.SortOrder order = "DESCENDING".equals(Config.getSortOrder())
                    ? javax.swing.SortOrder.DESCENDING : javax.swing.SortOrder.ASCENDING;
                sorter.setSortKeys(java.util.Collections.singletonList(
                    new javax.swing.RowSorter.SortKey(savedSortCol, order)));
            }
        }
    }

    void toggleVista() {
        if (!state.vista.isGaleriaMode()) {
            state.vista.getGaleria().updateLlibres(
                state.biblio instanceof java.util.ArrayList<Llibre> a ? a : new java.util.ArrayList<>(state.biblio));
            state.vista.showGaleria();
            state.vista.getBtnToggleVista().setText(I18n.t("btn_table_view"));
            UiConfig.setViewMode("galeria");
        } else {
            state.vista.showTaula();
            state.vista.getBtnToggleVista().setText(I18n.t("btn_gallery_view"));
            UiConfig.setViewMode("taula");
        }
    }

    void toggleGroupBySeries() {
        state.groupBySeries = !state.groupBySeries;
        state.vista.getBtnGroupSeries().setFont(state.groupBySeries
            ? state.vista.getBtnGroupSeries().getFont().deriveFont(java.awt.Font.BOLD)
            : UITheme.fontBold());
        if (state.groupBySeries && state.biblio != null) {
            java.util.List<Llibre> sorted = new java.util.ArrayList<>(state.biblio);
            sorted.sort(Comparator
                .comparing((Llibre l) -> l.getSerie() == null || l.getSerie().isBlank() ? "￿" : l.getSerie())
                .thenComparingInt(Llibre::getVolum));
            state.biblio = sorted;
        }
        host.pageCtrl().setCurrentPage(0);
        host.showPage(0);
    }

    void updateTitleBar() {
        JTable t = state.vista.getjTableBilio();
        int shown = t.getRowCount();
        int total = state.biblio != null ? state.biblio.size() : 0;
        java.awt.Window w = SwingUtilities.getWindowAncestor(state.vista);
        if (w instanceof JFrame frame && frame.getContentPane() instanceof MainFramePanel mfp) {
            frame.setTitle(I18n.t("app_title"));
            String shelf = state.currentLlistaId == null ? I18n.t("lbl_all_lists") : currentShelfName();
            String count = shown == total
                ? total + " llibres"
                : shown + " / " + total + " llibres";
            mfp.getStatusBar().setText(shelf + "  —  " + count);
        }
    }

    private String currentShelfName() {
        Object sel = state.vista.getComboLlistes().getSelectedItem();
        if (sel instanceof domini.Llista ll) return ll.getNom();
        return I18n.t("lbl_all_lists");
    }
}
