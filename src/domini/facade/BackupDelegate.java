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
     *   <li>Pre-load heavy fields ({@code descripcio}, {@code notes})
     *       for every book in a single batched round-trip BEFORE the
     *       snapshot — the previous implementation did one round-trip
     *       per book (O(n) DB calls + O(n) lock acquisitions), which
     *       on a 5 000-book library took several seconds and stalled
     *       the rest of the facade for the whole duration.</li>
     *   <li>Take an atomic snapshot of {@code bib}, {@code llistes},
     *       {@code tags} under the lock (defensive copies, post-snapshot
     *       mutations do not affect the copy).</li>
     *   <li>Hand the snapshot to {@link BackupService#backupToSQL} with
     *       no lock held.</li>
     * </ol>
     */
    public void backupToSQL(File file) {
        preLoadHeavyFieldsBatched();
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

    /**
     * Pre-load {@code descripcio} and {@code notes} for every book that
     * doesn't already have them in memory. Uses a single batched
     * round-trip (no N+1): one {@code SELECT isbn, descripcio, notes
     * FROM llibre WHERE isbn IN (?, ?, ...)} call, then mutate each
     * Llibre in place via the light {@code loadHeavyFields} path. Books
     * whose heavy fields are already loaded are skipped (the in-memory
     * instance is fresh and matches the DB).
     */
    private void preLoadHeavyFieldsBatched() {
        // Snapshot the (ISBN → in-memory Llibre) pairs we need to load.
        // The map is read-only from the caller's perspective; the Llibre
        // references are mutable in place but that mutation is what the
        // load is FOR. Concurrent mutations during the SELECT window are
        // acceptable (the same reasoning as the pre-loop code that this
        // replaces: the snapshot in backupToSQL is taken right after and
        // either sees the heavy-loaded or light view, both of which are
        // self-consistent).
        java.util.Map<Long, Llibre> targets;
        synchronized (state.lock()) {
            targets = new java.util.HashMap<>();
            for (Llibre l : state.bib()) {
                if (!l.isHeavyFieldsLoaded()) targets.put(l.getISBN(), l);
            }
        }
        if (targets.isEmpty()) return;
        try {
            state.persistence().loadHeavyFieldsBatched(new ArrayList<>(targets.keySet()), targets::get);
        } catch (RuntimeException e) {
            // Best-effort: if the batched load fails (network blip,
            // permission issue), the backup will still run with light
            // rows. The BackupService.writeLlibreINSERT will emit
            // null/empty descripcio and notes for those books, which is
            // a graceful degradation.
            java.util.logging.Logger.getLogger(BackupDelegate.class.getName())
                .warning("Batched heavy-field pre-load failed; backup will use light rows: " + e.getMessage());
        }
    }

    public void restoreFromSQL(File file) {
        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File backup = new File(tmpDir, "biblioteca_restore_backup_" + System.currentTimeMillis() + ".sql");
            backup.deleteOnExit();
            ArrayList<Llibre> preBib;
            ArrayList<Llista> preLlistes;
            ArrayList<Tag> preTags;
            // Snapshot the pre-restore state under the lock; the lock is
            // held only for the cheap defensive copies, not for the
            // backup write itself.
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
        // Post-restore snapshot is taken WITHOUT the state lock — the
        // three JDBC getAll*() calls can take seconds on a large
        // restored library, and holding the lock the whole time would
        // freeze every other delegate call (filter, search, shelf
        // add) until the restore finishes. The replaceAll() below
        // publishes atomically via synchronized (lock) inside the
        // StateContext, so the only window where the new state is
        // observable is the one statement inside replaceAll.
        ArrayList<Llibre> newBib = new ArrayList<>(state.persistence().getAllLlibres());
        if (newBib.isEmpty()) throw new BibliotecaException("Restore completat però no s'han carregat llibres — el fitxer pot estar buit o corrupte");
        Collections.sort(newBib, BookDelegate.ISBN_COMPARATOR);
        ArrayList<Llista> newLlistes = new ArrayList<>(state.persistence().getAllLlistes());
        ArrayList<Tag> newTags = new ArrayList<>(state.persistence().getAllTags());
        state.replaceAll(newBib, newLlistes, newTags);
    }

    public void clearAll() {
        try { state.persistence().clearAllData(); }
        catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.clearAll();
    }
}
