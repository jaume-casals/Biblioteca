package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics;

import static org.assertj.core.api.Assertions.*;

class TestUtilitatsColor {

    @Test
    @DisplayName("toHex: formata RGB com a #RRGGBB")
    void toHexRgb() {
        assertThat(UtilitatsColor.toHex(new Color(0xAA, 0xBB, 0xCC))).isEqualTo("#AABBCC");
        assertThat(UtilitatsColor.toHex(new Color(0, 0, 0))).isEqualTo("#000000");
        assertThat(UtilitatsColor.toHex(new Color(255, 255, 255))).isEqualTo("#FFFFFF");
    }

    @Test
    @DisplayName("toHex: null → null")
    void toHexNull() {
        assertThat(UtilitatsColor.toHex(null)).isNull();
    }

    @Test
    @DisplayName("fromHex: hex de 6 dígits s'analitza com a Color")
    void fromHex6() {
        Color c = UtilitatsColor.fromHex("#AABBCC");
        assertThat(c).isNotNull();
        assertThat(c.getRed()).isEqualTo(0xAA);
        assertThat(c.getGreen()).isEqualTo(0xBB);
        assertThat(c.getBlue()).isEqualTo(0xCC);
    }

    @Test
    @DisplayName("fromHex: hex de 6 dígits sense # també s'analitza")
    void fromHex6NoHash() {
        assertThat(UtilitatsColor.fromHex("AABBCC")).isNotNull();
    }

    @Test
    @DisplayName("fromHex: hex de 3 dígits s'expandeix a 6 dígits")
    void fromHex3Expands() {
        Color c = UtilitatsColor.fromHex("#abc");
        assertThat(c).isNotNull();
        assertThat(c.getRed()).isEqualTo(0xAA);
        assertThat(c.getGreen()).isEqualTo(0xBB);
        assertThat(c.getBlue()).isEqualTo(0xCC);
    }

    @Test
    @DisplayName("fromHex: hex de 3 dígits sense #")
    void fromHex3NoHash() {
        Color c = UtilitatsColor.fromHex("abc");
        assertThat(c).isNotNull();
    }

    @Test
    @DisplayName("fromHex: null / buit / longitud incorrecta → null")
    void fromHexInvalid() {
        assertThat(UtilitatsColor.fromHex(null)).isNull();
        assertThat(UtilitatsColor.fromHex("")).isNull();
        assertThat(UtilitatsColor.fromHex("   ")).isNull();
        assertThat(UtilitatsColor.fromHex("#")).isNull();
        assertThat(UtilitatsColor.fromHex("#ab")).isNull();
        assertThat(UtilitatsColor.fromHex("#abcd")).isNull();
        assertThat(UtilitatsColor.fromHex("#abcdefg")).isNull();
        assertThat(UtilitatsColor.fromHex("#zzzzzz")).isNull();
    }

    @Test
    @DisplayName("fromHex: les lletres majúscules funcionen")
    void fromHexUppercase() {
        assertThat(UtilitatsColor.fromHex("#ABCDEF")).isNotNull();
    }

    @Test
    @DisplayName("colorSwatch: retorna una Icon de 14x14")
    void colorSwatchDimensions() {
        var icon = UtilitatsColor.colorSwatch(Color.RED);
        assertThat(icon.getIconWidth()).isEqualTo(14);
        assertThat(icon.getIconHeight()).isEqualTo(14);
    }

    @Test
    @DisplayName("colorSwatch: paintIcon amb Graphics null llença NPE (contracte Swing documentat)")
    void colorSwatchPaintNoop() {
        var icon = UtilitatsColor.colorSwatch(null);
        // paintIcon delega a Graphics.setColor / fillRoundRect sense protegir-se
        // del null en l'argument Graphics; passar null és una violació de contracte
        // en Swing. El comportament és NPE — es registra aquí perquè un futur
        // canvi a prova de null sigui una ruptura de comportament clara.
        assertThatThrownBy(() -> icon.paintIcon(null, (Graphics) null, 0, 0))
            .isInstanceOf(NullPointerException.class);
    }
}
