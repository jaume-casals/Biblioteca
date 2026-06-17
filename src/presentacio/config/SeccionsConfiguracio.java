package presentacio.config;

import java.awt.Container;
import javax.swing.JComponent;

/**
 * Shared utilities for {@code presentacio.config.*} sections.
 * Each section panel tags its input children with a {@code ClientProperty
 * ("id", ...)} so the dialog can look them up by name in
 * {@link #reloadFromConfig(Container, String)}.
 */
public final class SeccionsConfiguracio {
    private SeccionsConfiguracio() {}

    /** Locate a tagged child by id. Recurses into nested containers. */
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
