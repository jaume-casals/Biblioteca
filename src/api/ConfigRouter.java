package api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import herramienta.Config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ConfigRouter {

    public ConfigRouter(HttpRouter app) {
        app.get("/api/config", ctx -> getConfig(ctx));
        app.put("/api/config", ctx -> setConfig(ctx));
    }

    private void getConfig(HttpCtx ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("theme",           Config.getTheme().key());
        m.put("darkMode",        Config.isDarkMode());
        m.put("dbType",          Config.getDbType());
        m.put("dbHost",          Config.getDbHost());
        m.put("dbUser",          Config.getDbUser());
        m.put("dbPassword",      Config.getDbPassword().isEmpty() ? "" : "***");
        m.put("fontSize",        Config.getFontSize());
        m.put("currencySymbol",  Config.getCurrencySymbol());
        m.put("defaultValoracio",Config.getDefaultValoracio());
        m.put("readingGoal",     Config.getReadingGoal());
        m.put("viewMode",        Config.getViewMode());
        m.put("galleryZoom",     Config.getGalleryZoom());
        m.put("defaultImgDir",   Config.getDefaultImgDir());
        ctx.json(m);
    }

    private static final Map<String, Consumer<JsonElement>> CONFIG_SETTERS = new LinkedHashMap<>();
    static {
        CONFIG_SETTERS.put("theme",            e -> Config.setTheme(herramienta.UITheme.Theme.fromKey(e.getAsString())));
        CONFIG_SETTERS.put("darkMode",         e -> Config.setDarkMode(e.getAsBoolean()));
        CONFIG_SETTERS.put("dbType",           e -> Config.setDbType(e.getAsString()));
        CONFIG_SETTERS.put("dbHost",           e -> Config.setDbHost(e.getAsString()));
        CONFIG_SETTERS.put("dbUser",           e -> Config.setDbUser(e.getAsString()));
        CONFIG_SETTERS.put("dbPassword",       e -> Config.setDbPassword(e.getAsString()));
        CONFIG_SETTERS.put("fontSize",         e -> {
            String v = e.getAsString();
            if (!Set.of("small", "medium", "large", "xlarge").contains(v))
                throw new IllegalArgumentException("Invalid fontSize: " + v);
            Config.setFontSize(v);
        });
        CONFIG_SETTERS.put("currencySymbol",   e -> Config.setCurrencySymbol(e.getAsString()));
        CONFIG_SETTERS.put("defaultValoracio", e -> Config.setDefaultValoracio(e.getAsDouble()));
        CONFIG_SETTERS.put("readingGoal",      e -> Config.setReadingGoal(e.getAsInt()));
        CONFIG_SETTERS.put("viewMode",         e -> Config.setViewMode(e.getAsString()));
        CONFIG_SETTERS.put("galleryZoom",      e -> Config.setGalleryZoom(e.getAsInt()));
        CONFIG_SETTERS.put("defaultImgDir",    e -> Config.setDefaultImgDir(e.getAsString()));
    }

    private void setConfig(HttpCtx ctx) {
        // Note: theme/font changes here do NOT trigger UI refresh — web mode has no Swing UI.
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        CONFIG_SETTERS.forEach((key, setter) -> { if (j.has(key)) setter.accept(j.get(key)); });
        ctx.json(Map.of("ok", true));
    }
}
