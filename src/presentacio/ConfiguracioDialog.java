package presentacio;

import java.awt.Frame;
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
import herramienta.I18n;
import herramienta.UITheme;
import interficie.BibliotecaWriter;

import static herramienta.I18n.t;

public class ConfiguracioDialog extends JDialog {

	private final BibliotecaWriter cd;

	public ConfiguracioDialog(Frame parent) { this(parent, null, null, null); }

	public ConfiguracioDialog(Frame parent, Runnable onReapply, Runnable onRefreshData) {
		this(parent, onReapply, onRefreshData, null);
	}

	public ConfiguracioDialog(Frame parent, Runnable onReapply, Runnable onRefreshData, BibliotecaWriter cd) {
		super(parent, t("modal_settings"), true);
		this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
		setResizable(false);
		setBounds(0, 0, 490, 776);
		setLocationRelativeTo(parent);
		getContentPane().setLayout(null);
		getContentPane().setBackground(UITheme.BG_PANEL);

		int lx = 20, fx = 185, fw = 280, fh = 32, lh = 22;

		// ── Base de dades ────────────────────────────────────────────────────
		JLabel lblSeccioDb = new JLabel(t("lbl_database"));
		lblSeccioDb.setFont(UITheme.FONT_BOLD);
		lblSeccioDb.setForeground(UITheme.ACCENT);
		lblSeccioDb.setBounds(lx, 16, 300, lh);
		getContentPane().add(lblSeccioDb);

		JLabel lblTipus = new JLabel(t("lbl_type"));
		UITheme.styleLabel(lblTipus);
		lblTipus.setBounds(lx, 48, 155, lh);
		getContentPane().add(lblTipus);

		JComboBox<String> cmbType = new JComboBox<>(new String[]{
			t("opt_h2_full"),
			t("opt_mariadb_full")
		});
		cmbType.setSelectedIndex("h2".equals(Config.getDbType()) ? 0 : 1);
		cmbType.setFont(UITheme.FONT_BASE);
		cmbType.setBounds(fx, 48, fw, fh);
		getContentPane().add(cmbType);

		JLabel lblHost = new JLabel(t("lbl_server"));
		UITheme.styleLabel(lblHost);
		lblHost.setBounds(lx, 92, 155, lh);
		getContentPane().add(lblHost);

		JTextField txtHost = new JTextField(Config.getDbHost());
		UITheme.styleField(txtHost);
		txtHost.setBounds(fx, 92, fw, fh);
		getContentPane().add(txtHost);

		JLabel lblUser = new JLabel(t("lbl_user"));
		UITheme.styleLabel(lblUser);
		lblUser.setBounds(lx, 136, 155, lh);
		getContentPane().add(lblUser);

		JTextField txtUser = new JTextField(Config.getDbUser());
		UITheme.styleField(txtUser);
		txtUser.setBounds(fx, 136, fw, fh);
		getContentPane().add(txtUser);

		JLabel lblPass = new JLabel(t("lbl_password"));
		UITheme.styleLabel(lblPass);
		lblPass.setBounds(lx, 180, 155, lh);
		getContentPane().add(lblPass);

		JPasswordField txtPass = new JPasswordField(Config.getDbPassword());
		UITheme.styleField(txtPass);
		txtPass.setBounds(fx, 180, fw, fh);
		getContentPane().add(txtPass);

		JLabel lblDbNote = new JLabel(t("lbl_db_restart"));
		lblDbNote.setFont(UITheme.FONT_SMALL);
		lblDbNote.setForeground(UITheme.TEXT_MID);
		lblDbNote.setBounds(lx, 222, 440, lh);
		getContentPane().add(lblDbNote);

		Runnable updateFields = () -> {
			boolean external = cmbType.getSelectedIndex() == 1;
			txtHost.setEnabled(external);
			txtUser.setEnabled(external);
			txtPass.setEnabled(external);
		};
		updateFields.run();
		cmbType.addActionListener(e -> updateFields.run());

		// ── Imatges ──────────────────────────────────────────────────────────
		JLabel lblSeccioImg = new JLabel(t("lbl_images"));
		lblSeccioImg.setFont(UITheme.FONT_BOLD);
		lblSeccioImg.setForeground(UITheme.ACCENT);
		lblSeccioImg.setBounds(lx, 258, 300, lh);
		getContentPane().add(lblSeccioImg);

		JLabel lblImgDir = new JLabel(t("lbl_default_folder"));
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
		JLabel lblSeccioFont = new JLabel(t("lbl_appearance"));
		lblSeccioFont.setFont(UITheme.FONT_BOLD);
		lblSeccioFont.setForeground(UITheme.ACCENT);
		lblSeccioFont.setBounds(lx, 334, 300, lh);
		getContentPane().add(lblSeccioFont);

		JLabel lblFont = new JLabel(t("lbl_font_size"));
		UITheme.styleLabel(lblFont);
		lblFont.setBounds(lx, 366, 155, lh);
		getContentPane().add(lblFont);

		String[] fontSizeLabels = {t("opt_small"), t("opt_medium"), t("opt_large")};
		String[] fontSizeKeys   = {"small", "medium", "large"};
		JComboBox<String> cmbFont = new JComboBox<>(fontSizeLabels);
		String curSize = Config.getFontSize();
		for (int i = 0; i < fontSizeKeys.length; i++)
			if (fontSizeKeys[i].equals(curSize)) { cmbFont.setSelectedIndex(i); break; }
		cmbFont.setFont(UITheme.FONT_BASE);
		cmbFont.setBounds(fx, 366, 140, fh);
		getContentPane().add(cmbFont);

		JLabel lblCurrency = new JLabel(t("lbl_currency_symbol"));
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

		JLabel lblDefVal = new JLabel(t("lbl_default_rating"));
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

		// ── Language ─────────────────────────────────────────────────────────
		JLabel lblLang = new JLabel(t("lbl_language_setting"));
		UITheme.styleLabel(lblLang);
		lblLang.setBounds(lx, 490, 160, lh);
		getContentPane().add(lblLang);

		String[] langLabels = {t("opt_lang_ca"), t("opt_lang_es"), t("opt_lang_en")};
		String[] langKeys   = {"ca", "es", "en"};
		JComboBox<String> cmbLang = new JComboBox<>(langLabels);
		String curLang = Config.getLang();
		for (int i = 0; i < langKeys.length; i++)
			if (langKeys[i].equals(curLang)) { cmbLang.setSelectedIndex(i); break; }
		cmbLang.setFont(UITheme.FONT_BASE);
		cmbLang.setBounds(fx, 488, 160, fh);
		getContentPane().add(cmbLang);

		// ── Dades ────────────────────────────────────────────────────────────
		JLabel lblSeccioDades = new JLabel(t("lbl_data"));
		lblSeccioDades.setFont(UITheme.FONT_BOLD);
		lblSeccioDades.setForeground(UITheme.ACCENT);
		lblSeccioDades.setBounds(lx, 544, 300, lh);
		getContentPane().add(lblSeccioDades);

		JLabel lblDbSize = new JLabel(t("lbl_db_size"));
		UITheme.styleLabel(lblDbSize);
		lblDbSize.setBounds(lx, 576, 155, lh);
		getContentPane().add(lblDbSize);

		long dbBytes = cd.getDbSizeBytes();
		String dbSizeStr = dbBytes < 0 ? "N/D" :
			dbBytes < 1024 * 1024 ? String.format("%.1f KB", dbBytes / 1024.0) :
			String.format("%.2f MB", dbBytes / (1024.0 * 1024.0));
		JLabel lblDbSizeVal = new JLabel(dbSizeStr);
		lblDbSizeVal.setFont(UITheme.FONT_BASE);
		lblDbSizeVal.setForeground(UITheme.TEXT_DARK);
		lblDbSizeVal.setBounds(fx, 576, 180, lh);
		getContentPane().add(lblDbSizeVal);

		JButton btnBuidar = new JButton(t("btn_clear_library"));
		btnBuidar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		btnBuidar.setBackground(UITheme.DANGER);
		btnBuidar.setForeground(java.awt.Color.WHITE);
		btnBuidar.setFont(UITheme.FONT_BOLD);
		btnBuidar.setFocusPainted(false);
		btnBuidar.setBorderPainted(false);
		btnBuidar.setOpaque(true);
		btnBuidar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		btnBuidar.setBounds(lx, 610, fw + fx - lx, 36);
		btnBuidar.addActionListener(e -> {
			int r1 = JOptionPane.showConfirmDialog(this,
				t("dlg_confirm_clear_1"),
				t("dlg_confirm_clear_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (r1 != JOptionPane.YES_OPTION) return;
			int r2 = JOptionPane.showConfirmDialog(this,
				t("dlg_confirm_clear_2"),
				t("dlg_confirm_clear_2_title"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
			if (r2 != JOptionPane.YES_OPTION) return;
			try {
				cd.clearAll();
				if (onRefreshData != null) onRefreshData.run();
				JOptionPane.showMessageDialog(this, t("dlg_clear_done"),
					t("dlg_clear_done_title"), JOptionPane.INFORMATION_MESSAGE);
				dispose();
			} catch (Exception ex) {
				new herramienta.DialogoError(ex).showErrorMessage();
			}
		});
		getContentPane().add(btnBuidar);

		// ── DB Profiles ──────────────────────────────────────────────────────
		JButton btnPerfils = new JButton(t("btn_gestio_perfils"));
		UITheme.styleSecondaryButton(btnPerfils);
		btnPerfils.setBounds(lx, 655, fw + fx - lx, 32);
		btnPerfils.setToolTipText(t("tip_gestio_perfils"));
		btnPerfils.addActionListener(e -> {
			if (!"h2".equals(Config.getDbType())) {
				JOptionPane.showMessageDialog(this,
					t("dlg_perfils_h2_only"),
					t("dlg_perfils_bd_title"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			java.util.List<String> profiles = Config.listDbProfiles();
			String[] opts = profiles.toArray(new String[0]);
			String current = Config.getDbProfile();
			String chosen = (String) JOptionPane.showInputDialog(this,
				t("dlg_perfil_actiu", current),
				t("dlg_canviar_perfil_title"), JOptionPane.PLAIN_MESSAGE, null, opts, current);
			if (chosen == null) return;
			chosen = chosen.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
			if (chosen.isEmpty() || chosen.equals(current)) return;
			Config.setDbProfile(chosen);
			String finalChosen = chosen;
			JOptionPane.showMessageDialog(this,
				t("dlg_perfil_canviat", finalChosen),
				t("dlg_perfil_bd_info_title"), JOptionPane.INFORMATION_MESSAGE);
		});
		getContentPane().add(btnPerfils);

		// ── Buttons ──────────────────────────────────────────────────────────
		JButton btnGuardar = new JButton(t("btn_save"));
		UITheme.styleAccentButton(btnGuardar);
		btnGuardar.setBounds(lx, 701, 215, 42);
		btnGuardar.addActionListener(e -> {
			boolean external = cmbType.getSelectedIndex() == 1;
			if (external) {
				String host = txtHost.getText().trim();
				String user = txtUser.getText().trim();
				if (host.isEmpty() || user.isEmpty()) {
					JOptionPane.showMessageDialog(this,
						t("dlg_db_validation"),
						t("dlg_error_title"), JOptionPane.ERROR_MESSAGE);
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
			Config.setLang(langKeys[Math.max(0, cmbLang.getSelectedIndex())]);
			try { Config.setDefaultValoracio(Double.parseDouble(txtDefVal.getText().trim())); }
			catch (NumberFormatException ignored) {}
			if (onReapply != null) onReapply.run();
			JOptionPane.showMessageDialog(this,
				t("dlg_config_saved"),
				t("dlg_config_saved_title"), JOptionPane.INFORMATION_MESSAGE);
			dispose();
		});
		getContentPane().add(btnGuardar);

		JButton btnCancel = new JButton(t("btn_cancel"));
		UITheme.styleSecondaryButton(btnCancel);
		btnCancel.setBounds(255, 701, 215, 42);
		btnCancel.addActionListener(e -> dispose());
		getContentPane().add(btnCancel);

		getRootPane().registerKeyboardAction(
			e -> dispose(),
			javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
			JPanel.WHEN_IN_FOCUSED_WINDOW);
	}
}
