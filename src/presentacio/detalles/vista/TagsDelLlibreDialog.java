package presentacio.detalles.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import domini.ControladorDomini;
import domini.Llibre;
import domini.Tag;
import herramienta.DialogoError;
import herramienta.UITheme;

public class TagsDelLlibreDialog extends JDialog {

    private final Llibre llibre;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private ArrayList<Tag> tagsCache = new ArrayList<>();
    private JComboBox<Tag> comboAdd;

    public TagsDelLlibreDialog(Window owner, Llibre llibre) {
        super(owner, "Etiquetes de: " + llibre.getNom(), ModalityType.APPLICATION_MODAL);
        this.llibre = llibre;
        setSize(400, 380);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel);

        tableModel = new DefaultTableModel(new String[]{"Etiqueta"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setBackground(UITheme.BG_PANEL);
        table.setForeground(UITheme.TEXT_DARK);
        table.setRowHeight(28);
        table.setFont(UITheme.FONT_BASE);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(4, 8));
        south.setBackground(UITheme.BG_PANEL);

        comboAdd = new JComboBox<>();
        reloadComboAdd();
        comboAdd.setPreferredSize(new Dimension(180, 30));

        JButton btnAfegir = new JButton("Afegir etiqueta");
        UITheme.styleAccentButton(btnAfegir);

        JTextField txtNovaEtiqueta = new JTextField(12);
        UITheme.styleField(txtNovaEtiqueta);
        txtNovaEtiqueta.setToolTipText("Nom nova etiqueta");

        JButton btnCrear = new JButton("Crear");
        UITheme.styleSecondaryButton(btnCrear);

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addRow.setBackground(UITheme.BG_PANEL);
        JLabel lblEtiq = new JLabel("Etiqueta:");
        UITheme.styleLabel(lblEtiq);
        addRow.add(lblEtiq);
        addRow.add(comboAdd);
        addRow.add(btnAfegir);

        JPanel createRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        createRow.setBackground(UITheme.BG_PANEL);
        JLabel lblNova = new JLabel("Nova:");
        UITheme.styleLabel(lblNova);
        createRow.add(lblNova);
        createRow.add(txtNovaEtiqueta);
        createRow.add(btnCrear);

        JButton btnTreure = new JButton("Treure seleccionada");
        UITheme.styleSecondaryButton(btnTreure);
        btnTreure.setBackground(UITheme.DANGER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        btnRow.setBackground(UITheme.BG_PANEL);
        btnRow.add(btnTreure);

        JPanel addArea = new JPanel(new BorderLayout(0, 2));
        addArea.setBackground(UITheme.BG_PANEL);
        addArea.add(addRow, BorderLayout.NORTH);
        addArea.add(createRow, BorderLayout.CENTER);
        addArea.add(btnRow, BorderLayout.SOUTH);

        south.add(addArea, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);

        btnCrear.addActionListener(e -> {
            String nom = txtNovaEtiqueta.getText().trim();
            if (nom.isEmpty()) return;
            try {
                ControladorDomini.getInstance().addTag(nom);
                txtNovaEtiqueta.setText("");
                reloadComboAdd();
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
            }
        });

        btnAfegir.addActionListener(e -> {
            reloadComboAdd();
            if (comboAdd.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    "No hi ha cap etiqueta. Crea'n una primer.",
                    "Sense etiquetes", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Tag tag = (Tag) comboAdd.getSelectedItem();
            if (tag == null) return;
            if (tagsCache.stream().anyMatch(t -> t.getId() == tag.getId())) {
                JOptionPane.showMessageDialog(this,
                    "El llibre ja te l'etiqueta \"" + tag.getNom() + "\".",
                    "Duplicat", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                ControladorDomini.getInstance().addLlibreToTag(llibre.getISBN(), tag.getId());
                reload();
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
            }
        });

        btnTreure.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Tag target = tagsCache.get(row);
            try {
                ControladorDomini.getInstance().removeLlibreFromTag(llibre.getISBN(), target.getId());
                reload();
            } catch (Exception ex) {
                new DialogoError(ex).showErrorMessage();
            }
        });

        reload();
    }

    private void reloadComboAdd() {
        Tag prev = (Tag) comboAdd.getSelectedItem();
        comboAdd.removeAllItems();
        for (Tag t : ControladorDomini.getInstance().getAllTags()) comboAdd.addItem(t);
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
        tagsCache = ControladorDomini.getInstance().getTagsForLlibre(llibre.getISBN());
        tableModel.setRowCount(0);
        for (Tag t : tagsCache) tableModel.addRow(new Object[]{ t.getNom() });
    }
}
