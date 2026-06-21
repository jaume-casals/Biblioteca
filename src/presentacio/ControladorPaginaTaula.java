package presentacio;

import domini.Llibre;
import herramienta.i18n.I18n;
import persistencia.contract.LectorLlibre;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Gestiona l'estat de paginació de la taula de llibres.
 * <p>Hi ha dos modes de paginació:
 * <ul>
 *   <li><b>En memòria</b> ({@code useDBPagination=false}): tota la biblioteca
 *       es manté en una llista i es talla per {@link #PAGE_SIZE}.</li>
 *   <li><b>A nivell de BBDD</b> ({@code useDBPagination=true}): les pàgines
 *       s'obtenen de la base de dades via
 *       {@code getLlibresPage(offset, limit)}; s'activa quan la biblioteca
 *       supera {@code ControladorDomini.isLargeLibrary()}.</li>
 * </ul>
 * <p>El model de paginació ({@code currentPage}, {@code paginatedMode},
 * {@code useDBPagination}) el llegeix el {@link ControladorMostrarBiblioteca}
 * contenidor via mètodes getter; l'accés directe al camp des d'altres
 * classes no és recomanable.
 */
class ControladorPaginaTaula {

    // PAGE_SIZE is unrelated to ControladorDomini.SQL_FILTER_THRESHOLD (2000); pagination kicks in at 100 rows, SQL-filter at 2000 books.
    static final int PAGE_SIZE = 100;

    private final ModelPaginacio model = new ModelPaginacio();
    private final VistaPaginacio paginationView;

    int obtenirCurrentPage() { return model.obtenirCurrentPage(); }
    void posarCurrentPage(int p) { model.posarCurrentPage(p); }
    boolean esPaginatedMode() { return model.esPaginatedMode(); }
    boolean esUseDBPagination() { return model.esUseDBPagination(); }
    void posarUseDBPagination(boolean v) { model.posarUseDBPagination(v); }

    void invalidateCache() { model.invalidateCache(); }

    private final PanelMostrarBiblioteca vista;
    private final LectorLlibre cd;
    private final Consumer<java.util.List<Llibre>> posarTable;

    ControladorPaginaTaula(PanelMostrarBiblioteca vista, LectorLlibre cd,
                        Consumer<java.util.List<Llibre>> posarTable) {
        this.vista = vista;
        this.cd = cd;
        this.posarTable = posarTable;
        this.paginationView = new VistaPaginacio(vista);
    }

    void mostrarPage(int page, java.util.List<Llibre> biblio) {
        if (biblio == null) {
            model.posarPaginatedMode(false);
            model.invalidateCache();
            paginationView.hide();
            posarTable.accept(new ArrayList<>());
            return;
        }
        if (model.esUseDBPagination() && cd.esLargeLibrary()) {
            final int requestedPage = page;
            new javax.swing.SwingWorker<PageLoad, Void>() {
                @Override protected PageLoad doInBackground() {
                    if (model.obtenirCachedTotalCount() < 0) {
                        model.posarCachedTotalCount(cd.comptarLlibresDB());
                    }
                    int totalCount = model.obtenirCachedTotalCount();
                    if (totalCount <= PAGE_SIZE) {
                        ArrayList<Llibre> rows = new ArrayList<>(cd.obtenirLlibresPage(0, PAGE_SIZE));
                        return new PageLoad(false, 0, 1, rows);
                    }
                    int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                    int p = Math.max(0, Math.min(requestedPage, totalPages - 1));
                    ArrayList<Llibre> rows = new ArrayList<>(cd.obtenirLlibresPage(p * PAGE_SIZE, PAGE_SIZE));
                    return new PageLoad(true, p, totalPages, rows);
                }
                @Override protected void done() {
                    if (isCancelled()) return;
                    try {
                        PageLoad r = get();
                        if (!r.paginated) {
                            model.posarPaginatedMode(false);
                            paginationView.hide();
                            posarTable.accept(r.rows);
                        } else {
                            model.posarCurrentPage(r.page);
                            model.posarPaginatedMode(true);
                            posarTable.accept(r.rows);
                            paginationView.apply(r.page, r.totalPages);
                        }
                    } catch (Exception ex) {
                        new herramienta.ui.DialegError(ex).mostrarErrorMessage();
                    }
                }
            }.execute();
            return;
        }
        if (biblio.size() <= PAGE_SIZE) {
            model.posarPaginatedMode(false);
            model.invalidateCache();
            paginationView.hide();
            posarTable.accept(biblio instanceof ArrayList<Llibre> a ? a : new ArrayList<>(biblio));
            return;
        }
        int totalPages = (int) Math.ceil((double) biblio.size() / PAGE_SIZE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        model.posarCurrentPage(page);
        model.posarPaginatedMode(true);
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, biblio.size());
        posarTable.accept(new ArrayList<>(biblio.subList(from, to)));
        paginationView.apply(page, totalPages);
    }

    private record PageLoad(boolean paginated, int page, int totalPages, ArrayList<Llibre> rows) {}
}
