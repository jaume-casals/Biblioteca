package domini.facade;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import domini.BibliotecaException;
import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.ServeiCopiaSeguretat;

/**
 * Backup, restore, and clear-all. Touches all three state lists; the
 * snapshot is taken atomically under the state lock; the file I/O runs
 * outside the lock (the original code did the same — I/O is not held
 * against concurrent state reads).
 */
public final class DelegatCopiaSeguretat {

    private final StateContext state;
    private final ServeiCopiaSeguretat copiaSegService;

    public DelegatCopiaSeguretat(StateContext state, ServeiCopiaSeguretat copiaSegService) {
        this.state = state;
        this.copiaSegService = copiaSegService;
    }

    /**
     * Serialitza la biblioteca a un fitxer SQL.
     *
     * <p>Ordre d'operacions (intencionat):
     * <ol>
     *   <li>Precàrrega els camps pesats ({@code descripcio}, {@code notes})
     *       de cada llibre en un sol viatge agrupat ABANS de la captura —
     *       la implementació anterior feia un viatge per llibre (O(n)
     *       crides a la BBDD + O(n) adquisicions de lock), cosa que en
     *       una biblioteca de 5 000 llibres trigava uns quants segons
     *       i bloquejava la resta de la façana durant tota l'operació.</li>
     *   <li>Pren una captura atòmica de {@code bib}, {@code llistes},
     *       {@code tags} sota el lock (còpies defensives; les mutacions
     *       posteriors a la captura no afecten la còpia).</li>
     *   <li>Lliura la captura a {@link BackupService#backupToSQL} sense
     *       tenir el lock agafat.</li>
     * </ol>
     */
    public void copiaSegToSQL(File file) {
        preLoadHeavyFieldsBatched();
        ArrayList<Llibre> bibSnapshot;
        ArrayList<Llista> llistesSnapshot;
        ArrayList<Tag> tagsSnapshot;
        synchronized (state.lock()) {
            bibSnapshot = new ArrayList<>(state.bib());
            llistesSnapshot = new ArrayList<>(state.llistes());
            tagsSnapshot = new ArrayList<>(state.tags());
        }
        try { copiaSegService.copiaSegToSQL(file, bibSnapshot, llistesSnapshot, tagsSnapshot); }
        catch (Exception e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    /**
     * Precàrrega {@code descripcio} i {@code notes} de cada llibre que
     * encara no els té en memòria. Fa servir un sol viatge agrupat (sense
     * N+1): una sola crida {@code SELECT isbn, descripcio, notes FROM
     * llibre WHERE isbn IN (?, ?, ...)}, i després muta cada Llibre
     * in situ pel camí lleuger de {@code loadHeavyFields}. Es salten
     * els llibres els camps pesats dels quals ja estan carregats (la
     * instància en memòria és fresca i coincideix amb la BBDD).
     */
    private void preLoadHeavyFieldsBatched() {
        // Captura els parells (ISBN → Llibre en memòria) que cal carregar.
        // El mapa és de només lectura des de la perspectiva del consumidor;
        // les referències a Llibre són mutables in situ, però aquesta
        // mutació és la FINALITAT de la càrrega. Les mutacions concurrents
        // durant la finestra del SELECT són acceptables (el mateix raonament
        // que el codi previ al bucle que això substitueix: la captura a
        // backupToSQL es fa tot seguit i veu o bé la vista amb camps
        // pesats o bé la lleugera, totes dues autoconsistents).
        java.util.Map<Long, Llibre> targets;
        synchronized (state.lock()) {
            targets = new java.util.HashMap<>();
            for (Llibre l : state.bib()) {
                if (!l.esHeavyFieldsLoaded()) targets.put(l.obtenirISBN(), l);
            }
        }
        if (targets.isEmpty()) return;
        try {
            state.persistence().carregarHeavyFieldsBatched(new ArrayList<>(targets.keySet()), targets::get);
        } catch (RuntimeException e) {
            // Best-effort: si la càrrega agrupada falla (microtall de
            // xarxa, problema de permisos), la còpia de seguretat
            // continuarà amb files lleugeres. BackupService.writeLlibreINSERT
            // emetrà descripcio/notes buides o nul·les per a aquests
            // llibres, cosa que constitueix una degradació suau.
            java.util.logging.Logger.getLogger(DelegatCopiaSeguretat.class.getName())
                .warning("Ha fallat la precàrrega agrupada de camps pesats; la còpia utilitzarà files lleugeres: " + e.getMessage());
        }
    }

    public void restaurarFromSQL(File file) {
        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File backup = new File(tmpDir, "biblioteca_restore_backup_" + System.currentTimeMillis() + ".sql");
            backup.deleteOnExit();
            ArrayList<Llibre> preBib;
            ArrayList<Llista> preLlistes;
            ArrayList<Tag> preTags;
            // Captura l'estat previ a la restauració sota el lock; el
            // lock es manté només per a les còpies defensives barates,
            // no per a l'escriptura de la còpia de seguretat.
            synchronized (state.lock()) {
                preBib = new ArrayList<>(state.bib());
                preLlistes = new ArrayList<>(state.llistes());
                preTags = new ArrayList<>(state.tags());
            }
            copiaSegService.copiaSegToSQL(backup, preBib, preLlistes, preTags);
            try {
                // Una sola transacció JDBC envolta clearAllData i
                // executeSQLFile. Si falla l'execució del fitxer, també
                // es reverteixen les sentències DELETE, de manera que
                // la base de dades queda intacta. El fitxer SQL de pre-
                // restauració es conserva com a camí d'undoing visible
                // per a l'usuari; si el propi rollback JDBC falla
                // (driver/connexió caiguda), el fitxer és l'últim recurs.
                state.persistence().restaurarFromSQLFile(file);
            } catch (Exception e) {
                try { state.persistence().executarSQLFile(backup); }
                catch (Exception ex) { throw new BibliotecaException(
                    herramienta.I18n.t("dlg_restore_rollback_failed", backup.getAbsolutePath())
                    + "\n\nOriginal error: " + e.getMessage()
                    + "\nRollback error: " + ex.getMessage(), ex); }
                throw new BibliotecaException("Restore failed: " + e.getMessage(), e);
            }
        } catch (Exception e) { throw new BibliotecaException(e.getMessage(), e); }
        // La captura post-restauració es fa SENSE el lock d'estat — les
        // tres crides JDBC getAll*() poden trigar segons en una
        // biblioteca gran acabada de restaurar, i mantenir el lock tot
        // el temps congelaria totes les altres crides de la façana
        // (filtre, cerca, afegir prestatgeria) fins que acabés la
        // restauració. El replaceAll() següent publica atòmicament
        // mitjançant synchronized (lock) dins del StateContext, de
        // manera que l'única finestra on el nou estat és observable
        // és la instrucció dins de replaceAll.
        ArrayList<Llibre> newBib = new ArrayList<>(state.persistence().obtenirAllLlibres());
        if (newBib.isEmpty()) throw new BibliotecaException("Restore completat però no s'han carregat llibres — el fitxer pot estar buit o corrupte");
        Collections.sort(newBib, DelegatLlibre.ISBN_COMPARATOR);
        ArrayList<Llista> newLlistes = new ArrayList<>(state.persistence().obtenirAllLlistes());
        ArrayList<Tag> newTags = new ArrayList<>(state.persistence().obtenirAllTags());
        state.replaceAll(newBib, newLlistes, newTags);
    }

    public void netejarAll() {
        try { state.persistence().netejarAllData(); }
        catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.netejarAll();
    }
}
