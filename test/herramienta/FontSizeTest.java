package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FontSizeTest {

    @Test
    @DisplayName("SMALL: key='small', px=11")
    void small() {
        assertThat(FontSize.SMALL.key).isEqualTo("small");
        assertThat(FontSize.SMALL.px).isEqualTo(11);
    }

    @Test
    @DisplayName("MEDIUM: key='medium', px=13")
    void medium() {
        assertThat(FontSize.MEDIUM.key).isEqualTo("medium");
        assertThat(FontSize.MEDIUM.px).isEqualTo(13);
    }

    @Test
    @DisplayName("LARGE: key='large', px=16")
    void large() {
        assertThat(FontSize.LARGE.key).isEqualTo("large");
        assertThat(FontSize.LARGE.px).isEqualTo(16);
    }

    @Test
    @DisplayName("fromKey: 'small' → SMALL")
    void fromKeySmall() {
        assertThat(FontSize.fromKey("small")).isEqualTo(FontSize.SMALL);
    }

    @Test
    @DisplayName("fromKey: 'large' → LARGE")
    void fromKeyLarge() {
        assertThat(FontSize.fromKey("large")).isEqualTo(FontSize.LARGE);
    }

    @Test
    @DisplayName("fromKey: 'medium' / unknown / mixed case → MEDIUM")
    void fromKeyMedium() {
        assertThat(FontSize.fromKey("medium")).isEqualTo(FontSize.MEDIUM);
        assertThat(FontSize.fromKey("Medium")).isEqualTo(FontSize.MEDIUM);
        assertThat(FontSize.fromKey("garbage")).isEqualTo(FontSize.MEDIUM);
    }

    @Test
    @DisplayName("fromKey: null → MEDIUM")
    void fromKeyNull() {
        assertThat(FontSize.fromKey(null)).isEqualTo(FontSize.MEDIUM);
    }
}
