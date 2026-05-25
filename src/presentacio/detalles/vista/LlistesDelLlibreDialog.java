package presentacio.detalles.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import domini.Llibre;
import interficie.BibliotecaWriter;
import domini.Llista;
import herramienta.I18n;
import herramienta.UITheme;
import presentacio.detalles.control.LlistesDelLlibreControl;

public class LlistesDelLlibreDialog extends JDialog {

    private final Llibre llibre;
    private final LlistesDelLlibreTableModel tableModel;
    private final JTable table;
    private JComboBox<Llista> comboAdd;
    private JTextField txtVal;
    private JCheckBox chkLlegit;
    private JList<Llista> shelfCheckList;
    private JButton btnAfegir;
    private JButton btnTreure;
    private JButton btnGuardar;
    private final BibliotecaWriter cd;

    public LlistesDelLlibreDialog(Window owner, Llibre llibre) {
        this(owner, llibre, null);
    }

    public LlistesDelLlibreDialog(Window owner, Llibre llibre, BibliotecaWriter cd) {
        super(owner, I18n.t("dlg_llistes_title", llibre.getNom()), ModalityType.APPLICATION_MODAL);
        this.llibre = llibre;
        this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
        setSize(520, 460);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel);

        tableModel = new LlistesDelLlibreTableModel();
        table = new JTable(tableModel);
        table.setBackground(UITheme.BG_PANEL);
        table.setForeground(UITheme.TEXT_DARK);
        table.setRowHeight(30);
        table.setFont(UITheme.FONT_BASE);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.getColumnModel().getColumn(LlistesDelLlibreTableModel.COL_NOM).setPreferredWidth(200);
        table.getColumnModel().getColumn(LlistesDelLlibreTableModel.COL_VAL).setPreferredWidth(80);
        table.getColumnModel().getColumn(LlistesDelLlibreTableModel.COL_LLEGIT).setPreferredWidth(60);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        shelfCheckList = new JList<>();
        shelfCheckList.setVisibleRowCount(8);
        shelfCheckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shelfCheckList.setBackground(UITheme.BG_PANEL);
        shelfCheckList.setForeground(UITheme.TEXT_DARK);
        shelfCheckList.setFont(UITheme.FONT_BASE);
        JPanel west = new JPanel(new BorderLayout(4, 4));
        west.setBackground(UITheme.BG_PANEL);
        west.setPreferredSize(new Dimension(180, 0));
        JLabel lblChecks = new JLabel(I18n.t("lbl_llistes"));
        UITheme.styleLabel(lblChecks);
        west.add(lblChecks, BorderLayout.NORTH);
        west.add(new JScrollPane(shelfCheckList), BorderLayout.CENTER);
        panel.add(west, BorderLayout.WEST);

        // ── South: add-to-shelf controls + save/remove ───────────────────────
        JPanel south = new JPanel(new BorderLayout(4, 8));
        south.setBackground(UITheme.BG_PANEL);

        comboAdd = new JComboBox<>();
        comboAdd.setPreferredSize(new Dimension(160, 30));

        txtVal = new JTextField("0.0");
        UITheme.styleField(txtVal);
        txtVal.setPreferredSize(new Dimension(60, 30));
        txtVal.setToolTipText(I18n.t("lbl_valoracio_tip"));

        chkLlegit = new JCheckBox(I18n.t("col_read"));
        chkLlegit.setBackground(UITheme.BG_PANEL);
        chkLlegit.setForeground(UITheme.TEXT_DARK);
        chkLlegit.setFont(UITheme.FONT_BASE);

        btnAfegir = new JButton(I18n.t("btn_afegir_llista"));
        UITheme.styleAccentButton(btnAfegir);

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addRow.setBackground(UITheme.BG_PANEL);
        JLabel lblLlista = new JLabel(I18n.t("lbl_llista_colon"));
        UITheme.styleLabel(lblLlista);
        addRow.add(lblLlista);
        addRow.add(comboAdd);
        addRow.add(txtVal);
        addRow.add(chkLlegit);
        addRow.add(btnAfegir);

        btnTreure = new JButton(I18n.t("btn_treure_seleccionada"));
        UITheme.styleSecondaryButton(btnTreure);
        btnTreure.setBackground(UITheme.DANGER);

        btnGuardar = new JButton(I18n.t("btn_guardar_canvis"));
        UITheme.styleAccentButton(btnGuardar);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        btnRow.setBackground(UITheme.BG_PANEL);
        btnRow.add(btnTreure);
        btnRow.add(btnGuardar);

        south.add(addRow, BorderLayout.NORTH);
        south.add(btnRow, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        new LlistesDelLlibreControl(this, llibre, cd);
    }

    public JTable getTable() { return table; }
    public LlistesDelLlibreTableModel getTableModel() { return tableModel; }
    public JComboBox<Llista> getComboAdd() { return comboAdd; }
    public JTextField getTxtVal() { return txtVal; }
    public JCheckBox getChkLlegit() { return chkLlegit; }
    public JList<Llista> getShelfCheckList() { return shelfCheckList; }
    public JButton getBtnAfegir() { return btnAfegir; }
    public JButton getBtnTreure() { return btnTreure; }
    public JButton getBtnGuardar() { return btnGuardar; }
}
