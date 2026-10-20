package presentacio.formularis;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Constructor de files compartit pels diàlegs de llibre nou i d'edició.
 *
 * <p>Cada crida {@code addX} afegeix un parell (etiqueta, control) a un
 * {@link JPanel} amfitrió configurat amb {@link GridBagLayout}.
 *
 * <p>Per què no {@link java.awt.GridLayout}: un {@code GridLayout} dona la
 * mateixa mida a totes les cel·les, de manera que en口红 el diàleg tots
 * els camps d'una línia creixen a la vegada i es converteixen en camps
 * gegants. Amb {@code GridBagLayout} el control conserva la seva alçada
 * preferida i només creix en horitzontal; l'única peça que pot absorbir
 * alçada extra és la zona de text multilínia.
 *
 * <p>Reutilitzar el mateix constructor entre "llibre nou" i "editar" evita
 * tenir dues disposicions que divergeixen, i valida que el canvi de
 * GridLayout a GridBagLayout es_notes necessari una sola vegada.
 */
public final class ConstructorGraellaFormulari {

    /** Amplada mínima de la columna de l'etiqueta, en píxels. */
    private static final int MIN_ETIQUETA = 120;

    private final JPanel host;
    private final GridBagConstraints c;

    private int fila = 0;

    public ConstructorGraellaFormulari(JPanel host) {
        this.host = host;
        host.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
    }

    public static ConstructorGraellaFormulari columnsOf(JPanel host) {
        return new ConstructorGraellaFormulari(host);
    }

    /** Etiqueta + camp d'una línia. El camp s'estén horitzontalment. */
    public ConstructorGraellaFormulari afegirField(String label, JTextField field) {
        return afegirRaw(label, field);
    }

    /** Etiqueta + desplegable. */
    public ConstructorGraellaFormulari afegirCombo(String label, JComboBox<?> combo) {
        return afegirRaw(label, combo);
    }

    /** Etiqueta + casella. */
    public ConstructorGraellaFormulari afegirCheck(String label, JCheckBox check) {
        return afegirRaw(label, check);
    }

    /**
     * Etiqueta + component.
     *
     * <p>El control no s'estén verticalment: conserva la seva alçada
     * preferida encara que el diàleg creixi.
     */
    public ConstructorGraellaFormulari afegirRaw(String label, JComponent comp) {
        JLabel lbl = etiqueta(label);
        c.gridx = 0; c.gridy = fila;
        c.weightx = 0; c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 8);
        host.add(lbl, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 4);
        host.add(comp, c);
        fila++;
        return this;
    }

    /**
     * Component que ocupa tota l'amplada i pot absorbir l'alçada extra.
     *
     * <p>És el que s'usa per a la descripció i les notes: són les úniques
     * zones on tenir més espai és útil.
     */
    public ConstructorGraellaFormulari afegirArea(String label, JComponent area) {
        c.gridx = 0; c.gridy = fila;
        c.gridwidth = 2;
        c.weightx = 1; c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(4, 4, 4, 4);
        if (label != null && !label.isBlank()) {
            JPanel cap = new JPanel(new java.awt.BorderLayout(0, 2));
            cap.setOpaque(false);
            cap.add(etiqueta(label), java.awt.BorderLayout.NORTH);
            cap.add(area, java.awt.BorderLayout.CENTER);
            host.add(cap, c);
        } else {
            host.add(area, c);
        }
        c.gridwidth = 1;
        fila++;
        return this;
    }

    /** Separador amb títol. */
    public ConstructorGraellaFormulari separador(String titol) {
        JLabel lbl = new JLabel(titol);
        lbl.setFont(herramienta.ui.UITheme.fontBold());
        lbl.setForeground(herramienta.ui.UITheme.palette().textMuted());
        c.gridx = 0; c.gridy = fila;
        c.gridwidth = 2;
        c.weightx = 1; c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(12, 4, 2, 4);
        host.add(lbl, c);
        c.gridwidth = 1;
        fila++;
        return this;
    }

    /** Component a tota l'amplada sense etiqueta. */
    public ConstructorGraellaFormulari componentPle(Component comp) {
        c.gridx = 0; c.gridy = fila;
        c.gridwidth = 2;
        c.weightx = 1; c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 4);
        host.add(comp, c);
        c.gridwidth = 1;
        fila++;
        return this;
    }

    private JLabel etiqueta(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(herramienta.ui.UITheme.fontBase());
        lbl.setForeground(herramienta.ui.UITheme.palette().textMid());
        lbl.setPreferredSize(new java.awt.Dimension(MIN_ETIQUETA, lbl.getPreferredSize().height));
        lbl.setMinimumSize(new java.awt.Dimension(MIN_ETIQUETA, lbl.getPreferredSize().height));
        return lbl;
    }
}
