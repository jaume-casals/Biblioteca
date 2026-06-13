package domini.facade;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import domini.BibliotecaException;
import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.BackupService;

/**
 * Backup, restore, and clear-all. Touches all three state lists; the
 * snapshot is taken atomically under the state lock; the file I/O runs
 * outside the lock (the original code did the same — I/O is not held
 * against concurrent state reads).
 */
public final class BackupDelegate {

    private final StateContext state;
    private final BackupService backupService;

    public BackupDelegate(StateContext state, BackupService backupService) {
        this.state = state;
        this.backupService = backupService;
    }

    /**
     * Serialise the library to a SQL file.
     *
     * <p>Order of operations (intentional):
     * <ol>
     *   <li>Pre-load heavy fields ({@code descripcio}, {@code notes},
     *       blob) for every book BEFORE the snapshot, OUTSIDE the lock —
     *       each {@code loadHeavyFields} does a DB round-trip and the
     *       I/O must not block the rest of the controller.</li>
     *   <li>Take an atomic snapshot of {@code bib}, {@code llistes},
     *       {@code tags} under the lock (defensive copies, post-snapshot
     *       mutations do not affect the copy).</li>
     *   <li>Hand the snapshot to {@link BackupService#backupToSQL} with
     *       no lock held.</li>
     * </ol>
     */
    public void backupToSQL(File file) {
        for (Llibre l : state.bib()) {
            if (!l.isHeavyFieldsLoaded()) {
                state.withLock(() -> state.persistence().loadHeavyFields(l.getISBN(), l));
            }
        }
        ArrayList<Llibre> bibSnapshot;
        ArrayList<Llista> llistesSnapshot;
        ArrayList<Tag> tagsSnapshot;
        synchronized (state.lock()) {
            bibSnapshot = new ArrayList<>(state.bib());
            llistesSnapshot = new ArrayList<>(state.llistes());
            tagsSnapshot = new ArrayList<>(state.tags());
        }
        try { backupService.backupToSQL(file, bibSnapshot, llistesSnapshot, tagsSnapshot); }
        catch (Exception e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void restoreFromSQL(File file) {
        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File backup = new File(tmpDir, "biblioteca_restore_backup_" + System.currentTimeMillis() + ".sql");
            backup.deleteOnExit();
            ArrayList<Llibre> preBib;
            ArrayList<Llista> preLlistes;
            ArrayList<Tag> preTags;
            synchronized (state.lock()) {
                preBib = new ArrayList<>(state.bib());
                preLlistes = new ArrayList<>(state.llistes());
                preTags = new ArrayList<>(state.tags());
            }
            backupService.backupToSQL(backup, preBib, preLlistes, preTags);
            try {
                state.persistence().clearAllData();
                state.persistence().executeSQLFile(file);
            } catch (Exception e) {
                try { state.persistence().executeSQLFile(backup); }
                catch (Exception ex) { throw new BibliotecaException(
                    herramienta.I18n.t("dlg_restore_rollback_failed", backup.getAbsolutePath())
                    + "\n\nOriginal error: " + e.getMessage()
                    + "\nRollback error: " + ex.getMessage(), ex); }
                throw new BibliotecaException("Restore failed: " + e.getMessage(), e);
            }
        } catch (Exception e) { throw new BibliotecaException(e.getMessage(), e); }
        ArrayList<Llibre> newBib;
        ArrayList<Llista> newLlistes;
        ArrayList<Tag> newTags;
        synchronized (state.lock()) {
            newBib = new ArrayList<>(state.persistence().getAllLlibres());
            if (newBib.isEmpty()) throw new BibliotecaException("Restore completat però no s'han carregat llibres — el fitxer pot estar buit o corrupte");
            Collections.sort(newBib, BookDelegate.ISBN_COMPARATOR);
            newLlistes = new ArrayList<>(state.persistence().getAllLlistes());
            newTags = new ArrayList<>(state.persistence().getAllTags());
        }
        state.replaceAll(newBib, newLlistes, newTags);
    }

    public void clearAll() {
        try { state.persistence().clearAllData(); }
        catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.clearAll();
    }
}
