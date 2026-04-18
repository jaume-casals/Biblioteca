package presentacio;

import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import interficie.EnActualizarBBDD;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class MainFrameControl implements EnActualizarBBDD {

	private ControladorDomini cLlibres;
	private ArrayList<Llibre> biblio;
	private MostrarBibliotecaControl MostrarBibliotecaControl;
	private MainFramePanel vista;
	private GuardarLlibresDialogoControl guardarLlibresDialogoControl;
	private static MainFrameControl instance;

	private MainFrameControl(MainFramePanel vista) {
		this.vista = vista;

		cLlibres = ControladorDomini.getInstance();

		this.vista.getMostrarBibliotecaPanel().getBtnNouLlibre()
				.addActionListener(e -> new Thread(() -> crearGuardarLlibreDialogo()).start());

		// Ctrl+N — open new book dialog from anywhere in the main window
		this.vista.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK), "nouLlibre");
		this.vista.getRootPane().getActionMap().put("nouLlibre",
				new javax.swing.AbstractAction() {
					@Override public void actionPerformed(java.awt.event.ActionEvent e) {
						new Thread(() -> crearGuardarLlibreDialogo()).start();
					}
				});

		MostrarBibliotecaControl = new MostrarBibliotecaControl(this.vista.getMostrarBibliotecaPanel(),
				cLlibres.getAllLlibres(), this);
	}

	public static MainFrameControl getInstance(MainFramePanel vista) {
		if (instance == null) {
			instance = new MainFrameControl(vista);
		}
		return instance;
	}

	protected ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Long ISBN,
			Integer iniciAny, Integer fiAny,
			Double valoracioMin, Double valoracioMax,
			Double preuMin, Double preuMax, Boolean Llegit) {
		return cLlibres.aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny,
			valoracioMin, valoracioMax, preuMin, preuMax, Llegit);
	}

	private WindowEvent caca() {
		MostrarBibliotecaControl.refresh();
		return null;

	}

	private void crearGuardarLlibreDialogo() {
		this.guardarLlibresDialogoControl = new GuardarLlibresDialogoControl(new GuardarLlibresDialogo());
		this.guardarLlibresDialogoControl.getVista().setLocationRelativeTo(this.vista);
		this.guardarLlibresDialogoControl.getVista().setVisible(true);
		this.guardarLlibresDialogoControl.windowClosed(caca());
	}

	protected Llibre getLlibreIsbn(long ISBN) {

		try {
			return cLlibres.getLlibre(ISBN);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
		return null;
	}

	public void setVisible(boolean b) {
		this.vista.setVisible(b);
	}

	@Override
	public void actualitzarLlibre(Llibre l, boolean nuevo) {
		this.MostrarBibliotecaControl.refreshLlibre(l, nuevo);
	}

	@Override
	public void eliminarLlibre(Llibre l) {
		this.MostrarBibliotecaControl.eliminarFila(l);
	}

}
