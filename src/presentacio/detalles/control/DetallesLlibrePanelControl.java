package presentacio.detalles.control;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import herramienta.checkLlibre;
import interficie.EnActualizarBBDD;
import presentacio.detalles.vista.DetallesLlibrePanel;

public class DetallesLlibrePanelControl {

	private DetallesLlibrePanel vista;
	private ControladorDomini cLlibres;
	private EnActualizarBBDD enActualizarBBDD;

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD) {
		this.vista = new DetallesLlibrePanel();

		this.enActualizarBBDD = enActualizarBBDD;
		cLlibres = ControladorDomini.getInstance();
		BufferedImage img = null;
		ImageIcon imageIcon = null;
		try {
			img = ImageIO.read(new FileInputStream(l.getPortada()));
			Image dimg = img.getScaledInstance(this.vista.getLabelIcono().getWidth(),
					this.vista.getLabelIcono().getHeight(), Image.SCALE_SMOOTH);

			imageIcon = new ImageIcon(dimg);
		} catch (IOException e) {
			try {
				img = ImageIO.read(new FileInputStream("portades/default_cover.png"));
				Image dimg = img.getScaledInstance(this.vista.getLabelIcono().getWidth(),
						this.vista.getLabelIcono().getHeight(), Image.SCALE_SMOOTH);

				imageIcon = new ImageIcon(dimg);
			} catch (IOException e1) {
				new DialogoError(e1).showErrorMessage();
			}
		}

		this.vista.getLabelIcono().setIcon(imageIcon);

		this.vista.getBtnEditar().addActionListener(e -> editar(l));

		this.vista.getTextAny().setText(l.getAny().toString());
		this.vista.getTextAutor().setText(l.getAutor().toString());
		this.vista.getTextISBN().setText(l.getISBN().toString());
		this.vista.getTextDescripcio().setText(l.getDescripcio().toString());
		this.vista.getTextNom().setText(l.getNom().toString());
		this.vista.getTextPortada().setText(l.getPortada().toString());
		this.vista.getTextPreu().setText(l.getPreu().toString());
		this.vista.getTextValoracio().setText(l.getValoracio().toString());
		this.vista.getChckLlegit().setSelected(l.getLlegit());

		this.vista.setTitle("Expedient del llibre " + l.getNom());
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
			this.vista.getBtnEditar().setText("Editar");
			try {
				cLlibres.deleteLlibre(llibre);
				Llibre a = checkLlibre.cheackLlibre(Integer.parseInt(vista.getTextISBN().getText()),
						vista.getTextNom().getText(), vista.getTextAutor().getText(),
						Integer.parseInt(vista.getTextAny().getText()), vista.getTextDescripcio().getText(),
						Double.parseDouble(vista.getTextValoracio().getText()),
						Double.parseDouble(vista.getTextPreu().getText()), vista.getChckLlegit().isSelected(),
						vista.getTextPortada().getText());
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
