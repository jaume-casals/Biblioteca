package presentacio.config;

import herramienta.Config;
import herramienta.DbConfig;
import herramienta.FilterConfig;
import herramienta.I18n;
import herramienta.UITheme;
import herramienta.UiConfig;
import presentacio.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import static herramienta.I18n.t;

/**
 * Save / cancel button bar. Pulls field values from the tagged input
 * children of the section panels and persists them through
 * {@link Config} in a single batch.
 */
public final class ConfiguracioButtonBar {
    private ConfiguracioButtonBar() {}

    public static JPanel build(JDialog owner,
                               JComboBox<String> cmbType,
                               JTextField txtHost,
                               JTextField txtUser,
                               JPasswordField txtPass,
                               JTextField txtImgDir,
                               JComboBox<String> cmbTheme,
                               JComboBox<String> cmbFont,
                               JComboBox<String> cmbCurrency,
                               JComboBox<String> cmbLang,
                               JTextField txtDefVal,
                               ConfiguracioDialogListener listener) {

        JPanel panel = new JPanel();
        panel.setBackground(UITheme.palette().bgPanel());
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        UITheme.Theme[] themeValues = UITheme.Theme.values();
        String[] fontSizeKeys = {"small", "medium", "large"};
        String[] langKeys = {"ca", "es", "en"};

        JButton btnGuardar = new JButton(t("btn_save"));
        UIComponents.styleAccentButton(btnGuardar);
        btnGuardar.addActionListener(e -> {
            boolean external = cmbType.getSelectedIndex() == 1;
            String prevDbType = Config.getDbType();
            if (external) {
                String host = txtHost.getText().trim();
                String user = txtUser.getText().trim();
                if (host.isEmpty() || user.isEmpty()) {
                    JOptionPane.showMessageDialog(owner,
                        t("dlg_db_validation"),
                        t("dlg_error_title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            Config.withBatch(() -> {
                if (external) {
                    DbConfig.setHost(txtHost.getText().trim());
                    DbConfig.setUser(txtUser.getText().trim());
                    DbConfig.setPassword(new String(txtPass.getPassword()));
                }
                DbConfig.setType(external ? "mariadb" : "h2");
                String imgDir = txtImgDir.getText().trim();
                if (!imgDir.isEmpty()) FilterConfig.setDefaultImgDir(imgDir);
                UITheme.Theme selTheme = themeValues[Math.max(0, cmbTheme.getSelectedIndex())];
                UITheme.setTheme(selTheme);
                UiConfig.setTheme(selTheme);
                UiConfig.setFontSize(fontSizeKeys[Math.max(0, cmbFont.getSelectedIndex())]);
                UiConfig.setCurrency((String) cmbCurrency.getSelectedItem());
                UiConfig.setLang(langKeys[Math.max(0, cmbLang.getSelectedIndex())]);
                I18n.applySwingOptionPane();
                try { UiConfig.setDefaultValoracio(Double.parseDouble(txtDefVal.getText().trim())); }
                catch (NumberFormatException ignored) {}
            });
            if (listener != null) listener.onThemeChange();
            String newDbType = external ? "mariadb" : "h2";
            boolean dbTypeChanged = !newDbType.equals(prevDbType);
            if (dbTypeChanged) {
                int restart = JOptionPane.showConfirmDialog(owner,
                    t("dlg_db_restart_msg"),
                    t("dlg_db_restart_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(owner,
                    t("dlg_config_saved"),
                    t("dlg_config_saved_title"), JOptionPane.INFORMATION_MESSAGE);
            }
            owner.dispose();
        });

        JButton btnCancel = new JButton(t("btn_cancel"));
        UIComponents.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> owner.dispose());

        gl.setHorizontalGroup(gl.createSequentialGroup()
            .addComponent(btnGuardar, GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
            .addComponent(btnCancel, GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
        );

        gl.setVerticalGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(btnGuardar, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
            .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
        );

        return panel;
    }
}
