package api;

import interficie.BibliotecaWriter;

/**
 * Common helpers for routers that toggle a many-to-many membership of a book to either a shelf
 * (LlistaRouter) or a tag (TagRouter). Reduces duplication in addToBook/removeFromBook handlers.
 */
final class MembershipRouter {
    private MembershipRouter() {}

    @FunctionalInterface interface AddOp { void run(long isbn, int parentId) throws Exception; }
    @FunctionalInterface interface RemoveOp { void run(long isbn, int parentId) throws Exception; }

    static void addBookTo(HttpCtx ctx, BibliotecaWriter cd, String parentParam, AddOp op) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        int parentId = ctx.pathParamInt(parentParam);
        if (!cd.existsLlibre(isbn)) { ctx.status(404).json(java.util.Map.of("error", "Book not found")); return; }
        synchronized (cd) { op.run(isbn, parentId); }
        ctx.status(201).json(java.util.Map.of("ok", true));
    }

    static void removeBookFrom(HttpCtx ctx, BibliotecaWriter cd, String parentParam, RemoveOp op) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        int parentId = ctx.pathParamInt(parentParam);
        synchronized (cd) { op.run(isbn, parentId); }
        ctx.status(204);
    }
}
