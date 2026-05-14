package persistencia;

import domini.Llibre;
import java.sql.*;
import java.util.ArrayList;

public class LlibreDao {

    private final Connection con;

    static final String LLIBRE_SELECT =
        "SELECT ISBN, nom, autor, `any`, descripcio, valoracio, preu, llegit, imatge, " +
        "(imatge_blob IS NOT NULL) AS has_blob, notes, pagines, pagines_llegides, editorial, serie, " +
        "volum, data_compra, data_lectura, idioma, format, desitjat, pais_origen, estat, exemplars, llengua_original ";

    LlibreDao(Connection con) { this.con = con; }

    static Llibre buildLlibre(ResultSet rs) throws SQLException {
        Llibre l = new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
            rs.getInt(4), rs.getString(5), rs.getDouble(6),
            rs.getDouble(7), rs.getBoolean(8), rs.getString(9));
        l.setHasBlob(rs.getBoolean(10));
        l.setNotes(rs.getString(11));
        l.setPagines(rs.getInt(12));
        l.setPaginesLlegides(rs.getInt(13));
        l.setEditorial(rs.getString(14));
        l.setSerie(rs.getString(15));
        l.setVolum(rs.getInt(16));
        l.setDataCompra(rs.getString(17));
        l.setDataLectura(rs.getString(18));
        l.setIdioma(rs.getString(19));
        l.setFormat(rs.getString(20));
        l.setDesitjat(rs.getBoolean(21));
        l.setPaisOrigen(rs.getString(22));
        l.setEstat(rs.getString(23));
        l.setExemplars(rs.getInt(24) > 0 ? rs.getInt(24) : 1);
        l.setLlenguaOriginal(rs.getString(25));
        return l;
    }

    public synchronized ArrayList<Llibre> getAll() {
        ArrayList<Llibre> biblio = new ArrayList<>();
        try {
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(LLIBRE_SELECT + "FROM llibre")) {
                while (rs.next()) biblio.add(buildLlibre(rs));
            }
            java.util.Map<Long, Llibre> byISBN = new java.util.HashMap<>();
            for (Llibre l : biblio) byISBN.put(l.getISBN(), l);
            try (Statement aSt = con.createStatement();
                 ResultSet ars = aSt.executeQuery(
                    "SELECT la.isbn, a.nom FROM llibre_autor la JOIN autor a ON la.autor_id = a.id ORDER BY la.isbn, a.nom")) {
                while (ars.next()) {
                    Llibre l = byISBN.get(ars.getLong(1));
                    if (l != null) l.getAutors().add(ars.getString(2));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al agafar tots els llibres: " + e.getMessage());
        }
        return biblio;
    }

    public synchronized void insert(Llibre ll) throws SQLException {
        if (ll == null) return;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO llibre (`ISBN`,`nom`,`autor`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`imatge_blob`," +
                "`notes`,`pagines`,`pagines_llegides`,`editorial`,`serie`,`volum`,`data_compra`,`data_lectura`," +
                "`idioma`,`format`,`desitjat`,`pais_origen`,`estat`,`exemplars`,`llengua_original`) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, ll.getISBN());
            ps.setString(2, ll.getNom());
            ps.setString(3, ll.getAutor() != null ? ll.getAutor() : "");
            ps.setInt(4, ll.getAny() != null ? ll.getAny() : 0);
            ps.setString(5, ll.getDescripcio() != null ? ll.getDescripcio() : "");
            ps.setDouble(6, ll.getValoracio() != null ? ll.getValoracio() : 0.0);
            ps.setDouble(7, ll.getPreu() != null ? ll.getPreu() : 0.0);
            ps.setBoolean(8, Boolean.TRUE.equals(ll.getLlegit()));
            ps.setString(9, ll.getImatge() != null ? ll.getImatge() : "");
            ps.setBytes(10, ll.getImatgeBlob());
            ps.setString(11, ll.getNotes());
            ps.setInt(12, ll.getPagines());
            ps.setInt(13, ll.getPaginesLlegides());
            ps.setString(14, ll.getEditorial());
            ps.setString(15, ll.getSerie());
            ps.setInt(16, ll.getVolum());
            String dc = ll.getDataCompra(), dl = ll.getDataLectura();
            if (dc != null) { try { ps.setDate(17, java.sql.Date.valueOf(dc)); } catch (IllegalArgumentException e) { ps.setNull(17, java.sql.Types.DATE); } }
            else ps.setNull(17, java.sql.Types.DATE);
            if (dl != null) { try { ps.setDate(18, java.sql.Date.valueOf(dl)); } catch (IllegalArgumentException e) { ps.setNull(18, java.sql.Types.DATE); } }
            else ps.setNull(18, java.sql.Types.DATE);
            if (ll.getIdioma() != null) ps.setString(19, ll.getIdioma()); else ps.setNull(19, java.sql.Types.VARCHAR);
            if (ll.getFormat() != null) ps.setString(20, ll.getFormat()); else ps.setNull(20, java.sql.Types.VARCHAR);
            ps.setBoolean(21, ll.getDesitjat());
            if (ll.getPaisOrigen() != null) ps.setString(22, ll.getPaisOrigen()); else ps.setNull(22, java.sql.Types.VARCHAR);
            if (ll.getEstat() != null) ps.setString(23, ll.getEstat()); else ps.setNull(23, java.sql.Types.VARCHAR);
            ps.setInt(24, Math.max(1, ll.getExemplars()));
            if (ll.getLlenguaOriginal() != null) ps.setString(25, ll.getLlenguaOriginal()); else ps.setNull(25, java.sql.Types.VARCHAR);
            ps.execute();
        }
        if (!ll.getAutors().isEmpty()) syncAutors(ll.getISBN(), ll.getAutors());
    }

    public synchronized void update(Llibre ll) throws SQLException {
        if (ll == null) return;
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE llibre SET `nom`=?,`autor`=?,`any`=?,`descripcio`=?,`valoracio`=?,`preu`=?,`llegit`=?,`imatge`=?," +
                "`notes`=?,`pagines`=?,`pagines_llegides`=?,`editorial`=?,`serie`=?,`volum`=?," +
                "`data_compra`=?,`data_lectura`=?,`idioma`=?,`format`=?,`desitjat`=?,`pais_origen`=?," +
                "`estat`=?,`exemplars`=?,`llengua_original`=? WHERE `ISBN`=?")) {
            ps.setString(1, ll.getNom());
            ps.setString(2, ll.getAutor() != null ? ll.getAutor() : "");
            ps.setInt(3, ll.getAny() != null ? ll.getAny() : 0);
            ps.setString(4, ll.getDescripcio() != null ? ll.getDescripcio() : "");
            ps.setDouble(5, ll.getValoracio() != null ? ll.getValoracio() : 0.0);
            ps.setDouble(6, ll.getPreu() != null ? ll.getPreu() : 0.0);
            ps.setBoolean(7, Boolean.TRUE.equals(ll.getLlegit()));
            ps.setString(8, ll.getImatge() != null ? ll.getImatge() : "");
            ps.setString(9, ll.getNotes());
            ps.setInt(10, ll.getPagines());
            ps.setInt(11, ll.getPaginesLlegides());
            ps.setString(12, ll.getEditorial());
            ps.setString(13, ll.getSerie());
            ps.setInt(14, ll.getVolum());
            String dc = ll.getDataCompra(), dl = ll.getDataLectura();
            if (dc != null) { try { ps.setDate(15, java.sql.Date.valueOf(dc)); } catch (IllegalArgumentException e) { ps.setNull(15, java.sql.Types.DATE); } }
            else ps.setNull(15, java.sql.Types.DATE);
            if (dl != null) { try { ps.setDate(16, java.sql.Date.valueOf(dl)); } catch (IllegalArgumentException e) { ps.setNull(16, java.sql.Types.DATE); } }
            else ps.setNull(16, java.sql.Types.DATE);
            if (ll.getIdioma() != null) ps.setString(17, ll.getIdioma()); else ps.setNull(17, java.sql.Types.VARCHAR);
            if (ll.getFormat() != null) ps.setString(18, ll.getFormat()); else ps.setNull(18, java.sql.Types.VARCHAR);
            ps.setBoolean(19, ll.getDesitjat());
            if (ll.getPaisOrigen() != null) ps.setString(20, ll.getPaisOrigen()); else ps.setNull(20, java.sql.Types.VARCHAR);
            if (ll.getEstat() != null) ps.setString(21, ll.getEstat()); else ps.setNull(21, java.sql.Types.VARCHAR);
            ps.setInt(22, Math.max(1, ll.getExemplars()));
            if (ll.getLlenguaOriginal() != null) ps.setString(23, ll.getLlenguaOriginal()); else ps.setNull(23, java.sql.Types.VARCHAR);
            ps.setLong(24, ll.getISBN());
            ps.execute();
        }
        if (!ll.getAutors().isEmpty()) syncAutors(ll.getISBN(), ll.getAutors());
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
            System.err.println("Error carregant els llibres recents: " + e.getMessage());
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
            System.err.println("Error carregant la imatge del llibre: " + e.getMessage());
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

    public synchronized ArrayList<Llibre> search(domini.LlibreFilter f, int offset, int pageSize) {
        ArrayList<Llibre> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT l.ISBN, l.nom, l.autor, l.`any`, l.descripcio, l.valoracio, l.preu, l.llegit, l.imatge, " +
            "(l.imatge_blob IS NOT NULL) AS has_blob, l.notes, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
            "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original FROM llibre l");
        if (f.llistaId != null) sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ").append(f.llistaId);
        if (f.tagId    != null) sql.append(" JOIN llibre_tag lt ON l.ISBN = lt.isbn AND lt.tag_id = ").append(f.tagId);
        sql.append(" WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (f.nom          != null) { sql.append(" AND l.nom LIKE ?");        params.add("%" + f.nom + "%"); }
        if (f.autor        != null) { sql.append(" AND l.autor LIKE ?");      params.add("%" + f.autor + "%"); }
        if (f.isbn         != null) { sql.append(" AND l.ISBN = ?");          params.add(f.isbn); }
        if (f.anyMin       != null) { sql.append(" AND l.`any` >= ?");        params.add(f.anyMin); }
        if (f.anyMax       != null) { sql.append(" AND l.`any` <= ?");        params.add(f.anyMax); }
        if (f.valoracioMin != null) { sql.append(" AND l.valoracio >= ?");    params.add(f.valoracioMin); }
        if (f.valoracioMax != null) { sql.append(" AND l.valoracio <= ?");    params.add(f.valoracioMax); }
        if (f.preuMin      != null) { sql.append(" AND l.preu >= ?");         params.add(f.preuMin); }
        if (f.preuMax      != null) { sql.append(" AND l.preu <= ?");         params.add(f.preuMax); }
        if (f.llegit       != null) { sql.append(" AND l.llegit = ?");        params.add(f.llegit); }
        if (f.editorial    != null) { sql.append(" AND l.editorial LIKE ?");  params.add("%" + f.editorial + "%"); }
        if (f.serie        != null) { sql.append(" AND l.serie LIKE ?");      params.add("%" + f.serie + "%"); }
        if (f.format       != null) { sql.append(" AND l.format = ?");        params.add(f.format); }
        if (f.idioma       != null) { sql.append(" AND l.idioma LIKE ?");     params.add("%" + f.idioma + "%"); }
        sql.append(" ORDER BY l.ISBN");
        if (pageSize > 0) sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String s)   ps.setString(i + 1, s);
                else if (p instanceof Long v)    ps.setLong(i + 1, v);
                else if (p instanceof Integer v) ps.setInt(i + 1, v);
                else if (p instanceof Double v)  ps.setDouble(i + 1, v);
                else if (p instanceof Boolean v) ps.setBoolean(i + 1, v);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(buildLlibre(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error en searchLlibres: " + e.getMessage());
        }
        return result;
    }

    public synchronized int count() {
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM llibre")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error comptant llibres: " + e.getMessage());
        }
        return 0;
    }

    public synchronized void clearAllData() throws SQLException {
        try (Statement s = con.createStatement()) {
            s.executeUpdate("DELETE FROM prestec");
            s.executeUpdate("DELETE FROM llibre_llista");
            s.executeUpdate("DELETE FROM llista");
            s.executeUpdate("DELETE FROM llibre_autor");
            s.executeUpdate("DELETE FROM llibre_tag");
            s.executeUpdate("DELETE FROM tag");
            s.executeUpdate("DELETE FROM autor");
            s.executeUpdate("DELETE FROM llibre");
        }
    }

    public synchronized long getDbSizeBytes() {
        try {
            String url = con.getMetaData().getURL();
            if (url != null && url.startsWith("jdbc:h2:")) {
                String path = url.replaceFirst("jdbc:h2:", "").replaceAll(";.*", "");
                java.io.File f = new java.io.File(path + ".mv.db");
                return f.exists() ? f.length() : -1;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public synchronized void executeSQLFile(java.io.File file) throws Exception {
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8));
             Statement st = con.createStatement()) {
            StringBuilder stmt = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) continue;
                String upper = line.toUpperCase();
                if (upper.startsWith("USE ") || upper.startsWith("CREATE DATABASE")
                        || upper.startsWith("DROP DATABASE")) continue;
                stmt.append(line).append(" ");
                if (line.endsWith(";")) {
                    st.execute(stmt.toString().trim());
                    stmt = new StringBuilder();
                }
            }
        }
    }

    private void syncAutors(long isbn, java.util.List<String> autors) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM llibre_autor WHERE isbn = ?")) {
            del.setLong(1, isbn);
            del.execute();
        }
        for (String nom : autors) {
            if (nom == null || nom.isBlank()) continue;
            try (PreparedStatement ins = con.prepareStatement("INSERT IGNORE INTO autor (nom) VALUES (?)")) {
                ins.setString(1, nom);
                ins.execute();
            }
            try (PreparedStatement sel = con.prepareStatement("SELECT id FROM autor WHERE nom = ?")) {
                sel.setString(1, nom);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement link = con.prepareStatement(
                                "INSERT IGNORE INTO llibre_autor (isbn, autor_id) VALUES (?, ?)")) {
                            link.setLong(1, isbn);
                            link.setInt(2, rs.getInt(1));
                            link.execute();
                        }
                    }
                }
            }
        }
    }
}
