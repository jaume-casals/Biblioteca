package presentacio.detalles.control;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import herramienta.LlibreValidator;
import interficie.EnActualizarBBDD;
import presentacio.detalles.vista.DetallesLlibrePanel;

public class DetallesLlibrePanelControl {

	private DetallesLlibrePanel vista;
	private ControladorDomini cLlibres;
	private EnActualizarBBDD enActualizarBBDD;

	private static final int IMG_W = 200;
	private static final int IMG_H = 200;

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD) {
		this.vista = new DetallesLlibrePanel();

		this.enActualizarBBDD = enActualizarBBDD;
		cLlibres = ControladorDomini.getInstance();

		carregarImatge(l.getImatge());

		this.vista.getBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.getBtnEditar().addActionListener(e -> editar(l));
		this.vista.getBtnEliminar().addActionListener(e -> eliminar(l));

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
		if (path.isEmpty()) {
			this.vista.getLabelIcono().setIcon(null);
		} else {
			carregarImatge(path);
		}
	}

	private void carregarImatge(String path) {
		if (path == null || path.isBlank()) return;
		try {
			BufferedImage img = ImageIO.read(new FileInputStream(path));
			if (img == null) return;
			ImageIcon icon = new ImageIcon(img.getScaledInstance(IMG_W, IMG_H, Image.SCALE_SMOOTH));
			this.vista.getLabelIcono().setIcon(icon);
		} catch (Exception e) {
			// invalid path or unreadable file — leave label empty
		}
	}

	private void seleccionarImatge() {
		String imgDir = herramienta.Config.getDefaultImgDir();
		JFileChooser chooser = new JFileChooser(new File(imgDir).exists() ? imgDir : System.getProperty("user.home"));
		chooser.setFileFilter(new FileNameExtensionFilter("Imatges", "jpg", "jpeg", "png", "gif", "bmp", "webp"));
		if (chooser.showOpenDialog(this.vista) == JFileChooser.APPROVE_OPTION) {
			String path = chooser.getSelectedFile().getAbsolutePath();
			this.vista.getTextPortada().setText(path);
			carregarImatge(path);
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
			this.vista.getBtnEditar().setText("Editar");
			try {
				// Validate before deleting so a bad edit can't destroy the record
				Llibre a = LlibreValidator.checkLlibre(Long.parseLong(vista.getTextISBN().getText()),
						vista.getTextNom().getText(), vista.getTextAutor().getText(),
						Integer.parseInt(vista.getTextAny().getText()), vista.getTextDescripcio().getText(),
						Double.parseDouble(vista.getTextValoracio().getText()),
						Double.parseDouble(vista.getTextPreu().getText()), vista.getChckLlegit().isSelected(),
						vista.getTextPortada().getText());
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
