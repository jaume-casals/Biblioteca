package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import domini.Llibre;
import herramienta.ui.Tokens;
import herramienta.ui.UITheme;
import presentacio.models.ModelTaulaBiblioteca;

/**
 * Cel·la de progrés de lectura.
 *
 * <p>Dibuixa una barra <em>fi</em> amb el text al costat, no un rectangle que
 * omple la cel·la: un {@code JProgressBar} estàndard amb el text pintat
 * dibuixa el fons de la barra a tota l'alçada de la cel·la i el text queda
 * il·legible.
 *
 * <p>Els estats es distingeixen pel text, no només pel color:
 * <ul>
 *   <li><b>Sense dades</b> (l'obra no té pàgines conegudes): "—".</li>
 *   <li><b>Sense començar</b> (pàgines conegudes, cap llegida): "0 / 320"
 *       amb la barra buida. És un 0% real, no una manca de dades.</li>
 *   <li><b>En curs</b>: "140 / 320" amb la barra partially omplida.</li>
 * </ul>
 */
public class RenderitzadorBarraProgres extends JComponent implements TableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final int ALCADA_BARRA = 4;

    private int pagines;
    private int llegides;
    private boolean dadesDesconegudes = true;

    /**
     * Mesures de la font en caché.
     *
     * <p>{@code getFontMetrics} crea un objecte nou cada crida; a la taula
     * això s'ha de cridar una vegada per cel·la i per pintat, i amb desenes
     * de files es nota. Només es torna a calcular quan canvia la font.
     */
    private Font fontCorrent;
    private FontMetrics fmCorrent;

    public RenderitzadorBarraProgres() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        dadesDesconegudes = true;
        pagines = 0;
        llegides = 0;
        try {
            if (t.getModel() instanceof ModelTaulaBiblioteca model) {
                int modelRow = t.convertRowIndexToModel(row);
                Llibre l = model.obtenirBookAt(modelRow);
                if (l != null) {
                    int total = l.obtenirPagines();
                    if (total > 0) {
                        dadesDesconegudes = false;
                        pagines = total;
                        llegides = Math.min(Math.max(0, l.obtenirPaginesLlegides()), total);
                    }
                }
            }
        } catch (RuntimeException ignored) {
            dadesDesconegudes = true;
        }
        setBackground(selected ? UITheme.palette().selectionBg() : UITheme.palette().bgPanel());
        setForeground(selected ? UITheme.palette().selectionText() : UITheme.palette().textDark());
        return this;
    }

    /**
     * Text que mostra la cel·la.
     *
     * <p>"—" quan no hi ha pàgines conegudes; "0 / 320" quan hi ha pàgines
     * però encara no se n'ha llegit cap (un 0% real, no una manca de dades).
     */
    public String obtenirTextProgres() {
        if (dadesDesconegudes) return "\u2014";
        return llegides + " / " + pagines;
    }

    /** Cert si no hi ha pàgines totals vàlides. */
    public boolean esProgresDesconegut() {
        return dadesDesconegudes;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(110, 40);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            int marge = Tokens.SPACE_2;
            Font font = getFont() != null ? getFont() : UITheme.fontBase();
            FontMetrics fm = metrics(font);

            String text = dadesDesconegudes ? "—" : (llegides + " / " + pagines);
            g2.setFont(font);
            g2.setColor(dadesDesconegudes
                ? (isSelected() ? getForeground() : UITheme.palette().textMuted())
                : getForeground());
            int textY = dadesDesconegudes
                ? (getHeight() + fm.getAscent() - fm.getDescent()) / 2
                : getHeight() / 2 - Tokens.SPACE_2;
            g2.drawString(text, marge, textY);

            if (dadesDesconegudes) return;

            // Barra fina al peu de la cel·la.
            int ample = getWidth() - 2 * marge;
            int y = getHeight() - marge - ALCADA_BARRA;
            if (y < 0 || ample <= 0) return;
            pintaBarra(g2, ample, y);
        } finally {
            g2.dispose();
        }
    }

    /** Mesures de la font, en caché fins que la font canviï. */
    private FontMetrics metrics(Font f) {
        if (fmCorrent == null || f != fontCorrent) {
            fontCorrent = f;
            fmCorrent = getFontMetrics(f);
        }
        return fmCorrent;
    }

    /** Pinta la pista i la part completada de la barra de progrés. */
    private void pintaBarra(Graphics2D g2, int ample, int y) {
        g2.setColor(isSelected() ? UITheme.palette().accentSoft() : UITheme.palette().bgSunken());
        g2.fillRoundRect(Tokens.SPACE_2, y, ample, ALCADA_BARRA, ALCADA_BARRA, ALCADA_BARRA);
        double frac = pagines > 0 ? (llegides / (double) pagines) : 0;
        int omplert = (int) Math.round(ample * frac);
        if (omplert > 0) {
            g2.setColor(UITheme.palette().accent());
            g2.fillRoundRect(Tokens.SPACE_2, y, Math.max(omplert, ALCADA_BARRA), ALCADA_BARRA,
                ALCADA_BARRA, ALCADA_BARRA);
        }
    }

    private boolean isSelected() {
        Color bg = getBackground();
        return bg != null && bg.equals(UITheme.palette().selectionBg());
    }
}
