package api;

import com.google.gson.JsonObject;
import interficie.BibliotecaWriter;
import domini.Tag;

import java.util.Map;

public class TagRouter {

    private final BibliotecaWriter cd;

    public TagRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        app.get("/api/tags",                          ctx -> getAll(ctx));
        app.post("/api/tags",                         ctx -> create(ctx));
        app.put("/api/tags/{id}",                     ctx -> rename(ctx));
        app.delete("/api/tags/{id}",                  ctx -> delete(ctx));
        app.post("/api/books/{isbn}/tags/{id}",       ctx -> addToBook(ctx));
        app.delete("/api/books/{isbn}/tags/{id}",     ctx -> removeFromBook(ctx));
    }

    private void getAll(HttpCtx ctx) {
        ctx.json(cd.getAllTags().stream().map(JsonMapper::tagToMap).collect(java.util.stream.Collectors.toList()));
    }

    private void create(HttpCtx ctx) throws Exception {
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        if (!j.has("nom") || j.get("nom").isJsonNull()) throw new Exception("Field 'nom' is required");
        Tag created;
        synchronized (cd) { created = cd.addTag(j.get("nom").getAsString()); }
        ctx.status(201).json(JsonMapper.tagToMap(created));
    }

    private void rename(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        if (!j.has("nom") || j.get("nom").isJsonNull()) throw new Exception("Field 'nom' is required");
        Tag target = findById(id);
        synchronized (cd) { cd.renameTag(id, j.get("nom").getAsString()); }
        ctx.json(JsonMapper.tagToMap(findById(id)));
    }

    private void delete(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        Tag target = findById(id);
        synchronized (cd) { cd.deleteTag(target); }
        ctx.status(204);
    }

    private void addToBook(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        int id    = ctx.pathParamInt("id");
        if (!cd.existsLlibre(isbn)) { ctx.status(404).json(Map.of("error", "Book not found")); return; }
        synchronized (cd) { cd.addLlibreToTag(isbn, id); }
        ctx.status(201).json(Map.of("ok", true));
    }

    private void removeFromBook(HttpCtx ctx) throws Exception {
        long isbn = ctx.pathParamLong("isbn");
        int id    = ctx.pathParamInt("id");
        synchronized (cd) { cd.removeLlibreFromTag(isbn, id); }
        ctx.status(204);
    }

    private Tag findById(int id) throws Exception {
        for (Tag t : cd.getAllTags()) if (t.getId() == id) return t;
        throw new Exception("Tag not found: " + id);
    }
}
