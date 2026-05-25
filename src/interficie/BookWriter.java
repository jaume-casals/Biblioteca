package interficie;

import domini.Llibre;

public interface BookWriter extends BookReader {
    void addLlibre(Llibre l);
    /** @deprecated prefer {@link #deleteLlibre(Long)} so callers carry only the key, not the full record. */
    @Deprecated
    void deleteLlibre(Llibre l);
    void deleteLlibre(Long isbn);
    void updateLlibre(Llibre l);
    void setLlibreBlob(long isbn, byte[] blob);
}
