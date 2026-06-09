package presentacio;



import presentacio.UIComponents;
import domini.Llibre;
import herramienta.UITheme;
import interficie.BibliotecaWriter;
import presentacio.listener.EnActualizarBBDD;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.util.ArrayList;

/** Slim coordinator for the main library screen; delegates to sub-controllers. */
public class MostrarBibliotecaControl implements LibraryScreenHost {

    public static void clearCoverCache() { CoverImageCache.clear(); }

    private final LibraryViewState state;
    private final TableController tableCtrl;
    private final TablePageController pageCtrl;
    private final JButton botonDetalles;
    private BookIOController ioCtrl;

    private final FilterController filterCtrl;
    private final ShelfController shelfCtrl;
    private final BookActionsController bookActionsCtrl;
    private final ContextMenuController contextMenuCtrl;
    private final ViewModeController viewModeCtrl;

    public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, java.util.List<Llibre> biblio,
            EnActualizarBBDD enActualizarBBDD) {
        this(vista, biblio, enActualizarBBDD, null);
    }

    public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, java.util.List<Llibre> biblio,
            EnActualizarBBDD enActualizarBBDD, BibliotecaWriter cd) {
        BibliotecaWriter writer = cd != null ? cd : domini.ControladorDomini.getInstance();
        this.state = new LibraryViewState(vista, biblio, enActualizarBBDD, writer);
        this.botonDetalles = new JButton();
        UIComponents.styleAccentButton(this.botonDetalles);
        this.tableCtrl = new TableController(vista);
        this.pageCtrl = new TablePageController(vista, writer, this::setTable);

        this.filterCtrl = new FilterController(state, this);
        this.shelfCtrl = new ShelfController(state, this, this);
        this.bookActionsCtrl = new BookActionsController(state, this, filterCtrl, shelfCtrl);
        this.contextMenuCtrl = new ContextMenuController(state, this, shelfCtrl, bookActionsCtrl);
        this.viewModeCtrl = new ViewModeController(state, this, bookActionsCtrl, contextMenuCtrl);

        initButtons();
        initTable();
        filterCtrl.wireListeners();
        shelfCtrl.wireListeners();
        bookActionsCtrl.wireListeners();
        viewModeCtrl.wireListeners();
        viewModeCtrl.initGaleria();

        state.loanedISBNs = writer.getLoanedISBNs();
        shelfCtrl.refreshComboLlistes();
        shelfCtrl.refreshComboTags();

        showPage(0);
        viewModeCtrl.restoreViewMode();
        viewModeCtrl.restoreSort();
    }

    private void initButtons() {
        ioCtrl = new BookIOController(state.vista, state.cd, this::getCurrentViewBooks, this::refresh);
        state.vista.getBtnPaginaAnterior().addActionListener(e -> showPage(pageCtrl.getCurrentPage() - 1));
        state.vista.getBtnPaginaSeguent().addActionListener(e -> showPage(pageCtrl.getCurrentPage() + 1));
        state.vista.getBtnExportCSV().addActionListener(e -> ioCtrl.exportarCSV());
        state.vista.getBtnImportarCSV().addActionListener(e -> ioCtrl.importarCSV());
        state.vista.getBtnImportarCalibre().addActionListener(e -> ioCtrl.importarCalibre());
        state.vista.getBtnExportJSON().addActionListener(e -> ioCtrl.exportarJSON());
        state.vista.getBtnImportarJSON().addActionListener(e -> ioCtrl.importarJSON());
        state.vista.getBtnExportHTML().addActionListener(e -> ioCtrl.exportarHTML());
        state.vista.getBtnExportPDF().addActionListener(e -> ioCtrl.exportarPDF());
        state.vista.getBtnFetchCovers().addActionListener(e -> ioCtrl.fetchMissingCovers(state.vista.getBtnFetchCovers()));
        state.vista.getBtnEscanejarISBN().addActionListener(e -> ioCtrl.escanejarISBN());
        state.vista.getBtnBackupBD().addActionListener(e -> ioCtrl.backupBD());
        state.vista.getBtnRestaurarBD().addActionListener(e -> ioCtrl.restaurarBD(() -> {
            state.currentLlistaId = null;
            refresh();
            shelfCtrl.refreshComboLlistes();
            shelfCtrl.refreshComboTags();
        }));
        state.vista.getBtnSobre().addActionListener(e ->
            new AboutDialog((java.awt.Frame) state.vista.getTopLevelAncestor()).setVisible(true));
        botonDetalles.addActionListener(e -> bookActionsCtrl.abrirDetallesLlibres());
    }

    private void initTable() {
        tableCtrl.installInteractionListeners(this,
            bookActionsCtrl::abrirDetallesLlibres,
            filterCtrl::filtrar,
            contextMenuCtrl.contextMenu());
    }

    // ── LibraryScreenHost ──────────────────────────────────────────────────────

    @Override public void setTable(java.util.List<Llibre> llibres) {
        state.modelLibres = llibres instanceof ArrayList<Llibre> a ? a : new ArrayList<>(llibres);
        tableCtrl.setBooks(llibres != null ? llibres : new ArrayList<>(), state.cd, botonDetalles,
            CoverImageCache.cache(), CoverImageCache.loading(), () -> state.loanedISBNs, this::refreshRow);
        tableCtrl.applyColumnVisibility();
        filterCtrl.aplicarSearchBar();
        if (state.vista.isGaleriaMode()) {
            state.vista.getGaleria().updateLlibres(llibres instanceof ArrayList<Llibre> a ? a : new ArrayList<>(llibres));
        }
    }

    @Override public void showPage(int page) { pageCtrl.showPage(page, state.biblio); }

    @Override public void refreshAll() { refresh(); }

    @Override public void updateTitleBar() { viewModeCtrl.updateTitleBar(); }

    @Override public void refreshComboLlistes() { shelfCtrl.refreshComboLlistes(); }

    @Override public void refreshComboTags() { shelfCtrl.refreshComboTags(); }

    @Override public void refreshRow(Llibre l) {
        int row = tableCtrl.indexOfIsbn(l.getISBN());
        if (row >= 0) tableCtrl.refreshRow(row, l);
    }

    @Override public void removeRow(Llibre l) { tableCtrl.removeRowByIsbn(l.getISBN()); }

    @Override public void appendRow(Llibre l) { tableCtrl.appendBook(l); }

    @Override public TablePageController pageCtrl() { return pageCtrl; }

    @Override public TableController tableCtrl() { return tableCtrl; }

    @Override public JButton detallesBtn() { return botonDetalles; }

    @Override public LibraryViewState state() { return state; }

    // ── Public API (MainFrameControl, GestioLlistesDialog, EnActualizarBBDD) ─

    public JPanel view() { return state.vista; }

    public void abrirDetallesEnEdicio() { bookActionsCtrl.abrirDetallesEnEdicio(); }

    public void refreshLlibre(Llibre l, boolean nuevo) { bookActionsCtrl.refreshLlibre(l, nuevo); }

    public void eliminarFilaSeleccionada() { bookActionsCtrl.eliminarFilaSeleccionada(); }

    public void eliminarFila(Llibre l) { bookActionsCtrl.eliminarFila(l); }

    public Integer getCurrentLlistaId() { return state.currentLlistaId; }

    public void refresh() {
        pageCtrl.setCurrentPage(0);
        if (state.currentLlistaId != null) {
            state.biblio = new ArrayList<>(state.cd.getLlibresInLlista(state.currentLlistaId));
            pageCtrl.setUseDBPagination(false);
        } else {
            // Lean — heavy text/notes/cover are loaded lazily by
            // DetallesLlibrePanelControl via loadHeavyFields() when the
            // user opens the book detail dialog.
            state.biblio = new ArrayList<>(state.cd.getAllLlibresSummary());
            pageCtrl.setUseDBPagination(state.cd.isLargeLibrary());
        }
        filterCtrl.quitarFiltros();
    }

    public void undoDelete() { bookActionsCtrl.undoDelete(); }

    private java.util.List<Llibre> getCurrentViewBooks() {
        return state.biblio != null ? new ArrayList<>(state.biblio) : new ArrayList<>();
    }
}
