package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import persistencia.ControladorPersistencia;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** BackupService reads all data from ControladorPersistencia to produce SQL dumps.
 *  Constructed with cp (not cd) because cp is the single source of truth for backup data —
 *  all rows come from DAO queries, not the in-memory cache. This avoids a desync where
 *  cd.allLlibres() might lag behind cp.getAllLlibres() after recent mutations.
 *
 *  <p><b>Cover images are NOT included in the SQL backup.</b> The {@code llibre.imatge_blob}
 *  column is intentionally excluded (covers can be tens to hundreds of KB each, and the
 *  SQL format would balloon 10-100x). After a restore, the user must re-fetch covers via
 *  the {@code btnFetchCovers} ("Cerca a Internet") action. This is documented to the user
 *  in the restore-done dialog via the {@code dlg_restore_done_cover_note} i18n key. */
public class BackupService {

    private static final Logger LOG = Logger.getLogger(BackupService.class.getName());

    private final ControladorPersistencia cp;
    private final java.util.concurrent.atomic.AtomicBoolean schedulerStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
    private java.util.concurrent.ScheduledExecutorService scheduler;

    /** Seconds between auto-backup runs (24 hours). */
    private static final long AUTO_BACKUP_INTERVAL_S = 86_400;

    public BackupService(ControladorPersistencia cp) {
        this.cp = cp;
    }

    public void scheduleAutoBackup() {
        if (!schedulerStarted.compareAndSet(false, true)) return;
        scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auto-backup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::autoBackup, 30, AUTO_BACKUP_INTERVAL_S, java.util.concurrent.TimeUnit.SECONDS);
        main.ShutdownHooks.register(this::shutdownScheduler);
    }

    public void shutdownScheduler() {
        java.util.concurrent.ScheduledExecutorService local = scheduler;
        if (local != null) {
            // Disable rescheduling before tearing down the executor so a
            // racing scheduleAutoBackup() call cannot create a new
            // scheduler while shutdownNow is in flight (the tot.txt
            // MEDIUM finding on the half-stopped double-scheduler).
            schedulerStarted.set(false);
            local.shutdownNow();
            scheduler = null;
        } else {
            schedulerStarted.set(false);
        }
    }

    private void autoBackup() {
        List<Llibre> bib = cp.getAllLlibres();
        if (bib.isEmpty()) return;
        File dir = Config.getBackupDir();
        if (!dir.exists()) {
            // mkdirs is the rare/IO error case — log and bail (was
            // previously swallowed by the catch-all below; the tot.txt
            // MEDIUM finding flagged the loss of the IOException signal).
            if (!dir.mkdirs()) {
                LOG.log(Level.WARNING, "Backup automàtic: no s'ha pogut crear el directori " + dir);
                return;
            }
        }
        String today = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File[] existing = dir.listFiles((d, n) -> n.startsWith("biblioteca_" + today) && n.endsWith(".sql"));
        if (existing != null && existing.length > 0) return;
        String ts = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File outFile = new File(dir, "biblioteca_" + ts + ".sql");
        // Reserve a unique filename up front so a concurrent prune
        // can't delete the in-progress file. The .tmp suffix marks
        // it as in-flight; we rename to the final name after the
        // backup completes.
        File tmpFile = new File(dir, outFile.getName() + ".tmp");
        try {
            backupToSQL(tmpFile, bib, cp.getAllLlistes(), cp.getAllTags());
        } catch (java.io.IOException ioe) {
            // Per the tot.txt MEDIUM finding on swallowed exceptions:
            // log+continue (this is the scheduled auto-backup; one
            // failure should not poison the next scheduled run).
            LOG.log(Level.WARNING, "Backup automàtic fallat (I/O)", ioe);
            return;
        } catch (java.sql.SQLException sqle) {
            // Propagate DB errors loudly — a corrupt DB affects every
            // operation, not just backups.
            LOG.log(Level.SEVERE, "Backup automàtic fallat (SQL)", sqle);
            throw new RuntimeException("Auto-backup SQL failure", sqle);
        } catch (RuntimeException re) {
            // Propagate unexpected runtime errors (NPE, IllegalState, …).
            // Per the finding: do not catch these silently.
            LOG.log(Level.SEVERE, "Backup automàtic fallat (runtime)", re);
            throw re;
        }
        try {
            java.nio.file.Files.move(tmpFile.toPath(), outFile.toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            try {
                java.nio.file.Files.move(tmpFile.toPath(), outFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException ioe) {
                LOG.log(Level.WARNING, "Backup automàtic: no s'ha pogut moure el .tmp", ioe);
                return;
            }
        } catch (java.io.IOException ioe) {
            LOG.log(Level.WARNING, "Backup automàtic: no s'ha pogut moure el .tmp", ioe);
            return;
        }
        // Prune AFTER rename so a race between prune and a fresh
        // backup run can't delete the just-written file (the .tmp
        // suffix is gone, the file is no longer "in flight").
        File[] backups = dir.listFiles((d, n) -> n.startsWith("biblioteca_") && n.endsWith(".sql"));
        if (backups != null && backups.length > 5) {
            java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < backups.length - 5; i++) backups[i].delete();
        }
    }

    public void backupToSQL(File file, List<Llibre> bib, List<Llista> llistes, List<Tag> tags) throws java.io.IOException, java.sql.SQLException {
        // Column list is the single source of truth — the tot.txt MEDIUM
        // finding on the INSERT printf duplication is closed by deriving
        // the SQL from LlibreFieldBindings (which the new
        // LlibreDaoCore.ColumnSpec source drives).
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("-- Biblioteca backup " + java.time.LocalDate.now());
            for (String t : persistencia.Schema.CLEAR_ORDER) pw.println("DELETE FROM " + t + ";");
            for (Llibre l : bib) {
                writeLlibreINSERT(pw, l);
            }
            for (persistencia.AutorRow row : cp.getAllAutorRows()) {
                writeAutorINSERT(pw, row.id(), row.nom());
            }
            for (persistencia.LlibreAutorRow row : cp.getAllLlibreAutorRows()) {
                writeLlibreAutorINSERT(pw, row.isbn(), row.autorId());
            }
            for (Llista ll : llistes) {
                writeLlistaINSERT(pw, ll);
            }
            for (persistencia.LlibreLlistaRow row : cp.getAllLlibreLlista()) {
                writeLlibreLlistaINSERT(pw, row);
            }
            for (Tag t : tags) {
                writeTagINSERT(pw, t);
            }
            for (persistencia.LlibreTagRow row : cp.getAllLlibreTag()) {
                writeLlibreTagINSERT(pw, row);
            }
            for (persistencia.PrestecRow row : cp.getAllPrestecs()) {
                writePrestecINSERT(pw, row);
            }
            for (persistencia.LecturaRow row : cp.getAllLectures()) {
                writeLecturaINSERT(pw, row);
            }
        }
    }

    private void writeLlibreINSERT(java.io.PrintWriter pw, Llibre l) {
        // The 27 column list + the 27 value list both come from
        // LlibreFieldBindings (the single source of truth for the
        // Llibre INSERT shape). Adding a new column now requires
        // touching only LlibreFieldBindings.COLUMNS_INSERT +
        // forInsert() (per the tot.txt MEDIUM finding on column-
        // list drift). The literal `imatge_blob` column is
        // excluded here because the BackupService does NOT embed
        // cover bytes in the SQL dump (see class Javadoc).
        Object[] vals = persistencia.LlibreFieldBindings.forInsert(l);
        String[] cols = persistencia.LlibreFieldBindings.COLUMNS_INSERT;
        // Find the imatge_blob index in COLUMNS_INSERT and skip it
        // for the SQL output, but keep the corresponding value in
        // place — the dump intentionally drops the blob.
        StringBuilder sb = new StringBuilder(640);
        sb.append("INSERT INTO llibre (");
        boolean first = true;
        for (String c : cols) {
            if ("imatge_blob".equals(c)) continue;
            if (!first) sb.append(',');
            sb.append('`').append(c).append('`');
            first = false;
        }
        sb.append(") VALUES (");
        first = true;
        for (int i = 0; i < cols.length; i++) {
            if ("imatge_blob".equals(cols[i])) continue;
            if (!first) sb.append(',');
            sb.append(formatValue(vals[i]));
            first = false;
        }
        sb.append(");\n");
        pw.print(sb);
    }

    private static String formatValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof persistencia.LlibreFieldBindings.Null) return "NULL";
        if (v instanceof Boolean b) return b.toString();
        if (v instanceof Integer i) return i.toString();
        if (v instanceof Long l) return l.toString();
        if (v instanceof Double d) return String.format(java.util.Locale.ROOT, "%.4f", d);
        if (v instanceof java.sql.Date dt) return "'" + dt.toString() + "'";
        if (v instanceof byte[] bytes) return "NULL /* " + bytes.length + " bytes dropped */";
        // String
        return sqlNullable(v.toString());
    }

    private void writeAutorINSERT(java.io.PrintWriter pw, int id, String nom) {
        pw.print("INSERT INTO autor (`id`,`nom`) VALUES (");
        pw.print(id);
        pw.print(',');
        pw.print(sqlNullable(nom));
        pw.println(");");
    }

    private void writeLlibreAutorINSERT(java.io.PrintWriter pw, long isbn, int autorId) {
        pw.print("INSERT INTO llibre_autor (`isbn`,`autor_id`) VALUES (");
        pw.print(isbn);
        pw.print(',');
        pw.print(autorId);
        pw.println(");");
    }

    private void writeLlistaINSERT(java.io.PrintWriter pw, Llista ll) {
        pw.print("INSERT INTO llista (`id`,`nom`,`ordre`,`color`) VALUES (");
        pw.print(ll.getId());
        pw.print(',');
        pw.print(sqlNullable(ll.getNom()));
        pw.print(',');
        pw.print(ll.getOrdre());
        pw.print(',');
        pw.print(sqlNullable(ll.getColor()));
        pw.println(");");
    }

    private void writeLlibreLlistaINSERT(java.io.PrintWriter pw, persistencia.LlibreLlistaRow row) {
        pw.print("INSERT INTO llibre_llista (`isbn`,`llista_id`,`valoracio`,`llegit`) VALUES (");
        pw.print(row.isbn());
        pw.print(',');
        pw.print(row.llistaId());
        pw.print(',');
        pw.print(String.format(java.util.Locale.ROOT, "%.4f", row.valoracio()));
        pw.print(',');
        pw.print(row.llegit());
        pw.println(");");
    }

    private void writeTagINSERT(java.io.PrintWriter pw, Tag t) {
        pw.print("INSERT INTO tag (`id`,`nom`) VALUES (");
        pw.print(t.getId());
        pw.print(',');
        pw.print(sqlNullable(t.getNom()));
        pw.println(");");
    }

    private void writeLlibreTagINSERT(java.io.PrintWriter pw, persistencia.LlibreTagRow row) {
        pw.print("INSERT INTO llibre_tag (`isbn`,`tag_id`) VALUES (");
        pw.print(row.isbn());
        pw.print(',');
        pw.print(row.tagId());
        pw.println(");");
    }

    private void writePrestecINSERT(java.io.PrintWriter pw, persistencia.PrestecRow row) {
        pw.print("INSERT INTO prestec (`isbn`,`nom_persona`,`data_prestec`,`retornat`) VALUES (");
        pw.print(row.isbn());
        pw.print(',');
        pw.print(sqlNullable(row.nomPersona()));
        pw.print(',');
        pw.print(sqlNullable(row.dataPrestec() != null ? row.dataPrestec().toString() : null));
        pw.print(',');
        pw.print(row.retornat());
        pw.println(");");
    }

    private void writeLecturaINSERT(java.io.PrintWriter pw, persistencia.LecturaRow row) {
        pw.print("INSERT INTO lectura (`isbn`,`data_inici`,`data_fi`,`pagines_llegides`) VALUES (");
        pw.print(row.isbn());
        pw.print(',');
        pw.print(sqlNullable(row.dataInici() == null ? null : row.dataInici().toString()));
        pw.print(',');
        pw.print(sqlNullable(row.dataFi() == null ? null : row.dataFi().toString()));
        pw.print(',');
        pw.print(row.paginesLlegides());
        pw.println(");");
    }

    private static String sqlNullable(String s) {
        if (s == null) return "NULL";
        return "'" + sqlEsc(s) + "'";
    }

    /**
     * Escapa una cadena per incrustar-la dins un literal SQL dins un script
     * {@code .sql} generat per {@link #backupToSQL}. No es poden usar
     * {@code PreparedStatement} aquí perquè estem escrivint un fitxer de text,
     * no executant SQL contra la BBDD. S'escapa:
     * <ul>
     *   <li>{@code \\} → {@code \\\\} — per a engines que interpretin
     *       backslashes dins literals</li>
     *   <li>{@code '} → {@code ''} — estàndard SQL</li>
     *   <li>{@code \u0000} → buit — el null byte trenca JDBC i molts CLIs</li>
     *   <li>{@code \n}, {@code \r} → espais — un newline dins un literal
     *       és vàlid per JDBC però trenca el parser del MySQL CLI; per seguretat
     *       es normalitza a espai (la pèrdua d'informació és negligible per camps
     *       de text lliure com {@code notes})</li>
     * </ul>
     */
    private static String sqlEsc(String s) {
        if (s == null) return "";
        String out = s.replace("\\", "\\\\").replace("'", "''");
        out = out.replace("\u0000", "").replace("\n", " ").replace("\r", " ").replace("\u001A", "");
        return out;
    }
}
