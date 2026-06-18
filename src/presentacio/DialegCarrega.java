package presentacio;

import herramienta.UITheme;
import javax.swing.*;
import java.awt.*;

public class DialegCarrega {

    private final JDialog dialog;
    private final JLabel label;
    private final JProgressBar bar;
    private long mostrarTime = 0;

    public DialegCarrega(Frame owner, String message) {
        dialog = new JDialog(owner, true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setBackground(UITheme.palette().bgPanel());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.palette().accent(), 2),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)));

        label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(UITheme.fontBase());
        label.setForeground(UITheme.palette().textDark());

        bar = new JProgressBar();
        bar.setIndeterminate(true);

        panel.add(label, BorderLayout.CENTER);
        panel.add(bar, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(360, 100);
        dialog.setLocationRelativeTo(owner);
    }

    public DialegCarrega(Frame owner) {
        this(owner, "");
    }

    public void setMessage(String message) {
        label.setText(message);
    }

    public void posarResult(int value) {
        bar.setIndeterminate(false);
        bar.setValue(value);
    }

    public void setMaximum(int max) {
        bar.setMaximum(max);
    }

    public void show() {
        if (dialog.isVisible()) return;
        mostrarTime = System.currentTimeMillis();
        dialog.setVisible(true);
    }

    public void hide() {
        dialog.dispose();
    }

    public void amagarAfterMinimum(long minMs) {
        if (!dialog.isVisible()) return;
        long elapsed = System.currentTimeMillis() - mostrarTime;
        long remaining = minMs - elapsed;
        if (remaining > 0) {
            Timer t = new Timer((int) remaining, e -> dialog.dispose());
            t.setRepeats(false);
            t.start();
        } else {
            dialog.dispose();
        }
    }
}