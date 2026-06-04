package api;

import herramienta.OpenLibraryClient;

import java.util.Map;

public class OpenLibraryRouter {

    public OpenLibraryRouter(HttpRouter app) {
        app.get("/api/openlibrary/isbn/{isbn}", ctx -> openLibraryIsbn(ctx));
        app.get("/api/openlibrary/title/{title}", ctx -> openLibraryTitle(ctx));
    }

    private void openLibraryIsbn(HttpCtx ctx) throws Exception {
        String isbn = ctx.pathParam("isbn").trim();
        if (isbn.isEmpty()) throw new IllegalArgumentException("ISBN is required");
        try {
            ctx.json(OpenLibraryClient.lookupByISBN(isbn));
        } catch (Exception e) {
            throw new OpenLibraryUpstreamException(e);
        }
    }

    private void openLibraryTitle(HttpCtx ctx) throws Exception {
        String title = ctx.pathParam("title").trim();
        if (title.isEmpty()) throw new IllegalArgumentException("Title is required");
        try {
            ctx.json(OpenLibraryClient.lookupByTitle(title));
        } catch (Exception e) {
            throw new OpenLibraryUpstreamException(e);
        }
    }

    /** Mapped to HTTP 502 in {@link ApiServer#classify}. */
    static final class OpenLibraryUpstreamException extends RuntimeException {
        OpenLibraryUpstreamException(Throwable cause) {
            super("Open Library request failed", cause);
        }
    }
}
