package presentacio.detalles.control;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import domini.Llibre;
import persistencia.contract.EscritorBiblioteca;
import persistencia.contract.EscritorPrestatgeria;
import persistencia.contract.EscritorEtiqueta;
import herramienta.ui.DialegError;
import herramienta.text.FieldAutoComplete;
import herramienta.i18n.I18n;
import herramienta.text.ValidadorLlibre;
import herramienta.text.ParseHelpers;
import presentacio.listener.EnActualitzarBBDD;
import presentacio.listener.EnEliminarLlibre;
import presentacio.detalles.vista.PanellDetallsLlibre;
import presentacio.detalles.vista.DialegLlistesLlibre;
import presentacio.detalles.vista.DialegEtiquetesLlibre;

import persistencia.row.PrestecRow;
public class ControladorPanellDetallsLlibre {

	private final PanellDetallsLlibre vista;
	private final EscritorBiblioteca cLlibres;
	private final EnActualitzarBBDD enActualizarBBDD;
	private byte[] pendingBlob;
	private javax.swing.SwingWorker<byte[], Void> imageWorker;
	private javax.swing.SwingWorker<Void, Void> heavyFieldsWorker;

	private static final int IMG_W = 200;
	private static final int MAX_DESCRIPTION_CHARS = 120;
	private static final int MAX_NOTES_CHARS = 200;

	private final GestorEsborrar eliminarHandler = new GestorEsborrar();
	private final GestorHistorial historyHandler = new GestorHistorial();
	private final GestorEdicio editHandler = new GestorEdicio();

	public ControladorPanellDetallsLlibre(Llibre l, EnActualitzarBBDD enActualizarBBDD) {
		this(l, enActualizarBBDD, null);
	}

	public ControladorPanellDetallsLlibre(Llibre l, EnActualitzarBBDD enActualizarBBDD, EscritorBiblioteca cd) {
		this.vista = new PanellDetallsLlibre();

		this.vista.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) {
				if (imageWorker != null) imageWorker.cancel(true);
				if (heavyFieldsWorker != null) heavyFieldsWorker.cancel(true);
			}
		});

		this.enActualizarBBDD = enActualizarBBDD;
		cLlibres = cd != null ? cd : domini.ControladorDomini.getInstance();
				if (!l.teCampsPesatsCarregats()) {
					final Llibre book = l;
					heavyFieldsWorker = new javax.swing.SwingWorker<Void, Void>() {
						@Override protected Void doInBackground() {
							try { cLlibres.carregarHeavyFields(book); }
							catch (Exception e) {
								Logger.getLogger(ControladorPanellDetallsLlibre.class.getName())
									.log(Level.WARNING, "Ha fallat la càrrega de camps pesats per a l'ISBN " + book.obtenirISBN()
										+ "; la descripció i les notes es renderitzaran buides i qualsevol desat sobreescriurà els originals", e);
							}
							return null;
						}
						@Override protected void done() {
							if (isCancelled() || !vista.isDisplayable()) return;
							vista.obtenirTextDescripcio().setText(java.util.Objects.toString(book.obtenirDescripcio(), ""));
							vista.obtenirTextNotes().setText(book.obtenirNotes() != null ? book.obtenirNotes() : "");
						}
					};
					heavyFieldsWorker.execute();
				}

		pendingBlob = l.obtenirImatgeBlob();
		if (pendingBlob != null) {
			carregarImatgeBlob(pendingBlob);
		} else if (l.teBlob()) {
			final long isbn = l.obtenirISBN();
			iniciarImatgeWorker(() -> cLlibres.obtenirLlibreBlob(isbn));
		} else {
			carregarImatgeAsync(l.obtenirImatge());
		}

		this.vista.obtenirBtnSeleccionarImatge().addActionListener(e -> seleccionarImatge());
		this.vista.obtenirBtnEditar().addActionListener(e -> editHandler.editar(l));
		this.vista.obtenirBtnEliminar().addActionListener(e -> eliminarHandler.eliminar(l));
		this.vista.obtenirBtnGestioLlistes().addActionListener(e -> obrirLlistes(l));
		this.vista.obtenirBtnGestioTags().addActionListener(e -> obrirTags(l));
		this.vista.obtenirBtnHistorialPrestecs().addActionListener(e -> historyHandler.mostrarHistorialPrestecs(l));
		this.vista.obtenirBtnImprimir().addActionListener(e -> imprimirFitxa(l));

		this.vista.obtenirTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
		});

		new javax.swing.SwingWorker<java.util.List<java.util.List<String>>, Void>() {
			@Override protected java.util.List<java.util.List<String>> doInBackground() {
				java.util.List<java.util.List<String>> lists = new java.util.ArrayList<>();
				lists.add(cLlibres.obtenirDistinctAutorNames());
				lists.add(cLlibres.obtenirDistinctValues("editorial"));
				lists.add(cLlibres.obtenirDistinctValues("serie"));
				lists.add(cLlibres.obtenirDistinctValues("idioma"));
				return lists;
			}
			@Override protected void done() {
				try {
					java.util.List<java.util.List<String>> lists = get();
					FieldAutoComplete.attach(vista.obtenirTextAutor(), lists.get(0));
					FieldAutoComplete.attach(vista.obtenirTextEditorial(), lists.get(1));
					FieldAutoComplete.attach(vista.obtenirTextSerie(), lists.get(2));
					FieldAutoComplete.attach(vista.obtenirTextIdioma(), lists.get(3));
				} catch (Exception e) { /* l'autocompletat és best-effort; el diàleg continua funcionant sense */ }
			}
		}.execute();

		this.vista.obtenirTextAny().setText(java.util.Objects.toString(l.obtenirAny(), ""));
		this.vista.obtenirTextAutor().setText(java.util.Objects.toString(l.obtenirAutor(), ""));
		this.vista.obtenirTextISBN().setText(java.util.Objects.toString(l.obtenirISBN(), ""));
		this.vista.obtenirTextDescripcio().setText(java.util.Objects.toString(l.obtenirDescripcio(), ""));
		this.vista.obtenirTextNom().setText(java.util.Objects.toString(l.obtenirNom(), ""));
		this.vista.obtenirTextPortada().setText(l.obtenirImatge() != null ? l.obtenirImatge() : "");
		this.vista.obtenirTextPreu().setText(java.util.Objects.toString(l.obtenirPreu(), ""));
		this.vista.obtenirTextValoracio().setText(java.util.Objects.toString(l.obtenirValoracio(), ""));
		this.vista.obtenirChckLlegit().setSelected(Boolean.TRUE.equals(l.obtenirLlegit()));
		this.vista.obtenirTextNotes().setText(l.obtenirNotes() != null ? l.obtenirNotes() : "");
		this.vista.obtenirTextEditorial().setText(l.obtenirEditorial() != null ? l.obtenirEditorial() : "");
		this.vista.obtenirTextSerie().setText(l.obtenirSerie() != null ? l.obtenirSerie() : "");
		this.vista.obtenirTextVolum().setText(l.obtenirVolum() > 0 ? String.valueOf(l.obtenirVolum()) : "");
		this.vista.obtenirTextDataCompra().setText(l.obtenirDataCompra() != null ? l.obtenirDataCompra() : "");
		this.vista.obtenirTextDataLectura().setText(herramienta.text.UtilitatsData.formatejarDateForDisplay(l.obtenirDataLectura()));
		this.vista.obtenirTextIdioma().setText(l.obtenirIdioma() != null ? l.obtenirIdioma() : "");
		this.vista.obtenirTextPaisOrigen().setText(l.obtenirPaisOrigen() != null ? l.obtenirPaisOrigen() : "");
		this.vista.obtenirComboFormat().setSelectedItem(l.obtenirFormat() != null ? l.obtenirFormat() : "");
		this.vista.obtenirComboEstat().setSelectedItem(l.obtenirEstat() != null ? l.obtenirEstat() : "");
		this.vista.obtenirTextExemplars().setText(l.obtenirExemplars() > 1 ? String.valueOf(l.obtenirExemplars()) : "");
		this.vista.obtenirTextLlenguaOriginal().setText(l.obtenirLlenguaOriginal() != null ? l.obtenirLlenguaOriginal() : "");
		this.vista.obtenirChckDesitjat().setSelected(l.esDesitjat());
		this.vista.obtenirTextPagines().setText(l.obtenirPagines() > 0 ? String.valueOf(l.obtenirPagines()) : "");
		this.vista.obtenirTextPaginesLlegides().setText(l.obtenirPaginesLlegides() > 0 ? String.valueOf(l.obtenirPaginesLlegides()) : "");
		this.vista.obtenirTextNomCa().setText(l.obtenirNomCa() != null ? l.obtenirNomCa() : "");
		this.vista.obtenirTextNomEs().setText(l.obtenirNomEs() != null ? l.obtenirNomEs() : "");
		this.vista.obtenirTextNomEn().setText(l.obtenirNomEn() != null ? l.obtenirNomEn() : "");

		this.vista.setTitle(I18n.t("dlg_book_detail_title", l.obtenirDisplayNom(herramienta.config.Configuracio.obtenirLang())));

		this.vista.obtenirTextPaginesLlegides().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
		});
	}

	private void clampPaginesLlegides() {
		try {
			int llegides = Integer.parseInt(this.vista.obtenirTextPaginesLlegides().getText().trim());
			int total = Integer.parseInt(this.vista.obtenirTextPagines().getText().trim());
			if (total > 0 && llegides > total) {
				String clamped = String.valueOf(total);
				javax.swing.SwingUtilities.invokeLater(() ->
					this.vista.obtenirTextPaginesLlegides().setText(clamped));
			}
		} catch (NumberFormatException e) { /* entrada buida o no numèrica — deixa el camp de pàgina intacte */ }
	}

	private void previewPortada() {
		String path = this.vista.obtenirTextPortada().getText().trim();
		if (path.isEmpty()) { this.vista.obtenirLabelIcono().setIcon(null); pendingBlob = null; return; }
		carregarImatgeAsync(path);
	}

	private void carregarImatgeAsync(String path) {
		if (path == null || path.isBlank()) return;
		iniciarImatgeWorker(() -> java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)));
	}

	private void iniciarImatgeWorker(java.util.concurrent.Callable<byte[]> loader) {
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
					/* la càrrega de la imatge ha fallat — neteja la icona */
					vista.obtenirLabelIcono().setIcon(null);
				}
			}
		};
		imageWorker.execute();
	}

	private void carregarImatgeBlob(byte[] data) {
		javax.swing.ImageIcon icon = herramienta.ui.UITheme.scaledIcon(data, IMG_W);
		this.vista.obtenirLabelIcono().setIcon(icon != null ? icon : presentacio.util.MemoriaImatgesCoberta.NO_COVER);
	}

	private void seleccionarImatge() {
		File f = herramienta.ui.UITheme.chooseImageFile(this.vista);
		if (f != null) {
			this.vista.obtenirTextPortada().setText(f.getAbsolutePath());
			carregarImatgeAsync(f.getAbsolutePath());
		}
	}

	private void obrirLlistes(Llibre l) {
		new DialegLlistesLlibre(this.vista, l, (EscritorPrestatgeria) cLlibres).setVisible(true);
	}

	private void obrirTags(Llibre l) {
		new DialegEtiquetesLlibre(this.vista, l, (EscritorEtiqueta) cLlibres).setVisible(true);
	}

	private void imprimirFitxa(Llibre l) {
		java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
		String nom = l.obtenirNom() == null ? "" : l.obtenirNom().toString();
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
			g2.drawString(I18n.t("field_author") + ": " + l.obtenirAutor(), x, y += lineH + 4);
			g2.drawString("ISBN: " + l.obtenirISBN(), x, y += lineH);
			Integer any = l.obtenirAny();
			if (any != null && any > 0) g2.drawString(I18n.t("field_year") + ": " + any, x, y += lineH);
			if (l.obtenirEditorial() != null && !l.obtenirEditorial().isEmpty())
				g2.drawString(I18n.t("field_publisher") + ": " + l.obtenirEditorial(), x, y += lineH);
			if (l.obtenirSerie() != null && !l.obtenirSerie().isEmpty())
				g2.drawString(I18n.t("field_series") + ": " + l.obtenirSerie()
					+ (l.obtenirVolum() > 0 ? " #" + l.obtenirVolum() : ""), x, y += lineH);
			g2.drawString(I18n.t("field_rating") + ": " + l.obtenirValoracio() + "/10", x, y += lineH);
			g2.drawString(I18n.t("field_read") + ": " + (Boolean.TRUE.equals(l.obtenirLlegit()) ? I18n.t("yes_lbl") : I18n.t("no_lbl")), x, y += lineH);
			if (l.obtenirPagines() > 0) g2.drawString(I18n.t("field_pages") + ": " + l.obtenirPagines(), x, y += lineH);
			if (pendingBlob != null) {
				javax.swing.ImageIcon icon = herramienta.ui.UITheme.scaledIcon(pendingBlob, 120);
				if (icon != null) icon.paintIcon(null, g2, (int)(w - 130), 10);
			}
			if (l.obtenirDescripcio() != null && !l.obtenirDescripcio().toString().isEmpty()) {
				y += lineH + 4;
				g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 11));
				String desc = l.obtenirDescripcio().toString();
				if (desc.length() > MAX_DESCRIPTION_CHARS) desc = desc.substring(0, MAX_DESCRIPTION_CHARS) + "…";
				g2.drawString(desc, x, y += lineH);
			}
			if (l.obtenirNotes() != null && !l.obtenirNotes().isEmpty()) {
				y += 4;
				g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
				String notes = l.obtenirNotes();
				if (notes.length() > MAX_NOTES_CHARS) notes = notes.substring(0, MAX_NOTES_CHARS) + "…";
				g2.drawString(I18n.t("field_notes") + ": " + notes, x, y += lineH);
			}
			return java.awt.print.Printable.PAGE_EXISTS;
		});
		if (job.printDialog()) {
			try { job.print(); }
			catch (java.awt.print.PrinterException e) { new DialegError(e).mostrarErrorMessage(); }
		}
	}

	public PanellDetallsLlibre obtenirDetallesLlibrePanel() {
		return this.vista;
	}

	private class GestorEsborrar {
		void eliminar(Llibre llibre) {
			int confirm = javax.swing.JOptionPane.showConfirmDialog(
				vista,
				I18n.t("dlg_confirm_delete_one", llibre.obtenirNom()),
				I18n.t("dlg_confirm_delete_title"),
				javax.swing.JOptionPane.YES_NO_OPTION,
				javax.swing.JOptionPane.WARNING_MESSAGE);
			if (confirm != javax.swing.JOptionPane.YES_OPTION) return;
			EnEliminarLlibre.EsborrarEvent ev = new EnEliminarLlibre.EsborrarEvent(llibre, true);
			enActualizarBBDD.enEliminantLlibre(ev);
			if (!EnEliminarLlibre.hauriaProceed(ev)) return;
			new javax.swing.SwingWorker<Void, Void>() {
				@Override protected Void doInBackground() throws Exception {
					cLlibres.eliminarLlibre(llibre);
					return null;
				}
				@Override protected void done() {
					try {
						get();
						enActualizarBBDD.enEliminarLlibre(llibre);
						vista.dispose();
					} catch (Exception e) {
						new DialegError(e).mostrarErrorMessage();
					}
				}
			}.execute();
		}
	}

	private class GestorHistorial {
		void mostrarHistorialPrestecs(Llibre l) {
			new javax.swing.SwingWorker<java.util.List<persistencia.row.PrestecRow>, Void>() {
				@Override protected java.util.List<persistencia.row.PrestecRow> doInBackground() {
					return cLlibres.obtenirLoansForIsbn(l.obtenirISBN());
				}
				@Override protected void done() {
					if (isCancelled()) return;
					try {
						java.util.List<persistencia.row.PrestecRow> loans = get();
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
							I18n.t("dlg_historial_title") + " — " + l.obtenirDisplayNom(herramienta.config.Configuracio.obtenirLang()), javax.swing.JOptionPane.PLAIN_MESSAGE);
					} catch (Exception e) {
						new DialegError(e).mostrarErrorMessage();
					}
				}
			}.execute();
		}
	}

	private class GestorEdicio {
		void editar(Llibre llibre) {
			if (I18n.t("btn_edit_java").equals(vista.obtenirBtnEditar().getText())) {
				posarEditMode(true);
				vista.obtenirBtnEditar().setText(I18n.t("btn_save_java"));
			} else if (vista.obtenirBtnEditar().getText().equals(I18n.t("btn_save_java"))) {
				try {
					// La validació per camps s'executa ABANS de tocar el model
					// perquè una sola entrada incorrecta no deixi un Llibre
					// mig mutat en memòria. Qualsevol entrada no numèrica en
					// un camp numèric es recull i es mostra a l'usuari; el
					// desat s'aborta.
					java.util.List<String> errors = new java.util.ArrayList<>();
					Integer any = ParseHelpers.analitzarIntOrNull(vista.obtenirTextAny().getText());
					Double valoracio = ParseHelpers.analitzarDoubleOrNull(vista.obtenirTextValoracio().getText());
					Double preu = ParseHelpers.analitzarDoubleOrNull(vista.obtenirTextPreu().getText());
					int volum = ParseHelpers.parseInt(vista.obtenirTextVolum().getText(), 0, "field_volume", errors);
					int exemplars = ParseHelpers.parseInt(vista.obtenirTextExemplars().getText(), 1, "field_exemplars", errors);
					int pagines = ParseHelpers.parseInt(vista.obtenirTextPagines().getText(), 0, "field_pages", errors);
					int paginesLlegides = ParseHelpers.parseInt(vista.obtenirTextPaginesLlegides().getText(), 0, "field_pages_read", errors);

					if (!errors.isEmpty()) {
						new DialegError(new IllegalArgumentException(String.join("\n", errors))).mostrarErrorMessage();
						return;
					}

					Llibre a = ValidadorLlibre.comprovarLlibre(llibre.obtenirISBN(),
							vista.obtenirTextNom().getText(), vista.obtenirTextAutor().getText(),
							any, vista.obtenirTextDescripcio().getText(),
							valoracio, preu, vista.obtenirChckLlegit().isSelected(),
							vista.obtenirTextPortada().getText());
					a.posarEditorial(vista.obtenirTextEditorial().getText().trim());
					a.posarSerie(vista.obtenirTextSerie().getText().trim());
					a.posarVolum(volum);
					a.posarDataCompra(vista.obtenirTextDataCompra().getText().trim());
					a.posarDataLectura(vista.obtenirTextDataLectura().getText().trim());
					a.posarIdioma(vista.obtenirTextIdioma().getText().trim());
					a.posarPaisOrigen(vista.obtenirTextPaisOrigen().getText().trim());
					String fmt = (String) vista.obtenirComboFormat().getSelectedItem();
					a.posarFormat(fmt != null && !fmt.isEmpty() ? fmt : null);
					String estat = (String) vista.obtenirComboEstat().getSelectedItem();
					a.posarEstat(estat != null && !estat.isEmpty() ? estat : null);
					a.posarExemplars(exemplars);
					a.posarLlenguaOriginal(vista.obtenirTextLlenguaOriginal().getText().trim());
					String nc = vista.obtenirTextNomCa().getText().trim(); a.posarNomCa(nc.isEmpty() ? null : nc);
					String nse = vista.obtenirTextNomEs().getText().trim(); a.posarNomEs(nse.isEmpty() ? null : nse);
					String nen = vista.obtenirTextNomEn().getText().trim(); a.posarNomEn(nen.isEmpty() ? null : nen);
					a.posarDesitjat(vista.obtenirChckDesitjat().isSelected());
					java.util.List<String> autors = java.util.Arrays.stream(vista.obtenirTextAutor().getText().split(","))
						.map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
					a.posarAutors(autors);
					a.posarNotes(vista.obtenirTextNotes().getText());
					a.posarPagines(pagines);
					a.posarPaginesLlegides(paginesLlegides);
					if (a.obtenirPagines() > 0 && a.obtenirPaginesLlegides() > a.obtenirPagines()) {
						a.posarPaginesLlegides(a.obtenirPagines());
						vista.obtenirTextPaginesLlegides().setText(String.valueOf(a.obtenirPagines()));
					}
				a.posarImatgeBlob(pendingBlob);
				ValidadorLlibre.validarExtrasAll(a.obtenirEditorial(), a.obtenirSerie(), a.obtenirIdioma(), a.obtenirFormat(), a.obtenirPaisOrigen(), a.obtenirEstat());
				// El camp ISBN està permanentment desactivat (veure
				// setEditMode), de manera que a.getISBN() sempre és
				// igual a llibre.getISBN(). L'antiga branca "ISBN
				// canviat" — que eliminava i tornava a afegir el llibre
				// per satisfer la restricció de clau única a la taula
				// principal — és inabastable i s'ha eliminat.
				final Llibre toSave = a;
				vista.obtenirBtnEditar().setEnabled(false);
				new javax.swing.SwingWorker<Void, Void>() {
					@Override protected Void doInBackground() throws Exception {
						cLlibres.actualitzarLlibre(toSave);
						return null;
					}
					@Override protected void done() {
						vista.obtenirBtnEditar().setEnabled(true);
						try {
							get();
							enActualizarBBDD.enActualitzarLlibre(toSave, false);
							posarEditMode(false);
							vista.obtenirBtnEditar().setText(I18n.t("btn_edit_java"));
							vista.setTitle(I18n.t("dlg_book_detail_title", toSave.obtenirDisplayNom(herramienta.config.Configuracio.obtenirLang())));
						} catch (Exception e) {
							new DialegError(e).mostrarErrorMessage();
						}
					}
				}.execute();
			} catch (Exception e) {
				new DialegError(e).mostrarErrorMessage();
			}
		}
	}
	}

	private void posarEditMode(boolean enabled) {
		for (javax.swing.JComponent c : vista.obtenirEditableInputs()) c.setEnabled(enabled);
		vista.obtenirTextISBN().setEnabled(false);
		vista.obtenirBtnSeleccionarImatge().setEnabled(enabled);
	}
}
