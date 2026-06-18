package presentacio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Registre declaratiu per al formulari del calaix de filtre.
 *
 * <p>Els 32 components declarats a mà del calaix (camps de text,
 * caselles, combos i botons) es descriuen amb una llista d'especificacions
 * {@link Camp}. {@link #build()} instancia la subclasse {@link JComponent}
 * adient per a cada especificació i la desa en un mapa de nom ->
 * component. Els getters públics a {@link PanelCalaixFiltre} continuen
 * exposant les mateixes instàncies de component (p. ex. {@code getTextISBN()})
 * perquè els 217 punts de crida externs continuïn funcionant sense canvis.
 */
public class RegistreCampsFormulari {

    public enum Tipus { TEXT, CHECK, COMBO, BUTTON }

    /**
     * Especificació declarativa per a un sol camp.
     *
     * @param key         nom lògic usat pel registre (p. ex. "textISBN")
     * @param labelKey    clau i18n de l'etiqueta del camp
     * @param kind        tipus de component (text / check / combo / button)
     * @param tooltipKey  clau i18n del tooltip, o {@code null}
     * @param width       amplada preferida en caràcters (només text/combo)
     * @param defaultText text inicial (només camps de text)
     */
    public record Camp(
        String key,
        String labelKey,
        Tipus kind,
        String tooltipKey,
        int width,
        String defaultText
    ) {
        public static Camp text(String key, String labelKey, int width) {
            return new Camp(key, labelKey, Tipus.TEXT, null, width, "");
        }
        public static Camp text(String key, String labelKey, String tooltipKey, int width) {
            return new Camp(key, labelKey, Tipus.TEXT, tooltipKey, width, "");
        }
        public static Camp check(String key, String labelKey, String tooltipKey) {
            return new Camp(key, labelKey, Tipus.CHECK, tooltipKey, 0, "");
        }
        public static Camp combo(String key, String labelKey, String tooltipKey, int width) {
            return new Camp(key, labelKey, Tipus.COMBO, tooltipKey, width, "");
        }
        public static Camp button(String key, String labelKey, String tooltipKey) {
            return new Camp(key, labelKey, Tipus.BUTTON, tooltipKey, 0, "");
        }
    }

    private final List<Camp> specs = new ArrayList<>();
    private final Map<String, JComponent> components = new HashMap<>();
    private final Map<String, JLabel> labels = new HashMap<>();

    /** Afegeix una especificació al registre. Es preserva l'ordre. */
    public RegistreCampsFormulari add(Camp f) {
        specs.add(f);
        return this;
    }

    /**
     * Instancia un {@link JComponent} per especificació i el registra sota
     * {@link Camp#key()}. Les crides posteriors a {@link #get(String)}
     * retornen el component viu.
     *
     * <p>Cada especificació és responsable d'un component; l'estil
     * per-component (font, color de paleta, tool tip) s'aplica al panell
     * un cop els components s'han connectat al layout — aquest mètode
     * només crea instàncies i fixa els valors per defecte mínims
     * (text, text del tool tip).
     */
    public RegistreCampsFormulari build() {
        for (Camp f : specs) {
            JComponent c = switch (f.kind()) {
                case TEXT   -> {
                    JTextField tf = new JTextField(f.width());
                    tf.setText(f.defaultText());
                    yield tf;
                }
                case CHECK  -> f.labelKey() == null ? new JCheckBox() : new JCheckBox(herramienta.I18n.t(f.labelKey()));
                case COMBO  -> new JComboBox<>();
                case BUTTON -> f.labelKey() == null ? new JButton() : new JButton(herramienta.I18n.t(f.labelKey()));
            };
            if (f.tooltipKey() != null) {
                c.setToolTipText(herramienta.I18n.t(f.tooltipKey()));
            }
            components.put(f.key(), c);
            if (f.labelKey() != null && f.kind() != Tipus.CHECK && f.kind() != Tipus.BUTTON) {
                JLabel lbl = new JLabel(herramienta.I18n.t(f.labelKey()) + ":");
                labels.put(f.key(), lbl);
            }
        }
        return this;
    }

    public JComponent get(String key)            { return components.get(key); }
    public JTextField  textField(String key)     { return (JTextField)  components.get(key); }
    public JCheckBox   comprovarBox(String key)      { return (JCheckBox)   components.get(key); }
    /**
     * Accessor sense tipus conservat pels més de 60 callers existents
     * que declaraven {@code JComboBox<String>} o {@code JComboBox<Object>}
     * localment. Una variant tipada requeriria coneixement per-component
     * (T des de l'especificació) — no compensa la migració donat que
     * els call sites existents funcionen tal qual. Documentat a la
     * troballa LOW de tot.txt; es manté sense tipus per compatibilitat
     * enrere.
     */
    @SuppressWarnings("rawtypes")
    public JComboBox   comboBox(String key)      { return (JComboBox)   components.get(key); }
    public JButton     button(String key)        { return (JButton)     components.get(key); }
    public JLabel      label(String key)         { return labels.get(key); }
    public boolean has(String key)               { return components.containsKey(key); }

    public List<Camp> specs()                   { return Collections.unmodifiableList(specs); }

    public List<Camp> specsOfKind(Tipus k) {
        return specs.stream().filter(s -> s.kind() == k).collect(Collectors.toUnmodifiableList());
    }

    public Collection<JComponent> components() {
        return Collections.unmodifiableCollection(components.values());
    }

    /**
     * API legacy per als call sites esquelet de 25 línies que connectaven
     * components a mà abans que existís l'especificació declarativa.
     * Actualment només {@code PanellDetallsLlibre.addFieldEntry} encara
     * l'usa (línia 295 d'aquest fitxer); el sistema d'especificacions
     * basat en registre és el camí suportat. Marcat com a {@link Deprecated}
     * per eliminar-lo quan l'últim caller extern hagi migrat.
     */
    @Deprecated
    public void register(String name, JComponent component) {
        components.put(name, component);
    }

    public void linkLabel(JLabel label, JComponent component) {
        label.setLabelFor(component);
    }
}
