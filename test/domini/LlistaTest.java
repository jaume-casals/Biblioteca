package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link Llista}.
 */
class LlistaTest {

    // ── Constructor / basic getters ─────────────────────────────────────

    @Test
    @DisplayName("constructor stores id and nom")
    void constructorStores() {
        Llista l = new Llista(7, "Favorits");
        assertThat(l.getId()).isEqualTo(7);
        assertThat(l.getNom()).isEqualTo("Favorits");
    }

    @Test
    @DisplayName("default ordre is 0; setter updates it")
    void ordreSetter() {
        Llista l = new Llista(1, "x");
        assertThat(l.getOrdre()).isZero();
        l.setOrdre(5);
        assertThat(l.getOrdre()).isEqualTo(5);
    }

    @Test
    @DisplayName("default color is null; setter accepts any string")
    void colorSetter() {
        Llista l = new Llista(1, "x");
        assertThat(l.getColor()).isNull();
        l.setColor("#aabbcc");
        assertThat(l.getColor()).isEqualTo("#aabbcc");
    }

    // ── setNom validation ───────────────────────────────────────────────

    @Test
    @DisplayName("setNom(null) throws BibliotecaException.Validation")
    void setNomNullThrows() {
        Llista l = new Llista(1, "ok");
        assertThatThrownBy(() -> l.setNom(null))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
    }

    @Test
    @DisplayName("setNom(blank) throws BibliotecaException.Validation")
    void setNomBlankThrows() {
        Llista l = new Llista(1, "ok");
        assertThatThrownBy(() -> l.setNom(""))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
        assertThatThrownBy(() -> l.setNom("   "))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
    }

    @Test
    @DisplayName("setNom accepts a non-blank value")
    void setNomValid() {
        Llista l = new Llista(1, "old");
        l.setNom("new");
        assertThat(l.getNom()).isEqualTo("new");
    }

    // ── isValidColor ─────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidColor accepts #abc (3-digit hex)")
    void validColor3Hex() {
        assertThat(Llista.isValidColor("#abc")).isTrue();
        assertThat(Llista.isValidColor("#FFF")).isTrue();
        assertThat(Llista.isValidColor("#000")).isTrue();
    }

    @Test
    @DisplayName("isValidColor accepts #aabbcc (6-digit hex)")
    void validColor6Hex() {
        assertThat(Llista.isValidColor("#aabbcc")).isTrue();
        assertThat(Llista.isValidColor("#AABBCC")).isTrue();
        assertThat(Llista.isValidColor("#000000")).isTrue();
        assertThat(Llista.isValidColor("#ffffff")).isTrue();
    }

    @Test
    @DisplayName("isValidColor accepts null (clears color)")
    void validColorNull() {
        assertThat(Llista.isValidColor(null)).isTrue();
    }

    @Test
    @DisplayName("isValidColor rejects unprefixed hex, wrong length, garbage")
    void validColorRejects() {
        assertThat(Llista.isValidColor("aabbcc")).isFalse();    // no #
        assertThat(Llista.isValidColor("#aabb")).isFalse();      // 4 digits
        assertThat(Llista.isValidColor("#aabbccd")).isFalse();   // 7 digits
        assertThat(Llista.isValidColor("red")).isFalse();        // color name
        assertThat(Llista.isValidColor("#zzzzzz")).isFalse();    // non-hex
        assertThat(Llista.isValidColor("")).isFalse();           // empty
        assertThat(Llista.isValidColor("#")).isFalse();          // just hash
    }

    // ── per-book shelf values ───────────────────────────────────────────

    @Test
    @DisplayName("getValoracioLlibre / getLlegitLlibre default null; setters update")
    void perBookDefaults() {
        Llista l = new Llista(1, "x");
        assertThat(l.getValoracioLlibre()).isNull();
        assertThat(l.getLlegitLlibre()).isNull();
        l.setValoracioLlibre(7.5);
        l.setLlegitLlibre(true);
        assertThat(l.getValoracioLlibre()).isEqualTo(7.5);
        assertThat(l.getLlegitLlibre()).isTrue();
    }

    // ── toMap ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toMap exposes id/nom/ordre/color")
    void toMapExposesAll() {
        Llista l = new Llista(3, "Favorits");
        l.setOrdre(2);
        l.setColor("#abc");
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
