package herramienta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import herramienta.io.JsonHelpers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anàlisi JSON pura per a respostes d'OpenLibrary. Extret de {@link ClientOpenLibrary}
 * perquè el camí de xarxa i la lògica d'anàlisi es puguin provar independentment.
 */
public final class AnalitzadorOpenLibrary {
    private AnalitzadorOpenLibrary() {}

    /** Analitza un cos de resposta {@code /api/books?bibkeys=ISBN:...&format=json&jscmd=data}. */
    public static Map<String, String> analitzarIsbnResponse(String json) {
        Map<String, String> r = new HashMap<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject book = null;
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (e.getValue().isJsonObject()) { book = e.getValue().getAsJsonObject(); break; }
        }
        if (book == null) return r;
        put(r, "title",      JsonHelpers.jsonStr(book, "title"));
        put(r, "descripcio", JsonHelpers.jsonStrNested(book, "description", "value"));
        if (r.get("descripcio") == null) put(r, "descripcio", JsonHelpers.jsonStr(book, "description"));
        String date = JsonHelpers.jsonStr(book, "publish_date");
        if (date != null) {
            Matcher m = Pattern.compile("\\b(\\d{4})\\b").matcher(date);
            if (m.find()) r.put("any", m.group(1));
        }
        if (book.has("number_of_pages") && !book.get("number_of_pages").isJsonNull()) {
            try {
                long pages = book.get("number_of_pages").getAsLong();
                if (pages >= 0 && pages <= Integer.MAX_VALUE)
                    r.put("pagines", String.valueOf((int) pages));
            } catch (NumberFormatException ignored) {
                // El valor no és un nombre vàlid (per exemple,
                // "320 p." en lloc de "320") — simplement no
                // informem el recompte.
            }
        }
        put(r, "editorial", JsonHelpers.jsonArrayFirstField(book, "publishers", "name"));
        put(r, "autor",     JsonHelpers.jsonArrayFirstField(book, "authors",    "name"));
        if (book.has("languages")) {
            JsonArray langs = book.getAsJsonArray("languages");
            if (langs.size() > 0) {
                String key = JsonHelpers.jsonStr(langs.get(0).getAsJsonObject(), "key");
                if (key != null) r.put("idioma", key.replaceAll(".*/", ""));
            }
        }
        return r;
    }

    private static void put(Map<String, String> m, String k, String v) { if (v != null) m.put(k, v); }
}
