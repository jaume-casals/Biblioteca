package presentacio.config;

import herramienta.Config;
import herramienta.UITheme;
import presentacio.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static herramienta.I18n.t;

/** Appearance section: theme, font size, currency symbol, default rating. */
public final class ConfiguracioAppearanceSection {
    private ConfiguracioAppearanceSection() {}

    public static JPanel build(JDialog owner) {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.BG_PANEL);
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lblSeccio = new JLabel(t("lbl_appearance"));
        lblSeccio.setFont(UITheme.fontBold());
        lblSeccio.setForeground(UITheme.ACCENT);

        JLabel lblTheme = new JLabel(t("lbl_theme"));
        UIComponents.styleLabel(lblTheme);
        UITheme.Theme[] themeValues = UITheme.Theme.values();
        String[] themeLabels = new String[themeValues.length];
        for (int i = 0; i < themeValues.length; i++) themeLabels[i] = themeValues[i].displayName();
        JComboBox<String> cmbTheme = new JComboBox<>(themeLabels);
        UITheme.Theme curTheme = Config.getTheme();
        for (int i = 0; i < themeValues.length; i++)
            if (themeValues[i] == curTheme) { cmbTheme.setSelectedIndex(i); break; }
        cmbTheme.setFont(UITheme.fontBase());
        cmbTheme.putClientProperty("id", "cmbTheme");

        JLabel lblFont = new JLabel(t("lbl_font_size"));
        UIComponents.styleLabel(lblFont);
        String[] fontSizeLabels = {t("opt_small"), t("opt_medium"), t("opt_large")};
        String[] fontSizeKeys = {"small", "medium", "large"};
        JComboBox<String> cmbFont = new JComboBox<>(fontSizeLabels);
        String curSize = Config.getFontSize();
        for (int i = 0; i < fontSizeKeys.length; i++)
            if (fontSizeKeys[i].equals(curSize)) { cmbFont.setSelectedIndex(i); break; }
        cmbFont.setFont(UITheme.fontBase());
        cmbFont.putClientProperty("id", "cmbFont");

        JLabel lblCurrency = new JLabel(t("lbl_currency_symbol"));
        UIComponents.styleLabel(lblCurrency);
        String[] currencySymbols = {"€", "$", "£", "¥", "CHF"};
        JComboBox<String> cmbCurrency = new JComboBox<>(currencySymbols);
        cmbCurrency.setFont(UITheme.fontBase());
        String curCurrency = Config.getCurrencySymbol();
        for (int i = 0; i < currencySymbols.length; i++)
            if (currencySymbols[i].equals(curCurrency)) { cmbCurrency.setSelectedIndex(i); break; }
        cmbCurrency.putClientProperty("id", "cmbCurrency");

        JLabel lblDefVal = new JLabel(t("lbl_default_rating"));
        UIComponents.styleLabel(lblDefVal);
        JTextField txtDefVal = new JTextField(String.valueOf(Config.getDefaultValoracio()));
        UIComponents.styleField(txtDefVal);
        txtDefVal.putClientProperty("id", "txtDefVal");

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
        JComponent jc;
        jc = ConfigSections.findById(root, "cmbTheme");
        if (jc instanceof JComboBox) ((JComboBox<?>) jc).setSelectedIndex(UITheme.getTheme().ordinal());
        jc = ConfigSections.findById(root, "cmbFont");
        if (jc instanceof JComboBox) {
            String fs = Config.getFontSize();
            ((JComboBox<?>) jc).setSelectedIndex("small".equals(fs) ? 0 : "large".equals(fs) ? 2 : 1);
        }
        jc = ConfigSections.findById(root, "cmbCurrency");
        if (jc instanceof JComboBox) ((JComboBox<?>) jc).setSelectedItem(Config.getCurrencySymbol());
        jc = ConfigSections.findById(root, "txtDefVal");
        if (jc instanceof JTextField) ((JTextField) jc).setText(String.valueOf(Config.getDefaultValoracio()));
    }
}
