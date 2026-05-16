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
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
            new herramienta.DialogoError("Uncaught exception in " + t.getName(), (Exception)(e instanceof Exception ? e : new RuntimeException(e))).showErrorMessage());

        String mode = resolveMode(args);
        if (mode == null) return; // user closed dialog

        Runtime.getRuntime().addShutdownHook(new Thread(
            persistencia.ControladorPersistencia::resetForProfileSwitch));

        if ("web".equals(mode)) {
            System.out.println("Starting database...");
            try {
                ControladorDomini.getInstance();
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
            startWeb();
        } else {
            startSwingWithSplash();
        }
    }

    private static String resolveMode(String[] args) {
        if (args.length > 0) {
            if ("--web".equals(args[0]))   { Config.setLastMode("web");   return "web"; }
            if ("--swing".equals(args[0])) { Config.setLastMode("swing"); return "swing"; }
        }
        String last = Config.getLastMode();
        ModeSelectorDialog.Mode choice = ModeSelectorDialog.prompt(last);
        if (choice == ModeSelectorDialog.Mode.WEB)   { Config.setLastMode("web");   return "web"; }
        if (choice == ModeSelectorDialog.Mode.SWING) { Config.setLastMode("swing"); return "swing"; }
        return null; // CANCELLED
    }

    private static void startWeb() throws Exception {
        int port = Config.getApiPort();
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
                UITheme.setTheme(Config.getTheme());
                UITheme.rebuildFonts(Config.getFontSize());
                UIManager.put("nimbusBase",                UITheme.ACCENT);
                UIManager.put("nimbusBlueGrey",            UITheme.isDark() ? new Color(0x3D4451) : new Color(0x5D8AA8));
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
                    java.util.concurrent.atomic.AtomicReference<BibliotecaWriter> cdRef = new java.util.concurrent.atomic.AtomicReference<>();
                    try {
                        cdRef.set(ControladorDomini.getInstance());
                    } catch (RuntimeException e) {
                        javax.swing.JOptionPane.showMessageDialog(null, e.getMessage());
                        System.exit(1);
                    }
                    EventQueue.invokeLater(() -> {
                        splash.hide();
                        try {
                            MainFramePanel vista = new MainFramePanel();
                            MainFrameControl.getInstance(vista, cdRef.get()).setVisible(true);
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
