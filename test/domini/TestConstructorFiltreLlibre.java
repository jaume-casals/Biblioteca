package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestConstructorFiltreLlibre {

    @Test
    @DisplayName("of() returns a fresh builder")
    void ofReturnsBuilder() {
        assertThat(ConstructorFiltreLlibre.of()).isNotNull();
    }

    @Test
    @DisplayName("every setter returns this for chaining")
    void allSettersChainable() {
        ConstructorFiltreLlibre b = ConstructorFiltreLlibre.of();
        assertThat(b.isbn(1L)).isSameAs(b);
        assertThat(b.autor("a")).isSameAs(b);
        assertThat(b.nom("n")).isSameAs(b);
        assertThat(b.anyMin(1900)).isSameAs(b);
        assertThat(b.anyMax(2025)).isSameAs(b);
        assertThat(b.valoracioMin(0.0)).isSameAs(b);
        assertThat(b.valoracioMax(10.0)).isSameAs(b);
        assertThat(b.preuMin(0.0)).isSameAs(b);
        assertThat(b.preuMax(100.0)).isSameAs(b);
        assertThat(b.llegit(true)).isSameAs(b);
        assertThat(b.tagId(1)).isSameAs(b);
        assertThat(b.llistaId(1)).isSameAs(b);
        assertThat(b.editorial("e")).isSameAs(b);
        assertThat(b.serie("s")).isSameAs(b);
        assertThat(b.format("f")).isSameAs(b);
        assertThat(b.idioma("i")).isSameAs(b);
    }

    @Test
    @DisplayName("sort(column, asc) wraps a SortSpec")
    void ordenarWraps() {
        LlibreFilter f = ConstructorFiltreLlibre.of().sort("nom", false).build();
        assertThat(f.obtenirSort().column()).isEqualTo("nom");
        assertThat(f.obtenirSort().ascending()).isFalse();
    }

    @Test
    @DisplayName("build() returns a LlibreFilter with the set fields")
    void buildPopulatesAllFields() {
        LlibreFilter f = ConstructorFiltreLlibre.of()
            .isbn(123L).autor("A").nom("N").anyMin(1900).anyMax(2025)
            .valoracioMin(0.0).valoracioMax(10.0).preuMin(0.0).preuMax(100.0)
            .llegit(true).tagId(1).llistaId(2)
            .editorial("E").serie("S").format("F").idioma("I")
            .sort("any", true)
            .build();

        assertThat(f.obtenirIsbn()).isEqualTo(123L);
        assertThat(f.obtenirAutor()).isEqualTo("A");
        assertThat(f.obtenirNom()).isEqualTo("N");
        assertThat(f.obtenirAnyMin()).isEqualTo(1900);
        assertThat(f.obtenirAnyMax()).isEqualTo(2025);
        assertThat(f.obtenirValoracioMin()).isEqualTo(0.0);
        assertThat(f.obtenirValoracioMax()).isEqualTo(10.0);
        assertThat(f.obtenirPreuMin()).isEqualTo(0.0);
        assertThat(f.obtenirPreuMax()).isEqualTo(100.0);
        assertThat(f.obtenirLlegit()).isTrue();
        assertThat(f.obtenirTagId()).isEqualTo(1);
        assertThat(f.obtenirLlistaId()).isEqualTo(2);
        assertThat(f.obtenirEditorial()).isEqualTo("E");
        assertThat(f.obtenirSerie()).isEqualTo("S");
        assertThat(f.obtenirFormat()).isEqualTo("F");
        assertThat(f.obtenirIdioma()).isEqualTo("I");
        assertThat(f.obtenirSort().column()).isEqualTo("any");
        assertThat(f.obtenirSort().ascending()).isTrue();
    }
}
