package persistencia;

import domini.Llibre;
import java.sql.*;
import java.util.ArrayList;

public class LlibreDao {

    private final Connection con;

    static final String LLIBRE_SELECT =
        "SELECT ISBN, nom, " +
        "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = ISBN) AS autor, " +
        "`any`, descripcio, valoracio, preu, llegit, imatge, " +
        "(imatge_blob IS NOT NULL) AS has_blob, notes, pagines, pagines_llegides, editorial, serie, " +
        "volum, data_compra, data_lectura, idioma, format, desitjat, pais_origen, estat, exemplars, llengua_original, " +
        "nom_ca, nom_es, nom_en ";

    LlibreDao(Connection con) { this.con = con; }

    static Llibre buildLlibreLight(ResultSet rs) throws SQLException {
        Llibre l = Llibre.builder()
            .isbn(rs.getLong("ISBN"))
            .nom(rs.getString("nom"))
            .autor(rs.getString("autor"))
            .any(rs.getObject("any", Integer.class))
            .descripcio(null)
            .valoracio(rs.getObject("valoracio", Double.class))
            .preu(rs.getObject("preu", Double.class))
            .llegit(rs.getBoolean("llegit"))
            .imatge(rs.getString("imatge"))
            .build();
        l.setHasBlob(rs.getBoolean("has_blob"));
        fillLlibreTail(l, rs, false);
        l.setHeavyFieldsLoaded(false);
        return l;
    }

    static Llibre buildLlibre(ResultSet rs) throws SQLException {
        Llibre l = Llibre.builder()
            .isbn(rs.getLong("ISBN"))
            .nom(rs.getString("nom"))
            .autor(rs.getString("autor"))
            .any(rs.getObject("any", Integer.class))
            .descripcio(rs.getString("descripcio"))
            .valoracio(rs.getObject("valoracio", Double.class))
            .preu(rs.getObject("preu", Double.class))
            .llegit(rs.getBoolean("llegit"))
            .imatge(rs.getString("imatge"))
            .build();
        l.setHasBlob(rs.getBoolean("has_blob"));
        l.setNotes(rs.getString("notes"));
        fillLlibreTail(l, rs, true);
        return l;
    }

    private static void fillLlibreTail(Llibre l, ResultSet rs, boolean withHeavy) throws SQLException {
        if (withHeavy) {
            l.setNotes(rs.getString("notes"));
        }
        l.setPagines(rs.getInt("pagines"));
        l.setPaginesLlegides(rs.getInt("pagines_llegides"));
        l.setEditorial(rs.getString("editorial"));
        l.setSerie(rs.getString("serie"));
        l.setVolum(rs.getInt("volum"));
        l.setDataCompra(rs.getString("data_compra"));
        l.setDataLectura(rs.getString("data_lectura"));
        l.setIdioma(rs.getString("idioma"));
        l.setFormat(rs.getString("format"));
        l.setDesitjat(rs.getBoolean("desitjat"));
        l.setPaisOrigen(rs.getString("pais_origen"));
        l.setEstat(rs.getString("estat"));
        int exemplars = rs.getInt("exemplars");
        l.setExemplars(exemplars > 0 ? exemplars : 1);
        l.setLlenguaOriginal(rs.getString("llengua_original"));
        l.setNomCa(rs.getString("nom_ca"));
        l.setNomEs(rs.getString("nom_es"));
        l.setNomEn(rs.getString("nom_en"));
    }

    public synchronized void loadHeavyFields(long isbn, Llibre target) {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT descripcio, notes FROM llibre WHERE ISBN = ?")) {
            ps.setLong(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    target.setDescripcio(rs.getString("descripcio"));
                    target.setNotes(rs.getString("notes"));
                    target.setHeavyFieldsLoaded(true);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant camps pesats: " + e.getMessage(), e);
        }
    }

    private static final String LLIBRE_SELECT_L_LIGHT =
        "SELECT l.ISBN, l.nom, " +
        "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, " +
        "l.`any`, l.valoracio, l.preu, l.llegit, l.imatge, " +
        "(l.imatge_blob IS NOT NULL) AS has_blob, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
        "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, " +
        "l.nom_ca, l.nom_es, l.nom_en ";

    private static final String LLIBRE_SELECT_L =
        "SELECT l.ISBN, l.nom, " +
        "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, " +
        "l.`any`, l.descripcio, l.valoracio, l.preu, l.llegit, l.imatge, " +
        "(l.imatge_blob IS NOT NULL) AS has_blob, l.notes, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
        "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, " +
        "l.nom_ca, l.nom_es, l.nom_en ";

    public synchronized ArrayList<Llibre> getAll() {
        ArrayList<Llibre> biblio = new ArrayList<>();
        java.util.LinkedHashMap<Long, Llibre> byISBN = new java.util.LinkedHashMap<>();
        try {
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(
                    LLIBRE_SELECT_L_LIGHT + ", a.nom AS autor_nom FROM llibre l" +
                    " LEFT JOIN llibre_autor la ON l.ISBN = la.isbn" +
                    " LEFT JOIN autor a ON la.autor_id = a.id" +
                    " ORDER BY l.ISBN, a.nom")) {
                while (rs.next()) {
                    long isbn = rs.getLong("ISBN");
                    Llibre l = byISBN.get(isbn);
                    if (l == null) {
                        l = buildLlibreLight(rs);
                        byISBN.put(isbn, l);
                        biblio.add(l);
                    }
                    String autorNom = rs.getString("autor_nom");
                    if (autorNom != null) l.addAutorNom(autorNom);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error al agafar tots els llibres: " + e.getMessage(), e);
        }
        return biblio;
    }

    /**
     * Insereix un llibre nou a la BBDD. El binding dels 25 camps compartits
     * amb {@link #update} és gairebé idèntic; s'ha intentat extreure un
     * {@code bindLlibreFields} helper però la diferència de posicions
     * (INSERT té {@code imatge_blob} a la posició 9, UPDATE no) complica
     * l'extracció més del que estalvia — preferim la duplicació explícita
     * amb un {@code TODO} clar que un helper trencadís.
     */
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
            java.util.List<String> autorsSync = ll.getAutors().isEmpty() && ll.getAutor() != null && !ll.getAutor().isBlank()
                ? java.util.List.of(ll.getAutor()) : ll.getAutors();
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
            java.util.List<String> autorsSync = ll.getAutors().isEmpty() && ll.getAutor() != null && !ll.getAutor().isBlank()
                ? java.util.List.of(ll.getAutor()) : ll.getAutors();
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

    public synchronized ArrayList<Llibre> getRecentlyAdded(int n) {
        ArrayList<Llibre> result = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    LLIBRE_SELECT + "FROM llibre ORDER BY data_afegit DESC LIMIT ?")) {
                ps.setInt(1, n);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(buildLlibre(rs));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els llibres recents: " + e.getMessage(), e);
        }
        return result;
    }

    public synchronized byte[] getBlob(long isbn) {
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT imatge_blob FROM llibre WHERE ISBN = ?")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getBytes(1);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant la imatge del llibre: " + e.getMessage(), e);
        }
        return null;
    }

    public synchronized void setBlob(long isbn, byte[] blob) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llibre SET imatge_blob = ? WHERE ISBN = ?")) {
            ps.setBytes(1, blob);
            ps.setLong(2, isbn);
            ps.execute();
        }
    }

    /** Unified filter search; use {@link domini.LlibreFilter#empty()} for unconstrained lists. */
    public synchronized ArrayList<Llibre> search(domini.LlibreFilter f) {
        return search(f, 0, 0);
    }

    public synchronized ArrayList<Llibre> search(domini.LlibreFilter f, int offset, int pageSize) {
        ArrayList<Llibre> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT l.ISBN, l.nom, " +
            "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, " +
            "l.`any`, l.descripcio, l.valoracio, l.preu, l.llegit, l.imatge, " +
            "(l.imatge_blob IS NOT NULL) AS has_blob, l.notes, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
            "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, " +
            "l.nom_ca, l.nom_es, l.nom_en FROM llibre l");
        if (f.getLlistaId() != null) sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?");
        if (f.getTagId()    != null) sql.append(" JOIN llibre_tag lt ON l.ISBN = lt.isbn AND lt.tag_id = ?");
        sql.append(" WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (f.getLlistaId() != null) params.add(f.getLlistaId());
        if (f.getTagId()    != null) params.add(f.getTagId());
        if (f.getNom()          != null && !f.getNom().isBlank()) { sql.append(" AND (l.nom LIKE ? OR l.nom_ca LIKE ? OR l.nom_es LIKE ? OR l.nom_en LIKE ?)"); String p = "%" + f.getNom() + "%"; params.add(p); params.add(p); params.add(p); params.add(p); }
        if (f.getAutor()        != null && !f.getAutor().isBlank()) { sql.append(" AND EXISTS (SELECT 1 FROM llibre_autor la2 JOIN autor a2 ON la2.autor_id = a2.id WHERE la2.isbn = l.ISBN AND a2.nom LIKE ?)"); params.add("%" + f.getAutor() + "%"); }
        if (f.getIsbn()         != null) { sql.append(" AND l.ISBN = ?");          params.add(f.getIsbn()); }
        if (f.getAnyMin()       != null) { sql.append(" AND l.`any` >= ?");        params.add(f.getAnyMin()); }
        if (f.getAnyMax()       != null) { sql.append(" AND l.`any` <= ?");        params.add(f.getAnyMax()); }
        if (f.getValoracioMin() != null) { sql.append(" AND l.valoracio >= ?");    params.add(f.getValoracioMin()); }
        if (f.getValoracioMax() != null) { sql.append(" AND l.valoracio <= ?");    params.add(f.getValoracioMax()); }
        if (f.getPreuMin()      != null) { sql.append(" AND l.preu >= ?");         params.add(f.getPreuMin()); }
        if (f.getPreuMax()      != null) { sql.append(" AND l.preu <= ?");         params.add(f.getPreuMax()); }
        if (f.getLlegit()       != null) { sql.append(" AND l.llegit = ?");        params.add(f.getLlegit()); }
        if (f.getEditorial()    != null && !f.getEditorial().isBlank()) { sql.append(" AND l.editorial LIKE ?");  params.add("%" + f.getEditorial() + "%"); }
        if (f.getSerie()        != null && !f.getSerie().isBlank()) { sql.append(" AND l.serie LIKE ?");      params.add("%" + f.getSerie() + "%"); }
        if (f.getFormat()       != null && !f.getFormat().isBlank()) { sql.append(" AND l.format = ?");        params.add(f.getFormat()); }
        if (f.getIdioma()       != null && !f.getIdioma().isBlank()) { sql.append(" AND l.idioma LIKE ?");     params.add("%" + f.getIdioma() + "%"); }
        domini.SortSpec sort = f.getSort();
        if (sort == null) sort = domini.SortSpec.defaultAsc();
        sql.append(" ORDER BY ").append(sort.toSql());
        if (pageSize > 0) sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                bindParam(ps, i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(buildLlibre(rs));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error en searchLlibres: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Binds a single filter parameter to a {@link PreparedStatement} slot.
     * <p>Supported types (Java 21 pattern-matching switch):
     * <ul>
     *   <li>{@code String}  — bound as VARCHAR via {@code setString}.</li>
     *   <li>{@code Long}    — bound as BIGINT via {@code setLong}.</li>
     *   <li>{@code Integer} — bound as INT via {@code setInt}.</li>
     *   <li>{@code Double}  — bound as DOUBLE via {@code setDouble}.</li>
     *   <li>{@code Boolean} — bound as BIT via {@code setBoolean}.</li>
     *   <li>{@code null}    — bound as SQL NULL via {@code setObject(i, null)}.</li>
     * </ul>
     * Any other runtime type throws {@link IllegalArgumentException} with the
     * offending class name, so the caller sees exactly which filter value was
     * unhandled instead of silently producing a malformed query.
     */
    private static void bindParam(PreparedStatement ps, int index, Object p) throws SQLException {
        switch (p) {
            case String  s -> ps.setString(index, s);
            case Long    l -> ps.setLong(index, l);
            case Integer i -> ps.setInt(index, i);
            case Double  d -> ps.setDouble(index, d);
            case Boolean b -> ps.setBoolean(index, b);
            case null      -> ps.setObject(index, null);
            default        -> throw new IllegalArgumentException("Unsupported filter parameter type: " + p.getClass().getName());
        }
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

    public synchronized java.util.List<LecturaRow> getAllLectures() {
        java.util.List<LecturaRow> rows = new java.util.ArrayList<>();
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery(
                "SELECT isbn, data_inici, data_fi, pagines_llegides FROM lectura ORDER BY id")) {
            while (rs.next())
                rows.add(new LecturaRow(rs.getLong(1), LecturaRow.parseDateOrNull(rs.getString(2)), LecturaRow.parseDateOrNull(rs.getString(3)), rs.getInt(4)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les lectures: " + e.getMessage(), e);
        }
        return rows;
    }

    /**
     * Ordre correcte de DELETE per satisfer les foreign keys: taules filles
     * primer (les que tenen FK a altres), taules pare al final. Compartit
     * entre {@link #clearAllData()}, {@link BackupService#backupToSQL}
     * i qualsevol reset massiu — l'ordre s'ha de mantenir sincronitzat
     * o les FOREIGN KEY constraints fallaran en mode estricte.
     */
    public static final String[] CLEAR_ORDER = {
        "lectura", "prestec", "llibre_llista", "llista",
        "llibre_autor", "llibre_tag", "tag", "autor", "llibre"
    };

    public synchronized void clearAllData() throws SQLException {
        withTransaction(() -> {
            try (Statement s = con.createStatement()) {
                for (String t : CLEAR_ORDER) s.executeUpdate("DELETE FROM " + t);
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
            java.util.List<String> statements = splitAndStripComments(content);
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
     *
     * <p>Quote-aware behaviour matters because backup rows commonly contain
     * values like {@code 'pre--war ed.'}; a naive
     * {@code replaceAll("--[^\\n]*\\n", "")} on the whole file would corrupt
     * the value at the first {@code --} it finds. The same {@code inQuote}
     * flag that protects {@code ;} from terminating a statement inside a
     * literal also protects {@code --} from being interpreted as a comment
     * marker when it is just data. Two consecutive single quotes inside a
     * literal (the standard SQL escape for one quote) are kept verbatim and
     * do not close the literal.
     */
    private static java.util.List<String> splitAndStripComments(String sql) {
        java.util.List<String> stmts = new java.util.ArrayList<>();
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

    /**
     * Allow-list of SQL statement types acceptable in a backup file.
     *
     * <p>Backup files are user-controlled text and {@link #executeSQLFile}
     * runs each statement verbatim through {@link Statement#execute}, so a
     * block-list (e.g. "block DROP, ALTER, …") is not safe — any statement
     * type the author forgets to block goes through, including
     * {@code DELETE FROM llibre} or {@code UPDATE llibre SET autor='pwned'}.
     * An allow-list inverts the default: anything not explicitly listed is
     * rejected. The first non-space token must match one of
     * {@link #ALLOWED_LEAD}. Comparison is case-insensitive.
     *
     * <p>Combined with the quote-aware {@link #splitAndStripComments}, a
     * tampered file that smuggles {@code DELETE} inside a quoted literal is
     * still just data inside an allowed {@code INSERT}.
     *
     * <p>SECURITY: do not relax this list without re-reading the threat
     * model above.
     */
    private static boolean isAllowed(String sql) {
        String upper = sql.toUpperCase(java.util.Locale.ROOT);
        for (String kw : ALLOWED_LEAD) {
            if (upper.startsWith(kw)) return true;
        }
        return false;
    }

    private static final String[] ALLOWED_LEAD = {
        "INSERT ", "VALUES ", "SET ", "BEGIN", "COMMIT", "ROLLBACK"
    };

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

    /**
     * Sincronitza els autors d'un llibre: DELETE + INSERT IGNORE per lots.
     *
     * <p>Implementació optimitzada — 3 viatges round-trip totals (1 DELETE
     * + 1 batch INSERT IGNORE a {@code autor} + 1 batch INSERT IGNORE a
     * {@code llibre_autor}), independentment de quants autors tingui el
     * llibre. La versió original feia N+2 viatges; veure {@code todo.txt}
     * item [1] LlibreDao syncAutors().
     */
    private void syncAutors(long isbn, java.util.List<String> autors) throws SQLException {
        if (autors == null || autors.isEmpty()) {
            try (PreparedStatement del = con.prepareStatement("DELETE FROM llibre_autor WHERE isbn = ?")) {
                del.setLong(1, isbn);
                del.execute();
            }
            return;
        }
        // Batch the author sync: one DELETE + one batch INSERT IGNORE for authors + one batch INSERT IGNORE for links.
        // This reduces round trips from N+2 (N authors) to 3 (delete + author insert + link insert).
        try (PreparedStatement del = con.prepareStatement("DELETE FROM llibre_autor WHERE isbn = ?")) {
            del.setLong(1, isbn);
            del.execute();
        }
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
