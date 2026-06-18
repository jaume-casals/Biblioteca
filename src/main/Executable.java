package main;

import domini.ControladorDomini;
import interficie.EscritorBiblioteca;
import herramienta.Configuracio;
import herramienta.DialegError;
import herramienta.MidaLletra;
import herramienta.UITheme;
import presentacio.ControladorMarcPrincipal;
import presentacio.PanelMarcPrincipal;
import presentacio.PantallaInici;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

public class Executable {

    private static final Logger LOG = Logger.getLogger(Executable.class.getName());
    private static PantallaInici splashRef;

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            // Primer el missatge, després el throwable — el logger
            // marca el throwable com a causa (la traça va darrere del
            // missatge) i el resultat es llegeix com
            // "Uncaught … <cause>".
            LOG.log(Level.SEVERE, "Excepció no capturada al fil " + t.getName(), e);
            if (splashRef != null) splashRef.forceHide();
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

        main.ShutdownHooks.register(persistencia.ControladorPersistencia::reinicialitzarForProfileSwitch);

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
                        cdRef.set(ControladorDomini.getInstance());
                    } catch (RuntimeException e) {
                        final String msg = e.getMessage();
                        LOG.log(Level.SEVERE, "No s'ha pogut inicialitzar ControladorDomini", e);
                        if (splashRef != null) splashRef.forceHide();
                        EventQueue.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(null, msg);
                            System.exit(1);
                        });
                        return;
                    }
                    EventQueue.invokeLater(() -> {
                        splash.hide();
                        try {
                            PanelMarcPrincipal vista = new PanelMarcPrincipal();
                            ControladorMarcPrincipal.getInstance(vista, cdRef.get()).setVisible(true);
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, "No s'ha pogut iniciar el marc principal", e);
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
