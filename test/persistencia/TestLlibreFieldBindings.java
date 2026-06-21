package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.sql.Date;

import domini.Llibre;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import persistencia.internal.LlibreFieldBindings;
/**
 * Per-class unit tests for {@link LlibreFieldBindings}.
 * Verifies forInsert / forUpdate produce the expected value arrays and
 * that {@code bind()} dispatches by Java type correctly.
 */
class TestLlibreFieldBindings {

    private Llibre fullBook() {
        Llibre l = new Llibre(9780306406157L, "Dune", "Herbert", 1965, "desc", 9.0, 19.99, true, "/x");
        l.posarNotes("note");
        l.posarPagines(500);
        l.posarPaginesLlegides(100);
        l.posarEditorial("Chilton");
        l.posarSerie("Dune");
        l.posarVolum(1);
        l.posarDataCompra("2020-01-05");
        l.posarDataLectura("2021-02-10");
        l.posarIdioma("en");
        l.posarFormat("Tapa dura");
        l.posarDesitjat(true);
        l.posarPaisOrigen("US");
        l.posarEstat("ok");
        l.posarExemplars(2);
        l.posarLlenguaOriginal("en");
        l.posarNomCa("Duna");
        l.posarNomEs("Dune");
        l.posarNomEn("Dune");
        return l;
    }

    @Test
    @DisplayName("forInsert produces a 27-element array (includes imatge_blob)")
    void forInsertHas27Elements() {
        Object[] values = LlibreFieldBindings.forInsert(fullBook());
        assertThat(values).hasSize(27);
    }

    @Test
    @DisplayName("forInsert: ISBN is at position 0, blob is at position 8")
    void forInsertPositions() {
        Object[] v = LlibreFieldBindings.forInsert(fullBook());
        assertThat(v[0]).isEqualTo(9780306406157L);
        assertThat(v[8]).isNull(); // no blob set → null
    }

    @Test
    @DisplayName("forInsert: notes is stored (NOT null)")
    void forInsertNotes() {
        Object[] v = LlibreFieldBindings.forInsert(fullBook());
        assertThat(v[9]).isEqualTo("note");
    }

    @Test
    @DisplayName("forInsert: data_compra is converted to java.sql.Date")
    void forInsertDataCompra() {
        Object[] v = LlibreFieldBindings.forInsert(fullBook());
        assertThat(v[15]).isInstanceOf(java.sql.Date.class);
        assertThat((java.sql.Date) v[15]).isEqualTo(Date.valueOf(LocalDate.of(2020, 1, 5)));
    }

    @Test
    @DisplayName("forInsert: null idioma → Null marker of VARCHAR (translated by bind)")
    void forInsertNullIdioma() {
        Llibre l = fullBook();
        l.posarIdioma(null);
        Object[] v = LlibreFieldBindings.forInsert(l);
        assertThat(v[17]).isNull(); // blank string → null
    }

    @Test
    @DisplayName("forInsert: blank string fields become empty string, not null")
    void forInsertBlankStrings() {
        Llibre l = new Llibre(9780306406157L, "Dune", "", 0, "", 0.0, 0.0, false, "");
        l.posarIdioma(null);
        l.posarFormat(null);
        Object[] v = LlibreFieldBindings.forInsert(l);
        assertThat(v[1]).isEqualTo("Dune");
        assertThat(v[3]).isEqualTo("");
        assertThat(v[7]).isEqualTo(""); // imatge
    }

    @Test
    @DisplayName("forInsert: invalid date string → Null marker (not exception)")
    void forInsertInvalidDate() {
        Llibre l = fullBook();
        l.posarDataCompra("not-a-date");
        Object[] v = LlibreFieldBindings.forInsert(l);
        assertThat(v[15]).isInstanceOf(LlibreFieldBindings.Nul.class);
    }

    @Test
    @DisplayName("forInsert: exemplars clamped to >= 1")
    void forInsertExemplarsClamped() {
        Llibre l = fullBook();
        l.posarExemplars(0);
        assertThat(LlibreFieldBindings.forInsert(l)[22]).isEqualTo(1);
    }

    @Test
    @DisplayName("forUpdate produces a 25-element array (no imatge_blob, no ISBN)")
    void forUpdateHas25Elements() {
        Object[] v = LlibreFieldBindings.forUpdate(fullBook());
        assertThat(v).hasSize(25);
    }

    @Test
    @DisplayName("forUpdate: nom is at position 0, not ISBN")
    void forUpdateFirstIsNom() {
        Object[] v = LlibreFieldBindings.forUpdate(fullBook());
        assertThat(v[0]).isEqualTo("Dune");
    }

    // ── bind() dispatch ─────────────────────────────────────────────────

    @Test
    @DisplayName("bind: null → setNull(VARCHAR)")
    void vincularNull() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlibreFieldBindings.bind(ps, 1, null);
        verify(ps).setNull(1, Types.VARCHAR);
    }

    @Test
    @DisplayName("bind: Null marker → setNull(marker.sqlType)")
    void vincularNullMarker() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlibreFieldBindings.bind(ps, 1, new LlibreFieldBindings.Nul(Types.DATE));
        verify(ps).setNull(1, Types.DATE);
    }

    @Test
    @DisplayName("bind: Integer → setInt")
    void vincularInteger() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlibreFieldBindings.bind(ps, 1, Integer.valueOf(42));
        verify(ps).setInt(1, 42);
    }

    @Test
    @DisplayName("bind: Long → setLong")
    void vincularLong() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlibreFieldBindings.bind(ps, 1, Long.valueOf(9780306406157L));
        verify(ps).setLong(1, 9780306406157L);
    }

    @Test
    @DisplayName("bind: Double → setDouble")
    void vincularDouble() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlibreFieldBindings.bind(ps, 1, Double.valueOf(3.14));
        verify(ps).setDouble(1, 3.14);
    }

    @Test
    @DisplayName("bind: Boolean → setBoolean")
    void vincularBoolean() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlibreFieldBindings.bind(ps, 1, Boolean.TRUE);
        verify(ps).setBoolean(1, true);
    }

    @Test
    @DisplayName("bind: java.sql.Date → setDate")
    void vincularSqlDate() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        Date d = Date.valueOf(LocalDate.of(2024, 1, 1));
        LlibreFieldBindings.bind(ps, 1, d);
        verify(ps).setDate(1, d);
    }

    @Test
    @DisplayName("bind: byte[] → setBytes")
    void vincularBytes() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        byte[] data = {1, 2, 3};
        LlibreFieldBindings.bind(ps, 1, data);
        verify(ps).setBytes(1, data);
    }

    @Test
    @DisplayName("bind: unknown type → setString via String.valueOf")
    void vincularStringFallback() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlibreFieldBindings.bind(ps, 1, "hello");
        verify(ps).setString(1, "hello");
    }
}
