package presentacio;

import java.awt.Color;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import domini.Llibre;

public class MostrarBibliotecaControl {

	private MostrarBibliotecaPanel vista;
	private List<Llibre> biblio;
	private String header[];

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, List<Llibre> biblio, String header[]) {
		this.vista = vista;

		this.biblio = biblio;
		this.header = header;

		repaint(Color.DARK_GRAY);

		this.vista.getbttnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
	}

	public void repaint(Color c) {
		this.vista.setBackground(c);
		this.vista.getjTableBilio().setBackground(c);
		this.vista.getScrollPaneJTable().setBackground(c);
		this.vista.getScrolpaneFiltro().setBackground(c);
		this.vista.getPanelFiltros().setBackground(c);
	}

	public void ns() {

		((DefaultTableModel) this.vista.getjTableBilio().getModel()).setRowCount(0);
		DefaultTableModel model = new DefaultTableModel(header, 0);
		Llibre arr[] = new Llibre[biblio.size()];

		for (int i = 0; i < biblio.size(); i++) {
			arr[i] = biblio.get(i);
		}

		for (int i = 0; i < arr.length; i++) {
			System.out.println(arr[i]);
		}

		model.addRow(arr);

		this.vista.getjTableBilio().setModel(model);

	}

	public JPanel view() {
		return this.vista;
	}

	private void filtrar() {
	}

	private void quitarFiltros() {
		System.out.println("quito");
		this.vista.getbttnFiltrar().setSelected(false);
		this.vista.getbttnQuitarFiltros().setSelected(false);
	}
}
