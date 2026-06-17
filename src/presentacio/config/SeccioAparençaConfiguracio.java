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
import javax.swing.JTextField;

import static herramienta.I18n.t;

/** Appearance section: theme, font size, currency symbol, default rating. */
public final class SeccioAparençaConfiguracio {
    private SeccioAparençaConfiguracio() {}

    public static JPanel build(JDialog owner) {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.palette().bgPanel());
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lblSeccio = new JLabel(t("lbl_appearance"));
        lblSeccio.setFont(UITheme.fontBold());
        lblSeccio.setForeground(UITheme.palette().accent());

        JLabel lblTheme = new JLabel(t("lbl_theme"));
        UIComponents.styleLabel(lblTheme);
        UITheme.Tema[] themeValues = UITheme.Tema.values();
        String[] themeLabels = new String[themeValues.length];
        for (int i = 0; i < themeValues.length; i++) themeLabels[i] = themeValues[i].displayName();
        JComboBox<String> cmbTheme = new JComboBox<>(themeLabels);
        UITheme.Tema curTheme = Configuracio.obtenirTheme();
        for (int i = 0; i < themeValues.length; i++)
            if (themeValues[i] == curTheme) { cmbTheme.setSelectedIndex(i); break; }
        cmbTheme.setFont(UITheme.fontBase());
        cmbTheme.putClientProperty("id", "cmbTheme");

        JLabel lblFont = new JLabel(t("lbl_font_size"));
        UIComponents.styleLabel(lblFont);
        String[] fontSizeLabels = {t("opt_small"), t("opt_medium"), t("opt_large")};
        String[] fontSizeKeys = {"small", "medium", "large"};
        JComboBox<String> cmbFont = new JComboBox<>(fontSizeLabels);
        String curSize = Configuracio.obtenirFontSize();
        for (int i = 0; i < fontSizeKeys.length; i++)
            if (fontSizeKeys[i].equals(curSize)) { cmbFont.setSelectedIndex(i); break; }
        cmbFont.setFont(UITheme.fontBase());
        cmbFont.putClientProperty("id", "cmbFont");

        JLabel lblCurrency = new JLabel(t("lbl_currency_symbol"));
        UIComponents.styleLabel(lblCurrency);
        String[] currencySymbols = {"€", "$", "£", "¥", "CHF"};
        JComboBox<String> cmbCurrency = new JComboBox<>(currencySymbols);
        cmbCurrency.setFont(UITheme.fontBase());
        String curCurrency = Configuracio.getCurrencySymbol();
        for (int i = 0; i < currencySymbols.length; i++)
            if (currencySymbols[i].equals(curCurrency)) { cmbCurrency.setSelectedIndex(i); break; }
        cmbCurrency.putClientProperty("id", "cmbCurrency");

        JLabel lblDefVal = new JLabel(t("lbl_default_rating"));
        UIComponents.styleLabel(lblDefVal);
        JTextField txtDefVal = new JTextField(String.valueOf(Configuracio.obtenirDefaultValoracio()));
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
        jc = SeccionsConfiguracio.cercarById(root, "cmbTheme");
        if (jc instanceof JComboBox) ((JComboBox<?>) jc).setSelectedIndex(UITheme.obtenirTheme().ordinal());
        jc = SeccionsConfiguracio.cercarById(root, "cmbFont");
        if (jc instanceof JComboBox) {
            String fs = Configuracio.obtenirFontSize();
            ((JComboBox<?>) jc).setSelectedIndex("small".equals(fs) ? 0 : "large".equals(fs) ? 2 : 1);
        }
        jc = SeccionsConfiguracio.cercarById(root, "cmbCurrency");
        if (jc instanceof JComboBox) ((JComboBox<?>) jc).setSelectedItem(Configuracio.getCurrencySymbol());
        jc = SeccionsConfiguracio.cercarById(root, "txtDefVal");
        if (jc instanceof JTextField) ((JTextField) jc).setText(String.valueOf(Configuracio.obtenirDefaultValoracio()));
    }
}
