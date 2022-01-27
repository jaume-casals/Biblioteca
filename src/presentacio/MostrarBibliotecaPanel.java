package presentacio;

import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;

import herramienta.ConfiguracionPantalla;

import javax.swing.JScrollPane;

public class MostrarBibliotecaPanel extends JPanel {

	private ConfiguracionPantalla configuracionPantalla = new ConfiguracionPantalla();

	private JTable jTableBilio;
	private JScrollPane scrollPaneJTable;
	private JScrollPane scrolpaneFiltro;
	private JPanel panelFiltros;

	private JComboBox<String> comboBoxISBN;
	private JComboBox<String> comboBoxNom;
	private JComboBox<String> comboBoxAutor;
	// DATE
	// no descripcio
	// stars?
	// min max
	private CheckboxGroup checkboxgroup;
	private JCheckBox chckbxLlegit;
	private JCheckBox chckbxNoLlegit;

	private JButton bttnFiltrar;
	private JButton bttnQuitarFiltros;

	
	
	public MostrarBibliotecaPanel() {
		setLayout(null);

		// Table
		scrollPaneJTable = new JScrollPane();

		scrollPaneJTable.setBounds(amplada(50) - 5, altura(0) + 10, amplada(120), altura(52));
		add(scrollPaneJTable);
		jTableBilio = new JTable();
		scrollPaneJTable.setViewportView(jTableBilio);

		// Filtre

		scrolpaneFiltro = new JScrollPane();
		scrolpaneFiltro.setBounds(10, 11, amplada(40), altura(52));
		add(scrolpaneFiltro);

		panelFiltros = new JPanel();
		panelFiltros.setBorder(new LineBorder(Color.LIGHT_GRAY));
		scrolpaneFiltro.setViewportView(panelFiltros);
		panelFiltros.setLayout(null);

		checkboxgroup = new CheckboxGroup();
		chckbxLlegit = new JCheckBox("Llegit");
		chckbxNoLlegit = new JCheckBox("No llegit");

		chckbxLlegit.setBounds(amplada(10), altura(1), amplada(20), altura(2));
		chckbxNoLlegit.setBounds(amplada(10), altura(3), amplada(20), altura(2));

		panelFiltros.add(chckbxLlegit);
		panelFiltros.add(chckbxNoLlegit);

		bttnFiltrar = new JButton("Filtrar");
		bttnFiltrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bttnFiltrar.setBounds(amplada(10), altura(44), amplada(20), altura(2));
		panelFiltros.add(bttnFiltrar);

		bttnQuitarFiltros = new JButton("Quitar filtros");
		bttnQuitarFiltros.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bttnQuitarFiltros.setBounds(amplada(10), altura(48), amplada(20), altura(2));
		panelFiltros.add(bttnQuitarFiltros);

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

	public CheckboxGroup getcheckboxgroup() {
		return checkboxgroup;
	}

	public JButton getbttnFiltrar() {
		return bttnFiltrar;
	}

	public JButton getbttnQuitarFiltros() {
		return bttnQuitarFiltros;
	}

	private int altura(int a) {
		return ((int) configuracionPantalla.getHeight() * a) / 100;
	}

	private int amplada(int a) {
		return ((int) configuracionPantalla.getWidth() * a) / 100;
	}

}
