package main;

import domini.ControladorDomini;
import interficie.BibliotecaWriter;
import herramienta.Config;
import herramienta.UITheme;
import presentacio.MainFrameControl;
import presentacio.MainFramePanel;
import presentacio.SplashScreen;

import java.awt.Color;
import java.awt.EventQueue;
import javax.swing.UIManager;

public class Ejecutable {

    private static SplashScreen splashRef;

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--web".equals(args[0])) {
            System.err.println("Web mode was removed. Run the Swing desktop app instead.");
            System.exit(1);
        }
        // --swing accepted as no-op for backward-compatible launch scripts

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (splashRef != null) splashRef.forceHide();
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.err.println("Fatal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
            herramienta.DialogoError err = e instanceof Exception ? new herramienta.DialogoError((Exception) e) : new herramienta.DialogoError(new RuntimeException("Fatal error: " + e.getClass().getSimpleName(), e));
            err.showErrorMessage();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(
            persistencia.ControladorPersistencia::resetForProfileSwitch));

        startSwingWithSplash();
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
                UIManager.put("defaultFont",              UITheme.fontBase());
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
                        final String msg = e.getMessage();
                        EventQueue.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(null, msg);
                            System.exit(1);
                        });
                        return;
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
