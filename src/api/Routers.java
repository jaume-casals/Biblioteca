package api;

import interficie.BibliotecaWriter;

final class Routers {
    private Routers() {}

    static void registerAll(HttpRouter router, BibliotecaWriter cd) {
        new LlibreRouter(router, cd);
        new LlistaRouter(router, cd);
        new TagRouter(router, cd);
        new LoanRouter(router, cd);
        new BackupRouter(router, cd);
        new ConfigRouter(router);
        new MetaRouter(router, cd);
        new MetaLookupRouter(router);
        new ImportExportRouter(router, cd);
    }
}
