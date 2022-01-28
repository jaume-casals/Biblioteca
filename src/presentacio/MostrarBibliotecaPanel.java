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

	int ampladachk = amplada(20);
	int alturachk = altura(2);
	int alturachkllegit = 1;

	int iniciComponent = amplada(2);

	int alturabtnfiltrar = 68;
	int ampladabtn = amplada(16);
	int alturabtn = altura(5);

//	
//	
//	
//	
//	
//	
//	

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

		scrollPaneJTable.setBounds(amplada(45), 10, amplada(52), altura(83));
		add(scrollPaneJTable);
		jTableBilio = new JTable();
		scrollPaneJTable.setViewportView(jTableBilio);

		// Filtre

		scrolpaneFiltro = new JScrollPane();
		scrolpaneFiltro.setBounds(10, 11, amplada(40), altura(83));
		add(scrolpaneFiltro);

		panelFiltros = new JPanel();
		panelFiltros.setBorder(new LineBorder(Color.LIGHT_GRAY));
		scrolpaneFiltro.setViewportView(panelFiltros);
		panelFiltros.setLayout(null);

		checkboxgroup = new CheckboxGroup();
		chckbxLlegit = new JCheckBox("Llegit");

		chckbxNoLlegit = new JCheckBox("No llegit");

		chckbxLlegit.setBounds(iniciComponent, altura(alturachkllegit), ampladachk, alturachk);
		chckbxNoLlegit.setBounds(iniciComponent, altura(alturachkllegit + 3), ampladachk, alturachk);

		panelFiltros.add(chckbxLlegit);
		panelFiltros.add(chckbxNoLlegit);

		int a = 15;
		int b = altura(4);
		int c = amplada(15);

		comboBoxISBN = new JComboBox<String>();
		comboBoxISBN.setToolTipText("");
		comboBoxISBN.setBounds(iniciComponent, altura(a), c, b);
		panelFiltros.add(comboBoxISBN);

		comboBoxNom = new JComboBox<String>();
		comboBoxNom.setBounds(iniciComponent, altura(a + 12), c, b);
		comboBoxNom.setToolTipText("");
		panelFiltros.add(comboBoxNom);

		comboBoxAutor = new JComboBox<String>();
		comboBoxAutor.setBounds(iniciComponent, altura(a + 24), c, b);
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
