package presentacio.controladors;

import domini.Llibre;
import domini.LlibreFilter;
import herramienta.config.Configuracio;
import herramienta.config.ConfiguracioFinestra;
import herramienta.i18n.I18n;
import herramienta.ui.DialegError;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import persistencia.contract.EscritorBiblioteca;
import persistencia.row.PrestecEndarrerit;
import presentacio.listener.EnActualitzarBBDD;
import presentacio.panells.PanelMarcPrincipal;
import presentacio.panells.PanelMostrarBiblioteca;
import presentacio.util.DreceresTeclat;



/**
 * Façana sobre els 6 sub-controladors (bookActions, bookIO, contextMenu,
 * filter, shelf, tablePage) per a la pantalla principal.
 * MostrarBibliotecaControl posseeix el renderitzat de la taula/llista;
 * aquesta classe posseeix el frame de nivell superior, la barra de menú
 * i l'orquestració.
 */
public class ControladorMarcPrincipal implements presentacio.listener.EnActualitzarBBDD {

	private final EscritorBiblioteca cLlibres;
	private final PanelMostrarBiblioteca libraryPanel;
	private final PanelMarcPrincipal panel;
	private final JFrame frame;
	private ControladorMostrarBiblioteca mostrarControl;
	private ControladorIOLlibre ioCtrl;
	private presentacio.dialegs.EstacioEscaneigDialog estacioDialog;
	/**
	 * Singleton — un sol MainFrameControl per tota l'aplicació. A diferència
	 * de {@link domini.ControladorDomini}, no té resetForTest() perquè la UI
	 * Swing no es destrueix entre test runs: els tests que toquin la UI
	 * usen la bestreta de MostrarBibliotecaControl via SpringLayout o
	 * directament. La dependència de Swing fa que el patró DI de
	 * ControladorDomini (constructor injectable) no sigui fàcilment
	 * aplicable aquí.
	 */
	private static volatile ControladorMarcPrincipal instance;

	private ControladorMarcPrincipal(PanelMarcPrincipal panel, EscritorBiblioteca cd) {
		this.panel = panel;
		this.libraryPanel = panel.obtenirMostrarBibliotecaPanel();
		this.cLlibres = cd;
		this.frame = new JFrame(I18n.t("app_title"));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setBounds(Configuracio.obtenirWindowX(), Configuracio.obtenirWindowY(),
				Configuracio.obtenirWindowWidth(), Configuracio.obtenirWindowHeight());
		frame.setMinimumSize(new Dimension(800, 500));
		if (Configuracio.esWindowMaximized()) frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setContentPane(panel);
		frame.pack();

		ioCtrl = new ControladorIOLlibre(panel, cd, () -> cLlibres.obtenirAllLlibres(), () -> mostrarControl.refresh());

		final int ctrl = java.awt.event.InputEvent.CTRL_DOWN_MASK;
		final int ctrlShift = ctrl | java.awt.event.InputEvent.SHIFT_DOWN_MASK;
		JRootPane root = frame.getRootPane();

		libraryPanel.obtenirBtnNouLlibre()
				.addActionListener(e -> SwingUtilities.invokeLater(this::obrirNouLlibreDialeg));

		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl), "nouLlibre",
			() -> SwingUtilities.invokeLater(this::obrirNouLlibreDialeg));
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrlShift), "nouLlibreScan",
			() -> SwingUtilities.invokeLater(() -> ioCtrl.escanejarISBN()));
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_S, ctrlShift), "estacioEscaneig",
			() -> SwingUtilities.invokeLater(this::obrirEstacioEscaneig));
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl), "focusFiltres",
			() -> libraryPanel.obtenirSearchBar().requestFocusInWindow());
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ctrl), "galeriaZoomIn",
			() -> libraryPanel.obtenirGaleria().adjustZoom(1));
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ctrl), "galeriaZoomIn",
			() -> libraryPanel.obtenirGaleria().adjustZoom(1));
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ctrl), "galeriaZoomOut",
			() -> libraryPanel.obtenirGaleria().adjustZoom(-1));
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ctrl), "galeriaZoomOut",
			() -> libraryPanel.obtenirGaleria().adjustZoom(-1));
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl), "seleccionarTot", () -> {
			if (libraryPanel.esGaleriaMode()) {
				libraryPanel.obtenirGaleria().selectAll();
			} else {
				JTable t = libraryPanel.obtenirTaulaLlibres();
				if (t.getRowCount() > 0) t.setRowSelectionInterval(0, t.getRowCount() - 1);
			}
		});
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrl), "editarLlibre",
			() -> mostrarControl.abrirDetallesEnEdicio());
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_D, ctrlShift), "toggleTheme", () -> {
			herramienta.ui.UITheme.Tema[] themes = herramienta.ui.UITheme.Tema.values();
			herramienta.ui.UITheme.Tema next = themes[(herramienta.ui.UITheme.obtenirTheme().ordinal() + 1) % themes.length];
			herramienta.ui.UITheme.posarTheme(next);
			libraryPanel.aplicarTheme();
		});
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl), "undoDelete",
			() -> mostrarControl.undoDelete());
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "ajudaDreceres",
			this::mostrarAjudaDreceres);
		DreceresTeclat.bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, ctrl), "ajudaDreceres",
			this::mostrarAjudaDreceres);

		DreceresTeclat.bind(libraryPanel.obtenirTaulaLlibres(), JComponent.WHEN_FOCUSED,
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminarFila",
			() -> mostrarControl.eliminarFilaSeleccionada());

		mostrarControl = new ControladorMostrarBiblioteca(
				libraryPanel, new ArrayList<>(), this, cLlibres);

		new SwingWorker<ArrayList<domini.Llibre>, Void>() {
			@Override protected ArrayList<domini.Llibre> doInBackground() {
				return new ArrayList<>(cLlibres.obtenirAllLlibres());
			}
			@Override protected void done() {
				try { mostrarControl.posarTable(get()); }
				catch (Exception e) { java.util.logging.Logger.getLogger(ControladorMarcPrincipal.class.getName())
					.log(java.util.logging.Level.WARNING, "No s'ha pogut carregar la biblioteca inicial", e); }
			}
		}.execute();

		final java.awt.Rectangle[] normalBounds = {
			new java.awt.Rectangle(Configuracio.obtenirWindowX(), Configuracio.obtenirWindowY(),
				Configuracio.obtenirWindowWidth(), Configuracio.obtenirWindowHeight())
		};
		frame.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override public void componentResized(java.awt.event.ComponentEvent e) { captureNormal(); }
			@Override public void componentMoved(java.awt.event.ComponentEvent e) { captureNormal(); }
			private void captureNormal() {
				if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0)
					normalBounds[0] = frame.getBounds();
			}
		});
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) {
				if (javax.swing.JOptionPane.showConfirmDialog(frame,
						I18n.t("confirm_exit_msg"), I18n.t("confirm_exit_title"),
						javax.swing.JOptionPane.YES_NO_OPTION) != javax.swing.JOptionPane.YES_OPTION) return;
				boolean max = (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
				ConfiguracioFinestra.posarMaximized(max);
				java.awt.Rectangle r = normalBounds[0];
				ConfiguracioFinestra.setBounds(r.x, r.y, r.width, r.height);
				System.exit(0);
			}
		});
	}

	public static ControladorMarcPrincipal getInstance(PanelMarcPrincipal panel, EscritorBiblioteca cd) {
		synchronized (ControladorMarcPrincipal.class) {
			if (instance == null && panel != null) instance = new ControladorMarcPrincipal(panel, cd);
			else if (instance != null && panel != null) throw new IllegalStateException("MainFrameControl already initialized");
		}
		return instance;
	}

	public static ControladorMarcPrincipal getInstance() {
		return instance;
	}

	public JFrame obtenirFrame() { return frame; }

	private void obrirNouLlibreDialeg() {
		presentacio.detalles.control.ControladorDialegDesarLlibres ctrl =
			new presentacio.detalles.control.ControladorDialegDesarLlibres(
				new presentacio.detalles.vista.DialegDesarLlibres(), this, cLlibres);
		ctrl.obtenirVista().setLocationRelativeTo(frame);
		ctrl.obtenirVista().setVisible(true);
	}

	private void obrirEstacioEscaneig() {
		if (estacioDialog == null || !estacioDialog.isDisplayable()) {
			estacioDialog = new presentacio.dialegs.EstacioEscaneigDialog(frame, cLlibres, this);
		}
		estacioDialog.setVisible(true);
		estacioDialog.obtenirTextFieldISBN().requestFocusInWindow();
	}

	protected List<Llibre> aplicarFiltres(LlibreFilter f) {
		return cLlibres.aplicarFiltres(f);
	}

	public Llibre obtenirLlibreIsbn(long ISBN) {
		try {
			return cLlibres.obtenirLlibre(ISBN);
		} catch (Exception e) {
			return null;
		}
	}

	public void setVisible(boolean b) {
		frame.setVisible(b);
		if (b) javax.swing.SwingUtilities.invokeLater(this::mostrarAlertes);
	}

	private void mostrarAlertes() {
		new javax.swing.SwingWorker<Void, Void>() {
			private String overdueMsg;
			private String goalMsg;
			@Override
			protected Void doInBackground() {
				StringBuilder sb;
				try {
					List<persistencia.row.PrestecEndarrerit> loans = cLlibres.obtenirAllOverdueLoans(30);
					if (!loans.isEmpty()) {
						sb = new StringBuilder(I18n.t("alert_overdue_loans_msg") + "\n\n");
						for (persistencia.row.PrestecEndarrerit row : loans) {
							sb.append("• ").append(row.nomPersona()).append(" → ").append(row.nomLlibre()).append(" (").append(row.dataPrestecDisplay()).append(")\n");
						}
						overdueMsg = sb.toString();
					}
				} catch (Exception ignored) {}

				try {
					int goal = herramienta.config.Configuracio.obtenirReadingGoal();
					if (goal > 0) {
						List<Llibre> all = cLlibres.obtenirAllLlibres();
						java.time.LocalDate today = java.time.LocalDate.now();
						int currentYear = today.getYear();
						int dayOfYear = today.getDayOfYear();
						int daysInYear = today.isLeapYear() ? 366 : 365;
						long llegirThisYear = all.stream()
							.filter(l -> Boolean.TRUE.equals(l.obtenirLlegit()))
							.filter(l -> l.obtenirDataLectura() != null
								&& herramienta.text.UtilitatsData.analitzarYear(l.obtenirDataLectura())
									.filter(y -> y == currentYear).isPresent())
							.count();
						int daysLeft = daysInYear - dayOfYear;
						double neededPerDay = daysLeft > 0 ? (double)(goal - llegirThisYear) / daysLeft : 0;
						double actualPerDay = dayOfYear > 0 ? (double) llegirThisYear / dayOfYear : 0;
						if (llegirThisYear < goal && neededPerDay > actualPerDay * 1.5 && daysLeft < 90) {
							goalMsg = I18n.t("alert_goal_pace_msg", goal, llegirThisYear, daysLeft, String.format("%.2f", neededPerDay));
						}
					}
				} catch (Exception ignored) {}
				return null;
			}
			@Override
			protected void done() {
				if (overdueMsg != null) {
					javax.swing.JOptionPane.showMessageDialog(frame, overdueMsg,
						I18n.t("alert_overdue_loans_title"), javax.swing.JOptionPane.WARNING_MESSAGE);
				}
				if (goalMsg != null) {
					// Cua el segon diàleg en un invokeLater perquè l'usuari
					// pugui dismissar cada alerta per separat en lloc de
					// trobar-se dos JOptionPane apilats (segons el finding
					// MEDIUM de tot.txt).
					final String g = goalMsg;
					javax.swing.SwingUtilities.invokeLater(() ->
						javax.swing.JOptionPane.showMessageDialog(frame, g,
							I18n.t("alert_goal_pace_title"), javax.swing.JOptionPane.INFORMATION_MESSAGE));
				}
			}
		}.execute();
	}

	@Override
	public void enActualitzarLlibre(Llibre l, boolean esNew) {
		if (esNew) assignToCurrentShelfIfNeeded(l);
		mostrarControl.refrescarLlibre(l, esNew);
	}

	/**
	 * Si l'usuari està veient una prestatgeria específica, afegeix un llibre
	 * acabat de crear a aquesta prestatgeria perquè aparegui immediatament.
	 * No-op quan:
	 *  - el llibre no és nou (aquest helper és només per a {@code isNew}),
	 *  - l'usuari és a "Tots els llibres" (sense filtre de prestatgeria),
	 *  - l'addició falla (el ja ha gestionat l'error visible a l'usuari).
	 */
	private void assignToCurrentShelfIfNeeded(Llibre l) {
		Integer llistaId = mostrarControl.obtenirCurrentLlistaId();
		if (llistaId == null) return;
		try {
			cLlibres.afegirLlibreToLlista(l.obtenirISBN(), llistaId, 0.0, false);
		} catch (Exception e) {
			new DialegError(e).mostrarErrorMessage();
		}
	}

	@Override
	public void enEliminarLlibre(Llibre l) {
		mostrarControl.eliminarFila(l);
	}

	private void mostrarAjudaDreceres() {
		javax.swing.JOptionPane.showMessageDialog(frame, I18n.t("dlg_shortcuts_content"),
			I18n.t("dlg_shortcuts_title"), javax.swing.JOptionPane.INFORMATION_MESSAGE);
	}
}


