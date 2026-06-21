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
import persistencia.contract.EscritorBiblioteca;
import herramienta.ui.DialegError;
import herramienta.text.FieldAutoComplete;
import herramienta.i18n.I18n;
import herramienta.text.ValidadorLlibre;
import herramienta.text.ParseHelpers;
import herramienta.ui.UITheme;
import presentacio.ValidadorFormulari;
import presentacio.listener.EnActualitzarBBDD;
import presentacio.detalles.vista.DialegDesarLlibres;

public class ControladorDialegDesarLlibres {

	private final DialegDesarLlibres vista;
	private final EscritorBiblioteca cLlibres;
	private byte[] selectedBlob;
	private final EnActualitzarBBDD callback;
	private volatile TascaCercaOpenLibrary cercarTask;

	public ControladorDialegDesarLlibres(DialegDesarLlibres vista) {
		this(vista, null, null);
	}

	public ControladorDialegDesarLlibres(DialegDesarLlibres vista, EnActualitzarBBDD callback) {
		this(vista, callback, null);
	}

	public ControladorDialegDesarLlibres(DialegDesarLlibres vista, EnActualitzarBBDD callback, EscritorBiblioteca cd) {
		this.callback = callback;
		this.vista = vista;
		this.vista.setFocusable(true);
		this.vista.getRootPane().registerKeyboardAction(
			e -> vista.dispose(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);
		this.vista.obtenirBtnGuardar().addActionListener(e -> crearLlibre());
		this.vista.obtenirBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.obtenirBtnCercaInternet().addActionListener(e -> cercaInternet());
		this.vista.addWindowListener(new WindowAdapter() {
			@Override public void windowClosed(WindowEvent e) {
				TascaCercaOpenLibrary t = cercarTask; if (t != null) { t.cancel(true); cercarTask = null; }
			}
		});
		if (cd == null) throw new IllegalArgumentException("ControladorDialegDesarLlibres requires non-null cd");
		cLlibres = cd;

		double defVal = herramienta.config.Configuracio.obtenirDefaultValoracio();
		if (defVal > 0.0) this.vista.obtenirTextValoracio().setText(String.valueOf(defVal));

		this.vista.obtenirTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.obtenirTextPortada().getText().trim()); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.obtenirTextPortada().getText().trim()); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.obtenirTextPortada().getText().trim()); }
		});

		// Carrega les llistes d'autocompletat diferenciades fora de l'EDT —
		// són consultes a la BBDD que poden bloquejar la UI en biblioteques grans.
		new SwingWorker<java.util.List<java.util.List<String>>, Void>() {
			@Override protected java.util.List<java.util.List<String>> doInBackground() {
				java.util.List<java.util.List<String>> lists = new java.util.ArrayList<>();
				lists.add(cLlibres.obtenirDistinctAutorNames());
				lists.add(cLlibres.obtenirDistinctValues("editorial"));
				lists.add(cLlibres.obtenirDistinctValues("serie"));
				lists.add(cLlibres.obtenirDistinctValues("idioma"));
				return lists;
			}
			@Override protected void done() {
				try {
					java.util.List<java.util.List<String>> lists = get();
					FieldAutoComplete.attach(vista.obtenirTextAutor(),     lists.get(0));
					FieldAutoComplete.attach(vista.obtenirTextEditorial(), lists.get(1));
					FieldAutoComplete.attach(vista.obtenirTextSerie(),     lists.get(2));
					FieldAutoComplete.attach(vista.obtenirTextIdioma(),    lists.get(3));
				} catch (Exception ignored) {}
			}
		}.execute();

		javax.swing.event.DocumentListener live = new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { refrescarLiveValidation(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { refrescarLiveValidation(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { refrescarLiveValidation(); }
		};
		vista.obtenirTextISBN().getDocument().addDocumentListener(live);
		vista.obtenirTextNom().getDocument().addDocumentListener(live);
	}

	private void refrescarLiveValidation() {
		String isbn = vista.obtenirTextISBN().getText().trim();
		boolean isbnOk = false;
		if (!isbn.isEmpty()) {
			try {
				ValidadorLlibre.comprovarLlibreFromString(isbn, "x", null, null, null, null, null, null, null);
				isbnOk = true;
			} catch (IllegalArgumentException ignored) {}
		}
		ValidadorFormulari.validarField(vista.obtenirTextISBN(), isbnOk);
		String nom = vista.obtenirTextNom().getText().trim();
		ValidadorFormulari.validarField(vista.obtenirTextNom(), !nom.isBlank() && nom.length() <= 255);
	}

	private void cercaInternet() {
		String isbn  = vista.obtenirTextISBN().getText().trim();
		String titol = vista.obtenirTextNom().getText().trim();
		String autor = vista.obtenirTextAutor().getText().trim();

		if (isbn.isEmpty() && titol.isEmpty() && autor.isEmpty()) {
			JOptionPane.showMessageDialog(vista,
				I18n.t("dlg_search_hint_msg"),
				I18n.t("dlg_search_internet_title"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JButton btn = vista.obtenirBtnCercaInternet();
		btn.setEnabled(false);
		btn.setText(I18n.t("btn_searching"));
		vista.obtenirProgressBar().setVisible(true);
		if (!isbn.isEmpty()) vista.obtenirTextISBN().setEditable(false);

		cercarTask = new TascaCercaOpenLibrary(isbn, titol, autor, vista, selectedBlob, this::posarSelectedBlob);
		cercarTask.execute();
	}

	void posarSelectedBlob(byte[] blob) { this.selectedBlob = blob; }

	private void carregarImatge(String path) {
		selectedBlob = null;
		if (path == null || path.isBlank()) { this.vista.obtenirLabelPreview().setIcon(null); return; }
		final String finalPath = path;
		final DialegDesarLlibres target = this.vista;
		new SwingWorker<byte[], Void>() {
			@Override protected byte[] doInBackground() throws Exception {
				return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(finalPath));
			}
			@Override protected void done() {
				if (isCancelled()) return;
				try {
					selectedBlob = get();
					target.obtenirLabelPreview().setIcon(UITheme.scaledIcon(selectedBlob, 120));
				} catch (Exception ignored) {
					selectedBlob = null;
					target.obtenirLabelPreview().setIcon(null);
				}
			}
		}.execute();
	}

	private void seleccionarImatge() {
		File f = UITheme.chooseImageFile(this.vista);
		if (f != null) this.vista.obtenirTextPortada().setText(f.getAbsolutePath());
	}

	private void crearLlibre() {
		try {
			String isbnTxt = vista.obtenirTextISBN().getText().trim();
			String anyTxt  = vista.obtenirTextAny().getText().trim();
			String valTxt  = vista.obtenirTextValoracio().getText().trim();
			String preuTxt = vista.obtenirTextPreu().getText().trim();

			java.util.List<String> errors = new java.util.ArrayList<>();
			Long isbn       = isbnTxt.isEmpty() ? null
				: ParseHelpers.parseLong(isbnTxt, 0L, "ISBN", errors);
			Integer any     = ParseHelpers.parseInt(anyTxt, 0, "field_year", errors);
			Double valoracio = ParseHelpers.parseDouble(valTxt, 0.0, "field_rating", errors);
			Double preu      = ParseHelpers.parseDouble(preuTxt, 0.0, "field_price", errors);

			if (!errors.isEmpty()) {
				new DialegError(new IllegalArgumentException(String.join("\n", errors))).mostrarErrorMessage();
				return;
			}

			Llibre l = herramienta.text.ValidadorLlibre.comprovarLlibre(
				isbn, vista.obtenirTextNom().getText().trim(),
				vista.obtenirTextAutor().getText().trim(), any,
				vista.obtenirTextDescripcio().getText().trim(),
				valoracio, preu, vista.obtenirChckLlegit().isSelected(),
				vista.obtenirTextPortada().getText().trim());
			java.util.List<String> autors = java.util.Arrays.stream(vista.obtenirTextAutor().getText().split(","))
				.map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
			l.posarAutors(autors);
			l.posarEditorial(vista.obtenirTextEditorial().getText().trim());
			l.posarSerie(vista.obtenirTextSerie().getText().trim());
			try { l.posarVolum(Integer.parseInt(vista.obtenirTextVolum().getText().trim())); } catch (NumberFormatException ignored) {}
			l.posarDataCompra(vista.obtenirTextDataCompra().getText().trim());
			l.posarDataLectura(vista.obtenirTextDataLectura().getText().trim());
			l.posarIdioma(vista.obtenirTextIdioma().getText().trim());
			String fmt = (String) vista.obtenirComboFormat().getSelectedItem();
			l.posarFormat(fmt != null && !fmt.isEmpty() ? fmt : null);
			l.posarDesitjat(vista.obtenirChckDesitjat().isSelected());
			l.posarNotes(vista.obtenirTextNotes().getText().trim());
			l.posarPaisOrigen(vista.obtenirTextPaisOrigen().getText().trim());
			String estat = (String) vista.obtenirComboEstat().getSelectedItem();
			l.posarEstat(estat != null && !estat.isEmpty() ? estat : null);
			String exemplarsTxt = vista.obtenirTextExemplars().getText().trim();
			if (!exemplarsTxt.isEmpty()) {
				try { l.posarExemplars(Integer.parseInt(exemplarsTxt)); }
				catch (NumberFormatException ignored) {}
			}
			String nomCa = vista.obtenirTextNomCa().getText().trim();
			String nomEs = vista.obtenirTextNomEs().getText().trim();
			String nomEn = vista.obtenirTextNomEn().getText().trim();
			l.posarNomCa(nomCa.isEmpty() ? null : nomCa);
			l.posarNomEs(nomEs.isEmpty() ? null : nomEs);
			l.posarNomEn(nomEn.isEmpty() ? null : nomEn);
			l.posarImatgeBlob(selectedBlob);
			herramienta.text.ValidadorLlibre.validarExtrasAll(l.obtenirEditorial(), l.obtenirSerie(), l.obtenirIdioma(), l.obtenirFormat(), l.obtenirPaisOrigen(), l.obtenirEstat());

			vista.obtenirBtnGuardar().setEnabled(false);
			final EnActualitzarBBDD cb = callback;
			new SwingWorker<Void, Void>() {
				@Override protected Void doInBackground() throws Exception {
					cLlibres.afegirLlibre(l);
					return null;
				}
				@Override protected void done() {
					vista.obtenirBtnGuardar().setEnabled(true);
					try {
						get();
						vista.dispose();
						if (cb != null) cb.enActualitzarLlibre(l, true);
					} catch (Exception e) {
						new DialegError(e).mostrarErrorMessage();
					}
				}
			}.execute();
		} catch (Exception e) {
			new DialegError(e).mostrarErrorMessage();
		}
	}

	public Dialog obtenirVista() { return vista; }
}
