package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link Llista}.
 */
class TestLlista {

    // ── Constructor / basic getters ─────────────────────────────────────

    @Test
    @DisplayName("constructor stores id and nom")
    void constructorStores() {
        Llista l = new Llista(7, "Favorits");
        assertThat(l.obtenirId()).isEqualTo(7);
        assertThat(l.obtenirNom()).isEqualTo("Favorits");
    }

    @Test
    @DisplayName("default ordre is 0; setter updates it")
    void ordreSetter() {
        Llista l = new Llista(1, "x");
        assertThat(l.obtenirOrdre()).isZero();
        l.posarOrdre(5);
        assertThat(l.obtenirOrdre()).isEqualTo(5);
    }

    @Test
    @DisplayName("default color is null; setter accepts any string")
    void colorSetter() {
        Llista l = new Llista(1, "x");
        assertThat(l.obtenirColor()).isNull();
        l.posarColor("#aabbcc");
        assertThat(l.obtenirColor()).isEqualTo("#aabbcc");
    }

    // ── setNom validation ───────────────────────────────────────────────

    @Test
    @DisplayName("setNom(null) throws BibliotecaException.Validation")
    void posarNomNullThrows() {
        Llista l = new Llista(1, "ok");
        assertThatThrownBy(() -> l.posarNom(null))
            .isInstanceOf(domini.BibliotecaException.Validacio.class);
    }

    @Test
    @DisplayName("setNom(blank) throws BibliotecaException.Validation")
    void posarNomBlankThrows() {
        Llista l = new Llista(1, "ok");
        assertThatThrownBy(() -> l.posarNom(""))
            .isInstanceOf(domini.BibliotecaException.Validacio.class);
        assertThatThrownBy(() -> l.posarNom("   "))
            .isInstanceOf(domini.BibliotecaException.Validacio.class);
    }

    @Test
    @DisplayName("setNom accepts a non-blank value")
    void posarNomValid() {
        Llista l = new Llista(1, "old");
        l.posarNom("new");
        assertThat(l.obtenirNom()).isEqualTo("new");
    }

    // ── isValidColor ─────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidColor accepts #abc (3-digit hex)")
    void validColor3Hex() {
        assertThat(Llista.esValidColor("#abc")).isTrue();
        assertThat(Llista.esValidColor("#FFF")).isTrue();
        assertThat(Llista.esValidColor("#000")).isTrue();
    }

    @Test
    @DisplayName("isValidColor accepts #aabbcc (6-digit hex)")
    void validColor6Hex() {
        assertThat(Llista.esValidColor("#aabbcc")).isTrue();
        assertThat(Llista.esValidColor("#AABBCC")).isTrue();
        assertThat(Llista.esValidColor("#000000")).isTrue();
        assertThat(Llista.esValidColor("#ffffff")).isTrue();
    }

    @Test
    @DisplayName("isValidColor accepts null (clears color)")
    void validColorNull() {
        assertThat(Llista.esValidColor(null)).isTrue();
    }

    @Test
    @DisplayName("isValidColor rejects unprefixed hex, wrong length, garbage")
    void validColorRejects() {
        assertThat(Llista.esValidColor("aabbcc")).isFalse();    // no #
        assertThat(Llista.esValidColor("#aabb")).isFalse();      // 4 digits
        assertThat(Llista.esValidColor("#aabbccd")).isFalse();   // 7 digits
        assertThat(Llista.esValidColor("red")).isFalse();        // color name
        assertThat(Llista.esValidColor("#zzzzzz")).isFalse();    // non-hex
        assertThat(Llista.esValidColor("")).isFalse();           // empty
        assertThat(Llista.esValidColor("#")).isFalse();          // just hash
    }

    // ── per-book shelf values ───────────────────────────────────────────

    @Test
    @DisplayName("getValoracioLlibre / getLlegitLlibre default null; setters update")
    void perBookDefaults() {
        Llista l = new Llista(1, "x");
        assertThat(l.obtenirValoracioLlibre()).isNull();
        assertThat(l.obtenirLlegitLlibre()).isNull();
        l.posarValoracioLlibre(7.5);
        l.posarLlegitLlibre(true);
        assertThat(l.obtenirValoracioLlibre()).isEqualTo(7.5);
        assertThat(l.obtenirLlegitLlibre()).isTrue();
    }

    // ── toMap ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toMap exposes id/nom/ordre/color")
    void toMapExposesAll() {
        Llista l = new Llista(3, "Favorits");
        l.posarOrdre(2);
        l.posarColor("#abc");
        var m = l.toMap();
        assertThat(m).containsEntry("id", 3);
        assertThat(m).containsEntry("nom", "Favorits");
        assertThat(m).containsEntry("ordre", 2);
        assertThat(m).containsEntry("color", "#abc");
        assertThat(m).hasSize(4);
    }

    @Test
    @DisplayName("toMap with no color set has null color value")
    void toMapNullColor() {
        Llista l = new Llista(1, "x");
        var m = l.toMap();
        assertThat(m).containsEntry("color", null);
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString returns nom (for JComboBox display)")
    void toStringIsNom() {
        Llista l = new Llista(1, "Favorits");
        assertThat(l.toString()).isEqualTo("Favorits");
    }

    // ── equals / hashCode (id-based) ────────────────────────────────────

    @Test
    @DisplayName("equals is id-based: same id → equal")
    void equalsById() {
        Llista a = new Llista(7, "old");
        Llista b = new Llista(7, "new");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals is id-based: different id → not equal")
    void notEqualsDifferentId() {
        Llista a = new Llista(1, "x");
        Llista b = new Llista(2, "x");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals self / null / wrong-type")
    void equalsEdgeCases() {
        Llista a = new Llista(1, "x");
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("Llista");
    }
}
