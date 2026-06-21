package persistencia.contract;

import domini.Tag;

public interface EscritorEtiqueta extends LectorEtiqueta {
    Tag afegirTag(String nom);
    void eliminarTag(Tag tag);
    void reanomenarTag(int id, String newNom);
    void afegirLlibreToTag(long isbn, int tagId);
    void eliminarLlibreFromTag(long isbn, int tagId);
}
