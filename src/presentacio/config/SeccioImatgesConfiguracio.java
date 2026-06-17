package presentacio.config;

import herramienta.Configuracio;
import herramienta.UITheme;
import presentacio.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static herramienta.I18n.t;

/** Images section: default cover-folder picker. */
public final class SeccioImatgesConfiguracio {
    private SeccioImatgesConfiguracio() {}

    public static JPanel build(JDialog owner) {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.palette().bgPanel());
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lblSeccio = new JLabel(t("lbl_images"));
        lblSeccio.setFont(UITheme.fontBold());
        lblSeccio.setForeground(UITheme.palette().accent());

        JLabel lblImgDir = new JLabel(t("lbl_default_folder"));
        UIComponents.styleLabel(lblImgDir);
        JTextField txtImgDir = new JTextField(Configuracio.obtenirDefaultImgDir());
        UIComponents.styleField(txtImgDir);
        txtImgDir.putClientProperty("id", "txtImgDir");

        JButton btnExplorar = new JButton("...");
        UIComponents.styleSecondaryButton(btnExplorar);
        btnExplorar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(txtImgDir.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION)
                txtImgDir.setText(fc.getSelectedFile().getAbsolutePath());
        });

        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(lblSeccio)
            .addGroup(gl.createSequentialGroup()
                .addComponent(lblImgDir)
                .addComponent(txtImgDir, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                .addComponent(btnExplorar))
        );

        gl.setVerticalGroup(gl.createSequentialGroup()
            .addComponent(lblSeccio)
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblImgDir).addComponent(txtImgDir).addComponent(btnExplorar))
        );

        return panel;
    }

    public static void reloadFromConfig(JPanel root) {
        javax.swing.JComponent jc = SeccionsConfiguracio.cercarById(root, "txtImgDir");
        if (jc instanceof JTextField) ((JTextField) jc).setText(Configuracio.obtenirDefaultImgDir());
    }
}
