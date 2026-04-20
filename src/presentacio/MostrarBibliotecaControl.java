package presentacio;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import herramienta.DialogoError;
import herramienta.LlibreValidator;
import herramienta.OpenLibraryClient;
import herramienta.UITheme;
import domini.ControladorDomini;
import domini.Llibre;
import domini.Llista;
import interficie.EnActualizarBBDD;
import presentacio.detalles.control.DetallesLlibrePanelControl;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class MostrarBibliotecaControl {

	private static final int COLUMNA_ISBN = 0;
	private static final int COLUMNA_NOM = 1;
	private static final int COLUMNA_AUTOR = 2;
	private static final int COLUMNA_ANY = 3;
	private static final int COLUMNA_VALORACIO = 4;
	private static final int COLUMNA_PREU = 5;
	private static final int COLUMNA_lLEGIT = 6;
	private static final int COLUMNA_PROGRES = 7;
	private static final int COLUMNA_DETALLS = 8;

	private static final String[] COL_NAMES = {"ISBN","Nom","Autor","Any","Valoracio","Preu","Llegit","Progres","Detalls"};
	private static final boolean[] COL_TOGGLEABLE = {false, false, true, true, true, true, true, true, false};
	// columns removed from view: modelIndex → TableColumn
	private final java.util.TreeMap<Integer, javax.swing.table.TableColumn> hiddenCols = new java.util.TreeMap<>();

	private static final int PAGE_SIZE = 100;

	private MostrarBibliotecaPanel vista;
	private ArrayList<Llibre> biblio;
	private DefaultTableModel model;
	private JButton botonDetalles;
	private EnActualizarBBDD enActualizarBBDD;
	private int currentPage = 0;
	private boolean paginatedMode = false;
	private Integer currentLlistaId = null;
	private java.util.Set<Long> loanedISBNs = new java.util.HashSet<>();

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, ArrayList<Llibre> biblio,
			EnActualizarBBDD enActualizarBBDD) {
		this.vista = vista;
		this.botonDetalles = new JButton();
		UITheme.styleAccentButton(this.botonDetalles);
		this.biblio = biblio;
		this.enActualizarBBDD = enActualizarBBDD;

		this.vista.getBtnPaginaAnterior().addActionListener(e -> showPage(currentPage - 1));
		this.vista.getBtnPaginaSeguent().addActionListener(e -> showPage(currentPage + 1));
		this.vista.getBtnExportCSV().addActionListener(e -> exportarCSV());
		this.vista.getBtnImportarCSV().addActionListener(e -> importarCSV());
		this.vista.getBtnEscanejarISBN().addActionListener(e -> escanejarISBN());
		this.vista.getbtnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
		this.vista.getBtnEstadistiques().addActionListener(e -> mostrarEstadistiques());
		this.vista.getBtnLlibreAleatori().addActionListener(e -> mostrarLlibreAleatori());
		this.vista.getBtnBackupBD().addActionListener(e -> backupBD());
		this.vista.getBtnRestaurarBD().addActionListener(e -> restaurarBD());
		this.vista.getBtnConfiguracio().addActionListener(e -> obrirConfiguracio());
		this.botonDetalles.addActionListener(e -> abrirDetallesLlibres());
		this.vista.getjTableBilio().addMouseListener(abrirDetalles());
		this.vista.getjTableBilio().addMouseListener(contextMenu());

		JTable table = this.vista.getjTableBilio();
		table.getInputMap(JComponent.WHEN_FOCUSED)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "obrirDetalls");
		table.getActionMap().put("obrirDetalls", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				if (vista.getjTableBilio().getSelectedRow() >= 0) abrirDetallesLlibres();
			}
		});
		this.vista.getchckbxLlegit().addItemListener(e -> enLlegitSeleccionado(e));
		this.vista.getchckbxNoLlegit().addItemListener(e -> enNoLlegitSeleccionado(e));

		java.awt.event.ActionListener enterFiltrar = e -> filtrar();
		this.vista.getTextNom().addActionListener(enterFiltrar);
		this.vista.getTextAutor().addActionListener(enterFiltrar);
		this.vista.getTextISBN().addActionListener(enterFiltrar);
		this.vista.getAnyMin().addActionListener(enterFiltrar);
		this.vista.getAnyMax().addActionListener(enterFiltrar);
		this.vista.getValoracioMin().addActionListener(enterFiltrar);
		this.vista.getValoracioMax().addActionListener(enterFiltrar);
		this.vista.getPreuMin().addActionListener(enterFiltrar);
		this.vista.getPreuMax().addActionListener(enterFiltrar);

		this.vista.getSearchBar().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); vista.getjTableBilio().repaint(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); vista.getjTableBilio().repaint(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); vista.getjTableBilio().repaint(); }
		});

		this.vista.getComboLlistes().addActionListener(e -> onLlistaSelected());
		this.vista.getBtnGestioLlistes().addActionListener(e -> obrirGestioLlistes());
		this.vista.getBtnAfegitsRecentment().addActionListener(e -> mostrarAfegitsRecentment());
		this.vista.getBtnLlegitsRecentment().addActionListener(e -> mostrarLlegitsRecentment());
		refreshComboLlistes();
		loanedISBNs = ControladorDomini.getInstance().getLoanedISBNs();

		this.vista.getBtnCarregaPreset().addActionListener(e -> carregarPreset());
		this.vista.getBtnDesaPreset().addActionListener(e -> desarPreset());
		this.vista.getBtnEsborraPreset().addActionListener(e -> esborrarPreset());
		refreshComboPresets();

		showPage(0);

		this.vista.getjTableBilio().getTableHeader().addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e)  { maybeShowColMenu(e); }
			@Override public void mouseReleased(MouseEvent e) { maybeShowColMenu(e); }
			private void maybeShowColMenu(MouseEvent e) {
				if (!e.isPopupTrigger()) return;
				JPopupMenu menu = new JPopupMenu("Columnes visibles");
				for (int col = 0; col < COL_NAMES.length; col++) {
					if (!COL_TOGGLEABLE[col]) continue;
					final int c = col;
					boolean visible = !hiddenCols.containsKey(c);
					javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(COL_NAMES[c], visible);
					item.addActionListener(ev -> toggleColumn(c));
					menu.add(item);
				}
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		});

		javax.swing.Timer saveWidthsTimer = new javax.swing.Timer(800, e -> {
			JTable t = this.vista.getjTableBilio();
			int n = t.getColumnCount();
			if (n == 0) return;
			int[] widths = new int[n];
			for (int i = 0; i < n; i++) widths[i] = t.getColumnModel().getColumn(i).getWidth();
			herramienta.Config.setColWidths(widths);
		});
		saveWidthsTimer.setRepeats(false);
		this.vista.getjTableBilio().getColumnModel().addColumnModelListener(
			new javax.swing.event.TableColumnModelListener() {
				public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
					saveWidthsTimer.restart();
				}
				public void columnAdded(javax.swing.event.TableColumnModelEvent e)   {}
				public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
				public void columnMoved(javax.swing.event.TableColumnModelEvent e)   {}
				public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
			});
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
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JTable table = (JTable) e.getSource();
				int row = table.rowAtPoint(e.getPoint());
				if (row >= 0) table.setRowSelectionInterval(row, row);
				if (e.getClickCount() == 2 && row >= 0) abrirDetallesLlibres();
			}
		};
	}

	private MouseAdapter contextMenu() {
		return new MouseAdapter() {
			private void maybeShow(MouseEvent e) {
				if (!e.isPopupTrigger()) return;
				JTable table = vista.getjTableBilio();
				int row = table.rowAtPoint(e.getPoint());
				if (row < 0) return;
				if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
				String isbnStr = (String) table.getValueAt(row, COLUMNA_ISBN);

				JPopupMenu menu = new JPopupMenu();

				int[] selectedRows = table.getSelectedRows();

				JMenuItem itemObrir = new JMenuItem("Obrir detalls");
				itemObrir.setEnabled(selectedRows.length == 1);
				itemObrir.addActionListener(ev -> abrirDetallesLlibres());
				menu.add(itemObrir);

				JMenuItem itemEliminar = new JMenuItem(
					selectedRows.length > 1 ? "Eliminar " + selectedRows.length + " llibres" : "Eliminar");
				itemEliminar.addActionListener(ev -> eliminarFilaSeleccionada());
				menu.add(itemEliminar);

				JMenuItem itemAfegirLlista = new JMenuItem(
					selectedRows.length > 1 ? "Afegir " + selectedRows.length + " a llista..." : "Afegir a llista...");
				itemAfegirLlista.addActionListener(ev -> afegirSeleccionatsALlista(selectedRows));
				menu.add(itemAfegirLlista);

				JMenuItem itemDuplicar = new JMenuItem("Duplicar...");
				itemDuplicar.setEnabled(selectedRows.length == 1);
				itemDuplicar.addActionListener(ev -> duplicarLlibre(isbnStr));
				menu.add(itemDuplicar);

				menu.addSeparator();

				long isbnLong = Long.parseLong(isbnStr);
				boolean loaned = loanedISBNs.contains(isbnLong);
				if (selectedRows.length == 1 && loaned) {
					JMenuItem itemRetornar = new JMenuItem("Marcar retornat");
					itemRetornar.addActionListener(ev -> {
						try {
							ControladorDomini.getInstance().retornarLlibre(isbnLong);
							loanedISBNs = ControladorDomini.getInstance().getLoanedISBNs();
							vista.getjTableBilio().repaint();
						} catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
					});
					menu.add(itemRetornar);
				} else if (selectedRows.length == 1) {
					JMenuItem itemPrestar = new JMenuItem("Prestar...");
					itemPrestar.addActionListener(ev -> prestarLlibre(isbnLong));
					menu.add(itemPrestar);
				}

				menu.addSeparator();

				JMenuItem itemCopiarISBN = new JMenuItem("Copiar ISBN");
				itemCopiarISBN.setEnabled(selectedRows.length == 1);
				itemCopiarISBN.addActionListener(ev ->
					Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new StringSelection(isbnStr), null));
				menu.add(itemCopiarISBN);

				menu.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override public void mousePressed(MouseEvent e)  { maybeShow(e); }
			@Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
		};
	}

	private void abrirDetallesLlibres() {
		abrirDetalles(false);
	}

	public void abrirDetallesEnEdicio() {
		abrirDetalles(true);
	}

	private void abrirDetalles(boolean editMode) {
		try {
			int row = this.vista.getjTableBilio().getSelectedRow();
			if (row < 0) return;
			Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(Long.parseLong(
					(String) this.vista.getjTableBilio().getValueAt(row, COLUMNA_ISBN)));
			DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, enActualizarBBDD);
			detalles.getDetallesLlibrePanel().setLocationRelativeTo(this.vista);
			if (editMode) detalles.getDetallesLlibrePanel().getBtnEditar().doClick();
			detalles.getDetallesLlibrePanel().setVisible(true);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private void afegirSeleccionatsALlista(int[] rows) {
		java.util.List<Llista> llistes = ControladorDomini.getInstance().getAllLlistes();
		if (llistes.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "No hi ha cap llista. Crea'n una primer.", "Sense llistes",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		Llista sel = (Llista) JOptionPane.showInputDialog(vista,
			"Selecciona la llista on afegir " + rows.length + " llibre(s):",
			"Afegir a llista", JOptionPane.QUESTION_MESSAGE, null,
			llistes.toArray(new Llista[0]), llistes.get(0));
		if (sel == null) return;
		int ok = 0, skip = 0;
		JTable t = this.vista.getjTableBilio();
		for (int row : rows) {
			try {
				long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
				ControladorDomini.getInstance().addLlibreToLlista(isbn, sel.getId(), 0.0, false);
				ok++;
			} catch (Exception ignored) { skip++; }
		}
		String msg = ok + " llibres afegits a \"" + sel.getNom() + "\".";
		if (skip > 0) msg += "\n" + skip + " ja existien a la llista.";
		JOptionPane.showMessageDialog(vista, msg, "Afegit a llista", JOptionPane.INFORMATION_MESSAGE);
		refreshComboLlistes();
	}

	private void duplicarLlibre(String isbnStr) {
		try {
			Llibre src = MainFrameControl.getInstance(null).getLlibreIsbn(Long.parseLong(isbnStr));
			if (src == null) return;
			presentacio.detalles.vista.GuardarLlibresDialogo dialeg =
				new presentacio.detalles.vista.GuardarLlibresDialogo();
			new presentacio.detalles.control.GuardarLlibresDialogoControl(dialeg);
			dialeg.getTextNom().setText(src.getNom());
			dialeg.getTextAutor().setText(src.getAutor() != null ? src.getAutor() : "");
			dialeg.getTextAny().setText(src.getAny() != null && src.getAny() != 0
				? String.valueOf(src.getAny()) : "");
			dialeg.getTextDescripcio().setText(src.getDescripcio() != null ? src.getDescripcio() : "");
			dialeg.getTextValoracio().setText(src.getValoracio() != null && src.getValoracio() != 0.0
				? String.valueOf(src.getValoracio()) : "");
			dialeg.getTextPreu().setText(src.getPreu() != null && src.getPreu() != 0.0
				? String.valueOf(src.getPreu()) : "");
			dialeg.getChckLlegit().setSelected(Boolean.TRUE.equals(src.getLlegit()));
			dialeg.getTextISBN().setText("");
			dialeg.getTextISBN().requestFocusInWindow();
			dialeg.setLocationRelativeTo(this.vista);
			dialeg.setVisible(true);
			refresh();
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private void refreshComboPresets() {
		javax.swing.JComboBox<String> combo = vista.getComboPresets();
		combo.removeAllItems();
		int n = herramienta.Config.getPresetCount();
		if (n == 0) {
			combo.addItem("(sense presets)");
			vista.getBtnCarregaPreset().setEnabled(false);
			vista.getBtnEsborraPreset().setEnabled(false);
		} else {
			for (int i = 0; i < n; i++) combo.addItem(herramienta.Config.getPresetName(i));
			vista.getBtnCarregaPreset().setEnabled(true);
			vista.getBtnEsborraPreset().setEnabled(true);
		}
	}

	private java.util.Map<String, String> collectFilterState() {
		java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
		m.put("nom",          vista.getTextNom().getText());
		m.put("autor",        vista.getTextAutor().getText());
		m.put("isbn",         vista.getTextISBN().getText());
		m.put("anyMin",       vista.getAnyMin().getText());
		m.put("anyMax",       vista.getAnyMax().getText());
		m.put("valoracioMin", vista.getValoracioMin().getText());
		m.put("valoracioMax", vista.getValoracioMax().getText());
		m.put("preuMin",      vista.getPreuMin().getText());
		m.put("preuMax",      vista.getPreuMax().getText());
		m.put("llegit",       vista.getchckbxLlegit().isSelected() ? "true"
		                    : vista.getchckbxNoLlegit().isSelected() ? "false" : "");
		return m;
	}

	private void applyFilterState(java.util.Map<String, String> state) {
		vista.getTextNom().setText(state.getOrDefault("nom", ""));
		vista.getTextAutor().setText(state.getOrDefault("autor", ""));
		vista.getTextISBN().setText(state.getOrDefault("isbn", ""));
		vista.getAnyMin().setText(state.getOrDefault("anyMin", ""));
		vista.getAnyMax().setText(state.getOrDefault("anyMax", ""));
		vista.getValoracioMin().setText(state.getOrDefault("valoracioMin", ""));
		vista.getValoracioMax().setText(state.getOrDefault("valoracioMax", ""));
		vista.getPreuMin().setText(state.getOrDefault("preuMin", ""));
		vista.getPreuMax().setText(state.getOrDefault("preuMax", ""));
		String llegit = state.getOrDefault("llegit", "");
		vista.getchckbxLlegit().setSelected("true".equals(llegit));
		vista.getchckbxNoLlegit().setSelected("false".equals(llegit));
	}

	private void carregarPreset() {
		int idx = vista.getComboPresets().getSelectedIndex();
		if (idx < 0 || herramienta.Config.getPresetCount() == 0) return;
		applyFilterState(herramienta.Config.loadPreset(idx));
		filtrar();
	}

	private void desarPreset() {
		String name = JOptionPane.showInputDialog(vista, "Nom del preset:", "Desa filtre", JOptionPane.QUESTION_MESSAGE);
		if (name == null || name.isBlank()) return;
		herramienta.Config.savePreset(name.trim(), collectFilterState());
		refreshComboPresets();
		// select the newly saved preset
		vista.getComboPresets().setSelectedIndex(herramienta.Config.getPresetCount() - 1);
	}

	private void esborrarPreset() {
		int idx = vista.getComboPresets().getSelectedIndex();
		if (idx < 0 || herramienta.Config.getPresetCount() == 0) return;
		String name = herramienta.Config.getPresetName(idx);
		if (JOptionPane.showConfirmDialog(vista, "Eliminar preset \"" + name + "\"?",
				"Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
		herramienta.Config.deletePreset(idx);
		refreshComboPresets();
	}

	private void toggleColumn(int modelIndex) {
		JTable t = vista.getjTableBilio();
		if (hiddenCols.containsKey(modelIndex)) {
			// restore — insert at correct view position
			javax.swing.table.TableColumn tc = hiddenCols.remove(modelIndex);
			t.addColumn(tc);
			// move to correct position: count how many cols with modelIndex < this are visible
			int targetView = 0;
			for (int i = 0; i < modelIndex; i++)
				if (!hiddenCols.containsKey(i)) targetView++;
			int currentView = t.getColumnCount() - 1;
			if (currentView != targetView) t.moveColumn(currentView, targetView);
			herramienta.Config.setColVisible(modelIndex, true);
		} else {
			// hide
			javax.swing.table.TableColumn tc = t.getColumn(COL_NAMES[modelIndex]);
			hiddenCols.put(modelIndex, tc);
			t.removeColumn(tc);
			herramienta.Config.setColVisible(modelIndex, false);
		}
	}

	private void applyColumnVisibility() {
		for (int col = 0; col < COL_NAMES.length; col++) {
			if (!COL_TOGGLEABLE[col]) continue;
			boolean shouldBeVisible = herramienta.Config.getColVisible(col);
			boolean isVisible = !hiddenCols.containsKey(col);
			if (isVisible != shouldBeVisible) toggleColumn(col);
		}
	}

	private void setHeader(String[] s) {
		this.vista.getjTableBilio().setModel(new DefaultTableModel(s, 0));
	}

	private Object[] addLlibreMostrar(Llibre l) {
		String estat = Boolean.TRUE.equals(l.getLlegit()) ? "Llegit" : "No llegit";
		return new Object[] { l.getISBN() + "", l.getNom(), l.getAutor(), l.getAny(), l.getValoracio(), l.getPreu(),
				estat, l.getPagines() + "/" + l.getPaginesLlegides(), "" };
	}

	private void setTable(ArrayList<Llibre> llibres) {
		this.model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		this.model.getDataVector().removeAllElements();
		removeAlldataFiltros();
		setHeader(new String[] { "ISBN", "Nom", "Autor", "Any", "Valoracio", "Preu", "Llegit", "Progres", "Detalls" });
		if (llibres != null) {
			for (Llibre l : llibres) {
				addRow(addLlibreMostrar(l));
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
		t.getColumnModel().getColumn(COLUMNA_PROGRES).setPreferredWidth(90);
		t.getColumnModel().getColumn(COLUMNA_PROGRES).setMinWidth(50);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setPreferredWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMinWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMaxWidth(110);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellRenderer(new BotonDetallesRenderer());
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellEditor(new BotonDetallesEditor(new JCheckBox()));
		t.getColumnModel().getColumn(COLUMNA_lLEGIT).setCellRenderer(new LlegitCheckBoxRenderer());
		t.getColumnModel().getColumn(COLUMNA_lLEGIT).setCellEditor(new LlegitCheckBoxEditor());
		t.getColumnModel().getColumn(COLUMNA_PROGRES).setCellRenderer(new ProgressBarRenderer());
		SearchHighlightRenderer highlightRenderer = new SearchHighlightRenderer();
		for (int i = 0; i < t.getColumnCount(); i++) {
			if (i != COLUMNA_lLEGIT && i != COLUMNA_DETALLS && i != COLUMNA_PROGRES)
				t.getColumnModel().getColumn(i).setCellRenderer(highlightRenderer);
		}

		int[] defaults = { 130, 220, 180, 55, 75, 60, 80, 90, 85 };
		for (int i = 0; i < defaults.length; i++) {
			int saved = herramienta.Config.getColWidth(i, -1);
			if (saved > 0) t.getColumnModel().getColumn(i).setPreferredWidth(saved);
		}

		// update model ref after setHeader() replaced it
		this.model = (DefaultTableModel) t.getModel();

		// re-apply column visibility from config (setHeader() reset all columns)
		hiddenCols.clear();
		applyColumnVisibility();
		aplicarSearchBar();
	}

	private void aplicarSearchBar() {
		String query = this.vista.getSearchBar().getText().trim();
		javax.swing.RowSorter<?> sorter = this.vista.getjTableBilio().getRowSorter();
		if (!(sorter instanceof javax.swing.DefaultRowSorter)) { updateTitleBar(); return; }
		@SuppressWarnings("unchecked")
		javax.swing.DefaultRowSorter<DefaultTableModel, Integer> drs =
			(javax.swing.DefaultRowSorter<DefaultTableModel, Integer>) sorter;
		if (query.isEmpty()) {
			drs.setRowFilter(null);
		} else {
			String q = query.toLowerCase();
			drs.setRowFilter(new javax.swing.RowFilter<DefaultTableModel, Integer>() {
				@Override
				public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
					for (int i = 0; i < entry.getValueCount(); i++) {
						if (entry.getStringValue(i).toLowerCase().contains(q)) return true;
					}
					try {
						long isbn = Long.parseLong(entry.getStringValue(COLUMNA_ISBN));
						Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(isbn);
						if (l != null) {
							String desc = l.getDescripcio();
							String notes = l.getNotes();
							if (desc  != null && desc.toLowerCase().contains(q))  return true;
							if (notes != null && notes.toLowerCase().contains(q)) return true;
						}
					} catch (Exception ignored) {}
					return false;
				}
			});
		}
		updateTitleBar();
	}

	private void updateTitleBar() {
		JTable t = this.vista.getjTableBilio();
		int shown = t.getRowCount();
		int total = this.biblio != null ? this.biblio.size() : 0;
		java.awt.Window w = SwingUtilities.getWindowAncestor(this.vista);
		if (w instanceof MainFramePanel) {
			((JFrame) w).setTitle("Biblioteca");
			String shelf = currentLlistaId == null ? "Totes les llistes" : currentShelfName();
			String count = shown == total
				? total + " llibres"
				: shown + " / " + total + " llibres";
			((MainFramePanel) w).getStatusBar().setText(shelf + "  —  " + count);
		}
	}

	private String currentShelfName() {
		Object sel = this.vista.getComboLlistes().getSelectedItem();
		if (sel instanceof domini.Llista) return ((domini.Llista) sel).getNom();
		return "Totes les llistes";
	}

	private void removeAlldataFiltros() {
		this.vista.getTextISBN().setText("");
		this.vista.getTextNom().setText("");
		this.vista.getTextAutor().setText("");
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

	private void importarCSV() {
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
		if (fc.showOpenDialog(this.vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		int ok = 0, err = 0, skipped = 0;
		StringBuilder errors = new StringBuilder();
		ControladorDomini cd = ControladorDomini.getInstance();
		try (java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.FileReader(fc.getSelectedFile(), java.nio.charset.StandardCharsets.UTF_8))) {
			br.readLine();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isBlank()) continue;
				try {
					String[] c = parseCSVLine(line);
					long isbn = Long.parseLong(c[0].trim());
					try { cd.getLlibre(isbn); skipped++; continue; } catch (Exception ignored) {}
					Llibre l = LlibreValidator.checkLlibre(
						isbn, c[1], c[2],
						Integer.parseInt(c[3].trim()),
						c.length > 4 ? c[4] : "",
						c.length > 5 ? Double.parseDouble(c[5].trim()) : 0.0,
						c.length > 6 ? Double.parseDouble(c[6].trim()) : 0.0,
						c.length > 7 ? Boolean.parseBoolean(c[7].trim()) : false,
						c.length > 8 ? c[8] : "");
					cd.addLlibre(l);
					ok++;
				} catch (Exception ex) {
					err++;
					if (errors.length() < 400) errors.append("\n• ").append(ex.getMessage());
				}
			}
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
			return;
		}
		String msg = ok + " llibres importats.";
		if (skipped > 0) msg += "\n" + skipped + " omesos (ISBN ja existeix).";
		if (err > 0) msg += "\n" + err + " errors:" + errors;
		JOptionPane.showMessageDialog(this.vista, msg, "Importació",
			err > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
		quitarFiltros();
	}

	private static String[] parseCSVLine(String line) {
		java.util.List<String> fields = new java.util.ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inQuote = false;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (inQuote) {
				if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					sb.append('"'); i++;
				} else if (ch == '"') {
					inQuote = false;
				} else {
					sb.append(ch);
				}
			} else if (ch == '"') {
				inQuote = true;
			} else if (ch == ',') {
				fields.add(sb.toString()); sb = new StringBuilder();
			} else {
				sb.append(ch);
			}
		}
		fields.add(sb.toString());
		return fields.toArray(new String[0]);
	}

	private void escanejarISBN() {
		String isbn = JOptionPane.showInputDialog(this.vista,
			"Escaneja o escriu el codi ISBN\n(el lector de barres escriurà automàticament):",
			"Escanejar ISBN", JOptionPane.QUESTION_MESSAGE);
		if (isbn == null || isbn.isBlank()) return;

		GuardarLlibresDialogo dialeg = new GuardarLlibresDialogo();
		new GuardarLlibresDialogoControl(dialeg);
		dialeg.getTextISBN().setText(isbn.trim());

		String finalIsbn = isbn.trim();
		Thread fetchThread = new Thread(() -> {
			java.util.Map<String, String> meta = OpenLibraryClient.lookupByISBN(finalIsbn);
			SwingUtilities.invokeLater(() -> {
				if (!dialeg.isVisible()) return;
				String title = meta.get("title");
				String autor = meta.get("autor");
				String any = meta.get("any");
				if (title != null && !title.isEmpty() && dialeg.getTextNom().getText().isEmpty())
					dialeg.getTextNom().setText(title);
				if (autor != null && !autor.isEmpty() && dialeg.getTextAutor().getText().isEmpty())
					dialeg.getTextAutor().setText(autor);
				if (any != null && !any.isEmpty() && dialeg.getTextAny().getText().isEmpty())
					dialeg.getTextAny().setText(any);
			});
		});
		fetchThread.setDaemon(true);
		fetchThread.start();

		dialeg.setLocationRelativeTo(this.vista);
		dialeg.setVisible(true);
		quitarFiltros();
	}

	private void exportarCSV() {
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setSelectedFile(new java.io.File("biblioteca.csv"));
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
		if (fc.showSaveDialog(this.vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		java.io.File f = fc.getSelectedFile();
		if (!f.getName().toLowerCase().endsWith(".csv")) f = new java.io.File(f.getPath() + ".csv");
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
			pw.println("ISBN,Nom,Autor,Any,Descripcio,Valoracio,Preu,Llegit,Portada");
			javax.swing.table.TableModel m = this.vista.getjTableBilio().getModel();
			for (int row = 0; row < m.getRowCount(); row++) {
				try {
					long isbn = Long.parseLong(m.getValueAt(row, COLUMNA_ISBN).toString());
					Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(isbn);
					if (l == null) continue;
					pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\",%.1f,%.2f,%b,\"%s\"%n",
						l.getISBN(), esc(l.getNom()), esc(l.getAutor()), l.getAny(),
						esc(l.getDescripcio()), l.getValoracio(), l.getPreu(), l.getLlegit(),
						esc(l.getImatge()));
				} catch (Exception ignored) {}
			}
			JOptionPane.showMessageDialog(this.vista,
				"Exportat a: " + f.getAbsolutePath(), "Exportació completada",
				JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("\"", "\"\"");
	}

	private void mostrarEstadistiques() {
		ControladorDomini cd = ControladorDomini.getInstance();
		ArrayList<Llibre> global = cd.getAllLlibres();
		if (global.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "La biblioteca és buida.", "Estadístiques",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// ── Global summary ─────────────────────────────────────────────────────
		String summary = buildStatsSummary(global, "Tota la biblioteca");

		javax.swing.JTextArea txtSummary = new javax.swing.JTextArea(summary);
		txtSummary.setEditable(false);
		txtSummary.setFont(herramienta.UITheme.FONT_BASE);
		txtSummary.setBackground(herramienta.UITheme.BG_PANEL);
		txtSummary.setForeground(herramienta.UITheme.TEXT_DARK);
		txtSummary.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));

		// ── Per-shelf breakdown table ──────────────────────────────────────────
		DefaultTableModel shelfModel = new DefaultTableModel(
			new String[]{"Llista", "Llibres", "Llegits", "% Llegit", "Val. Mitjana"}, 0) {
			@Override public boolean isCellEditable(int r, int c) { return false; }
		};
		for (Llista ll : cd.getAllLlistes()) {
			ArrayList<Llibre> shelf = cd.getLlibresInLlista(ll.getId());
			if (shelf.isEmpty()) { shelfModel.addRow(new Object[]{ll.getNom(), 0, 0, "0.0%", "—"}); continue; }
			long llegits = shelf.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
			double avgVal = shelf.stream().mapToDouble(l -> l.getValoracio() != null ? l.getValoracio() : 0).average().orElse(0);
			shelfModel.addRow(new Object[]{
				ll.getNom(), shelf.size(), llegits,
				String.format("%.1f%%", 100.0 * llegits / shelf.size()),
				String.format("%.2f", avgVal)
			});
		}

		JTable shelfTable = new JTable(shelfModel);
		shelfTable.setFont(herramienta.UITheme.FONT_BASE);
		shelfTable.setBackground(herramienta.UITheme.BG_PANEL);
		shelfTable.setForeground(herramienta.UITheme.TEXT_DARK);
		shelfTable.setRowHeight(26);
		shelfTable.setEnabled(false);
		shelfTable.getTableHeader().setFont(herramienta.UITheme.FONT_BOLD);
		javax.swing.JScrollPane shelfScroll = new javax.swing.JScrollPane(shelfTable);
		shelfScroll.setPreferredSize(new java.awt.Dimension(480, Math.min(200, shelfModel.getRowCount() * 27 + 30)));
		shelfScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("Per llista"));

		// ── Reading goal ──────────────────────────────────────────────────────
		int totalLlegits = (int) global.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
		int savedGoal = herramienta.Config.getReadingGoal();
		javax.swing.JPanel goalPanel = new javax.swing.JPanel(new java.awt.BorderLayout(6, 4));
		goalPanel.setBackground(herramienta.UITheme.BG_PANEL);
		goalPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
			javax.swing.BorderFactory.createLineBorder(herramienta.UITheme.BORDER_CLR),
			"Objectiu de lectura", javax.swing.border.TitledBorder.LEFT,
			javax.swing.border.TitledBorder.TOP, herramienta.UITheme.FONT_BOLD, herramienta.UITheme.TEXT_MID));

		javax.swing.JProgressBar goalBar = new javax.swing.JProgressBar(0, Math.max(savedGoal, 1));
		goalBar.setValue(Math.min(totalLlegits, Math.max(savedGoal, 1)));
		goalBar.setStringPainted(true);
		goalBar.setFont(herramienta.UITheme.FONT_BASE);

		javax.swing.JSpinner goalSpinner = new javax.swing.JSpinner(
			new javax.swing.SpinnerNumberModel(Math.max(savedGoal, 1), 1, 9999, 1));
		goalSpinner.setFont(herramienta.UITheme.FONT_BASE);
		goalSpinner.setPreferredSize(new java.awt.Dimension(70, 28));
		goalSpinner.addChangeListener(ev -> {
			int goal = (int) goalSpinner.getValue();
			herramienta.Config.setReadingGoal(goal);
			goalBar.setMaximum(goal);
			goalBar.setValue(Math.min(totalLlegits, goal));
			goalBar.setString(totalLlegits + " / " + goal);
		});
		goalBar.setMaximum(Math.max(savedGoal, 1));
		goalBar.setString(totalLlegits + " / " + Math.max(savedGoal, 1));

		javax.swing.JLabel lblGoal = new javax.swing.JLabel("Objectiu:");
		herramienta.UITheme.styleLabel(lblGoal);
		javax.swing.JPanel goalControls = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
		goalControls.setBackground(herramienta.UITheme.BG_PANEL);
		goalControls.add(lblGoal);
		goalControls.add(goalSpinner);
		goalControls.add(new javax.swing.JLabel("llegits: " + totalLlegits));
		goalPanel.add(goalControls, java.awt.BorderLayout.NORTH);
		goalPanel.add(goalBar, java.awt.BorderLayout.CENTER);

		// ── Layout ────────────────────────────────────────────────────────────
		javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
		panel.setBackground(herramienta.UITheme.BG_PANEL);
		panel.add(goalPanel, java.awt.BorderLayout.NORTH);
		javax.swing.JPanel statsPanel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
		statsPanel.setBackground(herramienta.UITheme.BG_PANEL);
		statsPanel.add(txtSummary, java.awt.BorderLayout.NORTH);
		if (shelfModel.getRowCount() > 0) statsPanel.add(shelfScroll, java.awt.BorderLayout.CENTER);
		panel.add(statsPanel, java.awt.BorderLayout.CENTER);

		javax.swing.JDialog dlg = new javax.swing.JDialog(SwingUtilities.getWindowAncestor(vista),
			"Estadístiques", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		dlg.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
		dlg.getContentPane().setBackground(herramienta.UITheme.BG_PANEL);
		dlg.add(panel);
		javax.swing.JButton btnClose = new javax.swing.JButton("Tancar");
		herramienta.UITheme.styleSecondaryButton(btnClose);
		btnClose.addActionListener(e -> dlg.dispose());
		dlg.getRootPane().setDefaultButton(btnClose);
		dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(),
			javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);
		javax.swing.JPanel btnPanel = new javax.swing.JPanel();
		btnPanel.setBackground(herramienta.UITheme.BG_PANEL);
		btnPanel.add(btnClose);
		dlg.add(btnPanel, java.awt.BorderLayout.SOUTH);
		dlg.pack();
		dlg.setMinimumSize(new java.awt.Dimension(500, 200));
		dlg.setLocationRelativeTo(vista);
		dlg.setVisible(true);
	}

	private String buildStatsSummary(ArrayList<Llibre> llibres, String scope) {
		int total = llibres.size();
		long llegits = llibres.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
		double avgVal = llibres.stream().mapToDouble(l -> l.getValoracio() != null ? l.getValoracio() : 0).average().orElse(0);
		double avgPreu = llibres.stream().mapToDouble(l -> l.getPreu() != null ? l.getPreu() : 0).average().orElse(0);
		String topAnys = llibres.stream()
			.filter(l -> l.getAny() != null && l.getAny() > 0)
			.collect(Collectors.groupingBy(Llibre::getAny, Collectors.counting()))
			.entrySet().stream()
			.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
			.limit(3)
			.map(e -> "  " + e.getKey() + ": " + e.getValue() + " llibre" + (e.getValue() > 1 ? "s" : ""))
			.collect(Collectors.joining("\n"));
		return String.format(
			"%s%n" +
			"Total: %d  ·  Llegits: %d (%.1f%%)  ·  No llegits: %d%n" +
			"Valoració mitjana: %.2f / 10  ·  Preu mitjà: %.2f €%n" +
			"Anys top:%n%s",
			scope, total, llegits, 100.0 * llegits / total, total - llegits,
			avgVal, avgPreu, topAnys.isEmpty() ? "  (cap any registrat)" : topAnys);
	}

	private void mostrarLlibreAleatori() {
		if (biblio == null || biblio.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "No hi ha llibres a la vista actual.", "Llibre Aleatori",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		java.util.List<Llibre> noLlegits = biblio.stream()
			.filter(l -> !Boolean.TRUE.equals(l.getLlegit()))
			.collect(Collectors.toList());
		if (noLlegits.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "Tots els llibres de la vista actual ja estan llegits.", "Llibre Aleatori",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		Llibre aleatori = noLlegits.get(new java.util.Random().nextInt(noLlegits.size()));
		JTable t = vista.getjTableBilio();
		for (int row = 0; row < t.getRowCount(); row++) {
			if (String.valueOf(aleatori.getISBN()).equals(t.getValueAt(row, COLUMNA_ISBN))) {
				t.setRowSelectionInterval(row, row);
				t.scrollRectToVisible(t.getCellRect(row, 0, true));
				break;
			}
		}
		DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(aleatori, enActualizarBBDD);
		detalles.getDetallesLlibrePanel().setLocationRelativeTo(vista);
		detalles.getDetallesLlibrePanel().setVisible(true);
	}

	private void prestarLlibre(long isbn) {
		String nom = JOptionPane.showInputDialog(vista,
			"Nom de la persona que s'enduu el llibre:", "Prestar", JOptionPane.QUESTION_MESSAGE);
		if (nom == null || nom.isBlank()) return;
		try {
			ControladorDomini.getInstance().prestarLlibre(isbn, nom.trim());
			loanedISBNs = ControladorDomini.getInstance().getLoanedISBNs();
			vista.getjTableBilio().repaint();
			JOptionPane.showMessageDialog(vista, "Llibre prestat a \"" + nom.trim() + "\".",
				"Préstec registrat", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
	}

	private void mostrarAfegitsRecentment() {
		ArrayList<Llibre> recents = ControladorDomini.getInstance().getRecentlyAdded();
		if (recents.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "No hi ha llibres.", "Afegits recentment", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		biblio = recents;
		currentLlistaId = null;
		currentPage = 0;
		showPage(0);
	}

	private void mostrarLlegitsRecentment() {
		ArrayList<Llibre> llegits = new ArrayList<>(
			ControladorDomini.getInstance().getAllLlibres().stream()
				.filter(l -> Boolean.TRUE.equals(l.getLlegit()))
				.collect(java.util.stream.Collectors.toList()));
		if (llegits.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "Cap llibre marcat com a llegit.", "Llegits", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		biblio = llegits;
		currentLlistaId = null;
		currentPage = 0;
		showPage(0);
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

		String autorTyped = this.vista.getTextAutor().getText().trim();
		if (!autorTyped.isEmpty()) nomAutor = autorTyped;

		String nomTyped = this.vista.getTextNom().getText().trim();
		if (!nomTyped.isEmpty()) nomLlibre = nomTyped;
		String isbnText = this.vista.getTextISBN().getText().trim();
		if (!isbnText.isEmpty()) {
			try { ISBN = Long.parseLong(isbnText); } catch (NumberFormatException ignored) {}
		}

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
		currentPage = 0;
		showPage(0);
	}

	private void showPage(int page) {
		if (biblio == null || biblio.size() <= PAGE_SIZE) {
			paginatedMode = false;
			vista.getPaginationPanel().setVisible(false);
			setTable(biblio);
			return;
		}
		int totalPages = (int) Math.ceil((double) biblio.size() / PAGE_SIZE);
		page = Math.max(0, Math.min(page, totalPages - 1));
		currentPage = page;
		paginatedMode = true;

		int from = page * PAGE_SIZE;
		int to   = Math.min(from + PAGE_SIZE, biblio.size());
		setTable(new ArrayList<>(biblio.subList(from, to)));

		vista.getLblPagina().setText("Pàgina " + (page + 1) + " / " + totalPages);
		vista.getBtnPaginaAnterior().setEnabled(page > 0);
		vista.getBtnPaginaSeguent().setEnabled(page < totalPages - 1);
		vista.getPaginationPanel().setVisible(true);
	}

	// ── Search highlight renderer ─────────────────────────────────────────────

	private class SearchHighlightRenderer extends javax.swing.table.DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable t, Object value,
				boolean selected, boolean focus, int row, int col) {
			super.getTableCellRendererComponent(t, value, selected, focus, row, col);
			// Loan indicator: tint row orange if book is loaned out
			if (!selected) {
				try {
					long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
					if (loanedISBNs.contains(isbn)) {
						setBackground(UITheme.isDark ? new java.awt.Color(0x5C3A00) : new java.awt.Color(0xFFF3CD));
					}
				} catch (Exception ignored) {}
			}
			String query = vista.getSearchBar().getText().trim();
			String text = value != null ? value.toString() : "";
			if (!query.isEmpty() && !selected) {
				String escaped = text
					.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
				String escapedQ = java.util.regex.Pattern.quote(query);
				String highlighted = escaped.replaceAll(
					"(?i)(" + escapedQ.replace("\\Q", "\\Q").replace("\\E", "\\E") + ")",
					"<span style='background:#F39C12;color:#000'>$1</span>");
				if (!highlighted.equals(escaped))
					setText("<html>" + highlighted + "</html>");
			}
			return this;
		}
	}

	private class ProgressBarRenderer extends javax.swing.JProgressBar implements javax.swing.table.TableCellRenderer {
		ProgressBarRenderer() {
			setMinimum(0);
			setStringPainted(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object value,
				boolean selected, boolean focus, int row, int col) {
			try {
				long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
				Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(isbn);
				if (l != null && l.getPagines() > 0) {
					setMaximum(l.getPagines());
					setValue(l.getPaginesLlegides());
					setString(l.getPaginesLlegides() + " / " + l.getPagines());
				} else {
					setMaximum(1); setValue(0); setString("—");
				}
			} catch (Exception ignored) {
				setMaximum(1); setValue(0); setString("—");
			}
			setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
			setForeground(selected ? java.awt.Color.WHITE : UITheme.TEXT_DARK);
			return this;
		}
	}

	// ── Llegit column: checkbox renderer + in-place toggle editor ─────────────

	private class LlegitCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
		LlegitCheckBoxRenderer() {
			setHorizontalAlignment(JCheckBox.CENTER);
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object val, boolean selected,
				boolean focus, int row, int col) {
			setSelected("Llegit".equals(val));
			setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
			setForeground(selected ? Color.WHITE : UITheme.TEXT_DARK);
			return this;
		}
	}

	private class LlegitCheckBoxEditor extends AbstractCellEditor implements TableCellEditor {
		private final JCheckBox cb = new JCheckBox();
		private int editingRow = -1;

		LlegitCheckBoxEditor() {
			cb.setHorizontalAlignment(JCheckBox.CENTER);
			cb.setOpaque(true);
			cb.addActionListener(e -> {
				boolean newLlegit = cb.isSelected();
				int row = editingRow;
				String isbnStr = row >= 0 && row < model.getRowCount()
					? (String) model.getValueAt(row, COLUMNA_ISBN) : null;
				fireEditingStopped();
				if (isbnStr != null) {
					String isbn = isbnStr;
					Thread t = new Thread(() -> {
						try {
							Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(Long.parseLong(isbn));
							if (l == null) return;
							Llibre updated = new Llibre(l.getISBN(), l.getNom(), l.getAutor(), l.getAny(),
								l.getDescripcio(), l.getValoracio(), l.getPreu(), newLlegit, l.getImatge());
							ControladorDomini.getInstance().deleteLlibre(l);
							ControladorDomini.getInstance().addLlibre(updated);
						} catch (Exception ex) {
							SwingUtilities.invokeLater(() -> new DialogoError(ex).showErrorMessage());
						}
					});
					t.setDaemon(true);
					t.start();
				}
			});
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int col) {
			editingRow = row;
			cb.setSelected("Llegit".equals(value));
			cb.setBackground(UITheme.ACCENT);
			cb.setForeground(Color.WHITE);
			return cb;
		}

		@Override
		public Object getCellEditorValue() {
			return cb.isSelected() ? "Llegit" : "No llegit";
		}
	}

	// ── Detalls button renderer/editor ─────────────────────────────────────────

	private class BotonDetallesEditor extends DefaultCellEditor {
		BotonDetallesEditor(JCheckBox checkbox) {
			super(checkbox);
			UITheme.styleAccentButton(botonDetalles);
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
			botonDetalles.setText("Detalles");
			return botonDetalles;
		}
	}

	private class BotonDetallesRenderer extends JButton implements TableCellRenderer {
		BotonDetallesRenderer() { UITheme.styleAccentButton(this); }

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			setBackground(isSelected ? UITheme.ACCENT_ALT : UITheme.ACCENT);
			setForeground(Color.WHITE);
			setText("Detalles");
			return this;
		}
	}

	// ── Row operations ─────────────────────────────────────────────────────────

	public void refreshLlibre(Llibre l, boolean nuevo) {
		if (!nuevo) {
			actualizarfila(l);
		} else {
			addRow(addLlibreMostrar(l));
			updateTitleBar();
		}
	}

	private void actualizarfila(Llibre l) {
		model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		for (int x = 0; x < model.getRowCount(); x++) {
			if (model.getValueAt(x, COLUMNA_ISBN).toString().contentEquals(Long.toString(l.getISBN()))) {
				Object[] row = addLlibreMostrar(l);
				for (int col = 0; col < row.length; col++)
					model.setValueAt(row[col], x, col);
				break;
			}
		}
	}

	public void eliminarFilaSeleccionada() {
		JTable t = this.vista.getjTableBilio();
		int[] rows = t.getSelectedRows();
		if (rows.length == 0) return;
		String msg = rows.length == 1
			? "Eliminar \"" + t.getValueAt(rows[0], COLUMNA_NOM) + "\"?\nAquesta acció no es pot desfer."
			: "Eliminar " + rows.length + " llibres seleccionats?\nAquesta acció no es pot desfer.";
		if (JOptionPane.showConfirmDialog(vista, msg, "Confirmar eliminació",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
		// collect ISBNs before rows shift during deletion
		java.util.List<Long> isbns = new java.util.ArrayList<>();
		for (int row : rows) isbns.add(Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN)));
		for (long isbn : isbns) {
			try {
				Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(isbn);
				if (l == null) continue;
				ControladorDomini.getInstance().deleteLlibre(l);
				eliminarFila(l);
			} catch (Exception e) {
				new DialogoError(e).showErrorMessage();
			}
		}
	}

	public void eliminarFila(Llibre l) {
		model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		for (int x = 0; x < model.getRowCount(); x++) {
			if (model.getValueAt(x, COLUMNA_ISBN).toString().contentEquals(Long.toString(l.getISBN()))) {
				model.removeRow(x);
				biblio.removeIf(b -> b.getISBN().equals(l.getISBN()));
				break;
			}
		}
		updateTitleBar();
	}

	public Integer getCurrentLlistaId() { return currentLlistaId; }

	public void refresh() {
		currentPage = 0;
		if (currentLlistaId != null) {
			biblio = ControladorDomini.getInstance().getLlibresInLlista(currentLlistaId);
		} else {
			biblio = ControladorDomini.getInstance().getAllLlibres();
		}
		quitarFiltros();
	}

	public void refreshComboLlistes() {
		javax.swing.JComboBox<Object> combo = this.vista.getComboLlistes();
		// Remove listener temporarily to avoid triggering onLlistaSelected
		java.awt.event.ActionListener[] listeners = combo.getActionListeners();
		for (java.awt.event.ActionListener al : listeners) combo.removeActionListener(al);
		combo.removeAllItems();
		ControladorDomini cd = ControladorDomini.getInstance();
		java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
		combo.addItem("Totes les llistes (" + cd.getAllLlibres().size() + ")");
		for (Llista l : cd.getAllLlistes()) {
			counts.put(l.getId(), cd.getCountInLlista(l.getId()));
			combo.addItem(l);
		}
		combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override
			public java.awt.Component getListCellRendererComponent(
					javax.swing.JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				javax.swing.Icon icon = null;
				if (value instanceof Llista) {
					Llista ll = (Llista) value;
					if (ll.getColor() != null) {
						try {
							java.awt.Color c = java.awt.Color.decode(ll.getColor());
							icon = new javax.swing.Icon() {
								public int getIconWidth()  { return 12; }
								public int getIconHeight() { return 12; }
								public void paintIcon(java.awt.Component cp, java.awt.Graphics g, int x, int y) {
									g.setColor(c);
									g.fillRoundRect(x, y + 1, 10, 10, 3, 3);
									g.setColor(c.darker());
									g.drawRoundRect(x, y + 1, 10, 10, 3, 3);
								}
							};
						} catch (Exception ignored) {}
					}
					value = ll.getNom() + " (" + counts.getOrDefault(ll.getId(), 0) + ")";
				}
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setIcon(icon);
				return this;
			}
		});
		combo.setSelectedIndex(0);
		for (java.awt.event.ActionListener al : listeners) combo.addActionListener(al);
		// Reset to all-books view
		currentLlistaId = null;
		biblio = cd.getAllLlibres();
		currentPage = 0;
		showPage(0);
	}

	private void onLlistaSelected() {
		Object sel = this.vista.getComboLlistes().getSelectedItem();
		if (sel instanceof Llista) {
			currentLlistaId = ((Llista) sel).getId();
			biblio = ControladorDomini.getInstance().getLlibresInLlista(currentLlistaId);
		} else {
			currentLlistaId = null;
			biblio = ControladorDomini.getInstance().getAllLlibres();
		}
		currentPage = 0;
		showPage(0);
	}

	private void obrirGestioLlistes() {
		new GestioLlistesDialog(SwingUtilities.getWindowAncestor(vista), this).setVisible(true);
	}

	private void obrirConfiguracio() {
		java.awt.Window w = SwingUtilities.getWindowAncestor(vista);
		new ConfiguracioDialog(
			w instanceof java.awt.Frame ? (java.awt.Frame) w : null,
			() -> vista.applyTheme()
		).setVisible(true);
	}

	private void backupBD() {
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setSelectedFile(new java.io.File("biblioteca_backup.sql"));
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
		if (fc.showSaveDialog(vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		java.io.File f = fc.getSelectedFile();
		if (!f.getName().toLowerCase().endsWith(".sql")) f = new java.io.File(f.getPath() + ".sql");
		try {
			ControladorDomini.getInstance().backupToSQL(f);
			JOptionPane.showMessageDialog(vista, "Backup guardat a:\n" + f.getAbsolutePath(),
				"Backup complet", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private void restaurarBD() {
		int confirm = JOptionPane.showConfirmDialog(vista,
			"Restaurar la base de dades des d'un fitxer SQL?\n" +
			"ATENCIÓ: Tots els llibres actuals seran eliminats i substituïts.",
			"Confirmar restauració", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION) return;
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
		if (fc.showOpenDialog(vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		try {
			ControladorDomini.getInstance().restoreFromSQL(fc.getSelectedFile());
			biblio = ControladorDomini.getInstance().getAllLlibres();
			quitarFiltros();
			JOptionPane.showMessageDialog(vista, "Base de dades restaurada correctament.",
				"Restauració completada", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}
}
