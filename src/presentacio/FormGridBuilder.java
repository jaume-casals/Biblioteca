package presentacio;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * Shared row-builder for the new-book and edit-book dialogs. Each {@code addX} call appends a
 * (label, control) pair to a host {@link JPanel} configured with {@link GridLayout}.
 *
 * NOTE: DetallesLlibrePanel and GuardarLlibresDialogo still use their own private
 * addFieldEntry/addComboEntry/addCheckEntry methods instead of this class. Migrating them
 * to use FormGridBuilder (plus a registry for field binding) would eliminate the duplication
 * tracked in todo1.txt [un] items for those two classes.
 */
public final class FormGridBuilder {
    private final JPanel host;

    public FormGridBuilder(JPanel host) { this.host = host; }

    public static FormGridBuilder columnsOf(JPanel host) { return new FormGridBuilder(host); }

    public FormGridBuilder addField(String label, JTextField field) {
        host.add(new JLabel(label));
        host.add(field);
        return this;
    }

    public FormGridBuilder addCombo(String label, JComboBox<?> combo) {
        host.add(new JLabel(label));
        host.add(combo);
        return this;
    }

    public FormGridBuilder addCheck(String label, JCheckBox check) {
        host.add(new JLabel(label));
        host.add(check);
        return this;
    }

    public FormGridBuilder addRaw(String label, JComponent comp) {
        host.add(new JLabel(label));
        host.add(comp);
        return this;
    }
}
