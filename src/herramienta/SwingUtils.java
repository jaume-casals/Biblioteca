package herramienta;

import java.util.List;
import java.util.function.Function;
import javax.swing.JComboBox;

public final class SwingUtils {

    private SwingUtils() {}

    public static <T> void reloadComboPreserveSelection(JComboBox<T> combo, List<T> items, Function<T, Integer> idExtractor) {
        @SuppressWarnings("unchecked")
        T prev = (T) combo.getSelectedItem();
        combo.removeAllItems();
        for (T item : items) combo.addItem(item);
        if (prev != null) {
            int prevId = idExtractor.apply(prev);
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (idExtractor.apply(combo.getItemAt(i)) == prevId) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}