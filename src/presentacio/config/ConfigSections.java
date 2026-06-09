package presentacio.config;

import java.awt.Container;
import javax.swing.JComponent;

/**
 * Shared utilities for {@code presentacio.config.*} sections.
 * Each section panel tags its input children with a {@code ClientProperty
 * ("id", ...)} so the dialog can look them up by name in
 * {@link #reloadFromConfig(Container, String)}.
 */
final class ConfigSections {
    private ConfigSections() {}

    /** Locate a tagged child by id. Recurses into nested containers. */
    static JComponent findById(Container root, String id) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof JComponent jc && id.equals(jc.getClientProperty("id"))) return jc;
            if (c instanceof Container cont) {
                JComponent found = findById(cont, id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
