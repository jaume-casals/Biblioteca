package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MidaLletraTest {

    @Test
    @DisplayName("SMALL: key='small', px=11")
    void small() {
        assertThat(MidaLletra.SMALL.key).isEqualTo("small");
        assertThat(MidaLletra.SMALL.px).isEqualTo(11);
    }

    @Test
    @DisplayName("MEDIUM: key='medium', px=13")
    void medium() {
        assertThat(MidaLletra.MEDIUM.key).isEqualTo("medium");
        assertThat(MidaLletra.MEDIUM.px).isEqualTo(13);
    }

    @Test
    @DisplayName("LARGE: key='large', px=16")
    void large() {
        assertThat(MidaLletra.LARGE.key).isEqualTo("large");
        assertThat(MidaLletra.LARGE.px).isEqualTo(16);
    }

    @Test
    @DisplayName("fromKey: 'small' → SMALL")
    void fromKeySmall() {
        assertThat(MidaLletra.fromKey("small")).isEqualTo(MidaLletra.SMALL);
    }

    @Test
    @DisplayName("fromKey: 'large' → LARGE")
    void fromKeyLarge() {
        assertThat(MidaLletra.fromKey("large")).isEqualTo(MidaLletra.LARGE);
    }

    @Test
    @DisplayName("fromKey: 'medium' / unknown / mixed case → MEDIUM")
    void fromKeyMedium() {
        assertThat(MidaLletra.fromKey("medium")).isEqualTo(MidaLletra.MEDIUM);
        assertThat(MidaLletra.fromKey("Medium")).isEqualTo(MidaLletra.MEDIUM);
        assertThat(MidaLletra.fromKey("garbage")).isEqualTo(MidaLletra.MEDIUM);
    }

    @Test
    @DisplayName("fromKey: null → MEDIUM")
    void fromKeyNull() {
        assertThat(MidaLletra.fromKey(null)).isEqualTo(MidaLletra.MEDIUM);
    }
}
