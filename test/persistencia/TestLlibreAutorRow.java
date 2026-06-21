package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import persistencia.row.LlibreAutorRow;
import persistencia.row.RelationRow;
class TestLlibreAutorRow {

    @Test
    @DisplayName("record exposes isbn and autorId")
    void recordFields() {
        LlibreAutorRow r = new LlibreAutorRow(9780306406157L, 3);
        assertThat(r.isbn()).isEqualTo(9780306406157L);
        assertThat(r.autorId()).isEqualTo(3);
    }

    @Test
    @DisplayName("implements RelationRow; isbn() is delegated")
    void implementsRelationRow() {
        RelationRow rr = new LlibreAutorRow(42L, 1);
        assertThat(rr.isbn()).isEqualTo(42L);
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        assertThat(new LlibreAutorRow(1L, 1)).isEqualTo(new LlibreAutorRow(1L, 1));
        assertThat(new LlibreAutorRow(1L, 1)).isNotEqualTo(new LlibreAutorRow(1L, 2));
    }
}
