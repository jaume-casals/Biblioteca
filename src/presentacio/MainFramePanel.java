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
	private MostrarBibliotecaPanel vistaMostrarBibliotecaPanel = new MostrarBibliotecaPanel();
	private ConfiguracionPantalla configuracionPantalla = new ConfiguracionPantalla();

	private JMenuBar menuBar;
	private JMenu opcions;
	private JMenuItem addLlibre;

	public MainFramePanel() {
		setTitle("Biblioteca");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1600, 900));
		setMaximumSize(new Dimension(1600, 900));
		setBounds(100, 100, amplada(100), altura(100));
		setExtendedState(Frame.MAXIMIZED_BOTH);
		setResizable(false);

		contentPane = new JPanel();

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.add(vistaMostrarBibliotecaPanel);

		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		opcions = new JMenu("Opcions");
		menuBar.add(opcions);

		addLlibre = new JMenuItem("Nou Llibre");
		opcions.add(addLlibre);

		pack();
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
}
