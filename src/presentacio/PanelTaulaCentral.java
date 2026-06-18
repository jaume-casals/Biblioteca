package presentacio;



import presentacio.UIComponents;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;

import herramienta.I18n;
import herramienta.UITheme;

public class PanelTaulaCentral extends JPanel {

	private static final String CARD_TAULA = "TAULA";
	private static final String CARD_GALERIA = "GALERIA";

	private JTable taulaLlibres;
	private JScrollPane scrollPaneJTable;
	private JPanel paginationPanel;
	private JButton btnPaginaAnterior;
	private JButton btnPaginaSeguent;
	private JLabel lblPagina;
	private JPanel contentCards;
	private CardLayout cardLayout;
	private PanelGaleriaCobertes galeria;
	private boolean galeriaMode = false;

	public PanelTaulaCentral() {
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.palette().bgMain());

		galeria = new PanelGaleriaCobertes();
		buildTable();

		cardLayout = new CardLayout();
		contentCards = new JPanel(cardLayout);
		contentCards.setBackground(UITheme.palette().bgMain());
		contentCards.add(scrollPaneJTable, CARD_TAULA);
		contentCards.add(galeria, CARD_GALERIA);

		buildPagination();

		add(contentCards, BorderLayout.CENTER);
		add(paginationPanel, BorderLayout.SOUTH);
	}

	private void buildTable() {
		scrollPaneJTable = new JScrollPane();
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		scrollPaneJTable.getViewport().setBackground(UITheme.palette().bgPanel());
		scrollPaneJTable.getVerticalScrollBar().setUnitIncrement(16);

		taulaLlibres = new JTable() {
			// Etiqueta caché reutilitzada pel renderer de capçalera
			// (assignació única) — el renderer per defecte crea un JLabel
			// nou per cel·la per render, cosa que es malgasta en taules
			// de més de 1000 files.
			private final JLabel headerLabel = new JLabel();
			// FontMetrics en caché — recalcular-lo a cada crida a
			// getToolTipText era el cost dominant per píxel. Es
			// refresca de manera mandrosa quan canvia la font
			// (table.setFont dispara un canvi de propietat).
			private java.awt.FontMetrics cachedFm;
			private java.awt.Font cachedFont;
			private java.awt.FontMetrics fm() {
				java.awt.Font f = getFont();
				if (cachedFm == null || f != cachedFont) {
					cachedFm = getFontMetrics(f);
					cachedFont = f;
				}
				return cachedFm;
			}
			@Override
			public String getToolTipText(java.awt.event.MouseEvent e) {
				int row = rowAtPoint(e.getPoint());
				int col = columnAtPoint(e.getPoint());
				if (row < 0 || col < 0) return null;
				Object val = getValueAt(row, col);
				if (val == null) return null;
				String text = val.toString();
				if (text.isBlank()) return null;
				java.awt.Rectangle r = getCellRect(row, col, false);
				return fm().stringWidth(text) > r.width ? text : null;
			}
		};
		taulaLlibres.setDefaultEditor(Object.class, null);
		taulaLlibres.setAutoCreateRowSorter(true);
		taulaLlibres.getTableHeader().setReorderingAllowed(false);
		taulaLlibres.setBackground(UITheme.palette().bgPanel());
		taulaLlibres.setSelectionBackground(UITheme.palette().accent());
		taulaLlibres.setSelectionForeground(Color.WHITE);
		taulaLlibres.setGridColor(UITheme.palette().tableGrid());
		taulaLlibres.setRowHeight(32);
		taulaLlibres.setFont(UITheme.fontBase());
		taulaLlibres.setShowGrid(true);
		taulaLlibres.setIntercellSpacing(new Dimension(0, 1));
		taulaLlibres.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		String[] headerTips = {I18n.t("tip_col_cover"), I18n.t("tip_col_isbn"), I18n.t("tip_col_title"), I18n.t("tip_col_author"),
			I18n.t("tip_col_year"), I18n.t("tip_col_rating"), I18n.t("tip_col_price") + " (" + herramienta.Configuracio.getCurrencySymbol() + ")",
			I18n.t("tip_col_read"), I18n.t("tip_col_progress"), I18n.t("tip_col_details")};

		taulaLlibres.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
			// Reutilitzat a totes les cel·les de la capçalera. El renderer
			// per defecte assigna un JLabel nou per cel·la per event de
			// desplaçament; amb 10 columnes i 30 files visibles, són 300
			// assignacions per repaint. El JLabel és la mateixa instància
			// que retorna `super` (el `this` del DefaultTableCellRenderer),
			// de manera que el podem mutar in situ en lloc de construir-ne
			// un de nou.
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
				JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
				lbl.setBackground(UITheme.palette().headerBg());
				lbl.setForeground(UITheme.palette().headerFg());
				lbl.setFont(UITheme.fontBold());
				lbl.setOpaque(true);
				lbl.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 2, 1, UITheme.palette().borderClr()),
					BorderFactory.createEmptyBorder(5, 10, 5, 10)
				));
				String text = v != null ? v.toString() : "";
				javax.swing.RowSorter<?> sorter = t.getRowSorter();
				if (sorter != null) {
					List<? extends javax.swing.RowSorter.SortKey> keys = sorter.getSortKeys();
					if (!keys.isEmpty() && keys.get(0).getColumn() == c) {
						text += keys.get(0).getSortOrder() == javax.swing.SortOrder.ASCENDING ? "  ▲" : "  ▼";
					}
				}
				lbl.setText(text);
				lbl.setToolTipText(c < headerTips.length ? headerTips[c] : null);
				return lbl;
			}
		});
		scrollPaneJTable.setViewportView(taulaLlibres);

		taulaLlibres.setDragEnabled(true);
		taulaLlibres.setTransferHandler(new TransferHandler() {
			@Override
			public int getSourceActions(javax.swing.JComponent c) { return COPY; }
			@Override
			protected Transferable createTransferable(javax.swing.JComponent c) {
				JTable t = (JTable) c;
				StringBuilder sb = new StringBuilder();
				for (int row : t.getSelectedRows()) {
					Object v = t.getValueAt(row, presentacio.ModelTaulaBiblioteca.COL_ISBN);
					if (v != null) { if (sb.length() > 0) sb.append(","); sb.append(v); }
				}
				return new StringSelection(sb.toString());
			}
		});
	}

	private void buildPagination() {
		paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
		paginationPanel.setBackground(UITheme.palette().bgMain());
		btnPaginaAnterior = new JButton(I18n.t("btn_page_prev"));
		UIComponents.styleSecondaryButton(btnPaginaAnterior);
		lblPagina = new JLabel(I18n.t("page_info_java", 1, 1));
		UIComponents.styleLabel(lblPagina);
		btnPaginaSeguent = new JButton(I18n.t("btn_page_next"));
		UIComponents.styleSecondaryButton(btnPaginaSeguent);
		paginationPanel.add(btnPaginaAnterior);
		paginationPanel.add(lblPagina);
		paginationPanel.add(btnPaginaSeguent);
		paginationPanel.setVisible(false);
	}

	public boolean esGaleriaMode() { return galeriaMode; }

	public void mostrarGaleria() {
		galeriaMode = true;
		cardLayout.show(contentCards, CARD_GALERIA);
	}

	public void mostrarTaula() {
		galeriaMode = false;
		cardLayout.show(contentCards, CARD_TAULA);
	}

	public void aplicarTheme() {
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		scrollPaneJTable.getViewport().setBackground(UITheme.palette().bgPanel());
		taulaLlibres.setBackground(UITheme.palette().bgPanel());
		taulaLlibres.setForeground(UITheme.palette().textDark());
		taulaLlibres.setSelectionBackground(UITheme.palette().accent());
		taulaLlibres.setSelectionForeground(Color.WHITE);
		taulaLlibres.setGridColor(UITheme.palette().tableGrid());
		javax.swing.UIManager.put("Table.alternateRowColor", UITheme.palette().tableAlt());

		paginationPanel.setBackground(UITheme.palette().bgMain());
		UIComponents.styleSecondaryButton(btnPaginaAnterior);
		UIComponents.styleSecondaryButton(btnPaginaSeguent);
		UIComponents.styleLabel(lblPagina);

		galeria.aplicarTheme();
	}

	public void aplicarThemePostLaf() {
		UIComponents.styleSecondaryButton(btnPaginaAnterior);
		UIComponents.styleSecondaryButton(btnPaginaSeguent);
	}

	public void nameScrollBarButtons() {
		nameScrollBar(scrollPaneJTable.getVerticalScrollBar());
	}

	private void nameScrollBar(javax.swing.JScrollBar sb) {
		Component[] comps = sb.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].getAccessibleContext().setAccessibleName(
				i == 0 ? I18n.t("acc_scroll_up") : I18n.t("acc_scroll_down"));
		}
	}

	public JButton obtenirBtnPaginaAnterior()     { return btnPaginaAnterior; }
	public JButton obtenirBtnPaginaSeguent()      { return btnPaginaSeguent; }
	public JLabel  obtenirLblPagina()             { return lblPagina; }
	public JPanel  obtenirPaginationPanel()       { return paginationPanel; }
	public JTable  obtenirTaulaLlibres()           { return taulaLlibres; }
	public JScrollPane obtenirScrollPaneJTable()  { return scrollPaneJTable; }
	public PanelGaleriaCobertes obtenirGaleria()  { return galeria; }
}
