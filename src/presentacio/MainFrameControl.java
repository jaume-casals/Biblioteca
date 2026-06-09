package presentacio;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import domini.Llibre;
import domini.LlibreFilter;
import herramienta.Config;
import herramienta.DialogoError;
import herramienta.I18n;
import interficie.BibliotecaWriter;
import presentacio.listener.EnActualizarBBDD;

/**
 * Facade over the 6 sub-controllers (bookActions, bookIO, contextMenu, filter,
 * shelf, tablePage) for the main screen. MostrarBibliotecaControl owns the
 * table/list rendering; this class owns the top-level frame, menubar, and
 * orchestration.
 */
public class MainFrameControl implements presentacio.listener.EnActualizarBBDD {

	private final BibliotecaWriter cLlibres;
	private final MostrarBibliotecaPanel libraryPanel;
	private final MainFramePanel panel;
	private final JFrame frame;
	private MostrarBibliotecaControl mostrarControl;
	private BookIOController ioCtrl;
	/**
	 * Singleton — un sol MainFrameControl per tota l'aplicació. A diferència
	 * de {@link domini.ControladorDomini}, no té resetForTest() perquè la UI
	 * Swing no es destrueix entre test runs: els tests que toquin la UI
	 * usen la bestreta de MostrarBibliotecaControl via SpringLayout o
	 * directament. La dependència de Swing fa que el patró DI de
	 * ControladorDomini (constructor injectable) no sigui fàcilment
	 * aplicable aquí.
	 */
	private static MainFrameControl instance;

	private MainFrameControl(MainFramePanel panel, BibliotecaWriter cd) {
		this.panel = panel;
		this.libraryPanel = panel.getMostrarBibliotecaPanel();
		this.cLlibres = cd;
		this.frame = new JFrame(I18n.t("app_title"));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setBounds(Config.getWindowX(), Config.getWindowY(),
				Config.getWindowWidth(), Config.getWindowHeight());
		frame.setMinimumSize(new Dimension(800, 500));
		if (Config.isWindowMaximized()) frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setContentPane(panel);
		frame.pack();

		ioCtrl = new BookIOController(panel, cd, () -> cLlibres.getAllLlibres(), () -> mostrarControl.refresh());

		int ctrl = java.awt.event.InputEvent.CTRL_DOWN_MASK;
		int ctrlShift = ctrl | java.awt.event.InputEvent.SHIFT_DOWN_MASK;
		JComponent root = frame.getRootPane();
		javax.swing.InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		javax.swing.ActionMap am = root.getActionMap();

		libraryPanel.getBtnNouLlibre()
				.addActionListener(e -> SwingUtilities.invokeLater(this::obrirNouLlibreDialeg));

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl), "nouLlibre");
		am.put("nouLlibre", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				SwingUtilities.invokeLater(MainFrameControl.this::obrirNouLlibreDialeg);
			}
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrlShift), "nouLlibreScan");
		am.put("nouLlibreScan", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				SwingUtilities.invokeLater(() -> ioCtrl.escanejarISBN());
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl), "focusFiltres");
		am.put("focusFiltres", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				libraryPanel.getSearchBar().requestFocusInWindow();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ctrl), "galeriaZoomIn");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ctrl), "galeriaZoomIn");
		am.put("galeriaZoomIn", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				libraryPanel.getGaleria().adjustZoom(1);
			}
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ctrl), "galeriaZoomOut");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ctrl), "galeriaZoomOut");
		am.put("galeriaZoomOut", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				libraryPanel.getGaleria().adjustZoom(-1);
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl), "seleccionarTot");
		am.put("seleccionarTot", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				if (libraryPanel.isGaleriaMode()) {
					libraryPanel.getGaleria().selectAll();
				} else {
					JTable t = libraryPanel.getjTableBilio();
					if (t.getRowCount() > 0) t.setRowSelectionInterval(0, t.getRowCount() - 1);
				}
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrl), "editarLlibre");
		am.put("editarLlibre", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				mostrarControl.abrirDetallesEnEdicio();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, ctrlShift), "toggleTheme");
		am.put("toggleTheme", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				herramienta.UITheme.Theme[] themes = herramienta.UITheme.Theme.values();
				herramienta.UITheme.Theme next = themes[(herramienta.UITheme.getTheme().ordinal() + 1) % themes.length];
				herramienta.UITheme.setTheme(next);
				libraryPanel.applyTheme();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl), "undoDelete");
		am.put("undoDelete", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				mostrarControl.undoDelete();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "ajudaDreceres");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, ctrl), "ajudaDreceres");
		am.put("ajudaDreceres", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				mostrarAjudaDreceres();
			}
		});

		libraryPanel.getjTableBilio().getInputMap(JComponent.WHEN_FOCUSED)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminarFila");
		libraryPanel.getjTableBilio().getActionMap().put("eliminarFila", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				mostrarControl.eliminarFilaSeleccionada();
			}
		});

		mostrarControl = new MostrarBibliotecaControl(
				libraryPanel, new ArrayList<>(cLlibres.getAllLlibres()), this, cLlibres);

		final java.awt.Rectangle[] normalBounds = {
			new java.awt.Rectangle(Config.getWindowX(), Config.getWindowY(),
				Config.getWindowWidth(), Config.getWindowHeight())
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
				Config.setWindowMaximized(max);
				java.awt.Rectangle r = normalBounds[0];
				Config.setWindowBounds(r.x, r.y, r.width, r.height);
				System.exit(0);
			}
		});
	}

	public static MainFrameControl getInstance(MainFramePanel panel, BibliotecaWriter cd) {
		if (instance == null && panel != null) instance = new MainFrameControl(panel, cd);
		return instance;
	}

	public static MainFrameControl getInstance() {
		return instance;
	}

	public JFrame getFrame() { return frame; }

	private void obrirNouLlibreDialeg() {
		presentacio.detalles.control.GuardarLlibresDialogoControl ctrl =
			new presentacio.detalles.control.GuardarLlibresDialogoControl(
				new presentacio.detalles.vista.GuardarLlibresDialogo(), this, cLlibres);
		ctrl.getVista().setLocationRelativeTo(frame);
		ctrl.getVista().setVisible(true);
	}

	protected List<Llibre> aplicarFiltres(LlibreFilter f) {
		return cLlibres.aplicarFiltres(f);
	}

	public Llibre getLlibreIsbn(long ISBN) {
		try {
			return cLlibres.getLlibre(ISBN);
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
					List<Object[]> loans = cLlibres.getAllOverdueLoans(30);
					if (!loans.isEmpty()) {
						sb = new StringBuilder(I18n.t("alert_overdue_loans_msg") + "\n\n");
						for (Object[] row : loans) {
							sb.append("• ").append(row[0]).append(" → ").append(row[1]).append(" (").append(row[2]).append(")\n");
						}
						overdueMsg = sb.toString();
					}
				} catch (Exception ignored) {}

				try {
					int goal = herramienta.Config.getReadingGoal();
					if (goal > 0) {
						List<Llibre> all = cLlibres.getAllLlibres();
						java.time.LocalDate today = java.time.LocalDate.now();
						int currentYear = today.getYear();
						int dayOfYear = today.getDayOfYear();
						int daysInYear = today.isLeapYear() ? 366 : 365;
						long readThisYear = all.stream()
							.filter(l -> Boolean.TRUE.equals(l.getLlegit()))
							.filter(l -> l.getDataLectura() != null
								&& herramienta.DateUtils.parseYear(l.getDataLectura()) == currentYear)
							.count();
						int daysLeft = daysInYear - dayOfYear;
						double neededPerDay = daysLeft > 0 ? (double)(goal - readThisYear) / daysLeft : 0;
						double actualPerDay = dayOfYear > 0 ? (double) readThisYear / dayOfYear : 0;
						if (readThisYear < goal && neededPerDay > actualPerDay * 1.5 && daysLeft < 90) {
							goalMsg = I18n.t("alert_goal_pace_msg", goal, readThisYear, daysLeft, String.format("%.2f", neededPerDay));
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
					javax.swing.JOptionPane.showMessageDialog(frame, goalMsg,
						I18n.t("alert_goal_pace_title"), javax.swing.JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}.execute();
	}

	@Override
	public void onBookUpdated(Llibre l, boolean isNew) {
		if (isNew) assignToCurrentShelfIfNeeded(l);
		mostrarControl.refreshLlibre(l, isNew);
	}

	/**
	 * If the user is currently viewing a specific shelf, add a newly created
	 * book to that shelf so it shows up immediately.  No-op when:
	 *  - the book is not new (this helper is for {@code isNew} only),
	 *  - the user is on "All books" (no shelf filter),
	 *  - the add fails (caller already handled the user-facing error).
	 */
	private void assignToCurrentShelfIfNeeded(Llibre l) {
		Integer llistaId = mostrarControl.getCurrentLlistaId();
		if (llistaId == null) return;
		try {
			cLlibres.addLlibreToLlista(l.getISBN(), llistaId, 0.0, false);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	@Override
	public void onBookDeleted(Llibre l) {
		mostrarControl.eliminarFila(l);
	}

	private void mostrarAjudaDreceres() {
		javax.swing.JOptionPane.showMessageDialog(frame, I18n.t("dlg_shortcuts_content"),
			I18n.t("dlg_shortcuts_title"), javax.swing.JOptionPane.INFORMATION_MESSAGE);
	}
}
