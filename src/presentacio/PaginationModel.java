package presentacio;

/** Pagination state extracted from {@link TablePageController}. */
public class PaginationModel {

    private int currentPage = 0;
    private boolean paginatedMode = false;
    private boolean useDBPagination = false;
    private int cachedTotalCount = -1;

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public boolean isPaginatedMode() { return paginatedMode; }
    public void setPaginatedMode(boolean paginatedMode) { this.paginatedMode = paginatedMode; }

    public boolean isUseDBPagination() { return useDBPagination; }
    public void setUseDBPagination(boolean useDBPagination) { this.useDBPagination = useDBPagination; }

    public int getCachedTotalCount() { return cachedTotalCount; }
    public void setCachedTotalCount(int cachedTotalCount) { this.cachedTotalCount = cachedTotalCount; }

    public void invalidateCache() { cachedTotalCount = -1; }
}
