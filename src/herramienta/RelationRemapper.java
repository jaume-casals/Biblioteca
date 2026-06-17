package herramienta;

import domini.Llista;
import domini.Tag;
import interficie.ShelfWriter;
import interficie.TagWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps incoming external IDs (from CSV/JSON exports) to the locally-assigned IDs of shelves
 * and tags, creating new entries when no name match exists. Replaces duplicated remap loops in
 * {@code ImportExportRouter.importJson} and {@code BookImporter.importJSON}.
 */
public final class RelationRemapper {

    public static final class RemapejadorIdPrestatgeria {
        private final ShelfWriter cd;
        private final Map<String, Integer> byNom = new HashMap<>();
        public RemapejadorIdPrestatgeria(ShelfWriter cd) {
            this.cd = cd;
            for (Llista l : cd.obtenirAllLlistes()) byNom.put(l.obtenirNom(), l.obtenirId());
        }
        public int resolve(String name) {
            Integer cached = byNom.get(name);
            if (cached != null) return cached;
            Llista created = cd.afegirLlista(name);
            byNom.put(name, created.obtenirId());
            return created.obtenirId();
        }
    }

    public static final class RemapejadorIdEtiqueta {
        private final TagWriter cd;
        private final Map<String, Integer> byNom = new HashMap<>();
        public RemapejadorIdEtiqueta(TagWriter cd) {
            this.cd = cd;
            for (Tag t : cd.obtenirAllTags()) byNom.put(t.obtenirNom(), t.obtenirId());
        }
        public int resolve(String name) {
            Integer cached = byNom.get(name);
            if (cached != null) return cached;
            Tag created = cd.afegirTag(name);
            byNom.put(name, created.obtenirId());
            return created.obtenirId();
        }
    }

    private RelationRemapper() {}
}
