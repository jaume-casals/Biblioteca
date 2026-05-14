package presentacio;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
import herramienta.I18n;
import herramienta.LlibreValidator;
import herramienta.OpenLibraryClient;
import herramienta.UITheme;
import domini.Llibre;
import domini.LlibreFilter;
import interficie.BibliotecaWriter;
import domini.Llista;
import domini.Tag;
import interficie.EnActualizarBBDD;
import presentacio.detalles.control.DetallesLlibrePanelControl;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class MostrarBibliotecaControl {

	private static final int COLUMNA_COVER    = 0;
	private static final int COLUMNA_ISBN     = 1;
	private static final int COLUMNA_NOM      = 2;
	private static final int COLUMNA_AUTOR    = 3;
	private static final int COLUMNA_ANY      = 4;
	private static final int COLUMNA_VALORACIO = 5;
	private static final int COLUMNA_PREU     = 6;
	private static final int COLUMNA_LLEGIT    = 7;
	private static final int COLUMNA_PROGRES  = 8;
	private static final int COLUMNA_DETALLS  = 9;

	private static String[] colNames() {
		return new String[]{
			I18n.t("col_cover"), I18n.t("col_isbn"), I18n.t("col_title"),
			I18n.t("col_author"), I18n.t("col_year"), I18n.t("col_rating"),
			I18n.t("col_price"), I18n.t("col_read"), I18n.t("col_progress"),
			I18n.t("col_details")
		};
	}
	private static final boolean[] COL_TOGGLEABLE = {true, false, false, true, true, true, true, true, true, false};

	private static final java.util.Map<Long, javax.swing.ImageIcon> coverCache =
		java.util.Collections.synchronizedMap(
			new java.util.LinkedHashMap<Long, javax.swing.ImageIcon>(200, 0.75f, true) {
				@Override protected boolean removeEldestEntry(java.util.Map.Entry<Long, javax.swing.ImageIcon> e) {
					return size() > 150;
				}
			});
	private static final java.util.Set<Long> coverLoading = java.util.concurrent.ConcurrentHashMap.newKeySet();
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
	private boolean useDBPagination = false;
	private boolean groupBySeries = false;
	private final java.util.Deque<Llibre> undoBuffer = new java.util.ArrayDeque<>();
	private static final int UNDO_MAX = 20;
	private Integer currentLlistaId = null;
	private java.util.Set<Long> loanedISBNs = new java.util.HashSet<>();
	private BibliotecaWriter cd;

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, ArrayList<Llibre> biblio,
			EnActualizarBBDD enActualizarBBDD) {
		this(vista, biblio, enActualizarBBDD, null);
	}

	public MostrarBibliotecaControl(MostrarBibliotecaPanel vista, ArrayList<Llibre> biblio,
			EnActualizarBBDD enActualizarBBDD, BibliotecaWriter cd) {
		this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
		this.vista = vista;
		this.botonDetalles = new JButton();
		UITheme.styleAccentButton(this.botonDetalles);
		this.biblio = biblio;
		this.enActualizarBBDD = enActualizarBBDD;

		this.vista.getBtnPaginaAnterior().addActionListener(e -> showPage(currentPage - 1));
		this.vista.getBtnPaginaSeguent().addActionListener(e -> showPage(currentPage + 1));
		this.vista.getBtnExportCSV().addActionListener(e -> exportarCSV());
		this.vista.getBtnImportarCSV().addActionListener(e -> importarCSV());
		this.vista.getBtnImportarCalibre().addActionListener(e -> importarCalibre());
		this.vista.getBtnExportJSON().addActionListener(e -> exportarJSON());
		this.vista.getBtnImportarJSON().addActionListener(e -> importarJSON());
		this.vista.getBtnExportHTML().addActionListener(e -> exportarHTML());
		this.vista.getBtnExportPDF().addActionListener(e -> exportarPDF());
		this.vista.getBtnFetchCovers().addActionListener(e -> fetchMissingCovers());
		this.vista.getBtnEscanejarISBN().addActionListener(e -> escanejarISBN());
		this.vista.getbtnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
		this.vista.getBtnEstadistiques().addActionListener(e -> mostrarEstadistiques());
		this.vista.getBtnLlibreAleatori().addActionListener(e -> mostrarLlibreAleatori());
		this.vista.getBtnBackupBD().addActionListener(e -> backupBD());
		this.vista.getBtnRestaurarBD().addActionListener(e -> restaurarBD());
		this.vista.getBtnConfiguracio().addActionListener(e -> obrirConfiguracio());
		this.vista.getBtnSobre().addActionListener(e ->
			new AboutDialog((java.awt.Frame) vista.getTopLevelAncestor()).setVisible(true));
		this.botonDetalles.addActionListener(e -> abrirDetallesLlibres());
		this.vista.getjTableBilio().addMouseListener(abrirDetalles());
		this.vista.getjTableBilio().addMouseListener(contextMenu());
		this.vista.getjTableBilio().addMouseListener(autorClickFilter());

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
		this.vista.getFilterEditorial().addActionListener(enterFiltrar);
		this.vista.getFilterSerie().addActionListener(enterFiltrar);
		this.vista.getFilterIdioma().addActionListener(enterFiltrar);

		this.vista.getSearchBar().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); vista.getjTableBilio().repaint(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); vista.getjTableBilio().repaint(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); vista.getjTableBilio().repaint(); }
		});

		this.vista.getComboLlistes().addActionListener(e -> onLlistaSelected());
		this.vista.getBtnGestioLlistes().addActionListener(e -> obrirGestioLlistes());
		this.vista.setOnDragToShelf((shelfId, isbns) -> {
			for (long isbn : isbns) {
				try { cd.addLlibreToLlista(isbn, shelfId, 0.0, false); }
				catch (Exception ignored) {}
			}
			refreshComboLlistes();
		});
		this.vista.getBtnAfegitsRecentment().addActionListener(e -> mostrarAfegitsRecentment());
		this.vista.getBtnLlegitsRecentment().addActionListener(e -> mostrarLlegitsRecentment());
		this.vista.getBtnDesitjats().addActionListener(e -> mostrarDesitjats());
		this.vista.getBtnEnCurs().addActionListener(e -> mostrarEnCurs());
		refreshComboLlistes();
		refreshComboTags();
		loanedISBNs = cd.getLoanedISBNs();

		this.vista.getBtnCarregaPreset().addActionListener(e -> carregarPreset());
		this.vista.getBtnDesaPreset().addActionListener(e -> desarPreset());
		this.vista.getBtnEsborraPreset().addActionListener(e -> esborrarPreset());
		refreshComboPresets();

		this.vista.getBtnToggleFiltres().addActionListener(e -> {
			boolean show = !vista.isFilterDrawerVisible();
			vista.setFilterDrawerVisible(show);
			vista.getBtnToggleFiltres().setText(show ? I18n.t("btn_filters_label_open") : I18n.t("btn_filters_label"));
		});
		this.vista.getBtnToggleVista().addActionListener(e -> toggleVista());
		this.vista.getBtnGroupSeries().addActionListener(e -> toggleGroupBySeries());

		this.vista.getGaleria().setCd(this.cd);
		this.vista.getGaleria().setOnCardClick(l -> {
			try {
				DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, enActualizarBBDD, cd);
				detalles.getDetallesLlibrePanel().setLocationRelativeTo(this.vista);
				detalles.getDetallesLlibrePanel().setVisible(true);
			} catch (Exception e) {
				new DialogoError(e).showErrorMessage();
			}
		});
		this.vista.getGaleria().setOnRightClick((e, sel) -> showGaleriaContextMenu(e, sel));
		this.vista.getGaleria().setOnDeleteSelected(sel -> eliminarLlibresGaleria(sel));

		showPage(0);

		// Restore persisted view mode
		if ("galeria".equals(herramienta.Config.getViewMode())) {
			vista.getGaleria().updateLlibres(biblio);
			vista.showGaleria();
			vista.getBtnToggleVista().setText(I18n.t("btn_table_view"));
		}

		// Restore persisted sort
		int savedSortCol = herramienta.Config.getSortColumn();
		if (savedSortCol >= 0 && savedSortCol < vista.getjTableBilio().getColumnCount()) {
			javax.swing.RowSorter<?> sorter = vista.getjTableBilio().getRowSorter();
			if (sorter != null) {
				javax.swing.SortOrder order = "DESCENDING".equals(herramienta.Config.getSortOrder())
					? javax.swing.SortOrder.DESCENDING : javax.swing.SortOrder.ASCENDING;
				sorter.setSortKeys(java.util.Collections.singletonList(
					new javax.swing.RowSorter.SortKey(savedSortCol, order)));
			}
		}

		// Save sort on change
		vista.getjTableBilio().getRowSorter().addRowSorterListener(e -> {
			java.util.List<? extends javax.swing.RowSorter.SortKey> keys = vista.getjTableBilio().getRowSorter().getSortKeys();
			if (keys.isEmpty()) {
				herramienta.Config.setSortColumn(-1);
			} else {
				herramienta.Config.setSortColumn(keys.get(0).getColumn());
				herramienta.Config.setSortOrder(keys.get(0).getSortOrder().name());
			}
		});

		this.vista.getjTableBilio().getTableHeader().addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e)  { maybeShowColMenu(e); }
			@Override public void mouseReleased(MouseEvent e) { maybeShowColMenu(e); }
			private void maybeShowColMenu(MouseEvent e) {
				if (!e.isPopupTrigger()) return;
				JPopupMenu menu = new JPopupMenu("Columnes visibles");
				for (int col = 0; col < colNames().length; col++) {
					if (!COL_TOGGLEABLE[col]) continue;
					final int c = col;
					boolean visible = !hiddenCols.containsKey(c);
					javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(colNames()[c], visible);
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

	private MouseAdapter autorClickFilter() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!e.isControlDown() || e.getClickCount() != 1) return;
				JTable table = (JTable) e.getSource();
				int col = table.columnAtPoint(e.getPoint());
				int row = table.rowAtPoint(e.getPoint());
				if (col != COLUMNA_AUTOR || row < 0) return;
				Object val = table.getValueAt(row, COLUMNA_AUTOR);
				if (val == null) return;
				String autor = val.toString().trim();
				if (autor.isEmpty()) return;
				vista.getTextAutor().setText(autor);
				filtrar();
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

				JMenuItem itemObrir = new JMenuItem(I18n.t("menu_open_details"));
				itemObrir.setEnabled(selectedRows.length == 1);
				itemObrir.addActionListener(ev -> abrirDetallesLlibres());
				menu.add(itemObrir);

				JMenuItem itemEliminar = new JMenuItem(
					selectedRows.length > 1 ? I18n.t("menu_delete_n", selectedRows.length) : I18n.t("menu_delete_one"));
				itemEliminar.addActionListener(ev -> eliminarFilaSeleccionada());
				menu.add(itemEliminar);

				JMenuItem itemAfegirLlista = new JMenuItem(
					selectedRows.length > 1 ? I18n.t("menu_add_to_list_n", selectedRows.length) : I18n.t("menu_add_to_list"));
				itemAfegirLlista.addActionListener(ev -> afegirSeleccionatsALlista(selectedRows));
				menu.add(itemAfegirLlista);

				JMenuItem itemDuplicar = new JMenuItem(I18n.t("menu_duplicate"));
				itemDuplicar.setEnabled(selectedRows.length == 1);
				itemDuplicar.addActionListener(ev -> duplicarLlibre(isbnStr));
				menu.add(itemDuplicar);

				if (selectedRows.length > 1) {
					JMenuItem itemBatchEdit = new JMenuItem(I18n.t("menu_batch_edit_n", selectedRows.length));
					java.util.List<Long> batchIsbns = new java.util.ArrayList<>();
					for (int r : selectedRows) batchIsbns.add(Long.parseLong((String) table.getValueAt(r, COLUMNA_ISBN)));
					itemBatchEdit.addActionListener(ev -> batchEdit(batchIsbns));
					menu.add(itemBatchEdit);
				}

				menu.addSeparator();

				long isbnLong = Long.parseLong(isbnStr);
				boolean loaned = loanedISBNs.contains(isbnLong);
				if (selectedRows.length == 1 && loaned) {
					JMenuItem itemRetornar = new JMenuItem(I18n.t("menu_return_book"));
					itemRetornar.addActionListener(ev -> {
						try {
							cd.retornarLlibre(isbnLong);
							loanedISBNs = cd.getLoanedISBNs();
							vista.getjTableBilio().repaint();
						} catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
					});
					menu.add(itemRetornar);
				} else if (selectedRows.length == 1) {
					JMenuItem itemPrestar = new JMenuItem(I18n.t("menu_loan_book"));
					itemPrestar.addActionListener(ev -> prestarLlibre(isbnLong));
					menu.add(itemPrestar);
				}

				menu.addSeparator();

				JMenuItem itemCopiarISBN = new JMenuItem(I18n.t("menu_copy_isbn"));
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
		if (vista.isGaleriaMode()) {
			List<Llibre> sel = vista.getGaleria().getSelectedLlibres();
			if (!sel.isEmpty()) abrirDetallesDeLlibre(sel.get(0), true);
		} else {
			abrirDetalles(true);
		}
	}

	private void abrirDetalles(boolean editMode) {
		try {
			int row = this.vista.getjTableBilio().getSelectedRow();
			if (row < 0) return;
			Llibre l = MainFrameControl.getInstance().getLlibreIsbn(Long.parseLong(
					(String) this.vista.getjTableBilio().getValueAt(row, COLUMNA_ISBN)));
			abrirDetallesDeLlibre(l, editMode);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private void abrirDetallesDeLlibre(Llibre l, boolean editMode) {
		if (l == null) return;
		try {
			DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(l, enActualizarBBDD, cd);
			detalles.getDetallesLlibrePanel().setLocationRelativeTo(this.vista);
			if (editMode) detalles.getDetallesLlibrePanel().getBtnEditar().doClick();
			detalles.getDetallesLlibrePanel().setVisible(true);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private Llista pickLlista(String prompt) {
		java.util.List<Llista> llistes = cd.getAllLlistes();
		Object[] options = new Object[llistes.size() + 1];
		for (int i = 0; i < llistes.size(); i++) options[i] = llistes.get(i);
		options[llistes.size()] = I18n.t("menu_create_list");
		Object sel = JOptionPane.showInputDialog(vista, prompt, I18n.t("menu_add_to_list_title"),
			JOptionPane.QUESTION_MESSAGE, null, options,
			options.length > 1 ? options[0] : options[0]);
		if (sel == null) return null;
		if (sel instanceof String) {
			String name = JOptionPane.showInputDialog(vista, I18n.t("dlg_new_list_title"),
				I18n.t("dlg_new_list_title"), JOptionPane.QUESTION_MESSAGE);
			if (name == null || name.isBlank()) return null;
			try {
				Llista nova = cd.addLlista(name.trim());
				refreshComboLlistes();
				return nova;
			} catch (Exception ex) { new DialogoError(ex).showErrorMessage(); return null; }
		}
		return (Llista) sel;
	}

	private void afegirSeleccionatsALlista(int[] rows) {
		Llista sel = pickLlista(I18n.t("dlg_add_to_list_msg", rows.length));
		if (sel == null) return;
		int ok = 0, skip = 0;
		JTable t = this.vista.getjTableBilio();
		for (int row : rows) {
			try {
				long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
				cd.addLlibreToLlista(isbn, sel.getId(), 0.0, false);
				ok++;
			} catch (Exception ignored) { skip++; }
		}
		String msg = I18n.t("dlg_books_added_to_list", ok, sel.getNom());
		if (skip > 0) msg += "\n" + I18n.t("dlg_books_existing_list", skip);
		JOptionPane.showMessageDialog(vista, msg, I18n.t("dlg_added_to_list_title"), JOptionPane.INFORMATION_MESSAGE);
		refreshComboLlistes();
	}

	private void duplicarLlibre(String isbnStr) {
		try {
			Llibre src = MainFrameControl.getInstance().getLlibreIsbn(Long.parseLong(isbnStr));
			if (src == null) return;
			presentacio.detalles.vista.GuardarLlibresDialogo dialeg =
				new presentacio.detalles.vista.GuardarLlibresDialogo();
			new presentacio.detalles.control.GuardarLlibresDialogoControl(dialeg, null, cd);
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
		m.put("editorial",    vista.getFilterEditorial().getText());
		m.put("serie",        vista.getFilterSerie().getText());
		m.put("idioma",       vista.getFilterIdioma().getText());
		String fmt = (String) vista.getFilterFormat().getSelectedItem();
		m.put("format",       fmt != null ? fmt : "");
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
		vista.getFilterEditorial().setText(state.getOrDefault("editorial", ""));
		vista.getFilterSerie().setText(state.getOrDefault("serie", ""));
		vista.getFilterIdioma().setText(state.getOrDefault("idioma", ""));
		String fmtPreset = state.getOrDefault("format", "");
		vista.getFilterFormat().setSelectedItem(fmtPreset);
	}

	private void carregarPreset() {
		int idx = vista.getComboPresets().getSelectedIndex();
		if (idx < 0 || herramienta.Config.getPresetCount() == 0) return;
		applyFilterState(herramienta.Config.loadPreset(idx));
		filtrar();
	}

	private void desarPreset() {
		String name = JOptionPane.showInputDialog(vista, I18n.t("dlg_filter_name_prompt"), I18n.t("dlg_save_filter_title"), JOptionPane.QUESTION_MESSAGE);
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
		if (JOptionPane.showConfirmDialog(vista, I18n.t("dlg_delete_preset", name),
				I18n.t("dlg_delete_preset_title"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
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
			javax.swing.table.TableColumn tc = t.getColumn(colNames()[modelIndex]);
			hiddenCols.put(modelIndex, tc);
			t.removeColumn(tc);
			herramienta.Config.setColVisible(modelIndex, false);
		}
	}

	private void applyColumnVisibility() {
		for (int col = 0; col < colNames().length; col++) {
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
		String estat = Boolean.TRUE.equals(l.getLlegit()) ? I18n.t("filter_read") : I18n.t("filter_unread");
		return new Object[] { "", l.getISBN() + "", l.getNom(), l.getAutor(), l.getAny(), l.getValoracio(), l.getPreu(),
				estat, l.getPagines() + "/" + l.getPaginesLlegides(), "" };
	}

	private void setTable(ArrayList<Llibre> llibres) {
		this.model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		this.model.getDataVector().removeAllElements();
		removeAlldataFiltros();
		setHeader(colNames());
		if (llibres != null) {
			for (Llibre l : llibres) {
				addRow(addLlibreMostrar(l));
			}
		}
		JTable t = this.vista.getjTableBilio();
		t.setRowHeight(50);
		t.getColumnModel().getColumn(COLUMNA_COVER).setPreferredWidth(48);
		t.getColumnModel().getColumn(COLUMNA_COVER).setMinWidth(48);
		t.getColumnModel().getColumn(COLUMNA_COVER).setMaxWidth(56);
		t.getColumnModel().getColumn(COLUMNA_COVER).setCellRenderer(new CoverCellRenderer(t));
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
		t.getColumnModel().getColumn(COLUMNA_LLEGIT).setPreferredWidth(80);
		t.getColumnModel().getColumn(COLUMNA_LLEGIT).setMinWidth(55);
		t.getColumnModel().getColumn(COLUMNA_PROGRES).setPreferredWidth(90);
		t.getColumnModel().getColumn(COLUMNA_PROGRES).setMinWidth(50);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setPreferredWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMinWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMaxWidth(110);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellRenderer(new BotonDetallesRenderer());
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellEditor(new BotonDetallesEditor(new JCheckBox()));
		t.getColumnModel().getColumn(COLUMNA_LLEGIT).setCellRenderer(new LlegitCheckBoxRenderer());
		t.getColumnModel().getColumn(COLUMNA_LLEGIT).setCellEditor(new LlegitCheckBoxEditor());
		t.getColumnModel().getColumn(COLUMNA_PROGRES).setCellRenderer(new ProgressBarRenderer());
		SearchHighlightRenderer highlightRenderer = new SearchHighlightRenderer();
		for (int i = 0; i < t.getColumnCount(); i++) {
			if (i != COLUMNA_COVER && i != COLUMNA_LLEGIT && i != COLUMNA_DETALLS && i != COLUMNA_PROGRES)
				t.getColumnModel().getColumn(i).setCellRenderer(highlightRenderer);
		}

		int[] defaults = { 48, 130, 220, 180, 55, 75, 60, 80, 90, 85 };
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

		if (vista.isGaleriaMode()) vista.getGaleria().updateLlibres(llibres);
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
						Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
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
			String shelf = currentLlistaId == null ? I18n.t("lbl_all_lists") : currentShelfName();
			String count = shown == total
				? total + " llibres"
				: shown + " / " + total + " llibres";
			((MainFramePanel) w).getStatusBar().setText(shelf + "  —  " + count);
		}
	}

	private String currentShelfName() {
		Object sel = this.vista.getComboLlistes().getSelectedItem();
		if (sel instanceof domini.Llista) return ((domini.Llista) sel).getNom();
		return I18n.t("lbl_all_lists");
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
			JOptionPane.showMessageDialog(vista, I18n.t("dlg_import_warn"), I18n.t("dlg_error_title"),
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
		herramienta.BookImporter.ImportResult r = herramienta.BookImporter.importCSV(fc.getSelectedFile(), cd);
		if (r.errors() > 0 && r.imported() == 0 && !r.errorDetails().isEmpty()) {
			new DialogoError(new Exception(r.errorDetails())).showErrorMessage(); return;
		}
		String msg = r.imported() + " llibres importats.";
		if (r.errors() > 0) msg += "\n" + r.errors() + " errors:" + r.errorDetails();
		JOptionPane.showMessageDialog(this.vista, msg, I18n.t("dlg_import_title"),
			r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
		quitarFiltros();
	}

	private void importarCalibre() {
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setDialogTitle(I18n.t("dlg_calibre_choose_title"));
		fc.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
		if (fc.showOpenDialog(this.vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		java.io.File dbFile = new java.io.File(fc.getSelectedFile(), "metadata.db");
		if (!dbFile.exists()) {
			JOptionPane.showMessageDialog(this.vista, I18n.t("dlg_calibre_not_found"),
				I18n.t("dlg_calibre_choose_title"), JOptionPane.ERROR_MESSAGE); return;
		}
		String sqlite3 = herramienta.BookImporter.findSqlite3();
		if (sqlite3 == null) {
			JOptionPane.showMessageDialog(this.vista, I18n.t("dlg_calibre_no_sqlite3"),
				I18n.t("dlg_calibre_choose_title"), JOptionPane.ERROR_MESSAGE); return;
		}
		try {
			herramienta.BookImporter.ImportResult r = herramienta.BookImporter.importCalibre(dbFile, sqlite3, cd);
			refresh();
			String msg = r.imported() + " " + I18n.t("lbl_imported_ok");
			if (r.skipped() > 0) msg += "\n" + r.skipped() + " " + I18n.t("lbl_skipped");
			if (r.errors() > 0) msg += "\n" + r.errors() + " " + I18n.t("lbl_errors");
			JOptionPane.showMessageDialog(this.vista, msg, I18n.t("dlg_calibre_choose_title"),
				r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
	}


	private void escanejarISBN() {
		String isbn = JOptionPane.showInputDialog(this.vista,
			I18n.t("dlg_scan_isbn_msg"),
			I18n.t("dlg_scan_isbn_title"), JOptionPane.QUESTION_MESSAGE);
		if (isbn == null || isbn.isBlank()) return;

		GuardarLlibresDialogo dialeg = new GuardarLlibresDialogo();
		new GuardarLlibresDialogoControl(dialeg, null, cd);
		dialeg.getTextISBN().setText(isbn.trim());

		String finalIsbn = isbn.trim();
		Thread fetchThread = new Thread(() -> {
			java.util.Map<String, String> meta = OpenLibraryClient.lookupByISBN(finalIsbn);
			SwingUtilities.invokeLater(() -> {
				if (!dialeg.isVisible()) return;
				if (meta.containsKey("error")) {
					JOptionPane.showMessageDialog(dialeg,
						I18n.t("dlg_network_error") + "\n" + meta.get("error"),
						I18n.t("dlg_network_error_title"), JOptionPane.WARNING_MESSAGE);
					return;
				}
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

	private java.util.List<Llibre> getCurrentViewBooks() {
		javax.swing.table.TableModel m = this.vista.getjTableBilio().getModel();
		java.util.List<Llibre> view = new java.util.ArrayList<>();
		for (int row = 0; row < m.getRowCount(); row++) {
			try {
				long isbn = Long.parseLong(m.getValueAt(row, COLUMNA_ISBN).toString());
				Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
				if (l != null) view.add(l);
			} catch (Exception ignored) {}
		}
		return view;
	}

	private void exportarCSV() {
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setSelectedFile(new java.io.File("biblioteca.csv"));
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
		if (fc.showSaveDialog(this.vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		java.io.File f = fc.getSelectedFile();
		if (!f.getName().toLowerCase().endsWith(".csv")) f = new java.io.File(f.getPath() + ".csv");
		try {
			herramienta.BookExporter.exportCSV(f, getCurrentViewBooks(), cd);
			JOptionPane.showMessageDialog(this.vista, I18n.t("dlg_export_done", f.getAbsolutePath()),
				I18n.t("dlg_export_title"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
	}

	private void exportarJSON() {
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setSelectedFile(new java.io.File("biblioteca.json"));
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
		if (fc.showSaveDialog(this.vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		java.io.File f = fc.getSelectedFile();
		if (!f.getName().toLowerCase().endsWith(".json")) f = new java.io.File(f.getPath() + ".json");
		try {
			herramienta.BookExporter.exportJSON(f, cd);
			JOptionPane.showMessageDialog(this.vista, I18n.t("dlg_export_done", f.getAbsolutePath()),
				I18n.t("dlg_export_json_title"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
	}

	private void exportarHTML() {
		javax.swing.JCheckBox chkShelf = new javax.swing.JCheckBox(I18n.t("html_group_shelf_opt"), false);
		javax.swing.JCheckBox chkTable = new javax.swing.JCheckBox(I18n.t("html_table_view_opt"), false);
		int r = JOptionPane.showConfirmDialog(this.vista, new Object[]{chkShelf, chkTable},
			I18n.t("dlg_export_html_title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (r != JOptionPane.OK_OPTION) return;
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setSelectedFile(new java.io.File("biblioteca.html"));
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HTML files", "html", "htm"));
		if (fc.showSaveDialog(this.vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		java.io.File f = fc.getSelectedFile();
		if (!f.getName().toLowerCase().endsWith(".html") && !f.getName().toLowerCase().endsWith(".htm"))
			f = new java.io.File(f.getPath() + ".html");
		try {
			herramienta.BookExporter.exportHTML(f, getCurrentViewBooks(), cd, chkShelf.isSelected(), chkTable.isSelected());
			JOptionPane.showMessageDialog(this.vista, I18n.t("dlg_export_done", f.getAbsolutePath()),
				I18n.t("dlg_export_html_title"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
	}

	private void fetchMissingCovers() {

		ArrayList<Llibre> all = cd.getAllLlibres();
		List<Llibre> missing = all.stream()
			.filter(l -> !l.hasBlob() && l.getImatgeBlob() == null)
			.collect(Collectors.toList());
		if (missing.isEmpty()) {
			JOptionPane.showMessageDialog(this.vista,
				I18n.t("dlg_fetch_portades_all_done"), I18n.t("dlg_fetch_portades_title"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		int total = missing.size();
		javax.swing.JProgressBar bar = new javax.swing.JProgressBar(0, total);
		bar.setStringPainted(true);
		JLabel lbl = new JLabel(I18n.t("dlg_fetch_portades_progress", 0, total));
		JPanel p = new JPanel(new java.awt.BorderLayout(8, 8));
		p.add(lbl, java.awt.BorderLayout.NORTH);
		p.add(bar, java.awt.BorderLayout.CENTER);
		javax.swing.JDialog dlg = new javax.swing.JDialog(
			SwingUtilities.getWindowAncestor(this.vista), I18n.t("dlg_fetch_portades_title"),
			java.awt.Dialog.ModalityType.MODELESS);
		dlg.setContentPane(p);
		dlg.pack(); dlg.setSize(360, 90);
		dlg.setLocationRelativeTo(this.vista);
		dlg.setVisible(true);
		this.vista.getBtnFetchCovers().setEnabled(false);
		int[] done = {0}, fetched = {0};
		for (Llibre l : missing) {
			Thread t = new Thread(() -> {
				try {
					byte[] blob = OpenLibraryClient.fetchCoverByISBN(String.valueOf(l.getISBN()));
					if (blob != null && blob.length > 0) {
						cd.setLlibreBlob(l.getISBN(), blob);
						synchronized (done) { fetched[0]++; }
					}
				} catch (Exception ignored) {} finally {
					synchronized (done) {
						done[0]++;
						int d = done[0];
						SwingUtilities.invokeLater(() -> {
							bar.setValue(d);
							lbl.setText(I18n.t("dlg_fetch_portades_progress", d, total));
							if (d >= total) {
								dlg.dispose();
								this.vista.getBtnFetchCovers().setEnabled(true);
								JOptionPane.showMessageDialog(this.vista,
									I18n.t("dlg_fetch_portades_done", fetched[0], total),
									I18n.t("dlg_fetch_portades_done_title"), JOptionPane.INFORMATION_MESSAGE);
								refresh();
							}
						});
					}
				}
			});
			t.setDaemon(true);
			t.start();
		}
	}

	private void exportarPDF() {
		herramienta.BookExporter.exportPDF(getCurrentViewBooks());
	}

	private void importarJSON() {
		javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
		if (fc.showOpenDialog(this.vista) != javax.swing.JFileChooser.APPROVE_OPTION) return;
		herramienta.BookImporter.ImportResult r;
		try { r = herramienta.BookImporter.importJSON(fc.getSelectedFile(), cd); }
		catch (Exception e) { new DialogoError(e).showErrorMessage(); return; }
		String msg = I18n.t("dlg_import_json_msg", r.imported());
		if (r.skipped() > 0) msg += "\n" + I18n.t("dlg_import_json_skipped", r.skipped());
		if (r.errors() > 0) msg += "\n" + I18n.t("dlg_import_json_errors", r.errors());
		JOptionPane.showMessageDialog(this.vista, msg, I18n.t("dlg_import_json_title"),
			r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
		quitarFiltros();
	}

	private void mostrarEstadistiques() {

		ArrayList<Llibre> global = cd.getAllLlibres();
		if (global.isEmpty()) {
			JOptionPane.showMessageDialog(vista, I18n.t("dlg_empty_library"), I18n.t("dlg_stats_title"),
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// ── Global summary ─────────────────────────────────────────────────────
		String summary = buildStatsSummary(global, I18n.t("lbl_all_library"));

		javax.swing.JTextArea txtSummary = new javax.swing.JTextArea(summary);
		txtSummary.setEditable(false);
		txtSummary.setFont(herramienta.UITheme.FONT_BASE);
		txtSummary.setBackground(herramienta.UITheme.BG_PANEL);
		txtSummary.setForeground(herramienta.UITheme.TEXT_DARK);
		txtSummary.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));

		// ── Per-shelf breakdown table ──────────────────────────────────────────
		DefaultTableModel shelfModel = new DefaultTableModel(
			new String[]{I18n.t("col_stats_llista"), I18n.t("col_stats_llibres"), I18n.t("col_stats_llegits"), I18n.t("col_stats_pct"), I18n.t("col_stats_val")}, 0) {
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
		shelfScroll.setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("lbl_per_list")));

		// ── Reading goal ──────────────────────────────────────────────────────
		int totalLlegits = (int) global.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
		int savedGoal = herramienta.Config.getReadingGoal();
		javax.swing.JPanel goalPanel = new javax.swing.JPanel(new java.awt.BorderLayout(6, 4));
		goalPanel.setBackground(herramienta.UITheme.BG_PANEL);
		goalPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
			javax.swing.BorderFactory.createLineBorder(herramienta.UITheme.BORDER_CLR),
			I18n.t("lbl_reading_goal_section"), javax.swing.border.TitledBorder.LEFT,
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

		javax.swing.JLabel lblGoal = new javax.swing.JLabel(I18n.t("lbl_goal"));
		herramienta.UITheme.styleLabel(lblGoal);
		javax.swing.JPanel goalControls = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
		goalControls.setBackground(herramienta.UITheme.BG_PANEL);
		goalControls.add(lblGoal);
		goalControls.add(goalSpinner);
		goalControls.add(new javax.swing.JLabel(I18n.t("lbl_read_count", totalLlegits)));
		goalPanel.add(goalControls, java.awt.BorderLayout.NORTH);
		goalPanel.add(goalBar, java.awt.BorderLayout.CENTER);

		// ── Tab 1: General ───────────────────────────────────────────────────
		javax.swing.JPanel tab1 = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
		tab1.setBackground(herramienta.UITheme.BG_PANEL);
		tab1.add(goalPanel, java.awt.BorderLayout.NORTH);
		javax.swing.JPanel statsPanel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
		statsPanel.setBackground(herramienta.UITheme.BG_PANEL);
		statsPanel.add(txtSummary, java.awt.BorderLayout.NORTH);
		if (shelfModel.getRowCount() > 0) statsPanel.add(shelfScroll, java.awt.BorderLayout.CENTER);
		tab1.add(statsPanel, java.awt.BorderLayout.CENTER);

		// ── Tab 2: Charts (reading per year + publisher breakdown) ────────────
		javax.swing.JPanel tab2 = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 8));
		tab2.setBackground(herramienta.UITheme.BG_PANEL);
		tab2.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
		tab2.add(buildReadingChart(global));
		tab2.add(buildPublisherChart(global));

		// ── Tab 3: Tag cloud ──────────────────────────────────────────────────
		javax.swing.JPanel tab3 = buildTagCloud(global);

		// ── Tab 4: Reading pace ───────────────────────────────────────────────
		javax.swing.JPanel tab4 = buildReadingPacePanel(global);

		javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
		tabs.setBackground(herramienta.UITheme.BG_PANEL);
		tabs.addTab(I18n.t("stats_tab_general"), tab1);
		tabs.addTab(I18n.t("stats_tab_charts"), new javax.swing.JScrollPane(tab2));
		tabs.addTab(I18n.t("stats_tab_tags"), new javax.swing.JScrollPane(tab3));
		tabs.addTab(I18n.t("stats_tab_pace"), tab4);

		javax.swing.JDialog dlg = new javax.swing.JDialog(SwingUtilities.getWindowAncestor(vista),
			I18n.t("dlg_stats_title"), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		dlg.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
		dlg.getContentPane().setBackground(herramienta.UITheme.BG_PANEL);
		dlg.add(tabs);
		javax.swing.JButton btnClose = new javax.swing.JButton(I18n.t("btn_close"));
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
		dlg.setSize(600, 500);
		dlg.setMinimumSize(new java.awt.Dimension(500, 400));
		dlg.setLocationRelativeTo(vista);
		dlg.setVisible(true);
	}

	private javax.swing.JPanel buildReadingChart(ArrayList<Llibre> books) {
		// Count books finished per year (from data_lectura or llegit+any)
		java.util.Map<Integer, Long> perYear = books.stream()
			.filter(l -> Boolean.TRUE.equals(l.getLlegit()))
			.filter(l -> {
				if (l.getDataLectura() != null && !l.getDataLectura().isEmpty()) return true;
				return l.getAny() != null && l.getAny() > 1900;
			})
			.collect(Collectors.groupingBy(l -> {
				if (l.getDataLectura() != null && l.getDataLectura().length() >= 4) {
					try { return Integer.parseInt(l.getDataLectura().substring(0, 4)); } catch (Exception e2) {}
				}
				return l.getAny() != null ? l.getAny() : 0;
			}, Collectors.counting()));
		return new javax.swing.JPanel() {
			{ setPreferredSize(new java.awt.Dimension(560, 180)); setBackground(herramienta.UITheme.BG_PANEL); setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_chart_books_year"))); }
			@Override protected void paintComponent(java.awt.Graphics g) {
				super.paintComponent(g);
				if (perYear.isEmpty()) { g.drawString(I18n.t("stats_no_data"), 20, 60); return; }
				java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
				g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
				java.util.List<Integer> years = perYear.keySet().stream().filter(y -> y > 1900).sorted().collect(Collectors.toList());
				if (years.isEmpty()) { g2.drawString(I18n.t("stats_no_data"), 20, 60); return; }
				long maxVal = perYear.values().stream().mapToLong(v -> v).max().orElse(1);
				int pad = 40, barW = Math.max(18, (getWidth() - pad * 2) / years.size() - 4);
				int chartH = getHeight() - pad * 2;
				g2.setColor(herramienta.UITheme.ACCENT);
				for (int i = 0; i < years.size(); i++) {
					int yr = years.get(i);
					long cnt = perYear.getOrDefault(yr, 0L);
					int bH = (int) (chartH * cnt / maxVal);
					int x = pad + i * (barW + 4);
					int y = pad + chartH - bH;
					g2.fillRect(x, y, barW, bH);
					g2.setColor(herramienta.UITheme.TEXT_DARK);
					g2.setFont(herramienta.UITheme.FONT_BASE.deriveFont(9f));
					g2.drawString(String.valueOf(yr), x, getHeight() - 5);
					g2.drawString(String.valueOf(cnt), x + 2, y - 2);
					g2.setColor(herramienta.UITheme.ACCENT);
				}
			}
		};
	}

	private javax.swing.JPanel buildPublisherChart(ArrayList<Llibre> books) {
		java.util.Map<String, Long> byPublisher = books.stream()
			.filter(l -> l.getEditorial() != null && !l.getEditorial().isEmpty())
			.collect(Collectors.groupingBy(l -> l.getEditorial(), Collectors.counting()));
		java.util.List<java.util.Map.Entry<String, Long>> top = byPublisher.entrySet().stream()
			.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
			.limit(10).collect(Collectors.toList());
		return new javax.swing.JPanel() {
			{ setPreferredSize(new java.awt.Dimension(560, 200)); setBackground(herramienta.UITheme.BG_PANEL); setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_chart_publishers"))); }
			@Override protected void paintComponent(java.awt.Graphics g) {
				super.paintComponent(g);
				if (top.isEmpty()) { g.drawString(I18n.t("stats_no_data"), 20, 60); return; }
				java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
				g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
				long maxVal = top.get(0).getValue();
				int pad = 8, lblW = 130, chartH = 22, gap = 4;
				for (int i = 0; i < top.size(); i++) {
					int y = pad + i * (chartH + gap);
					String lbl = top.get(i).getKey();
					long cnt = top.get(i).getValue();
					int barW = (int) ((getWidth() - lblW - pad * 3 - 40) * cnt / maxVal);
					g2.setColor(herramienta.UITheme.TEXT_MID);
					g2.setFont(herramienta.UITheme.FONT_BASE.deriveFont(10f));
					java.awt.FontMetrics fm = g2.getFontMetrics();
					String lblTrunc = lbl.length() > 18 ? lbl.substring(0, 17) + "…" : lbl;
					g2.drawString(lblTrunc, pad, y + chartH - 6);
					g2.setColor(herramienta.UITheme.ACCENT);
					g2.fillRect(lblW + pad, y + 2, barW, chartH - 4);
					g2.setColor(herramienta.UITheme.TEXT_DARK);
					g2.drawString(String.valueOf(cnt), lblW + pad + barW + 4, y + chartH - 6);
				}
			}
		};
	}

	private javax.swing.JPanel buildTagCloud(ArrayList<Llibre> books) {
		javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 6));
		panel.setBackground(herramienta.UITheme.BG_PANEL);
		panel.setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.t("stats_tab_tags")));

		java.util.Map<String, Long> tagCount = new java.util.HashMap<>();
		for (Llibre l : books) {
			try {
				for (domini.Tag t : cd.getTagsForLlibre(l.getISBN()))
					tagCount.merge(t.getNom(), 1L, Long::sum);
			} catch (Exception ignored) {}
		}
		if (tagCount.isEmpty()) {
			javax.swing.JLabel lbl = new javax.swing.JLabel(I18n.t("stats_no_tags"));
			herramienta.UITheme.styleLabel(lbl);
			panel.add(lbl);
			return panel;
		}
		long maxCount = tagCount.values().stream().mapToLong(v -> v).max().orElse(1);
		tagCount.entrySet().stream()
			.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
			.forEach(e -> {
				float size = 11f + 14f * e.getValue() / maxCount;
				javax.swing.JLabel lbl = new javax.swing.JLabel(e.getKey() + " (" + e.getValue() + ")");
				lbl.setFont(herramienta.UITheme.FONT_BASE.deriveFont(size));
				lbl.setForeground(herramienta.UITheme.ACCENT);
				lbl.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
				lbl.setToolTipText(e.getValue() + " " + I18n.t("stats_books_with_tag"));
				panel.add(lbl);
			});
		return panel;
	}

	private javax.swing.JPanel buildReadingPacePanel(ArrayList<Llibre> books) {
		javax.swing.JPanel panel = new javax.swing.JPanel();
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
		panel.setBackground(herramienta.UITheme.BG_PANEL);
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));

		int currentYear = java.time.LocalDate.now().getYear();
		int dayOfYear = java.time.LocalDate.now().getDayOfYear();

		// Books finished this year
		long finishedThisYear = books.stream()
			.filter(l -> Boolean.TRUE.equals(l.getLlegit()))
			.filter(l -> {
				if (l.getDataLectura() != null && l.getDataLectura().length() >= 4) {
					try { return Integer.parseInt(l.getDataLectura().substring(0, 4)) == currentYear; } catch (Exception e2) {}
				}
				return false;
			}).count();
		double booksPerDay = dayOfYear > 0 ? (double) finishedThisYear / dayOfYear : 0;
		double booksPerMonth = booksPerDay * 30.44;
		double projectedYear = booksPerDay * 365;

		// Pages across all books with reading progress
		long totalPages = books.stream().mapToLong(Llibre::getPaginesLlegides).sum();
		int goal = herramienta.Config.getReadingGoal();

		java.util.function.Consumer<String> addLine = text -> {
			javax.swing.JLabel lbl = new javax.swing.JLabel(text);
			herramienta.UITheme.styleLabel(lbl);
			lbl.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
			panel.add(lbl);
			panel.add(javax.swing.Box.createVerticalStrut(6));
		};
		addLine.accept(I18n.t("stats_pace_year", currentYear, finishedThisYear));
		addLine.accept(I18n.t("stats_pace_per_month", String.format("%.1f", booksPerMonth)));
		addLine.accept(I18n.t("stats_pace_projected", String.format("%.0f", projectedYear)));
		if (goal > 0) {
			double remaining = goal - finishedThisYear;
			int daysLeft = 365 - dayOfYear;
			addLine.accept(I18n.t("stats_pace_goal_remaining", goal, (int) remaining, daysLeft));
		}
		addLine.accept(I18n.t("stats_pace_total_pages", totalPages));
		return panel;
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
			.map(e -> "  " + e.getKey() + ": " + e.getValue() + " " + (e.getValue() > 1 ? I18n.t("stats_book_plural") : I18n.t("stats_book_singular")))
			.collect(Collectors.joining("\n"));
		return scope + "\n" +
			I18n.t("stats_total") + " " + total + "  ·  " +
			I18n.t("stats_llegits_colon") + " " + llegits + " (" + String.format("%.1f", 100.0 * llegits / total) + "%)  ·  " +
			I18n.t("stats_no_llegits_colon") + " " + (total - llegits) + "\n" +
			I18n.t("stats_avg_rating_colon") + " " + String.format("%.2f", avgVal) + " / 10  ·  " +
			I18n.t("stats_avg_price_colon") + " " + String.format("%.2f", avgPreu) + " " + herramienta.Config.getCurrencySymbol() + "\n" +
			I18n.t("stats_top_years") + "\n" + (topAnys.isEmpty() ? "  " + I18n.t("stats_no_years") : topAnys);
	}

	private void mostrarLlibreAleatori() {
		if (biblio == null || biblio.isEmpty()) {
			JOptionPane.showMessageDialog(vista, I18n.t("dlg_no_books_view"), I18n.t("dlg_aleatori_title"),
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		java.util.List<Llibre> noLlegits = biblio.stream()
			.filter(l -> !Boolean.TRUE.equals(l.getLlegit()))
			.collect(Collectors.toList());
		if (noLlegits.isEmpty()) {
			JOptionPane.showMessageDialog(vista, I18n.t("dlg_all_read"), I18n.t("dlg_aleatori_title"),
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
		DetallesLlibrePanelControl detalles = new DetallesLlibrePanelControl(aleatori, enActualizarBBDD, cd);
		detalles.getDetallesLlibrePanel().setLocationRelativeTo(vista);
		detalles.getDetallesLlibrePanel().setVisible(true);
	}

	private void prestarLlibre(long isbn) {
		String nom = JOptionPane.showInputDialog(vista,
			I18n.t("dlg_loan_msg"), I18n.t("dlg_loan_dialog_title"), JOptionPane.QUESTION_MESSAGE);
		if (nom == null || nom.isBlank()) return;
		try {
			cd.prestarLlibre(isbn, nom.trim());
			loanedISBNs = cd.getLoanedISBNs();
			vista.getjTableBilio().repaint();
			JOptionPane.showMessageDialog(vista, I18n.t("dlg_loan_done", nom.trim()),
				I18n.t("dlg_loan_done_title"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
	}

	private void mostrarAfegitsRecentment() {
		ArrayList<Llibre> recents = cd.getRecentlyAdded();
		if (recents.isEmpty()) {
			JOptionPane.showMessageDialog(vista, I18n.t("dlg_no_books_recent"), I18n.t("dlg_recently_added_title"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		biblio = recents;
		useDBPagination = false;
		currentLlistaId = null;
		currentPage = 0;
		showPage(0);
	}

	private void mostrarLlegitsRecentment() {
		ArrayList<Llibre> llegits = new ArrayList<>(
			cd.getAllLlibres().stream()
				.filter(l -> Boolean.TRUE.equals(l.getLlegit()))
				.collect(java.util.stream.Collectors.toList()));
		if (llegits.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "Cap llibre marcat com a llegit.", "Llegits", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		biblio = llegits;
		useDBPagination = false;
		currentLlistaId = null;
		currentPage = 0;
		showPage(0);
	}

	private void mostrarDesitjats() {
		ArrayList<Llibre> desitjats = new ArrayList<>(
			cd.getAllLlibres().stream()
				.filter(l -> Boolean.TRUE.equals(l.getDesitjat()))
				.collect(java.util.stream.Collectors.toList()));
		biblio = desitjats;
		useDBPagination = false;
		currentLlistaId = null;
		currentPage = 0;
		showPage(0);
	}

	private void mostrarEnCurs() {
		ArrayList<Llibre> enCurs = new ArrayList<>(
			cd.getAllLlibres().stream()
				.filter(l -> l.getPaginesLlegides() > 0 && !Boolean.TRUE.equals(l.getLlegit()))
				.collect(java.util.stream.Collectors.toList()));
		biblio = enCurs;
		useDBPagination = false;
		currentLlistaId = null;
		currentPage = 0;
		showPage(0);
	}

	private void filtrar() {
		LlibreFilter f = LlibreFilter.empty();

		String autorTyped = this.vista.getTextAutor().getText().trim();
		if (!autorTyped.isEmpty()) f.autor = autorTyped;

		String nomTyped = this.vista.getTextNom().getText().trim();
		if (!nomTyped.isEmpty()) f.nom = nomTyped;

		String isbnText = this.vista.getTextISBN().getText().trim();
		if (!isbnText.isEmpty()) {
			try { f.isbn = Long.parseLong(isbnText); } catch (NumberFormatException ignored) {}
		}

		try { f.anyMin       = Integer.parseInt(this.vista.getAnyMin().getText().trim());         } catch (NumberFormatException ignored) {}
		try { f.anyMax       = Integer.parseInt(this.vista.getAnyMax().getText().trim());         } catch (NumberFormatException ignored) {}
		try { f.valoracioMin = Double.parseDouble(this.vista.getValoracioMin().getText().trim()); } catch (NumberFormatException ignored) {}
		try { f.valoracioMax = Double.parseDouble(this.vista.getValoracioMax().getText().trim()); } catch (NumberFormatException ignored) {}
		try { f.preuMin      = Double.parseDouble(this.vista.getPreuMin().getText().trim());      } catch (NumberFormatException ignored) {}
		try { f.preuMax      = Double.parseDouble(this.vista.getPreuMax().getText().trim());      } catch (NumberFormatException ignored) {}

		if (this.vista.getchckbxLlegit().isSelected())  f.llegit = true;
		if (this.vista.getchckbxNoLlegit().isSelected()) f.llegit = false;

		Object selTag = this.vista.getComboTagFilter().getSelectedItem();
		if (selTag instanceof Tag) f.tagId = ((Tag) selTag).getId();

		String editorial = this.vista.getFilterEditorial().getText().trim();
		if (!editorial.isEmpty()) f.editorial = editorial;
		String serie = this.vista.getFilterSerie().getText().trim();
		if (!serie.isEmpty()) f.serie = serie;
		String idioma = this.vista.getFilterIdioma().getText().trim();
		if (!idioma.isEmpty()) f.idioma = idioma;
		String format = (String) this.vista.getFilterFormat().getSelectedItem();
		if (format != null && !format.isEmpty()) f.format = format;

		if (currentLlistaId != null) {
			setTable(cd.aplicarFiltres(biblio, f));
		} else {
			setTable(MainFrameControl.getInstance().aplicarFiltres(f));
		}
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
		if (this.vista.getComboTagFilter().getItemCount() > 0)
			this.vista.getComboTagFilter().setSelectedIndex(0);
		this.vista.getFilterEditorial().setText("");
		this.vista.getFilterSerie().setText("");
		this.vista.getFilterIdioma().setText("");
		this.vista.getFilterFormat().setSelectedIndex(0);
		removeAlldataFiltros();
		currentPage = 0;
		showPage(0);
	}

	private void showPage(int page) {
		if (biblio == null) {
			paginatedMode = false;
			vista.getPaginationPanel().setVisible(false);
			setTable(new ArrayList<>());
			return;
		}
		if (useDBPagination && cd.isLargeLibrary()) {
			int totalCount = cd.countLlibresDB();
			if (totalCount <= PAGE_SIZE) {
				paginatedMode = false;
				vista.getPaginationPanel().setVisible(false);
				setTable(cd.getLlibresPage(0, PAGE_SIZE));
				return;
			}
			int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
			page = Math.max(0, Math.min(page, totalPages - 1));
			currentPage = page;
			paginatedMode = true;
			setTable(cd.getLlibresPage(page * PAGE_SIZE, PAGE_SIZE));
			vista.getLblPagina().setText(I18n.t("page_info_java", page + 1, totalPages));
			vista.getBtnPaginaAnterior().setEnabled(page > 0);
			vista.getBtnPaginaSeguent().setEnabled(page < totalPages - 1);
			vista.getPaginationPanel().setVisible(true);
			return;
		}
		if (biblio.size() <= PAGE_SIZE) {
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

		vista.getLblPagina().setText(I18n.t("page_info_java", page + 1, totalPages));
		vista.getBtnPaginaAnterior().setEnabled(page > 0);
		vista.getBtnPaginaSeguent().setEnabled(page < totalPages - 1);
		vista.getPaginationPanel().setVisible(true);
	}

	// ── Cover thumbnail renderer ──────────────────────────────────────────────

	private class CoverCellRenderer extends JLabel implements javax.swing.table.TableCellRenderer {
		private final JTable table;
		CoverCellRenderer(JTable table) {
			this.table = table;
			setHorizontalAlignment(JLabel.CENTER);
			setVerticalAlignment(JLabel.CENTER);
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object value,
				boolean selected, boolean focus, int row, int col) {
			setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
			setIcon(null);
			try {
				long isbn = Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN));
				javax.swing.ImageIcon icon = coverCache.get(isbn);
				if (icon != null) {
					setIcon(icon);
				} else if (!coverLoading.contains(isbn)) {
					coverLoading.add(isbn);
					final int r = row, c = col;
					new javax.swing.SwingWorker<javax.swing.ImageIcon, Void>() {
						@Override protected javax.swing.ImageIcon doInBackground() {
							Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
							return l != null ? scaledCover(loadCoverBytes(l)) : null;
						}
						@Override protected void done() {
							try {
								javax.swing.ImageIcon img = get();
								if (img != null) coverCache.put(isbn, img);
							} catch (Exception ignored) {}
							coverLoading.remove(isbn);
							if (r < table.getRowCount())
								table.repaint(table.getCellRect(r, c, false));
						}
					}.execute();
				}
			} catch (Exception ignored) {}
			return this;
		}
	}

	private byte[] loadCoverBytes(Llibre l) {
		byte[] blob = l.getImatgeBlob();
		if (blob == null && l.hasBlob())
			blob = cd.getLlibreBlob(l.getISBN());
		if (blob != null) return blob;
		String path = l.getImatge();
		if (path != null && !path.isEmpty()) {
			try { return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)); } catch (Exception ignored) {}
		}
		return null;
	}

	private static javax.swing.ImageIcon scaledCover(byte[] data) {
		if (data == null) return null;
		try {
			java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
			if (img == null) return null;
			int h = 46;
			int w = Math.max(1, (int)(img.getWidth() * (h / (double) img.getHeight())));
			return new javax.swing.ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
		} catch (Exception ignored) { return null; }
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
					"(?i)(" + escapedQ + ")",
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
				Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
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
			setSelected(I18n.t("filter_read").equals(val));
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
							Llibre l = MainFrameControl.getInstance().getLlibreIsbn(Long.parseLong(isbn));
							if (l == null) return;
							l.setLlegit(newLlegit);
							cd.updateLlibre(l);
							SwingUtilities.invokeLater(() -> actualizarfila(l));
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
			cb.setSelected(I18n.t("filter_read").equals(value));
			cb.setBackground(UITheme.ACCENT);
			cb.setForeground(Color.WHITE);
			return cb;
		}

		@Override
		public Object getCellEditorValue() {
			return cb.isSelected() ? I18n.t("filter_read") : I18n.t("filter_unread");
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
			? I18n.t("dlg_confirm_delete_one", t.getValueAt(rows[0], COLUMNA_NOM))
			: I18n.t("dlg_confirm_delete_n", rows.length);
		if (JOptionPane.showConfirmDialog(vista, msg, I18n.t("dlg_confirm_delete_title"),
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
		// collect ISBNs before rows shift during deletion
		java.util.List<Long> isbns = new java.util.ArrayList<>();
		for (int row : rows) isbns.add(Long.parseLong((String) t.getValueAt(row, COLUMNA_ISBN)));
		for (long isbn : isbns) {
			try {
				Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
				if (l == null) continue;
				undoBuffer.push(l);
				if (undoBuffer.size() > UNDO_MAX) undoBuffer.removeLast();
				cd.deleteLlibre(l);
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
				if (biblio != null) biblio.removeIf(b -> b.getISBN().equals(l.getISBN()));
				break;
			}
		}
		updateTitleBar();
	}

	public Integer getCurrentLlistaId() { return currentLlistaId; }

	public void refresh() {
		currentPage = 0;
		if (currentLlistaId != null) {
			biblio = cd.getLlibresInLlista(currentLlistaId);
			useDBPagination = false;
		} else {
			biblio = cd.getAllLlibres();
			useDBPagination = cd.isLargeLibrary();
		}
		quitarFiltros();
	}

	public void refreshComboLlistes() {
		javax.swing.JComboBox<Object> combo = this.vista.getComboLlistes();
		// Remove listener temporarily to avoid triggering onLlistaSelected
		java.awt.event.ActionListener[] listeners = combo.getActionListeners();
		for (java.awt.event.ActionListener al : listeners) combo.removeActionListener(al);
		combo.removeAllItems();

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
		// Restore previously selected shelf if it still exists; else fall back to "Totes"
		int selectIdx = 0;
		if (currentLlistaId != null) {
			for (int i = 1; i < combo.getItemCount(); i++) {
				Object item = combo.getItemAt(i);
				if (item instanceof Llista && ((Llista) item).getId() == currentLlistaId) {
					selectIdx = i;
					break;
				}
			}
			if (selectIdx == 0) currentLlistaId = null; // shelf no longer exists
		}
		combo.setSelectedIndex(selectIdx);
		for (java.awt.event.ActionListener al : listeners) combo.addActionListener(al);
		vista.rebuildSidebarShelves(cd.getAllLlistes(), counts);
		if (currentLlistaId != null) {
			biblio = cd.getLlibresInLlista(currentLlistaId);
			useDBPagination = false;
		} else {
			biblio = cd.getAllLlibres();
			useDBPagination = cd.isLargeLibrary();
		}
		currentPage = 0;
		showPage(0);
	}

	public void refreshComboTags() {
		javax.swing.JComboBox<Object> combo = this.vista.getComboTagFilter();
		combo.removeAllItems();
		combo.addItem("Totes les etiquetes");
		for (Tag t : cd.getAllTags()) combo.addItem(t);
	}

	private void onLlistaSelected() {
		Object sel = this.vista.getComboLlistes().getSelectedItem();
		if (sel instanceof Llista) {
			currentLlistaId = ((Llista) sel).getId();
			biblio = cd.getLlibresInLlista(currentLlistaId);
			useDBPagination = false;
		} else {
			currentLlistaId = null;
			biblio = cd.getAllLlibres();
			useDBPagination = cd.isLargeLibrary();
		}
		currentPage = 0;
		showPage(0);
	}

	private void showGaleriaContextMenu(java.awt.event.MouseEvent e, List<Llibre> selected) {
		if (selected.isEmpty()) return;
		JPopupMenu menu = new JPopupMenu();

		JMenuItem itemObrir = new JMenuItem("Obrir detalls");
		itemObrir.setEnabled(selected.size() == 1);
		itemObrir.addActionListener(ev -> {
			try {
				DetallesLlibrePanelControl d = new DetallesLlibrePanelControl(selected.get(0), enActualizarBBDD, cd);
				d.getDetallesLlibrePanel().setLocationRelativeTo(vista);
				d.getDetallesLlibrePanel().setVisible(true);
			} catch (Exception ex) { new DialogoError(ex).showErrorMessage(); }
		});
		menu.add(itemObrir);

		JMenuItem itemEliminar = new JMenuItem(
			selected.size() > 1 ? "Eliminar " + selected.size() + " llibres" : "Eliminar");
		itemEliminar.addActionListener(ev -> eliminarLlibresGaleria(selected));
		menu.add(itemEliminar);

		JMenuItem itemAfegirLlista = new JMenuItem(
			selected.size() > 1 ? "Afegir " + selected.size() + " a llista..." : "Afegir a llista...");
		itemAfegirLlista.addActionListener(ev -> afegirLlibresGaleriaALlista(selected));
		menu.add(itemAfegirLlista);

		if (selected.size() > 1) {
			JMenuItem itemBatchEdit = new JMenuItem(I18n.t("menu_batch_edit_n", selected.size()));
			java.util.List<Long> batchIsbns = selected.stream().map(Llibre::getISBN).collect(Collectors.toList());
			itemBatchEdit.addActionListener(ev -> batchEdit(batchIsbns));
			menu.add(itemBatchEdit);
		}

		menu.addSeparator();

		JMenuItem itemCopiarISBN = new JMenuItem("Copiar ISBN");
		itemCopiarISBN.setEnabled(selected.size() == 1);
		itemCopiarISBN.addActionListener(ev ->
			java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new java.awt.datatransfer.StringSelection(
					String.valueOf(selected.get(0).getISBN())), null));
		menu.add(itemCopiarISBN);

		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void eliminarLlibresGaleria(List<Llibre> llibres) {
		if (llibres.isEmpty()) return;
		String msg = llibres.size() == 1
			? "Eliminar \"" + llibres.get(0).getNom() + "\"?\nAquesta acció no es pot desfer."
			: "Eliminar " + llibres.size() + " llibres seleccionats?\nAquesta acció no es pot desfer.";
		if (JOptionPane.showConfirmDialog(vista, msg, "Confirmar eliminació",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
		List<Long> isbns = llibres.stream().map(Llibre::getISBN).collect(Collectors.toList());
		for (long isbn : isbns) {
			try {
				Llibre l = MainFrameControl.getInstance().getLlibreIsbn(isbn);
				if (l == null) continue;
				undoBuffer.push(l);
				if (undoBuffer.size() > UNDO_MAX) undoBuffer.removeLast();
				cd.deleteLlibre(l);
				eliminarFila(l);
			} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
		}
		// Refresh gallery after batch deletion
		ArrayList<Llibre> toShow = paginatedMode
			? new ArrayList<>(biblio.subList(currentPage * PAGE_SIZE, Math.min((currentPage + 1) * PAGE_SIZE, biblio.size())))
			: biblio;
		vista.getGaleria().updateLlibres(toShow);
	}

	private void batchEdit(java.util.List<Long> isbns) {
		String[] formatOpts = {"(sense canviar)", "Tapa dura", "Butxaca", "Ebook", "Audiollibre"};
		String[] llegitOpts = {"(sense canviar)", "Llegit", "No llegit"};
		javax.swing.JComboBox<String> comboFormat = new javax.swing.JComboBox<>(formatOpts);
		javax.swing.JComboBox<String> comboLlegit = new javax.swing.JComboBox<>(llegitOpts);
		java.util.List<Llista> llistes = cd.getAllLlistes();
		String[] llistaOpts = new String[llistes.size() + 1];
		llistaOpts[0] = "(no afegir a cap llista)";
		for (int i = 0; i < llistes.size(); i++) llistaOpts[i + 1] = llistes.get(i).getNom();
		javax.swing.JComboBox<String> comboLlista = new javax.swing.JComboBox<>(llistaOpts);
		Object[] fields = {
			"Format:", comboFormat,
			"Llegit:", comboLlegit,
			"Afegir a llista:", comboLlista
		};
		int result = JOptionPane.showConfirmDialog(vista, fields,
			"Edició massiva de " + isbns.size() + " llibres", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) return;
		String selFormat = (String) comboFormat.getSelectedItem();
		String selLlegit = (String) comboLlegit.getSelectedItem();
		int selLlistaIdx = comboLlista.getSelectedIndex();
		Llista selLlista = selLlistaIdx > 0 ? llistes.get(selLlistaIdx - 1) : null;
		for (long isbn : isbns) {
			try {
				Llibre l = cd.getLlibre(isbn);
				if (l == null) continue;
				if (selFormat != null && !selFormat.startsWith("(")) l.setFormat(selFormat);
				if ("Llegit".equals(selLlegit)) l.setLlegit(true);
				else if ("No llegit".equals(selLlegit)) l.setLlegit(false);
				cd.updateLlibre(l);
				if (selLlista != null) {
					try { cd.addLlibreToLlista(isbn, selLlista.getId(), 0.0, false); }
					catch (Exception ignored) {}
				}
			} catch (Exception e) { new DialogoError(e).showErrorMessage(); }
		}
		refresh();
	}

	private void afegirLlibresGaleriaALlista(List<Llibre> llibres) {
		Llista sel = pickLlista("Selecciona la llista on afegir " + llibres.size() + " llibre(s):");
		if (sel == null) return;
		int ok = 0, skip = 0;
		for (Llibre l : llibres) {
			try {
				cd.addLlibreToLlista(l.getISBN(), sel.getId(), 0.0, false);
				ok++;
			} catch (Exception ignored) { skip++; }
		}
		String msg = I18n.t("dlg_books_added_to_list", ok, sel.getNom());
		if (skip > 0) msg += "\n" + I18n.t("dlg_books_existing_list", skip);
		JOptionPane.showMessageDialog(vista, msg, I18n.t("dlg_added_to_list_title"), JOptionPane.INFORMATION_MESSAGE);
		refreshComboLlistes();
	}

	private void toggleVista() {
		if (!vista.isGaleriaMode()) {
			vista.getGaleria().updateLlibres(biblio);
			vista.showGaleria();
			vista.getBtnToggleVista().setText(I18n.t("btn_table_view"));
			herramienta.Config.setViewMode("galeria");
		} else {
			vista.showTaula();
			vista.getBtnToggleVista().setText("Galeria");
			herramienta.Config.setViewMode("taula");
		}
	}

	public void undoDelete() {
		if (undoBuffer.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "No hi ha cap eliminació per desfer.", "Desfer", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		Llibre l = undoBuffer.pop();
		try {
			cd.addLlibre(l);
			refreshLlibre(l, true);
			JOptionPane.showMessageDialog(vista,
				"\"" + l.getNom() + "\" restaurat.", "Desfer eliminació", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private void toggleGroupBySeries() {
		groupBySeries = !groupBySeries;
		vista.getBtnGroupSeries().setFont(groupBySeries
			? vista.getBtnGroupSeries().getFont().deriveFont(java.awt.Font.BOLD)
			: herramienta.UITheme.FONT_BOLD);
		if (groupBySeries && biblio != null) {
			biblio.sort(java.util.Comparator
				.comparing((Llibre l) -> l.getSerie() == null || l.getSerie().isBlank() ? "￿" : l.getSerie())
				.thenComparingInt(l -> l.getVolum()));
		}
		currentPage = 0;
		showPage(0);
	}

	private void obrirGestioLlistes() {
		new GestioLlistesDialog(SwingUtilities.getWindowAncestor(vista), this, cd).setVisible(true);
	}

	private void obrirConfiguracio() {
		java.awt.Window w = SwingUtilities.getWindowAncestor(vista);
		new ConfiguracioDialog(
			w instanceof java.awt.Frame ? (java.awt.Frame) w : null,
			() -> vista.applyTheme(),
			() -> { biblio = cd.getAllLlibres(); useDBPagination = cd.isLargeLibrary(); currentLlistaId = null; quitarFiltros(); refreshComboLlistes(); },
			cd
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
			cd.backupToSQL(f);
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
			cd.restoreFromSQL(fc.getSelectedFile());
			currentLlistaId = null;
			refresh();
			refreshComboLlistes();
			refreshComboTags();
			JOptionPane.showMessageDialog(vista, "Base de dades restaurada correctament.",
				"Restauració completada", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}
}
