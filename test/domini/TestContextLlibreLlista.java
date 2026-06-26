package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestContextLlibreLlista {

    @Test
    @DisplayName("constructor exposes all 7 fields")
    void constructorExposes() {
        var ctx = new LlibreLlistaContext(9780306406157L, 1, "Favorits", 2, "#abc", 9.0, true);
        assertThat(ctx.isbn()).isEqualTo(9780306406157L);
        assertThat(ctx.llistaId()).isEqualTo(1);
        assertThat(ctx.nom()).isEqualTo("Favorits");
        assertThat(ctx.ordre()).isEqualTo(2);
        assertThat(ctx.color()).isEqualTo("#abc");
        assertThat(ctx.valoracio()).isEqualTo(9.0);
        assertThat(ctx.llegit()).isTrue();
    }

    @Test
    @DisplayName("canonical constructor returns expected record")
    void canonicalConstructor() {
        var ctx = new LlibreLlistaContext(123L, 4, "S", 0, null, null, null);
        assertThat(ctx.isbn()).isEqualTo(123L);
        assertThat(ctx.llistaId()).isEqualTo(4);
        assertThat(ctx.nom()).isEqualTo("S");
        assertThat(ctx.color()).isNull();
        assertThat(ctx.valoracio()).isNull();
        assertThat(ctx.llegit()).isNull();
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        var a = new LlibreLlistaContext(1L, 2, "x", 3, "#abc", 4.0, true);
        var b = new LlibreLlistaContext(1L, 2, "x", 3, "#abc", 4.0, true);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
