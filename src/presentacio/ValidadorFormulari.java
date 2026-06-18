package presentacio;



import presentacio.UIComponents;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;

import herramienta.UITheme;

/** Feedback visual de validació per a camps de formulari. */
public final class ValidadorFormulari {

    private static final Border INVALID_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.RED),
        BorderFactory.createEmptyBorder(3, 7, 3, 7)
    );

    private ValidadorFormulari() {}

    public static void validarField(JTextField field, boolean valid) {
        if (valid) {
            UIComponents.styleField(field);
        } else {
            field.setBorder(INVALID_BORDER);
        }
    }
}
