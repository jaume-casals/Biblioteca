package presentacio.detalles.control;

import java.io.File;

import domini.Llibre;
import interficie.BibliotecaWriter;
import herramienta.DialogoError;
import herramienta.FieldAutoComplete;
import herramienta.I18n;
import herramienta.LlibreValidator;
import interficie.EnActualizarBBDD;
import presentacio.detalles.vista.DetallesLlibrePanel;
import presentacio.detalles.vista.LlistesDelLlibreDialog;
import presentacio.detalles.vista.TagsDelLlibreDialog;

public class DetallesLlibrePanelControl {

	private DetallesLlibrePanel vista;
	private BibliotecaWriter cLlibres;
	private EnActualizarBBDD enActualizarBBDD;
	private byte[] pendingBlob;
	private javax.swing.SwingWorker<byte[], Void> imatgeWorker;

	private static final int IMG_W = 200;

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD) {
		this(l, enActualizarBBDD, null);
	}

	public DetallesLlibrePanelControl(Llibre l, EnActualizarBBDD enActualizarBBDD, BibliotecaWriter cd) {
		this.vista = new DetallesLlibrePanel();

		this.enActualizarBBDD = enActualizarBBDD;
		cLlibres = cd != null ? cd : domini.ControladorDomini.getInstance();

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
		this.vista.getBtnEditar().addActionListener(e -> editar(l));
		this.vista.getBtnEliminar().addActionListener(e -> eliminar(l));
		this.vista.getBtnGestioLlistes().addActionListener(e ->
			new LlistesDelLlibreDialog(this.vista, l, cLlibres).setVisible(true));
		this.vista.getBtnGestioTags().addActionListener(e ->
			new TagsDelLlibreDialog(this.vista, l, cLlibres).setVisible(true));
		this.vista.getBtnHistorialPrestecs().addActionListener(e -> mostrarHistorialPrestecs(l));
		this.vista.getBtnImprimir().addActionListener(e -> imprimirFitxa(l));

		this.vista.getTextPortada().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { previewPortada(); }
		});

		FieldAutoComplete.attach(this.vista.getTextAutor(),    cLlibres.getDistinctAutorNames());
		FieldAutoComplete.attach(this.vista.getTextEditorial(), cLlibres.getDistinctValues("editorial"));
		FieldAutoComplete.attach(this.vista.getTextSerie(),     cLlibres.getDistinctValues("serie"));
		FieldAutoComplete.attach(this.vista.getTextIdioma(),    cLlibres.getDistinctValues("idioma"));

		this.vista.getTextAny().setText(l.getAny().toString());
		this.vista.getTextAutor().setText(l.getAutor().toString());
		this.vista.getTextISBN().setText(l.getISBN().toString());
		this.vista.getTextDescripcio().setText(l.getDescripcio().toString());
		this.vista.getTextNom().setText(l.getNom().toString());
		this.vista.getTextPortada().setText(l.getImatge() != null ? l.getImatge() : "");
		this.vista.getTextPreu().setText(l.getPreu().toString());
		this.vista.getTextValoracio().setText(l.getValoracio().toString());
		this.vista.getChckLlegit().setSelected(l.getLlegit());
		this.vista.getTextNotes().setText(l.getNotes());
		this.vista.getTextEditorial().setText(l.getEditorial() != null ? l.getEditorial() : "");
		this.vista.getTextSerie().setText(l.getSerie() != null ? l.getSerie() : "");
		this.vista.getTextVolum().setText(l.getVolum() > 0 ? String.valueOf(l.getVolum()) : "");
		this.vista.getTextDataCompra().setText(l.getDataCompra() != null ? l.getDataCompra() : "");
		this.vista.getTextDataLectura().setText(l.getDataLectura() != null ? l.getDataLectura() : "");
		this.vista.getTextIdioma().setText(l.getIdioma() != null ? l.getIdioma() : "");
		this.vista.getTextPaisOrigen().setText(l.getPaisOrigen() != null ? l.getPaisOrigen() : "");
		this.vista.getComboFormat().setSelectedItem(l.getFormat() != null ? l.getFormat() : "");
		this.vista.getComboEstat().setSelectedItem(l.getEstat() != null ? l.getEstat() : "");
		this.vista.getTextExemplars().setText(l.getExemplars() > 1 ? String.valueOf(l.getExemplars()) : "");
		this.vista.getTextLlenguaOriginal().setText(l.getLlenguaOriginal() != null ? l.getLlenguaOriginal() : "");
		this.vista.getChckDesitjat().setSelected(l.getDesitjat());
		this.vista.getTextPagines().setText(l.getPagines() > 0 ? String.valueOf(l.getPagines()) : "");
		this.vista.getTextPaginesLlegides().setText(l.getPaginesLlegides() > 0 ? String.valueOf(l.getPaginesLlegides()) : "");

		this.vista.setTitle(I18n.t("dlg_book_detail_title", l.getNom()));

		this.vista.getTextPaginesLlegides().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { clampPaginesLlegides(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { }
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
		} catch (NumberFormatException ignored) {}
	}

	private void eliminar(Llibre llibre) {
		int confirm = javax.swing.JOptionPane.showConfirmDialog(
			this.vista,
			I18n.t("dlg_confirm_delete_one", llibre.getNom()),
			I18n.t("dlg_confirm_delete_title"),
			javax.swing.JOptionPane.YES_NO_OPTION,
			javax.swing.JOptionPane.WARNING_MESSAGE);
		if (confirm != javax.swing.JOptionPane.YES_OPTION) return;
		try {
			cLlibres.deleteLlibre(llibre);
			enActualizarBBDD.eliminarLlibre(llibre);
			this.vista.dispose();
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
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
		if (imatgeWorker != null) imatgeWorker.cancel(true);
		imatgeWorker = new javax.swing.SwingWorker<>() {
			@Override protected byte[] doInBackground() throws Exception { return loader.call(); }
			@Override protected void done() {
				if (isCancelled()) return;
				try {
					byte[] data = get();
					if (data != null) { pendingBlob = data; carregarImatgeBlob(data); }
				} catch (Exception ignored) {
					vista.getLabelIcono().setIcon(null);
				}
			}
		};
		imatgeWorker.execute();
	}

	private void carregarImatgeBlob(byte[] data) {
		this.vista.getLabelIcono().setIcon(herramienta.UITheme.scaledIcon(data, IMG_W));
	}

	private void seleccionarImatge() {
		File f = herramienta.UITheme.chooseImageFile(this.vista);
		if (f != null) {
			this.vista.getTextPortada().setText(f.getAbsolutePath());
			carregarImatgeAsync(f.getAbsolutePath());
		}
	}

	private void editar(Llibre llibre) {
		if (I18n.t("btn_edit_java").equals(this.vista.getBtnEditar().getText())) {
			this.vista.getTextAny().setEnabled(true);
			this.vista.getTextAutor().setEnabled(true);
			this.vista.getTextISBN().setEnabled(true);
			this.vista.getTextDescripcio().setEnabled(true);
			this.vista.getTextNom().setEnabled(true);
			this.vista.getTextPortada().setEnabled(true);
			this.vista.getTextPreu().setEnabled(true);
			this.vista.getTextValoracio().setEnabled(true);
			this.vista.getTextEditorial().setEnabled(true);
			this.vista.getTextSerie().setEnabled(true);
			this.vista.getTextVolum().setEnabled(true);
			this.vista.getTextDataCompra().setEnabled(true);
			this.vista.getTextDataLectura().setEnabled(true);
			this.vista.getTextIdioma().setEnabled(true);
			this.vista.getTextPaisOrigen().setEnabled(true);
			this.vista.getComboFormat().setEnabled(true);
			this.vista.getComboEstat().setEnabled(true);
			this.vista.getTextExemplars().setEnabled(true);
			this.vista.getTextLlenguaOriginal().setEnabled(true);
			this.vista.getChckDesitjat().setEnabled(true);
			this.vista.getChckLlegit().setEnabled(true);
			this.vista.getBtnSeleccionarImatge().setEnabled(true);
			this.vista.getTextNotes().setEnabled(true);
			this.vista.getTextPagines().setEnabled(true);
			this.vista.getTextPaginesLlegides().setEnabled(true);
			this.vista.getBtnEditar().setText(I18n.t("btn_save_java"));
		} else if (this.vista.getBtnEditar().getText().equals(I18n.t("btn_save_java"))) {
			try {
				Llibre a = LlibreValidator.checkLlibre(Long.parseLong(vista.getTextISBN().getText()),
						vista.getTextNom().getText(), vista.getTextAutor().getText(),
						Integer.parseInt(vista.getTextAny().getText()), vista.getTextDescripcio().getText(),
						Double.parseDouble(vista.getTextValoracio().getText()),
						Double.parseDouble(vista.getTextPreu().getText()), vista.getChckLlegit().isSelected(),
						vista.getTextPortada().getText());
				a.setEditorial(vista.getTextEditorial().getText().trim());
				a.setSerie(vista.getTextSerie().getText().trim());
				try { a.setVolum(Integer.parseInt(vista.getTextVolum().getText().trim())); } catch (NumberFormatException ignored) {}
				a.setDataCompra(vista.getTextDataCompra().getText().trim());
				a.setDataLectura(vista.getTextDataLectura().getText().trim());
				a.setIdioma(vista.getTextIdioma().getText().trim());
				a.setPaisOrigen(vista.getTextPaisOrigen().getText().trim());
				String fmt = (String) vista.getComboFormat().getSelectedItem();
				a.setFormat(fmt != null && !fmt.isEmpty() ? fmt : null);
				String estat = (String) vista.getComboEstat().getSelectedItem();
				a.setEstat(estat != null && !estat.isEmpty() ? estat : null);
				try { a.setExemplars(Integer.parseInt(vista.getTextExemplars().getText().trim())); } catch (NumberFormatException ignored) {}
				a.setLlenguaOriginal(vista.getTextLlenguaOriginal().getText().trim());
				a.setDesitjat(vista.getChckDesitjat().isSelected());
				java.util.List<String> autors = java.util.Arrays.stream(vista.getTextAutor().getText().split(","))
					.map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
				a.setAutors(autors);
				a.setNotes(vista.getTextNotes().getText());
				try { a.setPagines(Integer.parseInt(vista.getTextPagines().getText().trim())); } catch (NumberFormatException ignored) {}
				try { a.setPaginesLlegides(Integer.parseInt(vista.getTextPaginesLlegides().getText().trim())); } catch (NumberFormatException ignored) {}
				if (a.getPagines() > 0 && a.getPaginesLlegides() > a.getPagines()) {
					a.setPaginesLlegides(a.getPagines());
					vista.getTextPaginesLlegides().setText(String.valueOf(a.getPagines()));
				}
				a.setImatgeBlob(pendingBlob);
				if (a.getISBN().equals(llibre.getISBN())) {
					cLlibres.updateLlibre(a);
				} else {
					if (cLlibres.existsLlibre(a.getISBN()))
						throw new Exception(I18n.t("dlg_isbn_exists", a.getISBN()));
					cLlibres.deleteLlibre(llibre);
					cLlibres.addLlibre(a);
				}
				enActualizarBBDD.actualitzarLlibre(a, false);
				// Only lock fields after successful save
				this.vista.getTextAny().setEnabled(false);
				this.vista.getTextAutor().setEnabled(false);
				this.vista.getTextISBN().setEnabled(false);
				this.vista.getTextDescripcio().setEnabled(false);
				this.vista.getTextNom().setEnabled(false);
				this.vista.getTextPortada().setEnabled(false);
				this.vista.getTextPreu().setEnabled(false);
				this.vista.getTextValoracio().setEnabled(false);
				this.vista.getTextEditorial().setEnabled(false);
				this.vista.getTextSerie().setEnabled(false);
				this.vista.getTextVolum().setEnabled(false);
				this.vista.getTextDataCompra().setEnabled(false);
				this.vista.getTextDataLectura().setEnabled(false);
				this.vista.getTextIdioma().setEnabled(false);
				this.vista.getTextPaisOrigen().setEnabled(false);
				this.vista.getComboFormat().setEnabled(false);
				this.vista.getComboEstat().setEnabled(false);
				this.vista.getTextExemplars().setEnabled(false);
				this.vista.getTextLlenguaOriginal().setEnabled(false);
				this.vista.getChckDesitjat().setEnabled(false);
				this.vista.getChckLlegit().setEnabled(false);
				this.vista.getBtnSeleccionarImatge().setEnabled(false);
				this.vista.getTextNotes().setEnabled(false);
				this.vista.getTextPagines().setEnabled(false);
				this.vista.getTextPaginesLlegides().setEnabled(false);
				this.vista.getBtnEditar().setText(I18n.t("btn_edit_java"));
			} catch (Exception e) {
				new DialogoError(e).showErrorMessage();
			}
		}
	}

	private void mostrarHistorialPrestecs(Llibre l) {
		java.util.List<Object[]> loans = cLlibres.getLoansForIsbn(l.getISBN());
		if (loans.isEmpty()) {
			javax.swing.JOptionPane.showMessageDialog(this.vista,
				"No hi ha préstecs registrats per a aquest llibre.",
				"Historial de préstecs", javax.swing.JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String[] cols = {"Persona", "Data préstec", "Retornat"};
		Object[][] data = new Object[loans.size()][3];
		for (int i = 0; i < loans.size(); i++) {
			data[i][0] = loans.get(i)[0];
			data[i][1] = loans.get(i)[1];
			data[i][2] = Boolean.TRUE.equals(loans.get(i)[2]) ? "Sí" : "No";
		}
		javax.swing.JTable tbl = new javax.swing.JTable(data, cols);
		tbl.setEnabled(false);
		javax.swing.JScrollPane sp = new javax.swing.JScrollPane(tbl);
		sp.setPreferredSize(new java.awt.Dimension(400, Math.min(200, loans.size() * 25 + 40)));
		javax.swing.JOptionPane.showMessageDialog(this.vista, sp,
			"Historial de préstecs — " + l.getNom(), javax.swing.JOptionPane.PLAIN_MESSAGE);
	}

	private void imprimirFitxa(Llibre l) {
		java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
		job.setJobName(l.getNom().toString());
		job.setPrintable((graphics, pageFormat, pageIndex) -> {
			if (pageIndex > 0) return java.awt.print.Printable.NO_SUCH_PAGE;
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics;
			g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
			double w = pageFormat.getImageableWidth();
			int x = 0, y = 0, lineH = 18;
			g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
			g2.drawString(l.getNom().toString(), x, y += 20);
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
				int maxChars = 120;
				if (desc.length() > maxChars) desc = desc.substring(0, maxChars) + "…";
				g2.drawString(desc, x, y += lineH);
			}
			if (l.getNotes() != null && !l.getNotes().isEmpty()) {
				y += 4;
				g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
				String notes = l.getNotes();
				int maxChars = 200;
				if (notes.length() > maxChars) notes = notes.substring(0, maxChars) + "…";
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
}
