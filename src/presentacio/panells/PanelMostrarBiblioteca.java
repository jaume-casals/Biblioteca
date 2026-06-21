package presentacio.panells;

import herramienta.config.ConfiguracioUi;
import herramienta.ui.UITheme;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import presentacio.formularis.RegistreCampsFormulari;



public class PanelMostrarBiblioteca extends JPanel {

	private final JComboBox<Object> comboLlistes;
	private final PanellBarraEsquerra leftSidebar;
	private final PanelBarraSuperior topBar;
	private final PanelCalaixFiltre filtrarDrawer;
	private final PanelTaulaCentral centerTable;

	public PanelMostrarBiblioteca() {
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.palette().bgMain());

		comboLlistes = new JComboBox<>();

		leftSidebar = new PanellBarraEsquerra(comboLlistes, this::aplicarTheme);
		add(leftSidebar, BorderLayout.WEST);

		JPanel rightArea = new JPanel(new BorderLayout(0, 0));
		rightArea.setBackground(UITheme.palette().bgMain());

		topBar = new PanelBarraSuperior();
		rightArea.add(topBar, BorderLayout.NORTH);

		JPanel contentArea = new JPanel(new BorderLayout(0, 0));
		contentArea.setBackground(UITheme.palette().bgMain());

		filtrarDrawer = new PanelCalaixFiltre();
		filtrarDrawer.setVisible(false);
		contentArea.add(filtrarDrawer, BorderLayout.NORTH);

		centerTable = new PanelTaulaCentral();
		contentArea.add(centerTable, BorderLayout.CENTER);

		rightArea.add(contentArea, BorderLayout.CENTER);

		add(rightArea, BorderLayout.CENTER);
	}

	public void rebuildSidebarShelves(List<domini.Llista> llistes, Map<Integer, Integer> counts,
			java.util.function.BiConsumer<Integer, java.util.List<Long>> onDragToShelf,
			java.util.function.Consumer<domini.Llista> onShelfRename) {
		leftSidebar.rebuildSidebarShelves(llistes, counts, onDragToShelf, onShelfRename);
	}

	public boolean esFilterDrawerVisible() { return filtrarDrawer.isVisible(); }

	public void posarFilterDrawerVisible(boolean v) {
		filtrarDrawer.setVisible(v);
		revalidate();
		repaint();
	}

	public boolean esGaleriaMode() { return centerTable.esGaleriaMode(); }

	public void mostrarGaleria() { centerTable.mostrarGaleria(); }

	public void mostrarTaula() { centerTable.mostrarTaula(); }

	/**
	 * L'ordre importa: (1) actualitza primer els camps derivats de la
	 * paleta, (2) overrides pre-LaF dels sub-panells, (3) instal·la el
	 * L&F (reestableix tots els valors per defecte), (4) re-aplica els
	 * overrides del UIManager a sobre del L&F, (5) updateComponentTreeUI,
	 * (6) overrides post-LaF dels sub-panells per a allò que el L&F ha
	 * reestablert.
	 */
	public void aplicarTheme() {
		UITheme.rebuildFonts(herramienta.config.MidaLletra.fromKey(herramienta.config.Configuracio.obtenirFontSize()));
        ConfiguracioUi.posarTheme(UITheme.obtenirTheme());

		setBackground(UITheme.palette().bgMain());

		leftSidebar.aplicarTheme();
		centerTable.aplicarTheme();
		topBar.aplicarTheme();
		filtrarDrawer.aplicarTheme();

		Window w = SwingUtilities.getWindowAncestor(this);
		if (w instanceof JFrame) {
			((JFrame) w).getContentPane().setBackground(UITheme.palette().bgMain());
		}

		try {
			UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
			UITheme.aplicarUIManager();
		} catch (Exception ignored) {}
		SwingUtilities.updateComponentTreeUI(this);
		SwingUtilities.invokeLater(this::nameScrollBarButtons);

		filtrarDrawer.aplicarThemePostLaf();
		leftSidebar.aplicarTheme();
		topBar.aplicarTheme();
		centerTable.aplicarThemePostLaf();

		repaint();
	}

	private void nameScrollBarButtons() {
		centerTable.nameScrollBarButtons();
		leftSidebar.nameScrollBarButtons();
	}

	// ── Getters (delegate to sub-panels) ──────────────────────────────────────

	public JButton obtenirBtnPaginaAnterior()     { return centerTable.obtenirBtnPaginaAnterior(); }
	public JButton obtenirBtnPaginaSeguent()      { return centerTable.obtenirBtnPaginaSeguent(); }
	public JLabel  obtenirLblPagina()             { return centerTable.obtenirLblPagina(); }
	public JPanel  obtenirPaginationPanel()       { return centerTable.obtenirPaginationPanel(); }
	public JButton obtenirBtnNouLlibre()          { return topBar.obtenirBtnNouLlibre(); }
	public JButton obtenirBtnExportCSV()          { return filtrarDrawer.obtenirBtnExportCSV(); }
	public JButton obtenirBtnImportarCSV()        { return filtrarDrawer.obtenirBtnImportarCSV(); }
	public JButton obtenirBtnImportarCalibre()    { return filtrarDrawer.obtenirBtnImportarCalibre(); }
	public JButton obtenirBtnExportJSON()         { return filtrarDrawer.obtenirBtnExportJSON(); }
	public JButton obtenirBtnImportarJSON()       { return filtrarDrawer.obtenirBtnImportarJSON(); }
	public JButton obtenirBtnExportHTML()         { return filtrarDrawer.obtenirBtnExportHTML(); }
	public JButton obtenirBtnExportPDF()          { return filtrarDrawer.obtenirBtnExportPDF(); }
	public JButton obtenirBtnFetchCovers()        { return filtrarDrawer.obtenirBtnFetchCovers(); }
	public JButton obtenirBtnEscanejarISBN()      { return filtrarDrawer.obtenirBtnEscanejarISBN(); }
	public JButton obtenirBtnEstadistiques()      { return leftSidebar.obtenirBtnEstadistiques(); }
	public JButton obtenirBtnLlibreAleatori()     { return leftSidebar.obtenirBtnLlibreAleatori(); }
	public JButton obtenirBtnBackupBD()           { return filtrarDrawer.obtenirBtnBackupBD(); }
	public JButton obtenirBtnRestaurarBD()        { return filtrarDrawer.obtenirBtnRestaurarBD(); }
	public JButton obtenirBtnConfiguracio()       { return leftSidebar.obtenirBtnConfiguracio(); }
	public JButton obtenirBtnSobre()              { return leftSidebar.obtenirBtnSobre(); }
	public JTextField obtenirSearchBar()          { return topBar.obtenirSearchBar(); }
	public JTextField obtenirTextISBN()           { return filtrarDrawer.obtenirTextISBN(); }
	public JTextField obtenirTextNom()            { return filtrarDrawer.obtenirTextNom(); }
	public JTextField obtenirTextAutor()          { return filtrarDrawer.obtenirTextAutor(); }
	public JTextField obtenirAnyMin()             { return filtrarDrawer.obtenirAnyMin(); }
	public JTextField obtenirAnyMax()             { return filtrarDrawer.obtenirAnyMax(); }
	public JTextField obtenirValoracioMin()       { return filtrarDrawer.obtenirValoracioMin(); }
	public JTextField obtenirValoracioMax()       { return filtrarDrawer.obtenirValoracioMax(); }
	public JTextField obtenirPreuMin()            { return filtrarDrawer.obtenirPreuMin(); }
	public JTextField obtenirPreuMax()            { return filtrarDrawer.obtenirPreuMax(); }
	public JTable  obtenirTaulaLlibres()           { return centerTable.obtenirTaulaLlibres(); }
	public JPanel  obtenirPanelFiltros()          { return filtrarDrawer.obtenirPanelFiltros(); }
	public JScrollPane obtenirScrollPaneJTable()  { return centerTable.obtenirScrollPaneJTable(); }
	public JScrollPane obtenirScrolpaneFiltro()   { return filtrarDrawer.obtenirScrolpaneFiltro(); }
	public JCheckBox obtenirCasellaLlegit()        { return filtrarDrawer.obtenirCasellaLlegit(); }
	public JCheckBox obtenirCasellaNoLlegit()      { return filtrarDrawer.obtenirCasellaNoLlegit(); }
	public JButton obtenirBtnFiltrar()            { return filtrarDrawer.obtenirBtnFiltrar(); }
	public JButton obtenirBtnQuitarFiltros()     { return filtrarDrawer.obtenirBtnQuitarFiltros(); }
	public JComboBox<Object> obtenirComboLlistes(){ return comboLlistes; }
	public JButton obtenirBtnGestioLlistes()      { return leftSidebar.obtenirBtnGestioLlistes(); }
	public JComboBox<Object> obtenirComboTagFilter() { return filtrarDrawer.obtenirComboTagFilter(); }
	public JTextField obtenirFilterEditorial()       { return filtrarDrawer.obtenirFilterEditorial(); }
	public JTextField obtenirFilterSerie()           { return filtrarDrawer.obtenirFilterSerie(); }
	public JTextField obtenirFilterIdioma()          { return filtrarDrawer.obtenirFilterIdioma(); }
	public JComboBox<String> obtenirFilterFormat()   { return filtrarDrawer.obtenirFilterFormat(); }
	public JButton obtenirBtnAfegitsRecentment()  { return leftSidebar.obtenirBtnAfegitsRecentment(); }
	public JButton obtenirBtnLlegitsRecentment()  { return leftSidebar.obtenirBtnLlegitsRecentment(); }
	public JButton obtenirBtnDesitjats()          { return leftSidebar.obtenirBtnDesitjats(); }
	public JButton obtenirBtnEnCurs()             { return leftSidebar.obtenirBtnEnCurs(); }
	public JComboBox<String> obtenirComboPresets(){ return filtrarDrawer.obtenirComboPresets(); }
	public JButton obtenirBtnCarregaPreset()      { return filtrarDrawer.obtenirBtnCarregaPreset(); }
	public JButton obtenirBtnDesaPreset()         { return filtrarDrawer.obtenirBtnDesaPreset(); }
	public JButton obtenirBtnEsborraPreset()      { return filtrarDrawer.obtenirBtnEsborraPreset(); }
	public JButton obtenirBtnToggleFiltres()      { return topBar.obtenirBtnToggleFiltres(); }
	public JButton obtenirBtnToggleVista()        { return topBar.obtenirBtnToggleVista(); }
	public JButton obtenirBtnGroupSeries()        { return topBar.obtenirBtnGroupSeries(); }
	public RegistreCampsFormulari obtenirFilterRegistry() { return filtrarDrawer.obtenirRegistry(); }
	public PanelGaleriaCobertes obtenirGaleria()  { return centerTable.obtenirGaleria(); }
}

