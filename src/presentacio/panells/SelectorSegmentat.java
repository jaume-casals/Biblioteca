package presentacio.panells;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import herramienta.ui.Tokens;
import herramienta.ui.UITheme;
import presentacio.util.Icones;

/**
 * Selector segmentat (per exemple "Galeria | Taula").
 *
 * <p>Dos botons exclusius que mostren <em>quina</em> vista és la que hi ha,
 * no quin mode s'activarà en prémer. L'estat seleccionat es marca amb el
 * fons accentuat, la vora i el pes de la lletra: el color sol no és prou.
 *
 * <p>Els botons són {@link JButton} de debò (no {@code JToggleButton}): així
 * el component dibuixa l'estat i el focus amb el mateix codi que la resta de
 * l'aplicació, i el Look &amp; Feel els dibuixa igual.
 */
public class SelectorSegmentat extends JPanel {

    private static final long serialVersionUID = 1L;

    private final List<JButton> botons = new ArrayList<>();
    private final List<String> icones = new ArrayList<>();
    private int seleccionat;

    /**
     * @param textos etiquetes dels segments, d'esquerra a dreta
     * @param icones icones dels segments, d'esquerra a dreta (pot ser buit)
     */
    public SelectorSegmentat(String[] textos, String[] icones) {
        setLayout(new FlowLayout(FlowLayout.LEFT, Tokens.SPACE_1, 0));
        setBackground(UITheme.palette().bgPanel());
        setOpaque(false);
        for (int i = 0; i < textos.length; i++) {
            JButton b = new JButton(textos[i]);
            if (icones != null && i < icones.length && icones[i] != null) {
                b.setIcon(Icones.icon(icones[i], Tokens.ICON_SIZE, UITheme.palette().textDark()));
                b.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
                b.setIconTextGap(Tokens.SPACE_2);
            }
            b.setFocusPainted(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // La font s'aplica abans de calcular cap mida: si es calcula
            // abans, el text queda tallat perquè la mida prové de la font
            // per defecte.
            b.setFont(UITheme.fontBase());
            botons.add(b);
            this.icones.add(icones != null && i < icones.length ? icones[i] : null);
            add(b);
        }
        // Amplada constant per a tots els segments: si la mida depengués de
        // l'estat (negrita, vora), canviar de vista redimensionaria la
        // barra d'eines i provocaria un relayout de tot el contingut.
        int ampladaMax = 0;
        for (JButton b : botons) {
            ampladaMax = Math.max(ampladaMax, b.getPreferredSize().width);
        }
        for (JButton b : botons) {
            Dimension pref = b.getPreferredSize();
            b.setPreferredSize(new Dimension(ampladaMax, Tokens.CONTROL_HEIGHT));
            b.setMinimumSize(new Dimension(1, Tokens.CONTROL_HEIGHT));
        }
        actualitzaEstils();
    }

    /** Marca el segment actiu. */
    public void setSeleccionat(int index) {
        if (index < 0 || index >= botons.size() || index == seleccionat) return;
        seleccionat = index;
        actualitzaEstils();
    }


    public JButton obtenirBotó(int index) {
        return (index >= 0 && index < botons.size()) ? botons.get(index) : null;
    }

    private void actualitzaEstils() {
        for (int i = 0; i < botons.size(); i++) {
            JButton b = botons.get(i);
            boolean actiu = i == seleccionat;
            b.setBackground(actiu ? UITheme.palette().accentSoft() : UITheme.palette().bgPanel());
            b.setForeground(actiu ? UITheme.palette().textDark() : UITheme.palette().textMid());
            b.setFont(actiu ? UITheme.fontBold() : UITheme.fontBase());
            b.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(
                    actiu ? UITheme.palette().accentBorder() : UITheme.palette().borderClr()),
                javax.swing.BorderFactory.createEmptyBorder(0, Tokens.SPACE_3, 0, Tokens.SPACE_3)));
            String icona = icones.get(i);
            if (icona != null) {
                b.setIcon(Icones.icon(icona, Tokens.ICON_SIZE,
                    actiu ? UITheme.palette().textDark() : UITheme.palette().textMid()));
            }
        }
    }

    public void aplicarTheme() {
        setBackground(UITheme.palette().bgPanel());
        for (JButton b : botons) {
            b.setFont(UITheme.fontBase());
        }
        actualitzaEstils();
        repaint();
    }
}
