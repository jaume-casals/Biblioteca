package interficie;

import domini.Llista;

public interface EscritorPrestatgeria extends LectorPrestatgeria {
    Llista afegirLlista(String nom);
    void eliminarLlista(Llista llista);
    void reanomenarLlista(int id, String newNom);
    void afegirLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void eliminarLlibreFromLlista(long isbn, int llistaId);
    void actualitzarLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void moureLlistaUp(int id);
    void moureLlistaDown(int id);
    void posarLlistaColor(int id, String color);
}
