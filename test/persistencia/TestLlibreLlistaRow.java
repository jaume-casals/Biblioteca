package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import persistencia.row.LlibreLlistaRow;
import persistencia.row.RelationRow;
class TestLlibreLlistaRow {

    @Test
    @DisplayName("record exposes all four fields")
    void recordFields() {
        LlibreLlistaRow r = new LlibreLlistaRow(9780306406157L, 1, 9.5, true);
        assertThat(r.isbn()).isEqualTo(9780306406157L);
        assertThat(r.llistaId()).isEqualTo(1);
        assertThat(r.valoracio()).isEqualTo(9.5);
        assertThat(r.llegit()).isTrue();
    }

    @Test
    @DisplayName("implements RelationRow; isbn() is delegated")
    void implementsRelationRow() {
        RelationRow rr = new LlibreLlistaRow(42L, 1, 0.0, false);
        assertThat(rr.isbn()).isEqualTo(42L);
    }

    @Test
    @DisplayName("NaN valoracio round-trips through equals (IEEE bit pattern)")
    void nanValoracioEquals() {
        LlibreLlistaRow a = new LlibreLlistaRow(1L, 1, Double.NaN, false);
        LlibreLlistaRow b = new LlibreLlistaRow(1L, 1, Double.NaN, false);
        assertThat(a.valoracio()).isNaN();
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("infinity valoracio preserved")
    void infinityValoracio() {
        LlibreLlistaRow r = new LlibreLlistaRow(1L, 1, Double.POSITIVE_INFINITY, true);
        assertThat(r.valoracio()).isPositive();
    }
}
