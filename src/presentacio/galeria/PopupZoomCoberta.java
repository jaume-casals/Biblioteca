package presentacio.galeria;

import domini.Llibre;
import herramienta.UITheme;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Popup de zoom en passar el ratolí que es mostra al costat d'una
 * targeta de galeria després d'un breu retard.
 *
 * <p>El popup és un JPanel petit afegit al {@link JLayeredPane#POPUP_LAYER}
 * del root pane. La galeria el posseeix: només un popup pot ser visible
 * alhora, i s'elimina quan l'amfitrió l'amaga (sortida del ratolí,
 * canvi de tema, actualització de llista, tancament).
 */
public final class PopupZoomCoberta {

    private static final int DELAY_MS = 500;
    private static final int MAX_POPUP_WIDTH = 300;

    private JPanel overlay;
    private Timer timer;
    private static final int ZOOM_CACHE_CAP = 32;
    private final Map<Long, BufferedImage> zoomCache = new LinkedHashMap<Long, BufferedImage>(ZOOM_CACHE_CAP, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> e) { return size() > ZOOM_CACHE_CAP; }
    };

    /**
     * Programa un popup de zoom per a {@code card}. El popup es mostra
     * {@value #DELAY_MS} ms després de la crida (debounced — les crides
     * repetides reinicien el temporitzador). La imatge ja ha de ser a la
     * memòria cau del {@link ServeiImatgesCoberta}; si no, aquesta crida
     * és un no-op.
     */
    public void schedule(Llibre l, Component card, ServeiImatgesCoberta images) {
        cancelarTimer();
        timer = new Timer(DELAY_MS, ev -> show(l, card, images));
        timer.setRepeats(false);
        timer.start();
    }

    /** Cancel·la qualsevol visualització pendent; elimina la superposició si és visible. */
    public void hide() {
        cancelarTimer();
        if (overlay != null) {
            java.awt.Container parent = overlay.getParent();
            if (parent != null) {
                parent.remove(overlay);
                parent.repaint();
            }
            overlay = null;
        }
    }

    private void cancelarTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void show(Llibre l, Component card, ServeiImatgesCoberta images) {
        hide();
        if (!card.isDisplayable()) return;
        long isbn = l.obtenirISBN();
        BufferedImage img = images.obtenirCached(isbn);
        if (img == null) return;

        int pw = Math.min(img.getWidth() * 2, MAX_POPUP_WIDTH);
        int ph = (int) ((double) img.getHeight() / img.getWidth() * pw);
        BufferedImage scaled = zoomCache.get(isbn);
        if (scaled == null || scaled.getWidth() != pw || scaled.getHeight() != ph) {
            scaled = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            try {
                g.drawImage(
                    img.getScaledInstance(pw, ph, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            } finally {
                g.dispose();
            }
            zoomCache.put(isbn, scaled);
        }

        JLabel lbl = new JLabel(new ImageIcon(scaled));
        lbl.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr(), 1));

        JRootPane root = SwingUtilities.getRootPane(card);
        if (root == null) return;

        overlay = new JPanel(new BorderLayout());
        overlay.setOpaque(true);
        overlay.setBackground(UITheme.palette().bgPanel());
        overlay.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr(), 1));
        overlay.add(lbl, BorderLayout.CENTER);

        java.awt.Point loc = SwingUtilities.convertPoint(card, card.getWidth() + 4, 0,
            root.getLayeredPane());
        overlay.setBounds(loc.x, loc.y, pw + 8, ph + 8);
        root.getLayeredPane().add(overlay, JLayeredPane.POPUP_LAYER);
        root.getLayeredPane().repaint();
    }
}
