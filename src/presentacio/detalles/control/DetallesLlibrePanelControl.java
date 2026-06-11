package presentacio.detalles.control;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import domini.Llibre;
import interficie.BibliotecaWriter;
import herramienta.DialogoError;
import herramienta.FieldAutoComplete;
import herramienta.I18n;
import herramienta.LlibreValidator;
import presentacio.listener.EnActualizarBBDD;
import presentacio.detalles.vista.DetallesLlibrePanel;
import presentacio.detalles.vista.LlistesDelLlibreDialog;
import presentacio.detalles.vista.TagsDelLlibreDialog;

public class DetallesLlibrePanelControl {

	private final DetallesLlibrePanel vista;
	private final BibliotecaWriter cLlibres;
	private final EnActualizarBBDD enActualizarBBDD;
	private byte[] pendingBlob;
	private javax.swing.SwingWorker<byte[], Void> imageWorker;
	private static final java.util.concurrent.ExecutorService IMAGE_EXECUTOR =
		java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "image-loader");
			t.setDaemon(true);
			return t;
		});
	private static final java.util.concurrent.atomic.AtomicBoolean SHUTDOWN_HOOK_REGISTERED =
		new java.util.concurrent.atomic.AtomicBoolean(false);

	private static Integer parseIntOrNull(String s) {
		if (s == null) return null;
		String t = s.trim();
		if (t.isEmpty()) return null;
		try { return Integer.parseInt(t); } catch (NumberFormatException e) { return null; }
	}

	private static Double parseDoubleOrNull(String s) {
		if (s == null) return null;
		String t = s.trim();
		if (t.isEmpty()) return null;
		try { return Double.parseDouble(t); } catch (NumberFormatException e) { return null; }
	}

	private static final int IMG_W = 200;
	private static final int MAX_DESCRIPTION_CHARS = 120;
	private static final int MAX_NOTES_CHARS = 200;

	private final DeleteHandler deleteHandler = new DeleteHandler();
	private final HistoryHandler historyHandler = new HistoryHandler();
	private final EditHandler editHandler = new EditHandler();

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD) {
		this(l, enActualizarBBDD, null);
	}

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD, BibliotecaWriter cd) {
		this.vista = new DetallesLlibrePanel();
		registerExecutorShutdownHook();

		this.vista.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) {
				if (imageWorker != null) imageWorker.cancel(true);
			}
		});

		this.enActualizarBBDD = enActualizarBBDD;
		cLlibres = cd != null ? cd : domini.ControladorDomini.getInstance();
				if (l != null && !l.isHeavyFieldsLoaded()) {
					final Llibre book = l;
					new javax.swing.SwingWorker<Void, Void>() {
						@Override protected Void doInBackground() {
							try { cLlibres.loadHeavyFields(book); }
							catch (Exception e) {
								Logger.getLogger(DetallesLlibrePanelControl.class.getName())
									.log(Level.WARNING, "Heavy-field load failed for ISBN " + book.getISBN()
										+ "; descripcio/notes will render empty and any save will overwrite the originals", e);
							}
							return null;
						}
						@Override protected void done() {
							vista.getTextDescripcio().setText(java.util.Objects.toString(book.getDescripcio(), ""));
							vista.getTextNotes().setText(book.getNotes() != null ? book.getNotes() : "");
						}
					}.execute();
				}

		pendingBlob = l.getImatgeBlob();
		if (pendingBlob != null) {
			carregarImatgeBlob(pendingBlob);
		} else if (l.hasBlob()) {
			final long isbn = l.getISBN();
			startImatgeWorker(() -> cLlibres.getLlibreBlob(isbn));
		} else {
			carregarImatgeAsync(l.getImatge());
		}

		this.vista.getBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.getBtnEditar().addActionListener(e -> editHandler.editar(l));
		this.vista.getBtnEliminar().addActionListener(e -> deleteHandler.eliminar(l));
		this.vista.getBtnGestioLlistes().addActionListener(e -> obrirLlistes(l));
		this.vista.getBtnGestioTags().addActionListener(e -> obrirTags(l));
		this.vista.getBtnHistorialPrestecs().addActionListener(e -> historyHandler.mostrarHistorialPrestecs(l));
		this.vista.getBtnImprimir().addActionListener(e -> imprimirFitxa(l));

		this.vista.getTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
		});

		new javax.swing.SwingWorker<java.util.List<java.util.List<String>>, Void>() {
			@Override protected java.util.List<java.util.List<String>> doInBackground() {
				java.util.List<java.util.List<String>> lists = new java.util.ArrayList<>();
				lists.add(cLlibres.getDistinctAutorNames());
				lists.add(cLlibres.getDistinctValues("editorial"));
				lists.add(cLlibres.getDistinctValues("serie"));
				lists.add(cLlibres.getDistinctValues("idioma"));
				return lists;
			}
			@Override protected void done() {
				try {
					java.util.List<java.util.List<String>> lists = get();
					FieldAutoComplete.attach(vista.getTextAutor(), lists.get(0));
					FieldAutoComplete.attach(vista.getTextEditorial(), lists.get(1));
					FieldAutoComplete.attach(vista.getTextSerie(), lists.get(2));
					FieldAutoComplete.attach(vista.getTextIdioma(), lists.get(3));
				} catch (Exception e) { /* autocomplete attach is best-effort; dialog still works without it */ }
			}
		}.execute();

		this.vista.getTextAny().setText(java.util.Objects.toString(l.getAny(), ""));
		this.vista.getTextAutor().setText(java.util.Objects.toString(l.getAutor(), ""));
		this.vista.getTextISBN().setText(java.util.Objects.toString(l.getISBN(), ""));
		this.vista.getTextDescripcio().setText(java.util.Objects.toString(l.getDescripcio(), ""));
		this.vista.getTextNom().setText(java.util.Objects.toString(l.getNom(), ""));
		this.vista.getTextPortada().setText(l.getImatge() != null ? l.getImatge() : "");
		this.vista.getTextPreu().setText(java.util.Objects.toString(l.getPreu(), ""));
		this.vista.getTextValoracio().setText(java.util.Objects.toString(l.getValoracio(), ""));
		this.vista.getChckLlegit().setSelected(Boolean.TRUE.equals(l.getLlegit()));
		this.vista.getTextNotes().setText(l.getNotes() != null ? l.getNotes() : "");
		this.vista.getTextEditorial().setText(l.getEditorial() != null ? l.getEditorial() : "");
		this.vista.getTextSerie().setText(l.getSerie() != null ? l.getSerie() : "");
		this.vista.getTextVolum().setText(l.getVolum() > 0 ? String.valueOf(l.getVolum()) : "");
		this.vista.getTextDataCompra().setText(l.getDataCompra() != null ? l.getDataCompra() : "");
		this.vista.getTextDataLectura().setText(herramienta.DateUtils.formatDateForDisplay(l.getDataLectura()));
		this.vista.getTextIdioma().setText(l.getIdioma() != null ? l.getIdioma() : "");
		this.vista.getTextPaisOrigen().setText(l.getPaisOrigen() != null ? l.getPaisOrigen() : "");
		this.vista.getComboFormat().setSelectedItem(l.getFormat() != null ? l.getFormat() : "");
		this.vista.getComboEstat().setSelectedItem(l.getEstat() != null ? l.getEstat() : "");
		this.vista.getTextExemplars().setText(l.getExemplars() > 1 ? String.valueOf(l.getExemplars()) : "");
		this.vista.getTextLlenguaOriginal().setText(l.getLlenguaOriginal() != null ? l.getLlenguaOriginal() : "");
		this.vista.getChckDesitjat().setSelected(l.isDesitjat());
		this.vista.getTextPagines().setText(l.getPagines() > 0 ? String.valueOf(l.getPagines()) : "");
		this.vista.getTextPaginesLlegides().setText(l.getPaginesLlegides() > 0 ? String.valueOf(l.getPaginesLlegides()) : "");
		this.vista.getTextNomCa().setText(l.getNomCa() != null ? l.getNomCa() : "");
		this.vista.getTextNomEs().setText(l.getNomEs() != null ? l.getNomEs() : "");
		this.vista.getTextNomEn().setText(l.getNomEn() != null ? l.getNomEn() : "");

		this.vista.setTitle(I18n.t("dlg_book_detail_title", l.getDisplayNom(herramienta.Config.getLang())));

		this.vista.getTextPaginesLlegides().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
		});
	}

	private void clampPaginesLlegides() {
		try {
			int llegides = Integer.parseInt(this.vista.getTextPaginesLlegides().getText().trim());
			int total = Integer.parseInt(this.vista.getTextPagines().getText().trim());
			if (total > 0 && llegides > total) {
				String clamped = String.valueOf(total);
				javax.swing.SwingUtilities.invokeLater(() ->
					this.vista.getTextPaginesLlegides().setText(clamped));
			}
		} catch (NumberFormatException e) { /* empty or non-numeric input — leave pagina field untouched */ }
	}

	private void previewPortada() {
		String path = this.vista.getTextPortada().getText().trim();
		if (path.isEmpty()) { this.vista.getLabelIcono().setIcon(null); pendingBlob = null; return; }
		carregarImatgeAsync(path);
	}

	private void carregarImatgeAsync(String path) {
		if (path == null || path.isBlank()) return;
		startImatgeWorker(() -> java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)));
	}

	private void startImatgeWorker(java.util.concurrent.Callable<byte[]> loader) {
		if (imageWorker != null) imageWorker.cancel(true);
		imageWorker = new javax.swing.SwingWorker<byte[], Void>() {
			@Override protected byte[] doInBackground() throws Exception {
				return loader.call();
			}
			@Override protected void done() {
				if (isCancelled()) return;
				try {
					byte[] data = get();
					if (data != null) {
						pendingBlob = data;
						carregarImatgeBlob(data);
					}
				} catch (Exception e) {
					/* image load failed — clear the icon */
					vista.getLabelIcono().setIcon(null);
				}
			}
		};
		imageWorker.execute();
	}

	private void carregarImatgeBlob(byte[] data) {
		javax.swing.ImageIcon icon = herramienta.UITheme.scaledIcon(data, IMG_W);
		this.vista.getLabelIcono().setIcon(icon != null ? icon : presentacio.CoverImageCache.NO_COVER);
	}

	public static void shutdownImageExecutor() {
		IMAGE_EXECUTOR.shutdownNow();
	}

	private static void registerExecutorShutdownHook() {
		if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
			main.ShutdownHooks.register(DetallesLlibrePanelControl::shutdownImageExecutor);
		}
	}

	private void seleccionarImatge() {
		File f = herramienta.UITheme.chooseImageFile(this.vista);
		if (f != null) {
			this.vista.getTextPortada().setText(f.getAbsolutePath());
			carregarImatgeAsync(f.getAbsolutePath());
		}
	}

	private void obrirLlistes(Llibre l) {
		new LlistesDelLlibreDialog(this.vista, l, cLlibres).setVisible(true);
	}

	private void obrirTags(Llibre l) {
		new TagsDelLlibreDialog(this.vista, l, cLlibres).setVisible(true);
	}

	private void imprimirFitxa(Llibre l) {
		java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
		String nom = l.getNom() == null ? "" : l.getNom().toString();
		job.setJobName(nom);
		job.setPrintable((graphics, pageFormat, pageIndex) -> {
			if (pageIndex > 0) return java.awt.print.Printable.NO_SUCH_PAGE;
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics;
			g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
			double w = pageFormat.getImageableWidth();
			int x = 0, y = 0, lineH = 18;
			g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
			g2.drawString(nom, x, y += 20);
			g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
			g2.drawString(I18n.t("field_author") + ": " + l.getAutor(), x, y += lineH + 4);
			g2.drawString("ISBN: " + l.getISBN(), x, y += lineH);
			if (l.getAny() > 0) g2.drawString(I18n.t("field_year") + ": " + l.getAny(), x, y += lineH);
			if (l.getEditorial() != null && !l.getEditorial().isEmpty())
				g2.drawString(I18n.t("field_publisher") + ": " + l.getEditorial(), x, y += lineH);
			if (l.getSerie() != null && !l.getSerie().isEmpty())
				g2.drawString(I18n.t("field_series") + ": " + l.getSerie()
					+ (l.getVolum() > 0 ? " #" + l.getVolum() : ""), x, y += lineH);
			g2.drawString(I18n.t("field_rating") + ": " + l.getValoracio() + "/10", x, y += lineH);
			g2.drawString(I18n.t("field_read") + ": " + (l.getLlegit() ? I18n.t("yes_lbl") : I18n.t("no_lbl")), x, y += lineH);
			if (l.getPagines() > 0) g2.drawString(I18n.t("field_pages") + ": " + l.getPagines(), x, y += lineH);
			if (pendingBlob != null) {
				javax.swing.ImageIcon icon = herramienta.UITheme.scaledIcon(pendingBlob, 120);
				if (icon != null) icon.paintIcon(null, g2, (int)(w - 130), 10);
			}
			if (l.getDescripcio() != null && !l.getDescripcio().toString().isEmpty()) {
				y += lineH + 4;
				g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 11));
				String desc = l.getDescripcio().toString();
				if (desc.length() > MAX_DESCRIPTION_CHARS) desc = desc.substring(0, MAX_DESCRIPTION_CHARS) + "…";
				g2.drawString(desc, x, y += lineH);
			}
			if (l.getNotes() != null && !l.getNotes().isEmpty()) {
				y += 4;
				g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
				String notes = l.getNotes();
				if (notes.length() > MAX_NOTES_CHARS) notes = notes.substring(0, MAX_NOTES_CHARS) + "…";
				g2.drawString(I18n.t("field_notes") + ": " + notes, x, y += lineH);
			}
			return java.awt.print.Printable.PAGE_EXISTS;
		});
		if (job.printDialog()) {
			try { job.print(); }
			catch (java.awt.print.PrinterException e) { new DialogoError(e).showErrorMessage(); }
		}
	}

	public DetallesLlibrePanel getDetallesLlibrePanel() {
		return this.vista;
	}

	private class DeleteHandler {
		void eliminar(Llibre llibre) {
			int confirm = javax.swing.JOptionPane.showConfirmDialog(
				vista,
				I18n.t("dlg_confirm_delete_one", llibre.getNom()),
				I18n.t("dlg_confirm_delete_title"),
				javax.swing.JOptionPane.YES_NO_OPTION,
				javax.swing.JOptionPane.WARNING_MESSAGE);
			if (confirm != javax.swing.JOptionPane.YES_OPTION) return;
			try {
				cLlibres.deleteLlibre(llibre);
				enActualizarBBDD.onBookDeleted(llibre);
				vista.dispose();
			} catch (Exception e) {
				new DialogoError(e).showErrorMessage();
			}
		}
	}

	private class HistoryHandler {
		void mostrarHistorialPrestecs(Llibre l) {
			java.util.List<persistencia.PrestecRow> loans = cLlibres.getLoansForIsbn(l.getISBN());
			if (loans.isEmpty()) {
				javax.swing.JOptionPane.showMessageDialog(vista,
					I18n.t("dlg_no_prestecs_msg"),
					I18n.t("dlg_historial_title"), javax.swing.JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			String[] cols = {I18n.t("col_persona"), I18n.t("col_data_prestec"), I18n.t("col_retornat")};
			Object[][] data = new Object[loans.size()][3];
			for (int i = 0; i < loans.size(); i++) {
				data[i][0] = loans.get(i).nomPersona();
				data[i][1] = loans.get(i).dataPrestec() != null ? loans.get(i).dataPrestec().toString() : "";
				data[i][2] = loans.get(i).retornat() ? I18n.t("yes_lbl") : I18n.t("no_lbl");
			}
			javax.swing.JTable tbl = new javax.swing.JTable(data, cols);
			tbl.setEnabled(false);
			javax.swing.JScrollPane sp = new javax.swing.JScrollPane(tbl);
			sp.setPreferredSize(new java.awt.Dimension(400, Math.min(200, loans.size() * 25 + 40)));
			javax.swing.JOptionPane.showMessageDialog(vista, sp,
				I18n.t("dlg_historial_title") + " — " + l.getDisplayNom(herramienta.Config.getLang()), javax.swing.JOptionPane.PLAIN_MESSAGE);
		}
	}

	private class EditHandler {
		void editar(Llibre llibre) {
			if (I18n.t("btn_edit_java").equals(vista.getBtnEditar().getText())) {
				setEditMode(true);
				vista.getBtnEditar().setText(I18n.t("btn_save_java"));
			} else if (vista.getBtnEditar().getText().equals(I18n.t("btn_save_java"))) {
				try {
					Llibre a = LlibreValidator.checkLlibre(Long.parseLong(vista.getTextISBN().getText()),
							vista.getTextNom().getText(), vista.getTextAutor().getText(),
							parseIntOrNull(vista.getTextAny().getText()),
							vista.getTextDescripcio().getText(),
							parseDoubleOrNull(vista.getTextValoracio().getText()),
							parseDoubleOrNull(vista.getTextPreu().getText()),
							vista.getChckLlegit().isSelected(),
							vista.getTextPortada().getText());
					a.setEditorial(vista.getTextEditorial().getText().trim());
					a.setSerie(vista.getTextSerie().getText().trim());
					try { a.setVolum(Integer.parseInt(vista.getTextVolum().getText().trim())); } catch (NumberFormatException e) { /* empty or non-numeric volum — leave default 0 */ }
					a.setDataCompra(vista.getTextDataCompra().getText().trim());
					a.setDataLectura(vista.getTextDataLectura().getText().trim());
					a.setIdioma(vista.getTextIdioma().getText().trim());
					a.setPaisOrigen(vista.getTextPaisOrigen().getText().trim());
					String fmt = (String) vista.getComboFormat().getSelectedItem();
					a.setFormat(fmt != null && !fmt.isEmpty() ? fmt : null);
					String estat = (String) vista.getComboEstat().getSelectedItem();
					a.setEstat(estat != null && !estat.isEmpty() ? estat : null);
					try { a.setExemplars(Integer.parseInt(vista.getTextExemplars().getText().trim())); } catch (NumberFormatException e) { /* empty or non-numeric exemplars — leave default 1 */ }
					a.setLlenguaOriginal(vista.getTextLlenguaOriginal().getText().trim());
					String nc = vista.getTextNomCa().getText().trim(); a.setNomCa(nc.isEmpty() ? null : nc);
					String nse = vista.getTextNomEs().getText().trim(); a.setNomEs(nse.isEmpty() ? null : nse);
					String nen = vista.getTextNomEn().getText().trim(); a.setNomEn(nen.isEmpty() ? null : nen);
					a.setDesitjat(vista.getChckDesitjat().isSelected());
					java.util.List<String> autors = java.util.Arrays.stream(vista.getTextAutor().getText().split(","))
						.map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
					a.setAutors(autors);
					a.setNotes(vista.getTextNotes().getText());
				try { a.setPagines(Integer.parseInt(vista.getTextPagines().getText().trim())); } catch (NumberFormatException e) { /* empty or non-numeric pagines — leave default 0 */ }
				try { a.setPaginesLlegides(Integer.parseInt(vista.getTextPaginesLlegides().getText().trim())); } catch (NumberFormatException e) { /* empty or non-numeric paginesLlegides — leave default 0 */ }
					if (a.getPagines() > 0 && a.getPaginesLlegides() > a.getPagines()) {
						a.setPaginesLlegides(a.getPagines());
						vista.getTextPaginesLlegides().setText(String.valueOf(a.getPagines()));
					}
					a.setImatgeBlob(pendingBlob);
					LlibreValidator.validateExtrasAll(a.getEditorial(), a.getSerie(), a.getIdioma(), a.getFormat(), a.getPaisOrigen(), a.getEstat());
					if (a.getISBN().equals(llibre.getISBN())) {
						cLlibres.updateLlibre(a);
					} else {
						if (cLlibres.existsLlibre(a.getISBN()))
							throw new Exception(I18n.t("dlg_isbn_exists", a.getISBN()));
						cLlibres.deleteLlibre(llibre);
						cLlibres.addLlibre(a);
					}
					enActualizarBBDD.onBookUpdated(a, false);
					setEditMode(false);
					vista.getBtnEditar().setText(I18n.t("btn_edit_java"));
					vista.setTitle(I18n.t("dlg_book_detail_title", a.getDisplayNom(herramienta.Config.getLang())));
				} catch (Exception e) {
					new DialogoError(e).showErrorMessage();
				}
			}
		}
	}

	private void setEditMode(boolean enabled) {
		vista.getTextAny().setEnabled(enabled);
		vista.getTextAutor().setEnabled(enabled);
		vista.getTextISBN().setEnabled(false);
		vista.getTextDescripcio().setEnabled(enabled);
		vista.getTextNom().setEnabled(enabled);
		vista.getTextPortada().setEnabled(enabled);
		vista.getTextPreu().setEnabled(enabled);
		vista.getTextValoracio().setEnabled(enabled);
		vista.getTextEditorial().setEnabled(enabled);
		vista.getTextSerie().setEnabled(enabled);
		vista.getTextVolum().setEnabled(enabled);
		vista.getTextDataCompra().setEnabled(enabled);
		vista.getTextDataLectura().setEnabled(enabled);
		vista.getTextIdioma().setEnabled(enabled);
		vista.getTextPaisOrigen().setEnabled(enabled);
		vista.getComboFormat().setEnabled(enabled);
		vista.getComboEstat().setEnabled(enabled);
		vista.getTextExemplars().setEnabled(enabled);
		vista.getTextLlenguaOriginal().setEnabled(enabled);
		vista.getChckDesitjat().setEnabled(enabled);
		vista.getChckLlegit().setEnabled(enabled);
		vista.getBtnSeleccionarImatge().setEnabled(enabled);
		vista.getTextNotes().setEnabled(enabled);
		vista.getTextPagines().setEnabled(enabled);
		vista.getTextPaginesLlegides().setEnabled(enabled);
	}
}
