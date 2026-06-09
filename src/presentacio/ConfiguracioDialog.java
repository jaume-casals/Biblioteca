package presentacio;

import presentacio.config.ConfiguracioAppearanceSection;
import presentacio.config.ConfiguracioButtonBar;
import presentacio.config.ConfiguracioDataSection;
import presentacio.config.ConfiguracioDbSection;
import presentacio.config.ConfiguracioDialogListener;
import presentacio.config.ConfiguracioImagesSection;
import presentacio.config.ConfiguracioLanguageSection;

import java.awt.Frame;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import herramienta.Config;
import interficie.BibliotecaWriter;

/**
 * Settings dialog.  Each visual section lives in its own class under
 * {@code presentacio.config.*}; this class wires them into a single
 * vertical column and exposes the public ctor / {@link #reloadFromConfig}
 * API that callers depend on.
 */
public class ConfiguracioDialog extends JDialog {

    private final BibliotecaWriter cd;
    private final ConfiguracioDialogListener listener;

    public ConfiguracioDialog(Frame parent) { this(parent, null, null); }

    public ConfiguracioDialog(Frame parent, ConfiguracioDialogListener listener) {
        this(parent, listener, null);
    }

    public ConfiguracioDialog(Frame parent, ConfiguracioDialogListener listener, BibliotecaWriter cd) {
        super(parent, herramienta.I18n.t("modal_settings"), true);
        this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
        this.listener = listener;

        JPanel dbSection        = ConfiguracioDbSection.build(this);
        JPanel imgSection       = ConfiguracioImagesSection.build(this);
        JPanel appearanceSection = ConfiguracioAppearanceSection.build(this);
        JPanel languageSection  = ConfiguracioLanguageSection.build(this);
        JPanel dataSection      = ConfiguracioDataSection.build(this, this.cd, this.listener);
        JPanel buttonBar = ConfiguracioButtonBar.build(this,
            (JComboBox<String>) findComponentByClientProperty(dbSection, "cmbType"),
            (JTextField) findComponentByClientProperty(dbSection, "txtHost"),
            (JTextField) findComponentByClientProperty(dbSection, "txtUser"),
            (JPasswordField) findComponentByClientProperty(dbSection, "txtPass"),
            (JTextField) findComponentByClientProperty(imgSection, "txtImgDir"),
            (JComboBox<String>) findComponentByClientProperty(appearanceSection, "cmbTheme"),
            (JComboBox<String>) findComponentByClientProperty(appearanceSection, "cmbFont"),
            (JComboBox<String>) findComponentByClientProperty(appearanceSection, "cmbCurrency"),
            (JComboBox<String>) findComponentByClientProperty(languageSection, "cmbLang"),
            (JTextField) findComponentByClientProperty(appearanceSection, "txtDefVal"),
            this.listener);

        JPanel content = new JPanel();
        content.setBackground(herramienta.UITheme.BG_PANEL);
        GroupLayout layout = new GroupLayout(content);
        content.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(dbSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(imgSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(appearanceSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(languageSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(dataSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(dbSection)
            .addComponent(imgSection)
            .addComponent(appearanceSection)
            .addComponent(languageSection)
            .addComponent(dataSection)
            .addComponent(buttonBar)
        );

        setContentPane(content);
        setResizable(true);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(parent);

        getRootPane().registerKeyboardAction(
            e -> dispose(),
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JPanel.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Refresh form fields from {@link Config} (e.g. after external/API changes). */
    public void reloadFromConfig() {
        Config.reload();
        JPanel root = (JPanel) getContentPane();
        ConfiguracioDbSection.reloadFromConfig(root);
        ConfiguracioImagesSection.reloadFromConfig(root);
        ConfiguracioAppearanceSection.reloadFromConfig(root);
        ConfiguracioLanguageSection.reloadFromConfig(root);
        ConfiguracioDataSection.reloadFromConfig(root);
    }

    private static javax.swing.JComponent findComponentByClientProperty(java.awt.Container panel, String id) {
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof javax.swing.JComponent jc) {
                if (id.equals(jc.getClientProperty("id"))) return jc;
            }
            if (c instanceof java.awt.Container cont) {
                javax.swing.JComponent found = findComponentByClientProperty(cont, id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
