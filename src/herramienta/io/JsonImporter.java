package herramienta.io;

import herramienta.text.ValidadorLlibre;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import domini.Llibre;
import persistencia.contract.EscritorBiblioteca;
import herramienta.ImportadorLlibres.ResultatImportacio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Importador JSON compartit per {@link ImportadorLlibres#importJSON(File, EscritorBiblioteca)}.
 * Lògica deduplicada — un
 * canvi en el format d'entrada només cal aplicar-lo aquí.
 */
public final class JsonImporter {

    private JsonImporter() {}

    public static ResultatImportacio run(File f, EscritorBiblioteca cd) throws Exception {
        try (Reader reader = new InputStreamReader(
                new BufferedInputStream(new FileInputStream(f)), StandardCharsets.UTF_8)) {
            return run(JsonParser.parseReader(reader).getAsJsonObject(), cd);
        }
    }

    public static ResultatImportacio run(JsonObject root, EscritorBiblioteca cd) throws Exception {
        int ok = 0, skipped = 0, err = 0;
        List<String> errDetails = new ArrayList<>();
        RelationRemapper.RemapejadorIdEtiqueta tagRemap = new RelationRemapper.RemapejadorIdEtiqueta(cd);
        RelationRemapper.RemapejadorIdPrestatgeria llistaRemap = new RelationRemapper.RemapejadorIdPrestatgeria(cd);
        Set<Long> existingIsbns = null;
        Map<Integer, Integer> tagIdMap = new HashMap<>();
        if (root.has("tags")) {
            mapOldToNewIds(root.getAsJsonArray("tags"), tagIdMap, tagRemap::resolve);
        }
        Map<Integer, Integer> llistaIdMap = new HashMap<>();
        if (root.has("llistes")) {
            mapOldToNewIds(root.getAsJsonArray("llistes"), llistaIdMap, llistaRemap::resolve);
        }
        if (root.has("llibres")) {
            existingIsbns = new HashSet<>();
            for (Llibre l : cd.obtenirAllLlibres()) existingIsbns.add(l.obtenirISBN());
            for (JsonElement be : root.getAsJsonArray("llibres")) {
                try {
                    JsonObject bo = be.getAsJsonObject();
                    long isbn = bo.get("isbn").getAsLong();
                    if (existingIsbns.contains(isbn)) { skipped++; continue; }
                    String nom = bo.has("nom") && !bo.get("nom").isJsonNull() ? bo.get("nom").getAsString() : "";
                    String autor = bo.has("autor") && !bo.get("autor").isJsonNull() ? bo.get("autor").getAsString() : "";
                    int any = bo.has("any") ? bo.get("any").getAsInt() : 0;
                    String desc = bo.has("descripcio") && !bo.get("descripcio").isJsonNull() ? bo.get("descripcio").getAsString() : "";
                    double val = bo.has("valoracio") ? bo.get("valoracio").getAsDouble() : 0.0;
                    double preu = bo.has("preu") ? bo.get("preu").getAsDouble() : 0.0;
                    boolean llegit = bo.has("llegit") && bo.get("llegit").getAsBoolean();
                    String imatge = bo.has("imatge") && !bo.get("imatge").isJsonNull() ? bo.get("imatge").getAsString() : "";
                    Llibre l = ValidadorLlibre.comprovarLlibre(isbn, nom, autor, any, desc, val, preu, llegit, imatge);
                    aplicarCampsOpcionals(bo, l);
                    cd.afegirLlibre(l);
                    if (bo.has("tags") && bo.get("tags").isJsonArray()) {
                        for (JsonElement te : bo.getAsJsonArray("tags")) {
                            int oldTagId = te.getAsInt();
                            Integer newId = tagIdMap.get(oldTagId);
                            if (newId != null) cd.afegirLlibreToTag(isbn, newId);
                        }
                    }
                    if (bo.has("llistes") && bo.get("llistes").isJsonArray()) {
                        for (JsonElement me : bo.getAsJsonArray("llistes")) {
                            JsonObject mo = me.getAsJsonObject();
                            Integer newLlistaId = llistaIdMap.get(mo.get("id").getAsInt());
                            if (newLlistaId == null) continue;
                            double mVal = mo.has("valoracio") ? mo.get("valoracio").getAsDouble() : 0.0;
                            boolean mLlegit = mo.has("llegit") && mo.get("llegit").getAsBoolean();
                            cd.afegirLlibreToLlista(isbn, newLlistaId, mVal, mLlegit);
                        }
                    }
                    ok++;
                } catch (Exception e) {
                    err++;
                    String msg = e.getMessage();
                    errDetails.add(msg != null ? msg : e.getClass().getSimpleName());
                    Logger.getLogger(JsonImporter.class.getName())
                        .log(Level.FINE, "No s'ha pogut importar l'entrada del llibre: " + msg, e);
                }
            }
        }
        return new ResultatImportacio(ok, skipped, err, errDetails);
    }

    private static void mapOldToNewIds(JsonArray arr, Map<Integer, Integer> idMap,
            java.util.function.Function<String, Integer> resolve) {
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            idMap.put(o.get("id").getAsInt(), resolve.apply(o.get("nom").getAsString()));
        }
    }

    private record CampOpcional(String key, java.util.function.BiConsumer<Llibre, JsonElement> apply,
            boolean nullableString) {}

    private static final CampOpcional[] CAMPS_OPCIONALS = {
        new CampOpcional("notes", (l, el) -> l.posarNotes(el.getAsString()), true),
        new CampOpcional("pagines", (l, el) -> l.posarPagines(el.getAsInt()), false),
        new CampOpcional("paginesLlegides", (l, el) -> l.posarPaginesLlegides(el.getAsInt()), false),
        new CampOpcional("editorial", (l, el) -> l.posarEditorial(el.getAsString()), true),
        new CampOpcional("serie", (l, el) -> l.posarSerie(el.getAsString()), true),
        new CampOpcional("volum", (l, el) -> l.posarVolum(el.getAsInt()), false),
        new CampOpcional("dataCompra", (l, el) -> l.posarDataCompra(el.getAsString()), true),
        new CampOpcional("dataLectura", (l, el) -> l.posarDataLectura(el.getAsString()), true),
        new CampOpcional("idioma", (l, el) -> l.posarIdioma(el.getAsString()), true),
        new CampOpcional("format", (l, el) -> l.posarFormat(el.getAsString()), true),
        new CampOpcional("desitjat", (l, el) -> l.posarDesitjat(el.getAsBoolean()), false),
        new CampOpcional("paisOrigen", (l, el) -> l.posarPaisOrigen(el.getAsString()), true),
        new CampOpcional("estat", (l, el) -> l.posarEstat(el.getAsString()), true),
        new CampOpcional("exemplars", (l, el) -> l.posarExemplars(el.getAsInt()), false),
        new CampOpcional("llenguaOriginal", (l, el) -> l.posarLlenguaOriginal(el.getAsString()), true),
        new CampOpcional("nomCa", (l, el) -> l.posarNomCa(el.getAsString()), true),
        new CampOpcional("nomEs", (l, el) -> l.posarNomEs(el.getAsString()), true),
        new CampOpcional("nomEn", (l, el) -> l.posarNomEn(el.getAsString()), true),
    };

    private static void aplicarCampsOpcionals(JsonObject bo, Llibre l) {
        for (CampOpcional camp : CAMPS_OPCIONALS) {
            if (!bo.has(camp.key())) continue;
            JsonElement el = bo.get(camp.key());
            if (camp.nullableString() && el.isJsonNull()) continue;
            camp.apply().accept(l, el);
        }
    }
}
