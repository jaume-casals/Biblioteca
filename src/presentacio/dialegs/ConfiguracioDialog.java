package presentacio.dialegs;

import presentacio.config.BarraBotonsConfiguracio;
import presentacio.config.ConfiguracioDataSection;
import presentacio.config.ConfiguracioDialogListener;
import presentacio.config.ConfiguracioSeccio;
import presentacio.config.SeccioAparençaConfiguracio;
import presentacio.config.SeccioDbConfiguracio;
import presentacio.config.SeccioIdiomaConfiguracio;
import presentacio.config.SeccioImatgesConfiguracio;
import presentacio.config.SeccionsConfiguracio;

import java.awt.Frame;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import herramienta.config.Configuracio;
import persistencia.contract.EscritorBiblioteca;

/**
 * Diàleg de configuració. Cada secció visual viu a la seva pròpia classe
 * sota {@code presentacio.config.*}; aquesta classe les connecta en una
 * única columna vertical i exposa el ctor públic / API
 * {@link #reloadFromConfig} que els callers necessiten.
 */
public class ConfiguracioDialog extends JDialog {

    private final EscritorBiblioteca cd;
    private final ConfiguracioDialogListener listener;
    private final List<ConfiguracioSeccio> seccions;

    public ConfiguracioDialog(Frame parent, ConfiguracioDialogListener listener, EscritorBiblioteca cd) {
        super(parent, herramienta.i18n.I18n.t("modal_settings"), true);
        if (cd == null) throw new IllegalArgumentException("ConfiguracioDialog requires non-null cd");
        this.cd = cd;
        this.listener = listener;
        this.seccions = List.of(
            ConfiguracioSeccio.of(SeccioDbConfiguracio::build, SeccioDbConfiguracio::reloadFromConfig),
            ConfiguracioSeccio.of(SeccioImatgesConfiguracio::build, SeccioImatgesConfiguracio::reloadFromConfig),
            ConfiguracioSeccio.of(SeccioAparençaConfiguracio::build, SeccioAparençaConfiguracio::reloadFromConfig),
            ConfiguracioSeccio.of(SeccioIdiomaConfiguracio::build, SeccioIdiomaConfiguracio::reloadFromConfig),
            ConfiguracioSeccio.of(d -> ConfiguracioDataSection.build(d, this.cd, this.listener),
                ConfiguracioDataSection::reloadFromConfig)
        );

        JPanel dbSection = seccions.get(0).build(this);
        JPanel imgSection = seccions.get(1).build(this);
        JPanel appearanceSection = seccions.get(2).build(this);
        JPanel languageSection = seccions.get(3).build(this);
        JPanel dataSection = seccions.get(4).build(this);
        JPanel buttonBar = BarraBotonsConfiguracio.build(this,
            SeccionsConfiguracio.cercar(dbSection, "cmbType", JComboBox.class),
            SeccionsConfiguracio.cercar(dbSection, "txtHost", JTextField.class),
            SeccionsConfiguracio.cercar(dbSection, "txtUser", JTextField.class),
            SeccionsConfiguracio.cercar(dbSection, "txtPass", JPasswordField.class),
            SeccionsConfiguracio.cercar(imgSection, "txtImgDir", JTextField.class),
            SeccionsConfiguracio.cercar(appearanceSection, "cmbTheme", JComboBox.class),
            SeccionsConfiguracio.cercar(appearanceSection, "cmbFont", JComboBox.class),
            SeccionsConfiguracio.cercar(appearanceSection, "cmbCurrency", JComboBox.class),
            SeccionsConfiguracio.cercar(languageSection, "cmbLang", JComboBox.class),
            SeccionsConfiguracio.cercar(appearanceSection, "txtDefVal", JTextField.class),
            this.listener);

        JPanel content = new JPanel();
        content.setBackground(herramienta.ui.UITheme.palette().bgPanel());
        GroupLayout layout = new GroupLayout(content);
        content.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
        GroupLayout.ParallelGroup hGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        for (JPanel section : List.of(dbSection, imgSection, appearanceSection, languageSection, dataSection)) {
            hGroup.addComponent(section, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
            vGroup.addComponent(section);
        }
        hGroup.addComponent(buttonBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        vGroup.addComponent(buttonBar);
        layout.setHorizontalGroup(hGroup);
        layout.setVerticalGroup(vGroup);

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

    /** Refresca els camps del formulari des de {@link Configuracio} (p. ex. després de canvis externs o de l'API). */
    public void reloadFromConfig() {
        Configuracio.reload();
        JPanel root = (JPanel) getContentPane();
        for (ConfiguracioSeccio seccio : seccions) {
            seccio.reloadFromConfig(root);
        }
    }
}
