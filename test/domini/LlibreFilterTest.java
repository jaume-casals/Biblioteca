package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LlibreFilterTest {

    @Test
    @DisplayName("empty() returns a filter with all-null criteria and default sort")
    void emptyHasAllNulls() {
        LlibreFilter f = LlibreFilter.empty();
        assertThat(f.getAutor()).isNull();
        assertThat(f.getNom()).isNull();
        assertThat(f.getIsbn()).isNull();
        assertThat(f.getAnyMin()).isNull();
        assertThat(f.getAnyMax()).isNull();
        assertThat(f.getValoracioMin()).isNull();
        assertThat(f.getValoracioMax()).isNull();
        assertThat(f.getPreuMin()).isNull();
        assertThat(f.getPreuMax()).isNull();
        assertThat(f.getLlegit()).isNull();
        assertThat(f.getTagId()).isNull();
        assertThat(f.getLlistaId()).isNull();
        assertThat(f.getEditorial()).isNull();
        assertThat(f.getSerie()).isNull();
        assertThat(f.getFormat()).isNull();
        assertThat(f.getIdioma()).isNull();
        assertThat(f.getSort()).isEqualTo(SortSpec.defaultAsc());
    }

    @Test
    @DisplayName("hasAnyFilter is false on an empty filter")
    void emptyHasNoFilters() {
        assertThat(LlibreFilter.empty().hasAnyFilter()).isFalse();
    }

    @Test
    @DisplayName("hasAnyFilter is false even when only sort is set")
    void sortDoesNotCountAsFilter() {
        LlibreFilter f = LlibreFilter.empty().withSort(new SortSpec("nom", true));
        assertThat(f.hasAnyFilter()).isFalse();
    }

    @Test
    @DisplayName("withAutor / withNom / withIsbn set values and count as filters")
    void withStringFields() {
        LlibreFilter f = LlibreFilter.empty();
        f.withAutor("X");
        assertThat(f.getAutor()).isEqualTo("X");
        assertThat(f.hasAnyFilter()).isTrue();

        LlibreFilter f2 = LlibreFilter.empty();
        f2.withNom("N");
        assertThat(f2.getNom()).isEqualTo("N");
        assertThat(f2.hasAnyFilter()).isTrue();

        LlibreFilter f3 = LlibreFilter.empty();
        f3.withIsbn(123L);
        assertThat(f3.getIsbn()).isEqualTo(123L);
        assertThat(f3.hasAnyFilter()).isTrue();
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
        assertThat(f.withSort(new SortSpec("nom", true))).isSameAs(f);
    }

    @Test
    @DisplayName("copy() preserves all fields but is a distinct instance")
    void copyIsDeep() {
        LlibreFilter src = LlibreFilter.empty()
            .withAutor("a").withNom("n").withIsbn(1L)
            .withAnyMin(1900).withAnyMax(2025)
            .withValoracioMin(0.0).withValoracioMax(10.0)
            .withPreuMin(0.0).withPreuMax(100.0)
            .withLlegit(true).withTagId(2).withLlistaId(3)
            .withEditorial("e").withSerie("s").withFormat("f").withIdioma("i")
            .withSort(new SortSpec("any", false));
        LlibreFilter c = src.copy();
        assertThat(c).isNotSameAs(src);
        assertThat(c.getAutor()).isEqualTo("a");
        assertThat(c.getNom()).isEqualTo("n");
        assertThat(c.getIsbn()).isEqualTo(1L);
        assertThat(c.getAnyMin()).isEqualTo(1900);
        assertThat(c.getAnyMax()).isEqualTo(2025);
        assertThat(c.getValoracioMin()).isEqualTo(0.0);
        assertThat(c.getValoracioMax()).isEqualTo(10.0);
        assertThat(c.getPreuMin()).isEqualTo(0.0);
        assertThat(c.getPreuMax()).isEqualTo(100.0);
        assertThat(c.getLlegit()).isTrue();
        assertThat(c.getTagId()).isEqualTo(2);
        assertThat(c.getLlistaId()).isEqualTo(3);
        assertThat(c.getEditorial()).isEqualTo("e");
        assertThat(c.getSerie()).isEqualTo("s");
        assertThat(c.getFormat()).isEqualTo("f");
        assertThat(c.getIdioma()).isEqualTo("i");
        assertThat(c.getSort().column()).isEqualTo("any");
        assertThat(c.getSort().ascending()).isFalse();
    }

    @Test
    @DisplayName("copy() of empty filter is empty")
    void copyOfEmpty() {
        LlibreFilter c = LlibreFilter.empty().copy();
        assertThat(c.hasAnyFilter()).isFalse();
    }

    @Test
    @DisplayName("hasAnyFilter: each field independently flips it to true")
    void hasAnyFilterPerField() {
        LlibreFilter f = LlibreFilter.empty();
        // all-null
        assertThat(f.hasAnyFilter()).isFalse();

        f.withAutor("x"); assertThat(f.hasAnyFilter()).isTrue(); f.withAutor(null);
        f.withNom("x");   assertThat(f.hasAnyFilter()).isTrue(); f.withNom(null);
        f.withIsbn(1L);   assertThat(f.hasAnyFilter()).isTrue(); f.withIsbn(null);
        f.withAnyMin(1);  assertThat(f.hasAnyFilter()).isTrue(); f.withAnyMin(null);
        f.withAnyMax(1);  assertThat(f.hasAnyFilter()).isTrue(); f.withAnyMax(null);
        f.withValoracioMin(0.0); assertThat(f.hasAnyFilter()).isTrue(); f.withValoracioMin(null);
        f.withValoracioMax(0.0); assertThat(f.hasAnyFilter()).isTrue(); f.withValoracioMax(null);
        f.withPreuMin(0.0);      assertThat(f.hasAnyFilter()).isTrue(); f.withPreuMin(null);
        f.withPreuMax(0.0);      assertThat(f.hasAnyFilter()).isTrue(); f.withPreuMax(null);
        f.withLlegit(true);      assertThat(f.hasAnyFilter()).isTrue(); f.withLlegit(null);
        f.withTagId(1);          assertThat(f.hasAnyFilter()).isTrue(); f.withTagId(null);
        f.withLlistaId(1);       assertThat(f.hasAnyFilter()).isTrue(); f.withLlistaId(null);
        f.withEditorial("e");    assertThat(f.hasAnyFilter()).isTrue(); f.withEditorial(null);
        f.withSerie("s");        assertThat(f.hasAnyFilter()).isTrue(); f.withSerie(null);
        f.withFormat("f");       assertThat(f.hasAnyFilter()).isTrue(); f.withFormat(null);
        f.withIdioma("i");       assertThat(f.hasAnyFilter()).isTrue(); f.withIdioma(null);
    }
}
