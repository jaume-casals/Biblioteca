package presentacio.galeria;

import domini.Llibre;
import herramienta.Config;
import herramienta.UITheme;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Builds the visual card panels that fill the gallery.
 *
 * <p>Each card owns its own paint path (card body, footer, progress bar,
 * stars) and is wired to a {@link Listener} so the host panel can stay
 * in sync with selection / focus / open events.
 *
 * <p>The card receives the {@link CoverImageService} so async image loads
 * update the card's paint reference on the EDT.
 */
public final class CoverCardFactory {

    // Cached Color constants — never allocate in paint path.
    // A = outer shadow, B = inner shadow; H prefix = hover (stronger alpha).
    private static final Color SHADOW_A   = new Color(0, 0, 0, 14);  // outer, normal
    private static final Color SHADOW_B   = new Color(0, 0, 0, 6);   // inner, normal
    private static final Color SHADOW_HA  = new Color(0, 0, 0, 30);  // outer, hover
    private static final Color SHADOW_HB  = new Color(0, 0, 0, 14);  // inner, hover
    private static final Color DOT_READ   = new Color(0x2D9E6B);
    private static final Color DOT_UNREAD = new Color(0xBBBBBB);
    private static final Color DOT_RIM    = new Color(255, 255, 255, 200);
    private static final Color SEL_FILL   = new Color(0, 120, 215, 40);

    private static final int ARC = 10;
    private static final int SDX = 3;
    private static final int SDY = 5;

    public interface Listener {
        /** Return the current set of selected ISBNs (live; mutated by the card). */
        Set<Long> selectedISBNs();

        /** Current focused card index in the visible list, or -1 for none. */
        int focusedIdx();

        void setFocusedIdx(int idx);

        /** Full list of currently displayed books (for shift-range selection). */
        List<Llibre> currentLlibres();

        /** A card was double-clicked; the host should open the book. */
        void onCardClicked(Llibre l);

        /** A card was right-clicked; the host should show its context menu. */
        void onCardRightClicked(MouseEvent e, List<Llibre> selected);

        /**
         * The selection set has changed. The host should repaint every ISBN
         * in {@code changedIsbns} (these are the cards whose border / overlay
         * may need to be redrawn). The set may include ISBNs that were
         * previously selected and are now de-selected, and vice versa.
         */
        void onSelectionMutated(Iterable<Long> changedIsbns);
    }

    private final CoverImageService imageService;
    private final int cardW;
    private final int cardH;
    private final int coverH;
    private final int footH;

    public CoverCardFactory(CoverImageService imageService,
                            int cardW, int cardH, int coverH, int footH) {
        this.imageService = imageService;
        this.cardW = cardW;
        this.cardH = cardH;
        this.coverH = coverH;
        this.footH = footH;
    }

    public JPanel build(Llibre l, int cardIdx, Listener listener) {
        boolean llegit   = Boolean.TRUE.equals(l.getLlegit());
        int     hue      = bookHue(l.getNom());
        double  val      = l.getValoracio() != null ? l.getValoracio() : 0.0;
        int     pagines  = l.getPagines();
        int     pagLleg  = l.getPaginesLlegides();
        double  pct      = pagines > 0 ? (double) pagLleg / pagines : 0.0;
        boolean showProg = pagines > 0 && !llegit && pct > 0;
        Long    isbn     = l.getISBN();

        final int capW = cardW, capH = coverH, capCardH = cardH;

        BufferedImage cached = imageService.getCached(isbn);
        final BufferedImage[] imgRef = {cached};
        final boolean[] hov = {false};

        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g2.setColor(UITheme.BG_MAIN);
                g2.fillRect(0, 0, getWidth(), getHeight());

                boolean h = hov[0];

                int sdY = h ? SDY + 6 : SDY;
                g2.setColor(h ? SHADOW_HA : SHADOW_A);
                g2.fillRoundRect(SDX, sdY, capW, capCardH, ARC + 2, ARC + 2);
                g2.setColor(h ? SHADOW_HB : SHADOW_B);
                g2.fillRoundRect(SDX + 2, sdY + 3, capW + 1, capCardH + 1, ARC + 4, ARC + 4);

                g2.setColor(UITheme.BG_PANEL);
                g2.fillRoundRect(0, 0, capW, capCardH, ARC, ARC);

                Shape oldClip = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(0, 0, capW, capH + ARC, ARC, ARC));
                paintCover(g2, imgRef[0], hue,
                    l.getDisplayNom(Config.getLang()), l.getAutor(), capW, capH);
                g2.setClip(oldClip);

                int dotD = 9, dotX = capW - dotD - 7, dotY = 7;
                g2.setColor(llegit ? DOT_READ : DOT_UNREAD);
                g2.fillOval(dotX, dotY, dotD, dotD);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(DOT_RIM);
                g2.drawOval(dotX, dotY, dotD, dotD);

                boolean sel = listener.selectedISBNs().contains(isbn);
                if (sel) {
                    g2.setColor(SEL_FILL);
                    g2.fillRoundRect(1, 1, capW - 2, capCardH - 2, ARC, ARC);
                }

                g2.setStroke(new BasicStroke(sel || h ? 2f : 1f));
                Color bdr;
                if (sel) bdr = UITheme.ACCENT;
                else if (h) bdr = new Color(UITheme.ACCENT.getRed(), UITheme.ACCENT.getGreen(), UITheme.ACCENT.getBlue(), 140);
                else bdr = new Color(UITheme.BORDER_CLR.getRed(), UITheme.BORDER_CLR.getGreen(), UITheme.BORDER_CLR.getBlue(), 180);
                g2.setColor(bdr);
                g2.drawRoundRect(0, 0, capW - 1, capCardH - 1, ARC, ARC);

                g2.dispose();
            }

            @Override public boolean isOpaque() { return false; }
        };

        card.setPreferredSize(new Dimension(capW + SDX + 3, capCardH + SDY + 4));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel footer = buildFooter(l, val, showProg, pct, capW);
        footer.setBounds(0, capH, capW, footH);
        card.add(footer);

        card.setFocusable(true);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hov[0] = true; card.repaint();
            }
            @Override public void mouseExited (MouseEvent e) {
                hov[0] = false; card.repaint();
            }
            @Override public void mousePressed(MouseEvent e) {
                card.requestFocusInWindow();
                if (SwingUtilities.isRightMouseButton(e)) {
                    Set<Long> sel = listener.selectedISBNs();
                    if (!sel.contains(isbn)) {
                        List<Long> prev = new ArrayList<>(sel);
                        sel.clear();
                        sel.add(isbn);
                        listener.onSelectionMutated(prev);
                    }
                    listener.onCardRightClicked(e, collectSelected(listener));
                    return;
                }
                boolean ctrl  = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK)  != 0;
                boolean shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
                Set<Long> sel = listener.selectedISBNs();
                if (shift && listener.focusedIdx() >= 0) {
                    int lo = Math.min(cardIdx, listener.focusedIdx());
                    int hi = Math.max(cardIdx, listener.focusedIdx());
                    List<Llibre> all = listener.currentLlibres();
                    List<Long> prev = new ArrayList<>(sel);
                    if (!ctrl) sel.clear();
                    for (int i = lo; i <= hi && i < all.size(); i++)
                        sel.add(all.get(i).getISBN());
                    java.util.HashSet<Long> changed = new java.util.HashSet<>(prev);
                    for (int i = lo; i <= hi && i < all.size(); i++)
                        changed.add(all.get(i).getISBN());
                    listener.onSelectionMutated(changed);
                } else if (ctrl) {
                    if (sel.contains(isbn)) sel.remove(isbn);
                    else sel.add(isbn);
                    listener.setFocusedIdx(cardIdx);
                    listener.onSelectionMutated(java.util.Collections.singletonList(isbn));
                } else {
                    List<Long> prev = new ArrayList<>(sel);
                    sel.clear();
                    sel.add(isbn);
                    listener.setFocusedIdx(cardIdx);
                    listener.onSelectionMutated(prev);
                    listener.onSelectionMutated(java.util.Collections.singletonList(isbn));
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
                    listener.onCardClicked(l);
            }
        });

        // Async image load — pre-scale at current card dimensions
        imageService.submit(l, capW, capH, () -> {
            BufferedImage now = imageService.getCached(isbn);
            if (now != null) imgRef[0] = now;
            card.repaint();
        });

        return card;
    }

    private static List<Llibre> collectSelected(Listener listener) {
        List<Llibre> out = new ArrayList<>();
        Set<Long> sel = listener.selectedISBNs();
        for (Llibre l : listener.currentLlibres())
            if (sel.contains(l.getISBN())) out.add(l);
        return out;
    }

    // ── Cover painting ────────────────────────────────────────────────────────

    private static void paintCover(Graphics2D g2, BufferedImage img,
                                   int hue, String nom, String autor, int w, int h) {
        if (img != null) {
            g2.drawImage(img, 0, 0, null);
        } else {
            Color c1 = hsvApprox(hue,             0.55f, 0.38f);
            Color c2 = hsvApprox((hue + 45) % 360, 0.45f, 0.26f);
            g2.setPaint(new GradientPaint(0, 0, c1, w * 0.75f, h, c2));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(null);

            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(9, 0, 3, h, 2, 2);

            String title = nom != null ? nom : "";
            if (!title.isEmpty()) {
                g2.setFont(new Font("Serif", Font.ITALIC, 13));
                FontMetrics fm = g2.getFontMetrics();
                List<String> lines = wrapText(title, fm, w - 28, 4);
                int lineH = (int)(fm.getHeight() * 1.35);
                int autorH = (autor != null && !autor.isEmpty()) ? 16 : 0;
                int startY = Math.max(fm.getAscent() + 10,
                                      (h - lines.size() * lineH - autorH) / 2 + fm.getAscent());
                g2.setColor(new Color(255, 255, 255, 235));
                for (int i = 0; i < lines.size(); i++)
                    g2.drawString(lines.get(i), 16, startY + i * lineH);
                if (autor != null && !autor.isEmpty()) {
                    g2.setColor(new Color(255, 255, 255, 120));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.drawString(truncate(autor, 20), 16, startY + lines.size() * lineH + 5);
                }
            }
        }
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private static JPanel buildFooter(Llibre l, double val, boolean showProg, double pct, int cardW) {
        JPanel f = new JPanel();
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(8, 10, 6, 10));

        JLabel lblT = new JLabel("<html><body style='width:" + (cardW - 22) + "px; margin:0; padding:0;'><b>"
            + htmlEsc(l.getDisplayNom(Config.getLang())) + "</b></body></html>");
        lblT.setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
        lblT.setForeground(UITheme.TEXT_DARK);
        lblT.setAlignmentX(0f);
        f.add(lblT);
        f.add(Box.createVerticalStrut(2));

        JLabel lblA = new JLabel(truncate(l.getAutor(), 24));
        lblA.setFont(UITheme.FONT_SMALL);
        lblA.setForeground(UITheme.TEXT_MID);
        lblA.setAlignmentX(0f);
        f.add(lblA);
        f.add(Box.createVerticalStrut(4));

        JPanel starsRow = new JPanel(new BorderLayout(4, 0));
        starsRow.setOpaque(false);
        starsRow.setAlignmentX(0f);
        starsRow.setMaximumSize(new Dimension(cardW - 20, 16));
        starsRow.add(starsLabel(val), BorderLayout.WEST);
        if (l.getAny() != null && l.getAny() > 0) {
            JLabel yr = new JLabel(String.valueOf(l.getAny()));
            yr.setFont(UITheme.FONT_SMALL);
            yr.setForeground(UITheme.TEXT_MID);
            starsRow.add(yr, BorderLayout.EAST);
        }
        f.add(starsRow);

        if (showProg) {
            f.add(Box.createVerticalStrut(4));
            f.add(progressBar(pct, cardW));
        }

        return f;
    }

    private static JLabel starsLabel(double valoracio) {
        double stars = valoracio / 2.0;
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < 5; i++) {
            boolean on = stars >= i + 0.5;
            sb.append("<font color='")
              .append(on ? "#c8920a" : (UITheme.isDark() ? "#555555" : "#dddddd"))
              .append("'>&#9733;</font>");
        }
        sb.append("</html>");
        JLabel lbl = new JLabel(sb.toString());
        lbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        return lbl;
    }

    private static JPanel progressBar(double pct, int cardW) {
        final float p = (float) Math.min(1.0, Math.max(0.0, pct));
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.isDark() ? new Color(0x555555) : new Color(0xEEEEEE));
                g2.fillRoundRect(0, 0, getWidth(), 3, 2, 2);
                if (p > 0) {
                    g2.setColor(UITheme.ACCENT);
                    g2.fillRoundRect(0, 0, Math.max(2, (int)(getWidth() * p)), 3, 2, 2);
                }
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        bar.setPreferredSize(new Dimension(cardW - 20, 3));
        bar.setMaximumSize(new Dimension(cardW - 20, 3));
        return bar;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static int bookHue(String nom) {
        if (nom == null || nom.isEmpty()) return 200;
        int h = 0;
        for (char c : nom.toCharArray()) h = (h * 31 + c) & 0xffff;
        return (h % 300) + 20;
    }

    private static Color hsvApprox(int hueDeg, float sat, float bri) {
        return new Color(Color.HSBtoRGB(hueDeg / 360f, sat, bri));
    }

    private static List<String> wrapText(String text, FontMetrics fm, int maxW, int maxLines) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            if (result.size() >= maxLines) break;
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(test) <= maxW) {
                cur = new StringBuilder(test);
            } else {
                if (cur.length() > 0) {
                    if (result.size() == maxLines - 1) {
                        String s = cur.toString();
                        while (s.length() > 1 && fm.stringWidth(s + "…") > maxW)
                            s = s.substring(0, s.length() - 1);
                        result.add(s + "…");
                        cur = new StringBuilder();
                        break;
                    }
                    result.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    String ww = word;
                    while (ww.length() > 1 && fm.stringWidth(ww) > maxW)
                        ww = ww.substring(0, ww.length() - 1);
                    result.add(ww);
                    cur = new StringBuilder();
                }
            }
        }
        if (cur.length() > 0 && result.size() < maxLines) result.add(cur.toString());
        return result;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String htmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
