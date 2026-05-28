package api;

import herramienta.OpenLibraryClient;

public class OpenLibraryRouter {

    public OpenLibraryRouter(HttpRouter app) {
        app.get("/api/openlibrary/isbn/{isbn}", ctx -> openLibraryIsbn(ctx));
        app.get("/api/openlibrary/title/{title}", ctx -> openLibraryTitle(ctx));
    }

    private void openLibraryIsbn(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByISBN(ctx.pathParam("isbn")));
    }

    private void openLibraryTitle(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByTitle(ctx.pathParam("title")));
    }
}
