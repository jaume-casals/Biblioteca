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
	private final PanelCapcalera capcalera;
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

		// Capçalera i barra d'eines comparteixen una columna: la capçalera
		// descriu què s'està mostrant, la barra d'eines hi actua.
		JPanel capcaleraITools = new JPanel(new BorderLayout(0, 0));
		capcaleraITools.setBackground(UITheme.palette().bgPanel());
		capcaleraITools.setOpaque(false);

		capcalera = new PanelCapcalera();
		capcaleraITools.add(capcalera, BorderLayout.NORTH);

		topBar = new PanelBarraSuperior();
		capcaleraITools.add(topBar, BorderLayout.SOUTH);

		rightArea.add(capcaleraITools, BorderLayout.NORTH);

		JPanel contentArea = new JPanel(new BorderLayout(0, 0));
		contentArea.setBackground(UITheme.palette().bgMain());

		filtrarDrawer = new PanelCalaixFiltre();
		filtrarDrawer.setVisible(false);
		contentArea.add(filtrarDrawer, BorderLayout.NORTH);

		centerTable = new PanelTaulaCentral();
		contentArea.add(centerTable, BorderLayout.CENTER);

		rightArea.add(contentArea, BorderLayout.CENTER);

		add(rightArea, BorderLayout.CENTER);

		// Les accions addicionals (importació, exportació, còpies de seguretat,
		// escaneig) viuen al calaix de filtres. El menú de la barra d'eines les
		// exposa sense duplicar-ne la lògica: delega amb doClick().
		topBar.afegirAccioMes("btn_import_csv", () -> filtrarDrawer.obtenirBtnImportarCSV().doClick());
		topBar.afegirAccioMes("btn_import_json_lbl", () -> filtrarDrawer.obtenirBtnImportarJSON().doClick());
		topBar.afegirAccioMes("btn_import_calibre", () -> filtrarDrawer.obtenirBtnImportarCalibre().doClick());
		topBar.afegirAccioMes("btn_export_csv", () -> filtrarDrawer.obtenirBtnExportCSV().doClick());
		topBar.afegirAccioMes("btn_export_json_lbl", () -> filtrarDrawer.obtenirBtnExportJSON().doClick());
		topBar.afegirAccioMes("btn_export_html_lbl", () -> filtrarDrawer.obtenirBtnExportHTML().doClick());
		topBar.afegirAccioMes("btn_export_pdf_lbl", () -> filtrarDrawer.obtenirBtnExportPDF().doClick());
		topBar.afegirAccioMes("btn_fetch_covers_lbl", () -> filtrarDrawer.obtenirBtnFetchCovers().doClick());
		topBar.afegirAccioMes("btn_scan_isbn_lbl", () -> filtrarDrawer.obtenirBtnEscanejarISBN().doClick());
		topBar.afegirAccioMes("btn_backup_bd", () -> filtrarDrawer.obtenirBtnBackupBD().doClick());
		topBar.afegirAccioMes("btn_restore_bd", () -> filtrarDrawer.obtenirBtnRestaurarBD().doClick());
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
	 * Aplica el tema actiu a tota la pantalla.
	 *
	 * <p>L'ordre importa: (1) les fonts es reconstruïen, (2) el tema es
	 * projecta sobre el UIManager (FlatLaf ja ha estat instal·lat per
	 * {@code UITheme.posarTheme}), (3) es reapliquen els tokens explícits
	 * dels sub-panells <em>després</em> de {@code updateComponentTreeUI},
	 * perquè l'actualització de l'arbre de components reinicialitza els
	 * colors que hagi fixat el LAF.
	 */
	public void aplicarTheme() {
		if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
			// El repintat del tema toca tots els components: fora de l'EDT
			// competeix amb el layout i deixa controls a mitges pintar.
			javax.swing.SwingUtilities.invokeLater(this::aplicarTheme);
			return;
		}
		UITheme.rebuildFonts(herramienta.config.MidaLletra.fromKey(herramienta.config.Configuracio.obtenirFontSize()));
        ConfiguracioUi.posarTheme(UITheme.obtenirTheme());
        UITheme.aplicarUIManager();

		setBackground(UITheme.palette().bgMain());

		SwingUtilities.updateComponentTreeUI(this);

		Window w = SwingUtilities.getWindowAncestor(this);
		if (w instanceof JFrame) {
			((JFrame) w).getContentPane().setBackground(UITheme.palette().bgMain());
			((JFrame) w).setBackground(UITheme.palette().bgMain());
		}

		filtrarDrawer.aplicarThemePostLaf();
		leftSidebar.aplicarTheme();
		capcalera.aplicarTheme();
		topBar.aplicarTheme();
		centerTable.aplicarTheme();
		centerTable.aplicarThemePostLaf();

		SwingUtilities.invokeLater(this::nameScrollBarButtons);

		repaint();

		// La barra d'estat viu al panellancè: s'actualitza des d'aquí perquè
		// el canvi de tema pot arribar per qualsevol camí (drecera de
		// teclat, diàleg de configuració o botó de la barra lateral).
		for (java.awt.Component c = getParent(); c != null; c = c.getParent()) {
			if (c instanceof PanelMarcPrincipal marc) {
				marc.aplicarStatusBar();
				break;
			}
		}

		// El repintat del panell no arriba a les regions del tanc que no en
		// formen part (títols, vores, el marc de la finestra). Sense aquest
		// repintat de la finestra sencera, les zones que no s'havien tornat a
		// pintar conservaven el dibuix del tema anterior i la pantalla
		// mostrava clar i fosc alhora.
		if (w != null) {
			w.invalidate();
			w.validate();
			w.repaint();
		}
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
	public JButton obtenirBtnNouLlibre()          { return capcalera.obtenirBtnAfegirLlibre(); }
	public PanelCapcalera obtenirCapcalera()      { return capcalera; }
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
	public javax.swing.JToggleButton obtenirBtnToggleFiltres() { return topBar.obtenirBtnToggleFiltres(); }
	public JButton obtenirBtnOrdena()             { return topBar.obtenirBtnOrdena(); }
	public javax.swing.JButton obtenirBtnVistaGaleria() { return topBar.obtenirBtnVistaGaleria(); }
	public javax.swing.JButton obtenirBtnVistaTaula()   { return topBar.obtenirBtnVistaTaula(); }
	public javax.swing.JToggleButton obtenirBtnAgruparSeries(){ return topBar.obtenirBtnAgruparSeries(); }
	public JButton obtenirBtnMesAccions()         { return topBar.obtenirBtnMesAccions(); }
	public PanelBarraSuperior obtenirBarraSuperior() { return topBar; }
	public PanellBarraEsquerra obtenirBarraEsquerra() { return leftSidebar; }
	public RegistreCampsFormulari obtenirFilterRegistry() { return filtrarDrawer.obtenirRegistry(); }
	public PanelGaleriaCobertes obtenirGaleria()  { return centerTable.obtenirGaleria(); }
}

