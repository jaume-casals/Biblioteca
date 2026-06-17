package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static org.assertj.core.api.Assertions.*;

class TestUtilitatsSwing {

    private record Entrada(int id, String name) {}

    @Test
    @DisplayName("reloadComboPreserveSelection: buida i torna a afegir els elements")
    void reloadReplacesItems() {
        JComboBox<Entrada> combo = new JComboBox<>();
        combo.addItem(new Entrada(1, "A"));
        combo.addItem(new Entrada(2, "B"));

        UtilitatsSwing.reloadComboPreserveSelection(combo,
            java.util.List.of(new Entrada(3, "C"), new Entrada(4, "D")),
            Entrada::id);

        assertThat(combo.getItemCount()).isEqualTo(2);
        assertThat(combo.getItemAt(0).name()).isEqualTo("C");
        assertThat(combo.getItemAt(1).name()).isEqualTo("D");
    }

    @Test
    @DisplayName("reloadComboPreserveSelection: restaura la selecció anterior per id")
    void reloadPreservesSelection() {
        JComboBox<Entrada> combo = new JComboBox<>();
        combo.addItem(new Entrada(1, "A"));
        combo.addItem(new Entrada(2, "B"));
        combo.setSelectedIndex(1); // seleccionat = id 2

        // La nova llista conté l'id anterior a l'índex 0
        UtilitatsSwing.reloadComboPreserveSelection(combo,
            java.util.List.of(new Entrada(2, "B"), new Entrada(3, "C"), new Entrada(4, "D")),
            Entrada::id);

        assertThat(combo.getSelectedIndex()).isZero();
        assertThat(combo.getItemAt(0).id()).isEqualTo(2);
    }

    @Test
    @DisplayName("reloadComboPreserveSelection: selecció anterior absent de la nova llista → se selecciona el primer element nou")
    void reloadLosesSelection() {
        JComboBox<Entrada> combo = new JComboBox<>();
        combo.addItem(new Entrada(1, "A"));
        combo.setSelectedIndex(0);

        UtilitatsSwing.reloadComboPreserveSelection(combo,
            java.util.List.of(new Entrada(99, "X"), new Entrada(100, "Y")),
            Entrada::id);

        // JComboBox autoselecciona el primer element després d'addItem() en una llista
        // prèviament poblada. El codi de preservació de selecció no pot trobar id=1 a
        // la nova llista, per la qual cosa la selecció pren per defecte el primer
        // element nou.
        assertThat(combo.getSelectedItem()).isNotNull();
        assertThat(combo.getSelectedItem()).isEqualTo(new Entrada(99, "X"));
    }

    @Test
    @DisplayName("reloadComboPreserveSelection: nova llista buida, sense selecció anterior")
    void reloadEmptyNoPrior() {
        JComboBox<Entrada> combo = new JComboBox<>();
        UtilitatsSwing.reloadComboPreserveSelection(combo, java.util.List.of(), Entrada::id);
        assertThat(combo.getItemCount()).isZero();
    }
}
