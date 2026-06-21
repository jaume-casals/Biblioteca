package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import persistencia.row.AutorRow;
class AutorRowTest {

    @Test
    @DisplayName("record exposes id and nom")
    void recordFields() {
        AutorRow r = new AutorRow(3, "Tolkien");
        assertThat(r.id()).isEqualTo(3);
        assertThat(r.nom()).isEqualTo("Tolkien");
    }

    @Test
    @DisplayName("fromArray builds row from [int, String]")
    void fromArray() {
        Object[] a = {7, "Asimov"};
        AutorRow r = AutorRow.fromArray(a);
        assertThat(r.id()).isEqualTo(7);
        assertThat(r.nom()).isEqualTo("Asimov");
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        assertThat(new AutorRow(1, "X")).isEqualTo(new AutorRow(1, "X"));
        assertThat(new AutorRow(1, "X")).isNotEqualTo(new AutorRow(2, "X"));
    }
}
