package presentacio.util;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * Cablejat de dreceres de teclat extret de {@link MainFrameControl}.
 * Usa {@link #bind} per registrar una combinació de tecles amb una
 * acció sense arguments.
 */
public final class DreceresTeclat {
    private DreceresTeclat() {}

    public static void bind(JRootPane root, KeyStroke ks, String name, Runnable action) {
        bind(root, JComponent.WHEN_IN_FOCUSED_WINDOW, ks, name, action);
    }

    public static void bind(JComponent c, int when, KeyStroke ks, String name, Runnable action) {
        InputMap im = c.getInputMap(when);
        ActionMap am = c.getActionMap();
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }
}
