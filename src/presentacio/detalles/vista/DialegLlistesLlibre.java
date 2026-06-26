package presentacio.detalles.vista;

import presentacio.util.UIComponents;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import domini.Llibre;
import persistencia.contract.EscritorPrestatgeria;
import domini.Llista;
import herramienta.i18n.I18n;
import herramienta.ui.UITheme;
import presentacio.detalles.control.ControladorLlistesLlibre;

public class DialegLlistesLlibre extends DialegEntitatLlibre<Llista> {

    private final ModelTaulaLlistesLlibre tableModel;
    private JTextField txtVal;
    private JCheckBox chkLlegit;
    private JList<Llista> shelfCheckList;
    private JButton btnGuardar;

    public DialegLlistesLlibre(Window owner, Llibre llibre) {
        this(owner, llibre, null);
    }

    public DialegLlistesLlibre(Window owner, Llibre llibre, EscritorPrestatgeria cd) {
        super(owner, I18n.t("dlg_llistes_title", llibre.obtenirNom()), 520, 460,
                new ModelTaulaLlistesLlibre(), 30);
        tableModel = (ModelTaulaLlistesLlibre) table.getModel();
        table.getColumnModel().getColumn(ModelTaulaLlistesLlibre.COL_NOM).setPreferredWidth(200);
        table.getColumnModel().getColumn(ModelTaulaLlistesLlibre.COL_VAL).setPreferredWidth(80);
        table.getColumnModel().getColumn(ModelTaulaLlistesLlibre.COL_LLEGIT).setPreferredWidth(60);
        rootPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        shelfCheckList = new JList<>();
        shelfCheckList.setVisibleRowCount(8);
        shelfCheckList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        shelfCheckList.setBackground(UITheme.palette().bgPanel());
        shelfCheckList.setForeground(UITheme.palette().textDark());
        shelfCheckList.setFont(UITheme.fontBase());
        JPanel west = new JPanel(new BorderLayout(4, 4));
        west.setBackground(UITheme.palette().bgPanel());
        west.setPreferredSize(new Dimension(180, 0));
        JLabel lblChecks = new JLabel(I18n.t("lbl_llistes"));
        UIComponents.styleLabel(lblChecks);
        west.add(lblChecks, BorderLayout.NORTH);
        west.add(new JScrollPane(shelfCheckList), BorderLayout.CENTER);
        rootPanel.add(west, BorderLayout.WEST);

        JPanel south = new JPanel(new BorderLayout(4, 8));
        south.setBackground(UITheme.palette().bgPanel());

        comboAdd.setPreferredSize(new Dimension(160, 30));
        btnAfegir.setText(I18n.t("btn_afegir_llista"));

        txtVal = new JTextField("0.0");
        UIComponents.styleField(txtVal);
        txtVal.setPreferredSize(new Dimension(60, 30));
        txtVal.setToolTipText(I18n.t("lbl_valoracio_tip"));

        chkLlegit = new JCheckBox(I18n.t("col_read"));
        chkLlegit.setBackground(UITheme.palette().bgPanel());
        chkLlegit.setForeground(UITheme.palette().textDark());
        chkLlegit.setFont(UITheme.fontBase());

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addRow.setBackground(UITheme.palette().bgPanel());
        JLabel lblLlista = new JLabel(I18n.t("lbl_llista_colon"));
        UIComponents.styleLabel(lblLlista);
        addRow.add(lblLlista);
        addRow.add(comboAdd);
        addRow.add(txtVal);
        addRow.add(chkLlegit);
        addRow.add(btnAfegir);

        btnGuardar = new JButton(I18n.t("btn_guardar_canvis"));
        UIComponents.styleAccentButton(btnGuardar);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        btnRow.setBackground(UITheme.palette().bgPanel());
        btnRow.add(btnTreure);
        btnRow.add(btnGuardar);

        south.add(addRow, BorderLayout.NORTH);
        south.add(btnRow, BorderLayout.SOUTH);
        rootPanel.add(south, BorderLayout.SOUTH);

        new ControladorLlistesLlibre(this, llibre, cd);
    }

    public ModelTaulaLlistesLlibre obtenirTableModel() { return tableModel; }
    public JTextField obtenirTxtVal() { return txtVal; }
    public JCheckBox obtenirChkLlegit() { return chkLlegit; }
    public JList<Llista> obtenirShelfCheckList() { return shelfCheckList; }
    public JButton obtenirBtnGuardar() { return btnGuardar; }
}
