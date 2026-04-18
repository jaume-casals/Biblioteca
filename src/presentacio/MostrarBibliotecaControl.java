package presentacio;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import java.awt.Color;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import herramienta.UITheme;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.AutoCompletion;
import herramienta.DialogoError;
import interficie.EnActualizarBBDD;
import presentacio.detalles.control.DetallesLlibrePanelControl;

public class MostrarBibliotecaControl {

	private static final int COLUMNA_ISBN = 0;
	private static final int COLUMNA_NOM = 1;
	private static final int COLUMNA_AUTOR = 2;
	private static final int COLUMNA_ANY = 3;
	private static final int COLUMNA_VALORACIO = 4;
	private static final int COLUMNA_PREU = 5;
	private static final int COLUMNA_lLEGIT = 6;
	private static final int COLUMNA_DETALLS = 7;

//	private ControladorDomini cLlibres;
	private MostrarBibliotecaPanel vista;
	private ArrayList<Llibre> biblio;
	private DefaultTableModel model;
	private JButton botonDetalles;
	private EnActualizarBBDD enActualizarBBDD;

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, ArrayList<Llibre> biblio,
			EnActualizarBBDD enActualizarBBDD) {
		this.vista = vista;
		this.botonDetalles = new JButton();
		UITheme.styleAccentButton(this.botonDetalles);
//		cLlibres = ControladorDomini.getInstance();
		this.biblio = biblio;
		this.enActualizarBBDD = enActualizarBBDD;

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

			Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(Long.parseLong(
					(String) this.vista.getjTableBilio().getValueAt(this.vista.getjTableBilio().getSelectedRow(), 0)));

			DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, enActualizarBBDD);

			detalles.getDetallesLlibrePanel().setLocationRelativeTo(this.vista);
			detalles.getDetallesLlibrePanel().setVisible(true);
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
		JTable t = this.vista.getjTableBilio();
		t.getColumnModel().getColumn(COLUMNA_ISBN).setPreferredWidth(130);
		t.getColumnModel().getColumn(COLUMNA_ISBN).setMinWidth(80);
		t.getColumnModel().getColumn(COLUMNA_NOM).setPreferredWidth(220);
		t.getColumnModel().getColumn(COLUMNA_NOM).setMinWidth(80);
		t.getColumnModel().getColumn(COLUMNA_AUTOR).setPreferredWidth(180);
		t.getColumnModel().getColumn(COLUMNA_AUTOR).setMinWidth(80);
		t.getColumnModel().getColumn(COLUMNA_ANY).setPreferredWidth(55);
		t.getColumnModel().getColumn(COLUMNA_ANY).setMinWidth(40);
		t.getColumnModel().getColumn(COLUMNA_VALORACIO).setPreferredWidth(75);
		t.getColumnModel().getColumn(COLUMNA_VALORACIO).setMinWidth(50);
		t.getColumnModel().getColumn(COLUMNA_PREU).setPreferredWidth(60);
		t.getColumnModel().getColumn(COLUMNA_PREU).setMinWidth(40);
		t.getColumnModel().getColumn(COLUMNA_lLEGIT).setPreferredWidth(80);
		t.getColumnModel().getColumn(COLUMNA_lLEGIT).setMinWidth(55);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setPreferredWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMinWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMaxWidth(110);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellRenderer(new BotonDetallesRenderer());
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellEditor(new BotonDetallesEditor(new JCheckBox()));
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
		if (this.vista.getjTableBilio().getModel().getColumnCount() == string.length) {
			((DefaultTableModel) this.vista.getjTableBilio().getModel()).addRow(string);
		} else {
			JOptionPane.showMessageDialog(vista, "Cuidado que hay diferentes columnas en la tabla de Libros", "Error",
					0, null);
		}
	}

	public JPanel view() {
		return this.vista;
	}

	private void filtrar() {
		String nomAutor = null;
		String nomLlibre = null;
		Long ISBN = null;
		Integer iniciAny = null;
		Integer fiAny = null;
		Double valoracioMin = null;
		Double valoracioMax = null;
		Double preuMin = null;
		Double preuMax = null;
		Boolean llegit = null;

		if (!this.vista.getComboBoxAutor().getSelectedItem().toString().equals(""))
			nomAutor = this.vista.getComboBoxAutor().getSelectedItem().toString();
		if (!this.vista.getComboBoxNom().getSelectedItem().toString().equals(""))
			nomLlibre = this.vista.getComboBoxNom().getSelectedItem().toString();
		if (!this.vista.getComboBoxISBN().getSelectedItem().toString().equals(""))
			ISBN = Long.parseLong(this.vista.getComboBoxISBN().getSelectedItem().toString());

		try { iniciAny = Integer.parseInt(this.vista.getAnyMin().getText().trim()); } catch (NumberFormatException e) {}
		try { fiAny    = Integer.parseInt(this.vista.getAnyMax().getText().trim()); } catch (NumberFormatException e) {}
		try { valoracioMin = Double.parseDouble(this.vista.getValoracioMin().getText().trim()); } catch (NumberFormatException e) {}
		try { valoracioMax = Double.parseDouble(this.vista.getValoracioMax().getText().trim()); } catch (NumberFormatException e) {}
		try { preuMin = Double.parseDouble(this.vista.getPreuMin().getText().trim()); } catch (NumberFormatException e) {}
		try { preuMax = Double.parseDouble(this.vista.getPreuMax().getText().trim()); } catch (NumberFormatException e) {}

		if (this.vista.getchckbxLlegit().isSelected())   llegit = true;
		if (this.vista.getchckbxNoLlegit().isSelected())  llegit = false;

		setTable(MainFrameControl.getInstance(null).aplicarFiltres(
			nomAutor, nomLlibre, ISBN, iniciAny, fiAny, valoracioMin, valoracioMax, preuMin, preuMax, llegit));
	}

	private void quitarFiltros() {
		this.vista.getchckbxLlegit().setSelected(false);
		this.vista.getchckbxNoLlegit().setSelected(false);
		this.vista.getAnyMin().setText("");
		this.vista.getAnyMax().setText("");
		this.vista.getValoracioMin().setText("");
		this.vista.getValoracioMax().setText("");
		this.vista.getPreuMin().setText("");
		this.vista.getPreuMax().setText("");
		removeAlldataFiltros();
		setTable(biblio);
	}

	private class BotonDetallesEditor extends DefaultCellEditor {

		private BotonDetallesEditor(JCheckBox checkbox) {
			super(checkbox);
			UITheme.styleAccentButton(botonDetalles);
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			botonDetalles.setText("Detalles");
			return botonDetalles;
		}
	}

	private class BotonDetallesRenderer extends JButton implements TableCellRenderer {

		public BotonDetallesRenderer() {
			UITheme.styleAccentButton(this);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			setBackground(isSelected ? UITheme.ACCENT_ALT : UITheme.ACCENT);
			setForeground(Color.WHITE);
			setText("Detalles");
			return this;
		}
	}

	public void refreshLlibre(Llibre l, boolean nuevo) {
		if (!nuevo) {
			actualizarfila(l);
		} else {
			addRow(addLlibreMostrar(l));
			anadirComboBoxLlibre(l);

		}

	}

	private void actualizarfila(Llibre l) {

		for (int x = 0; x < this.vista.getjTableBilio().getModel().getRowCount(); x++) {
			if (this.vista.getjTableBilio().getValueAt(x, COLUMNA_ISBN).toString()
					.contentEquals(Long.toString(l.getISBN()))) {
				model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
				model.setValueAt(l.getISBN(), x, COLUMNA_ISBN);
				break;
			}
		}

	}

	public void refresh() {
		quitarFiltros();
	}
}