package api;

import interficie.BibliotecaWriter;

import java.util.Map;

public class ApiServer {

    private final HttpRouter router;
    private final int port;

    public ApiServer(int port, BibliotecaWriter cd) {
        this.port = port;
        this.router = new HttpRouter();

        router.exception(ctx -> {
            Exception e = ctx.getException();
            if (e == null) { ctx.status(500).json(Map.of("error", "Internal error")); return; }
            String msg = e.getMessage() != null ? e.getMessage() : "Bad request";
            ctx.status(isBadInput(e) ? 400 : 500).json(Map.of("error", msg));
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

    private static boolean isBadInput(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) return true;
            t = t.getCause();
        }
        return false;
    }

    public void start() throws Exception {
        router.start(port);
    }

    public void stop() {
        router.stop();
    }
}