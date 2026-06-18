import static org.junit.jupiter.api.Assertions.*;

import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import domini.LlibreLlistaContext;
import domini.EspecificacioOrdenacio;
import persistencia.ConnectionConfig;
import persistencia.ControladorPersistencia;
import persistencia.ConnexioServidor;

/**
 * Regression tests for domini/persistencia fixes (ISBN long, migrations, tag cache, filters).
 */
class DominiPersistenciaJUnit5Test {

    private static final long ISBN_13 = 9780262533455L;

    @BeforeEach
    void reinicialitzarSingletons() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @Test
    void llibreLlistaContextPreservesIsbn13() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = new Llibre(ISBN_13, "Test", "Author", 2020, "", 0.0, 0.0, false, "");
        cd.afegirLlibre(l);
        var shelf = cd.afegirLlista("Shelf A");
        cd.afegirLlibreToLlista(ISBN_13, shelf.obtenirId(), 4.5, true);

        java.util.List<LlibreLlistaContext> ctx = cd.obtenirLlistesForLlibreContext(ISBN_13);
        assertEquals(1, ctx.size());
        assertEquals(ISBN_13, ctx.get(0).isbn());
    }

    @Test
    void tagCacheInvalidatedAfterBookDelete() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        ControladorDomini cd = ControladorDomini.getInstance();
        long isbn = 9780134685991L;
        cd.afegirLlibre(new Llibre(isbn, "Effective Java", "Bloch", 2018, "", 0.0, 0.0, false, ""));
        var tag = cd.afegirTag("java");
        cd.afegirLlibreToTag(isbn, tag.obtenirId());
        assertEquals(1, cp.obtenirAllLlibreTag().size());

        cd.eliminarLlibre(isbn);
        assertEquals(0, cp.obtenirAllLlibreTag().size());
    }

    @Test
    void cercarIgnoresBlankTitleFilter() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.afegirLlibre(new Llibre(9780132350884L, "Clean Code", "Martin", 2008, "", 0.0, 0.0, false, ""));
        cp.afegirLlibre(new Llibre(9781491950358L, "Fluent Python", "Ramalho", 2015, "", 0.0, 0.0, false, ""));

        LlibreFilter blankTitle = LlibreFilter.empty().withNom("   ");
        assertEquals(2, cp.cercarLlibres(blankTitle, 0, 0).size());
    }

    @Test
    void ordenarSpecDefaultUsesTableAlias() {
        assertTrue(new EspecificacioOrdenacio("unknown-column", true).toSql().startsWith("l.`ISBN`"));
    }

    @Test
    void migration34DropsAutorOnFreshDb() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        assertFalse(autorColumnExists(cp), "autor column should be dropped after migration 34");
    }

    @Test
    void cercarShelfAndTagIsIntersection() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        ControladorDomini cd = ControladorDomini.getInstance();
        long isbn1 = 9780262533455L;
        long isbn2 = 9780132350884L;
        cd.afegirLlibre(new Llibre(isbn1, "Book One", "Author A", 2020, "", 0.0, 0.0, false, ""));
        cd.afegirLlibre(new Llibre(isbn2, "Book Two", "Author B", 2021, "", 0.0, 0.0, false, ""));

        var shelf = cd.afegirLlista("Favorites");
        int shelfId = shelf.obtenirId();
        cd.afegirLlibreToLlista(isbn1, shelfId, 0.0, false);
        cd.afegirLlibreToLlista(isbn2, shelfId, 0.0, false);

        var tag = cd.afegirTag("science");
        int tagId = tag.obtenirId();
        cd.afegirLlibreToTag(isbn1, tagId);

        LlibreFilter onlyShelf = new LlibreFilter().withLlistaId(shelfId);
        assertEquals(2, cp.cercarLlibres(onlyShelf, 0, 0).size(), "shelf alone should match both books");

        LlibreFilter onlyTag = new LlibreFilter().withTagId(tagId);
        assertEquals(1, cp.cercarLlibres(onlyTag, 0, 0).size(), "tag alone should match book 1");

        LlibreFilter intersection = new LlibreFilter().withLlistaId(shelfId).withTagId(tagId);
        var result = cp.cercarLlibres(intersection, 0, 0);
        assertEquals(1, result.size(), "shelf AND tag should intersect to a single book");
        assertEquals(isbn1, result.get(0).obtenirISBN());
    }

    @Test
    void sanitizeH2ProfileRejectsPathSeparators() throws Exception {
        var method = ConnexioServidor.class.getDeclaredMethod("sanitizeH2Profile", String.class);
        method.setAccessible(true);
        var wrapped = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(null, "../evil"));
        assertInstanceOf(IllegalArgumentException.class, wrapped.getCause());
        assertTrue(wrapped.getCause().getMessage().contains("Invalid H2 db profile"));
    }

    private static boolean autorColumnExists(ControladorPersistencia cp) throws Exception {
        var scField = ControladorPersistencia.class.getDeclaredField("sc");
        scField.setAccessible(true);
        ConnexioServidor sc = (ConnexioServidor) scField.get(cp);
        try (Statement st = sc.obtenirConnexio().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                 "WHERE UPPER(TABLE_NAME) = 'LLIBRE' AND UPPER(COLUMN_NAME) = 'AUTOR'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
