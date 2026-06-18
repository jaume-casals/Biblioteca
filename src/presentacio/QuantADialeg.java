package presentacio;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.net.URI;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import herramienta.I18n;
import herramienta.UITheme;

public class QuantADialeg extends JDialog {

	private static final String SOURCE_URL = "https://github.com/jaume-casals/Biblioteca";

	private static String getVersion() {
		try (var in = QuantADialeg.class.getResourceAsStream("/version.properties")) {
			if (in != null) {
				var p = new java.util.Properties();
				p.load(in);
				return p.getProperty("version", "?");
			}
		} catch (Exception ignored) {}
		return "?";
	}

	public QuantADialeg(Frame parent) {
		super(parent, I18n.t("dlg_about_title"), true);
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(420, 480);
		setLocationRelativeTo(parent);
		getContentPane().setBackground(UITheme.palette().bgPanel());
		setLayout(new BorderLayout(0, 0));

		JPanel main = new JPanel();
		main.setBackground(UITheme.palette().bgPanel());
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		main.setBorder(new EmptyBorder(24, 32, 16, 32));

		// Nom de l'aplicació
		JLabel lblApp = new JLabel(I18n.t("app_title") + " " + getVersion());
		lblApp.setFont(UITheme.fontBold().deriveFont(22f));
		lblApp.setForeground(UITheme.palette().accent());
		lblApp.setAlignmentX(Component.CENTER_ALIGNMENT);
		main.add(lblApp);

		main.add(Box.createVerticalStrut(4));

		JLabel lblSub = new JLabel(I18n.t("lbl_app_desc"));
		UIComponents.styleLabel(lblSub);
		lblSub.setHorizontalAlignment(SwingConstants.CENTER);
		lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
		main.add(lblSub);

		main.add(Box.createVerticalStrut(20));

		// Autor
		addRow(main, I18n.t("lbl_autor_row"),    "Jordi Casals");
		addRow(main, I18n.t("lbl_llicencia_row"),"GNU General Public License v3");
		addRow(main, I18n.t("lbl_font_row"),     "H2 Database Engine (EPL-1.0)");
		addRow(main, I18n.t("lbl_font_row"),     "MariaDB Connector/J (LGPL-2.1)");

		main.add(Box.createVerticalStrut(16));

		// Fragment de llicència
		JLabel lblLicTitle = new JLabel(I18n.t("lbl_license_section"));
		UIComponents.styleLabel(lblLicTitle);
		lblLicTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		main.add(lblLicTitle);
		main.add(Box.createVerticalStrut(6));

		String licenseText = carregarLicense();
		JTextArea licText = new JTextArea(licenseText);
		licText.setLineWrap(true);
		licText.setWrapStyleWord(true);
		licText.setEditable(false);
		licText.setFont(UITheme.FONT_SMALL);
		licText.setBackground(UITheme.palette().bgMain());
		licText.setForeground(UITheme.palette().textMid());
		licText.setBorder(new EmptyBorder(6, 8, 6, 8));
		JScrollPane licScroll = new JScrollPane(licText);
		licScroll.setPreferredSize(new Dimension(0, 130));
		licScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
		licScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
		licScroll.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		main.add(licScroll);

		main.add(Box.createVerticalStrut(12));

		// Botó d'enllaç al codi font
		JButton btnSource = new JButton(I18n.t("btn_source_github"));
		UIComponents.styleSecondaryButton(btnSource);
		btnSource.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnSource.setMaximumSize(new Dimension(200, 32));
		btnSource.addActionListener(e -> {
			try {
				Desktop.getDesktop().browse(new URI(SOURCE_URL));
			} catch (Exception ex) { new herramienta.DialegError(ex).mostrarErrorMessage(); }
		});
		main.add(btnSource);
		main.add(Box.createVerticalStrut(8));

		add(main, BorderLayout.CENTER);

		JButton btnTancar = new JButton(I18n.t("btn_close"));
		UIComponents.styleAccentButton(btnTancar);
		btnTancar.addActionListener(e -> dispose());
		JPanel bottom = new JPanel();
		bottom.setBackground(UITheme.palette().bgPanel());
		bottom.setBorder(new EmptyBorder(0, 32, 16, 32));
		bottom.add(btnTancar);
		add(bottom, BorderLayout.SOUTH);

		getRootPane().registerKeyboardAction(
			e -> dispose(),
			javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
			JPanel.WHEN_IN_FOCUSED_WINDOW);
	}

	private void addRow(JPanel parent, String key, String value) {
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(UITheme.palette().bgPanel());
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel k = new JLabel(key + ":");
		UIComponents.styleLabel(k);
		k.setPreferredSize(new Dimension(80, 0));
		k.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel v = new JLabel(value);
		v.setFont(UITheme.fontBase());
		v.setForeground(UITheme.palette().textDark());
		row.add(k, BorderLayout.WEST);
		row.add(v, BorderLayout.CENTER);
		parent.add(row);
		parent.add(Box.createVerticalStrut(4));
	}

	private static String carregarLicense() {
		try (var in = QuantADialeg.class.getResourceAsStream("/LICENSE")) {
			if (in != null) return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception ignored) {}
		return "GNU General Public License v3 — veure el fitxer LICENSE per a detalls.";
	}
}
