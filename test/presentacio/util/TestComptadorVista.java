package presentacio.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import presentacio.panells.PanelMostrarBiblioteca;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves del recompte de llibres de la vista.
 *
 * <p>El comptador visible abans es derivava de dues quantitats independents
 * (el nombre de files del model de la taula i la mida de la llista en
 * memòria). Quan un resultat asíncron obsolet tocava una de les dues i no
 * l'altra, el recompte mostrava xifres contradictòries com "1000 / 0
 * llibres". Ara el recompte té una sola font.
 */
class TestComptadorVista {

    @Test
    @DisplayName("El recompte distingeix llibres en l'àmbit de llibres mostrats")
    void recompteDosValors() {
        EstatVistaBiblioteca state = new EstatVistaBiblioteca(null, java.util.List.of(), null, null);
        state.posarComptador(1000, 100);
        assertThat(state.obtenirLlibresEnAmbit()).isEqualTo(1000);
        assertThat(state.obtenirLlibresMostrats()).isEqualTo(100);
    }

    @Test
    @DisplayName("Un recompte negatiu es normalitza a zero")
    void recompteNegatiu() {
        EstatVistaBiblioteca state = new EstatVistaBiblioteca(null, java.util.List.of(), null, null);
        state.posarComptador(-5, -1);
        assertThat(state.obtenirLlibresEnAmbit()).isZero();
        assertThat(state.obtenirLlibresMostrats()).isZero();
    }

    @Test
    @DisplayName("El títol de la vista i el recompte es mostren a la capçalera")
    void capcaleraMostraTitolICompte() {
        PanelMostrarBiblioteca panel = new PanelMostrarBiblioteca();
        panel.obtenirCapcalera().posarTitol("Tots els llibres");
        panel.obtenirCapcalera().posarComptador("21 llibres");

        assertThat(panel.obtenirCapcalera().obtenirTitol()).isEqualTo("Tots els llibres");
        assertThat(panel.obtenirCapcalera().obtenirComptador()).isEqualTo("21 llibres");
    }

    @Test
    @DisplayName("El comptador de la capçalera i el de la vista comparteixen el mateix valor")
    void capcaleraIComptadorCoincideixen() {
        PanelMostrarBiblioteca panel = new PanelMostrarBiblioteca();
        EstatVistaBiblioteca state = new EstatVistaBiblioteca(panel, java.util.List.of(), null, null);
        state.posarComptador(42, 42);
        panel.obtenirCapcalera().posarComptador(state.obtenirLlibresEnAmbit() + " llibres");
        assertThat(panel.obtenirCapcalera().obtenirComptador()).isEqualTo("42 llibres");
    }
}
