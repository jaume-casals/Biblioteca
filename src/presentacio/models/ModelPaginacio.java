package presentacio.models;
import presentacio.controladors.ControladorPaginaTaula;

/** Estat de paginació extret de {@link ControladorPaginaTaula}. */
public class ModelPaginacio {

    private int currentPage = 0;
    private boolean paginatedMode = false;
    private boolean useDBPagination = false;
    private int cachedTotalCount = -1;

    public int obtenirCurrentPage() { return currentPage; }
    public void posarCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public boolean esPaginatedMode() { return paginatedMode; }
    public void posarPaginatedMode(boolean paginatedMode) { this.paginatedMode = paginatedMode; }

    public boolean esUseDBPagination() { return useDBPagination; }
    public void posarUseDBPagination(boolean useDBPagination) { this.useDBPagination = useDBPagination; }

    public int obtenirCachedTotalCount() { return cachedTotalCount; }
    public void posarCachedTotalCount(int cachedTotalCount) { this.cachedTotalCount = cachedTotalCount; }

    public void invalidateCache() { cachedTotalCount = -1; }
}

