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

import herramienta.UITheme;

public class AboutDialog extends JDialog {

	public AboutDialog(Frame parent) {
		super(parent, "Sobre Biblioteca", true);
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(420, 480);
		setLocationRelativeTo(parent);
		getContentPane().setBackground(UITheme.BG_PANEL);
		setLayout(new BorderLayout(0, 0));

		JPanel main = new JPanel();
		main.setBackground(UITheme.BG_PANEL);
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		main.setBorder(new EmptyBorder(24, 32, 16, 32));

		// App name
		JLabel lblApp = new JLabel("Biblioteca");
		lblApp.setFont(UITheme.FONT_BOLD.deriveFont(22f));
		lblApp.setForeground(UITheme.ACCENT);
		lblApp.setAlignmentX(Component.CENTER_ALIGNMENT);
		main.add(lblApp);

		main.add(Box.createVerticalStrut(4));

		JLabel lblSub = new JLabel("Gestió personal de llibres");
		UITheme.styleLabel(lblSub);
		lblSub.setHorizontalAlignment(SwingConstants.CENTER);
		lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
		main.add(lblSub);

		main.add(Box.createVerticalStrut(20));

		// Author
		addRow(main, "Autor",    "Jordi Casals");
		addRow(main, "Llicència","GNU General Public License v3");
		addRow(main, "Font",     "H2 Database Engine (EPL-1.0)");
		addRow(main, "Font",     "MariaDB Connector/J (LGPL-2.1)");

		main.add(Box.createVerticalStrut(16));

		// License excerpt
		JLabel lblLicTitle = new JLabel("Llicència (GPL v3 — extracte)");
		UITheme.styleLabel(lblLicTitle);
		lblLicTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		main.add(lblLicTitle);
		main.add(Box.createVerticalStrut(6));

		JTextArea licText = new JTextArea(
			"This program is free software: you can redistribute it and/or modify " +
			"it under the terms of the GNU General Public License as published by " +
			"the Free Software Foundation, either version 3 of the License, or " +
			"(at your option) any later version.\n\n" +
			"This program is distributed in the hope that it will be useful, " +
			"but WITHOUT ANY WARRANTY; without even the implied warranty of " +
			"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
			"GNU General Public License for more details.");
		licText.setLineWrap(true);
		licText.setWrapStyleWord(true);
		licText.setEditable(false);
		licText.setFont(UITheme.FONT_SMALL);
		licText.setBackground(UITheme.BG_MAIN);
		licText.setForeground(UITheme.TEXT_MID);
		licText.setBorder(new EmptyBorder(6, 8, 6, 8));
		JScrollPane licScroll = new JScrollPane(licText);
		licScroll.setPreferredSize(new Dimension(0, 130));
		licScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
		licScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
		licScroll.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		main.add(licScroll);

		main.add(Box.createVerticalStrut(12));

		// Source link button
		JButton btnSource = new JButton("Codi font (GitHub)");
		UITheme.styleSecondaryButton(btnSource);
		btnSource.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnSource.setMaximumSize(new Dimension(200, 32));
		btnSource.addActionListener(e -> openUrl("https://github.com/jaume-casals/Biblioteca"));
		main.add(btnSource);
		main.add(Box.createVerticalStrut(8));

		add(main, BorderLayout.CENTER);

		JButton btnTancar = new JButton("Tancar");
		UITheme.styleAccentButton(btnTancar);
		btnTancar.addActionListener(e -> dispose());
		JPanel bottom = new JPanel();
		bottom.setBackground(UITheme.BG_PANEL);
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
		row.setBackground(UITheme.BG_PANEL);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel k = new JLabel(key + ":");
		UITheme.styleLabel(k);
		k.setPreferredSize(new Dimension(80, 0));
		k.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel v = new JLabel(value);
		v.setFont(UITheme.FONT_BASE);
		v.setForeground(UITheme.TEXT_DARK);
		row.add(k, BorderLayout.WEST);
		row.add(v, BorderLayout.CENTER);
		parent.add(row);
		parent.add(Box.createVerticalStrut(4));
	}
}
