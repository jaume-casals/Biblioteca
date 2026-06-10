package presentacio;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import herramienta.I18n;
import herramienta.UITheme;

/** Main window content (library table + status bar). Frame chrome lives in {@link MainFrameControl}. */
public class MainFramePanel extends JPanel {

	private final MostrarBibliotecaPanel mostrarBibliotecaPanel = new MostrarBibliotecaPanel();
	private final JLabel statusBar;

	public MainFramePanel() {
		setBackground(UITheme.palette().bgMain());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setLayout(new BorderLayout(0, 0));
		add(mostrarBibliotecaPanel, BorderLayout.CENTER);

		statusBar = new JLabel(" ");
		statusBar.setFont(UITheme.fontBase());
		statusBar.setForeground(UITheme.palette().textMid());
		statusBar.getAccessibleContext().setAccessibleName(I18n.t("acc_status_bar"));
		statusBar.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.palette().borderClr()),
			BorderFactory.createEmptyBorder(3, 6, 3, 6)));
		statusBar.setBackground(UITheme.palette().bgPanel());
		statusBar.setOpaque(true);
		add(statusBar, BorderLayout.SOUTH);
	}

	public MostrarBibliotecaPanel getMostrarBibliotecaPanel() {
		return mostrarBibliotecaPanel;
	}

	public JLabel getStatusBar() { return statusBar; }
}
