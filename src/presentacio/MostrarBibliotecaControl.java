package presentacio;

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
		
//		this.biblio = biblio;
//		this.header = header;
		
		System.out.println("EE");
		
//		this.vista.getbttnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnFiltrar().addActionListener(e -> quitarFiltros());
	}

//	public void ns() {
//
//		((DefaultTableModel) this.vista.getTable().getModel()).setRowCount(0);
//		DefaultTableModel model = new DefaultTableModel(header, 0);
//		Llibre arr[] = new Llibre[biblio.size()];
//
//		for (int i = 0; i < biblio.size(); i++) {
//			arr[i] = biblio.get(i);
//		}
//
//		for (int i = 0; i < arr.length; i++) {
//			System.out.println(arr[i]);
//		}
//
//		model.addRow(arr);
//
//		this.vista.getTable().setModel(model);
//
//	}

	public JPanel view() {
		return this.vista;
	}

	private void quitarFiltros() {
		System.out.println("AAAA");
		this.vista.getbttnFiltrar().setSelected(false);
		this.vista.getbttnQuitarFiltros().setSelected(false);
	}
}
