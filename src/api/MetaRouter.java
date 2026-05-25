package api;

import interficie.BibliotecaReader;

import java.util.Map;
import java.util.Set;

public class MetaRouter {

    private final BibliotecaReader cd;

    public MetaRouter(HttpRouter app, BibliotecaReader cd) {
        this.cd = cd;
        app.get("/api/meta/distinct/{column}", ctx -> distinct(ctx));
        app.get("/api/meta/authors",           ctx -> authors(ctx));
        app.get("/api/meta/dbsize",            ctx -> dbsize(ctx));
        app.get("/api/meta/stats",             ctx -> stats(ctx));
    }

    private static final Set<String> ALLOWED_DISTINCT;

    static {
        Set<String> s = new java.util.HashSet<>(persistencia.TagDao.AUTOCOMPLETE_COLUMNS);
        s.add("autors");
        ALLOWED_DISTINCT = Set.copyOf(s);
    }

    private void distinct(HttpCtx ctx) throws Exception {
        String col = ctx.pathParam("column");
        if (!ALLOWED_DISTINCT.contains(col)) {
            ctx.status(400).json(Map.of("error", "Unknown column: " + col));
            return;
        }
        if ("autors".equals(col)) { ctx.json(cd.getDistinctAutorNames()); return; }
        ctx.json(cd.getDistinctValues(col));
    }

    private void authors(HttpCtx ctx) {
        ctx.json(cd.getDistinctAutorNames());
    }

    private void dbsize(HttpCtx ctx) {
        long bytes = cd.getDbSizeBytes();
        ctx.json(Map.of("bytes", bytes, "mb", Math.round(bytes / 1024.0 / 1024.0 * 100.0) / 100.0));
    }

    private void stats(HttpCtx ctx) {
        var all = cd.getAllLlibres();
        int total = all.size();
        long read = 0;
        long wanted = 0;
        for (var l : all) {
            if (Boolean.TRUE.equals(l.getLlegit())) read++;
            if (Boolean.TRUE.equals(l.getDesitjat())) wanted++;
        }
        ctx.json(Map.of("total", total, "read", read, "unread", total - read, "wanted", wanted));
    }
}
