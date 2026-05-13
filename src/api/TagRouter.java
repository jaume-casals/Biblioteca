package api;

import com.google.gson.JsonObject;
import domini.ControladorDomini;
import domini.Tag;

import java.util.ArrayList;
import java.util.Map;

public class TagRouter {

    private final ControladorDomini cd = ControladorDomini.getInstance();

    public TagRouter(HttpRouter app) {
        app.get("/api/tags",                          ctx -> getAll(ctx));
        app.post("/api/tags",                         ctx -> create(ctx));
        app.delete("/api/tags/{id}",                  ctx -> delete(ctx));
        app.post("/api/books/{isbn}/tags/{id}",       ctx -> addToBook(ctx));
        app.delete("/api/books/{isbn}/tags/{id}",     ctx -> removeFromBook(ctx));
    }

    private void getAll(HttpCtx ctx) {
        var tags = cd.getAllTags();
        var out = new ArrayList<Map<String, Object>>();
        for (var t : tags) out.add(JsonMapper.tagToMap(t));
        ctx.json(out);
    }

    private void create(HttpCtx ctx) throws Exception {
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        Tag created;
        synchronized (cd) { created = cd.addTag(j.get("nom").getAsString()); }
        ctx.status(201).json(JsonMapper.tagToMap(created));
    }

    private void delete(HttpCtx ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Tag target = findById(id);
        synchronized (cd) { cd.deleteTag(target); }
        ctx.status(204);
    }

    private void addToBook(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        int id    = Integer.parseInt(ctx.pathParam("id"));
        synchronized (cd) { cd.addLlibreToTag(isbn, id); }
        ctx.status(201).json(Map.of("ok", true));
    }

    private void removeFromBook(HttpCtx ctx) throws Exception {
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        int id    = Integer.parseInt(ctx.pathParam("id"));
        synchronized (cd) { cd.removeLlibreFromTag(isbn, id); }
        ctx.status(204);
    }

    private Tag findById(int id) throws Exception {
        for (Tag t : cd.getAllTags()) if (t.getId() == id) return t;
        throw new Exception("Tag not found: " + id);
    }
}
