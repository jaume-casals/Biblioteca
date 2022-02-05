package presentacio;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.AutoCompletion;
import herramienta.DialogoError;
import presentacio.detalles.DialogoDetalles;

public class MostrarBibliotecaControl {

	private ControladorDomini cLlibres;
	private MostrarBibliotecaPanel vista;
	private ArrayList<Llibre> biblio;
	private DefaultTableModel model;
	private JButton botonDetalles;

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, ArrayList<Llibre> biblio) {
		this.vista = vista;
		this.botonDetalles = new JButton();
		cLlibres = ControladorDomini.getInstance();
		this.biblio = biblio;

		AutoCompletion.enable(this.vista.getComboBoxISBN());
		AutoCompletion.enable(this.vista.getComboBoxAutor());
		AutoCompletion.enable(this.vista.getComboBoxNom());

		this.vista.getbtnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
		this.botonDetalles.addActionListener(e -> abrirDetallesLlibres());
		this.vista.getjTableBilio().addMouseListener(abrirDetalles());
		this.vista.getchckbxLlegit().addItemListener(e -> enLlegitSeleccionado(e));
		this.vista.getchckbxNoLlegit().addItemListener(e -> enNoLlegitSeleccionado(e));

		setTable(this.biblio);
	}

	private void enNoLlegitSeleccionado(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (this.vista.getchckbxLlegit().isSelected())
				this.vista.getchckbxLlegit().setSelected(false);
		}

	}

	private void enLlegitSeleccionado(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (this.vista.getchckbxNoLlegit().isSelected())
				this.vista.getchckbxNoLlegit().setSelected(false);
		}
	}

	private MouseAdapter abrirDetalles() {

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
			detalles.abrirDetalles(MainFrameControl.getInstance(null).getLlibreIsbn(Integer.parseInt(
					(String) this.vista.getjTableBilio().getValueAt(this.vista.getjTableBilio().getSelectedRow(), 0))));
			detalles.setLocationRelativeTo(this.vista);
			detalles.setVisible(true);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private void setHeader(String[] s) {
		this.vista.getjTableBilio().setModel(new DefaultTableModel(s, 0));
	}

	private Object[] addLlibreMostrar(Llibre l) {
		String estado = "";
		if (l.getLlegit() == true)
			estado = "Llegit";
		else if (l.getLlegit() == false)
			estado = "No llegit";

		return new Object[] { l.getISBN() + "", l.getNom(), l.getAutor(), l.getAny(), l.getValoracio(), l.getPreu(),
				estado, "" };

	}

	private void setTable(ArrayList<Llibre> llibres) {
		this.model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		this.model.getDataVector().removeAllElements();
		removeAlldataFiltros();
		setHeader(new String[] { "ISBN", "Nom", "Autor", "Any", "Valoracio", "Preu", "Llegit", "Detalls" });
		if (this.biblio != null) {
			this.vista.getComboBoxISBN().addItem("");
			this.vista.getComboBoxNom().addItem("");
			this.vista.getComboBoxAutor().addItem("");
			if (this.biblio.size() <= 100) {
				for (Llibre l : llibres) {
					addRow(addLlibreMostrar(l));
					anadirComboBoxLlibre(l);
				}
			} else {
				for (Llibre l : llibres) {
					addRow(addLlibreMostrar(l));
					anadirComboBoxLlibre(l);
				}
			}
		}
		this.vista.getjTableBilio().getColumnModel().getColumn(7).setMaxWidth(160);
		this.vista.getjTableBilio().getColumnModel().getColumn(0).setMinWidth(160);
		this.vista.getjTableBilio().getColumnModel().getColumn(7).setCellRenderer(new BotonDetallesRenderer());
		this.vista.getjTableBilio().getColumnModel().getColumn(7)
				.setCellEditor(new BotonDetallesEditor(new JCheckBox()));
	}

	private void removeAlldataFiltros() {
		this.vista.getComboBoxAutor().removeAllItems();
		this.vista.getComboBoxISBN().removeAllItems();
		this.vista.getComboBoxNom().removeAllItems();
	}

	private void anadirComboBoxLlibre(Llibre l) {
		this.vista.getComboBoxISBN().addItem(l.getISBN() + "");
		this.vista.getComboBoxAutor().addItem(l.getAutor());
		this.vista.getComboBoxNom().addItem(l.getNom());
	}

	private void addRow(Object... ob) {
		String[] string = new String[ob.length];

		for (int i = 0; i < ob.length; i++) {
			string[i] = ob[i].toString();
		}
		this.vista.getjTableBilio().setRowHeight(30);
		if (this.vista.getjTableBilio().getModel().getColumnCount() == string.length) {
			((DefaultTableModel) this.vista.getjTableBilio().getModel()).addRow(string);
		} else {
			JOptionPane.showMessageDialog(vista, "Cuidado que hay diferentes columnas en la tabla de Libros", "Error",
					0, null);
		}
		this.vista.getjTableBilio().getColumnModel().getColumn(0).setMaxWidth(110);
		this.vista.getjTableBilio().getColumnModel().getColumn(0).setMinWidth(110);

		this.vista.getjTableBilio().getColumnModel().getColumn(1).setMaxWidth(330);
		this.vista.getjTableBilio().getColumnModel().getColumn(1).setMinWidth(190);

		this.vista.getjTableBilio().getColumnModel().getColumn(2).setMaxWidth(330);
		this.vista.getjTableBilio().getColumnModel().getColumn(2).setMinWidth(190);

		this.vista.getjTableBilio().getColumnModel().getColumn(3).setMaxWidth(60);
		this.vista.getjTableBilio().getColumnModel().getColumn(3).setMinWidth(60);

		this.vista.getjTableBilio().getColumnModel().getColumn(4).setMaxWidth(80);
		this.vista.getjTableBilio().getColumnModel().getColumn(4).setMinWidth(80);

		this.vista.getjTableBilio().getColumnModel().getColumn(5).setMaxWidth(50);
		this.vista.getjTableBilio().getColumnModel().getColumn(5).setMinWidth(50);

		this.vista.getjTableBilio().getColumnModel().getColumn(6).setMaxWidth(90);
		this.vista.getjTableBilio().getColumnModel().getColumn(6).setMinWidth(90);
	}

	public JPanel view() {
		return this.vista;
	}

	private void filtrar() {
		String nomAutor = null;
		String nomLlibre = null;
		Integer ISBN = null;
		Integer iniciAny = null;
		Integer fiAny = null;
		Boolean llegit = null;

		if (!this.vista.getComboBoxAutor().getSelectedItem().toString().equals("")) {
			nomAutor = this.vista.getComboBoxAutor().getSelectedItem().toString();
		}
		if (!this.vista.getComboBoxNom().getSelectedItem().toString().equals("")) {
			nomLlibre = this.vista.getComboBoxNom().getSelectedItem().toString();
		}

		if (!this.vista.getComboBoxISBN().getSelectedItem().toString().equals("")) {
			ISBN = Integer.parseInt(this.vista.getComboBoxISBN().getSelectedItem().toString());
		}

		iniciAny = null;

		fiAny = null;

		if (this.vista.getchckbxLlegit().isSelected()) {
			llegit = true;
		}
		if (this.vista.getchckbxNoLlegit().isSelected()) {
			llegit = false;
		}

		setTable(MainFrameControl.getInstance(null).aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny));
	}

	private void quitarFiltros() {
		this.vista.getchckbxLlegit().setSelected(false);
		this.vista.getchckbxNoLlegit().setSelected(false);
		removeAlldataFiltros();
		setTable(biblio);

	}

	private class BotonDetallesEditor extends DefaultCellEditor {

		private BotonDetallesEditor(JCheckBox checkbox) {
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

	private class BotonDetallesRenderer extends JButton implements TableCellRenderer {

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