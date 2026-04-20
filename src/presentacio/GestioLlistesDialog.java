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

        JButton btnColor = new JButton("Color");
        UITheme.styleSecondaryButton(btnColor);
        btnColor.setToolTipText("Canviar color de la llista seleccionada");
        btnColor.addActionListener(e -> {
            Llista sel = jList.getSelectedValue();
            if (sel == null) return;
            java.awt.Color initial = sel.getColor() != null
                ? java.awt.Color.decode(sel.getColor()) : java.awt.Color.decode("#3498DB");
            java.awt.Color chosen = javax.swing.JColorChooser.showDialog(this, "Escull color", initial);
            if (chosen == null) return;
            String hex = String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
            try {
                ControladorDomini.getInstance().setLlistaColor(sel.getId(), hex);
                reload();
                mainControl.refreshComboLlistes();
            } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
        });

        JButton btnUp = new JButton("▲");
        UITheme.styleSecondaryButton(btnUp);
        btnUp.setToolTipText("Pujar llista");
        btnUp.addActionListener(e -> {
            Llista sel = jList.getSelectedValue();
            if (sel == null) return;
            try {
                ControladorDomini.getInstance().moveLlistaUp(sel.getId());
                reload();
                mainControl.refreshComboLlistes();
                for (int i = 0; i < listModel.size(); i++)
                    if (listModel.get(i).getId() == sel.getId()) { jList.setSelectedIndex(i); break; }
            } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
        });

        JButton btnDown = new JButton("▼");
        UITheme.styleSecondaryButton(btnDown);
        btnDown.setToolTipText("Baixar llista");
        btnDown.addActionListener(e -> {
            Llista sel = jList.getSelectedValue();
            if (sel == null) return;
            try {
                ControladorDomini.getInstance().moveLlistaDown(sel.getId());
                reload();
                mainControl.refreshComboLlistes();
                for (int i = 0; i < listModel.size(); i++)
                    if (listModel.get(i).getId() == sel.getId()) { jList.setSelectedIndex(i); break; }
            } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
        });

        JPanel reorderRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        reorderRow.setBackground(UITheme.BG_PANEL);
        reorderRow.add(btnUp);
        reorderRow.add(btnDown);
        reorderRow.add(btnColor);

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.setBackground(UITheme.BG_PANEL);
        inputRow.add(txtNom, BorderLayout.CENTER);
        inputRow.add(btnAfegir, BorderLayout.EAST);

        bottom.add(reorderRow, BorderLayout.NORTH);
        bottom.add(inputRow, BorderLayout.CENTER);
        bottom.add(btnEliminar, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);

        reload();
    }

    private void reload() {
        listModel.clear();
        for (Llista l : ControladorDomini.getInstance().getAllLlistes()) listModel.addElement(l);
        jList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                javax.swing.JLabel lbl = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Llista) {
                    Llista ll = (Llista) value;
                    String col = ll.getColor();
                    if (col != null) {
                        lbl.setIcon(colorSwatch(java.awt.Color.decode(col)));
                    } else {
                        lbl.setIcon(null);
                    }
                }
                return lbl;
            }
        });
    }

    private javax.swing.Icon colorSwatch(java.awt.Color c) {
        return new javax.swing.Icon() {
            public int getIconWidth()  { return 14; }
            public int getIconHeight() { return 14; }
            public void paintIcon(java.awt.Component comp, java.awt.Graphics g, int x, int y) {
                g.setColor(c);
                g.fillRoundRect(x, y + 1, 12, 12, 4, 4);
                g.setColor(c.darker());
                g.drawRoundRect(x, y + 1, 12, 12, 4, 4);
            }
        };
    }
}
