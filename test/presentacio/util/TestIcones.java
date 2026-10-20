package presentacio.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Proves de la família d'icones SVG locals.
 *
 * <p>Les icones són recursos versionats al repositori: no es descarrega
 * res en temps d'execució. Un nom incorrecte no ha de fer caure la
 * interfície, sinó deixar un espai bui on hauria anar la icona.
 */
class TestIcones {

    @Test
    @DisplayName("Una icona coneguda es rasteritza a la mida demanada")
    void iconaConeguda() {
        Icon ic = Icones.icon("cerca", 16, Color.BLACK);
        assertThat(ic).isNotNull();
        assertThat(ic.getIconWidth()).isEqualTo(16);
        assertThat(ic.getIconHeight()).isEqualTo(16);
    }

    @Test
    @DisplayName("La icona conté píxels dibuixats (no és un espai buit)")
    void iconaDibuixada() {
        Icon ic = Icones.icon("biblioteca", 24, new Color(0x1F2430));
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        ic.paintIcon(null, g, 0, 0);
        g.dispose();

        int pixels = 0;
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 24; x++) {
                if ((img.getRGB(x, y) >>> 24) > 0) pixels++;
            }
        }
        assertThat(pixels).as("la icona ha de dibuixar alguna línia").isGreaterThan(10);
    }

    @Test
    @DisplayName("Un nom d'icona desconegut no llança excepció")
    void iconaDesconegudaNoLanca() {
        assertThatCode(() -> {
            Icon ic = Icones.icon("no-existeix", 16, Color.BLACK);
            assertThat(ic).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("La memòria cau reutilitza la mateixa instància per al mateix color")
    void memoriaCau() {
        Icon a = Icones.icon("cerca", 16, Color.BLACK);
        Icon b = Icones.icon("cerca", 16, Color.BLACK);
        assertThat(a).isSameAs(b);
    }

    @Test
    @DisplayName("Invalidar la memòria cau en redibuixa les icones")
    void invalidar() {
        Icon abans = Icones.icon("cerca", 16, Color.BLACK);
        Icones.invalidar();
        Icon després = Icones.icon("cerca", 16, Color.BLACK);
        assertThat(peròNoÉsElMateix(abans, després)).isTrue();
    }

    private static boolean peròNoÉsElMateix(Object a, Object b) {
        return a != b;
    }
}
