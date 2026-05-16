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

    private static final Set<String> ALLOWED_DISTINCT = Set.of("editorial", "serie", "idioma", "pais_origen", "autors");

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
        long read = all.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
        long wanted = all.stream().filter(l -> Boolean.TRUE.equals(l.getDesitjat())).count();
        ctx.json(Map.of("total", total, "read", read, "unread", total - read, "wanted", wanted));
    }
}
