package presentacio;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;

import herramienta.UITheme;

/** Visual validation feedback for form fields. */
public final class FormValidator {

    private static final Border INVALID_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.RED),
        BorderFactory.createEmptyBorder(3, 7, 3, 7)
    );

    private FormValidator() {}

    public static void validateField(JTextField field, boolean valid) {
        if (valid) {
            UITheme.styleField(field);
        } else {
            field.setBorder(INVALID_BORDER);
        }
    }
}
