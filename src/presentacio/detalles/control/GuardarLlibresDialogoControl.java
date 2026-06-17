package presentacio.detalles.control;

import java.awt.Dialog;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import domini.Llibre;
import interficie.BibliotecaWriter;
import herramienta.DialogoError;
import herramienta.FieldAutoComplete;
import herramienta.I18n;
import herramienta.LlibreValidator;
import herramienta.ParseHelpers;
import herramienta.UITheme;
import presentacio.FormValidator;
import presentacio.listener.EnActualizarBBDD;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class GuardarLlibresDialogoControl {

	private final GuardarLlibresDialogo vista;
	private final BibliotecaWriter cLlibres;
	private byte[] selectedBlob;
	private final EnActualizarBBDD callback;
	private volatile OpenLibrarySearchTask searchTask;

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista) {
		this(vista, null, null);
	}

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista, EnActualizarBBDD callback) {
		this(vista, callback, null);
	}

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista, EnActualizarBBDD callback, BibliotecaWriter cd) {
		this.callback = callback;
		this.vista = vista;
		this.vista.setFocusable(true);
		this.vista.getRootPane().registerKeyboardAction(
			e -> vista.dispose(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);
		this.vista.getBtnGuardar().addActionListener(e -> crearLlibre());
		this.vista.getBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.getBtnCercaInternet().addActionListener(e -> cercaInternet());
		this.vista.addWindowListener(new WindowAdapter() {
			@Override public void windowClosed(WindowEvent e) {
				OpenLibrarySearchTask t = searchTask; if (t != null) { t.cancel(true); searchTask = null; }
			}
		});
		if (cd == null) throw new IllegalArgumentException("GuardarLlibresDialogoControl requires non-null cd");
		cLlibres = cd;

		double defVal = herramienta.Config.getDefaultValoracio();
		if (defVal > 0.0) this.vista.getTextValoracio().setText(String.valueOf(defVal));

		this.vista.getTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.getTextPortada().getText().trim()); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.getTextPortada().getText().trim()); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.getTextPortada().getText().trim()); }
		});

		// Load distinct autocomplete lists off the EDT — they're DB queries that
		// can block the UI for large libraries.
		new SwingWorker<java.util.List<java.util.List<String>>, Void>() {
			@Override protected java.util.List<java.util.List<String>> doInBackground() {
				java.util.List<java.util.List<String>> lists = new java.util.ArrayList<>();
				lists.add(cLlibres.getDistinctAutorNames());
				lists.add(cLlibres.getDistinctValues("editorial"));
				lists.add(cLlibres.getDistinctValues("serie"));
				lists.add(cLlibres.getDistinctValues("idioma"));
				return lists;
			}
			@Override protected void done() {
				try {
					java.util.List<java.util.List<String>> lists = get();
					FieldAutoComplete.attach(vista.getTextAutor(),     lists.get(0));
					FieldAutoComplete.attach(vista.getTextEditorial(), lists.get(1));
					FieldAutoComplete.attach(vista.getTextSerie(),     lists.get(2));
					FieldAutoComplete.attach(vista.getTextIdioma(),    lists.get(3));
				} catch (Exception ignored) {}
			}
		}.execute();

		javax.swing.event.DocumentListener live = new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshLiveValidation(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshLiveValidation(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshLiveValidation(); }
		};
		vista.getTextISBN().getDocument().addDocumentListener(live);
		vista.getTextNom().getDocument().addDocumentListener(live);
	}

	private void refreshLiveValidation() {
		String isbn = vista.getTextISBN().getText().trim();
		boolean isbnOk = false;
		if (!isbn.isEmpty()) {
			try {
				LlibreValidator.checkLlibreFromString(isbn, "x", null, null, null, null, null, null, null);
				isbnOk = true;
			} catch (IllegalArgumentException ignored) {}
		}
		FormValidator.validateField(vista.getTextISBN(), isbnOk);
		String nom = vista.getTextNom().getText().trim();
		FormValidator.validateField(vista.getTextNom(), !nom.isBlank() && nom.length() <= 255);
	}

	private void cercaInternet() {
		String isbn  = vista.getTextISBN().getText().trim();
		String titol = vista.getTextNom().getText().trim();
		String autor = vista.getTextAutor().getText().trim();

		if (isbn.isEmpty() && titol.isEmpty() && autor.isEmpty()) {
			JOptionPane.showMessageDialog(vista,
				I18n.t("dlg_search_hint_msg"),
				I18n.t("dlg_search_internet_title"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JButton btn = vista.getBtnCercaInternet();
		btn.setEnabled(false);
		btn.setText(I18n.t("btn_searching"));
		vista.getProgressBar().setVisible(true);
		if (!isbn.isEmpty()) vista.getTextISBN().setEditable(false);

		searchTask = new OpenLibrarySearchTask(isbn, titol, autor, vista, selectedBlob, this::setSelectedBlob);
		searchTask.execute();
	}

	void setSelectedBlob(byte[] blob) { this.selectedBlob = blob; }

	private void carregarImatge(String path) {
		selectedBlob = null;
		if (path == null || path.isBlank()) { this.vista.getLabelPreview().setIcon(null); return; }
		try {
			selectedBlob = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
			this.vista.getLabelPreview().setIcon(UITheme.scaledIcon(selectedBlob, 120));
		} catch (Exception ignored) {
			this.vista.getLabelPreview().setIcon(null);
		}
	}

	private void seleccionarImatge() {
		File f = UITheme.chooseImageFile(this.vista);
		if (f != null) this.vista.getTextPortada().setText(f.getAbsolutePath());
	}

	private void crearLlibre() {
		try {
			String isbnTxt = vista.getTextISBN().getText().trim();
			String anyTxt  = vista.getTextAny().getText().trim();
			String valTxt  = vista.getTextValoracio().getText().trim();
			String preuTxt = vista.getTextPreu().getText().trim();

			java.util.List<String> errors = new java.util.ArrayList<>();
			Long isbn       = isbnTxt.isEmpty() ? null
				: ParseHelpers.parseLong(isbnTxt, 0L, "ISBN", errors);
			Integer any     = ParseHelpers.parseInt(anyTxt, 0, "field_year", errors);
			Double valoracio = ParseHelpers.parseDouble(valTxt, 0.0, "field_rating", errors);
			Double preu      = ParseHelpers.parseDouble(preuTxt, 0.0, "field_price", errors);

			if (!errors.isEmpty()) {
				new DialogoError(new IllegalArgumentException(String.join("\n", errors))).showErrorMessage();
				return;
			}

			Llibre l = herramienta.LlibreValidator.checkLlibre(
				isbn, vista.getTextNom().getText().trim(),
				vista.getTextAutor().getText().trim(), any,
				vista.getTextDescripcio().getText().trim(),
				valoracio, preu, vista.getChckLlegit().isSelected(),
				vista.getTextPortada().getText().trim());
			java.util.List<String> autors = java.util.Arrays.stream(vista.getTextAutor().getText().split(","))
				.map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
			l.setAutors(autors);
			l.setEditorial(vista.getTextEditorial().getText().trim());
			l.setSerie(vista.getTextSerie().getText().trim());
			try { l.setVolum(Integer.parseInt(vista.getTextVolum().getText().trim())); } catch (NumberFormatException ignored) {}
			l.setDataCompra(vista.getTextDataCompra().getText().trim());
			l.setDataLectura(vista.getTextDataLectura().getText().trim());
			l.setIdioma(vista.getTextIdioma().getText().trim());
			String fmt = (String) vista.getComboFormat().getSelectedItem();
			l.setFormat(fmt != null && !fmt.isEmpty() ? fmt : null);
			l.setDesitjat(vista.getChckDesitjat().isSelected());
			l.setNotes(vista.getTextNotes().getText().trim());
			l.setPaisOrigen(vista.getTextPaisOrigen().getText().trim());
			String estat = (String) vista.getComboEstat().getSelectedItem();
			l.setEstat(estat != null && !estat.isEmpty() ? estat : null);
			String exemplarsTxt = vista.getTextExemplars().getText().trim();
			if (!exemplarsTxt.isEmpty()) {
				try { l.setExemplars(Integer.parseInt(exemplarsTxt)); }
				catch (NumberFormatException ignored) {}
			}
			String nomCa = vista.getTextNomCa().getText().trim();
			String nomEs = vista.getTextNomEs().getText().trim();
			String nomEn = vista.getTextNomEn().getText().trim();
			l.setNomCa(nomCa.isEmpty() ? null : nomCa);
			l.setNomEs(nomEs.isEmpty() ? null : nomEs);
			l.setNomEn(nomEn.isEmpty() ? null : nomEn);
			l.setImatgeBlob(selectedBlob);
			herramienta.LlibreValidator.validateExtrasAll(l.getEditorial(), l.getSerie(), l.getIdioma(), l.getFormat(), l.getPaisOrigen(), l.getEstat());
			cLlibres.addLlibre(l);
			vista.dispose();
			if (callback != null) callback.onBookUpdated(l, true);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	public Dialog getVista() { return vista; }
}
