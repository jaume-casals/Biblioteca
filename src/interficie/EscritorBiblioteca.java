package interficie;

import domini.Llibre;
import domini.Llista;
import domini.Tag;

/**
 * CRUD surface of the library. The three danger-zone admin operations
 * (backup / restore / clearAll) are declared on the
 * {@link AdministradorBiblioteca} sub-interface; this interface extends it for
 * backward-compatibility (per the tot.txt MEDIUM finding) but the
 * canonical way to obtain an admin handle is to require
 * {@code AdministradorBiblioteca} in the constructor — most consumers do not
 * need those operations and should not have them in scope.
 *
 * <p>The "extending AdministradorBiblioteca" half of the split is the
 * compatibility fix: legacy code that calls
 * {@code cd.backupToSQL(f)} on a {@code EscritorBiblioteca} continues to
 * compile. New code that needs the admin operations should declare the
 * field as {@code AdministradorBiblioteca} (the narrower type) so the type
 * system flags any caller that does not need to be trusted with
 * danger-zone operations.
 */
public interface EscritorBiblioteca extends LectorBiblioteca, AdministradorBiblioteca {

    void afegirLlibre(Llibre l);
    void eliminarLlibre(Llibre l);
    void eliminarLlibre(Long isbn);
    void actualitzarLlibre(Llibre l);

    Llista afegirLlista(String nom);
    void eliminarLlista(Llista llista);
    void reanomenarLlista(int id, String newNom);
    void afegirLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void eliminarLlibreFromLlista(long isbn, int llistaId);
    void actualitzarLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit);
    void moureLlistaUp(int id);
    void moureLlistaDown(int id);
    void posarLlistaColor(int id, String color);

    Tag afegirTag(String nom);
    void eliminarTag(Tag tag);
    void reanomenarTag(int id, String newNom);
    void afegirLlibreToTag(long isbn, int tagId);
    void eliminarLlibreFromTag(long isbn, int tagId);

    void prestarLlibre(long isbn, String nom);
    void retornarLlibre(long isbn);

    void posarLlibreBlob(long isbn, byte[] blob);
}
