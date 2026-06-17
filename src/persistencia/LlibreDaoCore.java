package persistencia;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    /**
     * Single source of truth for the {@code llibre} column layout. The
     * three SELECT projections ({@link #LLIBRE_COLUMNS_L_LIGHT},
     * {@link #LLIBRE_COLUMNS_L}, {@link #LLIBRE_COLUMNS}) and the
     * INSERT/UPDATE statements below are all derived from this array —
     * adding a new column to the schema means adding one entry here
     * and the rest stays in lockstep automatically. The {@code autor}
     * and {@code has_blob} columns are subqueries, so their
     * {@code selectFragment} is the full subselect (not a column
     * reference) and their {@code bind} is a no-op (no placeholders
     * to bind).
     */
    private enum Column {
        ISBN("ISBN", "l.ISBN", "ISBN", true, false, false),
        NOM("nom", "l.nom", "nom", true, false, true),
        AUTOR_SUBQUERY("autor",
            "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor",
            null, false, false, false),
        ANY("any", "l.`any`", "any", true, false, true),
        DESCRIPCIO("descripcio", "l.descripcio", "descripcio", true, true, true),
        VALORACIO("valoracio", "l.valoracio", "valoracio", true, false, true),
        PREU("preu", "l.preu", "preu", true, false, true),
        LLEGIT("llegit", "l.llegit", "llegit", true, false, true),
        IMATGE("imatge", "l.imatge", "imatge", true, false, true),
        // imatge_blob is in INSERT (cover upload via LlibreBlobDao is
        // separate) but not in UPDATE — the cover lives in the dedicated
        // llibre_blob setBlob path, and the updateLlibre facade only
        // touches metadata. Heavy = not selected by LIGHT view (loaded
        // lazily by LlibreBlobDao).
        IMATGE_BLOB("imatge_blob", null, "imatge_blob", true, true, false),
        HAS_BLOB_SUBQUERY("has_blob",
            "(l.imatge_blob IS NOT NULL) AS has_blob",
            null, false, false, false),
        NOTES("notes", "l.notes", "notes", true, true, true),
        PAGINES("pagines", "l.pagines", "pagines", true, false, true),
        PAGINES_LLEGIDES("pagines_llegides", "l.pagines_llegides", "pagines_llegides", true, false, true),
        EDITORIAL("editorial", "l.editorial", "editorial", true, false, true),
        SERIE("serie", "l.serie", "serie", true, false, true),
        VOLUM("volum", "l.volum", "volum", true, false, true),
        DATA_COMPRA("data_compra", "l.data_compra", "data_compra", true, false, true),
        DATA_LECTURA("data_lectura", "l.data_lectura", "data_lectura", true, false, true),
        IDIOMA("idioma", "l.idioma", "idioma", true, false, true),
        FORMAT("format", "l.format", "format", true, false, true),
        DESITJAT("desitjat", "l.desitjat", "desitjat", true, false, true),
        PAIS_ORIGEN("pais_origen", "l.pais_origen", "pais_origen", true, false, true),
        ESTAT("estat", "l.estat", "estat", true, false, true),
        EXEMPLARS("exemplars", "l.exemplars", "exemplars", true, false, true),
        LLENGUA_ORIGINAL("llengua_original", "l.llengua_original", "llengua_original", true, false, true),
        NOM_CA("nom_ca", "l.nom_ca", "nom_ca", true, false, true),
        NOM_ES("nom_es", "l.nom_es", "nom_es", true, false, true),
        NOM_EN("nom_en", "l.nom_en", "nom_en", true, false, true);

        final String name;            // logical / DB column name
        final String selectFragment;  // "l.col" or a subquery for SELECT
        final String insertColumn;    // column name in INSERT (null = skip in INSERT)
        final boolean bindable;       // whether this column has a bind value
        final boolean heavy;          // true = heavy field (excluded from LIGHT view)
        final boolean inUpdate;       // false = INSERT only (ISBN, imatge_blob)

        Column(String name, String selectFragment, String insertColumn,
               boolean bindable, boolean heavy, boolean inUpdate) {
            this.name = name;
            this.selectFragment = selectFragment;
            this.insertColumn = insertColumn;
            this.bindable = bindable;
            this.heavy = heavy;
            this.inUpdate = inUpdate;
        }
    }

    /**
     * Compose the SELECT column list with the {@code l.}-prefixed alias
     * and no heavy fields (descripcio / notes). Used by {@link #getAll}
     * and any caller that wants a cheap row.
     */
    static final String LLIBRE_COLUMNS_L_LIGHT = buildSelect(/*prefixed*/ true, /*heavy*/ false);
    /** Same shape as {@link #LLIBRE_COLUMNS_L_LIGHT} but with descripcio + notes included. */
    static final String LLIBRE_COLUMNS_L = buildSelect(true, true);
    /** No alias prefix (used by queries without a FROM alias — none today, kept for completeness). */
    @SuppressWarnings("unused")
    static final String LLIBRE_COLUMNS = buildSelect(false, true);

    private static String buildSelect(boolean prefixed, boolean includeHeavy) {
        StringBuilder sb = new StringBuilder(512);
        for (Column c : Column.values()) {
            if (c.heavy && !includeHeavy) continue;
            // Columns with no SELECT fragment (e.g. IMATGE_BLOB, which
            // is bound but never projected — the heavy blob lives in a
            // separate LlibreBlobDao call) are skipped here.
            if (c.selectFragment == null) continue;
            if (sb.length() > 0) sb.append(", ");
            // All Column.selectFragment values already carry the `l.`
            // prefix; for the unprefixed view we strip it.
            String frag = c.selectFragment;
            if (!prefixed) frag = frag.replace("l.", "");
            sb.append(frag);
        }
        return sb.toString();
    }

    /** Build the {@code (col1, col2, ...)} part of the INSERT statement
     *  from the {@link Column} enum, excluding the subquery-only entries
     *  (autor / has_blob) which are computed, not bound. */
    private static String insertColumnList() {
        StringBuilder sb = new StringBuilder(256);
        for (Column c : Column.values()) {
            if (c.insertColumn == null) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append("`").append(c.insertColumn).append("`");
        }
        return sb.toString();
    }

    /** Count of bindable columns — must match the value-list length from
     *  {@link LlibreFieldBindings#forInsert(Llibre)} (ISBN + 25
     *  non-subquery fields + imatge_blob = 27). Used to size the
     *  placeholder list. */
    private static int insertColumnCount() {
        int n = 0;
        for (Column c : Column.values()) if (c.insertColumn != null) n++;
        return n;
    }

    /** Build the {@code col=?, col=?, ...} part of the UPDATE statement.
     *  Excludes ISBN (WHERE clause supplies it) and imatge_blob (cover
     *  lives in the dedicated setBlob path, not the metadata UPDATE). */
    private static String updateSetList() {
        StringBuilder sb = new StringBuilder(256);
        for (Column c : Column.values()) {
            if (c.insertColumn == null) continue;
            if (!c.inUpdate) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append("`").append(c.insertColumn).append("`=?");
        }
        return sb.toString();
    }

    /** Number of {@code ?} placeholders in the UPDATE SET list. Must
     *  match the value-list length from
     *  {@link LlibreFieldBindings#forUpdate(Llibre)} (25 = no ISBN,
     *  no imatge_blob). The WHERE ISBN is bound separately by the
     *  caller. */
    private static int updateSetCount() {
        int n = 0;
        for (Column c : Column.values()) {
            if (c.insertColumn != null && c.inUpdate) n++;
        }
        return n;
    }

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
            // The FROM clause has no `l` alias, so this SELECT must use
            // the unprefixed column list (LLIBRE_COLUMNS). The LlibreMapper
            // looks up columns by name (not by index), so the resolution
            // is independent of the projection order.
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
                    "INSERT INTO llibre (" + insertColumnList() + ") VALUES (" + placeholders(insertColumnCount()) + ")")) {
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
                    "UPDATE llibre SET " + updateSetList() + " WHERE `ISBN`=?")) {
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
        withTransaction(this::clearAllDataNoTx);
    }

    /** Body of {@link #clearAllData()} without a surrounding transaction.
     *  Used by {@link #restoreFromSQL(java.io.File)} to compose the
     *  clear + execute into a single transaction. */
    private void clearAllDataNoTx() throws SQLException {
        try (Statement s = con.createStatement()) {
            for (String t : Schema.CLEAR_ORDER) s.executeUpdate("DELETE FROM " + t);
        }
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

    /**
     * Restore from a SQL backup. Previously read the entire file into a
     * byte[] + a String (peak memory ≈ 4× file size for a 50 MB backup).
     * Now streams the file line-by-line and accumulates one statement
     * at a time in a StringBuilder. The accumulator is bounded by the
     * largest single statement in the file (a few KB for typical
     * inserts), so peak memory is O(max-statement-size), not O(file).
     */
    public synchronized void executeSQLFile(java.io.File file) throws java.io.IOException, java.sql.SQLException {
        withTransaction(() -> {
            try { executeSQLFileNoTx(file); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
    }

    /** Body of {@link #executeSQLFile(java.io.File)} without a surrounding
     *  transaction. Lets {@link #restoreFromSQL(java.io.File)} compose the
     *  clear + file-execute into a single transaction so a mid-stream
     *  {@link SQLException} rolls back both.
     *  <p>Reads line-by-line via {@code br.lines().forEach(...)} so the
     *  parser sees full Unicode strings (including non-BMP code points
     *  encoded as 4-byte UTF-8). The earlier per-code-unit read broke
     *  surrogate-pair handling for emoji and CJK ideographs: a 4-byte
     *  UTF-8 character is two UTF-16 code units, and the comment/quote
     *  state machine split the pair between iterations. Streaming by
     *  line keeps multi-line string literals and their content as a
     *  single String, and lets {@code Statement.execute} receive the
     *  exact bytes the JDBC driver will encode. */
    private void executeSQLFileNoTx(java.io.File file) throws java.io.IOException, java.sql.SQLException {
        try (BufferedReader br = new BufferedReader(
                new FileReader(file, java.nio.charset.StandardCharsets.UTF_8));
             Statement st = con.createStatement()) {
            StringBuilder current = new StringBuilder(256);
            boolean[] inQuote = { false };
            br.lines().forEach(line -> processLine(line, current, inQuote, st));
            String remaining = current.toString().trim();
            if (!remaining.isEmpty() && isAllowed(remaining)) st.execute(remaining);
        }
    }

    private void processLine(String line, StringBuilder current, boolean[] inQuote, Statement st) {
        int i = 0;
        int len = line.length();
        while (i < len) {
            char ch = line.charAt(i);
            if (inQuote[0]) {
                if (ch == '\'') {
                    if (i + 1 < len && line.charAt(i + 1) == '\'') {
                        // SQL '' escape: keep both quotes in the statement.
                        current.append("''");
                        i += 2;
                        continue;
                    }
                    // Closing quote: keep the single quote and exit the literal.
                    current.append(ch);
                    inQuote[0] = false;
                    i++;
                } else {
                    current.append(ch);
                    i++;
                }
            } else if (ch == '\'') {
                inQuote[0] = true;
                current.append(ch);
                i++;
            } else if (ch == '-' && i + 1 < len && line.charAt(i + 1) == '-') {
                // -- line comment: drop the rest of the line.
                return;
            } else if (ch == ';') {
                String stmt = current.toString().trim();
                current.setLength(0);
                if (!stmt.isEmpty() && isAllowed(stmt)) {
                    try { st.execute(stmt); }
                    catch (SQLException e) { throw new RuntimeException(e); }
                }
                i++;
            } else {
                current.append(ch);
                i++;
            }
        }
    }

    /**
     * Restore the database from a SQL backup file: clears all data and
     * executes the file's statements inside a single JDBC transaction.
     * If the file execution fails partway through, the {@code DELETE}
     * statements are rolled back along with the partial restore, and
     * the database is left unchanged. The caller (BackupDelegate) is
     * still responsible for taking a pre-restore snapshot for the
     * user-initiated undo path; this method is the safety net.
     */
    public synchronized void restoreFromSQL(java.io.File file) throws java.io.IOException, java.sql.SQLException {
        withTransaction(() -> {
            clearAllDataNoTx();
            try { executeSQLFileNoTx(file); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
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

    /** {@code ?, ?, ?} × {@code n}. Helper for the INSERT statement. */
    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) sb.append(i == 0 ? "?" : ",?");
        return sb.toString();
    }

    @FunctionalInterface private interface SqlWork { void run() throws Exception; }

    private void withTransaction(SqlWork work) throws SQLException {
        boolean prev = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            work.run();
            con.commit();
        } catch (Exception e) {
            try { con.rollback(); }
            catch (SQLException re) { /* original exception is what matters */ }
            if (e instanceof SQLException) throw (SQLException) e;
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new SQLException(e);
        } finally {
            try { con.setAutoCommit(prev); }
            catch (SQLException ae) { /* ignore */ }
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
