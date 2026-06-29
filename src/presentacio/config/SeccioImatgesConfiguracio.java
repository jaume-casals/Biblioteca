package presentacio.config;

import herramienta.config.Configuracio;
import presentacio.util.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Images section: default cover-folder picker. */
public final class SeccioImatgesConfiguracio {
    private SeccioImatgesConfiguracio() {}

    public static JPanel build(JDialog owner) {
        UIComponents.SectionPanel section = UIComponents.sectionPanel();
        JPanel panel = section.panel();
        GroupLayout gl = section.layout();

        JLabel lblSeccio = UIComponents.sectionHeader("lbl_images");

        JLabel lblImgDir = UIComponents.label("lbl_default_folder");
        JTextField txtImgDir = UIComponents.field("txtImgDir", Configuracio.obtenirDefaultImgDir());

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
        SeccionsConfiguracio.apply(root, "txtImgDir", JTextField.class,
            c -> c.setText(Configuracio.obtenirDefaultImgDir()));
    }
}
