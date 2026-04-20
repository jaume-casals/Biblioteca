package presentacio;

import java.awt.Frame;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import herramienta.Config;
import herramienta.UITheme;

public class ConfiguracioDialog extends JDialog {

	public ConfiguracioDialog(Frame parent) { this(parent, null); }

	public ConfiguracioDialog(Frame parent, Runnable onReapply) {
		super(parent, "Configuració", true);
		setResizable(false);
		setBounds(0, 0, 490, 530);
		setLocationRelativeTo(parent);
		getContentPane().setLayout(null);
		getContentPane().setBackground(UITheme.BG_PANEL);

		int lx = 20, fx = 185, fw = 280, fh = 32, lh = 22;

		// ── Base de dades ────────────────────────────────────────────────────
		JLabel lblSeccioDb = new JLabel("Base de dades");
		lblSeccioDb.setFont(UITheme.FONT_BOLD);
		lblSeccioDb.setForeground(UITheme.ACCENT);
		lblSeccioDb.setBounds(lx, 16, 300, lh);
		getContentPane().add(lblSeccioDb);

		JLabel lblTipus = new JLabel("Tipus");
		UITheme.styleLabel(lblTipus);
		lblTipus.setBounds(lx, 48, 155, lh);
		getContentPane().add(lblTipus);

		JComboBox<String> cmbType = new JComboBox<>(new String[]{
			"Integrada — H2 (sense servidor)",
			"Externa — MariaDB / MySQL"
		});
		cmbType.setSelectedIndex("h2".equals(Config.getDbType()) ? 0 : 1);
		cmbType.setFont(UITheme.FONT_BASE);
		cmbType.setBounds(fx, 48, fw, fh);
		getContentPane().add(cmbType);

		JLabel lblHost = new JLabel("Servidor (host)");
		UITheme.styleLabel(lblHost);
		lblHost.setBounds(lx, 92, 155, lh);
		getContentPane().add(lblHost);

		JTextField txtHost = new JTextField(Config.getDbHost());
		UITheme.styleField(txtHost);
		txtHost.setBounds(fx, 92, fw, fh);
		getContentPane().add(txtHost);

		JLabel lblUser = new JLabel("Usuari");
		UITheme.styleLabel(lblUser);
		lblUser.setBounds(lx, 136, 155, lh);
		getContentPane().add(lblUser);

		JTextField txtUser = new JTextField(Config.getDbUser());
		UITheme.styleField(txtUser);
		txtUser.setBounds(fx, 136, fw, fh);
		getContentPane().add(txtUser);

		JLabel lblPass = new JLabel("Contrasenya");
		UITheme.styleLabel(lblPass);
		lblPass.setBounds(lx, 180, 155, lh);
		getContentPane().add(lblPass);

		JPasswordField txtPass = new JPasswordField(Config.getDbPassword());
		UITheme.styleField(txtPass);
		txtPass.setBounds(fx, 180, fw, fh);
		getContentPane().add(txtPass);

		JLabel lblDbNote = new JLabel("Els canvis de BD requereixen reiniciar l'aplicació.");
		lblDbNote.setFont(UITheme.FONT_SMALL);
		lblDbNote.setForeground(UITheme.TEXT_MID);
		lblDbNote.setBounds(lx, 222, 440, lh);
		getContentPane().add(lblDbNote);

		// Enable/disable external fields based on selected type
		Runnable updateFields = () -> {
			boolean external = cmbType.getSelectedIndex() == 1;
			txtHost.setEnabled(external);
			txtUser.setEnabled(external);
			txtPass.setEnabled(external);
		};
		updateFields.run();
		cmbType.addActionListener(e -> updateFields.run());

		// ── Imatges ──────────────────────────────────────────────────────────
		JLabel lblSeccioImg = new JLabel("Imatges");
		lblSeccioImg.setFont(UITheme.FONT_BOLD);
		lblSeccioImg.setForeground(UITheme.ACCENT);
		lblSeccioImg.setBounds(lx, 258, 300, lh);
		getContentPane().add(lblSeccioImg);

		JLabel lblImgDir = new JLabel("Carpeta per defecte");
		UITheme.styleLabel(lblImgDir);
		lblImgDir.setBounds(lx, 290, 155, lh);
		getContentPane().add(lblImgDir);

		JTextField txtImgDir = new JTextField(Config.getDefaultImgDir());
		UITheme.styleField(txtImgDir);
		txtImgDir.setBounds(fx, 290, fw - 82, fh);
		getContentPane().add(txtImgDir);

		JButton btnExplorar = new JButton("...");
		UITheme.styleSecondaryButton(btnExplorar);
		btnExplorar.setBounds(fx + fw - 78, 290, 78, fh);
		btnExplorar.addActionListener(e -> {
			JFileChooser fc = new JFileChooser(txtImgDir.getText());
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
				txtImgDir.setText(fc.getSelectedFile().getAbsolutePath());
		});
		getContentPane().add(btnExplorar);

		// ── Aparença ─────────────────────────────────────────────────────────
		JLabel lblSeccioFont = new JLabel("Aparença");
		lblSeccioFont.setFont(UITheme.FONT_BOLD);
		lblSeccioFont.setForeground(UITheme.ACCENT);
		lblSeccioFont.setBounds(lx, 334, 300, lh);
		getContentPane().add(lblSeccioFont);

		JLabel lblFont = new JLabel("Mida de lletra");
		UITheme.styleLabel(lblFont);
		lblFont.setBounds(lx, 366, 155, lh);
		getContentPane().add(lblFont);

		String[] fontSizeLabels = {"Petita", "Mitjana", "Gran"};
		String[] fontSizeKeys   = {"small",  "medium",  "large"};
		JComboBox<String> cmbFont = new JComboBox<>(fontSizeLabels);
		String curSize = Config.getFontSize();
		for (int i = 0; i < fontSizeKeys.length; i++)
			if (fontSizeKeys[i].equals(curSize)) { cmbFont.setSelectedIndex(i); break; }
		cmbFont.setFont(UITheme.FONT_BASE);
		cmbFont.setBounds(fx, 366, 140, fh);
		getContentPane().add(cmbFont);

		// ── Buttons ──────────────────────────────────────────────────────────
		JButton btnGuardar = new JButton("Guardar");
		UITheme.styleAccentButton(btnGuardar);
		btnGuardar.setBounds(lx, 418, 215, 42);
		btnGuardar.addActionListener(e -> {
			boolean external = cmbType.getSelectedIndex() == 1;
			if (external) {
				String host = txtHost.getText().trim();
				String user = txtUser.getText().trim();
				if (host.isEmpty() || user.isEmpty()) {
					JOptionPane.showMessageDialog(this,
						"El servidor i l'usuari no poden estar buits.",
						"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				Config.setDbHost(host);
				Config.setDbUser(user);
				Config.setDbPassword(new String(txtPass.getPassword()));
			}
			Config.setDbType(external ? "mariadb" : "h2");
			String imgDir = txtImgDir.getText().trim();
			if (!imgDir.isEmpty()) Config.setDefaultImgDir(imgDir);
			Config.setFontSize(fontSizeKeys[Math.max(0, cmbFont.getSelectedIndex())]);
			if (onReapply != null) onReapply.run();
			JOptionPane.showMessageDialog(this,
				"Configuració guardada.\nReinicia l'app per aplicar canvis de BD.",
				"Guardat", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		});
		getContentPane().add(btnGuardar);

		JButton btnCancel = new JButton("Cancel·lar");
		UITheme.styleSecondaryButton(btnCancel);
		btnCancel.setBounds(255, 418, 215, 42);
		btnCancel.addActionListener(e -> dispose());
		getContentPane().add(btnCancel);

		getRootPane().registerKeyboardAction(
			e -> dispose(),
			javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
			JPanel.WHEN_IN_FOCUSED_WINDOW);
	}
}
