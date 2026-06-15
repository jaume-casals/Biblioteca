package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class Isbn13NormalizerTest {

    @Test
    @DisplayName("toIsbn13: ISBN-10 with X check digit → ISBN-13")
    void isbn10WithX() {
        // "019853110X" → 9780198531104
        assertThat(Isbn13Normalizer.toIsbn13("019853110X")).isEqualTo("9780198531104");
    }

    @Test
    @DisplayName("toIsbn13: ISBN-10 with digit check → ISBN-13")
    void isbn10WithDigit() {
        // 0306406152 → 9780306406157
        String out = Isbn13Normalizer.toIsbn13("0306406152");
        assertThat(out).startsWith("978");
        assertThat(out).hasSize(13);
    }

    @Test
    @DisplayName("toIsbn13: ISBN-13 passes through unchanged")
    void isbn13PassesThrough() {
        assertThat(Isbn13Normalizer.toIsbn13("9780306406157")).isEqualTo("9780306406157");
    }

    @Test
    @DisplayName("toIsbn13: ISBN-10 with uppercase X is converted to ISBN-13")
    void isbn10UppercaseX() {
        assertThat(Isbn13Normalizer.toIsbn13("019853110X")).isEqualTo("9780198531104");
    }

    @Test
    @DisplayName("toIsbn13: ISBN-10 with lowercase x is stripped (regex is case-sensitive on 'X')")
    void isbn10LowercaseX() {
        // The pattern [^0-9X] in the source keeps only uppercase X; lowercase x is removed.
        // So '019853110x' becomes '019853110' (9 digits) which is not a
        // valid 10- or 13-digit ISBN, so the normalizer returns null
        // (caller must reject the input) — per the tot.txt LOW finding.
        assertThat(Isbn13Normalizer.toIsbn13("019853110x")).isNull();
    }

    @Test
    @DisplayName("toIsbn13: ISBN-10 with dashes / spaces is stripped")
    void isbn10WithDashes() {
        String out = Isbn13Normalizer.toIsbn13("0-306-40615-2");
        assertThat(out).startsWith("978");
        assertThat(out).hasSize(13);
    }

    @Test
    @DisplayName("toIsbn13: null → null")
    void nullInput() {
        assertThat(Isbn13Normalizer.toIsbn13(null)).isNull();
    }

    @Test
    @DisplayName("toIsbn13: empty / blank → null")
    void emptyInput() {
        assertThat(Isbn13Normalizer.toIsbn13("")).isNull();
        assertThat(Isbn13Normalizer.toIsbn13("   ")).isNull();
    }

    @Test
    @DisplayName("toIsbn13: non-recognisable digit count returns null (caller must reject)")
    void unknownLength() {
        // 5 digits is neither a valid ISBN-10 nor ISBN-13; the
        // normalizer returns null so the caller surfaces a clear
        // validation error (per the tot.txt LOW finding).
        assertThat(Isbn13Normalizer.toIsbn13("12345")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "'019853110X', '9780198531104'",
        "'9780306406157', '9780306406157'"
    })
    @DisplayName("toIsbn13 parameterized canonical cases")
    void parameterized(String input, String expected) {
        assertThat(Isbn13Normalizer.toIsbn13(input)).isEqualTo(expected);
    }
}
