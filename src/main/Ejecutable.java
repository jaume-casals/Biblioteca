package main;

import api.ApiServer;
import domini.ControladorDomini;
import herramienta.Config;
import herramienta.UITheme;
import presentacio.MainFrameControl;
import presentacio.MainFramePanel;
import presentacio.ModeSelectorDialog;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.net.URI;
import javax.swing.UIManager;

public class Ejecutable {

    public static void main(String[] args) throws Exception {
        String mode = resolveMode(args);
        if (mode == null) return; // user closed dialog

        if ("web".equals(mode)) {
            System.out.println("Starting database...");
            ControladorDomini.getInstance();
            startWeb();
        } else {
            startSwingWithSplash();
        }
    }

    private static String resolveMode(String[] args) {
        if (args.length > 0) {
            if ("--web".equals(args[0]))   return "web";
            if ("--swing".equals(args[0])) return "swing";
        }
        ModeSelectorDialog.Mode choice = ModeSelectorDialog.prompt();
        if (choice == ModeSelectorDialog.Mode.WEB)   return "web";
        if (choice == ModeSelectorDialog.Mode.SWING) return "swing";
        return null; // CANCELLED
    }

    private static void startWeb() throws Exception {
        int port = 7070;
        ApiServer server = new ApiServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("Biblioteca running at http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI("http://localhost:" + port));
        }
    }

    private static void startSwingWithSplash() {
        EventQueue.invokeLater(() -> {
            try {
                if (Config.isDarkMode()) UITheme.setDark(true);
                UITheme.rebuildFonts(Config.getFontSize());
                UIManager.put("nimbusBase",                UITheme.ACCENT);
                UIManager.put("nimbusBlueGrey",            UITheme.isDark ? new Color(0x3D4451) : new Color(0x5D8AA8));
                UIManager.put("control",                   UITheme.BG_MAIN);
                UIManager.put("text",                      UITheme.TEXT_DARK);
                UIManager.put("nimbusFocus",               UITheme.ACCENT);
                UIManager.put("nimbusSelectionBackground", UITheme.ACCENT);
                UIManager.put("nimbusSelectedText",        Color.WHITE);
                UIManager.put("defaultFont",               UITheme.FONT_BASE);
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                UIManager.put("Table.alternateRowColor",   UITheme.TABLE_ALT);

                // Splash with indeterminate progress bar
                javax.swing.JDialog splash = new javax.swing.JDialog();
                splash.setUndecorated(true);
                splash.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
                javax.swing.JPanel sp = new javax.swing.JPanel(new java.awt.BorderLayout(12, 12));
                sp.setBackground(UITheme.BG_PANEL);
                sp.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(UITheme.ACCENT, 2),
                    javax.swing.BorderFactory.createEmptyBorder(20, 30, 20, 30)));
                javax.swing.JLabel lbl = new javax.swing.JLabel("Carregant biblioteca...", javax.swing.SwingConstants.CENTER);
                lbl.setFont(UITheme.FONT_TITLE);
                lbl.setForeground(UITheme.ACCENT);
                javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
                bar.setIndeterminate(true);
                sp.add(lbl, java.awt.BorderLayout.CENTER);
                sp.add(bar, java.awt.BorderLayout.SOUTH);
                splash.setContentPane(sp);
                splash.pack();
                splash.setSize(360, 100);
                splash.setLocationRelativeTo(null);
                splash.setVisible(true);

                // Load DB in background, then open main window
                Thread loader = new Thread(() -> {
                    try {
                        ControladorDomini.getInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    EventQueue.invokeLater(() -> {
                        splash.dispose();
                        try {
                            MainFramePanel vista = new MainFramePanel();
                            MainFrameControl.getInstance(vista).setVisible(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
                loader.setDaemon(true);
                loader.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
