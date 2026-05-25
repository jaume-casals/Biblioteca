package api;

import herramienta.OpenLibraryClient;

public class MetaLookupRouter {

    public MetaLookupRouter(HttpRouter app) {
        app.get("/api/openlibrary/isbn/{isbn}",   ctx -> isbn(ctx));
        app.get("/api/openlibrary/title/{title}", ctx -> title(ctx));
    }

    private void isbn(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByISBN(ctx.pathParam("isbn")));
    }

    private void title(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByTitle(ctx.pathParam("title")));
    }
}
