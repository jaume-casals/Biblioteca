package presentacio;

import presentacio.config.ConfiguracioAppearanceSection;
import presentacio.config.ConfiguracioButtonBar;
import presentacio.config.ConfiguracioDataSection;
import presentacio.config.ConfiguracioDbSection;
import presentacio.config.ConfiguracioDialogListener;
import presentacio.config.ConfiguracioImagesSection;
import presentacio.config.ConfiguracioLanguageSection;
import presentacio.config.ConfigSections;

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
        if (cd == null) throw new IllegalArgumentException("ConfiguracioDialog requires non-null cd");
        this.cd = cd;
        this.listener = listener;

        JPanel dbSection        = ConfiguracioDbSection.build(this);
        JPanel imgSection       = ConfiguracioImagesSection.build(this);
        JPanel appearanceSection = ConfiguracioAppearanceSection.build(this);
        JPanel languageSection  = ConfiguracioLanguageSection.build(this);
        JPanel dataSection      = ConfiguracioDataSection.build(this, this.cd, this.listener);
        JPanel buttonBar = ConfiguracioButtonBar.build(this,
            (JComboBox<String>) ConfigSections.findById(dbSection, "cmbType"),
            (JTextField) ConfigSections.findById(dbSection, "txtHost"),
            (JTextField) ConfigSections.findById(dbSection, "txtUser"),
            (JPasswordField) ConfigSections.findById(dbSection, "txtPass"),
            (JTextField) ConfigSections.findById(imgSection, "txtImgDir"),
            (JComboBox<String>) ConfigSections.findById(appearanceSection, "cmbTheme"),
            (JComboBox<String>) ConfigSections.findById(appearanceSection, "cmbFont"),
            (JComboBox<String>) ConfigSections.findById(appearanceSection, "cmbCurrency"),
            (JComboBox<String>) ConfigSections.findById(languageSection, "cmbLang"),
            (JTextField) ConfigSections.findById(appearanceSection, "txtDefVal"),
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
}
