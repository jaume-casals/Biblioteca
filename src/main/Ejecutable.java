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

    private static SplashScreen splashRef;
    private static volatile boolean webMode;

    public static void main(String[] args) throws Exception {
        boolean isWeb = "--web".equals(args.length > 0 ? args[0] : null);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (webMode) {
                System.err.println("Fatal error on thread " + t.getName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
            if (splashRef != null) splashRef.forceHide();
            if (isWeb || java.awt.GraphicsEnvironment.isHeadless()) {
                System.err.println("Fatal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
            herramienta.DialogoError err = e instanceof Exception ? new herramienta.DialogoError((Exception) e) : new herramienta.DialogoError(new RuntimeException("Fatal error: " + e.getClass().getSimpleName(), e));
            err.showErrorMessage();
        });

        java.util.Optional<String> mode = resolveMode(args);
        if (mode.isEmpty()) {
            System.out.println("No mode selected. Exiting.");
            return;
        }
        String modeStr = mode.get();

        Runtime.getRuntime().addShutdownHook(new Thread(
            persistencia.ControladorPersistencia::resetForProfileSwitch));

        if ("web".equals(modeStr)) {
            webMode = true;
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

    public static java.util.Optional<String> resolveMode(String[] args) {
        if (args.length > 0) {
            if ("--web".equals(args[0]))   { Config.setLastMode("web");   return java.util.Optional.of("web"); }
            if ("--swing".equals(args[0])) { Config.setLastMode("swing"); return java.util.Optional.of("swing"); }
        }
        String last = Config.getLastMode();
        if (last != null) {
            if ("web".equals(last))   return java.util.Optional.of("web");
            if ("swing".equals(last)) return java.util.Optional.of("swing");
        }
        if (Boolean.getBoolean("biblioteca.test")) return java.util.Optional.of("swing");
        ModeSelectorDialog.Mode choice = ModeSelectorDialog.prompt(last);
        if (choice == ModeSelectorDialog.Mode.WEB)   { Config.setLastMode("web");   return java.util.Optional.of("web"); }
        if (choice == ModeSelectorDialog.Mode.SWING) { Config.setLastMode("swing"); return java.util.Optional.of("swing"); }
        return java.util.Optional.empty();
    }

    private static void startWeb() throws Exception {
        int port = Config.getApiPort();
        BibliotecaWriter cd = ControladorDomini.getInstance();
        ApiServer server = new ApiServer(port, cd);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("Biblioteca escoltant a http://localhost:" + port);
        System.out.println("Prem Ctrl+C per aturar.");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try { Desktop.getDesktop().browse(new URI("http://localhost:" + port)); }
            catch (Exception ignored) {}
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
                UIManager.put("defaultFont",              UITheme.FONT_BASE);
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                UIManager.put("Table.alternateRowColor",   UITheme.TABLE_ALT);

                SplashScreen splash = new SplashScreen();
                splashRef = splash;
                splash.show();

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