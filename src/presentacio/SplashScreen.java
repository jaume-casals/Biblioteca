package presentacio;

import herramienta.UITheme;
import javax.swing.*;
import java.awt.*;

public class SplashScreen {

    private final JDialog dialog;

    public SplashScreen() {
        dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT, 2),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        JLabel lbl = new JLabel("Carregant biblioteca...", SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_TITLE);
        lbl.setForeground(UITheme.ACCENT);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);

        panel.add(lbl, BorderLayout.CENTER);
        panel.add(bar, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(360, 100);
        dialog.setLocationRelativeTo(null);
    }

    public void show() { dialog.setVisible(true); }
    public void hide() { dialog.dispose(); }
}
