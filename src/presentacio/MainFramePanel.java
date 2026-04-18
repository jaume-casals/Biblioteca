package presentacio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import herramienta.ConfiguracionPantalla;
import herramienta.UITheme;

public class MainFramePanel extends JFrame {

	private JPanel contentPane;
	private MostrarBibliotecaPanel mostrarBibliotecaPanel = new MostrarBibliotecaPanel();
	private ConfiguracionPantalla configuracionPantalla = new ConfiguracionPantalla();

	public MainFramePanel() {

		int amplada = amplada(100);
		int altura = altura(100);

		setTitle("Biblioteca");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(amplada, altura));
		setBounds(100, 100, amplada, altura);
		setMinimumSize(new Dimension(800, 500));
		setExtendedState(Frame.MAXIMIZED_BOTH);

		contentPane = new JPanel();
		contentPane.setBackground(UITheme.BG_MAIN);
		contentPane.setBorder(new EmptyBorder(8, 8, 8, 8));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.add(mostrarBibliotecaPanel);

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

	public MostrarBibliotecaPanel getMostrarBibliotecaPanel() {
		return mostrarBibliotecaPanel;
	}
}
