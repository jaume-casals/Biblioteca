package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics;

import static org.assertj.core.api.Assertions.*;

class ColorUtilsTest {

    @Test
    @DisplayName("toHex: formats RGB as #RRGGBB")
    void toHexRgb() {
        assertThat(ColorUtils.toHex(new Color(0xAA, 0xBB, 0xCC))).isEqualTo("#AABBCC");
        assertThat(ColorUtils.toHex(new Color(0, 0, 0))).isEqualTo("#000000");
        assertThat(ColorUtils.toHex(new Color(255, 255, 255))).isEqualTo("#FFFFFF");
    }

    @Test
    @DisplayName("toHex: null → null")
    void toHexNull() {
        assertThat(ColorUtils.toHex(null)).isNull();
    }

    @Test
    @DisplayName("fromHex: 6-digit hex parses to Color")
    void fromHex6() {
        Color c = ColorUtils.fromHex("#AABBCC");
        assertThat(c).isNotNull();
        assertThat(c.getRed()).isEqualTo(0xAA);
        assertThat(c.getGreen()).isEqualTo(0xBB);
        assertThat(c.getBlue()).isEqualTo(0xCC);
    }

    @Test
    @DisplayName("fromHex: 6-digit hex without # also parses")
    void fromHex6NoHash() {
        assertThat(ColorUtils.fromHex("AABBCC")).isNotNull();
    }

    @Test
    @DisplayName("fromHex: 3-digit hex expands to 6-digit")
    void fromHex3Expands() {
        Color c = ColorUtils.fromHex("#abc");
        assertThat(c).isNotNull();
        assertThat(c.getRed()).isEqualTo(0xAA);
        assertThat(c.getGreen()).isEqualTo(0xBB);
        assertThat(c.getBlue()).isEqualTo(0xCC);
    }

    @Test
    @DisplayName("fromHex: 3-digit hex without #")
    void fromHex3NoHash() {
        Color c = ColorUtils.fromHex("abc");
        assertThat(c).isNotNull();
    }

    @Test
    @DisplayName("fromHex: null / blank / wrong length → null")
    void fromHexInvalid() {
        assertThat(ColorUtils.fromHex(null)).isNull();
        assertThat(ColorUtils.fromHex("")).isNull();
        assertThat(ColorUtils.fromHex("   ")).isNull();
        assertThat(ColorUtils.fromHex("#")).isNull();
        assertThat(ColorUtils.fromHex("#ab")).isNull();
        assertThat(ColorUtils.fromHex("#abcd")).isNull();
        assertThat(ColorUtils.fromHex("#abcdefg")).isNull();
        assertThat(ColorUtils.fromHex("#zzzzzz")).isNull();
    }

    @Test
    @DisplayName("fromHex: uppercase letters work")
    void fromHexUppercase() {
        assertThat(ColorUtils.fromHex("#ABCDEF")).isNotNull();
    }

    @Test
    @DisplayName("colorSwatch: returns 14x14 Icon")
    void colorSwatchDimensions() {
        var icon = ColorUtils.colorSwatch(Color.RED);
        assertThat(icon.getIconWidth()).isEqualTo(14);
        assertThat(icon.getIconHeight()).isEqualTo(14);
    }

    @Test
    @DisplayName("colorSwatch: paintIcon with null Graphics throws NPE (documented Swing contract)")
    void colorSwatchPaintNoop() {
        var icon = ColorUtils.colorSwatch(null);
        // paintIcon delegates to Graphics.setColor / fillRoundRect without null-guarding
        // the Graphics argument; passing null is a contract violation in Swing. The
        // behaviour is NPE — recorded here so a future null-safe change is a clear
        // behaviour break.
        assertThatThrownBy(() -> icon.paintIcon(null, (Graphics) null, 0, 0))
            .isInstanceOf(NullPointerException.class);
    }
}
