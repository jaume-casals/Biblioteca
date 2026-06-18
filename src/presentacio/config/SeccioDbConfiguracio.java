package presentacio.config;

import herramienta.Configuracio;
import herramienta.UITheme;
import presentacio.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import static herramienta.I18n.t;

/** Secció de base de dades: tipus / host / usuari / contrasenya + botó de provar connexió. */
public final class SeccioDbConfiguracio {
    private SeccioDbConfiguracio() {}

    public static JPanel build(JDialog owner) {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.palette().bgPanel());
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lblSeccio = new JLabel(t("lbl_database"));
        lblSeccio.setFont(UITheme.fontBold());
        lblSeccio.setForeground(UITheme.palette().accent());

        JLabel lblTipus = new JLabel(t("lbl_type"));
        UIComponents.styleLabel(lblTipus);
        JComboBox<String> cmbType = new JComboBox<>(new String[]{
            t("opt_h2_full"), t("opt_mariadb_full")
        });
        cmbType.setSelectedIndex("h2".equals(Configuracio.obtenirDbType()) ? 0 : 1);
        cmbType.setFont(UITheme.fontBase());
        cmbType.putClientProperty("id", "cmbType");

        JLabel lblHost = new JLabel(t("lbl_server"));
        UIComponents.styleLabel(lblHost);
        JTextField txtHost = new JTextField(Configuracio.obtenirDbHost());
        UIComponents.styleField(txtHost);
        txtHost.putClientProperty("id", "txtHost");

        JLabel lblUser = new JLabel(t("lbl_user"));
        UIComponents.styleLabel(lblUser);
        JTextField txtUser = new JTextField(Configuracio.obtenirDbUser());
        UIComponents.styleField(txtUser);
        txtUser.putClientProperty("id", "txtUser");

        JLabel lblPass = new JLabel(t("lbl_password"));
        UIComponents.styleLabel(lblPass);
        JPasswordField txtPass = new JPasswordField(Configuracio.obtenirDbPassword());
        UIComponents.styleField(txtPass);
        txtPass.putClientProperty("id", "txtPass");

        JLabel lblDbNote = new JLabel(t("lbl_db_restart"));
        lblDbNote.setFont(UITheme.FONT_SMALL);
        lblDbNote.setForeground(UITheme.palette().textMid());

        JButton btnTestConn = new JButton(t("btn_test_connection"));
        UIComponents.styleSecondaryButton(btnTestConn);
        btnTestConn.addActionListener(e -> {
            boolean external = cmbType.getSelectedIndex() == 1;
            String dbType = external ? "mariadb" : "h2";
            java.util.Properties testProps = new java.util.Properties();
            testProps.setProperty("dbType", dbType);
            char[] passChars = new char[0];
            if (external) {
                testProps.setProperty("dbHost", txtHost.getText().trim());
                testProps.setProperty("dbUser", txtUser.getText().trim());
                passChars = txtPass.getPassword();
            }
            final char[] passSnapshot = passChars;
            btnTestConn.setEnabled(false);
            btnTestConn.setText(t("btn_test_connection") + "…");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    java.sql.Connection conn = persistencia.ConnexioServidor.testConnection(testProps, passSnapshot);
                    conn.close();
                    return null;
                }
                @Override protected void done() {
                    java.util.Arrays.fill(passSnapshot, '\0');
                    btnTestConn.setEnabled(true);
                    btnTestConn.setText(t("btn_test_connection"));
                    try {
                        get();
                        JOptionPane.showMessageDialog(owner, t("dlg_connection_ok"),
                            t("dlg_connection_title"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(owner,
                            t("dlg_connection_fail") + "\n" + cause.getMessage(),
                            t("dlg_connection_title"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        Runnable actualitzarFields = () -> {
            boolean external = cmbType.getSelectedIndex() == 1;
            txtHost.setEnabled(external);
            txtUser.setEnabled(external);
            txtPass.setEnabled(external);
        };
        actualitzarFields.run();
        cmbType.addActionListener(e -> actualitzarFields.run());

        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(lblSeccio)
            .addGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(lblTipus)
                    .addComponent(lblHost)
                    .addComponent(lblUser)
                    .addComponent(lblPass))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(cmbType, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtHost, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtUser, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtPass, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)))
            .addComponent(lblDbNote)
            .addComponent(btnTestConn, GroupLayout.Alignment.TRAILING)
        );

        gl.setVerticalGroup(gl.createSequentialGroup()
            .addComponent(lblSeccio)
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblTipus).addComponent(cmbType))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblHost).addComponent(txtHost))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblUser).addComponent(txtUser))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblPass).addComponent(txtPass))
            .addComponent(lblDbNote)
            .addComponent(btnTestConn)
        );

        return panel;
    }

    public static void reloadFromConfig(JPanel root) {
        javax.swing.JComponent jc;
        jc = SeccionsConfiguracio.cercarById(root, "cmbType");
        if (jc instanceof JComboBox<?> cmb) cmb.setSelectedIndex("h2".equals(Configuracio.obtenirDbType()) ? 0 : 1);
        jc = SeccionsConfiguracio.cercarById(root, "txtHost");
        if (jc instanceof JTextField tf) tf.setText(Configuracio.obtenirDbHost());
        jc = SeccionsConfiguracio.cercarById(root, "txtUser");
        if (jc instanceof JTextField tf) tf.setText(Configuracio.obtenirDbUser());
        jc = SeccionsConfiguracio.cercarById(root, "txtPass");
        if (jc instanceof JPasswordField pf) pf.setText(Configuracio.obtenirDbPassword());
    }
}
