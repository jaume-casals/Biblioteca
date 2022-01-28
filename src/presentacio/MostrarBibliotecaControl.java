package presentacio;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import domini.Llibre;

public class MostrarBibliotecaControl {

	private MostrarBibliotecaPanel vista;
	private List<Llibre> biblio;
	private String header[];

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, List<Llibre> biblio, String header[]) {
		this.vista = vista;

		this.biblio = biblio;
		this.header = header;

		this.vista.getbtnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
	}

	public void setTable() {

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
		this.vista.getchckbxLlegit().setSelected(false);

		this.vista.getchckbxNoLlegit().setSelected(false);
	}

}
