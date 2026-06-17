package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestFiltreLlibre {

    @Test
    @DisplayName("empty() returns a filter with all-null criteria and default sort")
    void emptyHasAllNulls() {
        LlibreFilter f = LlibreFilter.empty();
        assertThat(f.obtenirAutor()).isNull();
        assertThat(f.obtenirNom()).isNull();
        assertThat(f.obtenirIsbn()).isNull();
        assertThat(f.obtenirAnyMin()).isNull();
        assertThat(f.obtenirAnyMax()).isNull();
        assertThat(f.obtenirValoracioMin()).isNull();
        assertThat(f.obtenirValoracioMax()).isNull();
        assertThat(f.obtenirPreuMin()).isNull();
        assertThat(f.obtenirPreuMax()).isNull();
        assertThat(f.obtenirLlegit()).isNull();
        assertThat(f.obtenirTagId()).isNull();
        assertThat(f.obtenirLlistaId()).isNull();
        assertThat(f.obtenirEditorial()).isNull();
        assertThat(f.obtenirSerie()).isNull();
        assertThat(f.getFormat()).isNull();
        assertThat(f.obtenirIdioma()).isNull();
        EspecificacioOrdenacio s = f.obtenirSort();
        assertThat(s.column()).isEqualTo(EspecificacioOrdenacio.COL_ISBN);
        assertThat(s.ascending()).isTrue();
    }

    @Test
    @DisplayName("hasAnyFilter is false on an empty filter")
    void emptyHasNoFilters() {
        assertThat(LlibreFilter.empty().teAnyFilter()).isFalse();
    }

    @Test
    @DisplayName("hasAnyFilter is false even when only sort is set")
    void ordenarDoesNotCountAsFilter() {
        LlibreFilter f = LlibreFilter.empty().withSort(new EspecificacioOrdenacio("nom", true));
        assertThat(f.teAnyFilter()).isFalse();
    }

    @Test
    @DisplayName("withAutor / withNom / withIsbn set values and count as filters")
    void withStringFields() {
        LlibreFilter f = LlibreFilter.empty();
        f.withAutor("X");
        assertThat(f.obtenirAutor()).isEqualTo("X");
        assertThat(f.teAnyFilter()).isTrue();

        LlibreFilter f2 = LlibreFilter.empty();
        f2.withNom("N");
        assertThat(f2.obtenirNom()).isEqualTo("N");
        assertThat(f2.teAnyFilter()).isTrue();

        LlibreFilter f3 = LlibreFilter.empty();
        f3.withIsbn(123L);
        assertThat(f3.obtenirIsbn()).isEqualTo(123L);
        assertThat(f3.teAnyFilter()).isTrue();
    }

    @Test
    @DisplayName("withX mutators return this (chainable)")
    void withXReturnsThis() {
        LlibreFilter f = LlibreFilter.empty();
        assertThat(f.withAutor("a")).isSameAs(f);
        assertThat(f.withNom("n")).isSameAs(f);
        assertThat(f.withIsbn(1L)).isSameAs(f);
        assertThat(f.withAnyMin(1900)).isSameAs(f);
        assertThat(f.withAnyMax(2025)).isSameAs(f);
        assertThat(f.withValoracioMin(0.0)).isSameAs(f);
        assertThat(f.withValoracioMax(10.0)).isSameAs(f);
        assertThat(f.withPreuMin(0.0)).isSameAs(f);
        assertThat(f.withPreuMax(100.0)).isSameAs(f);
        assertThat(f.withLlegit(true)).isSameAs(f);
        assertThat(f.withTagId(1)).isSameAs(f);
        assertThat(f.withLlistaId(1)).isSameAs(f);
        assertThat(f.withEditorial("e")).isSameAs(f);
        assertThat(f.withSerie("s")).isSameAs(f);
        assertThat(f.withFormat("f")).isSameAs(f);
        assertThat(f.withIdioma("i")).isSameAs(f);
        assertThat(f.withSort(new EspecificacioOrdenacio("nom", true))).isSameAs(f);
    }

    @Test
    @DisplayName("copy() preserves all fields but is a distinct instance")
    void copiarIsDeep() {
        LlibreFilter src = LlibreFilter.empty()
            .withAutor("a").withNom("n").withIsbn(1L)
            .withAnyMin(1900).withAnyMax(2025)
            .withValoracioMin(0.0).withValoracioMax(10.0)
            .withPreuMin(0.0).withPreuMax(100.0)
            .withLlegit(true).withTagId(2).withLlistaId(3)
            .withEditorial("e").withSerie("s").withFormat("f").withIdioma("i")
            .withSort(new EspecificacioOrdenacio("any", false));
        LlibreFilter c = src.copy();
        assertThat(c).isNotSameAs(src);
        assertThat(c.obtenirAutor()).isEqualTo("a");
        assertThat(c.obtenirNom()).isEqualTo("n");
        assertThat(c.obtenirIsbn()).isEqualTo(1L);
        assertThat(c.obtenirAnyMin()).isEqualTo(1900);
        assertThat(c.obtenirAnyMax()).isEqualTo(2025);
        assertThat(c.obtenirValoracioMin()).isEqualTo(0.0);
        assertThat(c.obtenirValoracioMax()).isEqualTo(10.0);
        assertThat(c.obtenirPreuMin()).isEqualTo(0.0);
        assertThat(c.obtenirPreuMax()).isEqualTo(100.0);
        assertThat(c.obtenirLlegit()).isTrue();
        assertThat(c.obtenirTagId()).isEqualTo(2);
        assertThat(c.obtenirLlistaId()).isEqualTo(3);
        assertThat(c.obtenirEditorial()).isEqualTo("e");
        assertThat(c.obtenirSerie()).isEqualTo("s");
        assertThat(c.getFormat()).isEqualTo("f");
        assertThat(c.obtenirIdioma()).isEqualTo("i");
        assertThat(c.obtenirSort().column()).isEqualTo("any");
        assertThat(c.obtenirSort().ascending()).isFalse();
    }

    @Test
    @DisplayName("copy() of empty filter is empty")
    void copiarOfEmpty() {
        LlibreFilter c = LlibreFilter.empty().copy();
        assertThat(c.teAnyFilter()).isFalse();
    }

    @Test
    @DisplayName("hasAnyFilter: each field independently flips it to true")
    void teAnyFilterPerField() {
        LlibreFilter f = LlibreFilter.empty();
        // all-null
        assertThat(f.teAnyFilter()).isFalse();

        f.withAutor("x"); assertThat(f.teAnyFilter()).isTrue(); f.withAutor(null);
        f.withNom("x");   assertThat(f.teAnyFilter()).isTrue(); f.withNom(null);
        f.withIsbn(1L);   assertThat(f.teAnyFilter()).isTrue(); f.withIsbn(null);
        f.withAnyMin(1);  assertThat(f.teAnyFilter()).isTrue(); f.withAnyMin(null);
        f.withAnyMax(1);  assertThat(f.teAnyFilter()).isTrue(); f.withAnyMax(null);
        f.withValoracioMin(0.0); assertThat(f.teAnyFilter()).isTrue(); f.withValoracioMin(null);
        f.withValoracioMax(0.0); assertThat(f.teAnyFilter()).isTrue(); f.withValoracioMax(null);
        f.withPreuMin(0.0);      assertThat(f.teAnyFilter()).isTrue(); f.withPreuMin(null);
        f.withPreuMax(0.0);      assertThat(f.teAnyFilter()).isTrue(); f.withPreuMax(null);
        f.withLlegit(true);      assertThat(f.teAnyFilter()).isTrue(); f.withLlegit(null);
        f.withTagId(1);          assertThat(f.teAnyFilter()).isTrue(); f.withTagId(null);
        f.withLlistaId(1);       assertThat(f.teAnyFilter()).isTrue(); f.withLlistaId(null);
        f.withEditorial("e");    assertThat(f.teAnyFilter()).isTrue(); f.withEditorial(null);
        f.withSerie("s");        assertThat(f.teAnyFilter()).isTrue(); f.withSerie(null);
        f.withFormat("f");       assertThat(f.teAnyFilter()).isTrue(); f.withFormat(null);
        f.withIdioma("i");       assertThat(f.teAnyFilter()).isTrue(); f.withIdioma(null);
    }
}
