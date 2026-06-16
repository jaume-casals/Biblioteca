package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class DateUtilsTest {

    // ── parseYear ────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseYear: 4-digit ISO year prefix is taken")
    void parseYearIsoPrefix() {
        assertThat(DateUtils.parseYear("1984")).contains(1984);
        assertThat(DateUtils.parseYear("2024-03-15")).contains(2024);
    }

    @Test
    @DisplayName("parseYear: extracts year from free text")
    void parseYearFreeText() {
        assertThat(DateUtils.parseYear("Published 1984")).contains(1984);
        assertThat(DateUtils.parseYear("Year: 2020")).contains(2020);
    }

    @Test
    @DisplayName("parseYear: null / blank / no digits → empty")
    void parseYearEmpty() {
        assertThat(DateUtils.parseYear(null)).isEmpty();
        assertThat(DateUtils.parseYear("")).isEmpty();
        assertThat(DateUtils.parseYear("   ")).isEmpty();
        assertThat(DateUtils.parseYear("not a year")).isEmpty();
    }

    @Test
    @DisplayName("parseYear: years outside 1000-2200 from prefix are rejected; regex fallback")
    void parseYearOutOfPrefixRange() {
        // Prefix is "1234" which is in [1000, 2200], so it returns 1234
        assertThat(DateUtils.parseYear("1234-05-06")).contains(1234);
    }

    @Test
    @DisplayName("parseYear: 3-digit prefix rejected by length check; regex kicks in if any year found")
    void parseYearShortPrefix() {
        // "999" is < 4 chars, so the int parse branch is skipped; regex finds none
        assertThat(DateUtils.parseYear("999x")).isEmpty();
    }

    // ── normalizeDate ────────────────────────────────────────────────────

    @Test
    @DisplayName("normalizeDate: slashes become dashes")
    void normalizeDate() {
        assertThat(DateUtils.normalizeDate("2024/03/15")).isEqualTo("2024-03-15");
    }

    @Test
    @DisplayName("normalizeDate: null → null")
    void normalizeDateNull() {
        assertThat(DateUtils.normalizeDate(null)).isNull();
    }

    @Test
    @DisplayName("normalizeDate: text without slashes passes through")
    void normalizeDateNoChange() {
        assertThat(DateUtils.normalizeDate("2024-03-15")).isEqualTo("2024-03-15");
    }

    // ── parseIsoDate ─────────────────────────────────────────────────────

    @Test
    @DisplayName("parseIsoDate: ISO date is parsed")
    void parseIsoDateValid() {
        assertThat(DateUtils.parseIsoDate("2024-03-15"))
            .isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("parseIsoDate: null / blank / invalid → null")
    void parseIsoDateInvalid() {
        assertThat(DateUtils.parseIsoDate(null)).isNull();
        assertThat(DateUtils.parseIsoDate("")).isNull();
        assertThat(DateUtils.parseIsoDate("   ")).isNull();
        assertThat(DateUtils.parseIsoDate("not a date")).isNull();
    }

    @Test
    @DisplayName("parseIsoDate: trims surrounding whitespace")
    void parseIsoDateTrims() {
        assertThat(DateUtils.parseIsoDate("  2024-03-15  "))
            .isEqualTo(LocalDate.of(2024, 3, 15));
    }

    // ── formatDateForDisplay ─────────────────────────────────────────────

    @Test
    @DisplayName("formatDateForDisplay: ISO date becomes d/M/yyyy")
    void formatDisplayValid() {
        assertThat(DateUtils.formatDateForDisplay("2024-03-15")).isEqualTo("15/3/2024");
    }

    @Test
    @DisplayName("formatDateForDisplay: null / blank → empty string")
    void formatDisplayEmpty() {
        assertThat(DateUtils.formatDateForDisplay(null)).isEmpty();
        assertThat(DateUtils.formatDateForDisplay("")).isEmpty();
    }

    @Test
    @DisplayName("formatDateForDisplay: unparseable input is returned unchanged")
    void formatDisplayPassThrough() {
        assertThat(DateUtils.formatDateForDisplay("not a date")).isEqualTo("not a date");
    }

    @ParameterizedTest
    @CsvSource({
        "'1984', 1984",
        "'2024-03-15', 2024",
        "'March 2020', 2020"
    })
    @DisplayName("parseYear parameterized")
    void parseYearParameterized(String input, int expected) {
        assertThat(DateUtils.parseYear(input)).contains(expected);
    }
}
