package presentacio;

import javax.swing.JButton;
import java.awt.Color;

/** Custom-painted button used by {@link ModeSelectorDialog}. Lifted from inline subclass. */
public class ModeButton extends JButton {
    public ModeButton(String text, Color fg, Color bg) {
        super(text);
        setForeground(fg);
        setBackground(bg);
        setFocusPainted(false);
        setBorderPainted(false);
    }
}
