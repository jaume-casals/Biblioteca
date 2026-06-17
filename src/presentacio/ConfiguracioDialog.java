package presentacio;

import presentacio.config.SeccioAparençaConfiguracio;
import presentacio.config.BarraBotonsConfiguracio;
import presentacio.config.ConfiguracioDataSection;
import presentacio.config.SeccioDbConfiguracio;
import presentacio.config.ConfiguracioDialogListener;
import presentacio.config.SeccioImatgesConfiguracio;
import presentacio.config.SeccioIdiomaConfiguracio;
import presentacio.config.SeccionsConfiguracio;

import java.awt.Frame;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import herramienta.Configuracio;
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

    public ConfiguracioDialog(Frame parent, ConfiguracioDialogListener listener, BibliotecaWriter cd) {
        super(parent, herramienta.I18n.t("modal_settings"), true);
        if (cd == null) throw new IllegalArgumentException("ConfiguracioDialog requires non-null cd");
        this.cd = cd;
        this.listener = listener;

        JPanel dbSection        = SeccioDbConfiguracio.build(this);
        JPanel imgSection       = SeccioImatgesConfiguracio.build(this);
        JPanel appearanceSection = SeccioAparençaConfiguracio.build(this);
        JPanel languageSection  = SeccioIdiomaConfiguracio.build(this);
        JPanel dataSection      = ConfiguracioDataSection.build(this, this.cd, this.listener);
        JPanel buttonBar = BarraBotonsConfiguracio.build(this,
            (JComboBox<String>) SeccionsConfiguracio.cercarById(dbSection, "cmbType"),
            (JTextField) SeccionsConfiguracio.cercarById(dbSection, "txtHost"),
            (JTextField) SeccionsConfiguracio.cercarById(dbSection, "txtUser"),
            (JPasswordField) SeccionsConfiguracio.cercarById(dbSection, "txtPass"),
            (JTextField) SeccionsConfiguracio.cercarById(imgSection, "txtImgDir"),
            (JComboBox<String>) SeccionsConfiguracio.cercarById(appearanceSection, "cmbTheme"),
            (JComboBox<String>) SeccionsConfiguracio.cercarById(appearanceSection, "cmbFont"),
            (JComboBox<String>) SeccionsConfiguracio.cercarById(appearanceSection, "cmbCurrency"),
            (JComboBox<String>) SeccionsConfiguracio.cercarById(languageSection, "cmbLang"),
            (JTextField) SeccionsConfiguracio.cercarById(appearanceSection, "txtDefVal"),
            this.listener);

        JPanel content = new JPanel();
        content.setBackground(herramienta.UITheme.palette().bgPanel());
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
        Configuracio.reload();
        JPanel root = (JPanel) getContentPane();
        SeccioDbConfiguracio.reloadFromConfig(root);
        SeccioImatgesConfiguracio.reloadFromConfig(root);
        SeccioAparençaConfiguracio.reloadFromConfig(root);
        SeccioIdiomaConfiguracio.reloadFromConfig(root);
        ConfiguracioDataSection.reloadFromConfig(root);
    }
}
