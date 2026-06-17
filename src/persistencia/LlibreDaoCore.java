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
 * CRUD principal sobre la taula {@code llibre}: insert / update / delete /
 * getAll / getRecentlyAdded / count / clearAllData / executeSQLFile /
 * getDbSizeBytes. La cerca viu a {@link LlibreSearchDao}, l'accés a blobs
 * a {@link LlibreBlobDao}, i les sessions de lectura a {@link LlibreLecturaDao}.
 * Tots quatre DAOs comparteixen la mateixa {@link Connection} que té
 * {@link ControladorPersistencia}.
 */
public class LlibreDaoCore {

    private final Connection con;

    /**
     * Font única de veritat per a l'esquema de columnes de {@code llibre}.
     * Les tres projeccions SELECT ({@link #LLIBRE_COLUMNS_L_LIGHT},
     * {@link #LLIBRE_COLUMNS_L}, {@link #LLIBRE_COLUMNS}) i les sentències
     * INSERT/UPDATE següents deriven totes d'aquest array — afegir una
     * columna nova a l'esquema significa afegir una entrada aquí i la
     * resta es manté en sincronia automàticament. Les columnes
     * {@code autor} i {@code has_blob} són subconsultes, de manera que
     * el seu {@code selectFragment} és la subselect sencera (no una
     * referència de columna) i el seu {@code bind} és no-op (no hi ha
     * placeholders per lligar).
     */
    private enum Columna {
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
        // imatge_blob és a l'INSERT (la pujada de coberta via LlibreBlobDao
        // és independent) però no a l'UPDATE — la coberta viu al camí
        // dedicat setBlob de llibre_blob, i la façana updateLlibre només
        // toca metadades. Pesat = no seleccionat per la vista LIGHT
        // (es carrega mandrosament des de LlibreBlobDao).
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

        final String name;            // nom lògic / de columna a la BBDD
        final String selectFragment;  // "l.col" o una subconsulta per a SELECT
        final String inserirColumn;    // nom de columna a l'INSERT (null = omet a l'INSERT)
        final boolean bindable;       // si aquesta columna té valor lligat
        final boolean heavy;          // true = camp pesat (exclòs de la vista LIGHT)
        final boolean inUpdate;       // false = només INSERT (ISBN, imatge_blob)

        Columna(String name, String selectFragment, String inserirColumn,
               boolean bindable, boolean heavy, boolean inUpdate) {
            this.name = name;
            this.selectFragment = selectFragment;
            this.inserirColumn = inserirColumn;
            this.bindable = bindable;
            this.heavy = heavy;
            this.inUpdate = inUpdate;
        }
    }

    /**
     * Composa la llista de columnes SELECT amb l'àlies {@code l.}-prefixada
     * i sense camps pesats (descripcio / notes). La fan servir {@link #getAll}
     * i qualsevol consumidor que vulgui una fila barata.
     */
    static final String LLIBRE_COLUMNS_L_LIGHT = buildSelect(/*prefixed*/ true, /*heavy*/ false);
    /** Mateixa forma que {@link #LLIBRE_COLUMNS_L_LIGHT} però amb descripcio + notes inclosos. */
    static final String LLIBRE_COLUMNS_L = buildSelect(true, true);
    /** Sense prefix d'àlies (per a consultes sense àlies al FROM — cap avui, es conserva per completesa). */
    @SuppressWarnings("unused")
    static final String LLIBRE_COLUMNS = buildSelect(false, true);

    private static String buildSelect(boolean prefixed, boolean includeHeavy) {
        StringBuilder sb = new StringBuilder(512);
        for (Columna c : Columna.values()) {
            if (c.heavy && !includeHeavy) continue;
            // Les columnes sense fragment SELECT (p. ex. IMATGE_BLOB,
            // que es lliga però no es projecta — el blob pesat viu en
            // una crida independent de LlibreBlobDao) se salten aquí.
            if (c.selectFragment == null) continue;
            if (sb.length() > 0) sb.append(", ");
            // Tots els valors de Column.selectFragment ja porten el
            // prefix `l.`; per a la vista sense prefix l'esborrem.
            String frag = c.selectFragment;
            if (!prefixed) frag = frag.replace("l.", "");
            sb.append(frag);
        }
        return sb.toString();
    }

    /** Construeix la part {@code (col1, col2, ...)} de la sentència INSERT
     *  a partir de l'enum {@link Column}, excloent les entrades només de
     *  subconsulta (autor / has_blob) que es calculen, no es lliguen. */
    private static String inserirColumnList() {
        StringBuilder sb = new StringBuilder(256);
        for (Columna c : Columna.values()) {
            if (c.inserirColumn == null) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append("`").append(c.inserirColumn).append("`");
        }
        return sb.toString();
    }

    /** Recompte de columnes lligables — ha de coincidir amb la longitud de
     *  la llista de valors de {@link LlibreFieldBindings#forInsert(Llibre)}
     *  (ISBN + 25 camps sense subconsulta + imatge_blob = 27). S'usa per
     *  dimensionar la llista de placeholders. */
    private static int inserirColumnCount() {
        int n = 0;
        for (Columna c : Columna.values()) if (c.inserirColumn != null) n++;
        return n;
    }

    /** Construeix la part {@code col=?, col=?, ...} de la sentència UPDATE.
     *  Exclou ISBN (la clàusula WHERE el proporciona) i imatge_blob (la
     *  coberta viu al camí dedicat setBlob, no a l'UPDATE de metadades). */
    private static String actualitzarSetList() {
        StringBuilder sb = new StringBuilder(256);
        for (Columna c : Columna.values()) {
            if (c.inserirColumn == null) continue;
            if (!c.inUpdate) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append("`").append(c.inserirColumn).append("`=?");
        }
        return sb.toString();
    }

    /** Nombre de placeholders {@code ?} a la llista SET de l'UPDATE. Ha
     *  de coincidir amb la longitud de la llista de valors de
     *  {@link LlibreFieldBindings#forUpdate(Llibre)} (25 = sense ISBN,
     *  sense imatge_blob). El WHERE ISBN el lliga per separat el
     *  consumidor. */
    private static int actualitzarSetCount() {
        int n = 0;
        for (Columna c : Columna.values()) {
            if (c.inserirColumn != null && c.inUpdate) n++;
        }
        return n;
    }

    LlibreDaoCore(Connection con) { this.con = con; }

    public synchronized ArrayList<Llibre> obtenirAll() {
        ArrayList<Llibre> biblio = new ArrayList<>();
        try {
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(
                    "SELECT " + LLIBRE_COLUMNS_L_LIGHT + " FROM llibre l ORDER BY l.ISBN")) {
                while (rs.next()) {
                    Llibre l = LlibreMapper.buildLlibreLight(rs);
                    String autor = l.obtenirAutor();
                    if (autor != null && !autor.isBlank()) {
                        l.posarAutors(java.util.Arrays.asList(autor.split(", ", -1)));
                    }
                    biblio.add(l);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error al agafar tots els llibres: " + e.getMessage(), e);
        }
        return biblio;
    }

    public synchronized ArrayList<Llibre> obtenirRecentlyAdded(int n) {
        ArrayList<Llibre> result = new ArrayList<>();
        try {
            // La clàusula FROM no té àlies `l`, de manera que aquest
            // SELECT ha d'utilitzar la llista de columnes sense prefix
            // (LLIBRE_COLUMNS). LlibreMapper cerca les columnes per nom
            // (no per índex), de manera que la resolució és independent
            // de l'ordre de projecció.
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
                    "INSERT INTO llibre (" + inserirColumnList() + ") VALUES (" + placeholders(inserirColumnCount()) + ")")) {
                Object[] vals = LlibreFieldBindings.forInsert(ll);
                for (int i = 0; i < vals.length; i++) LlibreFieldBindings.bind(ps, i + 1, vals[i]);
                ps.execute();
            }
            List<String> autorsSync = ll.obtenirAutors().isEmpty() && ll.obtenirAutor() != null && !ll.obtenirAutor().isBlank()
                ? List.of(ll.obtenirAutor()) : ll.obtenirAutors();
            if (!autorsSync.isEmpty()) sincronitzarAutors(ll.obtenirISBN(), autorsSync);
        });
    }

    public synchronized void update(Llibre ll) throws SQLException {
        if (ll == null) return;
        withTransaction(() -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE llibre SET " + actualitzarSetList() + " WHERE `ISBN`=?")) {
                Object[] vals = LlibreFieldBindings.forUpdate(ll);
                for (int i = 0; i < vals.length; i++) LlibreFieldBindings.bind(ps, i + 1, vals[i]);
                ps.setLong(vals.length + 1, ll.obtenirISBN());
                ps.execute();
            }
            List<String> autorsSync = ll.obtenirAutors().isEmpty() && ll.obtenirAutor() != null && !ll.obtenirAutor().isBlank()
                ? List.of(ll.obtenirAutor()) : ll.obtenirAutors();
            sincronitzarAutors(ll.obtenirISBN(), autorsSync);
        });
    }

    public synchronized void delete(long isbn) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM llibre WHERE ISBN = ?")) {
            ps.setLong(1, isbn);
            ps.execute();
        }
    }

    public synchronized void delete(Llibre ll) throws SQLException {
        if (ll != null) delete(ll.obtenirISBN());
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

    public synchronized void netejarAllData() throws SQLException {
        withTransaction(this::netejarAllDataNoTx);
    }

    /** Cos de {@link #clearAllData()} sense la transacció envoltant.
     *  El fa servir {@link #restoreFromSQL(java.io.File)} per compondre
     *  el clear + execute en una sola transacció. */
    private void netejarAllDataNoTx() throws SQLException {
        try (Statement s = con.createStatement()) {
            for (String t : Schema.CLEAR_ORDER) s.executeUpdate("DELETE FROM " + t);
        }
    }

    public synchronized long obtenirDbSizeBytes() {
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
     * Restaura des d'una còpia de seguretat SQL. Abans llegia tot el
     * fitxer a un byte[] + un String (pic de memòria ≈ 4× la mida del
     * fitxer per a una còpia de 50 MB). Ara transmet el fitxer línia a
     * línia i acumula una sentència a la vegada en un StringBuilder.
     * L'acumulador està limitat per la sentència individual més gran
     * del fitxer (uns quants KB per a insercions típiques), de manera
     * que el pic de memòria és O(mida-màxima-sentència), no O(fitxer).
     */
    public synchronized void executarSQLFile(java.io.File file) throws java.io.IOException, java.sql.SQLException {
        withTransaction(() -> {
            try { executarSQLFileNoTx(file); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
    }

    /** Cos de {@link #executeSQLFile(java.io.File)} sense la transacció
     *  envoltant. Permet a {@link #restoreFromSQL(java.io.File)} compondre
     *  el clear + file-execute en una sola transacció perquè una
     *  {@link SQLException} a meitat del flux reverteixi les dues.
     *  <p>Llegeix línia a línia amb {@code br.lines().forEach(...)} de
     *  manera que l'analitzador vegi cadenes Unicode completes (inclosos
     *  punts de codi fora del BMP codificats com a UTF-8 de 4 bytes).
     *  L'antiga lectura per unitat de codi trencava el tractament de
     *  parelles substitutives per a emoji i ideogrames CJK: un caràcter
     *  UTF-8 de 4 bytes són dues unitats de codi UTF-16, i la màquina
     *  d'estats de comentari/cometes partia la parella entre iteracions.
     *  La transmissió per línia manté els literals de cadena multilínia
     *  i el seu contingut com un sol String, i permet a
     *  {@code Statement.execute} rebre els bytes exactes que el driver
     *  JDBC codificarà. */
    private void executarSQLFileNoTx(java.io.File file) throws java.io.IOException, java.sql.SQLException {
        try (BufferedReader br = new BufferedReader(
                new FileReader(file, java.nio.charset.StandardCharsets.UTF_8));
             Statement st = con.createStatement()) {
            StringBuilder current = new StringBuilder(256);
            boolean[] inQuote = { false };
            br.lines().forEach(line -> processarLine(line, current, inQuote, st));
            String remaining = current.toString().trim();
            if (!remaining.isEmpty() && esAllowed(remaining)) st.execute(remaining);
        }
    }

    private void processarLine(String line, StringBuilder current, boolean[] inQuote, Statement st) {
        int i = 0;
        int len = line.length();
        while (i < len) {
            char ch = line.charAt(i);
            if (inQuote[0]) {
                    if (ch == '\'') {
                        if (i + 1 < len && line.charAt(i + 1) == '\'') {
                            // Escapament SQL '': conserva les dues cometes a la sentència.
                            current.append("''");
                            i += 2;
                            continue;
                        }
                        // Cometa de tancament: conserva la cometa simple i surt del literal.
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
                    // -- comentari de línia: descarta la resta de la línia.
                    return;
                } else if (ch == ';') {
                String stmt = current.toString().trim();
                current.setLength(0);
                if (!stmt.isEmpty() && esAllowed(stmt)) {
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
     * Restaura la base de dades des d'un fitxer de còpia de seguretat SQL:
     * esborra totes les dades i executa les sentències del fitxer dins
     * d'una sola transacció JDBC. Si l'execució del fitxer falla a
     * meitat, les sentències {@code DELETE} es reverteixen juntament
     * amb la restauració parcial, i la base de dades queda intacta. El
     * consumidor (BackupDelegate) continua sent responsable de fer una
     * captura pre-restauració pel camí d'undoing iniciat per l'usuari;
     * aquest mètode és la xarxa de seguretat.
     */
    public synchronized void restaurarFromSQL(java.io.File file) throws java.io.IOException, java.sql.SQLException {
        withTransaction(() -> {
            netejarAllDataNoTx();
            try { executarSQLFileNoTx(file); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
    }

    /** Llista blanca de tipus de sentència SQL acceptables en un fitxer
     *  de còpia de seguretat. {@code SET} s'ha exclòs intencionadament:
     *  un {@code SET @@global.*} de MariaDB passaria la comprovació.
     *  BackupService no escriu mai sentències {@code SET}, de manera
     *  que el camí de restauració no les necessita. */
    private static final String[] ALLOWED_LEAD = {
        "INSERT ", "VALUES ", "BEGIN", "COMMIT", "ROLLBACK"
    };

    private static boolean esAllowed(String sql) {
        String upper = sql.toUpperCase(java.util.Locale.ROOT);
        for (String kw : ALLOWED_LEAD) {
            if (upper.startsWith(kw)) return true;
        }
        return false;
    }

    /** {@code ?, ?, ?} × {@code n}. Helper per a la sentència INSERT. */
    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) sb.append(i == 0 ? "?" : ",?");
        return sb.toString();
    }

    @FunctionalInterface private interface TreballSql { void run() throws Exception; }

    private void withTransaction(TreballSql work) throws SQLException {
        boolean prev = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            work.run();
            con.commit();
        } catch (Exception e) {
            try { con.rollback(); }
            catch (SQLException re) { /* l'excepció original és la que importa */ }
            if (e instanceof SQLException) throw (SQLException) e;
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new SQLException(e);
        } finally {
            try { con.setAutoCommit(prev); }
            catch (SQLException ae) { /* ignora */ }
        }
    }

    private void sincronitzarAutors(long isbn, List<String> autors) throws SQLException {
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
