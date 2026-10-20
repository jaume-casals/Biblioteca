package checkBiblio;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import domini.ControladorDomini;
import domini.Llibre;
import persistencia.contract.EscritorBiblioteca;
import presentacio.panells.PanelMostrarBiblioteca;
import presentacio.util.Icones;
import herramienta.ui.UITheme;

/**
 * Prova visual de fum: arrenca l'aplicació real, hi posa dades fictícies i
 * en captura la finestra en various estats.
 *
 * <p>No toca ni la base de dades ni la configuració reals: el {@code user.home}
 * apunta a un directori temporal i la base de dades és una H2 en memòria.
 *
 * <p>Ús:
 * <pre>
 *   java -cp bin:lib/* checkBiblio.ProvaVisual [sortida]
 * </pre>
 */
public final class ProvaVisual {

    private ProvaVisual() {}

    public static void main(String[] args) throws Exception {
        File sortida = new File(args.length > 0 ? args[0] : "build/captures");
        sortida.mkdirs();

        // ── Aïllament: perfil temporal i base de dades en memòria ───────────
        File perfilTemporal = Files.createTempDirectory("biblioteca-prova-visual").toFile();
        System.setProperty("user.home", perfilTemporal.getAbsolutePath());
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:provaVisual;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");

        CapturaPantalla.init();

        Thread appThread = new Thread(() -> {
            try { main.Executable.main(new String[]{"--swing"}); }
            catch (Exception e) { System.err.println("APP ERROR: " + e); }
        }, "biblioteca-visual");
        appThread.setDaemon(true);
        appThread.start();

        JFrame frame = SuportTestUi.waitForMainFrame(20000);
        if (frame == null) {
            System.err.println("FATAL: la finestra principal no ha aparegut");
            System.exit(1);
        }
        CapturaPantalla.settle();

        // ── Dades fictícies ──────────────────────────────────────────────────
        sembrar(frame);
        refrescar(frame);
        CapturaPantalla.settle();

        List<File> captures = new ArrayList<>();

        // 1) Vista taula, tema clar
        frame.setSize(1440, 900);
        CapturaPantalla.settle();
        captures.add(capturar(frame, sortida, "01-taula-clar"));

        // 2) Vista galeria — es prem el botó de la barra d'eines, que és el
        //    camí real que fa servir l'usuari (i que carrega els llibres).
        PanelMostrarBiblioteca panell = SuportTestUi.findComponent(frame, PanelMostrarBiblioteca.class);
        javax.swing.AbstractButton btnGaleria = SuportTestUi.findBtnIn(frame, "Galeria", "Galería", "Gallery");
        if (btnGaleria != null) SwingUtilities.invokeAndWait(btnGaleria::doClick);
        CapturaPantalla.settle();
        captures.add(capturar(frame, sortida, "02-galeria-clar"));

        // 3) Tema fosc
        SwingUtilities.invokeAndWait(() -> {
            UITheme.posarTheme(UITheme.Tema.DARK);
            if (panell != null) panell.aplicarTheme();
        });
        CapturaPantalla.settle();
        captures.add(capturar(frame, sortida, "03-galeria-fosc"));

        // 4) Tema fosc, vista taula
        javax.swing.AbstractButton btnTaula = SuportTestUi.findBtnIn(frame, "Taula", "Tabla", "Table");
        if (btnTaula != null) SwingUtilities.invokeAndWait(btnTaula::doClick);
        CapturaPantalla.settle();
        captures.add(capturar(frame, sortida, "04-taula-fosc"));

        // 5) Tema clar, finestra petita
        SwingUtilities.invokeAndWait(() -> {
            UITheme.posarTheme(UITheme.Tema.LIGHT);
            panell.aplicarTheme();
        });
        frame.setSize(900, 620);
        CapturaPantalla.settle();
        // El redimensionament arriba com a esdeveniment: cal deixar que
        // l'EDT el processi abans de capturar.
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        CapturaPantalla.settle();
        captures.add(capturar(frame, sortida, "05-finestra-petita-clar"));

        frame.setSize(1440, 900);
        CapturaPantalla.settle();

        // 6) Diàleg de llibre nou
        capturarDialog(frame, sortida, new String[]{"+ Nou llibre", "+ Nuevo libro", "+ New book"},
            new String[]{"llibre", "libro", "book"}, "06-dialeg-nou-llibre.png");

        // 7) Diàleg de configuració
        capturarDialog(frame, sortida, new String[]{"Configuració", "Configuración", "Settings"},
            new String[]{"onfiguració", "onfiguracion", "ettings"}, "07-configuracio.png");

        System.out.println("CAPTURES:");
        for (File f : captures) System.out.println("  " + f.getPath());
        System.out.println("CAPÇALERA: " + CapturaPantalla.llegirCapcalera(frame));
        System.exit(0);
    }


    /**
     * Obre un diàleg prement un botó i en captura la finestra.
     *
     * <p>Els diàlegs són modals: el clic s'ha de fer des d'un fil que no
     * sigui l'EDT, o bé l'EDT queda dins el bucle modal del diàleg.
     */
    private static void capturarDialog(JFrame frame, File sortida, String[] botons,
                                       String[] fragmentsTitol, String nomSortida) {
        javax.swing.AbstractButton btn = SuportTestUi.findBtnIn(frame, botons);
        if (btn == null) {
            System.err.println("no s'ha trobat el botó per a " + nomSortida);
            return;
        }
        Thread clic = new Thread(btn::doClick, "clic-" + nomSortida);
        clic.setDaemon(true);
        clic.start();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        java.awt.Window w = null;
        for (String frag : fragmentsTitol) {
            w = SuportTestUi.findWindowByTitle(frag);
            if (w != null) break;
        }
        if (w == null) {
            System.err.println("no s'ha trobat la finestra per a " + nomSortida);
            return;
        }
        CapturaPantalla.settle();
        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        try {
            CapturaPantalla.capturarFinestra(w, new File(sortida, nomSortida));
        } catch (Exception e) {
            System.err.println("no s'ha pogut capturar " + nomSortida + ": " + e);
        }
    }

    private static File capturar(JFrame frame, File dir, String nom) throws Exception {
        SwingUtilities.invokeAndWait(() -> { frame.toFront(); frame.requestFocus(); });
        Thread.sleep(400);
        File f = new File(dir, nom + ".png");
        CapturaPantalla.capturarFinestra(frame, f);
        System.out.println("capturat: " + f.getPath());
        return f;
    }

    /**
     * Recarrega la vista després de sembrar.
     *
     * <p>Es prem el element "Tots els llibres" de la barra lateral: així la
     * vista torna a consultar la base de dades i mostra els llibres sembrats.
     */
    private static void refrescar(JFrame frame) throws Exception {
        // Es torna a carregar la biblioteca des del controlador principal: així
        // la vista mostra els llibres sembrats.
        presentacio.controladors.ControladorMarcPrincipal ctrl =
            presentacio.controladors.ControladorMarcPrincipal.getInstance();
        if (ctrl == null) {
            System.err.println("no s'ha trobat el controlador principal");
            return;
        }
        ctrl.refrescarBiblioteca();
    }

    /** Insereix llibres ficticis perquè la galeria i la taula tinguin contingut. */
    private static void sembrar(JFrame frame) {
        try {
            EscritorBiblioteca cd = ControladorDomini.getInstance();
            String[][] dades = {
                {"El nom de la rosa",              "Umberto Eco",              "1980", "7.5", "12.90"},
                {"Cien años de soledad",           "Gabriel García Márquez",  "1967", "9.1", "10.50"},
                {"La sombra del viento",           "Carlos Ruiz Zafón",       "2001", "8.4", "14.20"},
                {"Ficciones",                     "Jorge Luis Borges",       "1944", "9.6", "11.00"},
                {"El Hobbit",                      "J. R. R. Tolkien",         "1937", "9.2", "13.75"},
                {"Un món feliç",                   "Aldous Huxley",            "1932", "8.8", "9.90"},
                {"La metamorfi",                  "Franz Kafka",              "1915", "8.1", "10.10"},
                {"El guardaplatos",                "José Saramago",            "1997", "8.9", "12.00"},
                {"Orgullo y prejuicio",            "Jane Austen",              "1813", "8.3", "8.75"},
                {"Pedro Páramo",                   "Juan Antonio de Zunzunegui", "1955", "7.2", "11.30"},
                {"2666",                           "Roberto Bolaño",           "2004", "9.0", "15.00"},
                {"Rayuela",                        "Julio Cortázar",           "1963", "8.7", "12.40"},
                {"La ciudad de los prodigios",     "Carlos Ruiz Zafón",       "1999", "8.4", "13.50"},
                {"El corazón de las tinieblas",    "Stephen King",             "2014", "7.8", "14.90"},
                {"Sapiens",                        "Yuval Noah Harari",        "2011", "8.6", "16.75"},
                {"Neu!",                           "Friedrich Nietzsche",      "1882", "6.9", "9.50"},
                {"Matar a un ruiseñor",            "Harper Lee",                "1960", "9.0", "11.80"},
                {"El principi",                    "Antoine de Saint-Exupéry",  "1943", "9.3", "10.20"},
                {"Les ones de la terra",           "A. J. Cronin",              "1935", "7.4", "9.90"},
                {"1984",                           "George Orwell",            "1949", "9.4", "10.60"}
            };
            long isbn = 9780000000001L;
            for (String[] d : dades) {
                Llibre l = new Llibre(isbn++, d[0], d[1], Integer.valueOf(d[2]),
                    "Descripció de prova per a la revisió visual de la interfície.",
                    Double.valueOf(d[3]), Double.valueOf(d[4]),
                    Boolean.valueOf(isbn % 3 == 0), null);
                l.posarPagines(320);
                l.posarPaginesLlegides(isbn % 4 == 0 ? 140 : 20);
                cd.afegirLlibre(l);
            }
            // Un llibre amb títol molt llarg, per comprovar que no es trunca.
            Llibre llarg = new Llibre(isbn++, "Un títol deliberadament molt llarg per provar el truncament",
                "Un autor amb un nom igualment força llarg per comprovar el comportament", 2024,
                null, null, null, Boolean.FALSE, null);
            cd.afegirLlibre(llarg);
            System.out.println("sembrats " + dades.length + " llibres");
        } catch (Exception e) {
            System.err.println("sembra fallida: " + e);
        }
    }
}
