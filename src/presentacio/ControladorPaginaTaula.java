package presentacio;

import domini.Llibre;
import herramienta.I18n;
import interficie.BookReader;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Manages pagination state for the book table.
 * <p>Two pagination modes exist:
 * <ul>
 *   <li><b>In-memory</b> ({@code useDBPagination=false}): the full library is held in a
 *       list and sliced by {@link #PAGE_SIZE}.</li>
 *   <li><b>DB-level</b> ({@code useDBPagination=true}): pages are fetched from the database
 *       via {@code getLlibresPage(offset, limit)}; this activates when the library exceeds
 *       {@code ControladorDomini.isLargeLibrary()}.</li>
 * </ul>
 * <p>The pagination model ({@code currentPage}, {@code paginatedMode},
 * {@code useDBPagination}) is read by the enclosing
 * {@link MostrarBibliotecaControl} via getter methods; direct field access by
 * other classes is discouraged.
 */
class ControladorPaginaTaula {

    // PAGE_SIZE is unrelated to ControladorDomini.SQL_FILTER_THRESHOLD (2000); pagination kicks in at 100 rows, SQL-filter at 2000 books.
    static final int PAGE_SIZE = 100;

    private final PaginationModel model = new PaginationModel();
    private final PaginationView paginationView;

    int obtenirCurrentPage() { return model.obtenirCurrentPage(); }
    void posarCurrentPage(int p) { model.posarCurrentPage(p); }
    boolean esPaginatedMode() { return model.esPaginatedMode(); }
    boolean esUseDBPagination() { return model.esUseDBPagination(); }
    void posarUseDBPagination(boolean v) { model.posarUseDBPagination(v); }

    void invalidateCache() { model.invalidateCache(); }

    private final PanelMostrarBiblioteca vista;
    private final BookReader cd;
    private final Consumer<java.util.List<Llibre>> posarTable;

    ControladorPaginaTaula(PanelMostrarBiblioteca vista, BookReader cd,
                        Consumer<java.util.List<Llibre>> posarTable) {
        this.vista = vista;
        this.cd = cd;
        this.posarTable = posarTable;
        this.paginationView = new PaginationView(vista);
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
            if (model.obtenirCachedTotalCount() < 0) model.posarCachedTotalCount(cd.comptarLlibresDB());
            int totalCount = model.obtenirCachedTotalCount();
            if (totalCount <= PAGE_SIZE) {
                model.posarPaginatedMode(false);
                paginationView.hide();
                posarTable.accept(new ArrayList<>(cd.obtenirLlibresPage(0, PAGE_SIZE)));
                return;
            }
            int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            page = Math.max(0, Math.min(page, totalPages - 1));
            model.posarCurrentPage(page);
            model.posarPaginatedMode(true);
            posarTable.accept(new ArrayList<>(cd.obtenirLlibresPage(page * PAGE_SIZE, PAGE_SIZE)));
            paginationView.apply(page, totalPages);
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
}
