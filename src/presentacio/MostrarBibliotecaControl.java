package presentacio;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import domini.Llibre;
import herramienta.AutoCompletion;
import herramienta.DialogoError;
import presentacio.detalles.DialogoDetalles;

public class MostrarBibliotecaControl {

	private MostrarBibliotecaPanel vista;
	private ArrayList<Llibre> biblio;
	private String header[];
	private DefaultTableModel model;
	private JButton botonDetalles;

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, ArrayList<Llibre> biblio, String header[]) {
		this.vista = vista;
		this.botonDetalles = new JButton();

		String[] a = { "1", "2", "3", "4", "5", "6", "7", "8" };
		ArrayList<Llibre> b = new ArrayList<Llibre>();
		b.add(new Llibre(1, "a", "2", 1, "sadfasdfasd", 2.3, 2.3, true));
		b.add(new Llibre(2, "s", "3", 2, "sadfasdfasdf", 2.3, 2.3, true));
		b.add(new Llibre(4, "d", "5", 4, "asdfasdfasdfas", 2.3, 2.3, true));
		b.add(new Llibre(5, "f", "6", 5, "dfasdfasdfasdf", 2.3, 2.3, true));
		b.add(new Llibre(6, "g", "7", 6, "asdfasdfasdf", 2.3, 2.3, true));
		b.add(new Llibre(6, "h", "7", 6, "dfasdfasdfasdf", 2.3, 2.3, true));
		b.add(new Llibre(6, "j", "8", 6, "asdfasdfasdfas", 2.3, 2.3, true));
		b.add(new Llibre(6, "k", "9", 6, "dfasdfasdfasdf", 2.3, 2.3, true));
		b.add(new Llibre(6, "l", "0", 6, "asdfasdfasdfasd", 2.3, 2.3, true));
		b.add(new Llibre(6, "ñ", "'", 6, "asdfasdfasdfasdf", 2.3, 2.3, true));
		b.add(new Llibre(6, "f", "8", 6, "asdfasdfasdfasdf", 2.3, 2.3, true));
		b.add(new Llibre(6, "g", "7", 6, "asdfasdfasdf", 2.3, 2.3, true));
		this.biblio = b;
		this.header = a;

		AutoCompletion.enable(this.vista.getComboBoxISBN());
		AutoCompletion.enable(this.vista.getComboBoxAutor());
		AutoCompletion.enable(this.vista.getComboBoxNom());

		this.vista.getbtnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
		this.botonDetalles.addActionListener(e -> abrirDetallesLlibres());
		this.vista.getjTableBilio().addMouseListener(abrirDetalles());

		setTable(this.biblio);
	}

	public MouseAdapter abrirDetalles() {

		MouseAdapter open = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				JTable table = (JTable) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
					abrirDetallesLlibres();
				}
			}
		};
		return open;

	}

	private void abrirDetallesLlibres() {
		try {
			DialogoDetalles detalles = new DialogoDetalles();
			detalles.abrirDetalles(
					this.vista.getjTableBilio().getValueAt(this.vista.getjTableBilio().getSelectedRow(), 0));
			detalles.setLocationRelativeTo(this.vista);
			detalles.setVisible(true);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	public void setHeader(String[] s) {
		this.vista.getjTableBilio().setModel(new DefaultTableModel(s, 0));
	}

	public Object[] addLlibreMostrar(Llibre l) {
		String estado = "";
		if (l.getLlegit() == true)
			estado = "Llegit";
		else if (l.getLlegit() == false)
			estado = "No llegit";

		return new Object[] { l.getISBN() + "", l.getNom(), l.getAutor(), l.getAny(), l.getValoracio(), l.getPreu(),
				estado, "" };

	}

	public void setTable(ArrayList<Llibre> llibres) {
		this.model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		this.model.getDataVector().removeAllElements();
//		this.modeloOrdenado = new TableRowSorter<TableModel>(this.model);
		this.vista.getjTableBilio().setRowSorter(null);
		setHeader(new String[] { "ISBN", "Nom", "Autor", "Any", "Valoracio", "Preu", "Llegit", "Detalls" });
		if (this.biblio != null) {
//			this.vista.getComboBoxNIF().addItem(Messages.getString("TODO")); 
//			this.vista.getComboBoxNombreCompleto().addItem(Messages.getString("TODO")); 
			if (this.biblio.size() <= 100) {
				for (Llibre l : llibres) {
					addRow(addLlibreMostrar(l));
//					anadirComboBoxTrabajador(l);
				}
			} else {
				for (Llibre l : llibres) {
					addRow(addLlibreMostrar(l));
//					anadirComboBoxTrabajador(t);
				}
			}
		}
		this.vista.getjTableBilio().getColumnModel().getColumn(7).setMaxWidth(80);
		this.vista.getjTableBilio().getColumnModel().getColumn(7).setCellRenderer(new BotonDetallesRenderer());
		this.vista.getjTableBilio().getColumnModel().getColumn(7)
				.setCellEditor(new BotonDetallesEditor(new JCheckBox()));
	}

	public void addRow(Object... ob) {
		String[] string = new String[ob.length];

		for (int i = 0; i < ob.length; i++) {
			string[i] = ob[i].toString();
		}
		this.vista.getjTableBilio().setRowHeight(30);
		if (this.vista.getjTableBilio().getModel().getColumnCount() == string.length) {
			((DefaultTableModel) this.vista.getjTableBilio().getModel()).addRow(string);
		} else {
//			JOptionPane.showMessageDialog(vista, "Cuidado que hay diferentes columnas en la tabla de Libros", "Error",
//					0, null);
		}
		this.vista.getjTableBilio().getColumnModel().getColumn(0).setMaxWidth(50);
		this.vista.getjTableBilio().getColumnModel().getColumn(0).setMinWidth(50);
		this.vista.getjTableBilio().getColumnModel().getColumn(1).setMaxWidth(90);
		this.vista.getjTableBilio().getColumnModel().getColumn(1).setMinWidth(90);
		this.vista.getjTableBilio().getColumnModel().getColumn(5).setMaxWidth(90);
		this.vista.getjTableBilio().getColumnModel().getColumn(5).setMinWidth(90);
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

	public class BotonDetallesEditor extends DefaultCellEditor {

		public BotonDetallesEditor(JCheckBox checkbox) {
			super(checkbox);
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			if (isSelected) {
				botonDetalles.setForeground(table.getSelectionForeground());
				botonDetalles.setBackground(table.getSelectionBackground());
			} else {
				botonDetalles.setForeground(table.getForeground());
				botonDetalles.setBackground(table.getBackground());
			}
			botonDetalles.setText("Detalles");
			return botonDetalles;
		}
	}

	public class BotonDetallesRenderer extends JButton implements TableCellRenderer {

		public BotonDetallesRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(UIManager.getColor("Button.background"));
			}
			setText("Detalles");
			return this;
		}
	}

}
