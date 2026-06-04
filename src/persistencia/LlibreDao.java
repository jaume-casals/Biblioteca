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
        Llibre l = new Llibre(rs.getLong("ISBN"), rs.getString("nom"), rs.getString("autor"),
            rs.getObject("any", Integer.class), null, rs.getObject("valoracio", Double.class),
            rs.getObject("preu", Double.class), rs.getBoolean("llegit"), rs.getString("imatge"));
        l.setHasBlob(rs.getBoolean("has_blob"));
        fillLlibreTail(l, rs, false);
        l.setHeavyFieldsLoaded(false);
        return l;
    }

    static Llibre buildLlibre(ResultSet rs) throws SQLException {
        Llibre l = new Llibre(rs.getLong("ISBN"), rs.getString("nom"), rs.getString("autor"),
            rs.getObject("any", Integer.class), rs.getString("descripcio"), rs.getObject("valoracio", Double.class),
            rs.getObject("preu", Double.class), rs.getBoolean("llegit"), rs.getString("imatge"));
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
                ps.setLong(1, ll.getISBN());
                ps.setString(2, ll.getNom());
                ps.setInt(3, ll.getAny() != null ? ll.getAny() : 0);
                ps.setString(4, ll.getDescripcio() != null ? ll.getDescripcio() : "");
                ps.setDouble(5, ll.getValoracio() != null ? ll.getValoracio() : 0.0);
                ps.setDouble(6, ll.getPreu() != null ? ll.getPreu() : 0.0);
                ps.setBoolean(7, Boolean.TRUE.equals(ll.getLlegit()));
                ps.setString(8, ll.getImatge() != null ? ll.getImatge() : "");
                ps.setBytes(9, ll.getImatgeBlob());
                ps.setString(10, ll.getNotes());
                ps.setInt(11, ll.getPagines());
                ps.setInt(12, ll.getPaginesLlegides());
                ps.setString(13, ll.getEditorial());
                ps.setString(14, ll.getSerie());
                ps.setInt(15, ll.getVolum());
                String dc = ll.getDataCompra(), dl = ll.getDataLectura();
                if (dc != null) { try { ps.setDate(16, java.sql.Date.valueOf(dc)); } catch (IllegalArgumentException e) { ps.setNull(16, java.sql.Types.DATE); } }
                else ps.setNull(16, java.sql.Types.DATE);
                if (dl != null) { try { ps.setDate(17, java.sql.Date.valueOf(dl)); } catch (IllegalArgumentException e) { ps.setNull(17, java.sql.Types.DATE); } }
                else ps.setNull(17, java.sql.Types.DATE);
                if (ll.getIdioma() != null) ps.setString(18, ll.getIdioma()); else ps.setNull(18, java.sql.Types.VARCHAR);
                if (ll.getFormat() != null) ps.setString(19, ll.getFormat()); else ps.setNull(19, java.sql.Types.VARCHAR);
                ps.setBoolean(20, ll.getDesitjat());
                if (ll.getPaisOrigen() != null) ps.setString(21, ll.getPaisOrigen()); else ps.setNull(21, java.sql.Types.VARCHAR);
                if (ll.getEstat() != null) ps.setString(22, ll.getEstat()); else ps.setNull(22, java.sql.Types.VARCHAR);
                ps.setInt(23, Math.max(1, ll.getExemplars()));
                if (ll.getLlenguaOriginal() != null) ps.setString(24, ll.getLlenguaOriginal()); else ps.setNull(24, java.sql.Types.VARCHAR);
                if (ll.getNomCa() != null && !ll.getNomCa().isBlank()) ps.setString(25, ll.getNomCa()); else ps.setNull(25, java.sql.Types.VARCHAR);
                if (ll.getNomEs() != null && !ll.getNomEs().isBlank()) ps.setString(26, ll.getNomEs()); else ps.setNull(26, java.sql.Types.VARCHAR);
                if (ll.getNomEn() != null && !ll.getNomEn().isBlank()) ps.setString(27, ll.getNomEn()); else ps.setNull(27, java.sql.Types.VARCHAR);
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
                ps.setString(1, ll.getNom());
                ps.setInt(2, ll.getAny() != null ? ll.getAny() : 0);
                ps.setString(3, ll.getDescripcio() != null ? ll.getDescripcio() : "");
                ps.setDouble(4, ll.getValoracio() != null ? ll.getValoracio() : 0.0);
                ps.setDouble(5, ll.getPreu() != null ? ll.getPreu() : 0.0);
                ps.setBoolean(6, Boolean.TRUE.equals(ll.getLlegit()));
                ps.setString(7, ll.getImatge() != null ? ll.getImatge() : "");
                ps.setString(8, ll.getNotes());
                ps.setInt(9, ll.getPagines());
                ps.setInt(10, ll.getPaginesLlegides());
                ps.setString(11, ll.getEditorial());
                ps.setString(12, ll.getSerie());
                ps.setInt(13, ll.getVolum());
                String dc = ll.getDataCompra(), dl = ll.getDataLectura();
                if (dc != null) { try { ps.setDate(14, java.sql.Date.valueOf(dc)); } catch (IllegalArgumentException e) { ps.setNull(14, java.sql.Types.DATE); } }
                else ps.setNull(14, java.sql.Types.DATE);
                if (dl != null) { try { ps.setDate(15, java.sql.Date.valueOf(dl)); } catch (IllegalArgumentException e) { ps.setNull(15, java.sql.Types.DATE); } }
                else ps.setNull(15, java.sql.Types.DATE);
                if (ll.getIdioma() != null) ps.setString(16, ll.getIdioma()); else ps.setNull(16, java.sql.Types.VARCHAR);
                if (ll.getFormat() != null) ps.setString(17, ll.getFormat()); else ps.setNull(17, java.sql.Types.VARCHAR);
                ps.setBoolean(18, ll.getDesitjat());
                if (ll.getPaisOrigen() != null) ps.setString(19, ll.getPaisOrigen()); else ps.setNull(19, java.sql.Types.VARCHAR);
                if (ll.getEstat() != null) ps.setString(20, ll.getEstat()); else ps.setNull(20, java.sql.Types.VARCHAR);
                ps.setInt(21, Math.max(1, ll.getExemplars()));
                if (ll.getLlenguaOriginal() != null) ps.setString(22, ll.getLlenguaOriginal()); else ps.setNull(22, java.sql.Types.VARCHAR);
                if (ll.getNomCa() != null && !ll.getNomCa().isBlank()) ps.setString(23, ll.getNomCa()); else ps.setNull(23, java.sql.Types.VARCHAR);
                if (ll.getNomEs() != null && !ll.getNomEs().isBlank()) ps.setString(24, ll.getNomEs()); else ps.setNull(24, java.sql.Types.VARCHAR);
                if (ll.getNomEn() != null && !ll.getNomEn().isBlank()) ps.setString(25, ll.getNomEn()); else ps.setNull(25, java.sql.Types.VARCHAR);
                ps.setLong(26, ll.getISBN());
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
                Object p = params.get(i);
                if (p instanceof String s)   ps.setString(i + 1, s);
                else if (p instanceof Long v)    ps.setLong(i + 1, v);
                else if (p instanceof Integer v) ps.setInt(i + 1, v);
                else if (p instanceof Double v)  ps.setDouble(i + 1, v);
                else if (p instanceof Boolean v) ps.setBoolean(i + 1, v);
                else throw new IllegalArgumentException("Unhandled parameter type: " + (p != null ? p.getClass().getName() : "null"));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(buildLlibre(rs));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error en searchLlibres: " + e.getMessage(), e);
        }
        return result;
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
            content = content.replaceAll("--[^\\n]*\\n", "");
            java.util.List<String> statements = splitStatements(content);
            try (Statement st = con.createStatement()) {
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (trimmed.isEmpty()) continue;
                    if (isForbidden(trimmed)) continue;
                    st.execute(trimmed);
                }
            }
        });
    }

    private static java.util.List<String> splitStatements(String sql) {
        java.util.List<String> stmts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !inString) {
                inString = true;
                current.append(c);
            } else if (c == '\'' && inString) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append("''");
                    i++;
                } else {
                    inString = false;
                    current.append(c);
                }
            } else if (c == ';' && !inString) {
                stmts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) stmts.add(remaining);
        return stmts;
    }

    /**
     * Block statements that would mutate the schema or change the active DB.
     * Backup files are user-controlled text — a tampered file could otherwise
     * drop tables or switch databases. The check is the first non-space token
     * followed by a space; case-insensitive.
     */
    private static boolean isForbidden(String sql) {
        String upper = sql.toUpperCase(java.util.Locale.ROOT);
        for (String kw : FORBIDDEN_LEAD) {
            if (upper.startsWith(kw)) return true;
        }
        return false;
    }

    private static final String[] FORBIDDEN_LEAD = {
        "USE ", "CREATE DATABASE", "DROP DATABASE", "DROP TABLE", "DROP SCHEMA",
        "ALTER ", "TRUNCATE ", "RENAME ", "GRANT ", "REVOKE ", "SHUTDOWN"
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
