package herramienta;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Selector compacte de color de prestatge amb una paleta fixa (sense el {@code JColorChooser} complet). */
public final class SelectorMostraColor {

    /**
     * Color per defecte del prestatge quan no se n'ha assignat cap. Compartit
     * per {@link #chooseHex} (quan el consumidor passa {@code null}) i
     * {@code GestioLlistesDialeg.onColorLlista} (quan el prestatge existent
     * no té color). Apujar-lo a una sola constant fa que la política
     * "prestatge nou comença en blau" sigui fàcil de canviar en un sol
     * lloc (segons el finding MEDIUM de tot.txt sobre el valor per defecte compartit).
     */
    public static final String DEFAULT_HEX = "#3498DB";

    public static final Color[] SHELF_PALETTE = {
        new Color(0xE74C3C), new Color(0x3498DB), new Color(0x27AE60), new Color(0x2C3E50),
        Color.WHITE, new Color(0xE67E22), new Color(0x9B59B6), new Color(0x795548)
    };

    private SelectorMostraColor() {}

    /** @return hex del color escollit (#RRGGBB) o {@code null} si es cancel·la */
    public static String chooseHex(java.awt.Component parent, Color initial, String titleKey) {
        final Color[] chosen = { initial != null ? initial : Color.decode(DEFAULT_HEX) };
        JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        grid.setBackground(UITheme.palette().bgPanel());
        for (Color c : SHELF_PALETTE) {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(32, 32));
            btn.setBackground(c);
            btn.setOpaque(true);
            btn.setBorderPainted(true);
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            btn.addActionListener(ev -> chosen[0] = c);
            grid.add(btn);
        }
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setBackground(UITheme.palette().bgPanel());
        wrap.add(grid, BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(parent, wrap,
            I18n.t(titleKey), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        Color c = chosen[0];
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
