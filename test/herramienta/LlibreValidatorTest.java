package herramienta;

import domini.Llibre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class LlibreValidatorTest {

    // ── ISBN validation ──────────────────────────────────────────────────

    @Test
    @DisplayName("checkLlibre: ISBN-13 accepted")
    void isbn13() {
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, null, null, null);
        assertThat(l.getISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("checkLlibre: ISBN-10 accepted")
    void isbn10() {
        Llibre l = LlibreValidator.checkLlibre(8420413739L, "X", null, null, null, null, null, null, null);
        assertThat(l.getISBN()).isEqualTo(8420413739L);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 123456789L, 12345678901L, 12345678901234L})
    @DisplayName("checkLlibre: bad-length ISBNs rejected")
    void invalidIsbnLengths(long isbn) {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(isbn, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkLlibre: null ISBN rejected")
    void nullIsbn() {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(null, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── nom validation ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("checkLlibre: blank nom rejected")
    void blankNom(String nom) {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, nom, null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkLlibre: nom over 255 chars rejected")
    void nomTooLong() {
        String longNom = "a".repeat(256);
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, longNom, null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── autor / valoracio / preu / any validation ───────────────────────

    @Test
    @DisplayName("checkLlibre: autor over 255 chars rejected")
    void autorTooLong() {
        String longAutor = "a".repeat(256);
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", longAutor, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, -1.0, 10.1, 11.0})
    @DisplayName("checkLlibre: out-of-range valoracio rejected")
    void outOfRangeValoracio(double v) {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, v, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 5.0, 10.0})
    @DisplayName("checkLlibre: boundary valoracio values accepted")
    void boundaryValoracio(double v) {
        assertThatNoException().isThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, v, null, null, null));
    }

    @Test
    @DisplayName("checkLlibre: negative preu rejected")
    void negativePreu() {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, -0.01, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkLlibre: preu = 0 accepted")
    void zeroPreu() {
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, 0.0, null, null);
        assertThat(l.getPreu()).isZero();
    }

    @Test
    @DisplayName("checkLlibre: year before 1000 rejected")
    void yearTooOld() {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", null, 999, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkLlibre: year > current+5 rejected")
    void yearTooFuture() {
        int tooFuture = java.time.Year.now().getValue() + 10;
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", null, tooFuture, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkLlibre: year 1000 accepted (boundary)")
    void yearBoundary() {
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, 1000, null, null, null, null, null);
        assertThat(l.getAny()).isEqualTo(1000);
    }

    @Test
    @DisplayName("checkLlibre: year = 0 accepted (no year)")
    void yearZero() {
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, 0, null, null, null, null, null);
        assertThat(l.getAny()).isZero();
    }

    // ── defaults ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkLlibre: default values for null optional fields")
    void defaults() {
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "T", null, null, null, null, null, null, null);
        assertThat(l.getAutor()).isEqualTo("");
        assertThat(l.getAny()).isZero();
        assertThat(l.getValoracio()).isZero();
        assertThat(l.getPreu()).isZero();
        assertThat(l.getLlegit()).isFalse();
        assertThat(l.getDescripcio()).isEqualTo("");
        assertThat(l.getImatge()).isEqualTo("");
    }

    // ── checkLlibreFromString ───────────────────────────────────────────

    @Test
    @DisplayName("checkLlibreFromString: ISBN-13 with dashes accepted")
    void fromStringDashes() {
        Llibre l = LlibreValidator.checkLlibreFromString(
            "978-0-306-40615-7", "X", null, null, null, null, null, null, null);
        assertThat(l.getISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("checkLlibreFromString: ISBN-10 with X check digit → ISBN-13")
    void fromStringIsbn10X() {
        Llibre l = LlibreValidator.checkLlibreFromString(
            "019853110X", "X", null, null, null, null, null, null, null);
        assertThat(l.getISBN()).isEqualTo(9780198531104L);
    }

    @Test
    @DisplayName("checkLlibreFromString: null / blank ISBN rejected")
    void fromStringBlank() {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibreFromString(null, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibreFromString("  ", "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkLlibreFromString: invalid ISBN-13 check digit rejected")
    void fromStringInvalidCheckDigit() {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibreFromString(
                "9780000000000", "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkLlibreFromString: too-short ISBN rejected")
    void fromStringTooShort() {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibreFromString(
                "123", "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── validateInto ────────────────────────────────────────────────────

    @Test
    @DisplayName("validateInto: updates core fields but keeps extras")
    void validateIntoPreservesExtras() {
        Llibre target = new Llibre(9780306406157L, "Old", "A", 2000, "desc", 1.0, 2.0, false, "");
        target.setNotes("kept");
        target.setPagines(400);

        LlibreValidator.validateInto(target, 9780306406157L, "New", "Auth", 2020,
            "desc", 5.0, 10.0, true, "/x");

        assertThat(target.getNom()).isEqualTo("New");
        assertThat(target.getNotes()).isEqualTo("kept");
        assertThat(target.getPagines()).isEqualTo(400);
    }

    @Test
    @DisplayName("validateInto: invalid input throws and target is not modified (atomic check first)")
    void validateIntoRejects() {
        Llibre target = new Llibre(9780306406157L, "Old", "A", 2000, "desc", 1.0, 2.0, false, "");
        assertThatThrownBy(() -> LlibreValidator.validateInto(target, 9780306406157L, "", "A",
            2000, "desc", 1.0, 2.0, false, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── validateExtras ──────────────────────────────────────────────────

    @Test
    @DisplayName("validateExtras: editorial over 255 rejected")
    void validateExtrasEditorial() {
        assertThatThrownBy(() -> LlibreValidator.validateExtras("a".repeat(256), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateExtras: serie over 255 rejected")
    void validateExtrasSerie() {
        assertThatThrownBy(() -> LlibreValidator.validateExtras(null, "a".repeat(256)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateExtras: short strings accepted")
    void validateExtrasShortOk() {
        assertThatNoException().isThrownBy(() -> LlibreValidator.validateExtras("a", "b"));
        assertThatNoException().isThrownBy(() -> LlibreValidator.validateExtras(null, null));
    }

    // ── validateExtrasAll ───────────────────────────────────────────────

    @Test
    @DisplayName("validateExtrasAll: idioma > 100 rejected")
    void validateExtrasAllIdioma() {
        assertThatThrownBy(() -> LlibreValidator.validateExtrasAll(null, null, "a".repeat(101), null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateExtrasAll: format > 50 rejected")
    void validateExtrasAllFormat() {
        assertThatThrownBy(() -> LlibreValidator.validateExtrasAll(null, null, null, "a".repeat(51), null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateExtrasAll: paisOrigen > 100 rejected")
    void validateExtrasAllPais() {
        assertThatThrownBy(() -> LlibreValidator.validateExtrasAll(null, null, null, null, "a".repeat(101), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateExtrasAll: estat > 50 rejected")
    void validateExtrasAllEstat() {
        assertThatThrownBy(() -> LlibreValidator.validateExtrasAll(null, null, null, null, null, "a".repeat(51)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateExtrasAll: nulls and short strings accepted")
    void validateExtrasAllNullsOk() {
        assertThatNoException().isThrownBy(() -> LlibreValidator.validateExtrasAll(null, null, null, null, null, null));
    }

    @ParameterizedTest
    @CsvSource({
        "9780306406157, Dune, '', 1965, '', 9.0, 19.99",
        "8420413739, Hamlet, Shakespeare, 1603, '', 8.5, 10.0"
    })
    @DisplayName("checkLlibre: realistic book happy-path")
    void realisticBook(long isbn, String nom, String autor, int any, String desc, double val, double preu) {
        Llibre l = LlibreValidator.checkLlibre(isbn, nom, autor, any, desc, val, preu, true, "");
        assertThat(l.getISBN()).isEqualTo(isbn);
        assertThat(l.getNom()).isEqualTo(nom);
        assertThat(l.getAny()).isEqualTo(any);
    }
}
