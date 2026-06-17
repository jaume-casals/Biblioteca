package herramienta;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import domini.Llista;
import domini.Llibre;
import domini.Tag;
import interficie.BibliotecaWriter;
import herramienta.ImportadorLlibres.ResultatImportacio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Importador JSON compartit per {@link BookImporter#importJSON(File, BibliotecaWriter)}.
 * Lònica deduplicada — un
 * canvi en el format d'entrada només cal aplicar-lo aquí.
 */
public final class JsonImporter {

    private JsonImporter() {}

    public static ResultatImportacio run(File f, BibliotecaWriter cd) throws Exception {
        try (Reader reader = new InputStreamReader(
                new BufferedInputStream(new FileInputStream(f)), StandardCharsets.UTF_8)) {
            return run(JsonParser.parseReader(reader).getAsJsonObject(), cd);
        }
    }

    public static ResultatImportacio run(JsonObject root, BibliotecaWriter cd) throws Exception {
        int ok = 0, skipped = 0, err = 0;
        List<String> errDetails = new ArrayList<>();
        Map<Integer, Integer> tagIdMap = new HashMap<>();
        if (root.has("tags")) {
            for (JsonElement te : root.getAsJsonArray("tags")) {
                JsonObject to = te.getAsJsonObject();
                int oldId = to.get("id").getAsInt();
                String nom = to.get("nom").getAsString();
                Tag existing = cd.obtenirAllTags().stream().filter(t -> t.obtenirNom().equals(nom)).findFirst().orElse(null);
                if (existing != null) { tagIdMap.put(oldId, existing.obtenirId()); }
                else { Tag nt = cd.afegirTag(nom); tagIdMap.put(oldId, nt.obtenirId()); }
            }
        }
        Map<Integer, Integer> llistaIdMap = new HashMap<>();
        if (root.has("llistes")) {
            for (JsonElement le : root.getAsJsonArray("llistes")) {
                JsonObject lo = le.getAsJsonObject();
                int oldId = lo.get("id").getAsInt();
                String nom = lo.get("nom").getAsString();
                Llista existing = cd.obtenirAllLlistes().stream().filter(l -> l.obtenirNom().equals(nom)).findFirst().orElse(null);
                if (existing != null) { llistaIdMap.put(oldId, existing.obtenirId()); }
                else { Llista nl = cd.afegirLlista(nom); llistaIdMap.put(oldId, nl.obtenirId()); }
            }
        }
        if (root.has("llibres")) {
            for (JsonElement be : root.getAsJsonArray("llibres")) {
                try {
                    JsonObject bo = be.getAsJsonObject();
                    long isbn = bo.get("isbn").getAsLong();
                    if (cd.existsLlibre(isbn)) { skipped++; continue; }
                    String nom = bo.has("nom") && !bo.get("nom").isJsonNull() ? bo.get("nom").getAsString() : "";
                    String autor = bo.has("autor") && !bo.get("autor").isJsonNull() ? bo.get("autor").getAsString() : "";
                    int any = bo.has("any") ? bo.get("any").getAsInt() : 0;
                    String desc = bo.has("descripcio") && !bo.get("descripcio").isJsonNull() ? bo.get("descripcio").getAsString() : "";
                    double val = bo.has("valoracio") ? bo.get("valoracio").getAsDouble() : 0.0;
                    double preu = bo.has("preu") ? bo.get("preu").getAsDouble() : 0.0;
                    boolean llegit = bo.has("llegit") && bo.get("llegit").getAsBoolean();
                    String imatge = bo.has("imatge") && !bo.get("imatge").isJsonNull() ? bo.get("imatge").getAsString() : "";
                    Llibre l = ValidadorLlibre.comprovarLlibre(isbn, nom, autor, any, desc, val, preu, llegit, imatge);
                    if (bo.has("notes") && !bo.get("notes").isJsonNull()) l.posarNotes(bo.get("notes").getAsString());
                    if (bo.has("pagines")) l.posarPagines(bo.get("pagines").getAsInt());
                    if (bo.has("paginesLlegides")) l.posarPaginesLlegides(bo.get("paginesLlegides").getAsInt());
                    if (bo.has("editorial") && !bo.get("editorial").isJsonNull()) l.posarEditorial(bo.get("editorial").getAsString());
                    if (bo.has("serie") && !bo.get("serie").isJsonNull()) l.posarSerie(bo.get("serie").getAsString());
                    if (bo.has("volum")) l.posarVolum(bo.get("volum").getAsInt());
                    if (bo.has("dataCompra") && !bo.get("dataCompra").isJsonNull()) l.posarDataCompra(bo.get("dataCompra").getAsString());
                    if (bo.has("dataLectura") && !bo.get("dataLectura").isJsonNull()) l.posarDataLectura(bo.get("dataLectura").getAsString());
                    if (bo.has("idioma") && !bo.get("idioma").isJsonNull()) l.posarIdioma(bo.get("idioma").getAsString());
                    if (bo.has("format") && !bo.get("format").isJsonNull()) l.posarFormat(bo.get("format").getAsString());
                    if (bo.has("desitjat")) l.posarDesitjat(bo.get("desitjat").getAsBoolean());
                    if (bo.has("paisOrigen") && !bo.get("paisOrigen").isJsonNull()) l.posarPaisOrigen(bo.get("paisOrigen").getAsString());
                    if (bo.has("estat") && !bo.get("estat").isJsonNull()) l.posarEstat(bo.get("estat").getAsString());
                    if (bo.has("exemplars")) l.posarExemplars(bo.get("exemplars").getAsInt());
                    if (bo.has("llenguaOriginal") && !bo.get("llenguaOriginal").isJsonNull()) l.posarLlenguaOriginal(bo.get("llenguaOriginal").getAsString());
                    if (bo.has("nomCa") && !bo.get("nomCa").isJsonNull()) l.posarNomCa(bo.get("nomCa").getAsString());
                    if (bo.has("nomEs") && !bo.get("nomEs").isJsonNull()) l.posarNomEs(bo.get("nomEs").getAsString());
                    if (bo.has("nomEn") && !bo.get("nomEn").isJsonNull()) l.posarNomEn(bo.get("nomEn").getAsString());
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
                    errDetails.add(e.getMessage());
                    Logger.getLogger(JsonImporter.class.getName())
                        .log(Level.FINE, "Failed to import book entry: " + e.getMessage(), e);
                }
            }
        }
        return new ResultatImportacio(ok, skipped, err, errDetails);
    }
}
