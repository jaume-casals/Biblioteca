package presentacio;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class MainFramePanel extends JFrame {

	private JPanel contentPane;
	private Table vistaTable = new Table();

	private JMenuBar menuBar;
	private JMenu opcions;
	private JMenuItem addLlibre;

	public MainFramePanel() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.add(vistaTable);

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
}
