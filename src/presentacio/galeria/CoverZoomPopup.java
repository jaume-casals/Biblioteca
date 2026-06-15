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
 * Hover-zoom popup shown next to a gallery card after a short delay.
 *
 * <p>The popup is a small JPanel added to the root pane's {@link JLayeredPane#POPUP_LAYER}.
 * It is owned by the gallery: only one popup can be visible at a time, and
 * it is removed when the host hides it (mouse exit, theme switch, list update,
 * shutdown).
 */
public final class CoverZoomPopup {

    private static final int DELAY_MS = 500;
    private static final int MAX_POPUP_WIDTH = 300;

    private JPanel overlay;
    private Timer timer;
    private static final int ZOOM_CACHE_CAP = 32;
    private final Map<Long, BufferedImage> zoomCache = new LinkedHashMap<Long, BufferedImage>(ZOOM_CACHE_CAP, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> e) { return size() > ZOOM_CACHE_CAP; }
    };

    /**
     * Schedule a zoom popup for {@code card}. The popup is shown
     * {@value #DELAY_MS} ms after the call (debounced — repeated calls reset
     * the timer). The image must already be in the {@link CoverImageService}
     * cache; if not, this call is a no-op.
     */
    public void schedule(Llibre l, Component card, CoverImageService images) {
        cancelTimer();
        timer = new Timer(DELAY_MS, ev -> show(l, card, images));
        timer.setRepeats(false);
        timer.start();
    }

    /** Cancel any pending show; remove the overlay if visible. */
    public void hide() {
        cancelTimer();
        if (overlay != null) {
            java.awt.Container parent = overlay.getParent();
            if (parent != null) {
                parent.remove(overlay);
                parent.repaint();
            }
            overlay = null;
        }
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void show(Llibre l, Component card, CoverImageService images) {
        hide();
        if (!card.isDisplayable()) return;
        long isbn = l.getISBN();
        BufferedImage img = images.getCached(isbn);
        if (img == null) return;

        int pw = Math.min(img.getWidth() * 2, MAX_POPUP_WIDTH);
        int ph = (int) ((double) img.getHeight() / img.getWidth() * pw);
        BufferedImage scaled = zoomCache.get(isbn);
        if (scaled == null || scaled.getWidth() != pw || scaled.getHeight() != ph) {
            scaled = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            scaled.createGraphics().drawImage(
                img.getScaledInstance(pw, ph, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
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
