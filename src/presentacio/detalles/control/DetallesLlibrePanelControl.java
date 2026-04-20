package presentacio.detalles.control;

import java.io.File;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import herramienta.LlibreValidator;
import interficie.EnActualizarBBDD;
import presentacio.detalles.vista.DetallesLlibrePanel;
import presentacio.detalles.vista.LlistesDelLlibreDialog;

public class DetallesLlibrePanelControl {

	private DetallesLlibrePanel vista;
	private ControladorDomini cLlibres;
	private EnActualizarBBDD enActualizarBBDD;
	private byte[] pendingBlob;

	private static final int IMG_W = 200;
	private static final int IMG_H = 200;

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD) {
		this.vista = new DetallesLlibrePanel();

		this.enActualizarBBDD = enActualizarBBDD;
		cLlibres = ControladorDomini.getInstance();

		pendingBlob = l.getImatgeBlob();
		if (pendingBlob != null) carregarImatgeBlob(pendingBlob);
		else carregarImatge(l.getImatge());

		this.vista.getBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.getBtnEditar().addActionListener(e -> editar(l));
		this.vista.getBtnEliminar().addActionListener(e -> eliminar(l));
		this.vista.getBtnGestioLlistes().addActionListener(e ->
			new LlistesDelLlibreDialog(this.vista, l).setVisible(true));

		this.vista.getTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
		});

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

		this.vista.setTitle("Expedient del llibre " + l.getNom());
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
		try {
			pendingBlob = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
			carregarImatgeBlob(pendingBlob);
		} catch (Exception e) {
			pendingBlob = null;
			this.vista.getLabelIcono().setIcon(null);
		}
	}

	private void carregarImatge(String path) {
		if (path == null || path.isBlank()) return;
		try {
			pendingBlob = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
			carregarImatgeBlob(pendingBlob);
		} catch (Exception e) {
			// invalid path or unreadable file — leave label empty
		}
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
			this.vista.getChckLlegit().setEnabled(true);
			this.vista.getBtnSeleccionarImatge().setEnabled(true);
			this.vista.getTextNotes().setEnabled(true);
			this.vista.getBtnEditar().setText("Guardar");
		} else if (this.vista.getBtnEditar().getText().equals("Guardar")) {
			this.vista.getTextAny().setEnabled(false);
			this.vista.getTextAutor().setEnabled(false);
			this.vista.getTextISBN().setEnabled(false);
			this.vista.getTextDescripcio().setEnabled(false);
			this.vista.getTextNom().setEnabled(false);
			this.vista.getTextPortada().setEnabled(false);
			this.vista.getTextPreu().setEnabled(false);
			this.vista.getTextValoracio().setEnabled(false);
			this.vista.getChckLlegit().setEnabled(false);
			this.vista.getBtnSeleccionarImatge().setEnabled(false);
			this.vista.getTextNotes().setEnabled(false);
			this.vista.getBtnEditar().setText("Editar");
			try {
				// Validate before deleting so a bad edit can't destroy the record
				Llibre a = LlibreValidator.checkLlibre(Long.parseLong(vista.getTextISBN().getText()),
						vista.getTextNom().getText(), vista.getTextAutor().getText(),
						Integer.parseInt(vista.getTextAny().getText()), vista.getTextDescripcio().getText(),
						Double.parseDouble(vista.getTextValoracio().getText()),
						Double.parseDouble(vista.getTextPreu().getText()), vista.getChckLlegit().isSelected(),
						vista.getTextPortada().getText());
				a.setNotes(vista.getTextNotes().getText());
				a.setImatgeBlob(pendingBlob);
				cLlibres.deleteLlibre(llibre);
				cLlibres.addLlibre(a);
				enActualizarBBDD.actualitzarLlibre(a, false);
			} catch (Exception e) {
				new DialogoError(e).showErrorMessage();
			}
		}
	}

	public DetallesLlibrePanel getDetallesLlibrePanel() {
		return this.vista;
	}
}
