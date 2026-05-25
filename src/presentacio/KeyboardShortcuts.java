package presentacio;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * Keyboard shortcut wiring extracted from {@link MainFrameControl}.
 * Use {@link #bind} to register a key combination with a no-arg action.
 */
public final class KeyboardShortcuts {
    private KeyboardShortcuts() {}

    public static void bind(JRootPane root, KeyStroke ks, String name, Runnable action) {
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
        });
    }
}
