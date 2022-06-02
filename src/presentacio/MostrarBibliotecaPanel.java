package presentacio;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import herramienta.ConfiguracionPantalla;

public class MostrarBibliotecaPanel extends JPanel {

	private ConfiguracionPantalla configuracionPantalla = new ConfiguracionPantalla();

	int ampladachk = amplada(20);
	int alturachk = altura(2);
	int alturachkllegit = 1;

	int iniciComponent = amplada(3);

	int alturabtnfiltrar = 68;
	int ampladabtn = amplada(16);
	int alturabtn = altura(5);

	private JTable jTableBilio;
	private JScrollPane scrollPaneJTable;
	private JScrollPane scrolpaneFiltro;
	private JPanel panelFiltros;

	private JComboBox<String> comboBoxISBN;
	private JComboBox<String> comboBoxNom;
	private JComboBox<String> comboBoxAutor;
	private JTextField anyMin;
	private JTextField anyMax;
	private JCheckBox chckbxLlegit;
	private JCheckBox chckbxNoLlegit;

	private JButton bttnFiltrar;
	private JButton bttnQuitarFiltros;
	private int comboBoxAlturaPos = 12;
	private int comboboxAltura = altura(4);
	private int comboboxAmplada = amplada(15);

	public MostrarBibliotecaPanel() {
		setLayout(null);

		// Table
		scrollPaneJTable = new JScrollPane();

		scrollPaneJTable.setBounds(amplada(30), 10, amplada(67), altura(83));
		scrollPaneJTable.setForeground(Color.black);
		add(scrollPaneJTable);

		jTableBilio = new JTable();
		jTableBilio.setDefaultEditor(Object.class, null);
		jTableBilio.setAutoCreateRowSorter(true);
		jTableBilio.getTableHeader().setReorderingAllowed(false);

		scrollPaneJTable.setViewportView(jTableBilio);

		// Filtre

		scrolpaneFiltro = new JScrollPane();
		scrolpaneFiltro.setBounds(10, 11, amplada(25), altura(83));
		add(scrolpaneFiltro);

		panelFiltros = new JPanel();
		panelFiltros.setBorder(new LineBorder(Color.LIGHT_GRAY));
		scrolpaneFiltro.setViewportView(panelFiltros);
		panelFiltros.setLayout(null);

		chckbxLlegit = new JCheckBox("Llegit");
		chckbxLlegit.setFont(new Font("Dialog", Font.PLAIN, 15));
		chckbxNoLlegit = new JCheckBox("No llegit");
		chckbxNoLlegit.setFont(new Font("Dialog", Font.PLAIN, 15));

		chckbxLlegit.setBounds(iniciComponent, altura(alturachkllegit), ampladachk, alturachk);
		chckbxNoLlegit.setBounds(iniciComponent, altura(alturachkllegit + 3), ampladachk, alturachk);

		panelFiltros.add(chckbxLlegit);
		panelFiltros.add(chckbxNoLlegit);

		JLabel lblISBN = new JLabel("ISBN");
		lblISBN.setHorizontalAlignment(SwingConstants.CENTER);
		lblISBN.setFont(new Font("Dialog", Font.PLAIN, 19));
		lblISBN.setBounds(iniciComponent - 20, altura(comboBoxAlturaPos - 4), 87, 32);
		panelFiltros.add(lblISBN);

		comboBoxISBN = new JComboBox<String>();
		comboBoxISBN.setToolTipText("");
		comboBoxISBN.setBounds(iniciComponent, altura(comboBoxAlturaPos), comboboxAmplada, comboboxAltura);
		panelFiltros.add(comboBoxISBN);

		JLabel lblNom = new JLabel("Nom");
		lblNom.setHorizontalAlignment(SwingConstants.CENTER);
		lblNom.setFont(new Font("Dialog", Font.PLAIN, 19));
		lblNom.setBounds(iniciComponent - 20, altura(comboBoxAlturaPos + 6), 87, 32);
		panelFiltros.add(lblNom);

		comboBoxNom = new JComboBox<String>();
		comboBoxNom.setBounds(iniciComponent, altura(comboBoxAlturaPos + 10), comboboxAmplada, comboboxAltura);
		comboBoxNom.setToolTipText("");
		panelFiltros.add(comboBoxNom);

		JLabel lblAutor = new JLabel("Autor");
		lblAutor.setHorizontalAlignment(SwingConstants.CENTER);
		lblAutor.setFont(new Font("Dialog", Font.PLAIN, 19));
		lblAutor.setBounds(iniciComponent - 15, altura(comboBoxAlturaPos + 17), 87, 32);
		panelFiltros.add(lblAutor);

		comboBoxAutor = new JComboBox<String>();
		comboBoxAutor.setBounds(iniciComponent, altura(comboBoxAlturaPos + 21), comboboxAmplada, comboboxAltura);
		comboBoxAutor.setToolTipText("");
		panelFiltros.add(comboBoxAutor);

		bttnFiltrar = new JButton("Filtrar");
		bttnFiltrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bttnFiltrar.setBounds(iniciComponent, altura(alturabtnfiltrar), ampladabtn, alturabtn);
		panelFiltros.add(bttnFiltrar);

		bttnQuitarFiltros = new JButton("Quitar filtros");
		bttnQuitarFiltros.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bttnQuitarFiltros.setBounds(iniciComponent, altura(alturabtnfiltrar + 7), ampladabtn, alturabtn);
		panelFiltros.add(bttnQuitarFiltros);

	}

	public JComboBox<String> getComboBoxISBN() {
		return comboBoxISBN;
	}

	public JComboBox<String> getComboBoxNom() {
		return comboBoxNom;
	}

	public JComboBox<String> getComboBoxAutor() {
		return comboBoxAutor;
	}

	public JTable getjTableBilio() {
		return jTableBilio;
	}

	public JPanel getPanelFiltros() {
		return panelFiltros;
	}

	public JScrollPane getScrollPaneJTable() {
		return scrollPaneJTable;
	}

	public JScrollPane getScrolpaneFiltro() {
		return scrolpaneFiltro;
	}

	public JCheckBox getchckbxLlegit() {
		return chckbxLlegit;
	}

	public JCheckBox getchckbxNoLlegit() {
		return chckbxNoLlegit;
	}

	public JButton getbtnFiltrar() {
		return bttnFiltrar;
	}

	public JButton getbttnQuitarFiltros() {
		return bttnQuitarFiltros;
	}

	private int altura(double a) {
		return (int) (((int) configuracionPantalla.getHeight() * a) / 100);
	}

	private int amplada(int a) {
		return ((int) configuracionPantalla.getWidth() * a) / 100;
	}

}
