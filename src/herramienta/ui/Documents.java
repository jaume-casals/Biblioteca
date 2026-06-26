package herramienta.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class Documents {

    private Documents() {}

    public static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }
}
