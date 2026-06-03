package presentacio;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import herramienta.I18n;

/**
 * Startup mode selector — shown before the main window.
 * Returns "web" or "swing". Cross-platform Swing, no console needed.
 */
public class ModeSelectorDialog extends JDialog {

    public enum Mode { WEB, SWING, CANCELLED }

    private Mode result = Mode.CANCELLED;

    private ModeSelectorDialog(Frame owner) {
        super(owner, I18n.t("dlg_mode_selector_title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { result = Mode.SWING; }
        });
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(0x1E2128));
        root.setBorder(new EmptyBorder(28, 36, 24, 36));

        // Title
        JLabel title = new JLabel("Biblioteca", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(new Color(0x7AA2F7));
        title.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel subtitle = new JLabel(I18n.t("lbl_mode_selector_subtitle"), SwingConstants.CENTER);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitle.setForeground(new Color(0xA9B1D6));
        subtitle.setBorder(new EmptyBorder(0, 0, 22, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Buttons
        JPanel buttons = new JPanel(new GridLayout(1, 2, 16, 0));
        buttons.setOpaque(false);

        JButton webBtn   = buildModeButton(
            I18n.t("mode_web"),
            I18n.t("mode_web_sub"),
            "🌐",
            new Color(0x7AA2F7)
        );
        JButton swingBtn = buildModeButton(
            I18n.t("mode_desktop"),
            I18n.t("mode_desktop_sub"),
            "🖥",
            new Color(0x9ECE6A)
        );

        webBtn.addActionListener(e -> { result = Mode.WEB;   dispose(); });
        swingBtn.addActionListener(e -> { result = Mode.SWING; dispose(); });

        // Enter = web (first option), Escape = cancelled
        getRootPane().setDefaultButton(webBtn);
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });

        buttons.add(webBtn);
        buttons.add(swingBtn);
        root.add(buttons, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JButton buildModeButton(String label, String sub, String icon, Color accent) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()   ? accent.darker()
                         : getModel().isRollover()  ? new Color(0x2A2D3A)
                         : new Color(0x24283A);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(accent.darker());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Layout: icon + label + subtitle stacked
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(18, 8, 18, 8));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 28));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLbl = new JLabel(label, SwingConstants.CENTER);
        nameLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        nameLbl.setForeground(accent);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLbl.setBorder(new EmptyBorder(6, 0, 2, 0));

        JLabel subLbl = new JLabel(sub, SwingConstants.CENTER);
        subLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        subLbl.setForeground(new Color(0x787C99));
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(iconLbl);
        inner.add(nameLbl);
        inner.add(subLbl);

        btn.setLayout(new BorderLayout());
        btn.add(inner, BorderLayout.CENTER);
        btn.setPreferredSize(new Dimension(160, 110));

        return btn;
    }

    /** Display the dialog and return the chosen mode. defaultMode: "swing" or "web" to pre-select, or null. */
    public static Mode prompt(String defaultMode) {
        if ("swing".equals(defaultMode)) return Mode.SWING;
        if ("web".equals(defaultMode))   return Mode.WEB;
        return prompt();
    }

    /**
     * Blocks on the EDT until the user picks a mode or closes the dialog (CANCELLED).
     * Call only from the EDT or via {@link #prompt(String)} which uses invokeAndWait.
     */
    public static Mode prompt() {
        if (EventQueue.isDispatchThread()) {
            ModeSelectorDialog d = new ModeSelectorDialog(null);
            d.setVisible(true);
            return d.result;
        }
        final Mode[] holder = { Mode.CANCELLED };
        try {
            EventQueue.invokeAndWait(() -> {
                ModeSelectorDialog d = new ModeSelectorDialog(null);
                d.setVisible(true);
                holder[0] = d.result;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return holder[0];
    }
}
