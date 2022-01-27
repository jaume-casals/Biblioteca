package presentacio;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;

import herramienta.ConfiguracionPantalla;

import javax.swing.JScrollPane;

public class MostrarBibliotecaPanel extends JPanel {

	private ConfiguracionPantalla configuracionPantalla = new ConfiguracionPantalla();

	private JTable table;
	private JScrollPane scrollPane;
	private JScrollPane scrolpaneFiltro;
	private JPanel panelFiltros;

	private JButton bttnFiltrar;
	private JButton bttnQuitarFiltros;

	public MostrarBibliotecaPanel() {
		setLayout(null);

		// Table
		scrollPane = new JScrollPane();

		scrollPane.setBounds(amplada(50) - 5, altura(0) + 10, amplada(120), altura(52));
		add(scrollPane);

		table = new JTable();
		scrollPane.setViewportView(table);
		// Filtre

		scrolpaneFiltro = new JScrollPane();
		scrolpaneFiltro.setBounds(10, 11, amplada(40), altura(52));
		add(scrolpaneFiltro);

		panelFiltros = new JPanel();
		panelFiltros.setBorder(new LineBorder(Color.LIGHT_GRAY));
		scrolpaneFiltro.setViewportView(panelFiltros);
		panelFiltros.setLayout(null);

		bttnFiltrar = new JButton("Filtrar");
		bttnFiltrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bttnFiltrar.setBounds(amplada(10), altura(44), amplada(20), altura(2));
		panelFiltros.add(bttnFiltrar);

		bttnQuitarFiltros = new JButton("Quitar filtros");
		bttnQuitarFiltros.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bttnQuitarFiltros.setBounds(amplada(10), altura(48), amplada(20), altura(2));
		panelFiltros.add(bttnQuitarFiltros);

	}

	public JTable getTable() {
		return table;
	}

	private int altura(int a) {
		return ((int) configuracionPantalla.getHeight() * a) / 100;
	}

	private int amplada(int a) {
		return ((int) configuracionPantalla.getWidth() * a) / 100;
	}
}
