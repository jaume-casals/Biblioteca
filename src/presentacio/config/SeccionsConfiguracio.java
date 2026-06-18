package presentacio.config;

import java.awt.Container;
import javax.swing.JComponent;

/**
 * Utilitats compartides per a les seccions de {@code presentacio.config.*}.
 * Cada panell de secció etiqueta els seus fills d'entrada amb un
 * {@code ClientProperty ("id", ...)} perquè el diàleg els pugui cercar
 * per nom a {@link #reloadFromConfig(Container, String)}.
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
}
