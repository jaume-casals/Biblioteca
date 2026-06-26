package presentacio.detalles.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import herramienta.i18n.I18n;
import herramienta.ui.UITheme;
import presentacio.util.UIComponents;

abstract class DialegEntitatLlibre<E> extends JDialog {

    protected final JPanel rootPanel;
    protected final JTable table;
    protected final JComboBox<E> comboAdd;
    protected final JButton btnAfegir;
    protected final JButton btnTreure;

    protected DialegEntitatLlibre(Window owner, String title, int width, int height,
                                  TableModel tableModel, int rowHeight) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setSize(width, height);
        setLocationRelativeTo(owner);
        setResizable(false);

        rootPanel = new JPanel(new BorderLayout(8, 8));
        rootPanel.setBackground(UITheme.palette().bgPanel());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(rootPanel);

        table = new JTable(tableModel);
        styleEntityTable(table, rowHeight);

        comboAdd = new JComboBox<>();
        comboAdd.setPreferredSize(new Dimension(180, 30));

        btnAfegir = new JButton();
        UIComponents.styleAccentButton(btnAfegir);

        btnTreure = new JButton(I18n.t("btn_treure_seleccionada"));
        UIComponents.styleSecondaryButton(btnTreure);
        btnTreure.setBackground(UITheme.palette().danger());
    }

    protected static void styleEntityTable(JTable table, int rowHeight) {
        table.setBackground(UITheme.palette().bgPanel());
        table.setForeground(UITheme.palette().textDark());
        table.setRowHeight(rowHeight);
        table.setFont(UITheme.fontBase());
        table.setSelectionBackground(UITheme.palette().accent());
        table.setSelectionForeground(Color.WHITE);
    }

    public JTable obtenirTable() { return table; }
    public JComboBox<E> obtenirComboAdd() { return comboAdd; }
    public JButton obtenirBtnAfegir() { return btnAfegir; }
    public JButton obtenirBtnTreure() { return btnTreure; }
}
