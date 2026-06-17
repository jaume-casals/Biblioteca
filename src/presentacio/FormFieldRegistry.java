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
 * Declarative registry for the filter-drawer form.
 *
 * <p>The drawer's 32 hand-declared components (text fields, checkboxes,
 * combo boxes, buttons) are described by a list of {@link Field} specs.
 * {@link #build()} instantiates the right {@link JComponent} subclass
 * for each spec and stores it in a name -> component map. Public
 * getters on {@link FilterDrawerPanel} continue to expose the same
 * component instances (e.g. {@code getTextISBN()}) so all 217 external
 * call sites keep working unchanged.
 */
public class FormFieldRegistry {

    public enum Tipus { TEXT, CHECK, COMBO, BUTTON }

    /**
     * Declarative spec for a single field.
     *
     * @param key         logical name used by the registry (e.g. "textISBN")
     * @param labelKey    i18n key for the field's label
     * @param kind        component kind (text / check / combo / button)
     * @param tooltipKey  i18n key for the tooltip, or {@code null}
     * @param width       preferred width in characters (text/combo only)
     * @param defaultText initial text (text fields only)
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

    /** Append a spec to the registry. Order is preserved. */
    public FormFieldRegistry add(Camp f) {
        specs.add(f);
        return this;
    }

    /**
     * Instantiate one {@link JComponent} per spec and register it under
     * {@link Field#key()}. Subsequent {@link #get(String)} calls return
     * the live component.
     *
     * <p>Each spec is responsible for one component; the per-component
     * styling (font, palette colour, tool tip) is applied in the panel
     * once the components are wired into the layout — this method only
     * creates instances and sets the bare-minimum defaults (text,
     * tool tip text).
     */
    public FormFieldRegistry build() {
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
     * Raw-typed accessor kept for the 60+ existing callers that
     * declared {@code JComboBox<String>} or {@code JComboBox<Object>}
     * locally. A typed variant would require per-component knowledge
     * (T from the spec) — not worth the migration given the
     * existing call sites work as-is. Documented in the tot.txt
     * LOW finding; kept raw for backward compatibility.
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
     * Legacy API for the 25-line skeleton callers that wired
     * components by hand before the declarative spec existed.
     * Currently only {@code DetallesLlibrePanel.addFieldEntry}
     * still uses this (line 295 of that file); the registry-based
     * spec system is the supported path. Marked {@link Deprecated}
     * for removal once the last external caller migrates.
     */
    @Deprecated
    public void register(String name, JComponent component) {
        components.put(name, component);
    }

    public void linkLabel(JLabel label, JComponent component) {
        label.setLabelFor(component);
    }
}
