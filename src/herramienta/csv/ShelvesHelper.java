package herramienta.csv;

import domini.Llista;
import interficie.BibliotecaWriter;
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
    public static void addBookToShelf(BibliotecaWriter cd, Map<String, Llista> cache,
                                      long isbn, String shelfName,
                                      double valoracio, boolean llegit) {
        if (shelfName == null || shelfName.isBlank()) return;
        if (cache == null) {
            cache = new HashMap<>();
            for (Llista ll : cd.getAllLlistes()) cache.put(ll.getNom(), ll);
        }
        Llista llista = cache.get(shelfName);
        if (llista == null) {
            llista = cd.addLlista(shelfName);
            cache.put(shelfName, llista);
        }
        cd.addLlibreToLlista(isbn, llista.getId(), valoracio, llegit);
    }
}
