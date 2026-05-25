package api;

import com.google.gson.JsonObject;
import domini.Llibre;

import java.util.Map;

/**
 * Single source of truth for Llibre↔JSON marshalling. Today it delegates to
 * {@link JsonMapper}; future refactor will host a single FIELDS array so adding a new field
 * touches one location instead of mirror getter/setter blocks.
 */
public final class LlibreCodec {
    private LlibreCodec() {}

    public static Map<String, Object> encode(Llibre l) { return JsonMapper.llibreToMap(l); }
    public static Llibre decode(JsonObject j)          { return JsonMapper.jsonToLlibre(j); }
}
