package presentacio;

import java.awt.Frame;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
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

	public ConfiguracioDialog(Frame parent) { this(parent, null, null); }

	public ConfiguracioDialog(Frame parent, ConfiguracioDialogListener listener) {
		this(parent, listener, null);
	}

	public ConfiguracioDialog(Frame parent, ConfiguracioDialogListener listener, BibliotecaWriter cd) {
		super(parent, t("modal_settings"), true);
		this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();

		JPanel dbSection = buildDbSection(listener);
		JPanel imgSection = buildImagesSection();
		JPanel appearanceSection = buildAppearanceSection();
		JPanel languageSection = buildLanguageSection();
		JPanel dataSection = buildDataSection(listener);
		JPanel buttonBar = buildButtonBar(listener,
			(JComboBox<String>) findComponentByClientProperty(dbSection, "cmbType"),
			(JTextField) findComponentByClientProperty(dbSection, "txtHost"),
			(JTextField) findComponentByClientProperty(dbSection, "txtUser"),
			(JPasswordField) findComponentByClientProperty(dbSection, "txtPass"),
			(JTextField) findComponentByClientProperty(imgSection, "txtImgDir"),
			(JComboBox<String>) findComponentByClientProperty(appearanceSection, "cmbTheme"),
			(JComboBox<String>) findComponentByClientProperty(appearanceSection, "cmbFont"),
			(JComboBox<String>) findComponentByClientProperty(appearanceSection, "cmbCurrency"),
			(JComboBox<String>) findComponentByClientProperty(languageSection, "cmbLang"),
			(JTextField) findComponentByClientProperty(appearanceSection, "txtDefVal"));

		JPanel content = new JPanel();
		content.setBackground(UITheme.BG_PANEL);
		GroupLayout layout = new GroupLayout(content);
		content.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(dbSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
			.addComponent(imgSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
			.addComponent(appearanceSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
			.addComponent(languageSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
			.addComponent(dataSection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
			.addComponent(buttonBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		);

		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(dbSection)
			.addComponent(imgSection)
			.addComponent(appearanceSection)
			.addComponent(languageSection)
			.addComponent(dataSection)
			.addComponent(buttonBar)
		);

		setContentPane(content);
		setResizable(true);
		pack();
		setMinimumSize(getSize());
		setLocationRelativeTo(parent);

		getRootPane().registerKeyboardAction(
			e -> dispose(),
			javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
			JPanel.WHEN_IN_FOCUSED_WINDOW);
	}

	/** Refresh form fields from {@link Config} (e.g. after external/API changes). */
	public void reloadFromConfig() {
		Config.reload();
		JPanel root = (JPanel) getContentPane();
		JComboBox<String> cmbType = (JComboBox<String>) findComponentByClientProperty(root, "cmbType");
		JTextField txtHost = (JTextField) findComponentByClientProperty(root, "txtHost");
		JTextField txtUser = (JTextField) findComponentByClientProperty(root, "txtUser");
		JPasswordField txtPass = (JPasswordField) findComponentByClientProperty(root, "txtPass");
		JTextField txtImgDir = (JTextField) findComponentByClientProperty(root, "txtImgDir");
		JComboBox<String> cmbTheme = (JComboBox<String>) findComponentByClientProperty(root, "cmbTheme");
		JComboBox<String> cmbFont = (JComboBox<String>) findComponentByClientProperty(root, "cmbFont");
		JComboBox<String> cmbCurrency = (JComboBox<String>) findComponentByClientProperty(root, "cmbCurrency");
		JComboBox<String> cmbLang = (JComboBox<String>) findComponentByClientProperty(root, "cmbLang");
		JTextField txtDefVal = (JTextField) findComponentByClientProperty(root, "txtDefVal");
		if (cmbType != null) cmbType.setSelectedIndex("h2".equals(Config.getDbType()) ? 0 : 1);
		if (txtHost != null) txtHost.setText(Config.getDbHost());
		if (txtUser != null) txtUser.setText(Config.getDbUser());
		if (txtPass != null) txtPass.setText(Config.getDbPassword());
		if (txtImgDir != null) txtImgDir.setText(Config.getDefaultImgDir());
		if (cmbTheme != null) cmbTheme.setSelectedIndex(herramienta.UITheme.getTheme().ordinal());
		if (cmbFont != null) {
			String fs = Config.getFontSize();
			cmbFont.setSelectedIndex("small".equals(fs) ? 0 : "large".equals(fs) ? 2 : 1);
		}
		if (cmbCurrency != null) cmbCurrency.setSelectedItem(Config.getCurrencySymbol());
		if (cmbLang != null) {
			String lang = Config.getLang();
			cmbLang.setSelectedIndex("es".equals(lang) ? 1 : "en".equals(lang) ? 2 : 0);
		}
		if (txtDefVal != null) txtDefVal.setText(String.valueOf(Config.getDefaultValoracio()));
	}

	private JPanel buildDbSection(ConfiguracioDialogListener listener) {
		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		GroupLayout gl = new GroupLayout(panel);
		panel.setLayout(gl);
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);

		JLabel lblSeccio = new JLabel(t("lbl_database"));
		lblSeccio.setFont(UITheme.fontBold());
		lblSeccio.setForeground(UITheme.ACCENT);

		JLabel lblTipus = new JLabel(t("lbl_type"));
		UITheme.styleLabel(lblTipus);
		JComboBox<String> cmbType = new JComboBox<>(new String[]{
			t("opt_h2_full"), t("opt_mariadb_full")
		});
		cmbType.setSelectedIndex("h2".equals(Config.getDbType()) ? 0 : 1);
		cmbType.setFont(UITheme.fontBase());
		cmbType.putClientProperty("id", "cmbType");

		JLabel lblHost = new JLabel(t("lbl_server"));
		UITheme.styleLabel(lblHost);
		JTextField txtHost = new JTextField(Config.getDbHost());
		UITheme.styleField(txtHost);
		txtHost.putClientProperty("id", "txtHost");

		JLabel lblUser = new JLabel(t("lbl_user"));
		UITheme.styleLabel(lblUser);
		JTextField txtUser = new JTextField(Config.getDbUser());
		UITheme.styleField(txtUser);
		txtUser.putClientProperty("id", "txtUser");

		JLabel lblPass = new JLabel(t("lbl_password"));
		UITheme.styleLabel(lblPass);
		JPasswordField txtPass = new JPasswordField(Config.getDbPassword());
		UITheme.styleField(txtPass);
		txtPass.putClientProperty("id", "txtPass");

		JLabel lblDbNote = new JLabel(t("lbl_db_restart"));
		lblDbNote.setFont(UITheme.FONT_SMALL);
		lblDbNote.setForeground(UITheme.TEXT_MID);

		JButton btnTestConn = new JButton(t("btn_test_connection"));
		UITheme.styleSecondaryButton(btnTestConn);
		btnTestConn.addActionListener(e -> {
			boolean external = cmbType.getSelectedIndex() == 1;
			String dbType = external ? "mariadb" : "h2";
			try {
				java.util.Properties testProps = new java.util.Properties();
				testProps.setProperty("dbType", dbType);
				if (external) {
					testProps.setProperty("dbHost", txtHost.getText().trim());
					testProps.setProperty("dbUser", txtUser.getText().trim());
					testProps.setProperty("dbPassword", new String(txtPass.getPassword()));
				}
				java.sql.Connection conn = persistencia.ServerConect.testConnection(testProps);
				conn.close();
				JOptionPane.showMessageDialog(this, t("dlg_connection_ok"), t("dlg_connection_title"), JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, t("dlg_connection_fail") + "\n" + ex.getMessage(),
					t("dlg_connection_title"), JOptionPane.ERROR_MESSAGE);
			}
		});

		Runnable updateFields = () -> {
			boolean external = cmbType.getSelectedIndex() == 1;
			txtHost.setEnabled(external);
			txtUser.setEnabled(external);
			txtPass.setEnabled(external);
		};
		updateFields.run();
		cmbType.addActionListener(e -> updateFields.run());

		gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(lblSeccio)
			.addGroup(gl.createSequentialGroup()
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
					.addComponent(lblTipus)
					.addComponent(lblHost)
					.addComponent(lblUser)
					.addComponent(lblPass))
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(cmbType, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
					.addComponent(txtHost, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
					.addComponent(txtUser, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
					.addComponent(txtPass, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)))
			.addComponent(lblDbNote)
			.addComponent(btnTestConn, GroupLayout.Alignment.TRAILING)
		);

		gl.setVerticalGroup(gl.createSequentialGroup()
			.addComponent(lblSeccio)
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblTipus).addComponent(cmbType))
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblHost).addComponent(txtHost))
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblUser).addComponent(txtUser))
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblPass).addComponent(txtPass))
			.addComponent(lblDbNote)
			.addComponent(btnTestConn)
		);

		return panel;
	}

	private JPanel buildImagesSection() {
		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		GroupLayout gl = new GroupLayout(panel);
		panel.setLayout(gl);
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);

		JLabel lblSeccio = new JLabel(t("lbl_images"));
		lblSeccio.setFont(UITheme.fontBold());
		lblSeccio.setForeground(UITheme.ACCENT);

		JLabel lblImgDir = new JLabel(t("lbl_default_folder"));
		UITheme.styleLabel(lblImgDir);
		JTextField txtImgDir = new JTextField(Config.getDefaultImgDir());
		UITheme.styleField(txtImgDir);
		txtImgDir.putClientProperty("id", "txtImgDir");

		JButton btnExplorar = new JButton("...");
		UITheme.styleSecondaryButton(btnExplorar);
		btnExplorar.addActionListener(e -> {
			JFileChooser fc = new JFileChooser(txtImgDir.getText());
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
				txtImgDir.setText(fc.getSelectedFile().getAbsolutePath());
		});

		gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(lblSeccio)
			.addGroup(gl.createSequentialGroup()
				.addComponent(lblImgDir)
				.addComponent(txtImgDir, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
				.addComponent(btnExplorar))
		);

		gl.setVerticalGroup(gl.createSequentialGroup()
			.addComponent(lblSeccio)
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblImgDir).addComponent(txtImgDir).addComponent(btnExplorar))
		);

		return panel;
	}

	private JPanel buildAppearanceSection() {
		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		GroupLayout gl = new GroupLayout(panel);
		panel.setLayout(gl);
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);

		JLabel lblSeccio = new JLabel(t("lbl_appearance"));
		lblSeccio.setFont(UITheme.fontBold());
		lblSeccio.setForeground(UITheme.ACCENT);

		JLabel lblTheme = new JLabel(t("lbl_theme"));
		UITheme.styleLabel(lblTheme);
		herramienta.UITheme.Theme[] themeValues = herramienta.UITheme.Theme.values();
		String[] themeLabels = new String[themeValues.length];
		for (int i = 0; i < themeValues.length; i++) themeLabels[i] = themeValues[i].displayName();
		JComboBox<String> cmbTheme = new JComboBox<>(themeLabels);
		herramienta.UITheme.Theme curTheme = Config.getTheme();
		for (int i = 0; i < themeValues.length; i++)
			if (themeValues[i] == curTheme) { cmbTheme.setSelectedIndex(i); break; }
		cmbTheme.setFont(UITheme.fontBase());
		cmbTheme.putClientProperty("id", "cmbTheme");

		JLabel lblFont = new JLabel(t("lbl_font_size"));
		UITheme.styleLabel(lblFont);
		String[] fontSizeLabels = {t("opt_small"), t("opt_medium"), t("opt_large")};
		String[] fontSizeKeys = {"small", "medium", "large"};
		JComboBox<String> cmbFont = new JComboBox<>(fontSizeLabels);
		String curSize = Config.getFontSize();
		for (int i = 0; i < fontSizeKeys.length; i++)
			if (fontSizeKeys[i].equals(curSize)) { cmbFont.setSelectedIndex(i); break; }
		cmbFont.setFont(UITheme.fontBase());
		cmbFont.putClientProperty("id", "cmbFont");

		JLabel lblCurrency = new JLabel(t("lbl_currency_symbol"));
		UITheme.styleLabel(lblCurrency);
		String[] currencySymbols = {"€", "$", "£", "¥", "CHF"};
		JComboBox<String> cmbCurrency = new JComboBox<>(currencySymbols);
		cmbCurrency.setFont(UITheme.fontBase());
		String curCurrency = Config.getCurrencySymbol();
		for (int i = 0; i < currencySymbols.length; i++)
			if (currencySymbols[i].equals(curCurrency)) { cmbCurrency.setSelectedIndex(i); break; }
		cmbCurrency.putClientProperty("id", "cmbCurrency");

		JLabel lblDefVal = new JLabel(t("lbl_default_rating"));
		UITheme.styleLabel(lblDefVal);
		JTextField txtDefVal = new JTextField(String.valueOf(Config.getDefaultValoracio()));
		UITheme.styleField(txtDefVal);
		txtDefVal.putClientProperty("id", "txtDefVal");

		JLabel lblDefValHint = new JLabel("(0.0 – 10.0)");
		UITheme.styleLabel(lblDefValHint);

		gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(lblSeccio)
			.addGroup(gl.createSequentialGroup()
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
					.addComponent(lblTheme)
					.addComponent(lblFont)
					.addComponent(lblCurrency)
					.addComponent(lblDefVal))
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(cmbTheme, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
					.addComponent(cmbFont, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
					.addComponent(cmbCurrency, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
					.addGroup(gl.createSequentialGroup()
						.addComponent(txtDefVal, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblDefValHint))))
		);

		gl.setVerticalGroup(gl.createSequentialGroup()
			.addComponent(lblSeccio)
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblTheme).addComponent(cmbTheme))
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblFont).addComponent(cmbFont))
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblCurrency).addComponent(cmbCurrency))
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblDefVal).addComponent(txtDefVal).addComponent(lblDefValHint))
		);

		return panel;
	}

	private JPanel buildLanguageSection() {
		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		GroupLayout gl = new GroupLayout(panel);
		panel.setLayout(gl);
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);

		JLabel lblLang = new JLabel(t("lbl_language_setting"));
		UITheme.styleLabel(lblLang);
		String[] langLabels = {t("opt_lang_ca"), t("opt_lang_es"), t("opt_lang_en")};
		String[] langKeys = {"ca", "es", "en"};
		JComboBox<String> cmbLang = new JComboBox<>(langLabels);
		String curLang = Config.getLang();
		for (int i = 0; i < langKeys.length; i++)
			if (langKeys[i].equals(curLang)) { cmbLang.setSelectedIndex(i); break; }
		cmbLang.setFont(UITheme.fontBase());
		cmbLang.putClientProperty("id", "cmbLang");

		gl.setHorizontalGroup(gl.createSequentialGroup()
			.addComponent(lblLang)
			.addComponent(cmbLang, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
		);

		gl.setVerticalGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
			.addComponent(lblLang).addComponent(cmbLang)
		);

		return panel;
	}

	private JPanel buildDataSection(ConfiguracioDialogListener listener) {
		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		GroupLayout gl = new GroupLayout(panel);
		panel.setLayout(gl);
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);

		JLabel lblSeccio = new JLabel(t("lbl_data"));
		lblSeccio.setFont(UITheme.fontBold());
		lblSeccio.setForeground(UITheme.ACCENT);

		JLabel lblDbSize = new JLabel(t("lbl_db_size"));
		UITheme.styleLabel(lblDbSize);
		JLabel lblDbSizeVal = new JLabel("...");
		lblDbSizeVal.setFont(UITheme.fontBase());
		lblDbSizeVal.setForeground(UITheme.TEXT_DARK);
		new SwingWorker<Long, Void>() {
			@Override protected Long doInBackground() { return cd.getDbSizeBytes(); }
			@Override protected void done() {
				try {
					long dbBytes = get();
					String dbSizeStr = dbBytes < 0 ? "N/D" :
						dbBytes < 1024 * 1024 ? String.format(java.util.Locale.ROOT, "%.1f KB", dbBytes / 1024.0) :
						String.format(java.util.Locale.ROOT, "%.2f MB", dbBytes / (1024.0 * 1024.0));
					lblDbSizeVal.setText(dbSizeStr);
				} catch (Exception ignored) {
					lblDbSizeVal.setText("N/D");
				}
			}
		}.execute();

		JButton btnBuidar = new JButton(t("btn_clear_library"));
		btnBuidar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		btnBuidar.setBackground(UITheme.DANGER);
		btnBuidar.setForeground(java.awt.Color.WHITE);
		btnBuidar.setFont(UITheme.fontBold());
		btnBuidar.setFocusPainted(false);
		btnBuidar.setBorderPainted(false);
		btnBuidar.setOpaque(true);
		btnBuidar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
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
				if (listener != null) listener.onRefreshData();
				JOptionPane.showMessageDialog(this, t("dlg_clear_done"),
					t("dlg_clear_done_title"), JOptionPane.INFORMATION_MESSAGE);
				dispose();
			} catch (Exception ex) {
				new herramienta.DialogoError(ex).showErrorMessage();
			}
		});

		JButton btnPerfils = new JButton(t("btn_gestio_perfils"));
		UITheme.styleSecondaryButton(btnPerfils);
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
			int confirm = JOptionPane.showConfirmDialog(this,
				t("dlg_perfil_confirmar", chosen), t("dlg_perfil_confirmar_title"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (confirm != JOptionPane.YES_OPTION) return;
			Config.setDbProfile(chosen);
			String finalChosen = chosen;
			JOptionPane.showMessageDialog(this,
				t("dlg_perfil_canviat", finalChosen),
				t("dlg_perfil_bd_info_title"), JOptionPane.INFORMATION_MESSAGE);
		});

		gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(lblSeccio)
			.addGroup(gl.createSequentialGroup()
				.addComponent(lblDbSize)
				.addComponent(lblDbSizeVal))
			.addComponent(btnBuidar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
			.addComponent(btnPerfils, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		);

		gl.setVerticalGroup(gl.createSequentialGroup()
			.addComponent(lblSeccio)
			.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lblDbSize).addComponent(lblDbSizeVal))
			.addComponent(btnBuidar)
			.addComponent(btnPerfils)
		);

		return panel;
	}

	private JPanel buildButtonBar(ConfiguracioDialogListener listener,
			JComboBox<String> cmbType, JTextField txtHost, JTextField txtUser,
			JPasswordField txtPass, JTextField txtImgDir,
			JComboBox<String> cmbTheme, JComboBox<String> cmbFont,
			JComboBox<String> cmbCurrency, JComboBox<String> cmbLang,
			JTextField txtDefVal) {

		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		GroupLayout gl = new GroupLayout(panel);
		panel.setLayout(gl);
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);

		herramienta.UITheme.Theme[] themeValues = herramienta.UITheme.Theme.values();
		String[] fontSizeKeys = {"small", "medium", "large"};
		String[] langKeys = {"ca", "es", "en"};

		JButton btnGuardar = new JButton(t("btn_save"));
		UITheme.styleAccentButton(btnGuardar);
		btnGuardar.addActionListener(e -> {
			boolean external = cmbType.getSelectedIndex() == 1;
			String prevDbType = Config.getDbType();
			if (external) {
				String host = txtHost.getText().trim();
				String user = txtUser.getText().trim();
				if (host.isEmpty() || user.isEmpty()) {
					JOptionPane.showMessageDialog(ConfiguracioDialog.this,
						t("dlg_db_validation"),
						t("dlg_error_title"), JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			Config.withBatch(() -> {
				if (external) {
					Config.setDbHost(txtHost.getText().trim());
					Config.setDbUser(txtUser.getText().trim());
					Config.setDbPassword(new String(txtPass.getPassword()));
				}
				Config.setDbType(external ? "mariadb" : "h2");
				String imgDir = txtImgDir.getText().trim();
				if (!imgDir.isEmpty()) Config.setDefaultImgDir(imgDir);
				herramienta.UITheme.Theme selTheme = themeValues[Math.max(0, cmbTheme.getSelectedIndex())];
				UITheme.setTheme(selTheme);
				Config.setTheme(selTheme);
				Config.setFontSize(fontSizeKeys[Math.max(0, cmbFont.getSelectedIndex())]);
				Config.setCurrencySymbol((String) cmbCurrency.getSelectedItem());
				Config.setLang(langKeys[Math.max(0, cmbLang.getSelectedIndex())]);
				I18n.applySwingOptionPane();
				try { Config.setDefaultValoracio(Double.parseDouble(txtDefVal.getText().trim())); }
				catch (NumberFormatException ignored) {}
			});
			if (listener != null) listener.onThemeChange();
			String newDbType = external ? "mariadb" : "h2";
			boolean dbTypeChanged = !newDbType.equals(prevDbType);
			if (dbTypeChanged) {
				int restart = JOptionPane.showConfirmDialog(this,
					t("dlg_db_restart_msg"),
					t("dlg_db_restart_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (restart == JOptionPane.YES_OPTION) {
					System.exit(0);
				}
			} else {
				JOptionPane.showMessageDialog(this,
					t("dlg_config_saved"),
					t("dlg_config_saved_title"), JOptionPane.INFORMATION_MESSAGE);
			}
			dispose();
		});

		JButton btnCancel = new JButton(t("btn_cancel"));
		UITheme.styleSecondaryButton(btnCancel);
		btnCancel.addActionListener(e -> dispose());

		gl.setHorizontalGroup(gl.createSequentialGroup()
			.addComponent(btnGuardar, GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
			.addComponent(btnCancel, GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
		);

		gl.setVerticalGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
			.addComponent(btnGuardar, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
			.addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
		);

		return panel;
	}

	private static javax.swing.JComponent findComponentByClientProperty(java.awt.Container panel, String id) {
		for (java.awt.Component c : panel.getComponents()) {
			if (c instanceof javax.swing.JComponent jc) {
				if (id.equals(jc.getClientProperty("id"))) return jc;
			}
			if (c instanceof java.awt.Container cont) {
				javax.swing.JComponent found = findComponentByClientProperty(cont, id);
				if (found != null) return found;
			}
		}
		return null;
	}
}
