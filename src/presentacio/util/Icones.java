package presentacio.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import herramienta.ui.Tokens;
import herramienta.ui.UITheme;

/**
 * Catàleg d'icones SVG locals de l'aplicació.
 *
 * <p>Els SVG viuen a {@code src/presentacio/icones} i es copien al classpath
 * en compilar. No es descarrega res en temps d'execució ni es depèn de
 * serveis externs: la família d'icones és local, coherent i versionada amb
 * el codi.
 *
 * <p>Els icones es rasteritzen una sola vegada per (nom, mida, color) i es
 * reutilitzen des d'una memòria cau. Rasteritzar SVG en cada pintat seria
 * massa car per a una galeria amb desenes de targetes, i el component
 * no crea fils ni càrrega imatges de reu.
 */
public final class Icones {

    private Icones() {}

    private static final String BASE = "/presentacio/icones/";
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    /** Icona de la mida estàndard, amb el color de text del tema. */
    public static Icon icon(String name) {
        return icon(name, Tokens.ICON_SIZE, UITheme.palette().textDark());
    }

    /** Icona de la mida estàndard amb un color explícit. */
    public static Icon icon(String name, Color color) {
        return icon(name, Tokens.ICON_SIZE, color);
    }

    /**
     * Icona d'una mida i un color concrets.
     *
     * @param name nom del fitxer sense l'extensió {@code .svg}
     * @param size mida en píxels del costat
     * @param color color del traç
     */
    public static Icon icon(String name, int size, Color color) {
        if (name == null || name.isBlank()) return null;
        String key = name + "|" + size + "|" + (color == null ? 0 : color.getRGB());
        return CACHE.computeIfAbsent(key, k -> rasterize(name, size, color));
    }

    /** Invalida la memòria cau. S'ha de cridar en canviar de tema. */
    public static void invalidar() {
        CACHE.clear();
    }

    private static ImageIcon rasterize(String name, int size, Color color) {
        URL url = Icones.class.getResource(BASE + name + ".svg");
        if (url == null) {
            // Un nom incorrecte no ha de deixar la interfície sense icona:
            // es torna un icona buida que encara ocupa l'espai correcte.
            return new ImageIcon(new BufferedImage(Math.max(1, size), Math.max(1, size),
                    BufferedImage.TYPE_INT_ARGB));
        }
        try {
            FlatSVGIcon svg = new FlatSVGIcon(url).derive(size, size);
            Color target = color != null ? color : UITheme.palette().textDark();
            svg.setColorFilter(new FlatSVGIcon.ColorFilter((Component c, Color src) -> target));
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            svg.paintIcon(null, g, 0, 0);
            g.dispose();
            return new ImageIcon(img);
        } catch (Exception e) {
            return new ImageIcon(new BufferedImage(Math.max(1, size), Math.max(1, size),
                    BufferedImage.TYPE_INT_ARGB));
        }
    }
}
