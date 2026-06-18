package presentacio;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * Constructor de files compartit pels diàlegs de llibre nou i d'edició.
 * Cada crida {@code addX} afegeix un parell (etiqueta, control) a un
 * {@link JPanel} amfitrió configurat amb {@link GridLayout}.
 *
 * NOTA: PanellDetallsLlibre i DialegDesarLlibres encara fan servir els seus
 * propis mètodes privats addFieldEntry/addComboEntry/addCheckEntry en lloc
 * d'aquesta classe. Migrar-los a usar ConstructorGraellaFormulari (més un
 * registre per a l'enllaç de camps) eliminaria la duplicació rastrejada a
 * les entrades [un] de todo1.txt per a aquestes dues classes.
 */
public final class ConstructorGraellaFormulari {
    private final JPanel host;

    public ConstructorGraellaFormulari(JPanel host) { this.host = host; }

    public static ConstructorGraellaFormulari columnsOf(JPanel host) { return new ConstructorGraellaFormulari(host); }

    public ConstructorGraellaFormulari afegirField(String label, JTextField field) {
        host.add(new JLabel(label));
        host.add(field);
        return this;
    }

    public ConstructorGraellaFormulari afegirCombo(String label, JComboBox<?> combo) {
        host.add(new JLabel(label));
        host.add(combo);
        return this;
    }

    public ConstructorGraellaFormulari afegirCheck(String label, JCheckBox check) {
        host.add(new JLabel(label));
        host.add(check);
        return this;
    }

    public ConstructorGraellaFormulari afegirRaw(String label, JComponent comp) {
        host.add(new JLabel(label));
        host.add(comp);
        return this;
    }
}
