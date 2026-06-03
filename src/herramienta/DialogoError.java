package herramienta;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

public class DialogoError {

    private static final boolean IS_HEADLESS = GraphicsEnvironment.isHeadless();

    private final String titol;
    private final String missatge;
    private final String detalls;
    private final boolean isValidation;

    public DialogoError(Exception e) {
        this(I18n.t("dlg_error_title"), e);
    }

    public DialogoError(String titol, Exception e) {
        this.isValidation = e instanceof IllegalArgumentException;
        this.titol = isValidation ? I18n.t("dlg_validacio_title") : titol;

        this.missatge = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

        if (isValidation) {
            this.detalls = "";
        } else {
            StringBuilder sb = new StringBuilder();
            if (e.getCause() != null && e.getCause().getMessage() != null)
                sb.append("\n").append(I18n.t("err_causa")).append(" ").append(e.getCause().getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            sb.append("\n\n").append(I18n.t("err_stack_trace")).append("\n").append(sw);
            this.detalls = sb.toString();
        }
    }

    public DialogoError(String titol, String missatge) {
        this.titol = titol;
        this.missatge = missatge;
        this.detalls = "";
        this.isValidation = false;
    }

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final long MAX_LOG_BYTES = 1024 * 1024; // 1 MB

    private static void writeToLog(String titol, String missatge, String detalls) {
        try {
            Path logFile = Path.of(System.getProperty("user.home"), ".biblioteca", "errors.log");
            logFile.getParent().toFile().mkdirs();
            java.io.File f = logFile.toFile();
            if (f.exists() && f.length() > MAX_LOG_BYTES) {
                Path old = logFile.resolveSibling("errors.log.1");
                java.nio.file.Files.move(logFile, old,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true), 4096)) {
                bw.write("[" + LocalDateTime.now().format(LOG_FMT) + "] " + titol + ": " + missatge);
                bw.newLine();
                if (!detalls.isBlank()) { bw.write(detalls); bw.newLine(); }
                bw.write("---");
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    public void showErrorMessage() {
        if (Boolean.getBoolean("biblioteca.test")) return;
        writeToLog(titol, missatge, detalls);
        if (IS_HEADLESS) {
            System.err.println("[" + titol + "] " + missatge);
            return;
        }
        if (isValidation) showValidationDialog();
        else showSystemErrorDialog();
    }

    // ── Validation (user mistake) ─────────────────────────────────────────────

    private void showValidationDialog() {
        Color accent = UITheme.SIDEBAR_ACCENT;

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 11));
        header.setBackground(accent);

        JLabel iconLbl = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));

        JLabel titleLbl = new JLabel(titol);
        titleLbl.setFont(UITheme.FONT_BOLD);
        titleLbl.setForeground(Color.WHITE);

        header.add(iconLbl);
        header.add(titleLbl);

        JLabel msgLbl = new JLabel(
            "<html><body style='width:300px'>" + escHtml(missatge) + "</body></html>");
        msgLbl.setFont(UITheme.FONT_BASE);
        msgLbl.setForeground(UITheme.TEXT_DARK);
        msgLbl.setBorder(BorderFactory.createEmptyBorder(18, 22, 10, 22));

        JButton btnOk = new JButton(I18n.t("btn_ok"));
        btnOk.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnOk.setBackground(accent);
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(UITheme.FONT_BOLD);
        btnOk.setFocusPainted(false);
        btnOk.setBorderPainted(false);
        btnOk.setOpaque(true);
        btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(110, 32));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        btnPanel.setBackground(UITheme.BG_MAIN);
        btnPanel.add(btnOk);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG_MAIN);
        content.add(msgLbl, BorderLayout.CENTER);
        content.add(btnPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog();
        dialog.setTitle(titol);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(header, BorderLayout.NORTH);
        dialog.add(content, BorderLayout.CENTER);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(360, dialog.getHeight()));
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);

        btnOk.addActionListener(ev -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(btnOk);
        dialog.getRootPane().registerKeyboardAction(
            ev -> dialog.dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setVisible(true);
    }

    // ── System / technical error ──────────────────────────────────────────────

    private void showSystemErrorDialog() {
        JLabel lblTitol = new JLabel(titol, SwingConstants.LEFT);
        lblTitol.setFont(UITheme.FONT_BOLD);
        lblTitol.setForeground(UITheme.DANGER);
        lblTitol.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));

        String full = missatge + (detalls.isBlank() ? "" : detalls);
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

        JButton btnTancar = new JButton(I18n.t("btn_close"));
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

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("\n", "<br>");
    }
}
