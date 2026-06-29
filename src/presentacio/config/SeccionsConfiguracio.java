package presentacio.config;

import java.awt.Container;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 * Utilitats compartides per a les seccions de {@code presentacio.config.*}.
 * Cada panell de secció etiqueta els seus fills d'entrada amb un
 * {@code ClientProperty ("id", ...)} perquè el diàleg els pugui cercar
 * per nom a {@link #cercarById(Container, String)}.
 */
public final class SeccionsConfiguracio {
    private SeccionsConfiguracio() {}

    /** Localitza un fill etiquetat per id. Recorre recursivament els contenidors niats. */
    public static JComponent cercarById(Container root, String id) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof JComponent jc && id.equals(jc.getClientProperty("id"))) return jc;
            if (c instanceof Container cont) {
                JComponent found = cercarById(cont, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Com {@link #cercarById} però retorna el fill ja convertit al tipus demanat (o {@code null} si no es troba / no coincideix). */
    public static <T extends JComponent> T cercar(Container root, String id, Class<T> type) {
        JComponent jc = cercarById(root, id);
        return type.isInstance(jc) ? type.cast(jc) : null;
    }

    /** Aplica {@code action} al fill identificat per {@code id}, convertit al tipus {@code type}. No-op si no es troba. */
    public static <T extends JComponent> void apply(Container root, String id, Class<T> type, Consumer<T> action) {
        T target = cercar(root, id, type);
        if (target != null) action.accept(target);
    }
}
