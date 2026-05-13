package api;

import com.google.gson.JsonObject;
import domini.ControladorDomini;
import domini.Llibre;
import herramienta.LlibreValidator;
import herramienta.OpenLibraryClient;
import persistencia.ControladorPersistencia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LlibreRouter {

    private final ControladorDomini cd = ControladorDomini.getInstance();
    private final ControladorPersistencia cp = ControladorPersistencia.getInstance();

    public LlibreRouter(HttpRouter app) {
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
        ctx.json(JsonMapper.llibresToList(cd.getRecentlyAdded()));
    }

    private void list(HttpCtx ctx) throws Exception {
        String title   = ctx.queryParam("title");
        String author  = ctx.queryParam("author");
        String isbnStr = ctx.queryParam("isbn");
        String pageStr = ctx.queryParam("page");

        Long isbnFilter = null;
        if (isbnStr != null && !isbnStr.isBlank()) {
            try { isbnFilter = Long.parseLong(isbnStr); } catch (NumberFormatException ignored) {}
        }

        boolean hasFilter = notBlank(author) || notBlank(title) || isbnFilter != null
            || ctx.queryParam("yearMin") != null || ctx.queryParam("ratingMin") != null
            || ctx.queryParam("priceMin") != null || ctx.queryParam("read") != null
            || ctx.queryParam("tagId") != null || ctx.queryParam("editorial") != null
            || ctx.queryParam("serie") != null || ctx.queryParam("format") != null
            || ctx.queryParam("idioma") != null;

        ArrayList<Llibre> result;
        if (hasFilter) {
            result = cd.aplicarFiltres(
                author, title, isbnFilter,
                parseInt(ctx.queryParam("yearMin")),
                parseInt(ctx.queryParam("yearMax")),
                parseDbl(ctx.queryParam("ratingMin")),
                parseDbl(ctx.queryParam("ratingMax")),
                parseDbl(ctx.queryParam("priceMin")),
                parseDbl(ctx.queryParam("priceMax")),
                parseBool(ctx.queryParam("read")),
                parseInt(ctx.queryParam("tagId")),
                ctx.queryParam("editorial"),
                ctx.queryParam("serie"),
                ctx.queryParam("format"),
                ctx.queryParam("idioma")
            );
        } else if (notBlank(pageStr)) {
            int page = 0;
            try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
            result = cd.get100Llibres(page);
        } else {
            result = cd.getAllLlibres();
        }
        System.out.println("[/books] hasFilter=" + hasFilter + " page=" + pageStr + " result.size=" + result.size());
        ctx.json(JsonMapper.llibresToList(result));
    }

    private void getOne(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
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
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        Llibre existing = cd.getLlibre(isbn);
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        Llibre updated = JsonMapper.jsonToLlibre(j);
        updated.setISBN(isbn);
        updated.setHasBlob(existing.hasBlob());
        synchronized (cd) { cd.updateLlibre(updated); }
        ctx.json(JsonMapper.llibreToMap(updated));
    }

    private void delete(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        synchronized (cd) { cd.deleteLlibre(isbn); }
        ctx.status(204);
    }

    private void image(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
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
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        byte[] bytes = ctx.bodyBytes();
        if (bytes.length == 0) throw new Exception("Empty image body");
        cp.setLlibreBlob(isbn, bytes);
        try { cd.getLlibre(isbn).setHasBlob(true); } catch (Exception ignored) {}
        ctx.json(Map.of("ok", true));
    }

    private void shelvesForBook(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
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
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        var tags = cd.getTagsForLlibre(isbn);
        var out = new java.util.ArrayList<Map<String, Object>>();
        for (var t : tags) out.add(JsonMapper.tagToMap(t));
        ctx.json(out);
    }

    private void openLibraryIsbn(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByISBN(ctx.pathParam("isbn")));
    }

    private void openLibraryTitle(HttpCtx ctx) throws Exception {
        ctx.json(OpenLibraryClient.lookupByTitle(ctx.pathParam("title")));
    }

    private static Llibre validate(Llibre l) {
        Llibre v = LlibreValidator.checkLlibre(
            l.getISBN(), l.getNom(), l.getAutor(), l.getAny(), l.getDescripcio(),
            l.getValoracio(), l.getPreu(), l.getLlegit(), l.getImatge());
        v.setNotes(l.getNotes()); v.setPagines(l.getPagines());
        v.setPaginesLlegides(l.getPaginesLlegides()); v.setEditorial(l.getEditorial());
        v.setSerie(l.getSerie()); v.setVolum(l.getVolum());
        v.setDataCompra(l.getDataCompra()); v.setDataLectura(l.getDataLectura());
        v.setIdioma(l.getIdioma()); v.setFormat(l.getFormat());
        v.setDesitjat(l.getDesitjat()); v.setPaisOrigen(l.getPaisOrigen());
        if (!l.getAutors().isEmpty()) v.setAutors(l.getAutors());
        return v;
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static Integer parseInt(String s) { if (!notBlank(s)) return null; try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; } }
    private static Double  parseDbl(String s)  { if (!notBlank(s)) return null; try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; } }
    private static Boolean parseBool(String s) { if (!notBlank(s)) return null; return Boolean.parseBoolean(s); }
}
