package presentacio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import herramienta.Config;
import herramienta.UITheme;

public class MainFramePanel extends JFrame {

	private JPanel contentPane;
	private MostrarBibliotecaPanel mostrarBibliotecaPanel = new MostrarBibliotecaPanel();
	private JLabel statusBar;

	public MainFramePanel() {
		setTitle("Biblioteca");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(Config.getWindowX(), Config.getWindowY(),
				Config.getWindowWidth(), Config.getWindowHeight());
		setMinimumSize(new Dimension(800, 500));
		if (Config.isWindowMaximized()) setExtendedState(Frame.MAXIMIZED_BOTH);

		contentPane = new JPanel();
		contentPane.setBackground(UITheme.BG_MAIN);
		contentPane.setBorder(new EmptyBorder(8, 8, 8, 8));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		contentPane.add(mostrarBibliotecaPanel);

		statusBar = new JLabel(" ");
		statusBar.setFont(UITheme.FONT_BASE);
		statusBar.setForeground(UITheme.TEXT_MID);
		statusBar.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CLR),
			BorderFactory.createEmptyBorder(3, 6, 3, 6)));
		statusBar.setBackground(UITheme.BG_PANEL);
		statusBar.setOpaque(true);
		contentPane.add(statusBar, BorderLayout.SOUTH);

		pack();
	}

	public MostrarBibliotecaPanel getMostrarBibliotecaPanel() {
		return mostrarBibliotecaPanel;
	}

	public JLabel getStatusBar() { return statusBar; }
}
