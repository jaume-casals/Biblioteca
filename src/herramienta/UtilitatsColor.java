package herramienta;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/** Utilitats de color (format/anàlisi hex, icona de mostra) usades pels diàlegs de color de prestatge i el codi de tema. */
public final class UtilitatsColor {
    private UtilitatsColor() {}

    public static String toHex(Color c) {
        if (c == null) return null;
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color fromHex(String hex) {
        if (hex == null || hex.isBlank()) return null;
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 3) {
            // Expand #rgb → #rrggbb
            char r = h.charAt(0), g = h.charAt(1), b = h.charAt(2);
            h = "" + r + r + g + g + b + b;
        }
        if (h.length() != 6) return null;
        try {
            int rgb = Integer.parseInt(h, 16);
            return new Color(rgb);
        } catch (NumberFormatException e) { return null; }
    }

    /** Icona de mostra amb rectangle arrodonit petit per a la visualització del color del prestatge. */
    public static Icon colorSwatch(Color c) {
        return new Icon() {
            @Override public int getIconWidth()  { return 14; }
            @Override public int getIconHeight() { return 14; }
            @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                g.setColor(c);
                g.fillRoundRect(x, y + 1, 12, 12, 4, 4);
                g.setColor(c.darker());
                g.drawRoundRect(x, y + 1, 12, 12, 4, 4);
            }
        };
    }
}
