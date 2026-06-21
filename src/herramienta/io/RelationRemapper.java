package herramienta.io;

import domini.Llista;
import domini.Tag;
import persistencia.contract.EscritorPrestatgeria;
import persistencia.contract.EscritorEtiqueta;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapeja IDs externs entrants (d'exportacions CSV/JSON) als IDs assignats
 * localment de prestatgeries i etiquetes, creant noves entrades quan no
 * existeix cap coincidència per nom. Substitueix els bucles de remapeig
 * duplicats a {@code ImportExportRouter.importJson} i
 * {@code ImportadorLlibres.importJSON}.
 */
public final class RelationRemapper {

    public static final class RemapejadorIdPrestatgeria {
        private final EscritorPrestatgeria cd;
        private final Map<String, Integer> byNom = new HashMap<>();
        public RemapejadorIdPrestatgeria(EscritorPrestatgeria cd) {
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
        private final EscritorEtiqueta cd;
        private final Map<String, Integer> byNom = new HashMap<>();
        public RemapejadorIdEtiqueta(EscritorEtiqueta cd) {
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
