package presentacio;

import domini.Llibre;
import herramienta.I18n;
import interficie.BibliotecaWriter;

import java.util.ArrayList;
import java.util.function.Consumer;

class TablePageController {

    // PAGE_SIZE is unrelated to ControladorDomini.SQL_FILTER_THRESHOLD (2000); pagination kicks in at 100 rows, SQL-filter at 2000 books.
    static final int PAGE_SIZE = 100;

    private int currentPage = 0;
    private boolean paginatedMode = false;
    private boolean useDBPagination = false;

    int getCurrentPage() { return currentPage; }
    void setCurrentPage(int p) { this.currentPage = p; }
    boolean isPaginatedMode() { return paginatedMode; }
    boolean isUseDBPagination() { return useDBPagination; }
    void setUseDBPagination(boolean v) { this.useDBPagination = v; }

    private final MostrarBibliotecaPanel vista;
    private final BibliotecaWriter cd;
    private final Consumer<ArrayList<Llibre>> setTable;

    TablePageController(MostrarBibliotecaPanel vista, BibliotecaWriter cd,
                        Consumer<ArrayList<Llibre>> setTable) {
        this.vista = vista;
        this.cd = cd;
        this.setTable = setTable;
    }

    void showPage(int page, ArrayList<Llibre> biblio) {
        if (biblio == null) {
            paginatedMode = false;
            vista.getPaginationPanel().setVisible(false);
            setTable.accept(new ArrayList<>());
            return;
        }
        if (useDBPagination && cd.isLargeLibrary()) {
            int totalCount = cd.countLlibresDB();
            if (totalCount <= PAGE_SIZE) {
                paginatedMode = false;
                vista.getPaginationPanel().setVisible(false);
                setTable.accept(cd.getLlibresPage(0, PAGE_SIZE));
                return;
            }
            int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            page = Math.max(0, Math.min(page, totalPages - 1));
            currentPage = page;
            paginatedMode = true;
            setTable.accept(cd.getLlibresPage(page * PAGE_SIZE, PAGE_SIZE));
            vista.getLblPagina().setText(I18n.t("page_info_java", page + 1, totalPages));
            vista.getBtnPaginaAnterior().setEnabled(page > 0);
            vista.getBtnPaginaSeguent().setEnabled(page < totalPages - 1);
            vista.getPaginationPanel().setVisible(true);
            return;
        }
        if (biblio.size() <= PAGE_SIZE) {
            paginatedMode = false;
            vista.getPaginationPanel().setVisible(false);
            setTable.accept(biblio);
            return;
        }
        int totalPages = (int) Math.ceil((double) biblio.size() / PAGE_SIZE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        currentPage = page;
        paginatedMode = true;
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, biblio.size());
        setTable.accept(new ArrayList<>(biblio.subList(from, to)));
        vista.getLblPagina().setText(I18n.t("page_info_java", page + 1, totalPages));
        vista.getBtnPaginaAnterior().setEnabled(page > 0);
        vista.getBtnPaginaSeguent().setEnabled(page < totalPages - 1);
        vista.getPaginationPanel().setVisible(true);
    }
}
