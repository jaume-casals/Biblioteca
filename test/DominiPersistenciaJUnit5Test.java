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
import domini.SortSpec;
import persistencia.ConnectionConfig;
import persistencia.ControladorPersistencia;
import persistencia.ServerConect;

/**
 * Regression tests for domini/persistencia fixes (ISBN long, migrations, tag cache, filters).
 */
class DominiPersistenciaJUnit5Test {

    private static final long ISBN_13 = 9780262533455L;

    @BeforeEach
    void resetSingletons() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    @Test
    void llibreLlistaContextPreservesIsbn13() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = new Llibre(ISBN_13, "Test", "Author", 2020, "", 0.0, 0.0, false, "");
        cd.addLlibre(l);
        var shelf = cd.addLlista("Shelf A");
        cd.addLlibreToLlista(ISBN_13, shelf.getId(), 4.5, true);

        java.util.List<LlibreLlistaContext> ctx = cd.getLlistesForLlibreContext(ISBN_13);
        assertEquals(1, ctx.size());
        assertEquals(ISBN_13, ctx.get(0).isbn());
    }

    @Test
    void tagCacheInvalidatedAfterBookDelete() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        ControladorDomini cd = ControladorDomini.getInstance();
        long isbn = 9780134685991L;
        cd.addLlibre(new Llibre(isbn, "Effective Java", "Bloch", 2018, "", 0.0, 0.0, false, ""));
        var tag = cd.addTag("java");
        cd.addLlibreToTag(isbn, tag.getId());
        assertEquals(1, cp.getAllLlibreTag().size());

        cd.deleteLlibre(isbn);
        assertEquals(0, cp.getAllLlibreTag().size());
    }

    @Test
    void searchIgnoresBlankTitleFilter() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.afegirLlibre(new Llibre(9780132350884L, "Clean Code", "Martin", 2008, "", 0.0, 0.0, false, ""));
        cp.afegirLlibre(new Llibre(9781491950358L, "Fluent Python", "Ramalho", 2015, "", 0.0, 0.0, false, ""));

        LlibreFilter blankTitle = LlibreFilter.empty().withNom("   ");
        assertEquals(2, cp.searchLlibres(blankTitle, 0, 0).size());
    }

    @Test
    void sortSpecDefaultUsesTableAlias() {
        assertTrue(new SortSpec("unknown-column", true).toSql().startsWith("l.`ISBN`"));
    }

    @Test
    void migration34DropsAutorOnFreshDb() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        assertFalse(autorColumnExists(cp), "autor column should be dropped after migration 34");
    }

    @Test
    void sanitizeH2ProfileRejectsPathSeparators() throws Exception {
        var method = ServerConect.class.getDeclaredMethod("sanitizeH2Profile", String.class);
        method.setAccessible(true);
        var wrapped = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(null, "../evil"));
        assertInstanceOf(IllegalArgumentException.class, wrapped.getCause());
        assertTrue(wrapped.getCause().getMessage().contains("Invalid H2 db profile"));
    }

    private static boolean autorColumnExists(ControladorPersistencia cp) throws Exception {
        var scField = ControladorPersistencia.class.getDeclaredField("sc");
        scField.setAccessible(true);
        ServerConect sc = (ServerConect) scField.get(cp);
        try (Statement st = sc.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                 "WHERE UPPER(TABLE_NAME) = 'LLIBRE' AND UPPER(COLUMN_NAME) = 'AUTOR'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
