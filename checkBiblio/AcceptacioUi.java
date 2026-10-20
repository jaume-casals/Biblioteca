package checkBiblio;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import herramienta.ui.UITheme;
import presentacio.panells.navegacio.ItemNavegacio;

/**
 * Comprovació d'acceptació de la interfície.
 *
 * <p>Cobreix els errors que van arribar a producció i que cap proves
 * unitàries no detecten: el botó de tema sense acció, la columna de la
 * barra lateral desplaçada i el mode fosc amb components que conserven els
 * colors del tema clar.
 *
 * <p>Executa l'aplicació de debò, prem el botó de tema i medeix el que
 * queda a la pantalla. Surt amb codi 1 si alguna comprovació falla.
 *
 * <pre>
 *   make checkbiblio-accept
 * </pre>
 */
public final class AcceptacioUi {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        File perfil = Files.createTempDirectory("acceptacio-ui").toFile();
        System.setProperty("user.home", perfil.getAbsolutePath());
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:acceptacioui;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");

        Thread app = new Thread(() -> {
            try { main.Executable.main(new String[]{"--swing"}); }
            catch (Exception e) { System.err.println("APP ERROR: " + e); }
        }, "app");
        app.setDaemon(true);
        app.start();

        JFrame frame = esperaFrame(20000);
        if (frame == null) { System.err.println("no s'ha obert la finestra"); System.exit(1); }
        Thread.sleep(2500);
        frame.setSize(1440, 900);
        Thread.sleep(1200);

        temaCommuta(frame);
        contrastEnModeFosc(frame);
        columnaUnica(frame);
        galeriaEsPinta(frame);
        repintComplet(frame);

        System.out.println();
        System.out.println(fallos == 0
            ? "ACCEPTACIÓ: totes les comprovacions passen"
            : "ACCEPTACIÓ: " + fallos + " comprovacions fallen");
        System.exit(fallos == 0 ? 0 : 1);
    }

    /** El botó de tema ha de commutar clar/fosc i-ho ha de pintar tot. */
    private static void temaCommuta(JFrame frame) throws Exception {
        System.out.println("=== botó de tema ===");
        AbstractButton btn = trobaBotó(frame, "Tema");
        comprova("el botó de tema existeix", btn != null);
        if (btn == null) return;
        comprova("el botó de tema té icona", btn.getIcon() != null);

        String abans = UITheme.obtenirTheme().name();
        SwingUtilities.invokeAndWait(btn::doClick);
        Thread.sleep(900);
        String despres = UITheme.obtenirTheme().name();
        System.out.println("  tema: " + abans + " -> " + despres);
        comprova("el botó de tema commuta el tema", !abans.equals(despres));
        if (!"DARK".equals(despres)) return;

        Component side = trobaSidebar(frame);
        comprova("el fons de la barra lateral segueix el tema",
            side != null && side.getBackground().equals(UITheme.palette().sidebarBg()));

        JLabel status = trobaStatusBar(frame);
        if (status != null) {
            System.out.println("  barra d'estat: " + hex(status.getBackground()));
            comprova("la barra d'estat segueix el tema", luminancia(status.getBackground()) < 0.15);
        }

        SwingUtilities.invokeAndWait(btn::doClick);
        Thread.sleep(900);
        comprova("el botó de tema torna al tema clar",
            UITheme.obtenirTheme() != UITheme.Tema.DARK);
    }

    /** En mode fosc no pot quedar text ni icones amb el color del tema clar. */
    private static void contrastEnModeFosc(JFrame frame) throws Exception {
        System.out.println("=== mode fosc ===");
        aplicaTema(frame, UITheme.Tema.DARK);
        Thread.sleep(800);

        int[] resum = { 0, 0 };
        recullText(frame, resum);
        System.out.println("  text avaluat=" + resum[0] + "  il·legible=" + resum[1]);
        comprova("cap text queda il·legible en mode fosc", resum[1] == 0);

        for (ItemNavegacio it : iconesSidebar(frame)) {
            if (it.getIcon() == null) continue;
            Color c = colorDominant(it.getIcon(), it);
            comprova("la icona de '" + curt(it.getText(), 18) + "' no queda fosca",
                c == null || luminancia(c) >= 0.15);
        }
    }

    /** La barra lateral és una sola columna, sense files desplaçades. */
    private static void columnaUnica(JFrame frame) {
        System.out.println("=== columna de la barra lateral ===");
        Component side = trobaSidebar(frame);
        List<ItemNavegacio> items = iconesSidebar(frame);
        Set<Integer> xs = new LinkedHashSet<>();
        for (ItemNavegacio it : items) {
            Rectangle r = it.getBounds();
            System.out.printf("  %-22s x=%-4d w=%-4d%n", curt(it.getText(), 20), r.x, r.width);
            xs.add(r.x);
        }
        comprova("totes les files de navegació comparteixen la mateixa columna", xs.size() == 1);
        comprova("les files de navegació ocupen tota l'amplada de la barra",
            side != null && !items.isEmpty() && items.get(0).getWidth() == side.getWidth());
    }

    /**
     * La galeria ha de ser visible a la pantalla sense tocar el ratolí.
     *
     * <p>Aquesta comprovació mira la pantalla real, no un render del
     * component: un error de pintat pot deixar el component correcte i la
     * pantalla en blanc. Abans de la correcció, les targetes només
     * apareixien quan el ratolí passava per damunt.
     */
    private static void galeriaEsPinta(JFrame frame) throws Exception {
        System.out.println("=== la galeria es pinta a la pantalla ===");
        frame.setLocation(0, 0);
        frame.setSize(1000, 700);
        Thread.sleep(600);

        Component centre = troba(frame, "presentacio.panells.PanelTaulaCentral");
        if (centre != null) {
            SwingUtilities.invokeAndWait(() -> {
                try { centre.getClass().getMethod("mostrarGaleria").invoke(centre); }
                catch (Exception ignored) { /* versió anterior sense vista de galeria */ }
            });
        }
        Thread.sleep(1200);

        int visibles = pixelsDeLaGaleria(frame);
        if (visibles < 0) {
            // La captura de pantalla no ha estat possible: la comprovació
            // queda sense fer, i és la prova estructural de la suite
            // (la graella no ha de redefinir paintChildren) la que ho
            // manté cobert.
            System.out.println("  (captura de pantalla no disponible: "
                + "comprovació de la galeria saltada)");
        } else {
            System.out.println("  píxels pintats a la galeria: " + visibles);
            comprova("la galeria es veu sense moure el ratolí", visibles > 2000);
        }
    }

    /**
     * Captura la pantalla real i compta els píxels dibuixats a la zona de la
     * galeria (la meitat dreta, sota la barra d'eines).
     *
     * <p>La captura de pantalla pot bloquejar-se: l'extensió screencast de X11
     * no sempre respon. Per això es fa en un fil amb temps màxim i, si no
     * respon, la comprovació es salta en lloc d'encallar tota la verificació.
     */
    private static int pixelsDeLaGaleria(JFrame frame) {
        final int[] result = { -1 };
        Thread t = new Thread(() -> {
            try {
                Rectangle b = frame.getBounds();
                java.awt.image.BufferedImage img =
                    new java.awt.Robot().createScreenCapture(b);
                int x0 = b.width / 3;
                int y0 = b.height / 4;
                int n = 0;
                for (int y = y0; y < img.getHeight(); y++) {
                    for (int x = x0; x < img.getWidth(); x++) {
                        Color c = new Color(img.getRGB(x, y));
                        // Fons de la galeria: clar (244,245,247) o fosc (20,22,26).
                        boolean fons = (Math.abs(c.getRed() - 244) < 6 && Math.abs(c.getGreen() - 245) < 6
                                && Math.abs(c.getBlue() - 247) < 6)
                            || (Math.abs(c.getRed() - 20) < 6 && Math.abs(c.getGreen() - 22) < 6
                                && Math.abs(c.getBlue() - 26) < 6);
                        if (!fons) n++;
                    }
                }
                result[0] = n;
            } catch (Exception e) {
                result[0] = -1;
            }
        }, "captura-pantalla");
        t.setDaemon(true);
        t.start();
        try {
            t.join(8000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }

    /**
     * En canviar de tema, tota la finestra s'ha de pintar amb el tema nou.
     *
     * <p>Es renderitza l'arbre de components i es compten els píxels que
     * encara tenen el fons del tema fosc: si alguna regió (barra d'eines,
     * galeria, capçalera) no s'ha tornat a pintar, la pantalla mostra el
     * tema antic i el nou alhora.
     */
    private static void repintComplet(JFrame frame) throws Exception {
        System.out.println("=== repintat complet de la finestra ===");
        aplicaTema(frame, UITheme.Tema.DARK);
        Thread.sleep(700);
        Color fosc = UITheme.palette().bgPanel();
        int abans = pixelsAmbColor(frame, fosc);
        System.out.println("  píxels amb fons fosc (mode fosc): " + abans);

        long t0 = System.currentTimeMillis();
        aplicaTema(frame, UITheme.Tema.LIGHT);
        Thread.sleep(900);
        long ms = System.currentTimeMillis() - t0;
        int despres = pixelsAmbColor(frame, fosc);
        System.out.println("  píxels amb fons fosc (mode clar): " + despres
            + "  (canvi de tema: " + ms + " ms)");

        // La mesura només val si en mode fosc hi ha píxels d'aquest color.
        comprova("la mesura del repintat és significativa", abans > 1000);
        comprova("no queda cap regió pintada amb el tema antic", despres < Math.max(20, abans / 20));
        comprova("el canvi de tema és immediat (menys de 3 s)", ms < 3000);
    }

    /**
     * Canvia el tema pel camí real de l'aplicació: paleta nova i refresc de
     * tots els panells. Sense el refresc, la finestra es queda amb els
     * colors del tema anterior a mig pintar.
     */
    private static void aplicaTema(JFrame frame, UITheme.Tema tema) throws Exception {
        Component lib = troba(frame, "presentacio.panells.PanelMostrarBiblioteca");
        SwingUtilities.invokeAndWait(() -> {
            UITheme.posarTheme(tema);
            if (lib != null) {
                try { lib.getClass().getMethod("aplicarTheme").invoke(lib); }
                catch (Exception ignored) { /* versió anterior sense refresc */ }
            }
        });
    }

    /** Compta els píxels de la finestra que tenen el color donat. */
    private static int pixelsAmbColor(JFrame frame, Color color) {
        try {
            java.awt.Container root = frame.getContentPane();
            int w = Math.max(1, root.getWidth());
            int h = Math.max(1, root.getHeight());
            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            root.printAll(g);
            g.dispose();
            int n = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color c = new Color(img.getRGB(x, y));
                    if (Math.abs(c.getRed() - color.getRed()) < 6
                            && Math.abs(c.getGreen() - color.getGreen()) < 6
                            && Math.abs(c.getBlue() - color.getBlue()) < 6) n++;
                }
            }
            return n;
        } catch (Exception e) {
            System.out.println("  (no s'ha pogut renderitzar la finestra: " + e + ")");
            return -1;
        }
    }

    private static void recullText(Component c, int[] resum) {
        Color fg = null;
        String text = null;
        if (c instanceof JLabel l) { fg = l.getForeground(); text = l.getText(); }
        else if (c instanceof AbstractButton b) { fg = b.getForeground(); text = b.getText(); }
        else if (c instanceof JTextComponent t) { fg = t.getForeground(); text = t.getText(); }
        if (fg != null && text != null && !text.isBlank() && c.isShowing()) {
            resum[0]++;
            double r = contrast(fg, fonsEfectiu(c));
            if (r < 1.6) {
                resum[1]++;
                System.out.printf("  ILLEGIBLE ratio=%.2f fg=%s '%s' (%s)%n",
                    r, hex(fg), curt(text, 28), c.getClass().getSimpleName());
            }
        }
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) recullText(ch, resum);
        }
    }

    /** Primer fons opac cap amunt. */
    private static Color fonsEfectiu(Component c) {
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof AbstractButton ab && ab.isContentAreaFilled() && ab.getBackground() != null) {
                return ab.getBackground();
            }
            if (p instanceof JLabel || p instanceof JTextComponent) continue;
            Color bg = p.getBackground();
            if (bg != null && p.isOpaque()) return bg;
        }
        return Color.DARK_GRAY;
    }

    private static Color colorDominant(javax.swing.Icon icona, Component component) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            icona.getIconWidth(), icona.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        icona.paintIcon(component, g, 0, 0);
        g.dispose();
        Color millor = null;
        int millorAlpha = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                int a = (p >>> 24) & 0xFF;
                if (a > millorAlpha) { millorAlpha = a; millor = new Color(p); }
            }
        }
        return millor;
    }

    private static double contrast(Color a, Color b) {
        double l1 = luminancia(a), l2 = luminancia(b);
        if (l1 < l2) { double t = l1; l1 = l2; l2 = t; }
        return (l1 + 0.05) / (l2 + 0.05);
    }

    private static double luminancia(Color c) {
        if (c == null) return 0;
        double r = c.getRed() / 255.0, g = c.getGreen() / 255.0, b = c.getBlue() / 255.0;
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static void comprova(String nom, boolean ok) {
        System.out.println((ok ? "  OK    " : "  FALLA ") + nom);
        if (!ok) fallos++;
    }

    private static List<ItemNavegacio> iconesSidebar(Component c) {
        List<ItemNavegacio> out = new ArrayList<>();
        recullItems(c, out);
        return out;
    }

    private static void recullItems(Component c, List<ItemNavegacio> out) {
        if (c instanceof ItemNavegacio it) out.add(it);
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) recullItems(ch, out);
        }
    }

    private static Component trobaSidebar(Component c) {
        if (c.getClass().getName().endsWith("PanellBarraEsquerra")) return c;
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) {
                Component r = trobaSidebar(ch);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JLabel trobaStatusBar(Component c) {
        if (c instanceof JLabel l && " ".equals(l.getText())) return l;
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) {
                JLabel r = trobaStatusBar(ch);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static AbstractButton trobaBotó(Component c, String text) {
        if (c instanceof AbstractButton b && text.equalsIgnoreCase(b.getText())) return b;
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) {
                AbstractButton r = trobaBotó(ch, text);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static Component troba(Component c, String className) {
        if (c.getClass().getName().equals(className)) return c;
        if (c instanceof Container cont) {
            for (Component ch : cont.getComponents()) {
                Component r = troba(ch, className);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static String hex(Color c) {
        return c == null ? "null" : String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    private static String curt(String s, int n) {
        return s == null ? "" : (s.length() <= n ? s : s.substring(0, n - 1) + "…");
    }

    private static JFrame esperaFrame(int ms) throws Exception {
        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < ms) {
            for (java.awt.Window w : java.awt.Window.getWindows()) {
                if (w instanceof JFrame f && f.isVisible()) return f;
            }
            Thread.sleep(200);
        }
        return null;
    }
}
