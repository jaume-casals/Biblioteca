package presentacio.formularis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTextField;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves de la disposició del formulari.
 *
 * <p>El problema que es vol evitar: amb un {@code GridLayout} tots els
 * camps comparteixen la mateixa alçada, de manera que en enlargir el
 * diàleg els camps d'una línia creixen fins a esdevenir camps gegants.
 * Amb {@code GridBagLayout} el camp conserva la seva alçada preferida.
 */
class TestConstructorGraellaFormulari {

    @Test
    @DisplayName("Un camp d'una línia no creix en alçada quan el formulari s'enlargeix")
    void campNoCreixEnAlcada() {
        JPanel form = new JPanel();
        ConstructorGraellaFormulari b = ConstructorGraellaFormulari.columnsOf(form);
        JTextField field = new JTextField("títol");
        b.afegirField("Títol", field);

        form.setSize(600, 200);
        form.doLayout();
        java.awt.Rectangle inicial = field.getBounds();

        // Formulari més alt: el camp NO ha de créixer en alçada.
        form.setSize(600, 800);
        form.doLayout();
        java.awt.Rectangle alt = field.getBounds();

        assertThat(alt.height)
            .as("l'alçada del camp ha de ser la mateixa amb un formulari alt")
            .isEqualTo(inicial.height);

        // Formulari més ample: el camp sí que s'ha d'estendre.
        form.setSize(900, 800);
        form.doLayout();
        java.awt.Rectangle ample = field.getBounds();

        assertThat(ample.width)
            .as("el camp sí que s'ha d'estendre horitzontalment")
            .isGreaterThan(alt.width);
        assertThat(ample.height)
            .as("l'amplada no ha d'alterar l'alçada")
            .isEqualTo(alt.height);
    }

    @Test
    @DisplayName("L'etiqueta no es retalla: la seva amplada mínima es conserva")
    void etiquetaNoEsRetalla() {
        JPanel form = new JPanel();
        ConstructorGraellaFormulari b = ConstructorGraellaFormulari.columnsOf(form);
        JTextField field = new JTextField();
        b.afegirField("Editorial", field);

        form.setSize(400, 200);
        form.doLayout();

        assertThat(field.getBounds().x)
            .as("el camp ha de començar després de l'etiqueta, no a sobre")
            .isGreaterThan(0);
    }

    @Test
    @DisplayName("La zona de text multilínia és l'única que pot absorbir alçada extra")
    void areaPotCreixer() {
        JPanel form = new JPanel();
        ConstructorGraellaFormulari b = ConstructorGraellaFormulari.columnsOf(form);
        JTextField field = new JTextField();
        JTextField area = new JTextField();
        b.afegirField("Títol", field);
        b.afegirArea("Notes", area);

        form.setSize(500, 200);
        form.doLayout();
        int alçadaAreaBuida = area.getBounds().height;

        form.setSize(500, 800);
        form.doLayout();

        assertThat(area.getBounds().height)
            .as("la zona multilínia és la que ha d'assumir l'alçada sobrant")
            .isGreaterThanOrEqualTo(alçadaAreaBuida);
    }
}
