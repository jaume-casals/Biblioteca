package presentacio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import domini.Llista;
import interficie.BibliotecaWriter;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;
import herramienta.ColorUtils;

public class GestioLlistesDialog extends JDialog {

    private final MostrarBibliotecaControl mainControl;
    private final DefaultListModel<Llista> listModel = new DefaultListModel<>();
    private final JList<Llista> jList = new JList<>(listModel);
    private final BibliotecaWriter cd;

    public GestioLlistesDialog(Window owner, MostrarBibliotecaControl mainControl) {
        this(owner, mainControl, null);
    }

    public GestioLlistesDialog(Window owner, MostrarBibliotecaControl mainControl, BibliotecaWriter cd) {
        super(owner, I18n.t("dlg_gestio_llistes_title"), ModalityType.APPLICATION_MODAL);
        this.mainControl = mainControl;
        this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
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
        txtNom.setToolTipText(I18n.t("tip_nom_nova_llista"));

        JButton btnAfegir = new JButton(I18n.t("btn_nova_llista"));
        UITheme.styleAccentButton(btnAfegir);
        btnAfegir.addActionListener(e -> onAddLlista(txtNom));

        JButton btnEliminar = new JButton(I18n.t("btn_eliminar_seleccionada"));
        UITheme.styleSecondaryButton(btnEliminar);
        btnEliminar.setBackground(UITheme.DANGER);
        btnEliminar.addActionListener(e -> onDeleteLlista());

        JButton btnRename = new JButton(I18n.t("btn_rename_llista"));
        UITheme.styleSecondaryButton(btnRename);
        btnRename.setToolTipText(I18n.t("tip_rename_llista"));
        btnRename.addActionListener(e -> onRenameLlista());

        JButton btnColor = new JButton(I18n.t("btn_color_llista"));
        UITheme.styleSecondaryButton(btnColor);
        btnColor.setToolTipText(I18n.t("tip_color_llista"));
        btnColor.addActionListener(e -> onColorLlista());

        JButton btnUp = new JButton("▲");
        UITheme.styleSecondaryButton(btnUp);
        btnUp.setToolTipText(I18n.t("tip_pujar_llista"));
        btnUp.addActionListener(e -> onMoveLlista(true));

        JButton btnDown = new JButton("▼");
        UITheme.styleSecondaryButton(btnDown);
        btnDown.setToolTipText(I18n.t("tip_baixar_llista"));
        btnDown.addActionListener(e -> onMoveLlista(false));

        JPanel reorderRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        reorderRow.setBackground(UITheme.BG_PANEL);
        reorderRow.add(btnUp);
        reorderRow.add(btnDown);
        reorderRow.add(btnRename);
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

    private void onAddLlista(JTextField txtNom) {
        String nom = txtNom.getText().trim();
        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                I18n.t("err_nom_llista_buit"), I18n.t("dlg_validacio_title"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            cd.addLlista(nom);
            txtNom.setText("");
            reload();
            mainControl.refreshComboLlistes();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void onDeleteLlista() {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            I18n.t("dlg_confirm_delete_llista", sel.getNom()),
            I18n.t("dlg_confirm_delete_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            cd.deleteLlista(sel);
            reload();
            mainControl.refreshComboLlistes();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void onRenameLlista() {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        String newNom = (String) JOptionPane.showInputDialog(this,
            I18n.t("dlg_rename_llista_prompt", sel.getNom()),
            I18n.t("dlg_rename_llista_title"), JOptionPane.PLAIN_MESSAGE, null, null, sel.getNom());
        if (newNom == null || newNom.isBlank()) return;
        try {
            cd.renameLlista(sel.getId(), newNom.trim());
            reload();
            mainControl.refreshComboLlistes();
        } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
    }

    private void onColorLlista() {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        java.awt.Color initial = sel.getColor() != null
            ? java.awt.Color.decode(sel.getColor()) : java.awt.Color.decode("#3498DB");
        String hex = herramienta.ColorSwatchPicker.chooseHex(this, initial, "dlg_escull_color_title");
        if (hex == null) return;
        try {
            cd.setLlistaColor(sel.getId(), hex);
            reload();
            mainControl.refreshComboLlistes();
        } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
    }

    private void onMoveLlista(boolean up) {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        try {
            if (up) cd.moveLlistaUp(sel.getId()); else cd.moveLlistaDown(sel.getId());
            reload();
            mainControl.refreshComboLlistes();
            for (int i = 0; i < listModel.size(); i++)
                if (listModel.get(i).getId() == sel.getId()) { jList.setSelectedIndex(i); break; }
        } catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
    }

    private void reload() {
        listModel.clear();
        for (Llista l : cd.getAllLlistes()) listModel.addElement(l);
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
                        lbl.setIcon(ColorUtils.colorSwatch(java.awt.Color.decode(col)));
                    } else {
                        lbl.setIcon(null);
                    }
                }
                return lbl;
            }
        });
    }
}
