package herramienta.io.csv;

import domini.Llista;
import persistencia.contract.EscritorBiblioteca;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpers per crear i reutilitzar prestatges durant la importació de CSV.
 * Extret de la duplicació entre {@link GoodreadsCsvStrategy} i
 * {@link LibraryThingCsvStrategy} — ambdós fan exactament el mateix
 * recorregut: "per cada nom, troba'l o crea'l; afegeix el llibre".
 */
public final class ShelvesHelper {
    private ShelvesHelper() {}

    /**
     * Troba un prestatge pel nom donat o el crea si no existeix.
     * La primera crida amb {@code cache == null} pobla la cache
     * amb totes les llistes existents; les crides següents
     * aprofiten la cache per a cerques O(1).
     *
     * @param cd        lector/escriptor de domini
     * @param cache     cache opcional per evitar re-cerca a cada iteració;
     *                  si és null, es construeix a partir de {@code cd.getAllLlistes()}
     * @param shelfName nom del prestatge; si és null o blank, retorna null
     * @return la {@link Llista} trobada o creada, o null si {@code shelfName} és buit
     */
    public static Llista cercarOCrearPrestatge(EscritorBiblioteca cd, Map<String, Llista> cache, String shelfName) {
        if (shelfName == null || shelfName.isBlank()) return null;
        if (cache == null) {
            cache = new HashMap<>();
            for (Llista ll : cd.obtenirAllLlistes()) cache.put(ll.obtenirNom(), ll);
        }
        Llista llista = cache.get(shelfName);
        if (llista == null) {
            llista = cd.afegirLlista(shelfName);
            cache.put(shelfName, llista);
        }
        return llista;
    }

    /**
     * Crea o recupera un prestatge pel nom donat i hi afegeix el llibre.
     *
     * @param cd        lector/escriptor de domini
     * @param cache     cache opcional per evitar re-cerca a cada iteració;
     *                  si és null, es construeix a partir de {@code cd.getAllLlistes()}
     * @param isbn      ISBN del llibre a afegir al prestatge
     * @param shelfName nom del prestatge
     * @param valoracio valoració dins del prestatge
     * @param llegit    estat de lectura dins del prestatge
     */
    public static void afegirLlibreAPrestatge(EscritorBiblioteca cd, Map<String, Llista> cache,
                                      long isbn, String shelfName,
                                      double valoracio, boolean llegit) {
        Llista llista = cercarOCrearPrestatge(cd, cache, shelfName);
        if (llista == null) return;
        cd.afegirLlibreToLlista(isbn, llista.obtenirId(), valoracio, llegit);
    }

    /**
     * Divideix {@code raw} per {@code separador} i, per a cada nom no buit, crea o
     * recupera el prestatge i hi afegeix el llibre. No fa res si {@code raw} és buit.
     */
    public static void afegirLlibreAPrestatges(EscritorBiblioteca cd, Map<String, Llista> cache,
                                       long isbn, String raw, String separador,
                                       double valoracio, boolean llegit) {
        if (raw == null || raw.isEmpty()) return;
        for (String s : raw.split(separador)) {
            afegirLlibreAPrestatge(cd, cache, isbn, s.trim(), valoracio, llegit);
        }
    }
}
