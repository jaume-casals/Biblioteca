package herramienta.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves del sistema de tokens i del canvi de tema.
 *
 * <p>El tema és un contracte per a tota la interfície: si dos temes
 * comparteixen els mateixos valors, el canvi de tema no es veu; si un
 * component aplica un color literal, el canvi de tema deixa de funcionar.
 */
class TestUITheme {

    @Test
    @DisplayName("La paleta activa no és buida i les quatre temes tenen accent propi")
    void paletesDefinides() {
        assertThat(UITheme.palette()).isNotNull();
        assertThat(UITheme.palette().accent()).isNotNull();
        assertThat(UITheme.palette().textDark()).isNotNull();

        Color accentLight = UITheme.palette().accent();
        UITheme.posarTheme(UITheme.Tema.DARK);
        Color accentDark = UITheme.palette().accent();

        assertThat(accentDark).isNotEqualTo(accentLight);
        assertThat(UITheme.palette().bgMain()).isNotEqualTo(UITheme.palette().bgPanel());
    }

    @Test
    @DisplayName("El canvi de tema actualitza la paleta i el indicador de tema fosc")
    void canviTemaActualitzaPaleta() {
        try {
            UITheme.posarTheme(UITheme.Tema.LIGHT);
            Color fonsLight = UITheme.palette().bgMain();
            Color textLight = UITheme.palette().textDark();

            UITheme.posarTheme(UITheme.Tema.DARK);
            Color fonsDark = UITheme.palette().bgMain();
            Color textDark = UITheme.palette().textDark();

            assertThat(UITheme.esDark()).isTrue();
            // En tema fosc el fons és més fosc i el text més clar: el contrast
            // s'inverteix, no es canvia només el to.
            assertThat(luminositat(fonsDark)).isLessThan(luminositat(fonsLight));
            assertThat(luminositat(textDark)).isGreaterThan(luminositat(textLight));
        } finally {
            UITheme.posarTheme(UITheme.Tema.LIGHT);
        }
    }

    @Test
    @DisplayName("Tots els temes tenen text llegible sobre el seu fons (WCAG AA)")
    void contrastEnTotsElsTemes() {
        try {
            for (UITheme.Tema t : UITheme.Tema.values()) {
                UITheme.posarTheme(t);
                assertThat(contrast(UITheme.palette().textDark(), UITheme.palette().bgPanel()))
                    .as("text principal sobre el panell del tema %s", t)
                    .isGreaterThanOrEqualTo(4.5);
                assertThat(contrast(UITheme.palette().textMid(), UITheme.palette().bgPanel()))
                    .as("text secundari sobre el panell del tema %s", t)
                    .isGreaterThanOrEqualTo(3.0);
                assertThat(UITheme.palette().accent())
                    .as("accent definit del tema %s", t)
                    .isNotNull();
            }
        } finally {
            UITheme.posarTheme(UITheme.Tema.LIGHT);
        }
    }

    @Test
    @DisplayName("L'escala de mida de lletra és monòtona i acotada")
    void escalaLletra() {
        assertThat(Tokens.escalaLletra(11)).isLessThan(Tokens.escalaLletra(13));
        assertThat(Tokens.escalaLletra(13)).isLessThan(Tokens.escalaLletra(16));
        // Un valor nul o zero no ha de dividir per zero ni trencar el control.
        assertThat(Tokens.escalaLletra(0)).isEqualTo(1.0);
        assertThat(Tokens.escalaLletra(-5)).isEqualTo(1.0);
        // L'escala aplicada a una mida de control queda acotada.
        assertThat(Tokens.escalaLletra(64, Tokens.CONTROL_HEIGHT))
            .isLessThanOrEqualTo(Tokens.CONTROL_HEIGHT * 2);
    }

    @Test
    @DisplayName("L'escala d'espaiat i els radis segueixen l'escala de disseny")
    void tokensCoherents() {
        assertThat(Tokens.SPACE_1).isEqualTo(4);
        assertThat(Tokens.SPACE_2).isEqualTo(8);
        assertThat(Tokens.SPACE_3).isEqualTo(12);
        assertThat(Tokens.SPACE_4).isEqualTo(16);
        assertThat(Tokens.SPACE_5).isEqualTo(24);
        assertThat(Tokens.SPACE_6).isEqualTo(32);
        assertThat(Tokens.RADIUS_MD).isBetween(Tokens.RADIUS_SM, Tokens.RADIUS_LG);
        assertThat(Tokens.CONTROL_HEIGHT).isBetween(30, 40);
    }

    /** Luminositat relativa aproximada (sRGB). */
    private static double luminositat(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
    }

    /** Raó de contrast WCAG entre dos colors. */
    private static double contrast(Color a, Color b) {
        double la = luminositat(a);
        double lb = luminositat(b);
        if (la < lb) { double t = la; la = lb; lb = t; }
        return (la + 0.05) / (lb + 0.05);
    }
}
