package presentacio.panells.navegacio;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;

import herramienta.ui.Tokens;
import herramienta.ui.UITheme;

/**
 * Element de la navegació lateral.
 *
 * <p>Dibuixa el seu propi fons per poder comunicar tres estats sense
 * Dependsnomés del color:
 * <ul>
 *   <li><b>Seleccionat</b>: fons tou + indicador vertical de color accent
 *       a l'esquerra + text en pes fort.</li>
 *   <li><b>Sobrevolat</b> (no seleccionat): fons lleugerament diferent.</li>
 *   <li><b>Normal</b>: sense fons.</li>
 * </ul>
 *
 * <p>El component manté el focus de teclat i la navegació accessible: és un
 * {@link JButton}, de manera que Space/Enter l'activen i el focus es veu amb
 * el contorn que dibuixa FlatLaf.
 */
public class ItemNavegacio extends JButton {

    private static final long serialVersionUID = 1L;

    private boolean seleccionat;
    private boolean sobrevolat;
    private boolean pressionat;
    private String comptador;

    public ItemNavegacio(String text, Icon icona) {
        super(text);
        setIcon(icona);
        setHorizontalAlignment(LEFT);
        setHorizontalTextPosition(RIGHT);
        setIconTextGap(Tokens.SPACE_3);
        setFocusPainted(true);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UITheme.fontBase());
        setBorder(javax.swing.BorderFactory.createEmptyBorder(
            Tokens.SPACE_2 + 1, Tokens.SPACE_4, Tokens.SPACE_2 + 1, Tokens.SPACE_3));
        actualitzaAlcadaMaxima();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { sobrevolat = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { sobrevolat = false; pressionat = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressionat = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressionat = false; repaint(); }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
        });
    }

    /**
     * Recalcula l'alçada màxima.
     *
     * <p>En un {@code BoxLayout} vertical, l'alçada màxima és la que limita
     * la mida real del component: si és menor que la preferida, el text es
     * veu tallat. Ha de coincidir amb la preferida.
     */
    public void actualitzaAlcadaMaxima() {
        Dimension pref = getPreferredSize();
        setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
    }

    /** Marca l'element com a seleccionat o no. */
    public void setSeleccionat(boolean v) {
        if (seleccionat == v) return;
        seleccionat = v;
        setFont(v ? UITheme.fontBold() : UITheme.fontBase());
        actualitzaAlcadaMaxima();
        repaint();
    }

    public boolean esSeleccionat() { return seleccionat; }

    /** Text auxiliar a la dreta (per exemple el nombre de llibres d'una prestatgeria). */
    public void setComptador(String text) {
        if (text == null ? comptador == null : text.equals(comptador)) return;
        comptador = text;
        revalidate();
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (seleccionat) {
                Color bg = UITheme.palette().sidebarSelBg();
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, Tokens.RADIUS_MD, Tokens.RADIUS_MD);
                // Indicador de selecció: una barra de color accent, no només un canvi de fons.
                g2.setColor(UITheme.palette().sidebarAccent());
                g2.fillRoundRect(0, 0, 3, h, Tokens.RADIUS_SM, Tokens.RADIUS_SM);
            } else if (sobrevolat) {
                g2.setColor(UITheme.palette().sidebarHoverBg());
                g2.fillRoundRect(0, 0, w, h, Tokens.RADIUS_MD, Tokens.RADIUS_MD);
            }
            if (isFocusOwner()) {
                g2.setColor(UITheme.palette().borderFocus());
                g2.drawRoundRect(1, 1, w - 3, h - 3, Tokens.RADIUS_MD, Tokens.RADIUS_MD);
            }
            setForeground(seleccionat
                ? UITheme.palette().sidebarSelText()
                : UITheme.palette().sidebarText());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
        if (comptador != null && !comptador.isBlank()) {
            Graphics2D gc = (Graphics2D) g.create();
            try {
                gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gc.setFont(UITheme.fontSmall());
                gc.setColor(seleccionat
                    ? UITheme.palette().sidebarSelText()
                    : UITheme.palette().sidebarTextMid());
                int w = gc.getFontMetrics().stringWidth(comptador);
                gc.drawString(comptador, getWidth() - w - Tokens.SPACE_3, getHeight() / 2
                    + gc.getFontMetrics().getAscent() / 2 - 1);
            } finally {
                gc.dispose();
            }
        }
    }
}
