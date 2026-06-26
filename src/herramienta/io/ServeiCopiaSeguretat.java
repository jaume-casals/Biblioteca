package herramienta.io;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.config.Configuracio;
import persistencia.internal.ControladorPersistencia;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import persistencia.dao.LlibreDaoCore;
import persistencia.internal.LlibreFieldBindings;
import persistencia.internal.Schema;
import persistencia.row.AutorRow;
import persistencia.row.LecturaRow;
import persistencia.row.LlibreAutorRow;
import persistencia.row.LlibreLlistaRow;
import persistencia.row.LlibreTagRow;
import persistencia.row.PrestecRow;
/** ServeiCopiaSeguretat llegeix totes les dades de ControladorPersistencia per produir volcats SQL.
 *  Es construeix amb cp (no cd) perquè cp és la font única de veritat per a les dades de
 *  còpia — totes les files provenen de consultes al DAO, no de la memòria cau en memòria.
 *  Això evita una desincronia on cd.allLlibres() podria endarrerir-se respecte
 *  cp.getAllLlibres() després de mutacions recents.
 *
 *  <p><b>Les imatges de coberta NO s'inclouen a la còpia SQL.</b> La columna
 *  {@code llibre.imatge_blob} s'exclou intencionadament (les cobertes poden fer desenes
 *  fins a centenars de KB cadascuna, i el format SQL s'inflaria 10-100 vegades). Després
 *  d'una restauració, l'usuari ha de tornar a obtenir les cobertes mitjançant l'acció
 *  {@code btnFetchCovers} ("Cerca a Internet"). Això es documenta a l'usuari al diàleg
 *  de restauració completada via la clau i18n {@code dlg_restore_done_cover_note}. */
public class ServeiCopiaSeguretat {

    private static final Logger LOG = Logger.getLogger(ServeiCopiaSeguretat.class.getName());

    private final ControladorPersistencia cp;
    private final java.util.concurrent.atomic.AtomicBoolean schedulerStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
    private java.util.concurrent.ScheduledExecutorService scheduler;

    /** Segons entre execucions de còpia automàtica (24 hores). */
    private static final long AUTO_BACKUP_INTERVAL_S = 86_400;

    /** Segons entre reintents puntuals quan una còpia cau SQLException/RuntimeException. */
    private static final long AUTO_BACKUP_RETRY_DELAY_S = 300;

    public ServeiCopiaSeguretat(ControladorPersistencia cp) {
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
            // Desactiva la reprogramació abans d'enderrocar l'executor
            // perquè una crida concurrent a scheduleAutoBackup() no pugui
            // crear un nou programador mentre shutdownNow està en vol
            // (el finding MEDIUM de tot.txt sobre el doble programador
            // mig aturat).
            schedulerStarted.set(false);
            local.shutdownNow();
            scheduler = null;
        } else {
            schedulerStarted.set(false);
        }
    }

    private void autoBackup() {
        try {
            doAutoBackup();
        } catch (Throwable t) {
            // SQLException o RuntimeException escaparien del cos i les
            // empassaria silenciosament ScheduledExecutorService
            // (sense UncaughtExceptionHandler al fil de còpia). Log
            // sever + reintent puntual 5 min perquè la fallada no es
            // perdi del tot — la propera execució del cicle de 24 h
            // continua activa.
            LOG.log(Level.SEVERE, "Backup automàtic fallat — programant reintent en 5 min", t);
            scheduleAutoBackupRetry();
        }
    }

    private void scheduleAutoBackupRetry() {
        java.util.concurrent.ScheduledExecutorService local = scheduler;
        if (local == null || local.isShutdown()) return;
        local.schedule(this::autoBackup, AUTO_BACKUP_RETRY_DELAY_S, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void doAutoBackup() throws java.io.IOException, java.sql.SQLException {
        List<Llibre> bib = cp.obtenirAllLlibres();
        if (bib.isEmpty()) return;
        File dir = Configuracio.obtenirBackupDir();
        if (!dir.exists()) {
            // mkdirs és el cas rar / d'error d'I/O — registra i surt
            // (anteriorment se l'empassava el catch-all de baix; el
            // finding MEDIUM de tot.txt va assenyalar la pèrdua del
            // senyal d'IOException).
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
        // Reserva un nom de fitxer únic per endavant perquè una
        // poda concurrent no pugui eliminar el fitxer en curs. El
        // sufix .tmp el marca com en vol; el canviem de nom al nom
        // final quan la còpia acaba.
        File tmpFile = new File(dir, outFile.getName() + ".tmp");
        try {
            copiaSegToSQL(tmpFile, bib, cp.obtenirAllLlistes(), cp.obtenirAllTags());
        } catch (java.io.IOException ioe) {
            // Segons el finding MEDIUM de tot.txt sobre les exceptions
            // empassades: registra+continua (aquesta és la còpia
            // automàtica programada; un sol fracàs no ha d'enverinar la
            // propera execució programada). Els errors de BBDD
            // (SQLException) i els runtime inesperats es propaguen
            // cap amunt fins a autoBackup(), on es registren com a
            // SEVERE i es programa un reintent.
            LOG.log(Level.WARNING, "Backup automàtic fallat (I/O)", ioe);
            return;
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
        // Poda DESPRÉS del canvi de nom perquè una cursa entre la poda
        // i una execució nova de còpia no pugui esborrar el fitxer
        // acabat d'escriure (el sufix .tmp ha desaparegut, el fitxer ja
        // no està "en vol").
        File[] backups = dir.listFiles((d, n) -> n.startsWith("biblioteca_") && n.endsWith(".sql"));
        if (backups != null && backups.length > 5) {
            java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(File::lastModified));
            // Salta els fitxers escrits en l'últim minut: una còpia
            // concurrent (p. ex. un BackupController.backupBD manual
            // des d'una segona JVM) potser acaba d'acabar el seu canvi
            // de nom i ja és visible per a listFiles(), de manera que
            // una passada ingènua d'"esborrar les còpies més antigues"
            // competiria amb l'escriptor que tot just ha acabat. El
            // període de gràcia d'1 minut cobreix la finestra típica de
            // còpia sense un creixement il·limitat de retenció.
            long cutoff = System.currentTimeMillis() - 60_000L;
            for (int i = 0; i < backups.length - 5; i++) {
                if (backups[i].lastModified() < cutoff) backups[i].delete();
            }
        }
    }

    public void copiaSegToSQL(File file, List<Llibre> bib, List<Llista> llistes, List<Tag> tags) throws java.io.IOException, java.sql.SQLException {
        persistencia.internal.ControladorPersistencia.BackupSnapshot snap = cp.snapshotForBackup();
        snap.bib = new java.util.ArrayList<>(bib);
        snap.llistes = new java.util.ArrayList<>(llistes);
        snap.tags = new java.util.ArrayList<>(tags);
        copiaSegToSQL(file, snap);
    }

    public void copiaSegToSQL(File file, persistencia.internal.ControladorPersistencia.BackupSnapshot snap) throws java.io.IOException {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("-- Biblioteca backup " + java.time.LocalDate.now());
            for (String t : persistencia.internal.Schema.CLEAR_ORDER) pw.println("DELETE FROM " + t + ";");
            for (Llibre l : snap.bib) {
                escriureLlibreINSERT(pw, l);
            }
            for (persistencia.row.AutorRow row : snap.autors) {
                escriureAutorINSERT(pw, row.id(), row.nom());
            }
            for (persistencia.row.LlibreAutorRow row : snap.llibreAutors) {
                escriureLlibreAutorINSERT(pw, row.isbn(), row.autorId());
            }
            for (Llista ll : snap.llistes) {
                escriureLlistaINSERT(pw, ll);
            }
            for (persistencia.row.LlibreLlistaRow row : snap.llibreLlistes) {
                escriureLlibreLlistaINSERT(pw, row);
            }
            for (Tag t : snap.tags) {
                escriureTagINSERT(pw, t);
            }
            for (persistencia.row.LlibreTagRow row : snap.llibreTags) {
                escriureLlibreTagINSERT(pw, row);
            }
            for (persistencia.row.PrestecRow row : snap.prestecs) {
                escriurePrestecINSERT(pw, row);
            }
            for (persistencia.row.LecturaRow row : snap.lectures) {
                escriureLecturaINSERT(pw, row);
            }
        }
    }

    private void escriureLlibreINSERT(java.io.PrintWriter pw, Llibre l) {
        // La llista de 27 columnes i la llista de 27 valors provenen
        // totes dues de LlibreFieldBindings (la font única de veritat
        // per a la forma del INSERT de Llibre). Afegir una columna nova
        // ara només requereix tocar LlibreFieldBindings.COLUMNS_INSERT
        // + forInsert() (segons el finding MEDIUM de tot.txt sobre la
        // deriva de llistes de columnes). La columna literal
        // `imatge_blob` s'exclou aquí perquè el ServeiCopiaSeguretat NO
        // incrusta els bytes de coberta al volcats SQL (veure el
        // Javadoc de la classe).
        Object[] vals = persistencia.internal.LlibreFieldBindings.forInsert(l);
        String[] cols = persistencia.internal.LlibreFieldBindings.COLUMNS_INSERT;
        // Una sola passada: escriu el nom de la columna i el valor
        // junts al mateix StringBuilder (la versió anterior iterava
        // dues vegades — per a 10k llibres això duplica el treball).
        // Es conserva l'ordre del mapa, però s'omet imatge_blob
        // perquè el volcats deixa el blob intencionadament.
        StringBuilder sb = new StringBuilder(640);
        sb.append("INSERT INTO llibre (");
        boolean first = true;
        for (int i = 0; i < cols.length; i++) {
            if ("imatge_blob".equals(cols[i])) continue;
            if (!first) sb.append(',');
            sb.append('`').append(cols[i]).append('`');
            first = false;
        }
        sb.append(") VALUES (");
        first = true;
        for (int i = 0; i < cols.length; i++) {
            if ("imatge_blob".equals(cols[i])) continue;
            if (!first) sb.append(',');
            sb.append(formatejarValue(vals[i]));
            first = false;
        }
        sb.append(");\n");
        pw.print(sb);
    }

    /** Formatador reutilitzat per a tots els Double del volcats —
     *  evitar crear un nou {@link java.util.Formatter} per cel·la (10k
     *  llibres × ~5 doubles = 50k instanciacions). */
    private static final java.text.DecimalFormat DOUBLE_FMT_4 =
        new java.text.DecimalFormat("0.0000", new java.text.DecimalFormatSymbols(java.util.Locale.ROOT));

    private static String formatejarValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof persistencia.internal.LlibreFieldBindings.Nul) return "NULL";
        if (v instanceof Boolean b) return b.toString();
        if (v instanceof Integer i) return i.toString();
        if (v instanceof Long l) return l.toString();
        if (v instanceof Double d) return DOUBLE_FMT_4.format(d);
        if (v instanceof java.sql.Date dt) return "'" + dt.toString() + "'";
        if (v instanceof byte[] bytes) return "NULL /* " + bytes.length + " bytes descartats */";
        // String
        return sqlNullable(v.toString());
    }

    private void escriureAutorINSERT(java.io.PrintWriter pw, int id, String nom) {
        pw.print("INSERT INTO autor (`id`,`nom`) VALUES (");
        pw.print(id);
        pw.print(',');
        pw.print(sqlNullable(nom));
        pw.println(");");
    }

    private void escriureLlibreAutorINSERT(java.io.PrintWriter pw, long isbn, int autorId) {
        pw.print("INSERT INTO llibre_autor (`isbn`,`autor_id`) VALUES (");
        pw.print(isbn);
        pw.print(',');
        pw.print(autorId);
        pw.println(");");
    }

    private void escriureLlistaINSERT(java.io.PrintWriter pw, Llista ll) {
        pw.print("INSERT INTO llista (`id`,`nom`,`ordre`,`color`) VALUES (");
        pw.print(ll.obtenirId());
        pw.print(',');
        pw.print(sqlNullable(ll.obtenirNom()));
        pw.print(',');
        pw.print(ll.obtenirOrdre());
        pw.print(',');
        pw.print(sqlNullable(ll.obtenirColor()));
        pw.println(");");
    }

    private void escriureLlibreLlistaINSERT(java.io.PrintWriter pw, persistencia.row.LlibreLlistaRow row) {
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

    private void escriureTagINSERT(java.io.PrintWriter pw, Tag t) {
        pw.print("INSERT INTO tag (`id`,`nom`) VALUES (");
        pw.print(t.obtenirId());
        pw.print(',');
        pw.print(sqlNullable(t.obtenirNom()));
        pw.println(");");
    }

    private void escriureLlibreTagINSERT(java.io.PrintWriter pw, persistencia.row.LlibreTagRow row) {
        pw.print("INSERT INTO llibre_tag (`isbn`,`tag_id`) VALUES (");
        pw.print(row.isbn());
        pw.print(',');
        pw.print(row.tagId());
        pw.println(");");
    }

    private void escriurePrestecINSERT(java.io.PrintWriter pw, persistencia.row.PrestecRow row) {
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

    private void escriureLecturaINSERT(java.io.PrintWriter pw, persistencia.row.LecturaRow row) {
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
        String esc = sqlEsc(s);
        if (esc.indexOf('\n') < 0) return "'" + esc + "'";
        StringBuilder sb = new StringBuilder(esc.length() + 32);
        String[] parts = esc.split("\n", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" || CHAR(10) || ");
            sb.append('\'').append(parts[i]).append('\'');
        }
        return sb.toString();
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
     * </ul>
     * Els salts de línia es preserven via la concatenació
     * {@code '...' || CHAR(10) || '...'} que composa {@link #sqlNullable} —
     * un newline real dins d'un literal trencaria el lector de línies
     * del restaurador ({@code br.lines()}).
     */
    private static String sqlEsc(String s) {
        if (s == null) return "";
        String out = s.replace("\\", "\\\\").replace("'", "''");
        out = out.replace("\u0000", "").replace("\u001A", "");
        return out;
    }
}
