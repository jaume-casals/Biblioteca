package api;

import domini.LlibreFilter;

final class LlibreFilterFactory {
    private LlibreFilterFactory() {}

    /** Build a filter from query-string parameters on {@code ctx}. */
    static LlibreFilter fromQuery(HttpCtx ctx) {
        LlibreFilter f = LlibreFilter.empty();
        String isbnStr = ctx.queryParamOrNull("isbn");
        if (isbnStr != null) { try { f.isbn = Long.parseLong(isbnStr); } catch (NumberFormatException ignored) {} }
        f.autor        = ctx.queryParamOrNull("author");
        f.nom          = ctx.queryParamOrNull("title");
        f.anyMin       = ctx.queryParamInt("yearMin");
        f.anyMax       = ctx.queryParamInt("yearMax");
        f.valoracioMin = ctx.queryParamDbl("ratingMin");
        f.valoracioMax = ctx.queryParamDbl("ratingMax");
        f.preuMin      = ctx.queryParamDbl("priceMin");
        f.preuMax      = ctx.queryParamDbl("priceMax");
        f.llegit       = ctx.queryParamBool("read");
        f.tagId        = ctx.queryParamInt("tagId");
        f.llistaId     = ctx.queryParamInt("llistaId");
        f.editorial    = ctx.queryParamOrNull("editorial");
        f.serie        = ctx.queryParamOrNull("serie");
        f.format       = ctx.queryParamOrNull("format");
        f.idioma       = ctx.queryParamOrNull("idioma");
        f.sortColumn   = ctx.queryParamOrNull("sort");
        String sortDir = ctx.queryParam("sortDir");
        if (sortDir != null) f.sortAsc = !"desc".equalsIgnoreCase(sortDir);
        return f;
    }
}
