package herramienta.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Helpers compartits per a l'anàlisi de respostes JSON d'OpenLibrary i
 * Google Books. Són package-private perquè només
 * {@link AnalitzadorOpenLibrary} i {@link ClientOpenLibrary} els
 * necessiten — la duplicació anterior entre els dos fitxers havia
 * divergit (per exemple, {@code jsonStr} d'AnalitzadorOpenLibrary
 * comprovava {@code isJsonPrimitive()}, el de ClientOpenLibrary
 * comprovava {@code isJsonNull()}).
 */
public final class JsonHelpers {
    private JsonHelpers() {}

    public static String jsonStr(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        JsonElement e = obj.get(key);
        return e.isJsonPrimitive() ? e.getAsString() : null;
    }

    public static String jsonStrNested(JsonObject obj, String key, String subKey) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonObject()) return null;
        return jsonStr(obj.getAsJsonObject(key), subKey);
    }

    public static String jsonArrayFirstField(JsonObject obj, String arrayKey, String fieldKey) {
        if (obj == null || !obj.has(arrayKey) || !obj.get(arrayKey).isJsonArray()) return null;
        JsonArray arr = obj.getAsJsonArray(arrayKey);
        if (arr.isEmpty()) return null;
        JsonElement first = arr.get(0);
        if (!first.isJsonObject()) return null;
        return jsonStr(first.getAsJsonObject(), fieldKey);
    }
}
