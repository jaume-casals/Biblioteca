package presentacio;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import domini.Llibre;
import domini.LlibreFilter;
import interficie.BibliotecaWriter;
import herramienta.Config;
import herramienta.DialogoError;
import herramienta.I18n;
import interficie.EnActualizarBBDD;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class MainFrameControl implements EnActualizarBBDD {

	private BibliotecaWriter cLlibres;
	private MostrarBibliotecaControl mostrarControl;
	private MainFramePanel vista;
	private static MainFrameControl instance;

	private MainFrameControl(MainFramePanel vista, BibliotecaWriter cd) {
		this.vista = vista;
		cLlibres = cd;

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
				vista.getMostrarBibliotecaPanel().getSearchBar().requestFocusInWindow();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ctrl), "galeriaZoomIn");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,    ctrl), "galeriaZoomIn");
		am.put("galeriaZoomIn", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				vista.getMostrarBibliotecaPanel().getGaleria().adjustZoom(1);
			}
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,    ctrl), "galeriaZoomOut");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ctrl), "galeriaZoomOut");
		am.put("galeriaZoomOut", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				vista.getMostrarBibliotecaPanel().getGaleria().adjustZoom(-1);
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl), "seleccionarTot");
		am.put("seleccionarTot", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				if (vista.getMostrarBibliotecaPanel().isGaleriaMode()) {
					vista.getMostrarBibliotecaPanel().getGaleria().selectAll();
				} else {
					JTable t = vista.getMostrarBibliotecaPanel().getjTableBilio();
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

		int ctrlShift = ctrl | java.awt.event.InputEvent.SHIFT_DOWN_MASK;
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, ctrlShift), "toggleTheme");
		am.put("toggleTheme", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				herramienta.UITheme.Theme[] themes = herramienta.UITheme.Theme.values();
				herramienta.UITheme.Theme next = themes[(herramienta.UITheme.getTheme().ordinal() + 1) % themes.length];
				herramienta.UITheme.setTheme(next);
				vista.getMostrarBibliotecaPanel().applyTheme();
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

		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getInputMap(JComponent.WHEN_FOCUSED)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminarFila");
		this.vista.getMostrarBibliotecaPanel().getjTableBilio()
				.getActionMap().put("eliminarFila", new javax.swing.AbstractAction() {
					@Override public void actionPerformed(java.awt.event.ActionEvent e) {
						mostrarControl.eliminarFilaSeleccionada();
					}
				});

		mostrarControl = new MostrarBibliotecaControl(
				this.vista.getMostrarBibliotecaPanel(), cLlibres.getAllLlibres(), this, cLlibres);

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
				if (javax.swing.JOptionPane.showConfirmDialog(vista,
						I18n.t("confirm_exit_msg"), I18n.t("confirm_exit_title"),
						javax.swing.JOptionPane.YES_NO_OPTION) != javax.swing.JOptionPane.YES_OPTION) return;
				boolean max = (vista.getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) != 0;
				Config.setWindowMaximized(max);
				java.awt.Rectangle r = normalBounds[0];
				Config.setWindowBounds(r.x, r.y, r.width, r.height);
				System.exit(0);
			}
		});
	}

	// Singleton — call order: getInstance(vista, cd) first, then getInstance() anywhere else.
	public static MainFrameControl getInstance(MainFramePanel vista, BibliotecaWriter cd) {
		if (instance == null && vista != null) instance = new MainFrameControl(vista, cd);
		return instance;
	}

	public static MainFrameControl getInstance(MainFramePanel vista) {
		if (instance == null && vista != null) throw new IllegalStateException("MainFrameControl not yet initialized — use getInstance(vista, cd)");
		return instance;
	}

	public static MainFrameControl getInstance() {
		return instance;
	}

	private void obrirNouLlibreDialeg() {
		GuardarLlibresDialogoControl ctrl = new GuardarLlibresDialogoControl(new GuardarLlibresDialogo(), this, cLlibres);
		ctrl.getVista().setLocationRelativeTo(this.vista);
		ctrl.getVista().setVisible(true);
	}

	protected ArrayList<Llibre> aplicarFiltres(LlibreFilter f) {
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
		this.vista.setVisible(b);
		if (b) {
			javax.swing.SwingUtilities.invokeLater(this::mostrarAlertes);
		}
	}

	private void mostrarAlertes() {
		// Overdue loan alert: retornat=false and data_prestec > 30 days ago
		try {
			java.util.List<Object[]> loans = cLlibres.getAllOverdueLoans(30);
			if (!loans.isEmpty()) {
				StringBuilder sb = new StringBuilder(I18n.t("alert_overdue_loans_msg") + "\n\n");
				for (Object[] row : loans) {
					sb.append("• ").append(row[0]).append(" → ").append(row[1]).append(" (").append(row[2]).append(")\n");
				}
				javax.swing.JOptionPane.showMessageDialog(this.vista, sb.toString(),
					I18n.t("alert_overdue_loans_title"), javax.swing.JOptionPane.WARNING_MESSAGE);
			}
		} catch (Exception ignored) {}

		// Reading goal deadline alert
		try {
			int goal = herramienta.Config.getReadingGoal();
			if (goal > 0) {
				java.util.ArrayList<domini.Llibre> all = cLlibres.getAllLlibres();
				java.time.LocalDate today = java.time.LocalDate.now();
				int currentYear = today.getYear();
				int dayOfYear = today.getDayOfYear();
				int daysInYear = today.isLeapYear() ? 366 : 365;
				long readThisYear = all.stream()
					.filter(l -> Boolean.TRUE.equals(l.getLlegit()))
					.filter(l -> l.getDataLectura() != null && herramienta.DateUtils.parseYear(l.getDataLectura()) == currentYear)
					.count();
				int daysLeft = daysInYear - dayOfYear;
				double neededPerDay = daysLeft > 0 ? (double)(goal - readThisYear) / daysLeft : 0;
				double actualPerDay = dayOfYear > 0 ? (double) readThisYear / dayOfYear : 0;
				if (readThisYear < goal && neededPerDay > actualPerDay * 1.5 && daysLeft < 90) {
					javax.swing.JOptionPane.showMessageDialog(this.vista,
						I18n.t("alert_goal_pace_msg", goal, readThisYear, daysLeft, String.format("%.2f", neededPerDay)),
						I18n.t("alert_goal_pace_title"), javax.swing.JOptionPane.INFORMATION_MESSAGE);
				}
			}
		} catch (Exception ignored) {}
	}

	@Override
	public void actualitzarLlibre(Llibre l, boolean nuevo) {
		if (nuevo) {
			// Auto-assign the new book to the currently selected shelf (if any)
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

	private void mostrarAjudaDreceres() {
		javax.swing.JOptionPane.showMessageDialog(vista, I18n.t("dlg_shortcuts_content"),
			I18n.t("dlg_shortcuts_title"), javax.swing.JOptionPane.INFORMATION_MESSAGE);
	}
}
