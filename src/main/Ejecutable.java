package main;

import domini.ControladorDomini;
import interficie.BibliotecaWriter;
import herramienta.Config;
import herramienta.DialogoError;
import herramienta.FontSize;
import herramienta.UITheme;
import presentacio.MainFrameControl;
import presentacio.MainFramePanel;
import presentacio.SplashScreen;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

public class Ejecutable {

    private static final Logger LOG = Logger.getLogger(Ejecutable.class.getName());
    private static SplashScreen splashRef;

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--web".equals(args[0])) {
            System.err.println("Web mode was removed. Run the Swing desktop app instead.");
            System.exit(1);
        }
        // --swing accepted as no-op for backward-compatible launch scripts

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOG.log(Level.SEVERE, "Uncaught exception in thread " + t.getName(), e);
            if (splashRef != null) splashRef.forceHide();
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.err.println("Fatal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
            DialogoError err = e instanceof Exception ? new DialogoError((Exception) e) : new DialogoError(new RuntimeException("Fatal error: " + e.getClass().getSimpleName(), e));
            err.showErrorMessage();
        });

        main.ShutdownHooks.register(persistencia.ControladorPersistencia::resetForProfileSwitch);

        startSwingWithSplash();
    }

    private static void startSwingWithSplash() {
        EventQueue.invokeLater(() -> {
            try {
                UITheme.setTheme(Config.getTheme());
                UITheme.rebuildFonts(FontSize.fromKey(Config.getFontSize()));
                UIManager.put("nimbusBase",                UITheme.palette().accent());
                UIManager.put("nimbusBlueGrey",            UITheme.isDark() ? new Color(0x3D4451) : new Color(0x5D8AA8));
                UIManager.put("control",                   UITheme.palette().bgMain());
                UIManager.put("text",                      UITheme.palette().textDark());
                UIManager.put("nimbusFocus",               UITheme.palette().accent());
                UIManager.put("nimbusSelectionBackground", UITheme.palette().accent());
                UIManager.put("nimbusSelectedText",        Color.WHITE);
                UIManager.put("defaultFont",              UITheme.fontBase());
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                UIManager.put("Table.alternateRowColor",   UITheme.palette().tableAlt());

                SplashScreen splash = new SplashScreen();
                splashRef = splash;
                splash.show();

                Thread loader = new Thread(() -> {
                    java.util.concurrent.atomic.AtomicReference<BibliotecaWriter> cdRef = new java.util.concurrent.atomic.AtomicReference<>();
                    try {
                        cdRef.set(ControladorDomini.getInstance());
                    } catch (RuntimeException e) {
                        final String msg = e.getMessage();
                        LOG.log(Level.SEVERE, "Failed to initialise ControladorDomini", e);
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
                            LOG.log(Level.SEVERE, "Failed to start main frame", e);
                        }
                    });
                });
                loader.setDaemon(true);
                loader.start();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to start Swing UI", e);
            }
        });
    }
}
