package interficie;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import java.io.File;

public interface BibliotecaWriter extends BibliotecaReader {

    void addLlibre(Llibre l);
    void deleteLlibre(Llibre l);
    void deleteLlibre(Long isbn);
    void updateLlibre(Llibre l);

    Llista addLlista(String nom);
    void deleteLlista(Llista llista);
    void renameLlista(int id, String newNom);
    void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void removeLlibreFromLlista(long isbn, int llistaId);
    void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void moveLlistaUp(int id);
    void moveLlistaDown(int id);
    void setLlistaColor(int id, String color);

    Tag addTag(String nom);
    void deleteTag(Tag tag);
    void renameTag(int id, String newNom);
    void addLlibreToTag(long isbn, int tagId);
    void removeLlibreFromTag(long isbn, int tagId);

    void prestarLlibre(long isbn, String nom);
    void retornarLlibre(long isbn);

    void setLlibreBlob(long isbn, byte[] blob);

    void clearAll();
    void backupToSQL(File file);
    void restoreFromSQL(File file);
}
