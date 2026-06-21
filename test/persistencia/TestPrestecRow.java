package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

import persistencia.row.PrestecRow;
class TestPrestecRow {

    @Test
    @DisplayName("record exposes all four fields")
    void recordFields() {
        LocalDate d = LocalDate.of(2024, 1, 1);
        PrestecRow r = new PrestecRow(9780306406157L, "Paul", d, false);
        assertThat(r.isbn()).isEqualTo(9780306406157L);
        assertThat(r.nomPersona()).isEqualTo("Paul");
        assertThat(r.dataPrestec()).isEqualTo(d);
        assertThat(r.retornat()).isFalse();
    }

    @Test
    @DisplayName("fromStrings: null/blank date → null dataPrestec")
    void fromStringsNullDate() {
        PrestecRow r = PrestecRow.fromStrings(1L, "P", null, false);
        assertThat(r.dataPrestec()).isNull();
    }

    @Test
    @DisplayName("fromStrings: empty date → null dataPrestec")
    void fromStringsEmptyDate() {
        PrestecRow r = PrestecRow.fromStrings(1L, "P", "", false);
        assertThat(r.dataPrestec()).isNull();
    }

    @Test
    @DisplayName("fromStrings: invalid date → null dataPrestec (caller-friendly)")
    void fromStringsInvalidDate() {
        PrestecRow r = PrestecRow.fromStrings(1L, "P", "not a date", false);
        assertThat(r.dataPrestec()).isNull();
    }

    @Test
    @DisplayName("fromStrings: valid ISO date → LocalDate")
    void fromStringsValidDate() {
        PrestecRow r = PrestecRow.fromStrings(1L, "P", "2024-06-15", true);
        assertThat(r.dataPrestec()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(r.nomPersona()).isEqualTo("P");
        assertThat(r.retornat()).isTrue();
    }

    @Test
    @DisplayName("fromStrings: trims surrounding whitespace on the date")
    void fromStringsTrims() {
        PrestecRow r = PrestecRow.fromStrings(1L, "P", "  2024-06-15  ", false);
        assertThat(r.dataPrestec()).isEqualTo(LocalDate.of(2024, 6, 15));
    }

    @Test
    @DisplayName("overdueDays: null dataPrestec → -1 (signals 'unknown')")
    void overdueDaysNullDate() {
        PrestecRow r = new PrestecRow(1L, "x", null, false);
        assertThat(r.overdueDays(LocalDate.now(), 30)).isEqualTo(-1L);
    }

    @Test
    @DisplayName("overdueDays: past date with grace elapsed → days past grace")
    void overdueDaysPast() {
        PrestecRow r = new PrestecRow(1L, "x", LocalDate.of(2024, 1, 1), false);
        assertThat(r.overdueDays(LocalDate.of(2024, 2, 15), 30)).isEqualTo(15L);
    }

    @Test
    @DisplayName("overdueDays: within grace → 0")
    void overdueDaysWithinGrace() {
        PrestecRow r = new PrestecRow(1L, "x", LocalDate.of(2024, 1, 1), false);
        assertThat(r.overdueDays(LocalDate.of(2024, 1, 15), 30)).isZero();
    }

    @Test
    @DisplayName("overdueDays: future date → 0 (clamped)")
    void overdueDaysFuture() {
        PrestecRow r = new PrestecRow(1L, "x", LocalDate.of(2024, 1, 1), false);
        assertThat(r.overdueDays(LocalDate.of(2023, 12, 1), 30)).isZero();
    }

    @Test
    @DisplayName("toDisplayMap: contains all four keys, date formatted as dd/MM/yyyy")
    void toDisplayMap() {
        PrestecRow r = new PrestecRow(123L, "Alice", LocalDate.of(2024, 3, 15), false);
        var m = r.toDisplayMap();
        assertThat(m).containsEntry("isbn", 123L);
        assertThat(m).containsEntry("persona", "Alice");
        assertThat(m).containsEntry("dataPrestec", "15/03/2024");
        assertThat(m).containsEntry("retornat", false);
    }

    @Test
    @DisplayName("toDisplayMap: null date → null value")
    void toDisplayMapNullDate() {
        PrestecRow r = new PrestecRow(123L, "Bob", null, true);
        var m = r.toDisplayMap();
        assertThat(m.get("dataPrestec")).isNull();
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        LocalDate d = LocalDate.of(2024, 1, 1);
        var a = new PrestecRow(1L, "P", d, false);
        var b = new PrestecRow(1L, "P", d, false);
        assertThat(a).isEqualTo(b);
    }
}
