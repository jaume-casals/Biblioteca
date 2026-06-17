package presentacio.config;

import herramienta.Configuracio;
import herramienta.UITheme;
import presentacio.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static herramienta.I18n.t;

/** Language section: UI language picker. */
public final class SeccioIdiomaConfiguracio {
    private SeccioIdiomaConfiguracio() {}

    public static JPanel build(JDialog owner) {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.palette().bgPanel());
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lblLang = new JLabel(t("lbl_language_setting"));
        UIComponents.styleLabel(lblLang);
        String[] langLabels = {t("opt_lang_ca"), t("opt_lang_es"), t("opt_lang_en")};
        String[] langKeys = {"ca", "es", "en"};
        JComboBox<String> cmbLang = new JComboBox<>(langLabels);
        String curLang = Configuracio.obtenirLang();
        for (int i = 0; i < langKeys.length; i++)
            if (langKeys[i].equals(curLang)) { cmbLang.setSelectedIndex(i); break; }
        cmbLang.setFont(UITheme.fontBase());
        cmbLang.putClientProperty("id", "cmbLang");

        gl.setHorizontalGroup(gl.createSequentialGroup()
            .addComponent(lblLang)
            .addComponent(cmbLang, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
        );

        gl.setVerticalGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(lblLang).addComponent(cmbLang)
        );

        return panel;
    }

    public static void reloadFromConfig(JPanel root) {
        JComponent jc = SeccionsConfiguracio.cercarById(root, "cmbLang");
        if (jc instanceof JComboBox) {
            String lang = Configuracio.obtenirLang();
            ((JComboBox<?>) jc).setSelectedIndex("es".equals(lang) ? 1 : "en".equals(lang) ? 2 : 0);
        }
    }
}
