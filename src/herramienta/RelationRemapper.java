package herramienta;

import domini.Llista;
import domini.Tag;
import interficie.BibliotecaWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps incoming external IDs (from CSV/JSON exports) to the locally-assigned IDs of shelves
 * and tags, creating new entries when no name match exists. Replaces duplicated remap loops in
 * {@code ImportExportRouter.importJson} and {@code BookImporter.importJSON}.
 */
public final class RelationRemapper {

    public static final class ShelfIdRemapper {
        private final BibliotecaWriter cd;
        private final Map<String, Integer> byNom = new HashMap<>();
        public ShelfIdRemapper(BibliotecaWriter cd) {
            this.cd = cd;
            for (Llista l : cd.getAllLlistes()) byNom.put(l.getNom(), l.getId());
        }
        public int resolve(String name) {
            Integer cached = byNom.get(name);
            if (cached != null) return cached;
            Llista created = cd.addLlista(name);
            byNom.put(name, created.getId());
            return created.getId();
        }
    }

    public static final class TagIdRemapper {
        private final BibliotecaWriter cd;
        private final Map<String, Integer> byNom = new HashMap<>();
        public TagIdRemapper(BibliotecaWriter cd) {
            this.cd = cd;
            for (Tag t : cd.getAllTags()) byNom.put(t.getNom(), t.getId());
        }
        public int resolve(String name) {
            Integer cached = byNom.get(name);
            if (cached != null) return cached;
            Tag created = cd.addTag(name);
            byNom.put(name, created.getId());
            return created.getId();
        }
    }

    private RelationRemapper() {}
}
