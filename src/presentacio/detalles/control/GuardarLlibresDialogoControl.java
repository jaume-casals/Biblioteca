package presentacio.detalles.control;

import java.awt.Dialog;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class GuardarLlibresDialogoControl implements WindowListener {

	private GuardarLlibresDialogo vista;
	private ControladorDomini cLlibres;

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista) {
		this.vista = vista;
		this.vista.setFocusable(true);
		this.vista.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					vista.dispose();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

			}
		});
		this.vista.getBtnGuardar().addActionListener(e -> crearLlibre());
		this.vista.getBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		cLlibres = ControladorDomini.getInstance();
	}

	private static final String DEFAULT_IMG_DIR = "/home/j/Downloads/b/Harem_Hotel-v0.19.1-pc/game/images/";

	private void seleccionarImatge() {
		JFileChooser chooser = new JFileChooser(new File(DEFAULT_IMG_DIR).exists()
			? DEFAULT_IMG_DIR : System.getProperty("user.home"));
		chooser.setFileFilter(new FileNameExtensionFilter("Imatges", "jpg", "jpeg", "png", "gif", "bmp", "webp"));
		if (chooser.showOpenDialog(this.vista) == JFileChooser.APPROVE_OPTION) {
			this.vista.getTextPortada().setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void crearLlibre() {
		System.out.println(Long.parseLong(vista.getTextISBN().getText()));

		try {
			cLlibres.addLlibre(new Llibre(Long.parseLong(vista.getTextISBN().getText()), vista.getTextNom().getText(),
					vista.getTextAutor().getText(), Integer.parseInt(vista.getTextAny().getText()),
					vista.getTextDescripcio().getText(), Double.parseDouble(vista.getTextValoracio().getText()),
					Double.parseDouble(vista.getTextPreu().getText()), vista.getChckLlegit().isSelected(),
					vista.getTextPortada().getText()));
			vista.dispose();

		} catch (Exception e) {
			System.out.println("guardarllibresdialogocontrol" + e);

			new DialogoError(e).showErrorMessage();
		}

	}

	public Dialog getVista() {
		return vista;
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

}
