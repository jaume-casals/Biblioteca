package presentacio;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import domini.Llibre;

public class TableController {

	private MostrarBibliotecaPanel view;
	private List<Llibre> biblio;
	private String header[];

	public TableController(MostrarBibliotecaPanel view, List<Llibre> biblio, String header[]) {
		this.view = view;
		this.biblio = biblio;
		this.header = header;
	}

	public void ns() {

		((DefaultTableModel) view.getTable().getModel()).setRowCount(0);
		DefaultTableModel model = new DefaultTableModel(header, 0);
		Llibre arr[] = new Llibre[biblio.size()];

		for (int i = 0; i < biblio.size(); i++) {
			arr[i] = biblio.get(i);
		}

		for (int i = 0; i < arr.length; i++) {
			System.out.println(arr[i]);
		}

		model.addRow(arr);

		view.getTable().setModel(model);

	}

	public JPanel view() {
		return this.view;
	}
}
