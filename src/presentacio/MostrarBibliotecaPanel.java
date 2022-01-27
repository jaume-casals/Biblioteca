package presentacio;

import java.awt.Toolkit;

import javax.swing.JPanel;
import javax.swing.JTable;

import herramienta.ConfiguracionPantalla;

import javax.swing.JScrollPane;

public class MostrarBibliotecaPanel extends JPanel {
	private JTable table;
	private JScrollPane scrollPane;

	private ConfiguracionPantalla configuracionPantalla = new ConfiguracionPantalla();

	public MostrarBibliotecaPanel() {
		setLayout(null);
		scrollPane = new JScrollPane();

		scrollPane.setBounds(amplada(50)-5, altura(0)+10, amplada(120), altura(52));
		add(scrollPane);

		table = new JTable();
		scrollPane.setViewportView(table);
		// prova

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
