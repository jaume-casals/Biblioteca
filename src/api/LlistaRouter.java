package api;

import com.google.gson.JsonObject;
import interficie.BibliotecaWriter;
import domini.Llista;

import java.util.ArrayList;
import java.util.Map;

public class LlistaRouter {

    private final BibliotecaWriter cd;

    public LlistaRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        app.get("/api/shelves",                          ctx -> getAll(ctx));
        app.post("/api/shelves",                         ctx -> create(ctx));
        app.delete("/api/shelves/{id}",                  ctx -> delete(ctx));
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
        var out = new ArrayList<Map<String, Object>>();
        for (var l : llistes) {
            var m = JsonMapper.llistaToMap(l);
            m.put("count", cd.getCountInLlista(l.getId()));
            out.add(m);
        }
        ctx.json(out);
    }

    private void create(HttpCtx ctx) throws Exception {
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        String nom = j.get("nom").getAsString();
        Llista created;
        synchronized (cd) { created = cd.addLlista(nom); }
        ctx.status(201).json(JsonMapper.llistaToMap(created));
    }

    private void delete(HttpCtx ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        synchronized (cd) { cd.deleteLlista(findById(id)); }
        ctx.status(204);
    }

    private void setColor(HttpCtx ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        String color = j.has("color") && !j.get("color").isJsonNull() ? j.get("color").getAsString() : null;
        synchronized (cd) { cd.setLlistaColor(id, color); }
        ctx.json(Map.of("ok", true));
    }

    private void moveUp(HttpCtx ctx) throws Exception {
        synchronized (cd) { cd.moveLlistaUp(Integer.parseInt(ctx.pathParam("id"))); }
        ctx.json(Map.of("ok", true));
    }

    private void moveDown(HttpCtx ctx) throws Exception {
        synchronized (cd) { cd.moveLlistaDown(Integer.parseInt(ctx.pathParam("id"))); }
        ctx.json(Map.of("ok", true));
    }

    private void books(HttpCtx ctx) {
        ctx.json(JsonMapper.llibresToList(cd.getLlibresInLlista(Integer.parseInt(ctx.pathParam("id")))));
    }

    private void addBook(HttpCtx ctx) throws Exception {
        int id    = Integer.parseInt(ctx.pathParam("id"));
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        double valoracio = j.has("valoracio") ? j.get("valoracio").getAsDouble() : 0.0;
        boolean llegit   = j.has("llegit") && j.get("llegit").getAsBoolean();
        synchronized (cd) { cd.addLlibreToLlista(isbn, id, valoracio, llegit); }
        ctx.status(201).json(Map.of("ok", true));
    }

    private void updateBook(HttpCtx ctx) throws Exception {
        int id    = Integer.parseInt(ctx.pathParam("id"));
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        double valoracio = j.has("valoracio") ? j.get("valoracio").getAsDouble() : 0.0;
        boolean llegit   = j.has("llegit") && j.get("llegit").getAsBoolean();
        synchronized (cd) { cd.updateLlibreInLlista(isbn, id, valoracio, llegit); }
        ctx.json(Map.of("ok", true));
    }

    private void removeBook(HttpCtx ctx) throws Exception {
        int id    = Integer.parseInt(ctx.pathParam("id"));
        long isbn = Long.parseLong(ctx.pathParam("isbn"));
        synchronized (cd) { cd.removeLlibreFromLlista(isbn, id); }
        ctx.status(204);
    }

    private Llista findById(int id) throws Exception {
        for (Llista l : cd.getAllLlistes()) if (l.getId() == id) return l;
        throw new Exception("Shelf not found: " + id);
    }
}
