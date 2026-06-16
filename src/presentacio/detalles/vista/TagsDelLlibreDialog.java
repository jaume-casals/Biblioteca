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
import herramienta.SwingUtils;
import herramienta.UITheme;

/**
 * Dialog for managing the tags assigned to a single book.
 * <p>
 * Unlike {@link presentacio.detalles.vista.LlistesDelLlibreDialog}, which defers
 * shelf-membership changes until the user clicks OK/Save, this dialog applies every
 * tag add/remove operation immediately (via {@link presentacio.detalles.control.TagsDelLlibreControl}).
 * The immediate-persist model is chosen because tag operations are lightweight single
 * many-to-many rows, whereas shelf membership also carries per-book rating and read-state
 * that benefit from batch editing.
 */
public class TagsDelLlibreDialog extends JDialog {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<Tag> comboAdd;
    private final JTextField filterField;
    private final JTextField txtNovaEtiqueta;
    private final JButton btnAfegir;
    private final JButton btnCrear;
    private final JButton btnTreure;

    public TagsDelLlibreDialog(Window owner, Llibre llibre) {
        this(owner, llibre, null);
    }

    public TagsDelLlibreDialog(Window owner, Llibre llibre, interficie.TagWriter cd) {
        super(owner, I18n.t("dlg_tags_for_book", llibre.getNom()), ModalityType.APPLICATION_MODAL);
        setSize(400, 440);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.palette().bgPanel());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel);

        filterField = new JTextField();
        UIComponents.styleField(filterField);
        filterField.setToolTipText(I18n.t("tip_filter_tags"));
        JPanel filterPanel = new JPanel(new BorderLayout(4, 0));
        filterPanel.setBackground(UITheme.palette().bgPanel());
        JLabel lblFilter = new JLabel(I18n.t("lbl_filter_colon"));
        UIComponents.styleLabel(lblFilter);
        filterPanel.add(lblFilter, BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        panel.add(filterPanel, BorderLayout.NORTH);

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

        JPanel createRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        createRow.setBackground(UITheme.palette().bgPanel());
        JLabel lblNova = new JLabel(I18n.t("lbl_nova_colon"));
        UIComponents.styleLabel(lblNova);
        createRow.add(lblNova);
        createRow.add(txtNovaEtiqueta);
        createRow.add(btnCrear);

        btnTreure = new JButton(I18n.t("btn_treure_seleccionada"));
        UIComponents.styleSecondaryButton(btnTreure);
        btnTreure.setBackground(UITheme.palette().danger());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        btnRow.setBackground(UITheme.palette().bgPanel());
        btnRow.add(btnTreure);

        JPanel addArea = new JPanel(new BorderLayout(0, 2));
        addArea.setBackground(UITheme.palette().bgPanel());
        addArea.add(addRow, BorderLayout.NORTH);
        addArea.add(createRow, BorderLayout.CENTER);
        addArea.add(btnRow, BorderLayout.SOUTH);

        south.add(addArea, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);

        new presentacio.detalles.control.TagsDelLlibreControl(this, llibre, cd);
    }

    public JTable getTable() { return table; }
    public DefaultTableModel getTableModel() { return tableModel; }
    public JComboBox<Tag> getComboAdd() { return comboAdd; }
    public JTextField getFilterField() { return filterField; }
    public JTextField getTxtNovaEtiqueta() { return txtNovaEtiqueta; }
    public JButton getBtnAfegir() { return btnAfegir; }
    public JButton getBtnCrear() { return btnCrear; }
    public JButton getBtnTreure() { return btnTreure; }

    public void updateTable(ArrayList<Tag> tags, Map<Integer, Integer> counts) {
        tableModel.setRowCount(0);
        for (Tag t : tags) {
            Integer c = counts.get(t.getId());
            String display = c != null && c > 0 ? t.getNom() + " (" + c + ")" : t.getNom();
            tableModel.addRow(new Object[]{ display });
        }
    }

    public void updateComboAdd(ArrayList<Tag> tags) {
        SwingUtils.reloadComboPreserveSelection(comboAdd, tags, Tag::getId);
    }
}