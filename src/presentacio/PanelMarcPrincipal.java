package presentacio;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import herramienta.i18n.I18n;
import herramienta.ui.UITheme;

/** Contingut de la finestra principal (taula de biblioteca + barra d'estat). La cromoteca del frame viu a {@link MainFrameControl}. */
public class PanelMarcPrincipal extends JPanel {

	private final PanelMostrarBiblioteca mostrarBibliotecaPanel = new PanelMostrarBiblioteca();
	private final JLabel statusBar;

	public PanelMarcPrincipal() {
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

	public PanelMostrarBiblioteca obtenirMostrarBibliotecaPanel() {
		return mostrarBibliotecaPanel;
	}

	public JLabel obtenirStatusBar() { return statusBar; }
}
