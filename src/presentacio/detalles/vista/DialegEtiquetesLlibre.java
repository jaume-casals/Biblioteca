package presentacio.detalles.vista;



import presentacio.UIComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import domini.Llibre;
import domini.Tag;
import herramienta.I18n;
import herramienta.UtilitatsSwing;
import herramienta.UITheme;

/**
 * Diàleg per gestionar les etiquetes assignades a un sol llibre.
 * <p>
 * A diferència de {@link presentacio.detalles.vista.DialegLlistesLlibre}, que
 * ajorna els canvis de pertinença a prestatgeria fins que l'usuari fa clic a
 * OK/Desar, aquest diàleg aplica cada operació d'afegir/treure etiqueta
 * immediatament (via {@link presentacio.detalles.control.ControladorEtiquetesLlibre}).
 * Es tria el model de persistència immediata perquè les operacions
 * d'etiquetes són files individuals de many-to-many lleugeres, mentre que
 * la pertinença a prestatgeria també porta valoració per llibre i estat de
 * lectura que es beneficien de l'edició per lots.
 */
public class DialegEtiquetesLlibre extends JDialog {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<Tag> comboAdd;
    private final JTextField filtrarField;
    private final JTextField txtNovaEtiqueta;
    private final JButton btnAfegir;
    private final JButton btnCrear;
    private final JButton btnTreure;

    public DialegEtiquetesLlibre(Window owner, Llibre llibre) {
        this(owner, llibre, null);
    }

    public DialegEtiquetesLlibre(Window owner, Llibre llibre, interficie.EscritorEtiqueta cd) {
        super(owner, I18n.t("dlg_tags_for_book", llibre.obtenirNom()), ModalityType.APPLICATION_MODAL);
        setSize(400, 440);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.palette().bgPanel());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel);

        filtrarField = new JTextField();
        UIComponents.styleField(filtrarField);
        filtrarField.setToolTipText(I18n.t("tip_filter_tags"));
        JPanel filtrarPanel = new JPanel(new BorderLayout(4, 0));
        filtrarPanel.setBackground(UITheme.palette().bgPanel());
        JLabel lblFilter = new JLabel(I18n.t("lbl_filter_colon"));
        UIComponents.styleLabel(lblFilter);
        filtrarPanel.add(lblFilter, BorderLayout.WEST);
        filtrarPanel.add(filtrarField, BorderLayout.CENTER);
        panel.add(filtrarPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{I18n.t("col_tag_name")}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setBackground(UITheme.palette().bgPanel());
        table.setForeground(UITheme.palette().textDark());
        table.setRowHeight(28);
        table.setFont(UITheme.fontBase());
        table.setSelectionBackground(UITheme.palette().accent());
        table.setSelectionForeground(Color.WHITE);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(4, 8));
        south.setBackground(UITheme.palette().bgPanel());

        comboAdd = new JComboBox<>();
        comboAdd.setPreferredSize(new Dimension(180, 30));

        btnAfegir = new JButton(I18n.t("btn_afegir_etiqueta"));
        UIComponents.styleAccentButton(btnAfegir);

        txtNovaEtiqueta = new JTextField(12);
        UIComponents.styleField(txtNovaEtiqueta);
        txtNovaEtiqueta.setToolTipText(I18n.t("tip_nom_nova_etiqueta"));

        btnCrear = new JButton(I18n.t("btn_crear"));
        UIComponents.styleSecondaryButton(btnCrear);

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addRow.setBackground(UITheme.palette().bgPanel());
        JLabel lblEtiq = new JLabel(I18n.t("lbl_etiqueta_colon"));
        UIComponents.styleLabel(lblEtiq);
        addRow.add(lblEtiq);
        addRow.add(comboAdd);
        addRow.add(btnAfegir);

        JPanel crearRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        crearRow.setBackground(UITheme.palette().bgPanel());
        JLabel lblNova = new JLabel(I18n.t("lbl_nova_colon"));
        UIComponents.styleLabel(lblNova);
        crearRow.add(lblNova);
        crearRow.add(txtNovaEtiqueta);
        crearRow.add(btnCrear);

        btnTreure = new JButton(I18n.t("btn_treure_seleccionada"));
        UIComponents.styleSecondaryButton(btnTreure);
        btnTreure.setBackground(UITheme.palette().danger());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        btnRow.setBackground(UITheme.palette().bgPanel());
        btnRow.add(btnTreure);

        JPanel afegirArea = new JPanel(new BorderLayout(0, 2));
        afegirArea.setBackground(UITheme.palette().bgPanel());
        afegirArea.add(addRow, BorderLayout.NORTH);
        afegirArea.add(crearRow, BorderLayout.CENTER);
        afegirArea.add(btnRow, BorderLayout.SOUTH);

        south.add(afegirArea, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);

        new presentacio.detalles.control.ControladorEtiquetesLlibre(this, llibre, cd);
    }

    public JTable obtenirTable() { return table; }
    public DefaultTableModel obtenirTableModel() { return tableModel; }
    public JComboBox<Tag> obtenirComboAdd() { return comboAdd; }
    public JTextField obtenirFilterField() { return filtrarField; }
    public JTextField obtenirTxtNovaEtiqueta() { return txtNovaEtiqueta; }
    public JButton obtenirBtnAfegir() { return btnAfegir; }
    public JButton obtenirBtnCrear() { return btnCrear; }
    public JButton obtenirBtnTreure() { return btnTreure; }

    public void actualitzarTable(ArrayList<Tag> tags, Map<Integer, Integer> counts) {
        tableModel.setRowCount(0);
        for (Tag t : tags) {
            Integer c = counts.get(t.obtenirId());
            String display = c != null && c > 0 ? t.obtenirNom() + " (" + c + ")" : t.obtenirNom();
            tableModel.addRow(new Object[]{ display });
        }
    }

    public void actualitzarComboAdd(ArrayList<Tag> tags) {
        UtilitatsSwing.reloadComboPreserveSelection(comboAdd, tags, Tag::obtenirId);
    }
}