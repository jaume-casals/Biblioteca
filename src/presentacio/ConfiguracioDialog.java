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

	public ConfiguracioDialog(Frame parent) { this(parent, null, null); }

	public ConfiguracioDialog(Frame parent, Runnable onReapply, Runnable onRefreshData) {
		super(parent, "Configuració", true);
		setResizable(false);
		setBounds(0, 0, 490, 692);
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

		JLabel lblCurrency = new JLabel("Símbol de moneda");
		UITheme.styleLabel(lblCurrency);
		lblCurrency.setBounds(lx, 408, 160, lh);
		getContentPane().add(lblCurrency);

		String[] currencySymbols = {"€", "$", "£", "¥", "CHF"};
		JComboBox<String> cmbCurrency = new JComboBox<>(currencySymbols);
		cmbCurrency.setFont(UITheme.FONT_BASE);
		String curCurrency = Config.getCurrencySymbol();
		for (int i = 0; i < currencySymbols.length; i++)
			if (currencySymbols[i].equals(curCurrency)) { cmbCurrency.setSelectedIndex(i); break; }
		cmbCurrency.setBounds(fx, 406, 80, fh);
		getContentPane().add(cmbCurrency);

		JLabel lblDefVal = new JLabel("Valoració per defecte");
		UITheme.styleLabel(lblDefVal);
		lblDefVal.setBounds(lx, 448, 160, lh);
		getContentPane().add(lblDefVal);

		JTextField txtDefVal = new JTextField(String.valueOf(Config.getDefaultValoracio()));
		UITheme.styleField(txtDefVal);
		txtDefVal.setBounds(fx, 446, 80, fh);
		getContentPane().add(txtDefVal);

		JLabel lblDefValHint = new JLabel("(0.0 – 10.0)");
		UITheme.styleLabel(lblDefValHint);
		lblDefValHint.setBounds(fx + 86, 448, 100, lh);
		getContentPane().add(lblDefValHint);

		// ── Dades ────────────────────────────────────────────────────────────
		JLabel lblSeccioDades = new JLabel("Dades");
		lblSeccioDades.setFont(UITheme.FONT_BOLD);
		lblSeccioDades.setForeground(UITheme.ACCENT);
		lblSeccioDades.setBounds(lx, 500, 300, lh);
		getContentPane().add(lblSeccioDades);

		JLabel lblDbSize = new JLabel("Mida de la BD");
		UITheme.styleLabel(lblDbSize);
		lblDbSize.setBounds(lx, 532, 155, lh);
		getContentPane().add(lblDbSize);

		long dbBytes = domini.ControladorDomini.getInstance().getDbSizeBytes();
		String dbSizeStr = dbBytes < 0 ? "N/D" :
			dbBytes < 1024 * 1024 ? String.format("%.1f KB", dbBytes / 1024.0) :
			String.format("%.2f MB", dbBytes / (1024.0 * 1024.0));
		JLabel lblDbSizeVal = new JLabel(dbSizeStr);
		lblDbSizeVal.setFont(UITheme.FONT_BASE);
		lblDbSizeVal.setForeground(UITheme.TEXT_DARK);
		lblDbSizeVal.setBounds(fx, 532, 180, lh);
		getContentPane().add(lblDbSizeVal);

		JButton btnBuidar = new JButton("Buidar tota la biblioteca");
		btnBuidar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		btnBuidar.setBackground(UITheme.DANGER);
		btnBuidar.setForeground(java.awt.Color.WHITE);
		btnBuidar.setFont(UITheme.FONT_BOLD);
		btnBuidar.setFocusPainted(false);
		btnBuidar.setBorderPainted(false);
		btnBuidar.setOpaque(true);
		btnBuidar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		btnBuidar.setBounds(lx, 566, fw + fx - lx, 36);
		btnBuidar.addActionListener(e -> {
			int r1 = JOptionPane.showConfirmDialog(this,
				"Aquesta acció eliminarà TOTS els llibres, llistes i préstecs.\nNo es pot desfer.",
				"Confirmar buidat", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (r1 != JOptionPane.YES_OPTION) return;
			int r2 = JOptionPane.showConfirmDialog(this,
				"Segur? Es perdran totes les dades permanentment.",
				"Confirmació final", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
			if (r2 != JOptionPane.YES_OPTION) return;
			try {
				domini.ControladorDomini.getInstance().clearAll();
				if (onRefreshData != null) onRefreshData.run();
				JOptionPane.showMessageDialog(this, "Biblioteca buidada correctament.",
					"Completat", JOptionPane.INFORMATION_MESSAGE);
				dispose();
			} catch (Exception ex) {
				new herramienta.DialogoError(ex).showErrorMessage();
			}
		});
		getContentPane().add(btnBuidar);

		// ── Buttons ──────────────────────────────────────────────────────────
		JButton btnGuardar = new JButton("Guardar");
		UITheme.styleAccentButton(btnGuardar);
		btnGuardar.setBounds(lx, 617, 215, 42);
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
			Config.setCurrencySymbol((String) cmbCurrency.getSelectedItem());
			try { Config.setDefaultValoracio(Double.parseDouble(txtDefVal.getText().trim())); }
			catch (NumberFormatException ignored) {}
			if (onReapply != null) onReapply.run();
			JOptionPane.showMessageDialog(this,
				"Configuració guardada.\nReinicia l'app per aplicar canvis de BD.",
				"Guardat", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		});
		getContentPane().add(btnGuardar);

		JButton btnCancel = new JButton("Cancel·lar");
		UITheme.styleSecondaryButton(btnCancel);
		btnCancel.setBounds(255, 617, 215, 42);
		btnCancel.addActionListener(e -> dispose());
		getContentPane().add(btnCancel);

		getRootPane().registerKeyboardAction(
			e -> dispose(),
			javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
			JPanel.WHEN_IN_FOCUSED_WINDOW);
	}
}
