package main;

import domini.ControladorDomini;
import persistencia.contract.EscritorBiblioteca;
import herramienta.config.Configuracio;
import herramienta.io.SeedorBaseDades;
import herramienta.ui.DialegError;
import herramienta.config.MidaLletra;
import herramienta.ui.UITheme;
import presentacio.controladors.ControladorMarcPrincipal;
import presentacio.panells.PanelMarcPrincipal;
import presentacio.panells.PantallaInici;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

import persistencia.internal.ControladorPersistencia;
public class Executable {

    private static final Logger LOG = Logger.getLogger(Executable.class.getName());
    private static volatile PantallaInici splashRef;

    /** Tanca la pantalla d'inici (si n'hi ha) passant pel EDT quan
     *  l'invocador no hi és. Sense això, una excepció no capturada en
     *  un fil de fons pot manipular un component Swing fora de l'EDT
     *  i llançar IllegalComponentStateException. */
    private static void hideSplashSafely() {
        if (splashRef == null) return;
        if (EventQueue.isDispatchThread()) splashRef.forceHide();
        else EventQueue.invokeLater(() -> { if (splashRef != null) splashRef.forceHide(); });
    }

    /** Registra l'error, amaga la pantalla d'inici, mostra el diàleg d'error
     *  passant per l'EDT i surt amb codi 1. Usat pels camins fatals del
     *  loader (fil de fons) i de la continuació EDT quan fallen les fases
     *  d'inicialització. */
    private static void fatalExit(String logMsg, Throwable e, Runnable dialog) {
        LOG.log(Level.SEVERE, logMsg, e);
        hideSplashSafely();
        EventQueue.invokeLater(() -> {
            dialog.run();
            System.exit(1);
        });
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            // Primer el missatge, després el throwable — el logger
            // marca el throwable com a causa (la traça va darrere del
            // missatge) i el resultat es llegeix com
            // "Uncaught … <cause>".
            LOG.log(Level.SEVERE, "Excepció no capturada al fil " + t.getName(), e);
            hideSplashSafely();
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.err.println("Error fatal: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
            // La signatura del handler d'excepcions no captades rep
            // Throwable, no Exception; alguns consumidors llancen Error
            // (StackOverflowError, OutOfMemoryError) que DialegError
            // accepta via la sobrecàrrega (Throwable).
            DialegError err = e instanceof Exception
                ? new DialegError((Exception) e)
                : new DialegError(new RuntimeException("Error fatal: " + e.getClass().getSimpleName(), e));
            err.mostrarErrorMessage();
        });

        main.ShutdownHooks.register(persistencia.internal.ControladorPersistencia::reinicialitzarForProfileSwitch);

        iniciarSwingWithSplash();
    }

    private static void iniciarSwingWithSplash() {
        EventQueue.invokeLater(() -> {
            try {
                UITheme.posarTheme(Configuracio.obtenirTheme());
                UITheme.rebuildFonts(MidaLletra.fromKey(Configuracio.obtenirFontSize()));
                UIManager.put("nimbusBase",                UITheme.palette().accent());
                UIManager.put("nimbusBlueGrey",            UITheme.esDark() ? new Color(0x3D4451) : new Color(0x5D8AA8));
                UIManager.put("control",                   UITheme.palette().bgMain());
                UIManager.put("text",                      UITheme.palette().textDark());
                UIManager.put("nimbusFocus",               UITheme.palette().accent());
                UIManager.put("nimbusSelectionBackground", UITheme.palette().accent());
                UIManager.put("nimbusSelectedText",        Color.WHITE);
                UIManager.put("defaultFont",              UITheme.fontBase());
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                UIManager.put("Table.alternateRowColor",   UITheme.palette().tableAlt());

                PantallaInici splash = new PantallaInici();
                splashRef = splash;
                splash.show();

                Thread loader = new Thread(() -> {
                    java.util.concurrent.atomic.AtomicReference<EscritorBiblioteca> cdRef = new java.util.concurrent.atomic.AtomicReference<>();
                    try {
                        SeedorBaseDades.seedSiCal();
                        cdRef.set(ControladorDomini.getInstance());
                    } catch (RuntimeException e) {
                        fatalExit("No s'ha pogut inicialitzar ControladorDomini", e,
                            () -> javax.swing.JOptionPane.showMessageDialog(null, e.getMessage()));
                        return;
                    }
                    EventQueue.invokeLater(() -> {
                        splash.hide();
                        try {
                            PanelMarcPrincipal vista = new PanelMarcPrincipal();
                            ControladorMarcPrincipal.getInstance(vista, cdRef.get()).setVisible(true);
                        } catch (Exception e) {
                            // Superfície l'error via DialegError perquè
                            // l'usuari vegi què ha fallat en lloc d'una
                            // pantalla en blanc amb el procés penjat.
                            fatalExit("No s'ha pogut iniciar el marc principal", e,
                                () -> new DialegError(e).mostrarErrorMessage());
                        }
                    });
                });
                loader.setDaemon(true);
                loader.start();
            } catch (Exception e) {
                    LOG.log(Level.SEVERE, "No s'ha pogut iniciar la interfície Swing", e);
            }
        });
    }
}
