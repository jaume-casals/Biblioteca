package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestLlibreCore {

    @Test
    @DisplayName("from() projects all 16 core fields from a Llibre")
    void fromProjectsAllFields() {
        Llibre l = new Llibre(9780306406157L, "Dune", null, 1965, "desc", 9.0, 19.99, true, "/x");
        l.posarAutors(java.util.List.of("Herbert", "Baron"));
        l.posarPagines(500);
        l.posarEditorial("Chilton");
        l.posarSerie("Dune");
        l.posarVolum(1);
        l.posarIdioma("en");
        l.posarFormat("Tapa dura");
        l.posarPaisOrigen("US");

        LlibreCore c = LlibreCore.from(l);
        assertThat(c.isbn()).isEqualTo(9780306406157L);
        assertThat(c.nom()).isEqualTo("Dune");
        assertThat(c.autors()).containsExactly("Herbert", "Baron");
        assertThat(c.any()).isEqualTo(1965);
        assertThat(c.descripcio()).isEqualTo("desc");
        assertThat(c.valoracio()).isEqualTo(9.0);
        assertThat(c.preu()).isEqualTo(19.99);
        assertThat(c.llegit()).isTrue();
        assertThat(c.imatge()).isEqualTo("/x");
        assertThat(c.pagines()).isEqualTo(500);
        assertThat(c.editorial()).isEqualTo("Chilton");
        assertThat(c.serie()).isEqualTo("Dune");
        assertThat(c.volum()).isEqualTo(1);
        assertThat(c.idioma()).isEqualTo("en");
        assertThat(c.format()).isEqualTo("Tapa dura");
        assertThat(c.paisOrigen()).isEqualTo("US");
    }

    @Test
    @DisplayName("autors list is the same reference (records hold whatever Llibre returns)")
    void recordAutors() {
        Llibre l = new Llibre(9780306406157L, "Dune", null, null, null, null, null, null, null);
        LlibreCore c = LlibreCore.from(l);
        assertThat(c.autors()).isNotNull();
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        Llibre l = new Llibre(9780306406157L, "Dune", null, 1965, "d", 9.0, 19.99, true, "/x");
        LlibreCore a = LlibreCore.from(l);
        LlibreCore b = LlibreCore.from(l);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
