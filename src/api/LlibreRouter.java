package api;

import com.google.gson.JsonObject;
import interficie.BibliotecaWriter;
import domini.Llibre;
import domini.LlibreFilter;
import herramienta.LlibreValidator;

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
            int page;
            try { page = Integer.parseInt(pageStr); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid page: " + pageStr);
            }
            result = new java.util.ArrayList<>(cd.get100Llibres(page));
        } else {
            result = new java.util.ArrayList<>(cd.getAllLlibres());
        }
        if (fieldsParam != null && !fieldsParam.isBlank()) {
            Set<String> fields = new java.util.HashSet<>(java.util.List.of(fieldsParam.split(",")));
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
        if (j == null) throw new IllegalArgumentException("Empty or malformed JSON body");
        Llibre l = JsonMapper.jsonToLlibre(j);
        if (l == null) throw new IllegalArgumentException("Empty or malformed JSON body");
        Llibre validated = validate(l);
        synchronized (cd) { cd.addLlibre(validated); }
        ctx.status(201).json(JsonMapper.llibreToMap(validated));
    }

    private void update(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        if (j == null) throw new IllegalArgumentException("Empty or malformed JSON body");
        Llibre updated = JsonMapper.jsonToLlibre(j);
        if (updated == null) throw new IllegalArgumentException("Empty or malformed JSON body");
        updated.setISBN(isbn);
        synchronized (cd) {
            Llibre existing = cd.getLlibre(isbn);
            updated.setHasBlob(existing.hasBlob());
            cd.updateLlibre(updated);
        }
        ctx.json(JsonMapper.llibreToMap(updated));
    }

    private void delete(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        synchronized (cd) {
            if (!cd.existsLlibre(isbn)) {
                throw new domini.BibliotecaException.NotFound("Book not found: " + isbn);
            }
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
            ctx.contentType(sniffImageMime(blob));
        }
        ctx.responseHeader("Cache-Control", "max-age=3600");
        ctx.result(blob);
    }

    /** Minimal content-type sniff: 8 bytes for PNG signature, 3 for JPEG SOI. */
    public static String sniffImageMime(byte[] blob) {
        if (blob.length >= 8
                && (blob[0] & 0xFF) == 0x89 && blob[1] == 0x50 && blob[2] == 0x4E && blob[3] == 0x47
                && blob[4] == 0x0D && blob[5] == 0x0A && blob[6] == 0x1A && blob[7] == 0x0A) {
            return "image/png";
        }
        if (blob.length >= 3 && (blob[0] & 0xFF) == 0xFF && (blob[1] & 0xFF) == 0xD8 && (blob[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private void uploadImage(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        byte[] bytes = ctx.bodyBytes();
        if (bytes.length == 0) throw new IllegalArgumentException("Empty image body");
        if (bytes.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image too large");
        cd.setLlibreBlob(isbn, bytes);
        try { cd.getLlibre(isbn).setHasBlob(true); } catch (Exception ignored) {}
        ctx.json(Map.of("ok", true));
    }

    private void shelvesForBook(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        var ctxList = cd.getLlistesForLlibreContext(isbn);
        var out = new java.util.ArrayList<Map<String, Object>>();
        for (var lc : ctxList) {
            var m = new java.util.LinkedHashMap<String, Object>();
            m.put("id", lc.llistaId());
            m.put("nom", lc.nom());
            m.put("ordre", lc.ordre());
            m.put("color", lc.color());
            m.put("valoracio", lc.valoracio());
            m.put("llegit", lc.llegit());
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

    private static Llibre validate(Llibre l) {
        // validates required fields (throws IllegalArgumentException on error)
        // copyOf() carries all fields, so adding new fields to Llibre doesn't silently drop them here
        LlibreValidator.checkLlibre(
            l.getISBN(), l.getNom(), l.getAutor(), l.getAny(), l.getDescripcio(),
            l.getValoracio(), l.getPreu(), l.getLlegit(), l.getImatge());
        return domini.Llibre.copyOf(l);
    }

    private static LlibreFilter buildFilter(HttpCtx ctx) {
        domini.LlibreFilterBuilder b = domini.LlibreFilterBuilder.of();
        String isbnStr = ctx.queryParamOrNull("isbn");
        if (isbnStr != null) {
            try { b.isbn(Long.parseLong(isbnStr)); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid isbn query param: " + isbnStr);
            }
        }
        b.autor(ctx.queryParamOrNull("author"));
        b.nom(ctx.queryParamOrNull("title"));
        b.anyMin(ctx.queryParamInt("yearMin"));
        b.anyMax(ctx.queryParamInt("yearMax"));
        b.valoracioMin(ctx.queryParamDbl("ratingMin"));
        b.valoracioMax(ctx.queryParamDbl("ratingMax"));
        b.preuMin(ctx.queryParamDbl("priceMin"));
        b.preuMax(ctx.queryParamDbl("priceMax"));
        b.llegit(ctx.queryParamBool("read"));
        b.tagId(ctx.queryParamInt("tagId"));
        b.llistaId(ctx.queryParamInt("llistaId"));
        b.editorial(ctx.queryParamOrNull("editorial"));
        b.serie(ctx.queryParamOrNull("serie"));
        b.format(ctx.queryParamOrNull("format"));
        b.idioma(ctx.queryParamOrNull("idioma"));
        String sortCol = ctx.queryParamOrNull("sort");
        String sortDir = ctx.queryParam("sortDir");
        if (sortCol != null) b.sort(sortCol, !"desc".equalsIgnoreCase(sortDir));
        return b.build();
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
