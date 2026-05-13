package api;

import com.google.gson.JsonObject;
import domini.ControladorDomini;

import java.util.Map;

public class LoanRouter {

    private final ControladorDomini cd = ControladorDomini.getInstance();

    public LoanRouter(HttpRouter app) {
        app.get("/api/loans",            ctx -> getAll(ctx));
        app.post("/api/loans/{isbn}",    ctx -> loan(ctx));
        app.delete("/api/loans/{isbn}",  ctx -> returnBook(ctx));
    }

    private void getAll(HttpCtx ctx) {
        ctx.json(Map.of("loaned", cd.getLoanedISBNs()));
    }

    private void loan(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        String persona = j.has("persona") ? j.get("persona").getAsString() : "";
        if (persona.isBlank()) throw new Exception("Borrower name required");
        synchronized (cd) { cd.prestarLlibre(isbn, persona); }
        ctx.status(201).json(Map.of("ok", true));
    }

    private void returnBook(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        synchronized (cd) { cd.retornarLlibre(isbn); }
        ctx.status(204);
    }
}
