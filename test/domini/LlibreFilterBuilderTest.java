package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LlibreFilterBuilderTest {

    @Test
    @DisplayName("of() returns a fresh builder")
    void ofReturnsBuilder() {
        assertThat(LlibreFilterBuilder.of()).isNotNull();
    }

    @Test
    @DisplayName("every setter returns this for chaining")
    void allSettersChainable() {
        LlibreFilterBuilder b = LlibreFilterBuilder.of();
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
    void sortWraps() {
        LlibreFilter f = LlibreFilterBuilder.of().sort("nom", false).build();
        assertThat(f.getSort().column()).isEqualTo("nom");
        assertThat(f.getSort().ascending()).isFalse();
    }

    @Test
    @DisplayName("build() returns a LlibreFilter with the set fields")
    void buildPopulatesAllFields() {
        LlibreFilter f = LlibreFilterBuilder.of()
            .isbn(123L).autor("A").nom("N").anyMin(1900).anyMax(2025)
            .valoracioMin(0.0).valoracioMax(10.0).preuMin(0.0).preuMax(100.0)
            .llegit(true).tagId(1).llistaId(2)
            .editorial("E").serie("S").format("F").idioma("I")
            .sort("any", true)
            .build();

        assertThat(f.getIsbn()).isEqualTo(123L);
        assertThat(f.getAutor()).isEqualTo("A");
        assertThat(f.getNom()).isEqualTo("N");
        assertThat(f.getAnyMin()).isEqualTo(1900);
        assertThat(f.getAnyMax()).isEqualTo(2025);
        assertThat(f.getValoracioMin()).isEqualTo(0.0);
        assertThat(f.getValoracioMax()).isEqualTo(10.0);
        assertThat(f.getPreuMin()).isEqualTo(0.0);
        assertThat(f.getPreuMax()).isEqualTo(100.0);
        assertThat(f.getLlegit()).isTrue();
        assertThat(f.getTagId()).isEqualTo(1);
        assertThat(f.getLlistaId()).isEqualTo(2);
        assertThat(f.getEditorial()).isEqualTo("E");
        assertThat(f.getSerie()).isEqualTo("S");
        assertThat(f.getFormat()).isEqualTo("F");
        assertThat(f.getIdioma()).isEqualTo("I");
        assertThat(f.getSort().column()).isEqualTo("any");
        assertThat(f.getSort().ascending()).isTrue();
    }
}
