package presentacio;

import herramienta.I18n;
import herramienta.UITheme;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SplashScreen {

    private static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "splash-hide");
            t.setDaemon(true);
            return t;
        });

    private final JDialog dialog;
    private long mostrarTime = 0;
    private static final long MIN_DISPLAY_MS = 500;

    public SplashScreen() {
        dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setAlwaysOnTop(true);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(UITheme.palette().bgPanel());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.palette().accent(), 2),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        JLabel lbl = new JLabel(I18n.t("splash_loading"), SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_TITLE);
        lbl.setForeground(UITheme.palette().accent());

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);

        panel.add(lbl, BorderLayout.CENTER);
        panel.add(bar, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(360, 100);
        dialog.setLocationRelativeTo(null);
    }

    public void show() {
        if (dialog.isVisible()) return;
        mostrarTime = System.currentTimeMillis();
        dialog.setVisible(true);
    }

    public void hide() {
        if (!dialog.isVisible()) return;
        long elapsed = System.currentTimeMillis() - mostrarTime;
        long remaining = MIN_DISPLAY_MS - elapsed;
        if (remaining > 0) {
            ScheduledFuture<?> future = SCHEDULER.schedule(
                () -> SwingUtilities.invokeLater(dialog::dispose),
                remaining, TimeUnit.MILLISECONDS);
        } else {
            dialog.dispose();
        }
    }

    /** Emergency cleanup: dispose the dialog regardless of timing. */
    public void forceHide() {
        dialog.dispose();
    }
}
