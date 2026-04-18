package presentacio;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
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
	private static final int COLUMNA_DETALLS = 7;

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
		this.biblio = biblio;
		this.enActualizarBBDD = enActualizarBBDD;

		this.vista.getBtnExportCSV().addActionListener(e -> exportarCSV());
		this.vista.getBtnImportarCSV().addActionListener(e -> importarCSV());
		this.vista.getBtnEscanejarISBN().addActionListener(e -> escanejarISBN());
		this.vista.getbtnFiltrar().addActionListener(e -> filtrar());
		this.vista.getbttnQuitarFiltros().addActionListener(e -> quitarFiltros());
		this.vista.getBtnEstadistiques().addActionListener(e -> mostrarEstadistiques());
		this.vista.getBtnBackupBD().addActionListener(e -> backupBD());
		this.vista.getBtnRestaurarBD().addActionListener(e -> restaurarBD());
		this.vista.getBtnConfiguracio().addActionListener(e -> obrirConfiguracio());
		this.botonDetalles.addActionListener(e -> abrirDetallesLlibres());
		this.vista.getjTableBilio().addMouseListener(abrirDetalles());
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
			public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarSearchBar(); }
		});

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
		return new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				JTable table = (JTable) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
					abrirDetallesLlibres();
				}
			}
		};
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
		String estat = Boolean.TRUE.equals(l.getLlegit()) ? "Llegit" : "No llegit";
		return new Object[] { l.getISBN() + "", l.getNom(), l.getAutor(), l.getAny(), l.getValoracio(), l.getPreu(),
				estat, "" };
	}

	private void setTable(ArrayList<Llibre> llibres) {
		this.model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		this.model.getDataVector().removeAllElements();
		removeAlldataFiltros();
		setHeader(new String[] { "ISBN", "Nom", "Autor", "Any", "Valoracio", "Preu", "Llegit", "Detalls" });
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
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setPreferredWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMinWidth(85);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setMaxWidth(110);
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellRenderer(new BotonDetallesRenderer());
		t.getColumnModel().getColumn(COLUMNA_DETALLS).setCellEditor(new BotonDetallesEditor(new JCheckBox()));
		t.getColumnModel().getColumn(COLUMNA_lLEGIT).setCellRenderer(new LlegitCheckBoxRenderer());
		t.getColumnModel().getColumn(COLUMNA_lLEGIT).setCellEditor(new LlegitCheckBoxEditor());

		// update model ref after setHeader() replaced it
		this.model = (DefaultTableModel) t.getModel();

		aplicarSearchBar();
	}

	private void aplicarSearchBar() {
		String query = this.vista.getSearchBar().getText().trim();
		javax.swing.RowSorter<?> sorter = this.vista.getjTableBilio().getRowSorter();
		if (sorter instanceof javax.swing.DefaultRowSorter) {
			if (query.isEmpty()) {
				((javax.swing.DefaultRowSorter<?, ?>) sorter).setRowFilter(null);
			} else {
				try {
					((javax.swing.DefaultRowSorter<?, ?>) sorter).setRowFilter(
						javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
				} catch (java.util.regex.PatternSyntaxException ignored) {}
			}
		}
		updateTitleBar();
	}

	private void updateTitleBar() {
		JTable t = this.vista.getjTableBilio();
		int shown = t.getRowCount();
		int total = this.biblio != null ? this.biblio.size() : 0;
		java.awt.Window w = SwingUtilities.getWindowAncestor(this.vista);
		if (w instanceof JFrame) {
			String title = shown == total
				? "Biblioteca  (" + total + " llibres)"
				: "Biblioteca  (" + shown + " / " + total + " llibres)";
			((JFrame) w).setTitle(title);
		}
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
		int ok = 0, err = 0;
		StringBuilder errors = new StringBuilder();
		try (java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.FileReader(fc.getSelectedFile(), java.nio.charset.StandardCharsets.UTF_8))) {
			br.readLine();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isBlank()) continue;
				try {
					String[] c = parseCSVLine(line);
					Llibre l = LlibreValidator.checkLlibre(
						Long.parseLong(c[0].trim()), c[1], c[2],
						Integer.parseInt(c[3].trim()),
						c.length > 4 ? c[4] : "",
						c.length > 5 ? Double.parseDouble(c[5].trim()) : 0.0,
						c.length > 6 ? Double.parseDouble(c[6].trim()) : 0.0,
						c.length > 7 ? Boolean.parseBoolean(c[7].trim()) : false,
						c.length > 8 ? c[8] : "");
					ControladorDomini.getInstance().addLlibre(l);
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
		if (biblio == null || biblio.isEmpty()) {
			JOptionPane.showMessageDialog(vista, "La biblioteca és buida.", "Estadístiques",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		int total = biblio.size();
		long llegits = biblio.stream().filter(l -> Boolean.TRUE.equals(l.getLlegit())).count();
		double pctLlegit = 100.0 * llegits / total;
		double avgVal = biblio.stream().mapToDouble(l -> l.getValoracio() != null ? l.getValoracio() : 0).average().orElse(0);
		double avgPreu = biblio.stream().mapToDouble(l -> l.getPreu() != null ? l.getPreu() : 0).average().orElse(0);

		String topAnys = biblio.stream()
			.filter(l -> l.getAny() != null && l.getAny() > 0)
			.collect(java.util.stream.Collectors.groupingBy(Llibre::getAny, java.util.stream.Collectors.counting()))
			.entrySet().stream()
			.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
			.limit(5)
			.map(e -> "  " + e.getKey() + ": " + e.getValue() + " llibre" + (e.getValue() > 1 ? "s" : ""))
			.collect(java.util.stream.Collectors.joining("\n"));

		String msg = String.format(
			"Total de llibres: %d%n" +
			"Llegits: %d (%.1f%%)%n" +
			"No llegits: %d (%.1f%%)%n%n" +
			"Valoració mitjana: %.2f / 10%n" +
			"Preu mitjà: %.2f €%n%n" +
			"Anys amb més publicacions:%n%s",
			total, llegits, pctLlegit, (total - llegits), (100.0 - pctLlegit),
			avgVal, avgPreu, topAnys.isEmpty() ? "  (cap any registrat)" : topAnys);

		JOptionPane.showMessageDialog(vista, msg, "Estadístiques de la Biblioteca",
			JOptionPane.INFORMATION_MESSAGE);
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
		setTable(biblio);
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
		int row = this.vista.getjTableBilio().getSelectedRow();
		if (row < 0) return;
		try {
			Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(
				Long.parseLong((String) this.vista.getjTableBilio().getValueAt(row, COLUMNA_ISBN)));
			if (l == null) return;
			int confirm = JOptionPane.showConfirmDialog(this.vista,
				"Eliminar \"" + l.getNom() + "\"?\nAquesta acció no es pot desfer.",
				"Confirmar eliminació", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm != JOptionPane.YES_OPTION) return;
			ControladorDomini.getInstance().deleteLlibre(l);
			eliminarFila(l);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	public void eliminarFila(Llibre l) {
		model = (DefaultTableModel) this.vista.getjTableBilio().getModel();
		for (int x = 0; x < model.getRowCount(); x++) {
			if (model.getValueAt(x, COLUMNA_ISBN).toString().contentEquals(Long.toString(l.getISBN()))) {
				model.removeRow(x);
				biblio.remove(l);
				break;
			}
		}
		updateTitleBar();
	}

	public void refresh() {
		quitarFiltros();
	}

	private void obrirConfiguracio() {
		java.awt.Window w = SwingUtilities.getWindowAncestor(vista);
		new ConfiguracioDialog(w instanceof java.awt.Frame ? (java.awt.Frame) w : null).setVisible(true);
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
