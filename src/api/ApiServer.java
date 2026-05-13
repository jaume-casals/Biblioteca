package api;

import java.util.Map;

public class ApiServer {

    private final HttpRouter router;
    private final int port;

    public ApiServer(int port) throws Exception {
        this.port = port;
        this.router = new HttpRouter();

        router.exception(ctx -> {
            Exception e = ctx.getException();
            String msg = e != null && e.getMessage() != null ? e.getMessage() : "Internal error";
            ctx.status(400).json(Map.of("error", msg));
        });

        new LlibreRouter(router);
        new LlistaRouter(router);
        new TagRouter(router);
        new LoanRouter(router);
        new BackupRouter(router);
        new ConfigRouter(router);
        new MetaRouter(router);
        new ImportExportRouter(router);
    }

    public void start() throws Exception {
        router.start(port);
    }

    public void stop() {
        router.stop();
    }
}
