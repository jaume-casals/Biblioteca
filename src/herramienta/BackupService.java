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
 *  cd.allLlibres() might lag behind cp.getAllLlibres() after recent mutations. */
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
            local.shutdownNow();
            scheduler = null;
        }
        schedulerStarted.set(false);
    }

    private void autoBackup() {
        List<Llibre> bib = cp.getAllLlibres();
        if (bib.isEmpty()) return;
        try {
            File dir = Config.getBackupDir();
            if (!dir.exists()) {
                dir.mkdirs();
                if (!dir.exists()) return;
            }
            String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File[] existing = dir.listFiles((d, n) -> n.startsWith("biblioteca_" + today) && n.endsWith(".sql"));
            if (existing != null && existing.length > 0) return;
            String ts = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            backupToSQL(new File(dir, "biblioteca_" + ts + ".sql"),
                bib, cp.getAllLlistes(), cp.getAllTags());
            File[] backups = dir.listFiles((d, n) -> n.startsWith("biblioteca_") && n.endsWith(".sql"));
            if (backups != null && backups.length > 5) {
                java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < backups.length - 5; i++) backups[i].delete();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Backup automàtic fallat", e);
        }
    }

    public void backupToSQL(File file, List<Llibre> bib, List<Llista> llistes, List<Tag> tags) throws Exception {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("-- Biblioteca backup " + java.time.LocalDate.now());
            for (String t : persistencia.Schema.CLEAR_ORDER) pw.println("DELETE FROM " + t + ";");
            for (Llibre l : bib) {
                writeLlibreINSERT(pw, l);
            }
            for (Object[] row : cp.getAllAutors()) {
                writeAutorINSERT(pw, (int) row[0], (String) row[1]);
            }
            for (Object[] row : cp.getAllLlibreAutor()) {
                writeLlibreAutorINSERT(pw, ((Number) row[0]).longValue(), ((Number) row[1]).intValue());
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
        pw.print("INSERT INTO llibre (`ISBN`,`nom`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`notes`," +
                "`pagines`,`pagines_llegides`,`editorial`,`serie`,`volum`,`data_compra`,`data_lectura`,`idioma`,`format`," +
                "`desitjat`,`pais_origen`,`estat`,`exemplars`,`llengua_original`,`nom_ca`,`nom_es`,`nom_en`) " +
                "VALUES (");
        pw.print(l.getISBN());
        pw.print(',');
        pw.print(sqlNullable(l.getNom()));
        pw.print(',');
        pw.print(l.getAny() != null ? l.getAny() : 0);
        pw.print(',');
        pw.print(sqlNullable(l.getDescripcio()));
        pw.print(',');
        pw.print(String.format(java.util.Locale.ROOT, "%.4f", l.getValoracio() != null ? l.getValoracio() : 0.0));
        pw.print(',');
        pw.print(String.format(java.util.Locale.ROOT, "%.4f", l.getPreu() != null ? l.getPreu() : 0.0));
        pw.print(',');
        pw.print(Boolean.TRUE.equals(l.getLlegit()));
        pw.print(',');
        pw.print(sqlNullable(l.getImatge()));
        pw.print(',');
        pw.print(sqlNullable(l.getNotes()));
        pw.print(',');
        pw.print(l.getPagines());
        pw.print(',');
        pw.print(l.getPaginesLlegides());
        pw.print(',');
        pw.print(sqlNullable(l.getEditorial()));
        pw.print(',');
        pw.print(sqlNullable(l.getSerie()));
        pw.print(',');
        pw.print(l.getVolum());
        pw.print(',');
        pw.print(sqlNullable(l.getDataCompra()));
        pw.print(',');
        pw.print(sqlNullable(l.getDataLectura()));
        pw.print(',');
        pw.print(sqlNullable(l.getIdioma()));
        pw.print(',');
        pw.print(sqlNullable(l.getFormat()));
        pw.print(',');
        pw.print(l.isDesitjat());
        pw.print(',');
        pw.print(sqlNullable(l.getPaisOrigen()));
        pw.print(',');
        pw.print(sqlNullable(l.getEstat()));
        pw.print(',');
        pw.print(l.getExemplars());
        pw.print(',');
        pw.print(sqlNullable(l.getLlenguaOriginal()));
        pw.print(',');
        pw.print(sqlNullable(l.getNomCa()));
        pw.print(',');
        pw.print(sqlNullable(l.getNomEs()));
        pw.print(',');
        pw.print(sqlNullable(l.getNomEn()));
        pw.println(");");
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
