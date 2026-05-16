package api;

import interficie.BibliotecaWriter;

import java.util.Map;

public class ApiServer {

    private final HttpRouter router;
    private final int port;

    // All routes are registered eagerly at construction time. Call start() after construction.
    public ApiServer(int port, BibliotecaWriter cd) {
        this.port = port;
        this.router = new HttpRouter();

        router.exception(ctx -> {
            Exception e = ctx.getException();
            String msg = e != null && e.getMessage() != null ? e.getMessage() : "Internal error";
            ctx.status(400).json(Map.of("error", msg));
        });

        new LlibreRouter(router, cd);
        new LlistaRouter(router, cd);
        new TagRouter(router, cd);
        new LoanRouter(router, cd);
        new BackupRouter(router, cd);
        new ConfigRouter(router);
        new MetaRouter(router, cd);
        new ImportExportRouter(router, cd);
    }

    public void start() throws Exception {
        router.start(port);
    }

    public void stop() {
        router.stop();
    }
}
