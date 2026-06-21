package persistencia.contract;

import domini.Llibre;

public interface EscritorLlibre extends LectorLlibre {
    void afegirLlibre(Llibre l);
    /** @deprecated prefer {@link #eliminarLlibre(Long)} so callers carry only the key, not the full record. */
    @Deprecated
    void eliminarLlibre(Llibre l);
    void eliminarLlibre(Long isbn);
    void actualitzarLlibre(Llibre l);
    void posarLlibreBlob(long isbn, byte[] blob);
}
