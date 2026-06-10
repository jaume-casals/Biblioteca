package presentacio;



import presentacio.UIComponents;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import herramienta.I18n;
import herramienta.UITheme;

public class TopBarPanel extends JPanel {

	private JTextField searchBar;
	private JButton btnToggleFiltres;
	private JButton btnToggleVista;
	private JButton btnGroupSeries;
	private JButton btnNouLlibre;

	public TopBarPanel() {
		buildTopBar();
	}

	private void buildTopBar() {
		setLayout(new BorderLayout(8, 0));
		setBackground(UITheme.palette().bgPanel());
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()),
			BorderFactory.createEmptyBorder(10, 16, 10, 16)
		));

		JPanel searchWrap = new JPanel(new BorderLayout(6, 0));
		searchWrap.setBackground(UITheme.palette().bgPanel());
		searchBar = new JTextField();
		searchBar.setToolTipText(I18n.t("tip_search_bar"));
		UIComponents.styleField(searchBar);
		searchWrap.add(searchBar, BorderLayout.CENTER);
		add(searchWrap, BorderLayout.CENTER);

		JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		rightBtns.setBackground(UITheme.palette().bgPanel());

		btnToggleFiltres = new JButton(I18n.t("btn_toggle_filtres_lbl"));
		UIComponents.styleSecondaryButton(btnToggleFiltres);
		btnToggleFiltres.setToolTipText(I18n.t("tip_toggle_filtres"));
		rightBtns.add(btnToggleFiltres);

		btnToggleVista = new JButton(I18n.t("btn_toggle_vista_lbl"));
		UIComponents.styleSecondaryButton(btnToggleVista);
		btnToggleVista.setToolTipText(I18n.t("tip_toggle_vista"));
		rightBtns.add(btnToggleVista);

		btnGroupSeries = new JButton(I18n.t("btn_group_series_lbl"));
		UIComponents.styleSecondaryButton(btnGroupSeries);
		btnGroupSeries.setToolTipText(I18n.t("tip_group_series"));
		rightBtns.add(btnGroupSeries);

		btnNouLlibre = new JButton(I18n.t("btn_nou_llibre_short"));
		UIComponents.styleAccentButton(btnNouLlibre);
		btnNouLlibre.setToolTipText(I18n.t("tip_nou_llibre_short"));
		rightBtns.add(btnNouLlibre);

		add(rightBtns, BorderLayout.EAST);
	}

	public void applyTheme() {
		UIComponents.styleField(searchBar);
		UIComponents.styleAccentButton(btnNouLlibre);
		UIComponents.styleSecondaryButton(btnToggleFiltres);
		UIComponents.styleSecondaryButton(btnToggleVista);
		UIComponents.styleSecondaryButton(btnGroupSeries);
	}

	public JTextField getSearchBar()          { return searchBar; }
	public JButton getBtnToggleFiltres()      { return btnToggleFiltres; }
	public JButton getBtnToggleVista()        { return btnToggleVista; }
	public JButton getBtnGroupSeries()        { return btnGroupSeries; }
	public JButton getBtnNouLlibre()          { return btnNouLlibre; }
}
