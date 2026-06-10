package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RelationRowTest {

    @Test
    @DisplayName("LlibreLlistaRow exposes isbn via RelationRow")
    void llibreLlistaRowIsbn() {
        RelationRow rr = new LlibreLlistaRow(100L, 1, 0.0, false);
        assertThat(rr.isbn()).isEqualTo(100L);
    }

    @Test
    @DisplayName("LlibreTagRow exposes isbn via RelationRow")
    void llibreTagRowIsbn() {
        RelationRow rr = new LlibreTagRow(200L, 2);
        assertThat(rr.isbn()).isEqualTo(200L);
    }

    @Test
    @DisplayName("LlibreAutorRow exposes isbn via RelationRow")
    void llibreAutorRowIsbn() {
        RelationRow rr = new LlibreAutorRow(300L, 3);
        assertThat(rr.isbn()).isEqualTo(300L);
    }
}
