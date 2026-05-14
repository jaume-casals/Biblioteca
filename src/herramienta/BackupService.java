package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import persistencia.ControladorPersistencia;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class BackupService {

    private final ControladorPersistencia cp;

    public BackupService(ControladorPersistencia cp) {
        this.cp = cp;
    }

    public void scheduleAutoBackup(List<Llibre> bib, List<Llista> llistes, List<Tag> tags) {
        Thread t = new Thread(() -> autoBackup(bib, llistes, tags));
        t.setDaemon(true);
        t.start();
    }

    private void autoBackup(List<Llibre> bib, List<Llista> llistes, List<Tag> tags) {
        if (bib.isEmpty()) return;
        try {
            File dir = new File(System.getProperty("user.home") + "/.biblioteca/backups");
            dir.mkdirs();
            String ts = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            backupToSQL(new File(dir, "biblioteca_" + ts + ".sql"), bib, llistes, tags);
            File[] backups = dir.listFiles((d, n) -> n.startsWith("biblioteca_") && n.endsWith(".sql"));
            if (backups != null && backups.length > 5) {
                java.util.Arrays.sort(backups);
                for (int i = 0; i < backups.length - 5; i++) backups[i].delete();
            }
        } catch (Exception ignored) {}
    }

    public void backupToSQL(File file, List<Llibre> bib, List<Llista> llistes, List<Tag> tags) throws Exception {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("-- Biblioteca backup " + java.time.LocalDate.now());
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
                    "INSERT INTO llibre (`ISBN`,`nom`,`autor`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`notes`,`pagines`,`pagines_llegides`,`editorial`,`serie`,`volum`,`data_compra`,`data_lectura`,`idioma`,`format`,`desitjat`,`pais_origen`) VALUES (%d,'%s','%s',%d,'%s',%.4f,%.4f,%b,'%s','%s',%d,%d,'%s','%s',%d,%s,%s,%s,%s,%b,%s);%n",
                    l.getISBN(),
                    sqlEsc(l.getNom()),
                    sqlEsc(l.getAutor() != null ? l.getAutor() : ""),
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
                    l.getPaisOrigen() != null ? "'" + sqlEsc(l.getPaisOrigen()) + "'" : "NULL");
            }
            for (Object[] row : cp.getAllAutors()) {
                pw.printf("INSERT INTO autor (`id`,`nom`) VALUES (%d,'%s');%n",
                    (Integer) row[0], sqlEsc((String) row[1]));
            }
            for (Object[] row : cp.getAllLlibreAutor()) {
                pw.printf("INSERT INTO llibre_autor (`isbn`,`autor_id`) VALUES (%d,%d);%n",
                    (Long) row[0], (Integer) row[1]);
            }
            for (Llista ll : llistes) {
                pw.printf("INSERT INTO llista (`id`,`nom`,`ordre`,`color`) VALUES (%d,'%s',%d,%s);%n",
                    ll.getId(), sqlEsc(ll.getNom()), ll.getOrdre(),
                    ll.getColor() != null ? "'" + sqlEsc(ll.getColor()) + "'" : "NULL");
            }
            for (Object[] row : cp.getAllLlibreLlista()) {
                pw.printf("INSERT INTO llibre_llista (`isbn`,`llista_id`,`valoracio`,`llegit`) VALUES (%d,%d,%.4f,%b);%n",
                    (Long) row[0], (Integer) row[1], (Double) row[2], (Boolean) row[3]);
            }
            for (Tag t : tags) {
                pw.printf("INSERT INTO tag (`id`,`nom`) VALUES (%d,'%s');%n",
                    t.getId(), sqlEsc(t.getNom()));
            }
            for (Object[] row : cp.getAllLlibreTag()) {
                pw.printf("INSERT INTO llibre_tag (`isbn`,`tag_id`) VALUES (%d,%d);%n",
                    (Long) row[0], (Integer) row[1]);
            }
            for (Object[] row : cp.getAllPrestecs()) {
                pw.printf("INSERT INTO prestec (`isbn`,`nom_persona`,`data_prestec`,`retornat`) VALUES (%d,'%s','%s',%b);%n",
                    (Long) row[0], sqlEsc((String) row[1]), sqlEsc(row[2] != null ? row[2].toString() : ""), (Boolean) row[3]);
            }
        }
    }

    private static String sqlEsc(String s) {
        return s == null ? "" : s.replace("'", "''");
    }
}
