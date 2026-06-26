package presentacio.panells;



import presentacio.util.UIComponents;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import herramienta.i18n.I18n;
import herramienta.ui.UITheme;

public class PanelBarraSuperior extends JPanel {

	private record TopBtn(String lblKey, String tipKey, Consumer<JButton> styler) {}

	private static final List<TopBtn> TOP_BTNS = List.of(
		new TopBtn("btn_toggle_filtres_lbl", "tip_toggle_filtres", UIComponents::styleSecondaryButton),
		new TopBtn("btn_toggle_vista_lbl", "tip_toggle_vista", UIComponents::styleSecondaryButton),
		new TopBtn("btn_group_series_lbl", "tip_group_series", UIComponents::styleSecondaryButton),
		new TopBtn("btn_nou_llibre_short", "tip_nou_llibre_short", UIComponents::styleAccentButton)
	);

	private JTextField cercarBar;
	private JButton btnToggleFiltres;
	private JButton btnToggleVista;
	private JButton btnGroupSeries;
	private JButton btnNouLlibre;

	public PanelBarraSuperior() {
		buildTopBar();
	}

	private void buildTopBar() {
		setLayout(new BorderLayout(8, 0));
		setBackground(UITheme.palette().bgPanel());
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()),
			BorderFactory.createEmptyBorder(10, 16, 10, 16)
		));

		JPanel cercarWrap = new JPanel(new BorderLayout(6, 0));
		cercarWrap.setBackground(UITheme.palette().bgPanel());
		cercarBar = new JTextField();
		cercarBar.setToolTipText(I18n.t("tip_search_bar"));
		UIComponents.styleField(cercarBar);
		cercarWrap.add(cercarBar, BorderLayout.CENTER);
		add(cercarWrap, BorderLayout.CENTER);

		JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		rightBtns.setBackground(UITheme.palette().bgPanel());

		btnToggleFiltres = makeBtn(rightBtns, TOP_BTNS.get(0));
		btnToggleVista = makeBtn(rightBtns, TOP_BTNS.get(1));
		btnGroupSeries = makeBtn(rightBtns, TOP_BTNS.get(2));
		btnNouLlibre = makeBtn(rightBtns, TOP_BTNS.get(3));

		add(rightBtns, BorderLayout.EAST);
	}

	private JButton makeBtn(JPanel row, TopBtn spec) {
		JButton b = new JButton(I18n.t(spec.lblKey()));
		spec.styler().accept(b);
		b.setToolTipText(I18n.t(spec.tipKey()));
		row.add(b);
		return b;
	}

	public void aplicarTheme() {
		UIComponents.styleField(cercarBar);
		for (TopBtn spec : TOP_BTNS) spec.styler().accept(buttonFor(spec));
	}

	private JButton buttonFor(TopBtn spec) {
		if (spec == TOP_BTNS.get(0)) return btnToggleFiltres;
		if (spec == TOP_BTNS.get(1)) return btnToggleVista;
		if (spec == TOP_BTNS.get(2)) return btnGroupSeries;
		return btnNouLlibre;
	}

	public JTextField obtenirSearchBar()          { return cercarBar; }
	public JButton obtenirBtnToggleFiltres()      { return btnToggleFiltres; }
	public JButton obtenirBtnToggleVista()        { return btnToggleVista; }
	public JButton obtenirBtnGroupSeries()        { return btnGroupSeries; }
	public JButton obtenirBtnNouLlibre()          { return btnNouLlibre; }
}
