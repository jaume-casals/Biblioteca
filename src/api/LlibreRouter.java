package api;

import com.google.gson.JsonObject;
import interficie.BibliotecaWriter;
import domini.Llibre;
import domini.LlibreFilter;
import herramienta.LlibreValidator;
import herramienta.OpenLibraryClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LlibreRouter {

    private final BibliotecaWriter cd;

    public LlibreRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        // Specific paths before parameterised ones
        app.get("/api/books/count",             ctx -> count(ctx));
        app.get("/api/books/recent",            ctx -> recent(ctx));
        app.get("/api/books/{isbn}/image",      ctx -> image(ctx));
        app.post("/api/books/{isbn}/image",     ctx -> uploadImage(ctx));
        app.get("/api/books/{isbn}/shelves",    ctx -> shelvesForBook(ctx));
        app.get("/api/books/{isbn}/tags",       ctx -> tagsForBook(ctx));
        app.get("/api/books/{isbn}",            ctx -> getOne(ctx));
        app.get("/api/books",                   ctx -> list(ctx));
        app.post("/api/books",                  ctx -> add(ctx));
        app.put("/api/books/{isbn}",            ctx -> update(ctx));
        app.delete("/api/books/{isbn}",         ctx -> delete(ctx));
        app.get("/api/openlibrary/isbn/{isbn}", ctx -> openLibraryIsbn(ctx));
        app.get("/api/openlibrary/title/{title}", ctx -> openLibraryTitle(ctx));
    }

    private void count(HttpCtx ctx) {
        ctx.json(Map.of("total", cd.getSize(), "pages", cd.maxIndex100Llibres()));
    }

    private void recent(HttpCtx ctx) {
        ctx.json(JsonMapper.llibresToList(new java.util.ArrayList<>(cd.getRecentlyAdded())));
    }

    private void list(HttpCtx ctx) throws Exception {
        String pageStr = ctx.queryParam("page");
        String fieldsParam = ctx.queryParam("fields");

        LlibreFilter f = buildFilter(ctx);

        ArrayList<Llibre> result;
        if (f.hasAnyFilter()) {
            result = new java.util.ArrayList<>(cd.aplicarFiltres(f));
        } else if (notBlank(pageStr)) {
            int page = 0;
            try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
            result = new java.util.ArrayList<>(cd.get100Llibres(page));
        } else {
            result = new java.util.ArrayList<>(cd.getAllLlibres());
        }
        if (fieldsParam != null && !fieldsParam.isBlank()) {
            Set<String> fields = Set.of(fieldsParam.split(","));
            ctx.json(JsonMapper.llibresToSlimList(result, fields));
        } else {
            ctx.json(JsonMapper.llibresToList(result));
        }
    }

    private void getOne(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        ctx.json(JsonMapper.llibreToMap(cd.getLlibre(isbn)));
    }

    private void add(HttpCtx ctx) throws Exception {
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        Llibre l = JsonMapper.jsonToLlibre(j);
        Llibre validated = validate(l);
        synchronized (cd) { cd.addLlibre(validated); }
        ctx.status(201).json(JsonMapper.llibreToMap(validated));
    }

    private void update(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        Llibre updated = JsonMapper.jsonToLlibre(j);
        updated.setISBN(isbn);
        synchronized (cd) {
            Llibre existing = cd.getLlibre(isbn);
            if (existing == null) throw new IllegalArgumentException("Book not found: " + isbn);
            updated.setHasBlob(existing.hasBlob());
            cd.updateLlibre(updated);
        }
        ctx.json(JsonMapper.llibreToMap(updated));
    }

    private void delete(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        synchronized (cd) {
            if (cd.getLlibre(isbn) == null) throw new IllegalArgumentException("Book not found: " + isbn);
            cd.deleteLlibre(isbn);
        }
        ctx.status(204);
    }

    private void image(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        byte[] blob = cd.getLlibreBlob(isbn);
        if (blob == null || blob.length == 0) {
            try (var stream = getClass().getResourceAsStream("/web/img/default-cover.png")) {
                if (stream == null) { ctx.status(404); return; }
                blob = stream.readAllBytes();
            }
            if (blob == null || blob.length == 0) { ctx.status(404); return; }
            ctx.contentType("image/png");
        } else {
            boolean isPng = blob.length > 4 && blob[0] == (byte) 0x89 && blob[1] == 0x50;
            ctx.contentType(isPng ? "image/png" : "image/jpeg");
        }
        ctx.responseHeader("Cache-Control", "max-age=3600");
        ctx.result(blob);
    }

    private void uploadImage(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        byte[] bytes = ctx.bodyBytes();
        if (bytes.length == 0) throw new Exception("Empty image body");
        cd.setLlibreBlob(isbn, bytes);
        try { cd.getLlibre(isbn).setHasBlob(true); } catch (Exception ignored) {}
        ctx.json(Map.of("ok", true));
    }

    private void shelvesForBook(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        var llistes = cd.getLlistesForLlibre(isbn);
        var out = new java.util.ArrayList<Map<String, Object>>();
        for (var l : llistes) {
            var m = JsonMapper.llistaToMap(l);
            m.put("valoracio", l.getValoracioLlibre());
            m.put("llegit", l.getLlegitLlibre());
            out.add(m);
        }
        ctx.json(out);
    }

    private void tagsForBook(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        var tags = cd.getTagsForLlibre(isbn);
        var out = new java.util.ArrayList<Map<String, Object>>();
        for (var t : tags) out.add(JsonMapper.tagToMap(t));
        ctx.json(out);
    }

    // TODO: if OpenLibrary lookup grows to more endpoints, extract to a dedicated OpenLibraryRouter
    private void openLibraryIsbn(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByISBN(ctx.pathParam("isbn")));
    }

    private void openLibraryTitle(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByTitle(ctx.pathParam("title")));
    }

    private static Llibre validate(Llibre l) {
        // validates required fields (throws IllegalArgumentException on error)
        // copyOf() carries all fields, so adding new fields to Llibre doesn't silently drop them here
        LlibreValidator.checkLlibre(
            l.getISBN(), l.getNom(), l.getAutor(), l.getAny(), l.getDescripcio(),
            l.getValoracio(), l.getPreu(), l.getLlegit(), l.getImatge());
        return domini.Llibre.copyOf(l);
    }

    private static LlibreFilter buildFilter(HttpCtx ctx) {
        LlibreFilter f = LlibreFilter.empty();
        String isbnStr = ctx.queryParamOrNull("isbn");
        if (isbnStr != null) { try { f.isbn = Long.parseLong(isbnStr); } catch (NumberFormatException ignored) {} }
        f.autor        = ctx.queryParamOrNull("author");
        f.nom          = ctx.queryParamOrNull("title");
        f.anyMin       = ctx.queryParamInt("yearMin");
        f.anyMax       = ctx.queryParamInt("yearMax");
        f.valoracioMin = ctx.queryParamDbl("ratingMin");
        f.valoracioMax = ctx.queryParamDbl("ratingMax");
        f.preuMin      = ctx.queryParamDbl("priceMin");
        f.preuMax      = ctx.queryParamDbl("priceMax");
        f.llegit       = ctx.queryParamBool("read");
        f.tagId        = ctx.queryParamInt("tagId");
        f.llistaId     = ctx.queryParamInt("llistaId");
        f.editorial    = ctx.queryParamOrNull("editorial");
        f.serie        = ctx.queryParamOrNull("serie");
        f.format       = ctx.queryParamOrNull("format");
        f.idioma       = ctx.queryParamOrNull("idioma");
        f.sortColumn   = ctx.queryParamOrNull("sort");
        String sortDir = ctx.queryParam("sortDir");
        if (sortDir != null) f.sortAsc = !"desc".equalsIgnoreCase(sortDir);
        return f;
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
