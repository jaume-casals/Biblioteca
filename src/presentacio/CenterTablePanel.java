package presentacio;

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

public class CenterTablePanel extends JPanel {

	private JTable jTableBilio;
	private JScrollPane scrollPaneJTable;
	private JPanel paginationPanel;
	private JButton btnPaginaAnterior;
	private JButton btnPaginaSeguent;
	private JLabel lblPagina;
	private JPanel contentCards;
	private CardLayout cardLayout;
	private GaleriaCobertesPanel galeria;
	private boolean galeriaMode = false;

	public CenterTablePanel() {
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.BG_MAIN);

		galeria = new GaleriaCobertesPanel();
		buildTable();

		cardLayout = new CardLayout();
		contentCards = new JPanel(cardLayout);
		contentCards.setBackground(UITheme.BG_MAIN);
		contentCards.add(scrollPaneJTable, "TAULA");
		contentCards.add(galeria, "GALERIA");

		buildPagination();

		add(contentCards, BorderLayout.CENTER);
		add(paginationPanel, BorderLayout.SOUTH);
	}

	private void buildTable() {
		scrollPaneJTable = new JScrollPane();
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		scrollPaneJTable.getViewport().setBackground(UITheme.BG_PANEL);
		scrollPaneJTable.getVerticalScrollBar().setUnitIncrement(16);

		jTableBilio = new JTable() {
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
				java.awt.FontMetrics fm = getFontMetrics(getFont());
				return fm.stringWidth(text) > r.width ? text : null;
			}
		};
		jTableBilio.setDefaultEditor(Object.class, null);
		jTableBilio.setAutoCreateRowSorter(true);
		jTableBilio.getTableHeader().setReorderingAllowed(false);
		jTableBilio.setBackground(UITheme.BG_PANEL);
		jTableBilio.setSelectionBackground(UITheme.ACCENT);
		jTableBilio.setSelectionForeground(Color.WHITE);
		jTableBilio.setGridColor(UITheme.TABLE_GRID);
		jTableBilio.setRowHeight(32);
		jTableBilio.setFont(UITheme.fontBase());
		jTableBilio.setShowGrid(true);
		jTableBilio.setIntercellSpacing(new Dimension(0, 1));
		jTableBilio.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		String[] headerTips = {I18n.t("tip_col_cover"), I18n.t("tip_col_isbn"), I18n.t("tip_col_title"), I18n.t("tip_col_author"),
			I18n.t("tip_col_year"), I18n.t("tip_col_rating"), I18n.t("tip_col_price") + " (" + herramienta.Config.getCurrencySymbol() + ")",
			I18n.t("tip_col_read"), I18n.t("tip_col_progress"), I18n.t("tip_col_details")};

		jTableBilio.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
				JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
				lbl.setBackground(UITheme.HEADER_BG);
				lbl.setForeground(UITheme.HEADER_FG);
				lbl.setFont(UITheme.fontBold());
				lbl.setOpaque(true);
				lbl.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 2, 1, UITheme.BORDER_CLR),
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
		scrollPaneJTable.setViewportView(jTableBilio);

		jTableBilio.setDragEnabled(true);
		jTableBilio.setTransferHandler(new TransferHandler() {
			@Override
			public int getSourceActions(javax.swing.JComponent c) { return COPY; }
			@Override
			protected Transferable createTransferable(javax.swing.JComponent c) {
				JTable t = (JTable) c;
				StringBuilder sb = new StringBuilder();
				for (int row : t.getSelectedRows()) {
					Object v = t.getValueAt(row, 1);
					if (v != null) { if (sb.length() > 0) sb.append(","); sb.append(v); }
				}
				return new StringSelection(sb.toString());
			}
		});
	}

	private void buildPagination() {
		paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
		paginationPanel.setBackground(UITheme.BG_MAIN);
		btnPaginaAnterior = new JButton(I18n.t("btn_page_prev"));
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		lblPagina = new JLabel(I18n.t("page_info_java", 1, 1));
		UITheme.styleLabel(lblPagina);
		btnPaginaSeguent = new JButton(I18n.t("btn_page_next"));
		UITheme.styleSecondaryButton(btnPaginaSeguent);
		paginationPanel.add(btnPaginaAnterior);
		paginationPanel.add(lblPagina);
		paginationPanel.add(btnPaginaSeguent);
		paginationPanel.setVisible(false);
	}

	public boolean isGaleriaMode() { return galeriaMode; }

	public void showGaleria() {
		galeriaMode = true;
		cardLayout.show(contentCards, "GALERIA");
	}

	public void showTaula() {
		galeriaMode = false;
		cardLayout.show(contentCards, "TAULA");
	}

	public void applyTheme() {
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		scrollPaneJTable.getViewport().setBackground(UITheme.BG_PANEL);
		jTableBilio.setBackground(UITheme.BG_PANEL);
		jTableBilio.setForeground(UITheme.TEXT_DARK);
		jTableBilio.setSelectionBackground(UITheme.ACCENT);
		jTableBilio.setSelectionForeground(Color.WHITE);
		jTableBilio.setGridColor(UITheme.TABLE_GRID);
		javax.swing.UIManager.put("Table.alternateRowColor", UITheme.TABLE_ALT);

		paginationPanel.setBackground(UITheme.BG_MAIN);
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		UITheme.styleSecondaryButton(btnPaginaSeguent);
		UITheme.styleLabel(lblPagina);

		galeria.applyTheme();
	}

	public void applyThemePostLaf() {
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		UITheme.styleSecondaryButton(btnPaginaSeguent);
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

	public JButton getBtnPaginaAnterior()     { return btnPaginaAnterior; }
	public JButton getBtnPaginaSeguent()      { return btnPaginaSeguent; }
	public JLabel  getLblPagina()             { return lblPagina; }
	public JPanel  getPaginationPanel()       { return paginationPanel; }
	public JTable  getjTableBilio()           { return jTableBilio; }
	public JScrollPane getScrollPaneJTable()  { return scrollPaneJTable; }
	public GaleriaCobertesPanel getGaleria()  { return galeria; }
}
