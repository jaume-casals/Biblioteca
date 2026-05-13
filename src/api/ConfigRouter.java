package api;

import com.google.gson.JsonObject;
import herramienta.Config;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigRouter {

    public ConfigRouter(HttpRouter app) {
        app.get("/api/config", ctx -> getConfig(ctx));
        app.put("/api/config", ctx -> setConfig(ctx));
    }

    private void getConfig(HttpCtx ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("darkMode",        Config.isDarkMode());
        m.put("dbType",          Config.getDbType());
        m.put("dbHost",          Config.getDbHost());
        m.put("dbUser",          Config.getDbUser());
        m.put("fontSize",        Config.getFontSize());
        m.put("currencySymbol",  Config.getCurrencySymbol());
        m.put("defaultValoracio",Config.getDefaultValoracio());
        m.put("readingGoal",     Config.getReadingGoal());
        m.put("viewMode",        Config.getViewMode());
        m.put("galleryZoom",     Config.getGalleryZoom());
        m.put("defaultImgDir",   Config.getDefaultImgDir());
        ctx.json(m);
    }

    private void setConfig(HttpCtx ctx) {
        JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
        if (j.has("darkMode"))         Config.setDarkMode(j.get("darkMode").getAsBoolean());
        if (j.has("dbType"))           Config.setDbType(j.get("dbType").getAsString());
        if (j.has("dbHost"))           Config.setDbHost(j.get("dbHost").getAsString());
        if (j.has("dbUser"))           Config.setDbUser(j.get("dbUser").getAsString());
        if (j.has("dbPassword"))       Config.setDbPassword(j.get("dbPassword").getAsString());
        if (j.has("fontSize"))         Config.setFontSize(j.get("fontSize").getAsString());
        if (j.has("currencySymbol"))   Config.setCurrencySymbol(j.get("currencySymbol").getAsString());
        if (j.has("defaultValoracio")) Config.setDefaultValoracio(j.get("defaultValoracio").getAsDouble());
        if (j.has("readingGoal"))      Config.setReadingGoal(j.get("readingGoal").getAsInt());
        if (j.has("viewMode"))         Config.setViewMode(j.get("viewMode").getAsString());
        if (j.has("galleryZoom"))      Config.setGalleryZoom(j.get("galleryZoom").getAsInt());
        if (j.has("defaultImgDir"))    Config.setDefaultImgDir(j.get("defaultImgDir").getAsString());
        ctx.json(Map.of("ok", true));
    }
}
