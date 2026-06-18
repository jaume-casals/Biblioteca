package presentacio.detalles.vista;

import java.util.List;
import java.util.function.ToIntFunction;
import javax.swing.JComboBox;

/**
 * Lògica compartida de recàrrega de combos per a diàlegs de selecció
 * d'entitats (prestatgeries, etiquetes, etc.). Preserva l'element
 * seleccionat prèviament per id després de recarregar el model.
 */
public final class UtilitatsComboEntitat {

    private UtilitatsComboEntitat() {}

    /**
     * Recarrega un JComboBox des d'una llista d'entitats nova, restaurant
     * l'element seleccionat prèviament pel seu id.
     *
     * @param <E>          tipus d'entitat mostrada al combo
     * @param combo        el combo a recarregar
     * @param items        la llista completa d'entitats (nova des del domini)
     * @param idExtractor  funció que retorna l'id numèric de l'entitat
     */
    @SuppressWarnings("unchecked")
    public static <E> void reloadCombo(JComboBox<E> combo, List<E> items, ToIntFunction<E> idExtractor) {
        int anteriorId = -1;
        E prev = (E) combo.getSelectedItem();
        if (prev != null) anteriorId = idExtractor.applyAsInt(prev);
        combo.removeAllItems();
        for (E item : items) combo.addItem(item);
        if (anteriorId >= 0) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (idExtractor.applyAsInt(combo.getItemAt(i)) == anteriorId) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}