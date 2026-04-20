package herramienta;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

public class DialogoError {

    private final String titol;
    private final String missatge;
    private final String detalls;

    public DialogoError(Exception e) {
        this("Error", e);
    }

    public DialogoError(String titol, Exception e) {
        this.titol = titol;

        StringBuilder msg = new StringBuilder();
        if (e.getMessage() != null) msg.append(e.getMessage());
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            msg.append("\n\nCausa: ").append(e.getCause().getMessage());
        }
        this.missatge = msg.toString();

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        this.detalls = sw.toString();
    }

    public DialogoError(String titol, String missatge) {
        this.titol = titol;
        this.missatge = missatge;
        this.detalls = "";
    }

    public void showErrorMessage() {
        if (GraphicsEnvironment.isHeadless() || Boolean.getBoolean("biblioteca.test")) {
            System.err.println("[" + titol + "] " + missatge);
            if (!detalls.isBlank()) System.err.println(detalls);
            return;
        }

        // Top label with error title
        JLabel lblTitol = new JLabel(titol, SwingConstants.LEFT);
        lblTitol.setFont(UITheme.FONT_BOLD);
        lblTitol.setForeground(new java.awt.Color(0xC0392B));
        lblTitol.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));

        // Selectable text area — shows message + full stack trace
        String full = missatge + (detalls.isBlank() ? "" : "\n\n─── Stack trace ───\n" + detalls);
        JTextArea textArea = new JTextArea(full);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(UITheme.FONT_SMALL);
        textArea.setBackground(UITheme.BG_PANEL);
        textArea.setForeground(UITheme.TEXT_DARK);
        textArea.setCaretColor(UITheme.TEXT_DARK);
        textArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        textArea.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(520, 220));
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));

        JButton btnTancar = new JButton("Tancar");
        UITheme.styleSecondaryButton(btnTancar);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(UITheme.BG_MAIN);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        btnPanel.add(btnTancar);

        JDialog dialog = new JDialog();
        dialog.setTitle(titol);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(UITheme.BG_MAIN);
        dialog.setLayout(new BorderLayout());
        dialog.add(lblTitol, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(true);

        btnTancar.addActionListener(ev -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(btnTancar);

        dialog.setVisible(true);
    }
}
