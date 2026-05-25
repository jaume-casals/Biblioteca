package presentacio.detalles.vista;

import java.util.List;
import java.util.function.ToIntFunction;
import javax.swing.JComboBox;

/**
 * Shared combo-reload logic for entity selection dialogs (shelves, tags, etc.).
 * Preserves the previously selected item by id after reloading the model.
 */
public final class EntityComboUtils {

    private EntityComboUtils() {}

    /**
     * Reloads a JComboBox from a fresh entity list, restoring the previously
     * selected item by its id.
     *
     * @param <E>          entity type displayed in the combo
     * @param combo        the combo to reload
     * @param items        the full list of entities (fresh from domain)
     * @param idExtractor  function returning the entity's numeric id
     */
    @SuppressWarnings("unchecked")
    public static <E> void reloadCombo(JComboBox<E> combo, List<E> items, ToIntFunction<E> idExtractor) {
        int prevId = -1;
        E prev = (E) combo.getSelectedItem();
        if (prev != null) prevId = idExtractor.applyAsInt(prev);
        combo.removeAllItems();
        for (E item : items) combo.addItem(item);
        if (prevId >= 0) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (idExtractor.applyAsInt(combo.getItemAt(i)) == prevId) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}