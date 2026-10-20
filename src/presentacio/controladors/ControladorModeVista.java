package presentacio.controladors;

import domini.Llibre;
import herramienta.config.Configuracio;
import herramienta.config.ConfiguracioUi;
import herramienta.i18n.I18n;
import herramienta.ui.DialegError;
import java.util.Comparator;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import presentacio.detalles.control.ControladorPanellDetallsLlibre;
import presentacio.panells.PanelMarcPrincipal;
import presentacio.util.AmfitrioPantallaBiblioteca;
import presentacio.util.EstatVistaBiblioteca;
import presentacio.util.UtilitatsLlibre;


/** Vista galeria vs taula, agrupació per sèrie, ordenació i capçalera de la vista. */
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
        state.vista.obtenirBtnVistaGaleria().addActionListener(e -> activarGaleria());
        state.vista.obtenirBtnVistaTaula().addActionListener(e -> activarTaula());
        state.vista.obtenirBtnAgruparSeries().addActionListener(e -> toggleGroupBySeries());
        state.vista.obtenirBtnOrdena().addActionListener(e -> obrirMenuOrdenacio());
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
        else state.vista.obtenirBarraSuperior().posarModeVista(false);
    }

    private void activarGaleria() {
        state.vista.obtenirGaleria().actualitzarLlibres(UtilitatsLlibre.asArrayList(state.biblio));
        state.vista.mostrarGaleria();
        state.vista.obtenirBarraSuperior().posarModeVista(true);
        ConfiguracioUi.posarViewMode("galeria");
    }

    private void activarTaula() {
        state.vista.mostrarTaula();
        state.vista.obtenirBarraSuperior().posarModeVista(false);
        ConfiguracioUi.posarViewMode("taula");
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

    void toggleGroupBySeries() {
        state.groupBySeries = !state.groupBySeries;
        state.vista.obtenirBarraSuperior().posarAgruparSeries(state.groupBySeries);
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

    /**
     * Menú d'ordenació.
     *
     * <p>Les opcions apliquen una clau d'ordenació al {@code RowSorter} de la
     * taula i la desen, de manera que la preferència sobreviu a la sessió.
     * L'ordenació per columna de la capçalera es conserva.
     */
    private void obrirMenuOrdenacio() {
        JPopupMenu menu = new JPopupMenu();
        int[] columnes = {
            presentacio.models.ModelTaulaBiblioteca.COL_NOM,
            presentacio.models.ModelTaulaBiblioteca.COL_AUTOR,
            presentacio.models.ModelTaulaBiblioteca.COL_ANY,
            presentacio.models.ModelTaulaBiblioteca.COL_VALORACIO
        };
        for (int col : columnes) {
            if (col < 0 || col >= state.vista.obtenirTaulaLlibres().getColumnCount()) continue;
            String etiqueta = state.vista.obtenirTaulaLlibres().getColumnName(col);
            JMenuItem item = new JMenuItem(etiqueta);
            item.addActionListener(e -> aplicarOrdenacio(col, javax.swing.SortOrder.ASCENDING));
            menu.add(item);
        }
        JMenuItem desc = new JMenuItem(I18n.t("sort_descending"));
        desc.addActionListener(e -> aplicarOrdenacio(
            presentacio.models.ModelTaulaBiblioteca.COL_NOM, javax.swing.SortOrder.DESCENDING));
        menu.add(desc);
        menu.show(state.vista.obtenirBtnOrdena(), 0, state.vista.obtenirBtnOrdena().getHeight());
    }

    private void aplicarOrdenacio(int column, javax.swing.SortOrder order) {
        JTable t = state.vista.obtenirTaulaLlibres();
        javax.swing.RowSorter<?> sorter = t.getRowSorter();
        if (sorter == null) return;
        sorter.setSortKeys(java.util.Collections.singletonList(
            new javax.swing.RowSorter.SortKey(column, order)));
        ConfiguracioUi.posarSortColumn(column);
        ConfiguracioUi.posarSortOrder(order == javax.swing.SortOrder.DESCENDING ? "DESCENDING" : "ASCENDING");
    }

    /**
     * Actualitza la capçalera i la barra d'estat.
     *
     * <p>El recompte prové d'una única font
     * ({@link EstatVistaBiblioteca#obtenirLlibresEnAmbit()}): el nombre de
     * llibres que coincideixen amb l'àmbit i el filtre vigents. Abans es
     * derivava de la mida de la llista en memòria i del nombre de files de la
     * taula, que podien divergir i produir recomptes com "1000 / 0 llibres".
     */
    void actualitzarTitleBar() {
        int enAmbit = state.obtenirLlibresEnAmbit();
        int mostrats = state.obtenirLlibresMostrats();

        String titol = state.obtenirTitolVista();
        if (titol == null || titol.isBlank()) titol = I18n.t("nav_all_books");
        state.vista.obtenirCapcalera().posarTitol(titol);
        state.vista.obtenirCapcalera().posarComptador(textoCompte(enAmbit, mostrats));

        JTable t = state.vista.obtenirTaulaLlibres();
        java.awt.Window w = SwingUtilities.getWindowAncestor(state.vista);
        if (w instanceof JFrame frame) {
            frame.setTitle(I18n.t("app_title"));
            if (frame.getContentPane() instanceof PanelMarcPrincipal mfp) {
                mfp.obtenirStatusBar().setText(titol + "  —  " + textoCompte(enAmbit, mostrats));
            }
        }
    }

    /**
     * Text del recompte.
     *
     * <p>"N llibres" quan tot el que coincideix es mostra; "N de M" quan la
     * vista està paginada i només se'n mostra una part.
     */
    private String textoCompte(int enAmbit, int mostrats) {
        if (mostrats >= enAmbit) return I18n.t("lbl_books_count", enAmbit);
        return I18n.t("lbl_books_count_range", mostrats, enAmbit);
    }
}
