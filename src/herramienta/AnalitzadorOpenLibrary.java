package herramienta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-JSON parsing for OpenLibrary responses. Extracted from {@link OpenLibraryClient} so the
 * network path and the parsing logic can be tested independently.
 */
public final class AnalitzadorOpenLibrary {
    private AnalitzadorOpenLibrary() {}

    /** Parse an {@code /api/books?bibkeys=ISBN:...&format=json&jscmd=data} response body. */
    public static Map<String, String> analitzarIsbnResponse(String json) {
        Map<String, String> r = new HashMap<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject book = null;
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (e.getValue().isJsonObject()) { book = e.getValue().getAsJsonObject(); break; }
        }
        if (book == null) return r;
        put(r, "title",      jsonStr(book, "title"));
        put(r, "descripcio", jsonStrNested(book, "description", "value"));
        if (r.get("descripcio") == null) put(r, "descripcio", jsonStr(book, "description"));
        String date = jsonStr(book, "publish_date");
        if (date != null) {
            Matcher m = Pattern.compile("\\b(\\d{4})\\b").matcher(date);
            if (m.find()) r.put("any", m.group(1));
        }
        if (book.has("number_of_pages") && !book.get("number_of_pages").isJsonNull())
            r.put("pagines", String.valueOf(book.get("number_of_pages").getAsInt()));
        put(r, "editorial", jsonArrayFirstField(book, "publishers", "name"));
        put(r, "autor",     jsonArrayFirstField(book, "authors",    "name"));
        if (book.has("languages")) {
            JsonArray langs = book.getAsJsonArray("languages");
            if (langs.size() > 0) {
                String key = jsonStr(langs.get(0).getAsJsonObject(), "key");
                if (key != null) r.put("idioma", key.replaceAll(".*/", ""));
            }
        }
        return r;
    }

    private static void put(Map<String, String> m, String k, String v) { if (v != null) m.put(k, v); }

    private static String jsonStr(JsonObject obj, String key) {
        return obj != null && obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
    }

    private static String jsonStrNested(JsonObject obj, String key, String subKey) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonObject()) return null;
        return jsonStr(obj.getAsJsonObject(key), subKey);
    }

    private static String jsonArrayFirstField(JsonObject obj, String arrKey, String fieldKey) {
        if (obj == null || !obj.has(arrKey) || !obj.get(arrKey).isJsonArray()) return null;
        JsonArray a = obj.getAsJsonArray(arrKey);
        return a.size() > 0 && a.get(0).isJsonObject() ? jsonStr(a.get(0).getAsJsonObject(), fieldKey) : null;
    }
}
