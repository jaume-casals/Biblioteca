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

    /** Base de resolució nom→id amb cache i creació diferida. */
    public abstract static class RemapejadorId {
        private final Map<String, Integer> byNom = new HashMap<>();
        protected void seed(String nom, int id) { byNom.put(nom, id); }
        protected abstract int crear(String name);
        public int resolve(String name) {
            Integer cached = byNom.get(name);
            if (cached != null) return cached;
            int id = crear(name);
            byNom.put(name, id);
            return id;
        }
    }

    public static final class RemapejadorIdPrestatgeria extends RemapejadorId {
        private final EscritorPrestatgeria cd;
        public RemapejadorIdPrestatgeria(EscritorPrestatgeria cd) {
            this.cd = cd;
            for (Llista l : cd.obtenirAllLlistes()) seed(l.obtenirNom(), l.obtenirId());
        }
        @Override protected int crear(String name) { return cd.afegirLlista(name).obtenirId(); }
    }

    public static final class RemapejadorIdEtiqueta extends RemapejadorId {
        private final EscritorEtiqueta cd;
        public RemapejadorIdEtiqueta(EscritorEtiqueta cd) {
            this.cd = cd;
            for (Tag t : cd.obtenirAllTags()) seed(t.obtenirNom(), t.obtenirId());
        }
        @Override protected int crear(String name) { return cd.afegirTag(name).obtenirId(); }
    }

    private RelationRemapper() {}
}
