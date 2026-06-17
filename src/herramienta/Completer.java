package herramienta;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.util.List;

/** Common entry point for autocompletion on text fields and combo boxes. */
public final class Completer {
    private Completer() {}

    public static void attach(JComboBox<String> combo) { AutoCompletion.enable(combo); }
    public static FieldAutoComplete.Adjunts attach(JTextField field, List<String> suggestions) {
        return FieldAutoComplete.adjuntarReturning(field, suggestions);
    }
}
