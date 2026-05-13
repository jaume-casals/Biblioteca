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
import herramienta.I18n;
import interficie.EnActualizarBBDD;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class MainFrameControl implements EnActualizarBBDD {

	private ControladorDomini cLlibres;
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
				herramienta.UITheme.setDark(!herramienta.UITheme.isDark);
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

	public static MainFrameControl getInstance(MainFramePanel vista) {
		if (instance == null && vista != null) instance = new MainFrameControl(vista);
		return instance;
	}

	public static MainFrameControl getInstance() {
		return instance;
	}

	private void obrirNouLlibreDialeg() {
		GuardarLlibresDialogoControl ctrl = new GuardarLlibresDialogoControl(new GuardarLlibresDialogo(), this);
		ctrl.getVista().setLocationRelativeTo(this.vista);
		ctrl.getVista().setVisible(true);
	}

	protected ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Long ISBN,
			Integer iniciAny, Integer fiAny,
			Double valoracioMin, Double valoracioMax,
			Double preuMin, Double preuMax, Boolean llegit) {
		return cLlibres.aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny,
			valoracioMin, valoracioMax, preuMin, preuMax, llegit, null);
	}

	protected ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Long ISBN,
			Integer iniciAny, Integer fiAny,
			Double valoracioMin, Double valoracioMax,
			Double preuMin, Double preuMax, Boolean llegit, Integer tagId) {
		return cLlibres.aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny,
			valoracioMin, valoracioMax, preuMin, preuMax, llegit, tagId);
	}

	protected ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Long ISBN,
			Integer iniciAny, Integer fiAny,
			Double valoracioMin, Double valoracioMax,
			Double preuMin, Double preuMax, Boolean llegit, Integer tagId,
			String editorial, String serie, String format, String idioma) {
		return cLlibres.aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny,
			valoracioMin, valoracioMax, preuMin, preuMax, llegit, tagId, editorial, serie, format, idioma);
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
		if (b) {
			javax.swing.SwingUtilities.invokeLater(this::mostrarAlertes);
		}
	}

	private void mostrarAlertes() {
		// Overdue loan alert: retornat=false and data_prestec > 30 days ago
		try {
			java.util.List<Object[]> loans = ControladorDomini.getInstance().getAllOverdueLoans(30);
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
				java.util.ArrayList<domini.Llibre> all = ControladorDomini.getInstance().getAllLlibres();
				int currentYear = java.time.LocalDate.now().getYear();
				int dayOfYear = java.time.LocalDate.now().getDayOfYear();
				long readThisYear = all.stream()
					.filter(l -> Boolean.TRUE.equals(l.getLlegit()))
					.filter(l -> {
						if (l.getDataLectura() != null && l.getDataLectura().length() >= 4) {
							try { return Integer.parseInt(l.getDataLectura().substring(0, 4)) == currentYear; }
							catch (Exception e2) {}
						}
						return false;
					}).count();
				int daysLeft = 365 - dayOfYear;
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
		String msg =
			"Ctrl+N          Nou llibre\n" +
			"Ctrl+F          Cerca\n" +
			"Ctrl+E          Editar llibre seleccionat\n" +
			"Ctrl+A          Seleccionar tots\n" +
			"Enter           Obrir detalls\n" +
			"Delete          Eliminar seleccionat\n" +
			"Ctrl+Shift+D    Alternar tema fosc/clar\n" +
			"Ctrl++/-        Zoom galeria\n" +
			"Ctrl+Roda       Zoom galeria\n" +
			"Fletxes         Navegar galeria\n" +
			"Ctrl+Clic autor Filtrar per autor\n" +
			"Ctrl+Z          Desfer última eliminació\n" +
			"F1 / Ctrl+?     Aquesta ajuda";
		javax.swing.JOptionPane.showMessageDialog(vista, msg, "Dreceres de teclat", javax.swing.JOptionPane.INFORMATION_MESSAGE);
	}
}
