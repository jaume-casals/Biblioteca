package api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import herramienta.Config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ConfigRouter {

    private static final Set<String> RESTART_KEYS = Set.of("dbType", "dbHost", "dbUser", "dbPassword");

    private static final Map<String, Consumer<JsonElement>> UI_SETTERS = buildUiSetters();

    private static Map<String, Consumer<JsonElement>> buildUiSetters() {
        Map<String, Consumer<JsonElement>> m = new LinkedHashMap<>();
        m.put("theme",            e -> Config.setTheme(herramienta.UITheme.Theme.fromKey(e.getAsString())));
        m.put("darkMode",         e -> Config.setDarkMode(e.getAsBoolean()));
        m.put("fontSize",         e -> Config.setFontSize(e.getAsString()));
        m.put("currencySymbol",   e -> Config.setCurrencySymbol(e.getAsString()));
        m.put("defaultValoracio", e -> Config.setDefaultValoracio(e.getAsDouble()));
        m.put("readingGoal",      e -> Config.setReadingGoal(e.getAsInt()));
        m.put("viewMode",         e -> Config.setViewMode(e.getAsString()));
        m.put("galleryZoom",      e -> Config.setGalleryZoom(e.getAsInt()));
        m.put("defaultImgDir",    e -> Config.setDefaultImgDir(e.getAsString()));
        return Map.copyOf(m);
    }

    private static final Map<String, Consumer<JsonElement>> DB_SETTERS = buildDbSetters();

    private static Map<String, Consumer<JsonElement>> buildDbSetters() {
        Map<String, Consumer<JsonElement>> m = new LinkedHashMap<>();
        m.put("dbType",     e -> Config.setDbType(e.getAsString()));
        m.put("dbHost",     e -> Config.setDbHost(e.getAsString()));
        m.put("dbUser",     e -> Config.setDbUser(e.getAsString()));
        m.put("dbPassword", e -> {
            String v = e.getAsString();
            if (!"***".equals(v)) Config.setDbPassword(v);
        });
        return Map.copyOf(m);
    }

    public ConfigRouter(HttpRouter app) {
        app.get("/api/config",     ctx -> getConfig(ctx));
        app.put("/api/config",     ctx -> setConfig(ctx, unionSetters()));
        app.get("/api/config/ui",  ctx -> getUiConfig(ctx));
        app.put("/api/config/ui",  ctx -> setConfig(ctx, UI_SETTERS));
        app.get("/api/config/db",  ctx -> getDbConfig(ctx));
        app.put("/api/config/db",  ctx -> setConfig(ctx, DB_SETTERS));
    }

    private static Map<String, Consumer<JsonElement>> unionSetters() {
        Map<String, Consumer<JsonElement>> m = new LinkedHashMap<>(UI_SETTERS);
        m.putAll(DB_SETTERS);
        return m;
    }

    private void getConfig(HttpCtx ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.putAll(uiMap());
        m.putAll(dbMap());
        ctx.json(m);
    }

    private void getUiConfig(HttpCtx ctx) { ctx.json(uiMap()); }

    private void getDbConfig(HttpCtx ctx) { ctx.json(dbMap()); }

    private static Map<String, Object> uiMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("theme",            Config.getTheme().key());
        m.put("darkMode",         Config.isDarkMode());
        m.put("fontSize",         Config.getFontSize());
        m.put("currencySymbol",   Config.getCurrencySymbol());
        m.put("defaultValoracio", Config.getDefaultValoracio());
        m.put("readingGoal",      Config.getReadingGoal());
        m.put("viewMode",         Config.getViewMode());
        m.put("galleryZoom",      Config.getGalleryZoom());
        m.put("defaultImgDir",    Config.getDefaultImgDir());
        return m;
    }

    private static Map<String, Object> dbMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dbType",     Config.getDbType());
        m.put("dbHost",     Config.getDbHost());
        m.put("dbUser",     Config.getDbUser());
        m.put("dbPassword", Config.getDbPassword().isEmpty() ? "" : "***");
        m.put("requiresRestart", true);
        return m;
    }

    private void setConfig(HttpCtx ctx, Map<String, Consumer<JsonElement>> setters) {
        String body = ctx.body();
        if (body == null || body.isBlank()) { ctx.status(400).json(Map.of("error", "Empty body")); return; }
        JsonObject j = JsonMapper.gson().fromJson(body, JsonObject.class);
        List<String> unknown = new ArrayList<>();
        for (String key : j.keySet()) {
            if (!setters.containsKey(key)) unknown.add(key);
        }
        if (!unknown.isEmpty()) {
            ctx.status(400).json(Map.of("error", "Unknown config keys", "keys", unknown));
            return;
        }
        for (String key : j.keySet()) {
            try { setters.get(key).accept(j.get(key)); }
            catch (Exception e) { ctx.status(400).json(Map.of("error", "Invalid value for key: " + key, "detail", e.getMessage())); return; }
        }
        boolean needsRestart = j.keySet().stream().anyMatch(RESTART_KEYS::contains);
        ctx.json(Map.of("ok", true, "requiresRestart", needsRestart));
    }
}