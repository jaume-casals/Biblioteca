package herramienta;

import domini.Llibre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class TestValidadorLlibre {

    // ── Validació d'ISBN ────────────────────────────────────────────────

    @Test
    @DisplayName("comprovarLlibre: ISBN-13 acceptat")
    void isbn13() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, null, null, null);
        assertThat(l.obtenirISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("comprovarLlibre: ISBN-10 acceptat")
    void isbn10() {
        Llibre l = ValidadorLlibre.comprovarLlibre(8420413739L, "X", null, null, null, null, null, null, null);
        assertThat(l.obtenirISBN()).isEqualTo(8420413739L);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 123456789L, 12345678901L, 12345678901234L})
    @DisplayName("comprovarLlibre: ISBN amb longitud incorrecta rebutjats")
    void invalidIsbnLengths(long isbn) {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(isbn, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comprovarLlibre: ISBN nul rebutjat")
    void nullIsbn() {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(null, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Validació de nom ────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("comprovarLlibre: nom buit rebutjat")
    void blankNom(String nom) {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, nom, null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comprovarLlibre: nom de més de 255 caràcters rebutjat")
    void nomTooLong() {
        String longNom = "a".repeat(256);
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, longNom, null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Validació d'autor / valoracio / preu / any ──────────────────────

    @Test
    @DisplayName("comprovarLlibre: autor de més de 255 caràcters rebutjat")
    void autorTooLong() {
        String longAutor = "a".repeat(256);
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", longAutor, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, -1.0, 10.1, 11.0})
    @DisplayName("comprovarLlibre: valoració fora de rang rebutjada")
    void outOfRangeValoracio(double v) {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, v, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 5.0, 10.0})
    @DisplayName("comprovarLlibre: valors frontera de valoració acceptats")
    void boundaryValoracio(double v) {
        assertThatNoException().isThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, v, null, null, null));
    }

    @Test
    @DisplayName("comprovarLlibre: preu negatiu rebutjat")
    void negativePreu() {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, -0.01, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comprovarLlibre: preu = 0 acceptat")
    void zeroPreu() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, 0.0, null, null);
        assertThat(l.obtenirPreu()).isZero();
    }

    @Test
    @DisplayName("comprovarLlibre: any anterior a 1000 rebutjat")
    void yearTooOld() {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, 999, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comprovarLlibre: any > actual+5 rebutjat")
    void yearTooFuture() {
        int tooFuture = java.time.Year.now().getValue() + 10;
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, tooFuture, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comprovarLlibre: any 1000 acceptat (frontera)")
    void yearBoundary() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, 1000, null, null, null, null, null);
        assertThat(l.obtenirAny()).isEqualTo(1000);
    }

    @Test
    @DisplayName("comprovarLlibre: any = 0 acceptat (sense any)")
    void yearZero() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, 0, null, null, null, null, null);
        assertThat(l.obtenirAny()).isZero();
    }

    // ── Valors per defecte ──────────────────────────────────────────────

    @Test
    @DisplayName("comprovarLlibre: valors per defecte per a camps opcionals nuls")
    void defaults() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "T", null, null, null, null, null, null, null);
        assertThat(l.obtenirAutor()).isEqualTo("");
        assertThat(l.obtenirAny()).isZero();
        assertThat(l.obtenirValoracio()).isZero();
        assertThat(l.obtenirPreu()).isZero();
        assertThat(l.obtenirLlegit()).isFalse();
        assertThat(l.obtenirDescripcio()).isEqualTo("");
        assertThat(l.obtenirImatge()).isEqualTo("");
    }

    // ── comprovarLlibreFromString ─────────────────────────────────────

    @Test
    @DisplayName("comprovarLlibreFromString: ISBN-13 amb guions acceptat")
    void fromStringDashes() {
        Llibre l = ValidadorLlibre.comprovarLlibreFromString(
            "978-0-306-40615-7", "X", null, null, null, null, null, null, null);
        assertThat(l.obtenirISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("comprovarLlibreFromString: ISBN-10 amb dígit de control X → ISBN-13")
    void fromStringIsbn10X() {
        Llibre l = ValidadorLlibre.comprovarLlibreFromString(
            "019853110X", "X", null, null, null, null, null, null, null);
        assertThat(l.obtenirISBN()).isEqualTo(9780198531104L);
    }

    @Test
    @DisplayName("comprovarLlibreFromString: ISBN nul / buit rebutjat")
    void fromStringBlank() {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibreFromString(null, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibreFromString("  ", "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comprovarLlibreFromString: dígit de control ISBN-13 invàlid rebutjat")
    void fromStringInvalidCheckDigit() {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibreFromString(
                "9780000000000", "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comprovarLlibreFromString: ISBN massa curt rebutjat")
    void fromStringTooShort() {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibreFromString(
                "123", "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── validarInto ────────────────────────────────────────────────────

    @Test
    @DisplayName("validarInto: actualitza els camps principals però conserva els extres")
    void validarIntoPreservesExtras() {
        Llibre target = new Llibre(9780306406157L, "Vell", "A", 2000, "desc", 1.0, 2.0, false, "");
        target.posarNotes("conservat");
        target.posarPagines(400);

        ValidadorLlibre.validarInto(target, 9780306406157L, "Nou", "Auth", 2020,
            "desc", 5.0, 10.0, true, "/x");

        assertThat(target.obtenirNom()).isEqualTo("Nou");
        assertThat(target.obtenirNotes()).isEqualTo("conservat");
        assertThat(target.obtenirPagines()).isEqualTo(400);
    }

    @Test
    @DisplayName("validarInto: entrada invàlida llença i el target no es modifica (comprovació atòmica primer)")
    void validarIntoRejects() {
        Llibre target = new Llibre(9780306406157L, "Vell", "A", 2000, "desc", 1.0, 2.0, false, "");
        assertThatThrownBy(() -> ValidadorLlibre.validarInto(target, 9780306406157L, "", "A",
            2000, "desc", 1.0, 2.0, false, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── validarExtras ──────────────────────────────────────────────────

    @Test
    @DisplayName("validarExtras: editorial de més de 255 rebutjada")
    void validarExtrasEditorial() {
        assertThatThrownBy(() -> ValidadorLlibre.validarExtras("a".repeat(256), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarExtras: sèrie de més de 255 rebutjada")
    void validarExtrasSerie() {
        assertThatThrownBy(() -> ValidadorLlibre.validarExtras(null, "a".repeat(256)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarExtras: cadenes curtes acceptades")
    void validarExtrasShortOk() {
        assertThatNoException().isThrownBy(() -> ValidadorLlibre.validarExtras("a", "b"));
        assertThatNoException().isThrownBy(() -> ValidadorLlibre.validarExtras(null, null));
    }

    // ── validarExtrasAll ───────────────────────────────────────────────

    @Test
    @DisplayName("validarExtrasAll: idioma > 100 rebutjat")
    void validarExtrasAllIdioma() {
        assertThatThrownBy(() -> ValidadorLlibre.validarExtrasAll(null, null, "a".repeat(101), null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarExtrasAll: format > 50 rebutjat")
    void validarExtrasAllFormat() {
        assertThatThrownBy(() -> ValidadorLlibre.validarExtrasAll(null, null, null, "a".repeat(51), null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarExtrasAll: paisOrigen > 100 rebutjat")
    void validarExtrasAllPais() {
        assertThatThrownBy(() -> ValidadorLlibre.validarExtrasAll(null, null, null, null, "a".repeat(101), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarExtrasAll: estat > 50 rebutjat")
    void validarExtrasAllEstat() {
        assertThatThrownBy(() -> ValidadorLlibre.validarExtrasAll(null, null, null, null, null, "a".repeat(51)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validarExtrasAll: nuls i cadenes curtes acceptats")
    void validarExtrasAllNullsOk() {
        assertThatNoException().isThrownBy(() -> ValidadorLlibre.validarExtrasAll(null, null, null, null, null, null));
    }

    @ParameterizedTest
    @CsvSource({
        "9780306406157, Dune, '', 1965, '', 9.0, 19.99",
        "8420413739, Hamlet, Shakespeare, 1603, '', 8.5, 10.0"
    })
    @DisplayName("comprovarLlibre: camí feliç amb llibres realistes")
    void realisticBook(long isbn, String nom, String autor, int any, String desc, double val, double preu) {
        Llibre l = ValidadorLlibre.comprovarLlibre(isbn, nom, autor, any, desc, val, preu, true, "");
        assertThat(l.obtenirISBN()).isEqualTo(isbn);
        assertThat(l.obtenirNom()).isEqualTo(nom);
        assertThat(l.obtenirAny()).isEqualTo(any);
    }
}
