package presentacio;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.*;
import domini.Llibre;
import herramienta.UITheme;

public class GaleriaCobertesPanel extends JPanel {

    private static final int[] ZOOM_WIDTHS = {100, 120, 140, 170, 210};
    private static final int[] ZOOM_COVERS = {150, 180, 210, 255, 315};
    private static final int   FOOT_H = 84;
    private static final int   ARC    = 10;
    private static final int   SDX    = 3;
    private static final int   SDY    = 5;
    private static final int   GAP    = 14;

    // Cached Color constants — never allocate in paint path
    private static final Color SHADOW_A   = new Color(0, 0, 0, 14);
    private static final Color SHADOW_B   = new Color(0, 0, 0, 6);
    private static final Color SHADOW_HA  = new Color(0, 0, 0, 30);
    private static final Color SHADOW_HB  = new Color(0, 0, 0, 14);
    private static final Color DOT_READ   = new Color(0x2D9E6B);
    private static final Color DOT_UNREAD = new Color(0xBBBBBB);
    private static final Color DOT_RIM    = new Color(255, 255, 255, 200);
    private static final Color SEL_FILL   = new Color(0, 120, 215, 40);

    private int zoomLevel = herramienta.Config.getGalleryZoom();
    private int CARD_W  = ZOOM_WIDTHS[zoomLevel];
    private int COVER_H = ZOOM_COVERS[zoomLevel];
    private int CARD_H  = COVER_H + FOOT_H;

    private final JPanel wrap;
    // Key: isbn — value: image pre-scaled to CARD_W × COVER_H at current zoom
    private final Map<Long, BufferedImage> imageCache = java.util.Collections.synchronizedMap(
        new java.util.LinkedHashMap<Long, BufferedImage>(200, 0.75f, true) {
            @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long, BufferedImage> e) {
                return size() > 150;
            }
        });
    private final Set<Long> loading = ConcurrentHashMap.newKeySet();
    // isbn → card panel, for targeted repaints
    private final Map<Long, JPanel> cardMap = new java.util.HashMap<>();

    private java.util.function.Consumer<Llibre> onCardClick;
    private ArrayList<Llibre> currentLlibres;
    private final Set<Long> selectedISBNs = new java.util.LinkedHashSet<>();
    private java.util.function.BiConsumer<java.awt.event.MouseEvent, List<Llibre>> onRightClick;
    private java.util.function.Consumer<List<Llibre>> onDeleteSelected;
    private int lastClickedIdx = -1;

    public GaleriaCobertesPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);

        wrap = new JPanel(new WrapLayout(FlowLayout.LEADING, GAP, GAP));
        wrap.setBackground(UITheme.BG_MAIN);

        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(60);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UITheme.BG_MAIN);
        // SIMPLE_SCROLL_MODE forces full repaint on scroll — avoids blit artifacts with non-opaque cards
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        add(scroll, BorderLayout.CENTER);

        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                wrap.revalidate();
            }
        });

        scroll.addMouseWheelListener(e -> {
            if ((e.getModifiersEx() & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
                adjustZoom(e.getWheelRotation() < 0 ? 1 : -1);
            } else {
                javax.swing.JScrollBar vsb = scroll.getVerticalScrollBar();
                vsb.setValue(vsb.getValue() + e.getUnitsToScroll() * vsb.getUnitIncrement());
            }
        });

        setFocusable(true);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "galDelete");
        getActionMap().put("galDelete", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                List<Llibre> sel = getSelectedLlibres();
                if (!sel.isEmpty() && onDeleteSelected != null) onDeleteSelected.accept(sel);
            }
        });
    }

    public void setOnCardClick(java.util.function.Consumer<Llibre> cb)  { this.onCardClick = cb; }
    public void setOnRightClick(java.util.function.BiConsumer<java.awt.event.MouseEvent, List<Llibre>> cb) { this.onRightClick = cb; }
    public void setOnDeleteSelected(java.util.function.Consumer<List<Llibre>> cb) { this.onDeleteSelected = cb; }

    public List<Llibre> getSelectedLlibres() {
        if (currentLlibres == null || selectedISBNs.isEmpty()) return java.util.Collections.emptyList();
        List<Llibre> result = new ArrayList<>();
        for (Llibre l : currentLlibres)
            if (selectedISBNs.contains(l.getISBN())) result.add(l);
        return result;
    }

    public void selectAll() {
        if (currentLlibres == null) return;
        for (Llibre l : currentLlibres) selectedISBNs.add(l.getISBN());
        wrap.repaint();
    }

    public void clearSelection() {
        List<Long> prev = new ArrayList<>(selectedISBNs);
        selectedISBNs.clear();
        repaintCards(prev);
    }

    public void adjustZoom(int delta) {
        int newZoom = Math.max(0, Math.min(ZOOM_WIDTHS.length - 1, zoomLevel + delta));
        if (newZoom == zoomLevel) return;
        zoomLevel = newZoom;
        CARD_W  = ZOOM_WIDTHS[zoomLevel];
        COVER_H = ZOOM_COVERS[zoomLevel];
        CARD_H  = COVER_H + FOOT_H;
        herramienta.Config.setGalleryZoom(zoomLevel);
        // Pre-scaled images are now wrong size — discard
        imageCache.clear();
        loading.clear();
        if (currentLlibres != null) updateLlibres(currentLlibres);
    }

    public void updateLlibres(ArrayList<Llibre> llibres) {
        this.currentLlibres = llibres;
        selectedISBNs.clear();
        lastClickedIdx = -1;
        cardMap.clear();
        wrap.setBackground(UITheme.BG_MAIN);
        wrap.removeAll();
        if (llibres != null) {
            for (int i = 0; i < llibres.size(); i++) wrap.add(buildCard(llibres.get(i), i));
        }
        wrap.revalidate();
        wrap.repaint();
    }

    public void applyTheme() {
        setBackground(UITheme.BG_MAIN);
        wrap.setBackground(UITheme.BG_MAIN);
        if (currentLlibres != null) updateLlibres(currentLlibres);
    }

    // ── Targeted repaint helpers ──────────────────────────────────────────────

    private void repaintCards(Iterable<Long> isbns) {
        for (Long isbn : isbns) {
            JPanel c = cardMap.get(isbn);
            if (c != null) c.repaint();
        }
    }

    private void repaintCard(Long isbn) {
        JPanel c = cardMap.get(isbn);
        if (c != null) c.repaint();
    }

    // ── Card builder ──────────────────────────────────────────────────────────

    private JPanel buildCard(Llibre l, int cardIdx) {
        boolean llegit   = Boolean.TRUE.equals(l.getLlegit());
        int     hue      = bookHue(l.getNom());
        double  val      = l.getValoracio() != null ? l.getValoracio() : 0.0;
        int     pagines  = l.getPagines();
        int     pagLleg  = l.getPaginesLlegides();
        double  pct      = pagines > 0 ? (double) pagLleg / pagines : 0.0;
        boolean showProg = pagines > 0 && !llegit && pct > 0;
        Long    isbn     = l.getISBN();

        final int capW = CARD_W, capH = COVER_H, capCardH = CARD_H;

        BufferedImage cached = imageCache.get(isbn);
        final BufferedImage[] imgRef = {cached};
        final boolean[] hov = {false};

        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Only antialiasing — no RENDER_QUALITY, no INTERPOLATION (images pre-scaled)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Clear full bounds — prevents black corner artifacts when scrolling non-opaque panels
                g2.setColor(UITheme.BG_MAIN);
                g2.fillRect(0, 0, getWidth(), getHeight());

                boolean h = hov[0];

                // Shadow
                int sdY = h ? SDY + 6 : SDY;
                g2.setColor(h ? SHADOW_HA : SHADOW_A);
                g2.fillRoundRect(SDX, sdY, capW, capCardH, ARC + 2, ARC + 2);
                g2.setColor(h ? SHADOW_HB : SHADOW_B);
                g2.fillRoundRect(SDX + 2, sdY + 3, capW + 1, capCardH + 1, ARC + 4, ARC + 4);

                // Card background
                g2.setColor(UITheme.BG_PANEL);
                g2.fillRoundRect(0, 0, capW, capCardH, ARC, ARC);

                // Cover (clipped to top rounded section)
                Shape oldClip = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(0, 0, capW, capH + ARC, ARC, ARC));
                paintCover(g2, imgRef[0], hue, l.getNom(), l.getAutor(), capW, capH);
                g2.setClip(oldClip);

                // Read/unread dot
                int dotD = 9, dotX = capW - dotD - 7, dotY = 7;
                g2.setColor(llegit ? DOT_READ : DOT_UNREAD);
                g2.fillOval(dotX, dotY, dotD, dotD);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(DOT_RIM);
                g2.drawOval(dotX, dotY, dotD, dotD);

                // Selection overlay
                boolean sel = selectedISBNs.contains(isbn);
                if (sel) {
                    g2.setColor(SEL_FILL);
                    g2.fillRoundRect(1, 1, capW - 2, capCardH - 2, ARC, ARC);
                }

                // Border
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
        footer.setBounds(0, capH, capW, FOOT_H);
        card.add(footer);

        card.setFocusable(true);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hov[0] = true;  card.repaint(); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { hov[0] = false; card.repaint(); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                card.requestFocusInWindow();
                if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    if (!selectedISBNs.contains(isbn)) {
                        List<Long> prev = new ArrayList<>(selectedISBNs);
                        selectedISBNs.clear();
                        selectedISBNs.add(isbn);
                        repaintCards(prev);
                        repaintCard(isbn);
                    }
                    if (onRightClick != null) onRightClick.accept(e, getSelectedLlibres());
                    return;
                }
                boolean ctrl  = (e.getModifiersEx() & java.awt.event.InputEvent.CTRL_DOWN_MASK)  != 0;
                boolean shift = (e.getModifiersEx() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0;
                if (shift && lastClickedIdx >= 0 && currentLlibres != null) {
                    int lo = Math.min(cardIdx, lastClickedIdx), hi = Math.max(cardIdx, lastClickedIdx);
                    List<Long> prev = new ArrayList<>(selectedISBNs);
                    if (!ctrl) selectedISBNs.clear();
                    for (int i = lo; i <= hi && i < currentLlibres.size(); i++)
                        selectedISBNs.add(currentLlibres.get(i).getISBN());
                    // Range change: repaint prev + new range (simplest = all involved)
                    Set<Long> changed = new java.util.HashSet<>(prev);
                    for (int i = lo; i <= hi && i < currentLlibres.size(); i++)
                        changed.add(currentLlibres.get(i).getISBN());
                    repaintCards(changed);
                } else if (ctrl) {
                    if (selectedISBNs.contains(isbn)) selectedISBNs.remove(isbn);
                    else selectedISBNs.add(isbn);
                    lastClickedIdx = cardIdx;
                    repaintCard(isbn);
                } else {
                    List<Long> prev = new ArrayList<>(selectedISBNs);
                    selectedISBNs.clear();
                    selectedISBNs.add(isbn);
                    lastClickedIdx = cardIdx;
                    repaintCards(prev);
                    repaintCard(isbn);
                }
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e) && onCardClick != null)
                    onCardClick.accept(l);
            }
        });

        // Async image load — pre-scale at current card dimensions
        cardMap.put(isbn, card);
        if (cached == null && !loading.contains(isbn)) {
            loading.add(isbn);
            new SwingWorker<BufferedImage, Void>() {
                @Override protected BufferedImage doInBackground() { return loadAndScale(l, capW, capH); }
                @Override protected void done() {
                    try {
                        BufferedImage img = get();
                        if (img != null) {
                            imageCache.put(isbn, img);
                            imgRef[0] = img;
                        }
                    } catch (Exception ignored) {
                    } finally {
                        loading.remove(isbn);
                        card.repaint();
                    }
                }
            }.execute();
        }

        return card;
    }

    // ── Cover painting ────────────────────────────────────────────────────────

    private static void paintCover(Graphics2D g2, BufferedImage img,
                                   int hue, String nom, String autor, int w, int h) {
        if (img != null) {
            // Image is pre-scaled to w×h — 1:1 blit, no scaling needed
            g2.drawImage(img, 0, 0, null);
        } else {
            // Gradient placeholder
            Color c1 = hsvApprox(hue,             0.55f, 0.38f);
            Color c2 = hsvApprox((hue + 45) % 360, 0.45f, 0.26f);
            g2.setPaint(new GradientPaint(0, 0, c1, w * 0.75f, h, c2));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(null);

            // Spine accent line
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
            + htmlEsc(l.getNom() != null ? l.getNom() : "") + "</b></body></html>");
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
              .append(on ? "#c8920a" : (UITheme.isDark ? "#555555" : "#dddddd"))
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
                g2.setColor(UITheme.isDark ? new Color(0x555555) : new Color(0xEEEEEE));
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

    // ── Image loading ─────────────────────────────────────────────────────────

    /** Load raw image bytes from blob/path, then crop-scale to exact card dimensions. */
    private static BufferedImage loadAndScale(Llibre l, int w, int h) {
        BufferedImage raw = loadRaw(l);
        if (raw == null) return null;
        return cropScale(raw, w, h);
    }

    private static BufferedImage loadRaw(Llibre l) {
        byte[] blob = l.getImatgeBlob();
        if (blob == null && l.hasBlob())
            blob = domini.ControladorDomini.getInstance().getLlibreBlob(l.getISBN());
        if (blob != null) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(blob));
                if (img != null) return img;
            } catch (Exception ignored) {}
        }
        String path = l.getImatge();
        if (path != null && !path.isEmpty()) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) return img;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** Center-crop and scale src to exactly w×h. Result is TYPE_INT_RGB for fast blitting. */
    private static BufferedImage cropScale(BufferedImage src, int w, int h) {
        double ia = (double) src.getWidth() / src.getHeight();
        double ba = (double) w / h;
        int dw, dh, dx, dy;
        if (ia > ba) { dh = h; dw = (int)(h * ia); dx = (w - dw) / 2; dy = 0; }
        else          { dw = w; dh = (int)(w / ia); dx = 0; dy = (h - dh) / 2; }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, dx, dy, dw, dh, null);
        g.dispose();
        return out;
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

    // ── WrapLayout ────────────────────────────────────────────────────────────

    private static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container t) { return layoutSize(t, true);  }
        @Override public Dimension minimumLayoutSize(Container t)   { return layoutSize(t, false); }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                Container parent = target.getParent();
                int tw = (parent instanceof JViewport) ? parent.getWidth() : 0;
                if (tw == 0) {
                    Container c = target;
                    while (c.getSize().width == 0 && c.getParent() != null) c = c.getParent();
                    tw = c.getSize().width;
                }
                if (tw == 0) tw = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets ins = target.getInsets();
                int maxW = tw - (ins.left + ins.right + hgap * 2);
                Dimension dim = new Dimension(0, 0);
                int rowW = 0, rowH = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowW + d.width > maxW) {
                        dim.width = Math.max(dim.width, rowW);
                        dim.height += rowH + vgap;
                        rowW = 0; rowH = 0;
                    }
                    if (rowW != 0) rowW += hgap;
                    rowW += d.width;
                    rowH = Math.max(rowH, d.height);
                }
                dim.width  = Math.max(dim.width, rowW);
                dim.height += rowH;
                dim.width  += ins.left + ins.right + hgap * 2;
                dim.height += ins.top  + ins.bottom + vgap * 2;
                return dim;
            }
        }
    }
}
