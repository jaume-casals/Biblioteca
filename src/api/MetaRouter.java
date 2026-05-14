package api;

import interficie.BibliotecaWriter;

import java.util.Map;

public class MetaRouter {

    private final BibliotecaWriter cd;

    public MetaRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        app.get("/api/meta/distinct/{column}", ctx -> distinct(ctx));
        app.get("/api/meta/authors",           ctx -> authors(ctx));
        app.get("/api/meta/dbsize",            ctx -> dbsize(ctx));
    }

    private void distinct(HttpCtx ctx) {
        ctx.json(cd.getDistinctValues(ctx.pathParam("column")));
    }

    private void authors(HttpCtx ctx) {
        ctx.json(cd.getDistinctAutorNames());
    }

    private void dbsize(HttpCtx ctx) {
        ctx.json(Map.of("bytes", cd.getDbSizeBytes()));
    }
}
