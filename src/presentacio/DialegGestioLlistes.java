package presentacio;



import presentacio.UIComponents;
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

import domini.Llista;
import interficie.EscritorPrestatgeria;
import herramienta.DialegError;
import herramienta.I18n;
import herramienta.UITheme;
import herramienta.UtilitatsColor;

public class DialegGestioLlistes extends JDialog {

    private final ControladorMostrarBiblioteca mainControl;
    private final DefaultListModel<Llista> listModel = new DefaultListModel<>();
    private final JList<Llista> jList = new JList<>(listModel);
    private final EscritorPrestatgeria cd;

    /** El diàleg requereix un {@code cd} (EscritorPrestatgeria) no nul — la caiguda
     *  a null cap a {@code ControladorDomini.getInstance()} s'ha eliminat
     *  segons el finding MEDIUM de tot.txt. El consumidor (ShelfController)
     *  és l'únic punt d'entrada i sempre subministra {@code state.cd}. */
    public DialegGestioLlistes(Window owner, ControladorMostrarBiblioteca mainControl, EscritorPrestatgeria cd) {
        super(owner, I18n.t("dlg_gestio_llistes_title"), ModalityType.APPLICATION_MODAL);
        if (cd == null) throw new IllegalArgumentException("cd (EscritorPrestatgeria) is required");
        this.mainControl = mainControl;
        this.cd = cd;
        setSize(340, 400);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.palette().bgPanel());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel);

        jList.setBackground(UITheme.palette().bgMain());
        jList.setForeground(UITheme.palette().textDark());
        jList.setFont(UITheme.fontBase());
        panel.add(new JScrollPane(jList), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 6));
        bottom.setBackground(UITheme.palette().bgPanel());

        JTextField txtNom = new JTextField();
        UIComponents.styleField(txtNom);
        txtNom.setToolTipText(I18n.t("tip_nom_nova_llista"));

        JButton btnAfegir = new JButton(I18n.t("btn_nova_llista"));
        UIComponents.styleAccentButton(btnAfegir);
        btnAfegir.addActionListener(e -> onAddLlista(txtNom));

        JButton btnEliminar = new JButton(I18n.t("btn_eliminar_seleccionada"));
        UIComponents.styleSecondaryButton(btnEliminar);
        btnEliminar.setBackground(UITheme.palette().danger());
        btnEliminar.addActionListener(e -> onDeleteLlista());

        JButton btnRename = new JButton(I18n.t("btn_rename_llista"));
        UIComponents.styleSecondaryButton(btnRename);
        btnRename.setToolTipText(I18n.t("tip_rename_llista"));
        btnRename.addActionListener(e -> onRenameLlista());

        JButton btnColor = new JButton(I18n.t("btn_color_llista"));
        UIComponents.styleSecondaryButton(btnColor);
        btnColor.setToolTipText(I18n.t("tip_color_llista"));
        btnColor.addActionListener(e -> onColorLlista());

        JButton btnUp = new JButton("▲");
        UIComponents.styleSecondaryButton(btnUp);
        btnUp.setToolTipText(I18n.t("tip_pujar_llista"));
        btnUp.addActionListener(e -> onMoveLlista(true));

        JButton btnDown = new JButton("▼");
        UIComponents.styleSecondaryButton(btnDown);
        btnDown.setToolTipText(I18n.t("tip_baixar_llista"));
        btnDown.addActionListener(e -> onMoveLlista(false));

        JPanel reorderRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        reorderRow.setBackground(UITheme.palette().bgPanel());
        reorderRow.add(btnUp);
        reorderRow.add(btnDown);
        reorderRow.add(btnRename);
        reorderRow.add(btnColor);

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.setBackground(UITheme.palette().bgPanel());
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
            cd.afegirLlista(nom);
            txtNom.setText("");
            reload();
            mainControl.refrescarComboLlistes();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
        }
    }

    private void onDeleteLlista() {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            I18n.t("dlg_confirm_delete_llista", sel.obtenirNom()),
            I18n.t("dlg_confirm_delete_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            cd.eliminarLlista(sel);
            reload();
            mainControl.refrescarComboLlistes();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
        }
    }

    private void onRenameLlista() {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        String newNom = (String) JOptionPane.showInputDialog(this,
            I18n.t("dlg_rename_llista_prompt", sel.obtenirNom()),
            I18n.t("dlg_rename_llista_title"), JOptionPane.PLAIN_MESSAGE, null, null, sel.obtenirNom());
        if (newNom == null || newNom.isBlank()) return;
        try {
            cd.reanomenarLlista(sel.obtenirId(), newNom.trim());
            reload();
            mainControl.refrescarComboLlistes();
        } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); }
    }

    private void onColorLlista() {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        // Per defecte, el blau compartit del selector de color quan la
        // prestatgeria encara no té color (pujat a ColorSwatchPicker.DEFAULT_HEX
        // segons el finding MEDIUM de tot.txt sobre el valor per defecte compartit).
        java.awt.Color initial = sel.obtenirColor() != null
            ? java.awt.Color.decode(sel.obtenirColor()) : java.awt.Color.decode(herramienta.SelectorMostraColor.DEFAULT_HEX);
        String hex = herramienta.SelectorMostraColor.chooseHex(this, initial, "dlg_escull_color_title");
        if (hex == null) return;
        try {
            cd.posarLlistaColor(sel.obtenirId(), hex);
            reload();
            mainControl.refrescarComboLlistes();
        } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); }
    }

    private void onMoveLlista(boolean up) {
        Llista sel = jList.getSelectedValue();
        if (sel == null) return;
        try {
            if (up) cd.moureLlistaUp(sel.obtenirId()); else cd.moureLlistaDown(sel.obtenirId());
            reload();
            mainControl.refrescarComboLlistes();
            for (int i = 0; i < listModel.size(); i++)
                if (listModel.get(i).obtenirId() == sel.obtenirId()) { jList.setSelectedIndex(i); break; }
        } catch (Exception ex) { new DialegError(ex).mostrarErrorMessage(); }
    }

    /**
     * Instància única de renderer sense estat reutilitzada entre les
     * crides a {@link #reload()}. La implementació anterior assignava
     * un {@code DefaultListCellRenderer} nou a cada {@code reload()},
     * de manera que una sessió de drag-and-drop a la llista de
     * prestatgeries produïa O(n) assignacions per a una biblioteca
     * de 10 prestatgeries. El renderer no llegeix cap estat de camp —
     * només estableix una icona a partir de l'argument {@code value}.
     */
    private final javax.swing.ListCellRenderer<java.lang.Object> LIST_RENDERER =
        new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                javax.swing.JLabel lbl = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Llista) {
                    Llista ll = (Llista) value;
                    String col = ll.obtenirColor();
                    lbl.setIcon(col != null ? UtilitatsColor.colorSwatch(java.awt.Color.decode(col)) : null);
                }
                return lbl;
            }
        };

    private void reload() {
        listModel.clear();
        for (Llista l : cd.obtenirAllLlistes()) listModel.addElement(l);
        jList.setCellRenderer(LIST_RENDERER);
    }
}
