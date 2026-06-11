package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import domini.Llibre;

/**
 * Core CRUD on the {@code llibre} table: insert / update / delete / getAll /
 * getRecentlyAdded / count / clearAllData / executeSQLFile / getDbSizeBytes.
 * Search lives in {@link LlibreSearchDao}, blob access in {@link LlibreBlobDao},
 * reading sessions in {@link LlibreLecturaDao}. All four DAOs share the
 * same {@link Connection} held by {@link ControladorPersistencia}.
 */
public class LlibreDaoCore {

    private final Connection con;

    /** Unprefixed column list — used by SELECTs without a table alias. */
    static final String LLIBRE_COLUMNS =
        "ISBN, nom, " +
        "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = ISBN) AS autor, " +
        "`any`, descripcio, valoracio, preu, llegit, imatge, " +
        "(imatge_blob IS NOT NULL) AS has_blob, notes, pagines, pagines_llegides, editorial, serie, " +
        "volum, data_compra, data_lectura, idioma, format, desitjat, pais_origen, estat, exemplars, llengua_original, " +
        "nom_ca, nom_es, nom_en";

    /** {@code l.}-prefixed column list — used by SELECTs with {@code l} alias. */
    static final String LLIBRE_COLUMNS_L =
        "l.ISBN, l.nom, " +
        "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, " +
        "l.`any`, l.descripcio, l.valoracio, l.preu, l.llegit, l.imatge, " +
        "(l.imatge_blob IS NOT NULL) AS has_blob, l.notes, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
        "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, " +
        "l.nom_ca, l.nom_es, l.nom_en";

    /** {@code l.}-prefixed column list, light view (no descripcio, no notes). */
    static final String LLIBRE_COLUMNS_L_LIGHT =
        "l.ISBN, l.nom, " +
        "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, " +
        "l.`any`, l.valoracio, l.preu, l.llegit, l.imatge, " +
        "(l.imatge_blob IS NOT NULL) AS has_blob, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
        "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, " +
        "l.nom_ca, l.nom_es, l.nom_en";

    LlibreDaoCore(Connection con) { this.con = con; }

    public synchronized ArrayList<Llibre> getAll() {
        ArrayList<Llibre> biblio = new ArrayList<>();
        try {
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(
                    "SELECT " + LLIBRE_COLUMNS_L_LIGHT + " FROM llibre l ORDER BY l.ISBN")) {
                while (rs.next()) {
                    Llibre l = LlibreMapper.buildLlibreLight(rs);
                    String autor = l.getAutor();
                    if (autor != null && !autor.isBlank()) {
                        l.setAutors(java.util.Arrays.asList(autor.split(", ", -1)));
                    }
                    biblio.add(l);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error al agafar tots els llibres: " + e.getMessage(), e);
        }
        return biblio;
    }

    public synchronized ArrayList<Llibre> getRecentlyAdded(int n) {
        ArrayList<Llibre> result = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT " + LLIBRE_COLUMNS + " FROM llibre ORDER BY data_afegit DESC LIMIT ?")) {
                ps.setInt(1, n);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(LlibreMapper.buildLlibre(rs));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els llibres recents: " + e.getMessage(), e);
        }
        return result;
    }

    public synchronized void insert(Llibre ll) throws SQLException {
        if (ll == null) return;
        withTransaction(() -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO llibre (`ISBN`,`nom`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`imatge_blob`," +
                    "`notes`,`pagines`,`pagines_llegides`,`editorial`,`serie`,`volum`,`data_compra`,`data_lectura`," +
                    "`idioma`,`format`,`desitjat`,`pais_origen`,`estat`,`exemplars`,`llengua_original`," +
                    "`nom_ca`,`nom_es`,`nom_en`) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                Object[] vals = LlibreFieldBindings.forInsert(ll);
                for (int i = 0; i < vals.length; i++) LlibreFieldBindings.bind(ps, i + 1, vals[i]);
                ps.execute();
            }
            List<String> autorsSync = ll.getAutors().isEmpty() && ll.getAutor() != null && !ll.getAutor().isBlank()
                ? List.of(ll.getAutor()) : ll.getAutors();
            if (!autorsSync.isEmpty()) syncAutors(ll.getISBN(), autorsSync);
        });
    }

    public synchronized void update(Llibre ll) throws SQLException {
        if (ll == null) return;
        withTransaction(() -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE llibre SET `nom`=?,`any`=?,`descripcio`=?,`valoracio`=?,`preu`=?,`llegit`=?,`imatge`=?," +
                    "`notes`=?,`pagines`=?,`pagines_llegides`=?,`editorial`=?,`serie`=?,`volum`=?," +
                    "`data_compra`=?,`data_lectura`=?,`idioma`=?,`format`=?,`desitjat`=?,`pais_origen`=?," +
                    "`estat`=?,`exemplars`=?,`llengua_original`=?," +
                    "`nom_ca`=?,`nom_es`=?,`nom_en`=? WHERE `ISBN`=?")) {
                Object[] vals = LlibreFieldBindings.forUpdate(ll);
                for (int i = 0; i < vals.length; i++) LlibreFieldBindings.bind(ps, i + 1, vals[i]);
                ps.setLong(vals.length + 1, ll.getISBN());
                ps.execute();
            }
            List<String> autorsSync = ll.getAutors().isEmpty() && ll.getAutor() != null && !ll.getAutor().isBlank()
                ? List.of(ll.getAutor()) : ll.getAutors();
            syncAutors(ll.getISBN(), autorsSync);
        });
    }

    public synchronized void delete(long isbn) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM llibre WHERE ISBN = ?")) {
            ps.setLong(1, isbn);
            ps.execute();
        }
    }

    public synchronized void delete(Llibre ll) throws SQLException {
        if (ll != null) delete(ll.getISBN());
    }

    public synchronized int count() {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM llibre");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error comptant llibres: " + e.getMessage(), e);
        }
        return 0;
    }

    public synchronized void clearAllData() throws SQLException {
        withTransaction(() -> {
            try (Statement s = con.createStatement()) {
                for (String t : Schema.CLEAR_ORDER) s.executeUpdate("DELETE FROM " + t);
            }
        });
    }

    public synchronized long getDbSizeBytes() {
        try {
            String url = con.getMetaData().getURL();
            if (url != null && url.startsWith("jdbc:h2:")) {
                String path = url.replaceFirst("jdbc:h2:", "").replaceAll(";.*", "").trim().replaceAll("\\s+", "");
                if (path.startsWith("mem:") || path.startsWith("mem/")) return -1;
                if (path.startsWith("file:")) path = path.substring(5).trim().replaceAll("\\s+", "");
                if (path.startsWith("~")) path = (System.getProperty("user.home") + path.substring(1)).trim().replaceAll("\\s+", "");
                java.io.File f = new java.io.File(path + ".mv.db");
                return f.exists() ? f.length() : -1;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public synchronized void executeSQLFile(java.io.File file) throws java.io.IOException, java.sql.SQLException {
        withTransaction(() -> {
            byte[] bytes;
            try {
                bytes = java.nio.file.Files.readAllBytes(file.toPath());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            List<String> statements = splitAndStripComments(content);
            try (Statement st = con.createStatement()) {
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (trimmed.isEmpty()) continue;
                    if (!isAllowed(trimmed)) continue;
                    st.execute(trimmed);
                }
            }
        });
    }

    /**
     * Single-pass SQL scanner: splits the file into statements on top-level
     * {@code ;} <em>and</em> strips {@code --} line comments, both honouring
     * SQL single-quoted string literals.
     */
    private static List<String> splitAndStripComments(String sql) {
        List<String> stmts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inQuote) {
                if (c == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        current.append("''");
                        i++;
                    } else {
                        inQuote = false;
                        current.append(c);
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '\'') {
                    inQuote = true;
                    current.append(c);
                } else if (c == ';' ) {
                    stmts.add(current.toString());
                    current = new StringBuilder();
                } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                    while (i < sql.length() && sql.charAt(i) != '\n') i++;
                    if (i < sql.length()) current.append('\n');
                } else {
                    current.append(c);
                }
            }
        }
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) stmts.add(remaining);
        return stmts;
    }

    /** Allow-list of SQL statement types acceptable in a backup file. {@code SET}
     *  is intentionally NOT included: a MariaDB {@code SET @@global.*} would
     *  otherwise pass this check. BackupService never writes {@code SET}
     *  statements, so the restore path does not need them. */
    private static final String[] ALLOWED_LEAD = {
        "INSERT ", "VALUES ", "BEGIN", "COMMIT", "ROLLBACK"
    };

    private static boolean isAllowed(String sql) {
        String upper = sql.toUpperCase(java.util.Locale.ROOT);
        for (String kw : ALLOWED_LEAD) {
            if (upper.startsWith(kw)) return true;
        }
        return false;
    }

    @FunctionalInterface private interface SqlWork { void run() throws SQLException; }

    private void withTransaction(SqlWork work) throws SQLException {
        boolean prev = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            work.run();
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(prev);
        }
    }

    private void syncAutors(long isbn, List<String> autors) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM llibre_autor WHERE isbn = ?")) {
            del.setLong(1, isbn);
            del.execute();
        }
        if (autors == null || autors.isEmpty()) return;
        try (PreparedStatement insAutor = con.prepareStatement("INSERT IGNORE INTO autor (nom) VALUES (?)")) {
            for (String nom : autors) {
                if (nom == null || nom.isBlank()) continue;
                insAutor.setString(1, nom);
                insAutor.addBatch();
            }
            insAutor.executeBatch();
        }
        try (PreparedStatement link = con.prepareStatement(
                "INSERT IGNORE INTO llibre_autor (isbn, autor_id) SELECT ?, id FROM autor WHERE nom = ?")) {
            for (String nom : autors) {
                if (nom == null || nom.isBlank()) continue;
                link.setLong(1, isbn);
                link.setString(2, nom);
                link.addBatch();
            }
            link.executeBatch();
        }
    }
}
