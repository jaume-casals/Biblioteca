package api;

import com.google.gson.JsonObject;
import interficie.BibliotecaWriter;
import domini.Llibre;
import domini.Llista;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class LlistaRouter {

    private final BibliotecaWriter cd;

    public LlistaRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        app.get("/api/shelves",                          ctx -> getAll(ctx));
        app.post("/api/shelves",                         ctx -> create(ctx));
        app.delete("/api/shelves/{id}",                  ctx -> delete(ctx));
        app.put("/api/shelves/{id}",                     ctx -> rename(ctx));
        app.put("/api/shelves/{id}/color",               ctx -> setColor(ctx));
        app.post("/api/shelves/{id}/up",                 ctx -> moveUp(ctx));
        app.post("/api/shelves/{id}/down",               ctx -> moveDown(ctx));
        app.get("/api/shelves/{id}/books",               ctx -> books(ctx));
        app.post("/api/shelves/{id}/books/{isbn}",       ctx -> addBook(ctx));
        app.put("/api/shelves/{id}/books/{isbn}",        ctx -> updateBook(ctx));
        app.delete("/api/shelves/{id}/books/{isbn}",     ctx -> removeBook(ctx));
    }

    private void getAll(HttpCtx ctx) {
        var llistes = cd.getAllLlistes();
        var counts = cd.getAllCountsInLlistes();
        var out = new ArrayList<Map<String, Object>>();
        for (var l : llistes) {
            var m = JsonMapper.llistaToMap(l);
            m.put("count", counts.getOrDefault(l.getId(), 0));
            out.add(m);
        }
        ctx.json(out);
    }

    private void create(HttpCtx ctx) throws Exception {
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        if (j == null || !j.has("nom") || j.get("nom").isJsonNull()) throw new IllegalArgumentException("Field 'nom' is required");
        String nom = j.get("nom").getAsString();
        Llista created;
        synchronized (cd) { created = cd.addLlista(nom); }
        ctx.status(201).json(JsonMapper.llistaToMap(created));
    }

    private void delete(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        synchronized (cd) { cd.deleteLlista(cd.getLlistaById(id)); }
        ctx.status(204);
    }

    private void rename(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        if (j == null || !j.has("nom") || j.get("nom").isJsonNull()) throw new IllegalArgumentException("Field 'nom' is required");
        String nom = j.get("nom").getAsString();
        synchronized (cd) { cd.renameLlista(id, nom); }
        ctx.json(JsonMapper.llistaToMap(cd.getLlistaById(id)));
    }

    private void setColor(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        String color = j.has("color") && !j.get("color").isJsonNull() ? j.get("color").getAsString() : null;
        synchronized (cd) { cd.setLlistaColor(id, color); }
        ctx.json(Map.of("ok", true));
    }

    private void moveUp(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        cd.getLlistaById(id);
        synchronized (cd) { cd.moveLlistaUp(id); }
        ctx.json(Map.of("ok", true));
    }

    private void moveDown(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        cd.getLlistaById(id);
        synchronized (cd) { cd.moveLlistaDown(id); }
        ctx.json(Map.of("ok", true));
    }

    private void books(HttpCtx ctx) throws Exception {
        int id = ctx.pathParamInt("id");
        cd.getLlistaById(id);
        String fieldsParam = ctx.queryParam("fields");
        ArrayList<Llibre> books = new java.util.ArrayList<>(cd.getLlibresInLlista(id));
        if (fieldsParam != null && !fieldsParam.isBlank()) {
            Set<String> fields = new java.util.HashSet<>(java.util.List.of(fieldsParam.split(",")));
            ctx.json(JsonMapper.llibresToSlimList(books, fields));
        } else {
            ctx.json(JsonMapper.llibresToList(books));
        }
    }

    private void addBook(HttpCtx ctx) throws Exception {
        int id    = ctx.pathParamInt("id");
        long isbn = ctx.pathParamLong("isbn");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        double valoracio = j.has("valoracio") ? j.get("valoracio").getAsDouble() : 0.0;
        boolean llegit   = j.has("llegit") && j.get("llegit").getAsBoolean();
        synchronized (cd) { cd.addLlibreToLlista(isbn, id, valoracio, llegit); }
        ctx.status(201).json(Map.of("ok", true));
    }

    private void updateBook(HttpCtx ctx) throws Exception {
        int id    = ctx.pathParamInt("id");
        long isbn = ctx.pathParamLong("isbn");
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        double valoracio = j.has("valoracio") ? j.get("valoracio").getAsDouble() : 0.0;
        boolean llegit   = j.has("llegit") && j.get("llegit").getAsBoolean();
        synchronized (cd) { cd.updateLlibreInLlista(isbn, id, valoracio, llegit); }
        ctx.json(Map.of("ok", true));
    }

    private void removeBook(HttpCtx ctx) throws Exception {
        int id    = ctx.pathParamInt("id");
        long isbn = ctx.pathParamLong("isbn");
        synchronized (cd) { cd.removeLlibreFromLlista(isbn, id); }
        ctx.status(204);
    }
}
