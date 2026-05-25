package presentacio;

import domini.Llibre;
import herramienta.I18n;
import interficie.BibliotecaWriter;

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
class TablePageController {

    // PAGE_SIZE is unrelated to ControladorDomini.SQL_FILTER_THRESHOLD (2000); pagination kicks in at 100 rows, SQL-filter at 2000 books.
    static final int PAGE_SIZE = 100;

    private final PaginationModel model = new PaginationModel();
    private final PaginationView paginationView;

    int getCurrentPage() { return model.getCurrentPage(); }
    void setCurrentPage(int p) { model.setCurrentPage(p); }
    boolean isPaginatedMode() { return model.isPaginatedMode(); }
    boolean isUseDBPagination() { return model.isUseDBPagination(); }
    void setUseDBPagination(boolean v) { model.setUseDBPagination(v); }

    void invalidateCache() { model.invalidateCache(); }

    private final MostrarBibliotecaPanel vista;
    private final BibliotecaWriter cd;
    private final Consumer<java.util.List<Llibre>> setTable;

    TablePageController(MostrarBibliotecaPanel vista, BibliotecaWriter cd,
                        Consumer<java.util.List<Llibre>> setTable) {
        this.vista = vista;
        this.cd = cd;
        this.setTable = setTable;
        this.paginationView = new PaginationView(vista);
    }

    void showPage(int page, java.util.List<Llibre> biblio) {
        if (biblio == null) {
            model.setPaginatedMode(false);
            model.invalidateCache();
            paginationView.hide();
            setTable.accept(new ArrayList<>());
            return;
        }
        if (model.isUseDBPagination() && cd.isLargeLibrary()) {
            if (model.getCachedTotalCount() < 0) model.setCachedTotalCount(cd.countLlibresDB());
            int totalCount = model.getCachedTotalCount();
            if (totalCount <= PAGE_SIZE) {
                model.setPaginatedMode(false);
                paginationView.hide();
                setTable.accept(new ArrayList<>(cd.getLlibresPage(0, PAGE_SIZE)));
                return;
            }
            int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            page = Math.max(0, Math.min(page, totalPages - 1));
            model.setCurrentPage(page);
            model.setPaginatedMode(true);
            setTable.accept(new ArrayList<>(cd.getLlibresPage(page * PAGE_SIZE, PAGE_SIZE)));
            paginationView.apply(page, totalPages);
            return;
        }
        if (biblio.size() <= PAGE_SIZE) {
            model.setPaginatedMode(false);
            model.invalidateCache();
            paginationView.hide();
            setTable.accept(biblio instanceof ArrayList<Llibre> a ? a : new ArrayList<>(biblio));
            return;
        }
        int totalPages = (int) Math.ceil((double) biblio.size() / PAGE_SIZE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        model.setCurrentPage(page);
        model.setPaginatedMode(true);
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, biblio.size());
        setTable.accept(new ArrayList<>(biblio.subList(from, to)));
        paginationView.apply(page, totalPages);
    }
}
