package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import persistencia.ControladorPersistencia;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/** BackupService reads all data from ControladorPersistencia to produce SQL dumps.
 *  Constructed with cp (not cd) because cp is the single source of truth for backup data —
 *  all rows come from DAO queries, not the in-memory cache. This avoids a desync where
 *  cd.allLlibres() might lag behind cp.getAllLlibres() after recent mutations. */
public class BackupService {

    private final ControladorPersistencia cp;

    public BackupService(ControladorPersistencia cp) {
        this.cp = cp;
    }

    public void scheduleAutoBackup() {
        java.util.concurrent.ScheduledExecutorService scheduler =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auto-backup");
                t.setDaemon(true);
                return t;
            });
        scheduler.scheduleWithFixedDelay(this::autoBackup, 30, 86400, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void autoBackup() {
        try { Thread.sleep(30_000); } catch (InterruptedException e) { return; }
        List<Llibre> bib = cp.getAllLlibres();
        if (bib.isEmpty()) return;
        try {
            File dir = Config.getBackupDir();
            dir.mkdirs();
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
            System.err.println("Backup automàtic fallat: " + e.getMessage());
        }
    }

    public void backupToSQL(File file, List<Llibre> bib, List<Llista> llistes, List<Tag> tags) throws Exception {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("-- Biblioteca backup " + java.time.LocalDate.now());
            pw.println("DELETE FROM lectura;");
            pw.println("DELETE FROM prestec;");
            pw.println("DELETE FROM llibre_llista;");
            pw.println("DELETE FROM llista;");
            pw.println("DELETE FROM llibre_autor;");
            pw.println("DELETE FROM llibre_tag;");
            pw.println("DELETE FROM tag;");
            pw.println("DELETE FROM autor;");
            pw.println("DELETE FROM llibre;");
            for (Llibre l : bib) {
                pw.printf(
                    "INSERT INTO llibre (`ISBN`,`nom`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`notes`," +
                    "`pagines`,`pagines_llegides`,`editorial`,`serie`,`volum`,`data_compra`,`data_lectura`,`idioma`,`format`," +
                    "`desitjat`,`pais_origen`,`estat`,`exemplars`,`llengua_original`,`nom_ca`,`nom_es`,`nom_en`) " +
                    "VALUES (%d,'%s',%d,'%s',%.4f,%.4f,%b,'%s','%s',%d,%d,'%s','%s',%d,%s,%s,%s,%s,%b,%s,%s,%d,%s,%s,%s,%s);%n",
                    l.getISBN(),
                    sqlEsc(l.getNom()),
                    l.getAny() != null ? l.getAny() : 0,
                    sqlEsc(l.getDescripcio() != null ? l.getDescripcio() : ""),
                    l.getValoracio() != null ? l.getValoracio() : 0.0,
                    l.getPreu() != null ? l.getPreu() : 0.0,
                    Boolean.TRUE.equals(l.getLlegit()),
                    sqlEsc(l.getImatge() != null ? l.getImatge() : ""),
                    sqlEsc(l.getNotes()),
                    l.getPagines(),
                    l.getPaginesLlegides(),
                    sqlEsc(l.getEditorial()),
                    sqlEsc(l.getSerie()),
                    l.getVolum(),
                    l.getDataCompra() != null ? "'" + sqlEsc(l.getDataCompra()) + "'" : "NULL",
                    l.getDataLectura() != null ? "'" + sqlEsc(l.getDataLectura()) + "'" : "NULL",
                    l.getIdioma() != null ? "'" + sqlEsc(l.getIdioma()) + "'" : "NULL",
                    l.getFormat() != null ? "'" + sqlEsc(l.getFormat()) + "'" : "NULL",
                    l.getDesitjat(),
                    l.getPaisOrigen() != null ? "'" + sqlEsc(l.getPaisOrigen()) + "'" : "NULL",
                    l.getEstat() != null ? "'" + sqlEsc(l.getEstat()) + "'" : "NULL",
                    l.getExemplars(),
                    l.getLlenguaOriginal() != null ? "'" + sqlEsc(l.getLlenguaOriginal()) + "'" : "NULL",
                    l.getNomCa() != null ? "'" + sqlEsc(l.getNomCa()) + "'" : "NULL",
                    l.getNomEs() != null ? "'" + sqlEsc(l.getNomEs()) + "'" : "NULL",
                    l.getNomEn() != null ? "'" + sqlEsc(l.getNomEn()) + "'" : "NULL");
            }
            for (Object[] row : cp.getAllAutors()) {
                pw.printf("INSERT INTO autor (`id`,`nom`) VALUES (%d,'%s');%n",
                    (int) row[0], sqlEsc((String) row[1]));
            }
            for (Object[] row : cp.getAllLlibreAutor()) {
                pw.printf("INSERT INTO llibre_autor (`isbn`,`autor_id`) VALUES (%d,%d);%n",
                    ((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
            for (Llista ll : llistes) {
                pw.printf("INSERT INTO llista (`id`,`nom`,`ordre`,`color`) VALUES (%d,'%s',%d,%s);%n",
                    ll.getId(), sqlEsc(ll.getNom()), ll.getOrdre(),
                    ll.getColor() != null ? "'" + sqlEsc(ll.getColor()) + "'" : "NULL");
            }
            for (persistencia.LlibreLlistaRow row : cp.getAllLlibreLlista()) {
                pw.printf("INSERT INTO llibre_llista (`isbn`,`llista_id`,`valoracio`,`llegit`) VALUES (%d,%d,%.4f,%b);%n",
                    row.isbn(), row.llistaId(), row.valoracio(), row.llegit());
            }
            for (Tag t : tags) {
                pw.printf("INSERT INTO tag (`id`,`nom`) VALUES (%d,'%s');%n",
                    t.getId(), sqlEsc(t.getNom()));
            }
            for (persistencia.LlibreTagRow row : cp.getAllLlibreTag()) {
                pw.printf("INSERT INTO llibre_tag (`isbn`,`tag_id`) VALUES (%d,%d);%n",
                    row.isbn(), row.tagId());
            }
            for (persistencia.PrestecRow row : cp.getAllPrestecs()) {
                pw.printf("INSERT INTO prestec (`isbn`,`nom_persona`,`data_prestec`,`retornat`) VALUES (%d,'%s','%s',%b);%n",
                    row.isbn(), sqlEsc(row.nomPersona()), row.dataPrestec() != null ? sqlEsc(row.dataPrestec().toString()) : "", row.retornat());
            }
            for (persistencia.LecturaRow row : cp.getAllLectures()) {
                pw.printf("INSERT INTO lectura (`isbn`,`data_inici`,`data_fi`,`pagines_llegides`) VALUES (%d,%s,%s,%d);%n",
                    row.isbn(),
                    row.dataInici() != null ? "'" + sqlEsc(row.dataInici()) + "'" : "NULL",
                    row.dataFi() != null ? "'" + sqlEsc(row.dataFi()) + "'" : "NULL",
                    row.paginesLlegides());
            }
        }
    }

    private static String sqlEsc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "''");
    }
}
