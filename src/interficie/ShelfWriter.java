package interficie;

import domini.Llista;

public interface ShelfWriter extends ShelfReader {
    Llista addLlista(String nom);
    void deleteLlista(Llista llista);
    void renameLlista(int id, String newNom);
    void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void removeLlibreFromLlista(long isbn, int llistaId);
    void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void moveLlistaUp(int id);
    void moveLlistaDown(int id);
    void setLlistaColor(int id, String color);
}
