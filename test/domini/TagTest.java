package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TagTest {

    @Test
    @DisplayName("constructor stores id and nom")
    void constructorStores() {
        Tag t = new Tag(3, "Sci-Fi");
        assertThat(t.getId()).isEqualTo(3);
        assertThat(t.getNom()).isEqualTo("Sci-Fi");
    }

    @Test
    @DisplayName("setNom(null) throws BibliotecaException.Validation")
    void setNomNullThrows() {
        Tag t = new Tag(1, "ok");
        assertThatThrownBy(() -> t.setNom(null))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
    }

    @Test
    @DisplayName("setNom(blank) throws BibliotecaException.Validation")
    void setNomBlankThrows() {
        Tag t = new Tag(1, "ok");
        assertThatThrownBy(() -> t.setNom(""))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
        assertThatThrownBy(() -> t.setNom("  "))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
    }

    @Test
    @DisplayName("setNom accepts valid string")
    void setNomValid() {
        Tag t = new Tag(1, "old");
        t.setNom("new");
        assertThat(t.getNom()).isEqualTo("new");
    }

    @Test
    @DisplayName("toMap exposes id and nom (no color)")
    void toMapExposes() {
        Tag t = new Tag(5, "Fantasy");
        var m = t.toMap();
        assertThat(m).containsEntry("id", 5);
        assertThat(m).containsEntry("nom", "Fantasy");
        assertThat(m).hasSize(2);
    }

    @Test
    @DisplayName("toString returns nom (for combo display)")
    void toStringIsNom() {
        Tag t = new Tag(1, "TagX");
        assertThat(t.toString()).isEqualTo("TagX");
    }

    @Test
    @DisplayName("equals is id-based")
    void equalsById() {
        Tag a = new Tag(7, "old");
        Tag b = new Tag(7, "new");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals: different id, same nom → not equal")
    void notEqualsDifferentId() {
        Tag a = new Tag(1, "x");
        Tag b = new Tag(2, "x");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals self / null / wrong-type")
    void equalsEdgeCases() {
        Tag t = new Tag(1, "x");
        assertThat(t).isEqualTo(t);
        assertThat(t).isNotEqualTo(null);
        assertThat(t).isNotEqualTo("Tag");
    }
}
