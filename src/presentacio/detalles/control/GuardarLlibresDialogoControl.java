package presentacio.detalles.control;

import java.awt.Dialog;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import herramienta.FieldAutoComplete;
import herramienta.OpenLibraryClient;
import herramienta.UITheme;
import interficie.EnActualizarBBDD;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class GuardarLlibresDialogoControl implements WindowListener {

	private GuardarLlibresDialogo vista;
	private ControladorDomini cLlibres;
	private byte[] selectedBlob;
	private EnActualizarBBDD callback;
	private volatile Thread searchThread;

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista) {
		this(vista, null);
	}

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista, EnActualizarBBDD callback) {
		this.callback = callback;
		this.vista = vista;
		this.vista.setFocusable(true);
		this.vista.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) vista.dispose();
			}
			@Override public void keyReleased(KeyEvent e) {}
		});
		this.vista.getBtnGuardar().addActionListener(e -> crearLlibre());
		this.vista.getBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.getBtnCercaInternet().addActionListener(e -> cercaInternet());
		this.vista.addWindowListener(this);
		cLlibres = ControladorDomini.getInstance();

		double defVal = herramienta.Config.getDefaultValoracio();
		if (defVal > 0.0) this.vista.getTextValoracio().setText(String.valueOf(defVal));

		this.vista.getTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.getTextPortada().getText().trim()); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.getTextPortada().getText().trim()); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { carregarImatge(vista.getTextPortada().getText().trim()); }
		});

		FieldAutoComplete.attach(vista.getTextAutor(),    cLlibres.getDistinctAutorNames());
		FieldAutoComplete.attach(vista.getTextEditorial(), cLlibres.getDistinctValues("editorial"));
		FieldAutoComplete.attach(vista.getTextSerie(),     cLlibres.getDistinctValues("serie"));
		FieldAutoComplete.attach(vista.getTextIdioma(),    cLlibres.getDistinctValues("idioma"));
	}

	private void cercaInternet() {
		String isbn  = vista.getTextISBN().getText().trim();
		String titol = vista.getTextNom().getText().trim();
		String autor = vista.getTextAutor().getText().trim();

		if (isbn.isEmpty() && titol.isEmpty() && autor.isEmpty()) {
			JOptionPane.showMessageDialog(vista,
				"Introdueix un ISBN, un títol o un autor per cercar.",
				"Cerca a Internet", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JButton btn = vista.getBtnCercaInternet();
		btn.setEnabled(false);
		btn.setText("Cercant...");
		vista.getProgressBar().setVisible(true);

		Thread t = new Thread(() -> {
			Map<String, String> meta;
			if (!isbn.isEmpty()) {
				meta = OpenLibraryClient.lookupByISBN(isbn);
			} else if (!titol.isEmpty()) {
				meta = OpenLibraryClient.lookupByTitle(titol);
			} else {
				meta = OpenLibraryClient.lookupByAutor(autor);
			}

			if (Thread.interrupted()) return;

			// Auto-fetch cover if we have an ISBN and no image already selected
			String isbnForCover = !isbn.isEmpty() ? isbn : meta.get("isbn");
			byte[] coverBlob = null;
			if (isbnForCover != null && !isbnForCover.isBlank() && selectedBlob == null
					&& !meta.containsKey("error")) {
				coverBlob = OpenLibraryClient.fetchCoverByISBN(isbnForCover);
			}
			final byte[] finalCover = coverBlob;

			SwingUtilities.invokeLater(() -> {
				if (!vista.isDisplayable()) return;
				btn.setEnabled(true);
				btn.setText("⬇  Cerca a Internet (ISBN / Títol / Autor)");
				btn.setBackground(UITheme.GREEN);
				vista.getProgressBar().setVisible(false);

				if (meta.containsKey("error")) {
					JOptionPane.showMessageDialog(vista,
						"Error de connexió amb OpenLibrary:\n" + meta.get("error"),
						"Error de xarxa", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (meta.isEmpty()) {
					JOptionPane.showMessageDialog(vista,
						"No s'han trobat resultats a OpenLibrary.",
						"Cerca a Internet", JOptionPane.INFORMATION_MESSAGE);
					return;
				}

				if (meta.containsKey("title"))
					vista.getTextNom().setText(meta.get("title"));
				if (meta.containsKey("autor"))
					vista.getTextAutor().setText(meta.get("autor"));
				if (meta.containsKey("any") && vista.getTextAny().getText().isEmpty())
					vista.getTextAny().setText(meta.get("any"));
				if (meta.containsKey("isbn") && vista.getTextISBN().getText().isEmpty())
					vista.getTextISBN().setText(meta.get("isbn"));
				if (meta.containsKey("descripcio") && vista.getTextDescripcio().getText().isEmpty())
					vista.getTextDescripcio().setText(meta.get("descripcio"));

				if (finalCover != null && selectedBlob == null) {
					selectedBlob = finalCover;
					vista.getLabelPreview().setIcon(UITheme.scaledIcon(selectedBlob, 120));
				}
			});
		});
		t.setDaemon(true);
		searchThread = t;
		t.start();
	}

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

			Long isbn       = isbnTxt.isEmpty()  ? null : Long.parseLong(isbnTxt);
			Integer any     = anyTxt.isEmpty()   ? null : Integer.parseInt(anyTxt);
			Double valoracio = valTxt.isEmpty()  ? null : Double.parseDouble(valTxt);
			Double preu      = preuTxt.isEmpty() ? null : Double.parseDouble(preuTxt);

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
			l.setImatgeBlob(selectedBlob);
			cLlibres.addLlibre(l);
			vista.dispose();
			if (callback != null) callback.actualitzarLlibre(l, true);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	public Dialog getVista() { return vista; }

	@Override public void windowOpened(WindowEvent e) {}
	@Override public void windowClosing(WindowEvent e) {}
	@Override public void windowClosed(WindowEvent e) {
		if (searchThread != null) { searchThread.interrupt(); searchThread = null; }
	}
	@Override public void windowIconified(WindowEvent e) {}
	@Override public void windowDeiconified(WindowEvent e) {}
	@Override public void windowActivated(WindowEvent e) {}
	@Override public void windowDeactivated(WindowEvent e) {}
}
