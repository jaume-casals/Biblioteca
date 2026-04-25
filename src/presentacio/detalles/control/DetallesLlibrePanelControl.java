package presentacio.detalles.control;

import java.io.File;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import herramienta.FieldAutoComplete;
import herramienta.LlibreValidator;
import interficie.EnActualizarBBDD;
import presentacio.detalles.vista.DetallesLlibrePanel;
import presentacio.detalles.vista.LlistesDelLlibreDialog;
import presentacio.detalles.vista.TagsDelLlibreDialog;

public class DetallesLlibrePanelControl {

	private DetallesLlibrePanel vista;
	private ControladorDomini cLlibres;
	private EnActualizarBBDD enActualizarBBDD;
	private byte[] pendingBlob;
	private javax.swing.SwingWorker<byte[], Void> imatgeWorker;

	private static final int IMG_W = 200;
	private static final int IMG_H = 200;

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD) {
		this.vista = new DetallesLlibrePanel();

		this.enActualizarBBDD = enActualizarBBDD;
		cLlibres = ControladorDomini.getInstance();

		pendingBlob = l.getImatgeBlob();
		if (pendingBlob != null) {
			carregarImatgeBlob(pendingBlob);
		} else if (l.hasBlob()) {
			final long isbn = l.getISBN();
			startImatgeWorker(() -> cLlibres.getLlibreBlob(isbn));
		} else {
			carregarImatgeAsync(l.getImatge());
		}

		this.vista.getBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.getBtnEditar().addActionListener(e -> editar(l));
		this.vista.getBtnEliminar().addActionListener(e -> eliminar(l));
		this.vista.getBtnGestioLlistes().addActionListener(e ->
			new LlistesDelLlibreDialog(this.vista, l).setVisible(true));
		this.vista.getBtnGestioTags().addActionListener(e ->
			new TagsDelLlibreDialog(this.vista, l).setVisible(true));

		this.vista.getTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
		});

		FieldAutoComplete.attach(this.vista.getTextAutor(),    cLlibres.getDistinctAutorNames());
		FieldAutoComplete.attach(this.vista.getTextEditorial(), cLlibres.getDistinctValues("editorial"));
		FieldAutoComplete.attach(this.vista.getTextSerie(),     cLlibres.getDistinctValues("serie"));
		FieldAutoComplete.attach(this.vista.getTextIdioma(),    cLlibres.getDistinctValues("idioma"));

		this.vista.getTextAny().setText(l.getAny().toString());
		this.vista.getTextAutor().setText(l.getAutor().toString());
		this.vista.getTextISBN().setText(l.getISBN().toString());
		this.vista.getTextDescripcio().setText(l.getDescripcio().toString());
		this.vista.getTextNom().setText(l.getNom().toString());
		this.vista.getTextPortada().setText(l.getImatge() != null ? l.getImatge() : "");
		this.vista.getTextPreu().setText(l.getPreu().toString());
		this.vista.getTextValoracio().setText(l.getValoracio().toString());
		this.vista.getChckLlegit().setSelected(l.getLlegit());
		this.vista.getTextNotes().setText(l.getNotes());
		this.vista.getTextEditorial().setText(l.getEditorial() != null ? l.getEditorial() : "");
		this.vista.getTextSerie().setText(l.getSerie() != null ? l.getSerie() : "");
		this.vista.getTextVolum().setText(l.getVolum() > 0 ? String.valueOf(l.getVolum()) : "");
		this.vista.getTextDataCompra().setText(l.getDataCompra() != null ? l.getDataCompra() : "");
		this.vista.getTextDataLectura().setText(l.getDataLectura() != null ? l.getDataLectura() : "");
		this.vista.getTextIdioma().setText(l.getIdioma() != null ? l.getIdioma() : "");
		this.vista.getTextPaisOrigen().setText(l.getPaisOrigen() != null ? l.getPaisOrigen() : "");
		this.vista.getComboFormat().setSelectedItem(l.getFormat() != null ? l.getFormat() : "");
		this.vista.getChckDesitjat().setSelected(l.getDesitjat());
		this.vista.getTextPagines().setText(l.getPagines() > 0 ? String.valueOf(l.getPagines()) : "");
		this.vista.getTextPaginesLlegides().setText(l.getPaginesLlegides() > 0 ? String.valueOf(l.getPaginesLlegides()) : "");

		this.vista.setTitle("Expedient del llibre " + l.getNom());

		this.vista.getTextPaginesLlegides().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { }
		});
	}

	private void clampPaginesLlegides() {
		try {
			int llegides = Integer.parseInt(this.vista.getTextPaginesLlegides().getText().trim());
			int total = Integer.parseInt(this.vista.getTextPagines().getText().trim());
			if (total > 0 && llegides > total) {
				String clamped = String.valueOf(total);
				javax.swing.SwingUtilities.invokeLater(() ->
					this.vista.getTextPaginesLlegides().setText(clamped));
			}
		} catch (NumberFormatException ignored) {}
	}

	private void eliminar(Llibre llibre) {
		int confirm = javax.swing.JOptionPane.showConfirmDialog(
			this.vista,
			"Eliminar \"" + llibre.getNom() + "\"?\nAquesta acció no es pot desfer.",
			"Confirmar eliminació",
			javax.swing.JOptionPane.YES_NO_OPTION,
			javax.swing.JOptionPane.WARNING_MESSAGE);
		if (confirm != javax.swing.JOptionPane.YES_OPTION) return;
		try {
			cLlibres.deleteLlibre(llibre);
			enActualizarBBDD.eliminarLlibre(llibre);
			this.vista.dispose();
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	private void previewPortada() {
		String path = this.vista.getTextPortada().getText().trim();
		if (path.isEmpty()) { this.vista.getLabelIcono().setIcon(null); pendingBlob = null; return; }
		carregarImatgeAsync(path);
	}

	private void carregarImatgeAsync(String path) {
		if (path == null || path.isBlank()) return;
		startImatgeWorker(() -> java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)));
	}

	private void carregarImatge(String path) {
		carregarImatgeAsync(path);
	}

	private void startImatgeWorker(java.util.concurrent.Callable<byte[]> loader) {
		if (imatgeWorker != null) imatgeWorker.cancel(true);
		imatgeWorker = new javax.swing.SwingWorker<>() {
			@Override protected byte[] doInBackground() throws Exception { return loader.call(); }
			@Override protected void done() {
				if (isCancelled()) return;
				try {
					byte[] data = get();
					if (data != null) { pendingBlob = data; carregarImatgeBlob(data); }
				} catch (Exception ignored) {
					vista.getLabelIcono().setIcon(null);
				}
			}
		};
		imatgeWorker.execute();
	}

	private void carregarImatgeBlob(byte[] data) {
		this.vista.getLabelIcono().setIcon(herramienta.UITheme.scaledIcon(data, IMG_W));
	}

	private void seleccionarImatge() {
		File f = herramienta.UITheme.chooseImageFile(this.vista);
		if (f != null) {
			this.vista.getTextPortada().setText(f.getAbsolutePath());
			carregarImatge(f.getAbsolutePath());
		}
	}

	private void editar(Llibre llibre) {
		if (this.vista.getBtnEditar().getText().equals("Editar")) {
			this.vista.getTextAny().setEnabled(true);
			this.vista.getTextAutor().setEnabled(true);
			this.vista.getTextISBN().setEnabled(true);
			this.vista.getTextDescripcio().setEnabled(true);
			this.vista.getTextNom().setEnabled(true);
			this.vista.getTextPortada().setEnabled(true);
			this.vista.getTextPreu().setEnabled(true);
			this.vista.getTextValoracio().setEnabled(true);
			this.vista.getTextEditorial().setEnabled(true);
			this.vista.getTextSerie().setEnabled(true);
			this.vista.getTextVolum().setEnabled(true);
			this.vista.getTextDataCompra().setEnabled(true);
			this.vista.getTextDataLectura().setEnabled(true);
			this.vista.getTextIdioma().setEnabled(true);
			this.vista.getTextPaisOrigen().setEnabled(true);
			this.vista.getComboFormat().setEnabled(true);
			this.vista.getChckDesitjat().setEnabled(true);
			this.vista.getChckLlegit().setEnabled(true);
			this.vista.getBtnSeleccionarImatge().setEnabled(true);
			this.vista.getTextNotes().setEnabled(true);
			this.vista.getTextPagines().setEnabled(true);
			this.vista.getTextPaginesLlegides().setEnabled(true);
			this.vista.getBtnEditar().setText("Guardar");
		} else if (this.vista.getBtnEditar().getText().equals("Guardar")) {
			try {
				Llibre a = LlibreValidator.checkLlibre(Long.parseLong(vista.getTextISBN().getText()),
						vista.getTextNom().getText(), vista.getTextAutor().getText(),
						Integer.parseInt(vista.getTextAny().getText()), vista.getTextDescripcio().getText(),
						Double.parseDouble(vista.getTextValoracio().getText()),
						Double.parseDouble(vista.getTextPreu().getText()), vista.getChckLlegit().isSelected(),
						vista.getTextPortada().getText());
				a.setEditorial(vista.getTextEditorial().getText().trim());
				a.setSerie(vista.getTextSerie().getText().trim());
				try { a.setVolum(Integer.parseInt(vista.getTextVolum().getText().trim())); } catch (NumberFormatException ignored) {}
				a.setDataCompra(vista.getTextDataCompra().getText().trim());
				a.setDataLectura(vista.getTextDataLectura().getText().trim());
				a.setIdioma(vista.getTextIdioma().getText().trim());
				a.setPaisOrigen(vista.getTextPaisOrigen().getText().trim());
				String fmt = (String) vista.getComboFormat().getSelectedItem();
				a.setFormat(fmt != null && !fmt.isEmpty() ? fmt : null);
				a.setDesitjat(vista.getChckDesitjat().isSelected());
				java.util.List<String> autors = java.util.Arrays.stream(vista.getTextAutor().getText().split(","))
					.map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
				a.setAutors(autors);
				a.setNotes(vista.getTextNotes().getText());
				try { a.setPagines(Integer.parseInt(vista.getTextPagines().getText().trim())); } catch (NumberFormatException ignored) {}
				try { a.setPaginesLlegides(Integer.parseInt(vista.getTextPaginesLlegides().getText().trim())); } catch (NumberFormatException ignored) {}
				if (a.getPagines() > 0 && a.getPaginesLlegides() > a.getPagines()) {
					a.setPaginesLlegides(a.getPagines());
					vista.getTextPaginesLlegides().setText(String.valueOf(a.getPagines()));
				}
				a.setImatgeBlob(pendingBlob);
				if (a.getISBN() != llibre.getISBN() && cLlibres.existsLlibre(a.getISBN()))
					throw new Exception("El llibre amb ISBN " + a.getISBN() + " ja existeix a la biblioteca");
				cLlibres.deleteLlibre(llibre);
				cLlibres.addLlibre(a);
				enActualizarBBDD.actualitzarLlibre(a, false);
				// Only lock fields after successful save
				this.vista.getTextAny().setEnabled(false);
				this.vista.getTextAutor().setEnabled(false);
				this.vista.getTextISBN().setEnabled(false);
				this.vista.getTextDescripcio().setEnabled(false);
				this.vista.getTextNom().setEnabled(false);
				this.vista.getTextPortada().setEnabled(false);
				this.vista.getTextPreu().setEnabled(false);
				this.vista.getTextValoracio().setEnabled(false);
				this.vista.getTextEditorial().setEnabled(false);
				this.vista.getTextSerie().setEnabled(false);
				this.vista.getTextVolum().setEnabled(false);
				this.vista.getTextDataCompra().setEnabled(false);
				this.vista.getTextDataLectura().setEnabled(false);
				this.vista.getTextIdioma().setEnabled(false);
				this.vista.getTextPaisOrigen().setEnabled(false);
				this.vista.getComboFormat().setEnabled(false);
				this.vista.getChckDesitjat().setEnabled(false);
				this.vista.getChckLlegit().setEnabled(false);
				this.vista.getBtnSeleccionarImatge().setEnabled(false);
				this.vista.getTextNotes().setEnabled(false);
				this.vista.getTextPagines().setEnabled(false);
				this.vista.getTextPaginesLlegides().setEnabled(false);
				this.vista.getBtnEditar().setText("Editar");
			} catch (Exception e) {
				new DialogoError(e).showErrorMessage();
			}
		}
	}

	public DetallesLlibrePanel getDetallesLlibrePanel() {
		return this.vista;
	}
}
