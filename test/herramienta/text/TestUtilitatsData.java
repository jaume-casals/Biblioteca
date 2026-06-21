package herramienta.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class TestUtilitatsData {

    // ── analitzarYear ──────────────────────────────────────────────────

    @Test
    @DisplayName("analitzarYear: s'agafa el prefix d'any ISO de 4 dígits")
    void analitzarYearIsoPrefix() {
        assertThat(UtilitatsData.analitzarYear("1984")).contains(1984);
        assertThat(UtilitatsData.analitzarYear("2024-03-15")).contains(2024);
    }

    @Test
    @DisplayName("analitzarYear: extreu l'any de text lliure")
    void analitzarYearFreeText() {
        assertThat(UtilitatsData.analitzarYear("Publicat 1984")).contains(1984);
        assertThat(UtilitatsData.analitzarYear("Any: 2020")).contains(2020);
    }

    @Test
    @DisplayName("analitzarYear: null / buit / sense dígits → buit")
    void analitzarYearEmpty() {
        assertThat(UtilitatsData.analitzarYear(null)).isEmpty();
        assertThat(UtilitatsData.analitzarYear("")).isEmpty();
        assertThat(UtilitatsData.analitzarYear("   ")).isEmpty();
        assertThat(UtilitatsData.analitzarYear("no és un any")).isEmpty();
    }

    @Test
    @DisplayName("analitzarYear: els anys fora de 1000-2200 del prefix es rebutgen; fallback de regex")
    void analitzarYearOutOfPrefixRange() {
        // El prefix és "1234" que està a [1000, 2200], per tant retorna 1234
        assertThat(UtilitatsData.analitzarYear("1234-05-06")).contains(1234);
    }

    @Test
    @DisplayName("analitzarYear: prefix de 3 dígits rebutjat per la comprovació de longitud; la regex actua si troba algun any")
    void analitzarYearShortPrefix() {
        // "999" és < 4 chars, per la qual cosa la branca de parseig int se salta; la regex no en troba cap
        assertThat(UtilitatsData.analitzarYear("999x")).isEmpty();
    }

    // ── normalizeDate ────────────────────────────────────────────────────

    @Test
    @DisplayName("normalizeDate: les barres esdevenen guions")
    void normalizeDate() {
        assertThat(UtilitatsData.normalizeDate("2024/03/15")).isEqualTo("2024-03-15");
    }

    @Test
    @DisplayName("normalizeDate: null → null")
    void normalizeDateNull() {
        assertThat(UtilitatsData.normalizeDate(null)).isNull();
    }

    @Test
    @DisplayName("normalizeDate: text sense barres passa sense canvis")
    void normalizeDateNoChange() {
        assertThat(UtilitatsData.normalizeDate("2024-03-15")).isEqualTo("2024-03-15");
    }

    // ── analitzarIsoDate ───────────────────────────────────────────────

    @Test
    @DisplayName("analitzarIsoDate: una data ISO s'analitza")
    void analitzarIsoDateValid() {
        assertThat(UtilitatsData.analitzarIsoDate("2024-03-15"))
            .isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("analitzarIsoDate: null / buit / invàlid → null")
    void analitzarIsoDateInvalid() {
        assertThat(UtilitatsData.analitzarIsoDate(null)).isNull();
        assertThat(UtilitatsData.analitzarIsoDate("")).isNull();
        assertThat(UtilitatsData.analitzarIsoDate("   ")).isNull();
        assertThat(UtilitatsData.analitzarIsoDate("no és una data")).isNull();
    }

    @Test
    @DisplayName("analitzarIsoDate: retalla els espais en blanc dels extrems")
    void analitzarIsoDateTrims() {
        assertThat(UtilitatsData.analitzarIsoDate("  2024-03-15  "))
            .isEqualTo(LocalDate.of(2024, 3, 15));
    }

    // ── formatejarDateForDisplay ────────────────────────────────────────

    @Test
    @DisplayName("formatejarDateForDisplay: una data ISO esdevé d/M/yyyy")
    void formatejarDisplayValid() {
        assertThat(UtilitatsData.formatejarDateForDisplay("2024-03-15")).isEqualTo("15/3/2024");
    }

    @Test
    @DisplayName("formatejarDateForDisplay: null / buit → cadena buida")
    void formatejarDisplayEmpty() {
        assertThat(UtilitatsData.formatejarDateForDisplay(null)).isEmpty();
        assertThat(UtilitatsData.formatejarDateForDisplay("")).isEmpty();
    }

    @Test
    @DisplayName("formatejarDateForDisplay: una entrada no analitzable es retorna sense canvis")
    void formatejarDisplayPassThrough() {
        assertThat(UtilitatsData.formatejarDateForDisplay("no és una data")).isEqualTo("no és una data");
    }

    @ParameterizedTest
    @CsvSource({
        "'1984', 1984",
        "'2024-03-15', 2024",
        "'Març 2020', 2020"
    })
    @DisplayName("analitzarYear parametritzat")
    void analitzarYearParameterized(String input, int expected) {
        assertThat(UtilitatsData.analitzarYear(input)).contains(expected);
    }
}
