package presentacio.config;

import herramienta.config.Configuracio;
import herramienta.ui.UITheme;
import presentacio.util.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static herramienta.i18n.I18n.t;

/** Secció d'aparença: tema, mida de lletra, símbol de moneda, valoració per defecte. */
public final class SeccioAparençaConfiguracio {
    private SeccioAparençaConfiguracio() {}

    public static JPanel build(JDialog owner) {
        UIComponents.SectionPanel section = UIComponents.sectionPanel();
        JPanel panel = section.panel();
        GroupLayout gl = section.layout();

        JLabel lblSeccio = UIComponents.sectionHeader("lbl_appearance");

        JLabel lblTheme = UIComponents.label("lbl_theme");
        UITheme.Tema[] themeValues = UITheme.Tema.values();
        String[] themeLabels = new String[themeValues.length];
        String[] themeKeys = new String[themeValues.length];
        for (int i = 0; i < themeValues.length; i++) {
            themeLabels[i] = themeValues[i].displayName();
            themeKeys[i] = themeValues[i].name();
        }
        JComboBox<String> cmbTheme = UIComponents.combo("cmbTheme", themeLabels, themeKeys,
            Configuracio.obtenirTheme().name());

        JLabel lblFont = UIComponents.label("lbl_font_size");
        String[] fontSizeLabels = {t("opt_small"), t("opt_medium"), t("opt_large")};
        JComboBox<String> cmbFont = UIComponents.combo("cmbFont", fontSizeLabels,
            ConfiguracioConstants.FONT_SIZE_KEYS, Configuracio.obtenirFontSize());

        JLabel lblCurrency = UIComponents.label("lbl_currency_symbol");
        String[] currencySymbols = {"€", "$", "£", "¥", "CHF"};
        JComboBox<String> cmbCurrency = UIComponents.combo("cmbCurrency", currencySymbols,
            currencySymbols, Configuracio.getCurrencySymbol());

        JLabel lblDefVal = UIComponents.label("lbl_default_rating");
        JTextField txtDefVal = UIComponents.field("txtDefVal",
            String.valueOf(Configuracio.obtenirDefaultValoracio()));

        JLabel lblDefValHint = new JLabel("(0.0 – 10.0)");
        UIComponents.styleLabel(lblDefValHint);

        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(lblSeccio)
            .addGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(lblTheme)
                    .addComponent(lblFont)
                    .addComponent(lblCurrency)
                    .addComponent(lblDefVal))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(cmbTheme, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbFont, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbCurrency, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                    .addGroup(gl.createSequentialGroup()
                        .addComponent(txtDefVal, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblDefValHint))))
        );

        gl.setVerticalGroup(gl.createSequentialGroup()
            .addComponent(lblSeccio)
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblTheme).addComponent(cmbTheme))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblFont).addComponent(cmbFont))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblCurrency).addComponent(cmbCurrency))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblDefVal).addComponent(txtDefVal).addComponent(lblDefValHint))
        );

        return panel;
    }

    public static void reloadFromConfig(JPanel root) {
        SeccionsConfiguracio.apply(root, "cmbTheme", JComboBox.class,
            c -> c.setSelectedIndex(Configuracio.obtenirTheme().ordinal()));
        SeccionsConfiguracio.apply(root, "cmbFont", JComboBox.class, c -> {
            String fs = Configuracio.obtenirFontSize();
            c.setSelectedIndex("small".equals(fs) ? 0 : "large".equals(fs) ? 2 : 1);
        });
        SeccionsConfiguracio.apply(root, "cmbCurrency", JComboBox.class,
            c -> c.setSelectedItem(Configuracio.getCurrencySymbol()));
        SeccionsConfiguracio.apply(root, "txtDefVal", JTextField.class,
            c -> c.setText(String.valueOf(Configuracio.obtenirDefaultValoracio())));
    }
}
