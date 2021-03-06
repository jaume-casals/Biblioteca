package presentacio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import herramienta.ConfiguracionPantalla;

public class MainFramePanel extends JFrame {

	private JPanel contentPane;
	private MostrarBibliotecaPanel mostrarBibliotecaPanel = new MostrarBibliotecaPanel();
	private ConfiguracionPantalla configuracionPantalla = new ConfiguracionPantalla();

	private JMenuBar menuBar;
	private JMenu opcions;
	private JMenuItem addLlibre;

	private JMenu mnAyuda;
	private JMenuItem mntmAbout;
	private JMenuItem mntOpcions;

	public MainFramePanel() {

		int amplada = amplada(100);
		int altura = altura(100);

		setTitle("Biblioteca");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(amplada, altura));
		setMaximumSize(new Dimension(amplada, altura));
		setBounds(100, 100, amplada, altura);
		setExtendedState(Frame.MAXIMIZED_BOTH);
		setResizable(false);

		contentPane = new JPanel();

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.add(mostrarBibliotecaPanel);

		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		opcions = new JMenu("Opcions");
		menuBar.add(opcions);

		addLlibre = new JMenuItem("Nou Llibre");
		opcions.add(addLlibre);

		mnAyuda = new JMenu("Ajuda");
		menuBar.add(mnAyuda);

		mntmAbout = new JMenuItem("Sobre l'equip");
		mnAyuda.add(mntmAbout);

		mntOpcions = new JMenuItem("Opcions");
		mnAyuda.add(mntOpcions);

		pack();
	}

	public JMenuItem getMntmAbout() {
		return mntmAbout;
	}

	public JMenuItem getaddLlibre() {
		return addLlibre;
	}

	public JMenuItem getMntOpcions() {
		return mntOpcions;
	}

	public void setVisible(Boolean bool) {
		this.setVisible(bool);
	}

	private int altura(int a) {
		return ((int) configuracionPantalla.getHeight() * a) / 100;
	}

	private int amplada(int a) {
		return ((int) configuracionPantalla.getWidth() * a) / 100;
	}

	public MostrarBibliotecaPanel getMostrarBibliotecaPanel() {
		return mostrarBibliotecaPanel;
	}
}
