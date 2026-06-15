package interficie;

import domini.Llibre;
import domini.Llista;
import domini.Tag;

/**
 * CRUD surface of the library. The three danger-zone admin operations
 * (backup / restore / clearAll) are declared on the
 * {@link BibliotecaAdmin} sub-interface; this interface extends it for
 * backward-compatibility (per the tot.txt MEDIUM finding) but the
 * canonical way to obtain an admin handle is to require
 * {@code BibliotecaAdmin} in the constructor — most consumers do not
 * need those operations and should not have them in scope.
 *
 * <p>The "extending BibliotecaAdmin" half of the split is the
 * compatibility fix: legacy code that calls
 * {@code cd.backupToSQL(f)} on a {@code BibliotecaWriter} continues to
 * compile. New code that needs the admin operations should declare the
 * field as {@code BibliotecaAdmin} (the narrower type) so the type
 * system flags any caller that does not need to be trusted with
 * danger-zone operations.
 */
public interface BibliotecaWriter extends BibliotecaReader, BibliotecaAdmin {

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
}
