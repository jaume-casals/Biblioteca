package api;

import com.google.gson.JsonObject;
import domini.BibliotecaException;
import interficie.BibliotecaWriter;

import java.util.Map;

public class LoanRouter {

    private final BibliotecaWriter cd;

    public LoanRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        app.get("/api/loans",            ctx -> getAll(ctx));
        app.get("/api/loans/{isbn}",     ctx -> getForIsbn(ctx));
        app.post("/api/loans/{isbn}",    ctx -> loan(ctx));
        app.delete("/api/loans/{isbn}",  ctx -> returnBook(ctx));
    }

    private void getAll(HttpCtx ctx) {
        var loans = cd.getAllActiveLoans();
        var out = new java.util.ArrayList<Map<String, Object>>(loans.size());
        for (var r : loans) {
            out.add(Map.of("isbn", r.isbn(), "persona", r.nomPersona(), "dataPrestec", r.dataPrestec() != null ? r.dataPrestec().toString() : null));
        }
        ctx.json(Map.of("loans", out));
    }

    private void getForIsbn(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        var loans = cd.getLoansForIsbn(isbn);
        var out = new java.util.ArrayList<Map<String, Object>>(loans.size());
        for (var r : loans) {
            out.add(Map.of("persona", r.nomPersona(), "dataPrestec", r.dataPrestec() != null ? r.dataPrestec().toString() : null, "retornat", r.retornat()));
        }
        ctx.json(Map.of("isbn", isbn, "loans", out));
    }

    private void loan(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        if (j == null) throw new IllegalArgumentException("Empty or malformed JSON body");
        String persona = j.has("persona") ? j.get("persona").getAsString() : "";
        if (persona.isBlank()) throw new IllegalArgumentException("Borrower name required");
        synchronized (cd) {
            var loaned = cd.getLoanedISBNs();
            if (loaned.contains(isbn)) throw new BibliotecaException.Duplicate("Book already on loan");
            cd.prestarLlibre(isbn, persona);
        }
        ctx.status(201).json(Map.of("ok", true));
    }

    private void returnBook(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        synchronized (cd) {
            // Cache loaned ISBNs to avoid querying the DB multiple times per request
            var loaned = cd.getLoanedISBNs();
            if (!loaned.contains(isbn)) {
                ctx.status(404).json(Map.of("error", "Book is not on loan"));
                return;
            }
            cd.retornarLlibre(isbn);
        }
        ctx.status(204);
    }
}
