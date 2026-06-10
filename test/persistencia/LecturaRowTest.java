package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class LecturaRowTest {

    @Test
    @DisplayName("record exposes all four fields")
    void recordFields() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 15);
        LecturaRow r = new LecturaRow(9780306406157L, start, end, 100);
        assertThat(r.isbn()).isEqualTo(9780306406157L);
        assertThat(r.dataInici()).isEqualTo(start);
        assertThat(r.dataFi()).isEqualTo(end);
        assertThat(r.paginesLlegides()).isEqualTo(100);
    }

    @Test
    @DisplayName("parseDateOrNull: valid ISO returns LocalDate")
    void parseValid() {
        LocalDate d = LecturaRow.parseDateOrNull("2024-03-15");
        assertThat(d).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("parseDateOrNull: null → null")
    void parseNull() {
        assertThat(LecturaRow.parseDateOrNull(null)).isNull();
    }

    @Test
    @DisplayName("parseDateOrNull: blank → null")
    void parseBlank() {
        assertThat(LecturaRow.parseDateOrNull("")).isNull();
        assertThat(LecturaRow.parseDateOrNull("   ")).isNull();
    }

    @Test
    @DisplayName("parseDateOrNull: invalid format → throws DateTimeParseException (caller catches)")
    void parseInvalid() {
        assertThatThrownBy(() -> LecturaRow.parseDateOrNull("not a date"))
            .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    @DisplayName("parseDateOrNull: trims surrounding whitespace")
    void parseTrims() {
        assertThat(LecturaRow.parseDateOrNull("  2024-03-15  "))
            .isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        LocalDate d = LocalDate.of(2024, 1, 1);
        var a = new LecturaRow(1L, d, d, 0);
        var b = new LecturaRow(1L, d, d, 0);
        assertThat(a).isEqualTo(b);
    }
}
