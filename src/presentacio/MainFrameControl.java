package presentacio;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.Config;
import herramienta.DialogoError;
import interficie.EnActualizarBBDD;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class MainFrameControl implements EnActualizarBBDD {

	private ControladorDomini cLlibres;
	private ArrayList<Llibre> biblio;
	private MostrarBibliotecaControl mostrarControl;
	private MainFramePanel vista;
	private static MainFrameControl instance;

	private MainFrameControl(MainFramePanel vista) {
		this.vista = vista;
		cLlibres = ControladorDomini.getInstance();

		int ctrl = java.awt.event.InputEvent.CTRL_DOWN_MASK;
		javax.swing.InputMap  im = this.vista.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		javax.swing.ActionMap am = this.vista.getRootPane().getActionMap();

		this.vista.getMostrarBibliotecaPanel().getBtnNouLlibre()
				.addActionListener(e -> new Thread(this::obrirNouLlibreDialeg).start());

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl), "nouLlibre");
		am.put("nouLlibre", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				new Thread(MainFrameControl.this::obrirNouLlibreDialeg).start();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl), "focusFiltres");
		am.put("focusFiltres", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				vista.getMostrarBibliotecaPanel().getTextISBN().requestFocusInWindow();
			}
		});

		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getInputMap(JComponent.WHEN_FOCUSED)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminarFila");
		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getActionMap().put("eliminarFila", new javax.swing.AbstractAction() {
					@Override public void actionPerformed(java.awt.event.ActionEvent e) {
						mostrarControl.eliminarFilaSeleccionada();
					}
				});

		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getInputMap(JComponent.WHEN_FOCUSED)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl), "seleccionarTot");
		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getActionMap().put("seleccionarTot", new javax.swing.AbstractAction() {
					@Override public void actionPerformed(java.awt.event.ActionEvent e) {
						JTable t = vista.getMostrarBibliotecaPanel().getjTableBilio();
						if (t.getRowCount() > 0) t.setRowSelectionInterval(0, t.getRowCount() - 1);
					}
				});

		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getInputMap(JComponent.WHEN_FOCUSED)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrl), "editarLlibre");
		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getActionMap().put("editarLlibre", new javax.swing.AbstractAction() {
					@Override public void actionPerformed(java.awt.event.ActionEvent e) {
						mostrarControl.abrirDetallesEnEdicio();
					}
				});

		mostrarControl = new MostrarBibliotecaControl(
				this.vista.getMostrarBibliotecaPanel(), cLlibres.getAllLlibres(), this);

		// Track normal (non-maximized) bounds so we can save them on close
		final java.awt.Rectangle[] normalBounds = {
			new java.awt.Rectangle(Config.getWindowX(), Config.getWindowY(),
				Config.getWindowWidth(), Config.getWindowHeight())
		};
		this.vista.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override public void componentResized(java.awt.event.ComponentEvent e) { captureNormal(); }
			@Override public void componentMoved(java.awt.event.ComponentEvent e)   { captureNormal(); }
			private void captureNormal() {
				if ((vista.getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) == 0)
					normalBounds[0] = vista.getBounds();
			}
		});
		this.vista.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) {
				boolean max = (vista.getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) != 0;
				Config.setWindowMaximized(max);
				java.awt.Rectangle r = normalBounds[0];
				Config.setWindowBounds(r.x, r.y, r.width, r.height);
			}
		});
	}

	public static MainFrameControl getInstance(MainFramePanel vista) {
		if (instance == null) instance = new MainFrameControl(vista);
		return instance;
	}

	private void obrirNouLlibreDialeg() {
		GuardarLlibresDialogoControl ctrl = new GuardarLlibresDialogoControl(new GuardarLlibresDialogo());
		ctrl.getVista().setLocationRelativeTo(this.vista);
		ctrl.getVista().setVisible(true);
		mostrarControl.refresh();
	}

	protected ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Long ISBN,
			Integer iniciAny, Integer fiAny,
			Double valoracioMin, Double valoracioMax,
			Double preuMin, Double preuMax, Boolean llegit) {
		return cLlibres.aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny,
			valoracioMin, valoracioMax, preuMin, preuMax, llegit);
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
		if (nuevo) {
			Integer llistaId = mostrarControl.getCurrentLlistaId();
			if (llistaId != null) {
				try {
					cLlibres.addLlibreToLlista(l.getISBN(), llistaId, 0.0, false);
				} catch (Exception e) {
					new DialogoError(e).showErrorMessage();
				}
			}
		}
		mostrarControl.refreshLlibre(l, nuevo);
	}

	@Override
	public void eliminarLlibre(Llibre l) {
		mostrarControl.eliminarFila(l);
	}
}
