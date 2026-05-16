package api;

import com.google.gson.*;
import domini.Llibre;
import domini.Llista;
import domini.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonMapper {

    private JsonMapper() {}

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static Gson gson() { return GSON; }

    public static Map<String, Object> llibreToMap(Llibre l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("isbn", l.getISBN());
        m.put("nom", l.getNom());
        m.put("autor", l.getAutor());
        m.put("autors", l.getAutors());
        m.put("any", l.getAny());
        m.put("descripcio", l.getDescripcio());
        m.put("valoracio", l.getValoracio());
        m.put("preu", l.getPreu());
        m.put("llegit", l.getLlegit());
        m.put("imatge", l.getImatge());
        m.put("hasBlob", l.hasBlob());
        m.put("notes", l.getNotes());
        m.put("pagines", l.getPagines());
        m.put("paginesLlegides", l.getPaginesLlegides());
        m.put("editorial", l.getEditorial());
        m.put("serie", l.getSerie());
        m.put("volum", l.getVolum());
        m.put("dataCompra", l.getDataCompra());
        m.put("dataLectura", l.getDataLectura());
        m.put("idioma", l.getIdioma());
        m.put("format", l.getFormat());
        m.put("desitjat", l.getDesitjat());
        m.put("paisOrigen", l.getPaisOrigen());
        m.put("nomCa", l.getNomCa());
        m.put("nomEs", l.getNomEs());
        m.put("nomEn", l.getNomEn());
        // ImatgeBlob excluded — served via /api/books/:isbn/image
        return m;
    }

    public static List<Map<String, Object>> llibresToList(ArrayList<Llibre> llibres) {
        List<Map<String, Object>> out = new ArrayList<>(llibres.size());
        for (Llibre l : llibres) out.add(llibreToMap(l));
        return out;
    }

    public static Map<String, Object> llistaToMap(Llista l) { return l.toMap(); }

    public static Map<String, Object> tagToMap(Tag t) { return t.toMap(); }

    public static Llibre jsonToLlibre(JsonObject j) {
        long isbn = j.has("isbn") ? j.get("isbn").getAsLong() : 0L;
        Llibre l = new Llibre(
            isbn,
            optStr(j, "nom", ""),
            optStr(j, "autor", ""),
            optInt(j, "any", 0),
            optStr(j, "descripcio", ""),
            optDbl(j, "valoracio", 0.0),
            optDbl(j, "preu", 0.0),
            has(j, "llegit") && j.get("llegit").getAsBoolean(),
            optStr(j, "imatge", "")
        );

        String notes = optStr(j, "notes", null); if (notes != null) l.setNotes(notes);
        int pagines = optInt(j, "pagines", -1); if (pagines >= 0) l.setPagines(pagines);
        int pagLleg = optInt(j, "paginesLlegides", -1); if (pagLleg >= 0) l.setPaginesLlegides(pagLleg);
        String ed = optStr(j, "editorial", null); if (ed != null) l.setEditorial(ed);
        String serie = optStr(j, "serie", null); if (serie != null) l.setSerie(serie);
        int volum = optInt(j, "volum", -1); if (volum >= 0) l.setVolum(volum);
        l.setDataCompra(optStr(j, "dataCompra", null));
        l.setDataLectura(optStr(j, "dataLectura", null));
        l.setIdioma(optStr(j, "idioma", null));
        l.setFormat(optStr(j, "format", null));
        if (has(j, "desitjat")) l.setDesitjat(j.get("desitjat").getAsBoolean());
        l.setPaisOrigen(optStr(j, "paisOrigen", null));
        l.setNomCa(optStr(j, "nomCa", null));
        l.setNomEs(optStr(j, "nomEs", null));
        l.setNomEn(optStr(j, "nomEn", null));

        if (j.has("autors") && j.get("autors").isJsonArray()) {
            List<String> autors = new ArrayList<>();
            for (JsonElement e : j.getAsJsonArray("autors")) autors.add(e.getAsString());
            l.setAutors(autors);
        }

        return l;
    }

    private static boolean has(JsonObject j, String key) {
        return j.has(key) && !j.get(key).isJsonNull();
    }

    private static String optStr(JsonObject j, String key, String def) {
        return has(j, key) ? j.get(key).getAsString() : def;
    }

    private static int optInt(JsonObject j, String key, int def) {
        return has(j, key) ? j.get(key).getAsInt() : def;
    }

    private static double optDbl(JsonObject j, String key, double def) {
        return has(j, key) ? j.get(key).getAsDouble() : def;
    }
}
