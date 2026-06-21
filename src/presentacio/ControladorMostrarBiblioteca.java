package presentacio;



import presentacio.UIComponents;
import domini.Llibre;
import herramienta.ui.UITheme;
import persistencia.contract.EscritorBiblioteca;
import presentacio.listener.EnActualitzarBBDD;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.util.ArrayList;

/** Coordinador prim per a la pantalla principal de la biblioteca; delega als sub-controladors. */
public class ControladorMostrarBiblioteca implements AmfitrioPantallaBiblioteca {

    public static void netejarCoverCache() { MemoriaImatgesCoberta.clear(); }

    private final EstatVistaBiblioteca state;
    private final ControladorTaula tableCtrl;
    private final ControladorPaginaTaula pageCtrl;
    private final JButton botonDetalles;
    private ControladorIOLlibre ioCtrl;

    private final ControladorFiltre filtrarCtrl;
    private final ControladorPrestatgeria shelfCtrl;
    private final ControladorAccionsLlibre bookActionsCtrl;
    private final ControladorMenuContextual contextMenuCtrl;
    private final ControladorModeVista viewModeCtrl;

    public ControladorMostrarBiblioteca(PanelMostrarBiblioteca vista, java.util.List<Llibre> biblio,
            EnActualitzarBBDD enActualizarBBDD, EscritorBiblioteca cd) {
        if (cd == null) throw new IllegalArgumentException("cd (EscritorBiblioteca) is required");
        this.state = new EstatVistaBiblioteca(vista, biblio, enActualizarBBDD, cd);
        this.botonDetalles = new JButton();
        UIComponents.styleAccentButton(this.botonDetalles);
        this.tableCtrl = new ControladorTaula(vista);
        this.pageCtrl = new ControladorPaginaTaula(vista, cd, this::posarTable);

        this.filtrarCtrl = new ControladorFiltre(state, this);
        this.shelfCtrl = new ControladorPrestatgeria(state, this, this);
        this.bookActionsCtrl = new ControladorAccionsLlibre(state, this, filtrarCtrl, shelfCtrl);
        this.contextMenuCtrl = new ControladorMenuContextual(state, this, shelfCtrl, bookActionsCtrl);
        this.viewModeCtrl = new ControladorModeVista(state, this, bookActionsCtrl, contextMenuCtrl);

        inicialitzarButtons();
        inicialitzarTable();
        filtrarCtrl.wireListeners();
        shelfCtrl.wireListeners();
        bookActionsCtrl.wireListeners();
        viewModeCtrl.wireListeners();
        viewModeCtrl.inicialitzarGaleria();

        state.loanedISBNs = cd.obtenirLoanedISBNs();
        tableCtrl.posarLoanedISBNs(state.loanedISBNs);
        shelfCtrl.refrescarComboLlistes();
        shelfCtrl.refrescarComboTags();

        mostrarPage(0);
        viewModeCtrl.restaurarViewMode();
        viewModeCtrl.restaurarSort();
    }

    private void inicialitzarButtons() {
        ioCtrl = new ControladorIOLlibre(state.vista, state.cd, this::obtenirCurrentViewBooks, this::refresh);
        state.vista.obtenirBtnPaginaAnterior().addActionListener(e -> mostrarPage(pageCtrl.obtenirCurrentPage() - 1));
        state.vista.obtenirBtnPaginaSeguent().addActionListener(e -> mostrarPage(pageCtrl.obtenirCurrentPage() + 1));
        state.vista.obtenirBtnExportCSV().addActionListener(e -> ioCtrl.exportarCSV());
        state.vista.obtenirBtnImportarCSV().addActionListener(e -> ioCtrl.importarCSV());
        state.vista.obtenirBtnImportarCalibre().addActionListener(e -> ioCtrl.importarCalibre());
        state.vista.obtenirBtnExportJSON().addActionListener(e -> ioCtrl.exportarJSON());
        state.vista.obtenirBtnImportarJSON().addActionListener(e -> ioCtrl.importarJSON());
        state.vista.obtenirBtnExportHTML().addActionListener(e -> ioCtrl.exportarHTML());
        state.vista.obtenirBtnExportPDF().addActionListener(e -> ioCtrl.exportarPDF());
        state.vista.obtenirBtnFetchCovers().addActionListener(e -> ioCtrl.fetchMissingCovers(state.vista.obtenirBtnFetchCovers()));
        state.vista.obtenirBtnEscanejarISBN().addActionListener(e -> ioCtrl.escanejarISBN());
        state.vista.obtenirBtnBackupBD().addActionListener(e -> ioCtrl.copiaSegBD());
        state.vista.obtenirBtnRestaurarBD().addActionListener(e -> ioCtrl.restaurarBD(() -> {
            state.currentLlistaId = null;
            refresh();
            shelfCtrl.refrescarComboLlistes();
            shelfCtrl.refrescarComboTags();
        }));
        state.vista.obtenirBtnSobre().addActionListener(e ->
            new QuantADialeg((java.awt.Frame) state.vista.getTopLevelAncestor()).setVisible(true));
        botonDetalles.addActionListener(e -> bookActionsCtrl.abrirDetallesLlibres());
    }

    private void inicialitzarTable() {
        tableCtrl.installInteractionListeners(this,
            bookActionsCtrl::abrirDetallesLlibres,
            filtrarCtrl::filtrar,
            contextMenuCtrl.contextMenu());
    }

    // ── AmfitrioPantallaBiblioteca ──────────────────────────────────────────────────────

    @Override public void posarTable(java.util.List<Llibre> llibres) {
        state.modelLibres = llibres != null ? new ArrayList<>(llibres) : new ArrayList<>();
        tableCtrl.posarBooks(llibres != null ? llibres : new ArrayList<>(), state.cd, botonDetalles,
            MemoriaImatgesCoberta.cache(), MemoriaImatgesCoberta.loading(), state.loanedISBNs, this::refreshRow);
        tableCtrl.aplicarColumnVisibility();
        filtrarCtrl.aplicarSearchBar();
        if (state.vista.esGaleriaMode()) {
            state.vista.obtenirGaleria().actualitzarLlibres(llibres instanceof ArrayList<Llibre> a ? a : new ArrayList<>(llibres));
        }
    }

    @Override public void mostrarPage(int page) { pageCtrl.mostrarPage(page, state.biblio); }

    @Override public void refrescarAll() { refresh(); }

    @Override public void actualitzarTitleBar() { viewModeCtrl.actualitzarTitleBar(); }

    @Override public void refrescarComboLlistes() { shelfCtrl.refrescarComboLlistes(); }

    @Override public void refrescarComboTags() { shelfCtrl.refrescarComboTags(); }

    @Override public void refreshRow(Llibre l) {
        int row = tableCtrl.indexOfIsbn(l.obtenirISBN());
        if (row >= 0) tableCtrl.refreshRow(row, l);
    }

    @Override public void removeRow(Llibre l) { tableCtrl.eliminarRowByIsbn(l.obtenirISBN()); }

    @Override public void afegirRow(Llibre l) { tableCtrl.afegirBook(l); }

    @Override public ControladorPaginaTaula pageCtrl() { return pageCtrl; }

    @Override public ControladorTaula tableCtrl() { return tableCtrl; }

    @Override public ControladorFiltre filtrarCtrl() { return filtrarCtrl; }

    @Override public ControladorPrestatgeria shelfCtrl() { return shelfCtrl; }

    @Override public ControladorAccionsLlibre bookActionsCtrl() { return bookActionsCtrl; }

    @Override public ControladorMenuContextual contextMenuCtrl() { return contextMenuCtrl; }

    @Override public ControladorModeVista viewModeCtrl() { return viewModeCtrl; }

    @Override public JButton detallesBtn() { return botonDetalles; }

    @Override public EstatVistaBiblioteca state() { return state; }

    // ── Public API (MainFrameControl, GestioLlistesDialog, EnActualitzarBBDD) ─

    public JPanel view() { return state.vista; }

    public void abrirDetallesEnEdicio() { bookActionsCtrl.abrirDetallesEnEdicio(); }

    public void refrescarLlibre(Llibre l, boolean nuevo) { bookActionsCtrl.refrescarLlibre(l, nuevo); }

    public void eliminarFilaSeleccionada() { bookActionsCtrl.eliminarFilaSeleccionada(); }

    public void eliminarFila(Llibre l) { bookActionsCtrl.eliminarFila(l); }

    public Integer obtenirCurrentLlistaId() { return state.currentLlistaId; }

    public void refresh() {
        pageCtrl.posarCurrentPage(0);
        final Integer currentLlistaId = state.currentLlistaId;
        new javax.swing.SwingWorker<java.util.List<Llibre>, Void>() {
            @Override protected java.util.List<Llibre> doInBackground() {
                return currentLlistaId != null
                    ? state.cd.obtenirLlibresInLlista(currentLlistaId)
                    : state.cd.obtenirAllLlibresSummary();
            }
            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    state.biblio = new ArrayList<>(get());
                    if (currentLlistaId == null) {
                        pageCtrl.posarUseDBPagination(state.cd.esLargeLibrary());
                    } else {
                        pageCtrl.posarUseDBPagination(false);
                    }
                    filtrarCtrl.quitarFiltros();
                } catch (Exception e) {
                    new herramienta.ui.DialegError(e).mostrarErrorMessage();
                }
            }
        }.execute();
    }

    public void undoDelete() { bookActionsCtrl.undoDelete(); }

    private java.util.List<Llibre> obtenirCurrentViewBooks() {
        return state.biblio != null ? new ArrayList<>(state.biblio) : new ArrayList<>();
    }
}
