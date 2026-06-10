package presentacio;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import herramienta.UITheme;
import herramienta.UiConfig;

public class MostrarBibliotecaPanel extends JPanel {

	private final JComboBox<Object> comboLlistes;
	private final LeftSidebarPanel leftSidebar;
	private final TopBarPanel topBar;
	private final FilterDrawerPanel filterDrawer;
	private final CenterTablePanel centerTable;

	public MostrarBibliotecaPanel() {
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.palette().bgMain());

		comboLlistes = new JComboBox<>();

		leftSidebar = new LeftSidebarPanel(comboLlistes, this::applyTheme);
		add(leftSidebar, BorderLayout.WEST);

		JPanel rightArea = new JPanel(new BorderLayout(0, 0));
		rightArea.setBackground(UITheme.palette().bgMain());

		topBar = new TopBarPanel();
		rightArea.add(topBar, BorderLayout.NORTH);

		JPanel contentArea = new JPanel(new BorderLayout(0, 0));
		contentArea.setBackground(UITheme.palette().bgMain());

		filterDrawer = new FilterDrawerPanel();
		filterDrawer.setVisible(false);
		contentArea.add(filterDrawer, BorderLayout.NORTH);

		centerTable = new CenterTablePanel();
		contentArea.add(centerTable, BorderLayout.CENTER);

		rightArea.add(contentArea, BorderLayout.CENTER);

		add(rightArea, BorderLayout.CENTER);
	}

	public void rebuildSidebarShelves(List<domini.Llista> llistes, Map<Integer, Integer> counts,
			java.util.function.BiConsumer<Integer, java.util.List<Long>> onDragToShelf,
			java.util.function.Consumer<domini.Llista> onShelfRename) {
		leftSidebar.rebuildSidebarShelves(llistes, counts, onDragToShelf, onShelfRename);
	}

	public boolean isFilterDrawerVisible() { return filterDrawer.isVisible(); }

	public void setFilterDrawerVisible(boolean v) {
		filterDrawer.setVisible(v);
		revalidate();
		repaint();
	}

	public boolean isGaleriaMode() { return centerTable.isGaleriaMode(); }

	public void showGaleria() { centerTable.showGaleria(); }

	public void showTaula() { centerTable.showTaula(); }

	public void applyTheme() {
		UITheme.rebuildFonts(herramienta.FontSize.fromKey(herramienta.Config.getFontSize()));
        UiConfig.setTheme(UITheme.getTheme());

		setBackground(UITheme.palette().bgMain());

		leftSidebar.applyTheme();
		centerTable.applyTheme();
		topBar.applyTheme();
		filterDrawer.applyTheme();

		Window w = SwingUtilities.getWindowAncestor(this);
		if (w instanceof JFrame) {
			((JFrame) w).getContentPane().setBackground(UITheme.palette().bgMain());
		}

		try {
			UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
			UITheme.applyUIManager();
		} catch (Exception ignored) {}
		SwingUtilities.updateComponentTreeUI(this);
		SwingUtilities.invokeLater(this::nameScrollBarButtons);

		filterDrawer.applyThemePostLaf();
		leftSidebar.applyTheme();
		topBar.applyTheme();
		centerTable.applyThemePostLaf();

		repaint();
	}

	private void nameScrollBarButtons() {
		centerTable.nameScrollBarButtons();
		leftSidebar.nameScrollBarButtons();
	}

	// ── Getters (delegate to sub-panels) ──────────────────────────────────────

	public JButton getBtnPaginaAnterior()     { return centerTable.getBtnPaginaAnterior(); }
	public JButton getBtnPaginaSeguent()      { return centerTable.getBtnPaginaSeguent(); }
	public JLabel  getLblPagina()             { return centerTable.getLblPagina(); }
	public JPanel  getPaginationPanel()       { return centerTable.getPaginationPanel(); }
	public JButton getBtnNouLlibre()          { return topBar.getBtnNouLlibre(); }
	public JButton getBtnExportCSV()          { return filterDrawer.getBtnExportCSV(); }
	public JButton getBtnImportarCSV()        { return filterDrawer.getBtnImportarCSV(); }
	public JButton getBtnImportarCalibre()    { return filterDrawer.getBtnImportarCalibre(); }
	public JButton getBtnExportJSON()         { return filterDrawer.getBtnExportJSON(); }
	public JButton getBtnImportarJSON()       { return filterDrawer.getBtnImportarJSON(); }
	public JButton getBtnExportHTML()         { return filterDrawer.getBtnExportHTML(); }
	public JButton getBtnExportPDF()          { return filterDrawer.getBtnExportPDF(); }
	public JButton getBtnFetchCovers()        { return filterDrawer.getBtnFetchCovers(); }
	public JButton getBtnEscanejarISBN()      { return filterDrawer.getBtnEscanejarISBN(); }
	public JButton getBtnEstadistiques()      { return leftSidebar.getBtnEstadistiques(); }
	public JButton getBtnLlibreAleatori()     { return leftSidebar.getBtnLlibreAleatori(); }
	public JButton getBtnBackupBD()           { return filterDrawer.getBtnBackupBD(); }
	public JButton getBtnRestaurarBD()        { return filterDrawer.getBtnRestaurarBD(); }
	public JButton getBtnConfiguracio()       { return leftSidebar.getBtnConfiguracio(); }
	public JButton getBtnSobre()              { return leftSidebar.getBtnSobre(); }
	public JTextField getSearchBar()          { return topBar.getSearchBar(); }
	public JTextField getTextISBN()           { return filterDrawer.getTextISBN(); }
	public JTextField getTextNom()            { return filterDrawer.getTextNom(); }
	public JTextField getTextAutor()          { return filterDrawer.getTextAutor(); }
	public JTextField getAnyMin()             { return filterDrawer.getAnyMin(); }
	public JTextField getAnyMax()             { return filterDrawer.getAnyMax(); }
	public JTextField getValoracioMin()       { return filterDrawer.getValoracioMin(); }
	public JTextField getValoracioMax()       { return filterDrawer.getValoracioMax(); }
	public JTextField getPreuMin()            { return filterDrawer.getPreuMin(); }
	public JTextField getPreuMax()            { return filterDrawer.getPreuMax(); }
	public JTable  getjTableBilio()           { return centerTable.getjTableBilio(); }
	public JPanel  getPanelFiltros()          { return filterDrawer.getPanelFiltros(); }
	public JScrollPane getScrollPaneJTable()  { return centerTable.getScrollPaneJTable(); }
	public JScrollPane getScrolpaneFiltro()   { return filterDrawer.getScrolpaneFiltro(); }
	public JCheckBox getchckbxLlegit()        { return filterDrawer.getchckbxLlegit(); }
	public JCheckBox getchckbxNoLlegit()      { return filterDrawer.getchckbxNoLlegit(); }
	public JButton getbtnFiltrar()            { return filterDrawer.getbtnFiltrar(); }
	public JButton getbttnQuitarFiltros()     { return filterDrawer.getbttnQuitarFiltros(); }
	public JComboBox<Object> getComboLlistes(){ return comboLlistes; }
	public JButton getBtnGestioLlistes()      { return leftSidebar.getBtnGestioLlistes(); }
	public JComboBox<Object> getComboTagFilter() { return filterDrawer.getComboTagFilter(); }
	public JTextField getFilterEditorial()       { return filterDrawer.getFilterEditorial(); }
	public JTextField getFilterSerie()           { return filterDrawer.getFilterSerie(); }
	public JTextField getFilterIdioma()          { return filterDrawer.getFilterIdioma(); }
	public JComboBox<String> getFilterFormat()   { return filterDrawer.getFilterFormat(); }
	public JButton getBtnAfegitsRecentment()  { return leftSidebar.getBtnAfegitsRecentment(); }
	public JButton getBtnLlegitsRecentment()  { return leftSidebar.getBtnLlegitsRecentment(); }
	public JButton getBtnDesitjats()          { return leftSidebar.getBtnDesitjats(); }
	public JButton getBtnEnCurs()             { return leftSidebar.getBtnEnCurs(); }
	public JComboBox<String> getComboPresets(){ return filterDrawer.getComboPresets(); }
	public JButton getBtnCarregaPreset()      { return filterDrawer.getBtnCarregaPreset(); }
	public JButton getBtnDesaPreset()         { return filterDrawer.getBtnDesaPreset(); }
	public JButton getBtnEsborraPreset()      { return filterDrawer.getBtnEsborraPreset(); }
	public JButton getBtnToggleFiltres()      { return topBar.getBtnToggleFiltres(); }
	public JButton getBtnToggleVista()        { return topBar.getBtnToggleVista(); }
	public JButton getBtnGroupSeries()        { return topBar.getBtnGroupSeries(); }
	public GaleriaCobertesPanel getGaleria()  { return centerTable.getGaleria(); }
}
