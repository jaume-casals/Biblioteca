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
        // ImatgeBlob excluded — served via /api/books/:isbn/image
        return m;
    }

    public static List<Map<String, Object>> llibresToList(ArrayList<Llibre> llibres) {
        List<Map<String, Object>> out = new ArrayList<>(llibres.size());
        for (Llibre l : llibres) out.add(llibreToMap(l));
        return out;
    }

    public static Map<String, Object> llistaToMap(Llista l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("nom", l.getNom());
        m.put("ordre", l.getOrdre());
        m.put("color", l.getColor());
        return m;
    }

    public static Map<String, Object> tagToMap(Tag t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("nom", t.getNom());
        return m;
    }

    public static Llibre jsonToLlibre(JsonObject j) {
        long isbn = j.has("isbn") ? j.get("isbn").getAsLong() : 0L;
        String nom = j.has("nom") ? j.get("nom").getAsString() : "";
        String autor = j.has("autor") ? j.get("autor").getAsString() : "";
        int any = j.has("any") && !j.get("any").isJsonNull() ? j.get("any").getAsInt() : 0;
        String descripcio = j.has("descripcio") && !j.get("descripcio").isJsonNull() ? j.get("descripcio").getAsString() : "";
        double valoracio = j.has("valoracio") && !j.get("valoracio").isJsonNull() ? j.get("valoracio").getAsDouble() : 0.0;
        double preu = j.has("preu") && !j.get("preu").isJsonNull() ? j.get("preu").getAsDouble() : 0.0;
        boolean llegit = j.has("llegit") && !j.get("llegit").isJsonNull() && j.get("llegit").getAsBoolean();
        String imatge = j.has("imatge") && !j.get("imatge").isJsonNull() ? j.get("imatge").getAsString() : "";

        Llibre l = new Llibre(isbn, nom, autor, any, descripcio, valoracio, preu, llegit, imatge);

        if (j.has("notes") && !j.get("notes").isJsonNull()) l.setNotes(j.get("notes").getAsString());
        if (j.has("pagines") && !j.get("pagines").isJsonNull()) l.setPagines(j.get("pagines").getAsInt());
        if (j.has("paginesLlegides") && !j.get("paginesLlegides").isJsonNull()) l.setPaginesLlegides(j.get("paginesLlegides").getAsInt());
        if (j.has("editorial") && !j.get("editorial").isJsonNull()) l.setEditorial(j.get("editorial").getAsString());
        if (j.has("serie") && !j.get("serie").isJsonNull()) l.setSerie(j.get("serie").getAsString());
        if (j.has("volum") && !j.get("volum").isJsonNull()) l.setVolum(j.get("volum").getAsInt());
        if (j.has("dataCompra") && !j.get("dataCompra").isJsonNull()) l.setDataCompra(j.get("dataCompra").getAsString());
        if (j.has("dataLectura") && !j.get("dataLectura").isJsonNull()) l.setDataLectura(j.get("dataLectura").getAsString());
        if (j.has("idioma") && !j.get("idioma").isJsonNull()) l.setIdioma(j.get("idioma").getAsString());
        if (j.has("format") && !j.get("format").isJsonNull()) l.setFormat(j.get("format").getAsString());
        if (j.has("desitjat") && !j.get("desitjat").isJsonNull()) l.setDesitjat(j.get("desitjat").getAsBoolean());
        if (j.has("paisOrigen") && !j.get("paisOrigen").isJsonNull()) l.setPaisOrigen(j.get("paisOrigen").getAsString());

        if (j.has("autors") && j.get("autors").isJsonArray()) {
            List<String> autors = new ArrayList<>();
            for (JsonElement e : j.getAsJsonArray("autors")) autors.add(e.getAsString());
            l.setAutors(autors);
        }

        return l;
    }
}
