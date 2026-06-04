package api;

import herramienta.OpenLibraryClient;

import java.util.Map;

public class OpenLibraryRouter {

    public OpenLibraryRouter(HttpRouter app) {
        app.get("/api/openlibrary/isbn/{isbn}", ctx -> openLibraryIsbn(ctx));
        app.get("/api/openlibrary/title/{title}", ctx -> openLibraryTitle(ctx));
    }

    private void openLibraryIsbn(HttpCtx ctx) throws Exception {
        String isbn = ctx.pathParam("isbn");
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("ISBN is required");
        try {
            ctx.json(OpenLibraryClient.lookupByISBN(isbn));
        } catch (Exception e) {
            ctx.status(502).json(Map.of("error", "OpenLibrary request failed", "detail", e.getMessage()));
        }
    }

    private void openLibraryTitle(HttpCtx ctx) throws Exception {
        String title = ctx.pathParam("title");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        try {
            ctx.json(OpenLibraryClient.lookupByTitle(title));
        } catch (Exception e) {
            ctx.status(502).json(Map.of("error", "OpenLibrary request failed", "detail", e.getMessage()));
        }
    }
}