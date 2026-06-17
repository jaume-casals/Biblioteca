package herramienta.csv;

import domini.Llista;
import domini.Tag;
import interficie.BibliotecaWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

abstract class AbstractMappingCsvStrategy implements CsvImportStrategy {
    @Override public abstract String getName();
    private Map<String, Llista> shelfMap;
    private Map<String, Tag>    tagMap;

    protected Llista resolveShelf(BibliotecaWriter cd, String name) {
        return resolveOrCreate(cd, name, cd::obtenirAllLlistes, Llista::obtenirNom, cd::afegirLlista, this.shelfMap,
            m -> { this.shelfMap = m; });
    }

    protected Tag resolveTag(BibliotecaWriter cd, String name) {
        return resolveOrCreate(cd, name, cd::obtenirAllTags, Tag::obtenirNom, cd::afegirTag, this.tagMap,
            m -> { this.tagMap = m; });
    }

    /**
     * Generic "find or create by name" lookup. Caches the full table on
     * first call so subsequent lookups are O(1) hash hits. The cache is
     * held in the strategy so the same map is reused across rows.
     */
    private static <E> E resolveOrCreate(BibliotecaWriter cd,
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