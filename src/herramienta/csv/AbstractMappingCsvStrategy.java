package herramienta.csv;

import domini.Llista;
import domini.Tag;
import interficie.EscritorBiblioteca;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

abstract class AbstractMappingCsvStrategy implements CsvImportStrategy {
    @Override public abstract String obtenirNom();
    private Map<String, Llista> shelfMap;
    private Map<String, Tag>    tagMap;

    protected Llista resolveShelf(EscritorBiblioteca cd, String name) {
        return resolveOrCreate(cd, name, cd::obtenirAllLlistes, Llista::obtenirNom, cd::afegirLlista, this.shelfMap,
            m -> { this.shelfMap = m; });
    }

    protected Tag resolveTag(EscritorBiblioteca cd, String name) {
        return resolveOrCreate(cd, name, cd::obtenirAllTags, Tag::obtenirNom, cd::afegirTag, this.tagMap,
            m -> { this.tagMap = m; });
    }

    /**
     * Cerca genèrica "troba o crea per nom". Emmagatzema en caché la taula
     * sencera a la primera crida de manera que les cerques següents siguin
     * encerts O(1) de hash. La caché es manté a l'estratègia perquè el
     * mateix mapa es reutilitzi entre files.
     */
    private static <E> E resolveOrCreate(EscritorBiblioteca cd,
                                         String name,
                                         java.util.function.Supplier<java.util.List<E>> all,
                                         Function<E, String> nameOf,
                                         java.util.function.Function<String, E> add,
                                         Map<String, E> cache,
                                         java.util.function.Consumer<Map<String, E>> cacheSink) {
        if (cache == null) {
            Map<String, E> m = new HashMap<>();
            for (E e : all.get()) m.put(nameOf.apply(e), e);
            cacheSink.accept(m);
            cache = m;
        }
        E existing = cache.get(name);
        if (existing != null) return existing;
        E created = add.apply(name);
        cache.put(name, created);
        return created;
    }
}