package presentacio.config;

import herramienta.config.Configuracio;
import presentacio.util.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static herramienta.i18n.I18n.t;

/** Language section: UI language picker. */
public final class SeccioIdiomaConfiguracio {
    private SeccioIdiomaConfiguracio() {}

    public static JPanel build(JDialog owner) {
        UIComponents.SectionPanel section = UIComponents.sectionPanel();
        JPanel panel = section.panel();
        GroupLayout gl = section.layout();

        JLabel lblLang = UIComponents.label("lbl_language_setting");
        String[] langLabels = {t("opt_lang_ca"), t("opt_lang_es"), t("opt_lang_en")};
        JComboBox<String> cmbLang = UIComponents.combo("cmbLang", langLabels,
            ConfiguracioConstants.LANG_KEYS, Configuracio.obtenirLang());

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
