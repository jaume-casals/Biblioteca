package checkBiblio;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import presentacio.panells.PanelMostrarBiblioteca;
import presentacio.panells.PanelCapcalera;
import presentacio.util.Icones;

/**
 * Eines de captura per a les proves visuals.
 *
 * <p>Executa l'aplicació real contra una base de dades H2 en memòria i
 * en captura la finestra, de manera que les proves visuals es fan sobre el
 * codi que s'executa i no sobre una maqueta.
 *
 * <p>No toca cap base de dades real: la URL d'H2 s'imposa per propietat del
 * sistema i el perfil de configuració és temporal.
 */
public final class CapturaPantalla {

    private CapturaPantalla() {}

    private static Robot robot;

    /**
     * Captura la finestra principal en el tema actual.
     *
     * <p>La captura es fa pintant l'arbre de components damunt d una imatge
     * en memòria, no amb {@code Robot}: així el resultat és determinista,
     * no depèn del gestor de finestres de l'escriptori i es pot fer en un
     * servidor sense X.
     *
     * @param sortida fitxer PNG de sortida
     * @return el fitxer capturat
     */
    public static File capturarFinestra(JFrame frame, File sortida) throws Exception {
        java.awt.Container contingut = frame.getContentPane();
        java.awt.Dimension d = contingut.getSize();
        if (d.width <= 0 || d.height <= 0) d = frame.getSize();
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(frame.getBackground() == null ? java.awt.Color.LIGHT_GRAY : frame.getBackground());
        g.fillRect(0, 0, d.width, d.height);
        contingut.printAll(g);
        g.dispose();
        File fitxer = sortida;
        fitxer.getParentFile().mkdirs();
        ImageIO.write(img, "png", fitxer);
        return fitxer;
    }

    /**
     * Captura una finestra emergent qualsevol.
     *
     * <p>La imatge té la mida de la <em>finestra</em> i el contingut es
     * pinta a la seva posició dins la finestra: així el resultat coincideix
     * amb el que es veu a pantalla, sense retallar els botons del peu.
     */
    public static File capturarFinestra(Window w, File sortida) throws Exception {
        java.awt.Container contingut = (w instanceof javax.swing.JFrame f)
            ? f.getContentPane()
            : (w instanceof javax.swing.JDialog d) ? d.getContentPane() : w;
        // validate() força el layout pendent: sense això, la captura pot
        // reflectir un estat anterior al darrer redimensionament.
        w.validate();
        java.awt.Dimension d = w.getSize();
        if (d.width <= 0 || d.height <= 0) d = contingut.getSize();
        java.awt.Insets ins = w.getInsets();
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(w.getBackground() == null ? java.awt.Color.LIGHT_GRAY : w.getBackground());
        g.fillRect(0, 0, d.width, d.height);
        java.awt.Point pos = contingut.getLocationOnScreen();
        java.awt.Point orig = w.getLocationOnScreen();
        g.translate(pos.x - orig.x, pos.y - orig.y);
        contingut.printAll(g);
        g.dispose();
        File fitxer = sortida;
        fitxer.getParentFile().mkdirs();
        ImageIO.write(img, "png", fitxer);
        return fitxer;
    }

    /** Prepara el robot i l'entorn gràfic. */
    public static void init() throws Exception {
        robot = new Robot();
        robot.setAutoDelay(60);
    }

    /** Llegeix el text visible de la capçalera: títol de la vista i recompte. */
    public static String llegirCapcalera(JFrame frame) {
        PanelMostrarBiblioteca p = SuportTestUi.findComponent(frame, PanelMostrarBiblioteca.class);
        if (p == null) return "(sense panell)";
        PanelCapcalera c = p.obtenirCapcalera();
        return c.obtenirTitol() + " | " + c.obtenirComptador();
    }

    /** Espera que l'aplicació estigui estable. */
    public static void settle() { SuportTestUi.settle(); }

    /** Captures d'un dia concret: permet comparar abans/després. */
    public static String marca() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    /** Llista els fitxers capturats. */
    public static List<String> llistarCaptures(File dir) {
        List<String> out = new ArrayList<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
        if (files != null) for (File f : files) out.add(f.getName());
        return out;
    }
}
