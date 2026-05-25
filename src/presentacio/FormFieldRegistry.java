package presentacio;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

/** Maps logical field names to form components. */
public class FormFieldRegistry {

    private final Map<String, JComponent> fields = new HashMap<>();

    public void register(String name, JComponent component) {
        fields.put(name, component);
    }

    public JComponent get(String name) {
        return fields.get(name);
    }

    public void linkLabel(JLabel label, JComponent component) {
        label.setLabelFor(component);
    }
}
