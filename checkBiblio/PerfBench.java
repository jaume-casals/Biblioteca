import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import herramienta.ImportadorLlibres;
import persistencia.internal.ConnectionConfig;
import persistencia.internal.ControladorPersistencia;

/**
 * Banc de rendiment per als fluxos representatius de Biblioteca.
 *
 * <p>No forma part de la suite de proves ({@code make test}); s'executa a mà:
 *
 * <pre>
 *   javac -cp "bin:lib/*" -d bin checkBiblio/PerfBench.java
 *   java  -cp "bin:lib/*" PerfBench --books 5000 --reps 5
 * </pre>
 *
 * <p>Cada flux es mesura dues vegades: un passatge de temps (sense traça) i
 * un passatge de comptatge de sentències (amb {@code TRACE_LEVEL_SYSTEM_OUT=2}
 * de H2). Els dos passatges no es barregen: la traça de H2 afegeix una
 * impressió sincronitzada per sentència que falsejaria el temps.
 *
 * <p>El comptatge mira les línies {@code /*SQL ...*}{@code /} que H2
 * imprimeix: una per sentència executada (no per sentència preparada).
 */
public final class PerfBench {

    private static int books = 5000;
    private static int reps = 5;
    private static int csvRows = 2000;
    private static int coverBooks = 200;
    private static int bulkEdits = 100;
    private static boolean fileBased;
    private static String profileScenario;
    private static int profileSeconds = 20;
    private static java.io.File dbDir;

    private static int sink;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--books" -> books = Integer.parseInt(args[++i]);
                case "--reps" -> reps = Integer.parseInt(args[++i]);
                case "--csv-rows" -> csvRows = Integer.parseInt(args[++i]);
                case "--cover-books" -> coverBooks = Integer.parseInt(args[++i]);
                case "--bulk-edits" -> bulkEdits = Integer.parseInt(args[++i]);
                case "--file" -> fileBased = true;
                case "--profile" -> profileScenario = args[++i];
                case "--profile-seconds" -> profileSeconds = Integer.parseInt(args[++i]);
                default -> {
                    System.err.println("Opcio desconeguda: " + args[i]);
                    return;
                }
            }
        }
        System.out.printf(Locale.ROOT,
            "PerfBench  llibres=%d  reps=%d  csvRows=%d  coverBooks=%d  bulkEdits=%d  mode=%s%n%n",
            books, reps, csvRows, coverBooks, bulkEdits, fileBased ? "fitxer" : "memoria");

        String dbName = "perfbench-" + Long.toHexString(System.nanoTime());
        String realUrl;
        if (fileBased) {
            dbDir = java.nio.file.Files.createTempDirectory("perfbench").toFile();
            dbDir.deleteOnExit();
            realUrl = "jdbc:h2:" + dbDir.getAbsolutePath() + "/" + dbName
                + ";MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
        } else {
            realUrl = "jdbc:h2:mem:" + dbName + ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1";
        }
        System.out.printf(Locale.ROOT, "URL: %s%n%n", realUrl);

        if (profileScenario != null) {
            profile(realUrl, dbName);
            return;
        }

        ControladorPersistencia cp = ControladorPersistencia.create(
            new ConnectionConfig("h2", "localhost", "sa", "", dbName, realUrl));
        Connection con = cp.obtenirConnexio();
        try {
            // H2 2.x té una memòria cau de resultats (QUERY_CACHE_SIZE, 8 per
            // defecte) que fa que una consulta repetida amb els MÉS paràmetres
            // torni el resultat en lloc de tornar-lo a calcular. Això
            // contaminaria la mesura: el cas real (l'usuari canvia de pàgina)
            // sempre té paràmetres diferents. Es desactiva.
            try (Statement st = con.createStatement()) {
                st.execute("SET QUERY_CACHE_SIZE 0");
            } catch (Exception ignored) {}
            seed(con);
            System.out.printf(Locale.ROOT, "Seminada: %d llibres, %d autors, %d enllaços autor%n%n",
                cp.comptarLlibres(), count(con, "SELECT COUNT(*) FROM autor"),
                count(con, "SELECT COUNT(*) FROM llibre_autor"));

            // Escalfar la JVM i el pla d'execució abans de mesurar.
            cp.obtenirAllLlibresSummary();
            cp.cercarLlibres(LlibreFilter.empty(), 0, 100);
            System.out.println("--- lectura ---");

            measure("llista   obtenirAllLlibresSummary", reps,
                () -> { sink = cp.obtenirAllLlibresSummary().size(); return 0; },
                () -> countStatements(con, () -> { sink = cp.obtenirAllLlibresSummary().size(); return 0; }));

            measure("domini   ControladorDomini.create (startup)", reps,
                () -> { sink = ControladorDomini.create(cp).obtenirAllLlibres().size(); return 0; },
                () -> countStatements(con, () -> { sink = ControladorDomini.create(cp).obtenirAllLlibres().size(); return 0; }));

            measure("pagina   page(0,100)", reps,
                () -> { sink = cp.cercarLlibres(LlibreFilter.empty(), 0, 100).size(); return 0; },
                () -> countStatements(con, () -> { sink = cp.cercarLlibres(LlibreFilter.empty(), 0, 100).size(); return 0; }));

            int lastOffset = Math.max(0, books - 100);
            measure("pagina   page(ultima,100)", reps,
                () -> { sink = cp.cercarLlibres(LlibreFilter.empty(), lastOffset, 100).size(); return 0; },
                () -> countStatements(con, () -> { sink = cp.cercarLlibres(LlibreFilter.empty(), lastOffset, 100).size(); return 0; }));

            // Ordenació per una columna sense index (any / valoracio / preu):
            // obliga el motor a ordenar tot el conjunt i a materialitzar la
            // fila completa (incloent descripcio i notes) abans del LIMIT.
            measure("pagina   page(0,100) ordre=any", reps,
                () -> { sink = cp.cercarLlibres(sortAny(), 0, 100).size(); return 0; },
                () -> countStatements(con, () -> { sink = cp.cercarLlibres(sortAny(), 0, 100).size(); return 0; }));

            measure("pagina   page(0,100) ordre=nom", reps,
                () -> { sink = cp.cercarLlibres(sortNom(), 0, 100).size(); return 0; },
                () -> countStatements(con, () -> { sink = cp.cercarLlibres(sortNom(), 0, 100).size(); return 0; }));

            // Cost agregat de recórrer totes les pàgines (com ho fa l'usuari
            // preentrant pàgina a pàgina), amb el desglossament per pàgina.
            measure("pagines  totes les pagines (x" + (books / 100) + ")", Math.max(1, Math.min(reps, 3)),
                () -> { int n = 0; for (int off = 0; off < books; off += 100) n += cp.cercarLlibres(LlibreFilter.empty(), off, 100).size(); sink = n; return 0; },
                () -> countStatements(con, () -> { int n = 0; for (int off = 0; off < books; off += 100) n += cp.cercarLlibres(LlibreFilter.empty(), off, 100).size(); sink = n; return 0; }));
            perPageBreakdown(cp);

            measure("cerca    cercarLlibres(nom LIKE)", reps,
                () -> { sink = cp.cercarLlibres(LlibreFilter.empty().withNom("Llibre 1"), 0, 0).size(); return 0; },
                () -> countStatements(con, () -> { sink = cp.cercarLlibres(LlibreFilter.empty().withNom("Llibre 1"), 0, 0).size(); return 0; }));

            System.out.println("--- SQL en brut (aillant el cost de la projeccio) ---");
            rawProbes(con, reps);

            System.out.println("--- escriptura ---");

            measure("actualitza  update x" + bulkEdits, reps,
                () -> { bulkUpdate(cp); return 0; },
                () -> countStatements(con, () -> { bulkUpdate(cp); return 0; }));

            measureImport(reps);
            measureCovers(reps, cp, con);
        } finally {
            try { con.close(); } catch (Exception ignored) {}
        }
        System.out.println();
    }

    // ── Escriptures ───────────────────────────────────────────────────────────

    /**
     * Sondeja el SQL en brut per aïllar el cost de la projecció i del DISTINCT
     * del cost del transport i del mapeig Java.
     */
    private static void rawProbes(Connection con, int reps) {
        String light = "l.ISBN, l.nom, (SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') "
            + "FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, "
            + "l.`any`, l.valoracio, l.preu, l.llegit, l.imatge, (l.imatge_blob IS NOT NULL) AS has_blob, "
            + "l.pagines, l.pagines_llegides, l.editorial, l.serie, l.volum, l.data_compra, l.data_lectura, "
            + "l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, "
            + "l.nom_ca, l.nom_es, l.nom_en";
        String heavy = "l.ISBN, l.nom, (SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') "
            + "FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, "
            + "l.`any`, l.descripcio, l.valoracio, l.preu, l.llegit, l.imatge, (l.imatge_blob IS NOT NULL) AS has_blob, "
            + "l.pagines, l.pagines_llegides, l.editorial, l.serie, l.volum, l.data_compra, l.data_lectura, "
            + "l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, "
            + "l.nom_ca, l.nom_es, l.nom_en, l.notes";
        String noAutor = "l.ISBN, l.nom, '' AS autor, l.`any`, l.valoracio, l.preu, l.llegit, l.imatge, "
            + "(l.imatge_blob IS NOT NULL) AS has_blob, l.pagines, l.pagines_llegides, l.editorial, l.serie, "
            + "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, "
            + "l.exemplars, l.llengua_original, l.nom_ca, l.nom_es, l.nom_en";

        raw(con, reps, "SQL  llista lleugera (amb subconsulta autor)", "SELECT " + light + " FROM llibre l ORDER BY l.ISBN");
        raw(con, reps, "SQL  llista lleugera (sense autor)", "SELECT " + noAutor + " FROM llibre l ORDER BY l.ISBN");
        raw(con, reps, "SQL  llista pesada (amb autor+notes)", "SELECT " + heavy + " FROM llibre l ORDER BY l.ISBN");
        raw(con, reps, "SQL  pagina DISTINCT pesada (actual)", "SELECT DISTINCT " + heavy + " FROM llibre l ORDER BY l.ISBN LIMIT 100 OFFSET 0");
        raw(con, reps, "SQL  pagina DISTINCT lleugera", "SELECT DISTINCT " + light + " FROM llibre l ORDER BY l.ISBN LIMIT 100 OFFSET 0");
        raw(con, reps, "SQL  pagina lleugera sense DISTINCT", "SELECT " + light + " FROM llibre l ORDER BY l.ISBN LIMIT 100 OFFSET 0");
        raw(con, reps, "SQL  pagina pesada sense DISTINCT", "SELECT " + heavy + " FROM llibre l ORDER BY l.ISBN LIMIT 100 OFFSET 0");

        int off = Math.max(0, books - 100);
        rawP(con, reps, "SQL  pagina DISTINCT pesada OFFSET " + off,
            "SELECT DISTINCT " + heavy + " FROM llibre l ORDER BY l.ISBN LIMIT ? OFFSET ?", off);
        rawP(con, reps, "SQL  pagina DISTINCT lleugera OFFSET " + off,
            "SELECT DISTINCT " + light + " FROM llibre l ORDER BY l.ISBN LIMIT ? OFFSET ?", off);
        rawP(con, reps, "SQL  pagina lleugera sense DISTINCT OFFSET " + off,
            "SELECT " + light + " FROM llibre l ORDER BY l.ISBN LIMIT ? OFFSET ?", off);
        rawP(con, reps, "SQL  pagina pesada sense DISTINCT OFFSET " + off,
            "SELECT " + heavy + " FROM llibre l ORDER BY l.ISBN LIMIT ? OFFSET ?", off);

        // Mateixa consulta, però extraient les columnes com ho fa el mapper:
        // separa el cost del motor del cost del mapeig Java.
        rawMap(con, reps, "MAP  pagina DISTINCT pesada OFFSET " + off, heavy, off);
        rawMap(con, reps, "MAP  pagina lleugera sense DISTINCT OFFSET " + off, light, off);
        rawMap(con, reps, "MAP  llista lleugera completa (5000 files)", light, -1);
    }

    private static void rawP(Connection con, int reps, String label, String sql, int offset) {
        long best = Long.MAX_VALUE;
        long rows = 0;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, 100);
                ps.setInt(2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    rows = 0;
                    while (rs.next()) rows++;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            long dt = System.nanoTime() - t0;
            if (dt < best) best = dt;
        }
        report(label, best, 0, (int) rows);
    }

    /** Mateixa consulta que {@link #rawP} però llegint totes les columnes. */
    private static void rawMap(Connection con, int reps, String label, String cols, int offset) {
        String sql = "SELECT " + cols + " FROM llibre l ORDER BY l.ISBN"
            + (offset >= 0 ? " LIMIT ? OFFSET ?" : "");
        long best = Long.MAX_VALUE;
        long rows = 0;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (offset >= 0) { ps.setInt(1, 100); ps.setInt(2, offset); }
                try (ResultSet rs = ps.executeQuery()) {
                    rows = 0;
                    while (rs.next()) {
                        rows++;
                        rs.getLong("ISBN");
                        rs.getString("nom");
                        rs.getString("autor");
                        rs.getObject("any", Integer.class);
                        rs.getBoolean("has_blob");
                        rs.getInt("pagines");
                        rs.getInt("pagines_llegides");
                        rs.getString("editorial");
                        rs.getString("serie");
                        rs.getInt("volum");
                        rs.getString("data_compra");
                        rs.getString("data_lectura");
                        rs.getString("idioma");
                        rs.getString("format");
                        rs.getBoolean("desitjat");
                        rs.getString("pais_origen");
                        rs.getString("estat");
                        rs.getInt("exemplars");
                        rs.getString("llengua_original");
                        rs.getString("nom_ca");
                        rs.getString("nom_es");
                        rs.getString("nom_en");
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            long dt = System.nanoTime() - t0;
            if (dt < best) best = dt;
        }
        report(label, best, 0, (int) rows);
    }

    private static void raw(Connection con, int reps, String label, String sql) {
        long best = Long.MAX_VALUE;
        long rows = 0;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            try (Statement s = con.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                rows = 0;
                while (rs.next()) rows++;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            long dt = System.nanoTime() - t0;
            if (dt < best) best = dt;
        }
        report(label, best, 0, (int) rows);
    }

    /** Temps de cada pàgina individualment, per veure com creix amb l'OFFSET. */
    private static void perPageBreakdown(ControladorPersistencia cp) {        StringBuilder sb = new StringBuilder("    per-pagina (ms): ");
        for (int off = 0; off < books; off += 100) {
            long t0 = System.nanoTime();
            cp.cercarLlibres(LlibreFilter.empty(), off, 100);
            long dt = System.nanoTime() - t0;
            sb.append(String.format(Locale.ROOT, "%.1f ", dt / 1_000_000.0));
            if ((off / 100) % 10 == 9) sb.append("| ");
        }
        System.out.println(sb);
    }

    private static LlibreFilter sortAny() {
        return LlibreFilter.empty().withSort(new domini.EspecificacioOrdenacio(
            domini.EspecificacioOrdenacio.COL_ANY, true));
    }
    private static LlibreFilter sortNom() {
        return LlibreFilter.empty().withSort(new domini.EspecificacioOrdenacio(
            domini.EspecificacioOrdenacio.COL_NOM, true));
    }

    private static void bulkUpdate(ControladorPersistencia cp) {
        try {
            List<Llibre> all = cp.obtenirAllLlibresSummary();
            int n = Math.min(bulkEdits, all.size());
            for (int i = 0; i < n; i++) {
                Llibre l = all.get(i);
                l.posarEditorial("Editorial " + i);
                cp.actualitzarLlibre(l);
            }
            sink = n;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void measureImport(int reps) throws Exception {
        File csv = File.createTempFile("perfbench", ".csv");
        csv.deleteOnExit();
        StringBuilder sb = new StringBuilder();
        sb.append("ISBN,Nom,Autor,Any,Descripcio,Valoracio,Preu,Llegit,Portada\n");
        for (int i = 0; i < csvRows; i++) {
            sb.append(8600000000000L + i).append(",Titol ").append(i)
              .append(",Autor ").append(i % 50).append(',').append(1990 + (i % 30))
              .append(",Descripcio de prova amb text ").append(i)
              .append(",").append(i % 5).append(",").append(9.99).append(",false,,\n");
        }
        Files.writeString(csv.toPath(), sb.toString(), StandardCharsets.UTF_8);

        long best = Long.MAX_VALUE;
        long bestStmts = 0;
        String counts = "";
        for (int r = 0; r < reps; r++) {
            String dbName = "perfimport-" + Long.toHexString(System.nanoTime()) + "-" + r;
            String u = novaUrl(dbName);
            ControladorPersistencia cp = ControladorPersistencia.create(
                new ConnectionConfig("h2", "localhost", "sa", "", dbName, u));
            try {
                ControladorDomini domini = ControladorDomini.create(cp);
                long t0 = System.nanoTime();
                ImportadorLlibres.ResultatImportacio res = ImportadorLlibres.importarCSV(csv, domini);
                long dt = System.nanoTime() - t0;
                if (dt < best) best = dt;
                counts = String.format(Locale.ROOT, "importats=%d omesos=%d errors=%d%s",
                    res.imported(), res.skipped(), res.errors(),
                    res.errorDetails().isEmpty() ? "" : "  primer=" + res.errorDetails().get(0));
            } finally {
                try { cp.obtenirConnexio().close(); } catch (Exception ignored) {}
            }
        }
        // Passatge de comptatge: base nova, mateix contingut.
        {
            String dbName = "perfimport-count-" + Long.toHexString(System.nanoTime());
            String u = novaUrl(dbName);
            ControladorPersistencia cp = ControladorPersistencia.create(
                new ConnectionConfig("h2", "localhost", "sa", "", dbName, u));
            try {
                bestStmts = countStatements(cp.obtenirConnexio(), () -> {
                    ImportadorLlibres.importarCSV(csv, ControladorDomini.create(cp));
                    return 0;
                });
            } finally {
                try { cp.obtenirConnexio().close(); } catch (Exception ignored) {}
            }
        }
        report("importa  importarCSV(" + csvRows + " files)", best, bestStmts, csvRows);
        System.out.printf(Locale.ROOT, "    %s%n", counts);
    }

    /**
     * Mode de perfil: repeteix un flux durant {@code --profile-seconds}
     * segons perquè el JFR pugui mostrejar-lo.
     */
    private static void profile(String url, String dbName) throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.create(
            new ConnectionConfig("h2", "localhost", "sa", "", dbName, url));
        Connection con = cp.obtenirConnexio();
        try (Statement st = con.createStatement()) {
            st.execute("SET QUERY_CACHE_SIZE 0");
        } catch (Exception ignored) {}
        seed(con);
        long end = System.nanoTime() + profileSeconds * 1_000_000_000L;
        long n = 0;
        while (System.nanoTime() < end) {
            switch (profileScenario) {
                case "list" -> sink = cp.obtenirAllLlibresSummary().size();
                case "page" -> sink = cp.cercarLlibres(LlibreFilter.empty(), (int) (n % 50) * 100, 100).size();
                case "update" -> { bulkUpdate(cp); n += bulkEdits; }
                default -> throw new IllegalArgumentException("Escenari de perfil desconegut: " + profileScenario);
            }
            n++;
        }
        System.out.println("PROFILE-DONE " + profileScenario + " iter=" + n + " sink=" + sink);
    }

    /** URL nova segons el mode (memòria o fitxer). */
    private static String novaUrl(String dbName) throws Exception {
        if (fileBased) {
            java.io.File d = java.nio.file.Files.createTempDirectory("perfimport").toFile();
            d.deleteOnExit();
            return "jdbc:h2:" + d.getAbsolutePath() + "/" + dbName
                + ";MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
        }
        return "jdbc:h2:mem:" + dbName + ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1";
    }

    private static void measureCovers(int reps, ControladorPersistencia cp, Connection con) throws Exception {
        int n = Math.min(coverBooks, books);
        byte[] img = sinteticaPng(120, 180);
        try (PreparedStatement ps = con.prepareStatement("UPDATE llibre SET imatge_blob = ? WHERE ISBN = ?")) {
            for (long i = 0; i < n; i++) {
                ps.setBytes(1, img);
                ps.setLong(2, 8400000000000L + i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        List<Llibre> all = cp.obtenirAllLlibresSummary();
        List<Llibre> sample = new ArrayList<>(all.subList(0, n));
        persistencia.contract.LectorLlibre lector = ControladorDomini.create(cp);

        long best = Long.MAX_VALUE;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            for (Llibre l : sample) {
                javax.swing.ImageIcon ic = presentacio.galeria.ServeiImatgesCoberta.scaledCover(
                    presentacio.galeria.ServeiImatgesCoberta.carregarCoverBytes(l, lector));
                sink += ic == null ? 0 : ic.getIconWidth();
            }
            long dt = System.nanoTime() - t0;
            if (dt < best) best = dt;
        }
        report("cobertures scaledCover x" + n, best, 0, n);
    }

    private static byte[] sinteticaPng(int w, int h) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
            java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setPaint(new java.awt.GradientPaint(0, 0, java.awt.Color.BLUE, w, h, java.awt.Color.RED));
        g.fillRect(0, 0, w, h);
        g.dispose();
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    // ── Semilla ───────────────────────────────────────────────────────────────

    private static void seed(Connection con) throws Exception {
        con.setAutoCommit(false);
        try (Statement s = con.createStatement()) {
            s.executeUpdate("DELETE FROM llibre_autor");
            s.executeUpdate("DELETE FROM llibre");
            s.executeUpdate("DELETE FROM autor");
        }
        try (PreparedStatement insL = con.prepareStatement(
                "INSERT INTO llibre (ISBN, nom, `any`, descripcio, notes, valoracio, preu, llegit, imatge, data_afegit)"
              + " VALUES (?,?,?,?,?,?,?,?,?, NOW())");
             PreparedStatement insA = con.prepareStatement("INSERT IGNORE INTO autor (nom) VALUES (?)");
             PreparedStatement insLA = con.prepareStatement(
                "INSERT IGNORE INTO llibre_autor (isbn, autor_id) SELECT ?, id FROM autor WHERE nom = ?")) {
            String desc = "Descripcio de prova amb un text razonablement llarg per mesurar el cost de la projeccio.";
            String notes = "Notes de prova amb un text razonablement llarg per mesurar el cost de la projeccio.";
            for (int i = 0; i < books; i++) {
                long isbn = 8400000000000L + i;
                insL.setLong(1, isbn);
                insL.setString(2, "Llibre " + i);
                insL.setInt(3, 1990 + (i % 30));
                insL.setString(4, desc);
                insL.setString(5, notes);
                insL.setDouble(6, i % 5);
                insL.setDouble(7, 9.99);
                insL.setBoolean(8, false);
                insL.setString(9, "");
                insL.addBatch();

                for (int a = 0; a < 2; a++) {
                    String autor = "Autor " + (i % 50);
                    insA.setString(1, autor);
                    insA.addBatch();
                    insLA.setLong(1, isbn);
                    insLA.setString(2, autor);
                    insLA.addBatch();
                }
            }
            insL.executeBatch();
            insA.executeBatch();
            insLA.executeBatch();
        }
        con.commit();
        con.setAutoCommit(true);
    }

    // ── Comptatge de sentències ───────────────────────────────────────────────

    /**
     * H2 desa el seu stream de traça la primera vegada que escriu i el
     * conserva per a tota la sessió. Per això el banc substitueix
     * {@code System.out} per un {@link TraceSink} <em>abans</em> de qualsevol
     * traça, i el {@link TraceSink} decideix en temps real si escriu al buffer
     * de comptatge o a la sortida real.
     */
    private static final java.io.ByteArrayOutputStream TRACE_BUF = new java.io.ByteArrayOutputStream(1 << 20);
    private static boolean capturing;
    private static final PrintStream REAL_OUT = System.out;
    private static final PrintStream TRACE_SINK = new PrintStream(new java.io.OutputStream() {
        @Override public void write(int b) {
            synchronized (TRACE_BUF) {
                if (capturing) TRACE_BUF.write(b); else REAL_OUT.write(b);
            }
        }
        @Override public void write(byte[] b, int off, int len) {
            synchronized (TRACE_BUF) {
                if (capturing) TRACE_BUF.write(b, off, len); else REAL_OUT.write(b, off, len);
            }
        }
        @Override public void flush() { REAL_OUT.flush(); }
    }, true, StandardCharsets.UTF_8);

    static { System.setOut(TRACE_SINK); }

    private static long countStatements(Connection con, Body body) {
        try {
            try (Statement s = con.createStatement()) {
                s.execute("SET TRACE_LEVEL_SYSTEM_OUT 2");
            } catch (Exception ignored) {}
            synchronized (TRACE_BUF) { TRACE_BUF.reset(); capturing = true; }
            try {
                body.run();
            } finally {
                synchronized (TRACE_BUF) { capturing = false; }
            }
            try (Statement s = con.createStatement()) {
                s.execute("SET TRACE_LEVEL_SYSTEM_OUT 0");
            } catch (Exception ignored) {}
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String trace;
        synchronized (TRACE_BUF) { trace = TRACE_BUF.toString(StandardCharsets.UTF_8); }
        long count = 0;
        int idx = 0;
        while ((idx = trace.indexOf("/*SQL", idx)) >= 0) {
            int end = trace.indexOf("*/", idx);
            String stmt = end < 0 ? "" : trace.substring(idx, end);
            if (!stmt.contains("TRACE_LEVEL_SYSTEM_OUT")) count++;
            idx += 5;
        }
        return count;
    }

    private static long count(Connection con, String sql) {
        try (Statement s = con.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Mesura ────────────────────────────────────────────────────────────────

    private interface Body { long run(); }

    private static void measure(String label, int reps, Body timed, Body counted) {
        // Escalfar: sense això, el primer flux mapat es mesura amb el JIT
        // encara fred i el resultat depèn de l'ordre dels escenaris.
        for (int i = 0; i < 3; i++) timed.run();
        long[] times = new long[reps];
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            timed.run();
            times[r] = System.nanoTime() - t0;
        }
        long[] sorted = times.clone();
        java.util.Arrays.sort(sorted);
        long med = sorted[sorted.length / 2];
        long stmts = counted.run();
        double ms = sorted[0] / 1_000_000.0;
        System.out.printf(Locale.ROOT, "%-42s millor=%9.3f ms  mediana=%9.3f ms%s%n",
            label, ms, med / 1_000_000.0,
            stmts > 0 ? String.format(Locale.ROOT, "  sentencies=%d", stmts) : "");
    }

    private static void report(String label, long bestNanos, long statements, int units) {
        double ms = bestNanos / 1_000_000.0;
        String per = units > 0 ? String.format(Locale.ROOT, "  (%.3f ms/u)", ms / units) : "";
        String st = statements > 0 ? String.format(Locale.ROOT, "  sentencies=%d", statements) : "";
        System.out.printf(Locale.ROOT, "%-42s millor=%9.3f ms%s%s%n", label, ms, per, st);
    }
}
