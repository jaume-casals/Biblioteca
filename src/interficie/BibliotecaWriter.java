package interficie;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import java.io.File;

public interface BibliotecaWriter extends BibliotecaReader {

    void addLlibre(Llibre l) throws Exception;
    void deleteLlibre(Llibre l) throws Exception;
    void deleteLlibre(Long isbn) throws Exception;
    void updateLlibre(Llibre l) throws Exception;

    Llista addLlista(String nom) throws Exception;
    void deleteLlista(Llista llista) throws Exception;
    void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws Exception;
    void removeLlibreFromLlista(long isbn, int llistaId) throws Exception;
    void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws Exception;
    void moveLlistaUp(int id) throws Exception;
    void moveLlistaDown(int id) throws Exception;
    void setLlistaColor(int id, String color) throws Exception;

    Tag addTag(String nom) throws Exception;
    void deleteTag(Tag tag) throws Exception;
    void addLlibreToTag(long isbn, int tagId) throws Exception;
    void removeLlibreFromTag(long isbn, int tagId) throws Exception;

    void prestarLlibre(long isbn, String nom) throws Exception;
    void retornarLlibre(long isbn) throws Exception;

    void setLlibreBlob(long isbn, byte[] blob) throws java.sql.SQLException;

    void clearAll() throws Exception;
    void backupToSQL(File file) throws Exception;
    void restoreFromSQL(File file) throws Exception;
}
