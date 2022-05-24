package presentacio.detalles.control;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import domini.Llibre;
import presentacio.detalles.vista.DetallesLlibrePanel;

public class DetallesLlibrePanelControl {

	private DetallesLlibrePanel vista;

	public DetallesLlibrePanelControl(Llibre l) {
		this.vista = new DetallesLlibrePanel();

		BufferedImage img = null;
		try {
			img = ImageIO.read(new File("D:\\GitHub\\Biblioteca\\portades\\" + l.getNom() + ".png"));
		} catch (IOException e) {
			try {
				img = ImageIO.read(new File("D:\\GitHub\\Biblioteca\\portades\\default_cover.png"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		Image dimg = img.getScaledInstance(this.vista.getLabelIcono().getWidth(),
				this.vista.getLabelIcono().getHeight(), Image.SCALE_SMOOTH);

		ImageIcon imageIcon = new ImageIcon(dimg);

		this.vista.getLabelIcono().setIcon(imageIcon);

		this.vista.getBtnEditar().addActionListener(e -> editar());

		this.vista.getTextAny().setText(l.getAny().toString());
		this.vista.getTextAutor().setText(l.getAutor().toString());
		this.vista.getTextISBN().setText(l.getISBN().toString());
		this.vista.getTextDescripcio().setText(l.getDescripcio().toString());
		this.vista.getTextNom().setText(l.getNom().toString());
		this.vista.getTextPortada().setText(l.getPortada().toString());
		this.vista.getTextPreu().setText(l.getPreu().toString());
		this.vista.getTextValoracio().setText(l.getValoracio().toString());
		this.vista.getChckLlegit().setSelected(l.getLlegit());

	}

	private void editar() {
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
		}
	}

	public DetallesLlibrePanel getDetallesLlibrePanel() {
		return this.vista;
	}
}
