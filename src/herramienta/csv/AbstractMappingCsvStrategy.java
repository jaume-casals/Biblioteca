package herramienta.csv;

import domini.Llista;
import domini.Tag;
import interficie.BibliotecaWriter;

import java.util.HashMap;
import java.util.Map;

abstract class AbstractMappingCsvStrategy implements CsvImportStrategy {
    @Override public abstract String getName();
    private Map<String, Llista> shelfMap;
    private Map<String, Tag>    tagMap;

    protected Llista resolveShelf(BibliotecaWriter cd, String name) {
        if (shelfMap == null) shelfMap = buildShelfMap(cd);
        Llista existing = shelfMap.get(name);
        if (existing != null) return existing;
        Llista created = cd.addLlista(name);
        shelfMap.put(name, created);
        return created;
    }

    protected Tag resolveTag(BibliotecaWriter cd, String name) {
        if (tagMap == null) {
            tagMap = new HashMap<>();
            for (Tag t : cd.getAllTags()) tagMap.put(t.getNom(), t);
        }
        Tag existing = tagMap.get(name);
        if (existing != null) return existing;
        Tag created = cd.addTag(name);
        tagMap.put(name, created);
        return created;
    }

    private static Map<String, Llista> buildShelfMap(BibliotecaWriter cd) {
        Map<String, Llista> map = new HashMap<>();
        for (Llista ll : cd.getAllLlistes()) map.put(ll.getNom(), ll);
        return map;
    }
}