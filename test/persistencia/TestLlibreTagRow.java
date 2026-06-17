package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestLlibreTagRow {

    @Test
    @DisplayName("record exposes isbn and tagId")
    void recordFields() {
        LlibreTagRow r = new LlibreTagRow(9780306406157L, 3);
        assertThat(r.isbn()).isEqualTo(9780306406157L);
        assertThat(r.tagId()).isEqualTo(3);
    }

    @Test
    @DisplayName("implements RelationRow; isbn() is delegated")
    void implementsRelationRow() {
        RelationRow rr = new LlibreTagRow(42L, 7);
        assertThat(rr.isbn()).isEqualTo(42L);
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        assertThat(new LlibreTagRow(1L, 1)).isEqualTo(new LlibreTagRow(1L, 1));
        assertThat(new LlibreTagRow(1L, 1)).isNotEqualTo(new LlibreTagRow(1L, 2));
    }
}
