package herramienta.ui;

import java.util.List;
import java.util.function.Function;
import javax.swing.JComboBox;

public final class UtilitatsSwing {

    private UtilitatsSwing() {}

    public static <T> void reloadComboPreserveSelection(JComboBox<T> combo, List<T> items, Function<T, Integer> idExtractor) {
        Object prevObj = combo.getSelectedItem();
        Class<?> prevClass = prevObj != null ? prevObj.getClass() : null;
        combo.removeAllItems();
        for (T item : items) combo.addItem(item);
        if (prevObj != null && !items.isEmpty()
                && items.get(0).getClass().equals(prevClass)) {
            @SuppressWarnings("unchecked")
            T prev = (T) prevObj;
            int anteriorId = idExtractor.apply(prev);
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (idExtractor.apply(combo.getItemAt(i)) == anteriorId) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}