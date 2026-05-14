package main;

import api.ApiServer;
import domini.ControladorDomini;
import interficie.BibliotecaWriter;
import herramienta.Config;
import herramienta.UITheme;
import presentacio.MainFrameControl;
import presentacio.MainFramePanel;
import presentacio.ModeSelectorDialog;
import presentacio.SplashScreen;

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
        BibliotecaWriter cd = ControladorDomini.getInstance();
        ApiServer server = new ApiServer(port, cd);
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

                SplashScreen splash = new SplashScreen();
                splash.show();

                // Load DB in background, then open main window
                Thread loader = new Thread(() -> {
                    final BibliotecaWriter[] cdBox = {null};
                    try {
                        cdBox[0] = ControladorDomini.getInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    EventQueue.invokeLater(() -> {
                        splash.hide();
                        try {
                            MainFramePanel vista = new MainFramePanel();
                            MainFrameControl.getInstance(vista, cdBox[0]).setVisible(true);
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
