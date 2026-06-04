package api;

import com.google.gson.JsonSyntaxException;
import domini.BibliotecaException;
import interficie.BibliotecaWriter;

import java.util.Map;

public class ApiServer {

    private final HttpRouter router;
    private final int port;

    public ApiServer(int port, BibliotecaWriter cd) {
        this.port = port;
        this.router = new HttpRouter();
        ApiAuth.registerRoutes(router);

        router.exception(ctx -> {
            Exception e = ctx.getException();
            if (e == null) { ctx.status(500).json(Map.of("error", "Internal error")); return; }
            StatusBody sb = classify(e);
            ctx.status(sb.status).json(Map.of("error", sb.body));
        });

        new LlibreRouter(router, cd);
        new OpenLibraryRouter(router);
        new LlistaRouter(router, cd);
        new TagRouter(router, cd);
        new LoanRouter(router, cd);
        new BackupRouter(router, cd);
        new ConfigRouter(router);
        new MetaRouter(router, cd);
        new ImportExportRouter(router, cd);
    }

    private record StatusBody(int status, String body) {}

    /**
     * Map domain/library exceptions to HTTP statuses. <b>Body messages are
     * sanitised</b> — only the public-facing constant string is returned for
     * NotFound/Unknown; for Validation/Duplicate the local message is safe
     * (catalan/spanish I18n keys) and useful for the caller.
     */
    private static StatusBody classify(Throwable t) {
        while (t != null) {
            if (t instanceof BibliotecaException be) {
                return switch (be.code()) {
                    case NOT_FOUND  -> new StatusBody(404, "Not found");
                    case DUPLICATE  -> new StatusBody(409, be.getMessage() != null ? be.getMessage() : "Duplicate");
                    case VALIDATION -> new StatusBody(400, be.getMessage() != null ? be.getMessage() : "Validation failed");
                    case UNKNOWN    -> new StatusBody(500, "Internal error");
                };
            }
            if (t instanceof ApiAuth.UnauthorizedException) {
                return new StatusBody(401, "Unauthorized");
            }
            if (t instanceof OpenLibraryRouter.OpenLibraryUpstreamException) {
                return new StatusBody(502, "Upstream service error");
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException
                    || t instanceof JsonSyntaxException || t instanceof IllegalStateException) {
                return new StatusBody(400, t.getMessage() != null ? t.getMessage() : "Bad request");
            }
            t = t.getCause();
        }
        return new StatusBody(500, "Internal error");
    }

    public void start() throws Exception {
        router.start(port);
    }

    public void stop() {
        router.stop();
    }
}