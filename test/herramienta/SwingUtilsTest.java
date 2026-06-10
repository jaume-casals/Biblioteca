package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static org.assertj.core.api.Assertions.*;

class SwingUtilsTest {

    private record Entry(int id, String name) {}

    @Test
    @DisplayName("reloadComboPreserveSelection: clears and re-adds items")
    void reloadReplacesItems() {
        JComboBox<Entry> combo = new JComboBox<>();
        combo.addItem(new Entry(1, "A"));
        combo.addItem(new Entry(2, "B"));

        SwingUtils.reloadComboPreserveSelection(combo,
            java.util.List.of(new Entry(3, "C"), new Entry(4, "D")),
            Entry::id);

        assertThat(combo.getItemCount()).isEqualTo(2);
        assertThat(combo.getItemAt(0).name()).isEqualTo("C");
        assertThat(combo.getItemAt(1).name()).isEqualTo("D");
    }

    @Test
    @DisplayName("reloadComboPreserveSelection: restores prior selection by id")
    void reloadPreservesSelection() {
        JComboBox<Entry> combo = new JComboBox<>();
        combo.addItem(new Entry(1, "A"));
        combo.addItem(new Entry(2, "B"));
        combo.setSelectedIndex(1); // selected = id 2

        // New list contains the prior id at index 0
        SwingUtils.reloadComboPreserveSelection(combo,
            java.util.List.of(new Entry(2, "B"), new Entry(3, "C"), new Entry(4, "D")),
            Entry::id);

        assertThat(combo.getSelectedIndex()).isZero();
        assertThat(combo.getItemAt(0).id()).isEqualTo(2);
    }

    @Test
    @DisplayName("reloadComboPreserveSelection: prior selection absent from new list → no selection")
    void reloadLosesSelection() {
        JComboBox<Entry> combo = new JComboBox<>();
        combo.addItem(new Entry(1, "A"));
        combo.setSelectedIndex(0);

        SwingUtils.reloadComboPreserveSelection(combo,
            java.util.List.of(new Entry(99, "X"), new Entry(100, "Y")),
            Entry::id);

        // No id 1 in new list; selection is reset (no-op)
        assertThat(combo.getSelectedItem()).isNull();
    }

    @Test
    @DisplayName("reloadComboPreserveSelection: empty new list, no prior selection")
    void reloadEmptyNoPrior() {
        JComboBox<Entry> combo = new JComboBox<>();
        SwingUtils.reloadComboPreserveSelection(combo, java.util.List.of(), Entry::id);
        assertThat(combo.getItemCount()).isZero();
    }
}
