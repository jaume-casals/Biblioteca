package presentacio.dialegs;

import herramienta.i18n.I18n;
import herramienta.ui.UITheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import persistencia.contract.EscritorBiblioteca;
import presentacio.controladors.ControladorEstacioEscaneig;
import presentacio.listener.EnActualitzarBBDD;

public class EstacioEscaneigDialog extends JDialog {

	private final ControladorEstacioEscaneig controller;
	private final JTextField textField;
	private final JLabel lblStatus;
	private final JLabel lblCounters;
	private final Timer timer;

	public EstacioEscaneigDialog(Frame parent, EscritorBiblioteca cd, EnActualitzarBBDD listener) {
		super(parent, I18n.t("estacio_title"), false);
		this.controller = new ControladorEstacioEscaneig(cd, listener, this::dispose);
		this.controller.setOnResult(this::onControllerResult);
		this.timer = new Timer(2000, e -> resetStatus());
		this.timer.setRepeats(false);

		textField = new JTextField();
		lblStatus = new JLabel(controller.getStatusText());
		lblCounters = new JLabel();

		buildLayout();
		wireListeners();

		setSize(new Dimension(560, 260));
		setMinimumSize(new Dimension(480, 220));
		setLocationRelativeTo(parent);
		updateCounters();
	}

	private void buildLayout() {
		JPanel content = new JPanel();
		content.setBackground(UITheme.palette().bgPanel());
		content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JLabel lblHint = new JLabel("<html>" + herramienta.i18n.Escapers.htmlWithBreaks(I18n.t("estacio_hint")) + "</html>");
		lblHint.setFont(UITheme.fontBase());
		lblHint.setForeground(UITheme.palette().textDark());
		lblHint.setAlignmentX(JComponent.LEFT_ALIGNMENT);

		textField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
		textField.setHorizontalAlignment(JTextField.CENTER);
		textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		textField.setAlignmentX(JComponent.LEFT_ALIGNMENT);

		lblStatus.setFont(UITheme.fontBase().deriveFont(Font.PLAIN, 14f));
		lblStatus.setForeground(controller.getStatusColor());
		lblStatus.setAlignmentX(JComponent.LEFT_ALIGNMENT);

		lblCounters.setFont(UITheme.fontBase().deriveFont(Font.PLAIN, 12f));
		lblCounters.setForeground(UITheme.palette().textMid());
		lblCounters.setAlignmentX(JComponent.LEFT_ALIGNMENT);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		bottom.setOpaque(false);
		JLabel lblShortcut = new JLabel(I18n.t("estacio_shortcut_hint"));
		lblShortcut.setFont(UITheme.fontBase().deriveFont(Font.PLAIN, 11f));
		lblShortcut.setForeground(UITheme.palette().textMid());
		JButton btnClose = new JButton(I18n.t("estacio_btn_close"));
		btnClose.addActionListener(e -> dispose());
		bottom.add(lblShortcut);
		bottom.add(Box.createHorizontalStrut(16));
		bottom.add(btnClose);
		bottom.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		bottom.setMaximumSize(new Dimension(Integer.MAX_VALUE, bottom.getPreferredSize().height));

		content.add(lblHint);
		content.add(Box.createVerticalStrut(10));
		content.add(textField);
		content.add(Box.createVerticalStrut(10));
		content.add(lblStatus);
		content.add(Box.createVerticalStrut(4));
		content.add(lblCounters);
		content.add(Box.createVerticalGlue());
		content.add(bottom);

		setContentPane(content);
	}

	private void wireListeners() {
		textField.addActionListener(e -> {
			String text = textField.getText();
			textField.setText("");
			controller.submitIsbn(text);
		});
		getRootPane().registerKeyboardAction(
			e -> dispose(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);
		addWindowListener(new WindowAdapter() {
			@Override public void windowActivated(WindowEvent e) { textField.requestFocusInWindow(); }
			@Override public void windowClosing(WindowEvent e) { dispose(); }
			@Override public void windowClosed(WindowEvent e) { controller.shutdown(); }
		});
	}

	private void onControllerResult() {
		lblStatus.setText(controller.getStatusText());
		lblStatus.setForeground(controller.getStatusColor());
		updateCounters();
		textField.setText("");
		textField.requestFocusInWindow();
		if (timer.isRunning()) timer.stop();
		timer.start();
	}

	private void resetStatus() {
		lblStatus.setText(I18n.t("estacio_status_idle"));
		lblStatus.setForeground(Color.DARK_GRAY);
	}

	private void updateCounters() {
		lblCounters.setText(I18n.t("estacio_counters",
			controller.getCountAdded(),
			controller.getCountDuplicate(),
			controller.getCountError()));
	}

	public JTextField obtenirTextFieldISBN() { return textField; }
	public ControladorEstacioEscaneig obtenirControlador() { return controller; }
	public JLabel obtenirLblStatus() { return lblStatus; }

	@Override
	public void dispose() {
		timer.stop();
		super.dispose();
	}
}