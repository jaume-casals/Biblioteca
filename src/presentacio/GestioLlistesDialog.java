package presentacio;

import java.awt.BorderLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import domini.ControladorDomini;
import domini.Llista;
import herramienta.DialogoError;
import herramienta.UITheme;

public class GestioLlistesDialog extends JDialog {

    private final MostrarBibliotecaControl mainControl;
    private final DefaultListModel<Llista> listModel = new DefaultListModel<>();
    private final JList<Llista> jList = new JList<>(listModel);

    public GestioLlistesDialog(Window owner, MostrarBibliotecaControl mainControl) {
        super(owner, "Gestionar Llistes", ModalityType.APPLICATION_MODAL);
        this.mainControl = mainControl;
        setSize(340, 400);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel);

        jList.setBackground(UITheme.BG_MAIN);
        jList.setForeground(UITheme.TEXT_DARK);
        jList.setFont(UITheme.FONT_BASE);
        panel.add(new JScrollPane(jList), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 6));
        bottom.setBackground(UITheme.BG_PANEL);

        JTextField txtNom = new JTextField();
        UITheme.styleField(txtNom);
        txtNom.setToolTipText("Nom de la nova llista");

        JButton btnAfegir = new JButton("Nova Llista");
        UITheme.styleAccentButton(btnAfegir);
        btnAfegir.addActionListener(e -> {
            String nom = txtNom.getText().trim();
            if (nom.isEmpty()) return;
            try {
                ControladorDomini.getInstance().addLlista(nom);
                txtNom.setText("");
                reload();
                mainControl.refreshComboLlistes();
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
            }
        });

        JButton btnEliminar = new JButton("Eliminar Seleccionada");
        UITheme.styleSecondaryButton(btnEliminar);
        btnEliminar.setBackground(UITheme.DANGER);
        btnEliminar.addActionListener(e -> {
            Llista sel = jList.getSelectedValue();
            if (sel == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                "Eliminar la llista \"" + sel.getNom() + "\"?\n" +
                "S'eliminaran totes les assignacions de llibres a aquesta llista.",
                "Confirmar eliminació", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                ControladorDomini.getInstance().deleteLlista(sel);
                reload();
                mainControl.refreshComboLlistes();
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
            }
        });

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.setBackground(UITheme.BG_PANEL);
        inputRow.add(txtNom, BorderLayout.CENTER);
        inputRow.add(btnAfegir, BorderLayout.EAST);

        bottom.add(inputRow, BorderLayout.NORTH);
        bottom.add(btnEliminar, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);

        reload();
    }

    private void reload() {
        listModel.clear();
        for (Llista l : ControladorDomini.getInstance().getAllLlistes()) listModel.addElement(l);
    }
}
