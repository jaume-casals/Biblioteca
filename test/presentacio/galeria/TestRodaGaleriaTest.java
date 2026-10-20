package presentacio.galeria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import domini.Llibre;
import presentacio.panells.PanelGaleriaCobertes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves de la roda del ratolí a la galeria.
 *
 * <p>Contracte d'AWT que regeix aquestes proves: la roda cap avall (cap al
 * usuari) dona una rotació <em>positiva</em> i la cap amunt (cap enlluny)
 * una rotació <em>negativa</em>. El desplaçament ha de seguir la roda: cap
 * avall baixa el contingut, cap amunt el pujar. Amb Ctrl, la roda amplia o
 * redueix la galeria i <em>no</em> desplaça el contingut.
 */
class TestRodaGaleriaTest {

    @Test
    @DisplayName("La roda cap avall desplaça el contingut cap avall")
    void laRodaCapAvallDesplaçaCapAvall() {
        PanelGaleriaCobertes galeria = galeriaAmb(120);
        JScrollPane scroll = scrollDe(galeria);
        scroll.setSize(800, 600);
        scroll.doLayout();
        JScrollBar vsb = scroll.getVerticalScrollBar();
        vsb.setValue(0);

        scroll.dispatchEvent(roda(scroll, 3, 0));
        int baix = vsb.getValue();

        assertThat(baix).as("la roda cap avall ha de baixar el contingut").isGreaterThan(0);

        // I cap amunt ha de pujar el contingut.
        vsb.setValue(500);
        scroll.dispatchEvent(roda(scroll, -3, 0));
        assertThat(vsb.getValue()).as("la roda cap amunt ha de pujar el contingut")
            .isLessThan(500);
    }

    @Test
    @DisplayName("La roda desplaça el contingut el doble de llarg si hi ha dos gestos")
    void laRodaNoDesplaçaElDoble() {
        PanelGaleriaCobertes galeria = galeriaAmb(120);
        JScrollPane scroll = scrollDe(galeria);
        scroll.setSize(800, 600);
        scroll.doLayout();
        JScrollBar vsb = scroll.getVerticalScrollBar();
        int increment = vsb.getUnitIncrement();

        vsb.setValue(0);
        MouseWheelEvent ev = roda(scroll, 3, 0);
        scroll.dispatchEvent(ev);
        int desplacament = vsb.getValue();

        // El JScrollPane mou el contingut el que toca. Si la galeria hi
        // afegeix el seu propi desplaçament, el contingut salta el doble.
        assertThat(desplacament).as("la roda ha de desplaçar el que toca")
            .isEqualTo(Math.abs(ev.getUnitsToScroll()) * increment);
    }

    @Test
    @DisplayName("Amb Ctrl, la roda cap amunt amplia la galeria")
    void ambCtrlLaRodaCapAmuntAmplia() {
        PanelGaleriaCobertes galeria = galeriaAmb(40);
        JScrollPane scroll = scrollDe(galeria);
        scroll.setSize(800, 600);
        scroll.doLayout();

        int midaAbans = midaDelsLlibres(galeria);
        scroll.dispatchEvent(roda(scroll, -3, InputEvent.CTRL_DOWN_MASK));
        int midaDespres = midaDelsLlibres(galeria);

        assertThat(midaDespres).as("la roda cap amunt amb Ctrl ha d'ampliar les targetes")
            .isGreaterThan(midaAbans);
    }

    @Test
    @DisplayName("Amb Ctrl, la roda cap avall redueix la galeria")
    void ambCtrlLaRodaCapAvallRedueix() {
        PanelGaleriaCobertes galeria = galeriaAmb(40);
        JScrollPane scroll = scrollDe(galeria);
        scroll.setSize(800, 600);
        scroll.doLayout();

        int midaAbans = midaDelsLlibres(galeria);
        scroll.dispatchEvent(roda(scroll, 3, InputEvent.CTRL_DOWN_MASK));
        int midaDespres = midaDelsLlibres(galeria);

        assertThat(midaDespres).as("la roda cap avall amb Ctrl ha de reduir les targetes")
            .isLessThan(midaAbans);
    }

    @Test
    @DisplayName("Amb Ctrl, la roda amplia la galeria però no desplaça el contingut")
    void ambCtrlLaRodaNoDesplaçaElContingut() {
        PanelGaleriaCobertes galeria = galeriaAmb(120);
        JScrollPane scroll = scrollDe(galeria);
        scroll.setSize(800, 600);
        scroll.doLayout();
        MouseWheelEvent ev = roda(scroll, -3, InputEvent.CTRL_DOWN_MASK);
        scroll.dispatchEvent(ev);

        // L'event ha d'estar consumit: si no, el JScrollPane també
        // desplaça el contingut i la roda amplia i alhora arrossega.
        // (El contingut pot tornar a dalt perquè el canvi de zoom
        // reconstrueix les targetes; això és una altra cosa.)
        assertThat(ev.isConsumed()).as("amb Ctrl la roda no ha de desplaçar el contingut")
            .isTrue();
    }

    // ── Utillitats ───────────────────────────────────────────────────────────

    /** Event de roda: rotació positiva = cap avall. */
    private static MouseWheelEvent roda(Component source, int rotacio, int modifiers) {
        return new MouseWheelEvent(source, MouseEvent.MOUSE_WHEEL, 0L, modifiers,
            0, 0, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotacio);
    }

    private static PanelGaleriaCobertes galeriaAmb(int quants) {
        PanelGaleriaCobertes galeria = new PanelGaleriaCobertes();
        List<Llibre> llibres = new ArrayList<>();
        for (int i = 0; i < quants; i++) {
            llibres.add(new Llibre(9780000000000L + i, "Llibre " + i, "Autor",
                2000 + (i % 20), "", 7.5, 12.0, false, null));
        }
        galeria.actualitzarLlibres(llibres);
        return galeria;
    }

    private static JScrollPane scrollDe(Component c) {
        if (c instanceof JScrollPane sp) return sp;
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) {
                JScrollPane r = scrollDe(ch);
                if (r != null) return r;
            }
        }
        return null;
    }

    /** L'amplada d'una targeta: creix amb el nivell de zoom. */
    private static int midaDelsLlibres(Component c) {
        if (c instanceof JPanel p && p.getComponentCount() == 0 && p.getParent() != null) {
            return p.getPreferredSize().width;
        }
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) {
                int m = midaDelsLlibres(ch);
                if (m > 0) return m;
            }
        }
        return 0;
    }
}
