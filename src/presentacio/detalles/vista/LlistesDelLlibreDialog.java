package presentacio.detalles.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import domini.Llibre;
import interficie.BibliotecaWriter;
import domini.Llista;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;

public class LlistesDelLlibreDialog extends JDialog {

    private static final int COL_NOM    = 0;
    private static final int COL_VAL    = 1;
    private static final int COL_LLEGIT = 2;

    private final Llibre llibre;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private ArrayList<Llista> llistesCache = new ArrayList<>();
    private JComboBox<Llista> comboAdd;
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

        tableModel = new DefaultTableModel(new String[]{I18n.t("col_list"), I18n.t("col_rating"), I18n.t("col_read")}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == COL_VAL || c == COL_LLEGIT; }
            @Override public Class<?> getColumnClass(int c) { return c == COL_LLEGIT ? Boolean.class : String.class; }
        };
        table = new JTable(tableModel);
        table.setBackground(UITheme.BG_PANEL);
        table.setForeground(UITheme.TEXT_DARK);
        table.setRowHeight(30);
        table.setFont(UITheme.FONT_BASE);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.getColumnModel().getColumn(COL_NOM).setPreferredWidth(200);
        table.getColumnModel().getColumn(COL_VAL).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_LLEGIT).setPreferredWidth(60);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── South: add-to-shelf controls + save/remove ───────────────────────
        JPanel south = new JPanel(new BorderLayout(4, 8));
        south.setBackground(UITheme.BG_PANEL);

        comboAdd = new JComboBox<>();
        reloadComboAdd();
        comboAdd.setPreferredSize(new Dimension(160, 30));

        JTextField txtVal = new JTextField("0.0");
        UITheme.styleField(txtVal);
        txtVal.setPreferredSize(new Dimension(60, 30));
        txtVal.setToolTipText(I18n.t("lbl_valoracio_tip"));

        JCheckBox chkLlegit = new JCheckBox(I18n.t("col_read"));
        chkLlegit.setBackground(UITheme.BG_PANEL);
        chkLlegit.setForeground(UITheme.TEXT_DARK);
        chkLlegit.setFont(UITheme.FONT_BASE);

        JButton btnAfegir = new JButton(I18n.t("btn_afegir_llista"));
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

        JButton btnTreure = new JButton(I18n.t("btn_treure_seleccionada"));
        UITheme.styleSecondaryButton(btnTreure);
        btnTreure.setBackground(UITheme.DANGER);

        JButton btnGuardar = new JButton(I18n.t("btn_guardar_canvis"));
        UITheme.styleAccentButton(btnGuardar);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        btnRow.setBackground(UITheme.BG_PANEL);
        btnRow.add(btnTreure);
        btnRow.add(btnGuardar);

        south.add(addRow, BorderLayout.NORTH);
        south.add(btnRow, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        // ── Wire actions ──────────────────────────────────────────────────────
        btnAfegir.addActionListener(e -> {
            reloadComboAdd();
            if (comboAdd.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    I18n.t("dlg_no_llistes_msg"),
                    I18n.t("dlg_no_llistes_title"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Llista llista = (Llista) comboAdd.getSelectedItem();
            double val = 0.0;
            try { val = Double.parseDouble(txtVal.getText().trim()); } catch (NumberFormatException ignored) {}
            boolean llegit = chkLlegit.isSelected();
            try {
                cd.addLlibreToLlista(llibre.getISBN(), llista.getId(), val, llegit);
                reload();
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
            }
        });

        btnTreure.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            String llistaNom = (String) tableModel.getValueAt(row, COL_NOM);
            Llista target = findLlistaByNom(llistaNom);
            if (target == null) return;
            try {
                cd.removeLlibreFromLlista(llibre.getISBN(), target.getId());
                reload();
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
            }
        });

        btnGuardar.addActionListener(e -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                String llistaNom = (String) tableModel.getValueAt(r, COL_NOM);
                Llista target = findLlistaByNom(llistaNom);
                if (target == null) continue;
                double val = 0.0;
                try { val = Double.parseDouble(tableModel.getValueAt(r, COL_VAL).toString()); } catch (NumberFormatException ignored) {}
                boolean llegit = Boolean.TRUE.equals(tableModel.getValueAt(r, COL_LLEGIT));
                try {
                    cd.updateLlibreInLlista(llibre.getISBN(), target.getId(), val, llegit);
                } catch (Exception ex) {
                    new DialogoError(ex).showErrorMessage();
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, I18n.t("dlg_canvis_guardats"), I18n.t("dlg_config_saved_title"), JOptionPane.INFORMATION_MESSAGE);
        });

        reload();
    }

    private void reloadComboAdd() {
        Llista prev = (Llista) comboAdd.getSelectedItem();
        comboAdd.removeAllItems();
        for (Llista l : cd.getAllLlistes()) comboAdd.addItem(l);
        if (prev != null) {
            for (int i = 0; i < comboAdd.getItemCount(); i++) {
                if (comboAdd.getItemAt(i).getId() == prev.getId()) {
                    comboAdd.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void reload() {
        llistesCache = cd.getLlistesForLlibre(llibre.getISBN());
        tableModel.setRowCount(0);
        for (Llista l : llistesCache) {
            tableModel.addRow(new Object[]{
                l.getNom(),
                String.format("%.1f", l.getValoracioLlibre()),
                l.getLlegitLlibre()
            });
        }
    }

    private Llista findLlistaByNom(String nom) {
        return llistesCache.stream().filter(l -> l.getNom().equals(nom)).findFirst().orElse(null);
    }
}
