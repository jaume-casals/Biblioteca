package presentacio.formularis;



import presentacio.util.UIComponents;
import java.awt.Color;

import javax.swing.JTextField;

/** Feedback visual de validació per a camps de formulari. */
public final class ValidadorFormulari {

    private ValidadorFormulari() {}

    public static void validarField(JTextField field, boolean valid) {
        if (valid) {
            UIComponents.styleField(field);
        } else {
            field.setBorder(UIComponents.paddedLineBorder(Color.RED));
        }
    }
}
