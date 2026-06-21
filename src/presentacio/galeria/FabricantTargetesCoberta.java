package presentacio.galeria;

import domini.Llibre;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Construeix els panells visuals de targeta que omplen la galeria.
 *
 * <p>Cada targeta té el seu propi camí de pintat (cos de targeta, peu,
 * barra de progrés, estrelles) i està connectada a un
 * {@link AmfitrioTargeta} perquè el panell amfitrió es mantingui
 * sincronitzat amb els esdeveniments de selecció / focus / obertura.
 *
 * <p>La targeta rep el {@link ServeiImatgesCoberta} perquè les càrregues
 * d'imatge asíncrones actualitzin la referència de pintat de la targeta
 * a l'EDT.
 */
public final class FabricantTargetesCoberta {

    // Constants Color en caché — mai no assignar dins del paint path.
    // A = ombra externa, B = ombra interna; prefix H = hover (alfa més fort).
    private static final Color SHADOW_A   = new Color(0, 0, 0, 14);  // externa, normal
    private static final Color SHADOW_B   = new Color(0, 0, 0, 6);   // interna, normal
    private static final Color SHADOW_HA  = new Color(0, 0, 0, 30);  // externa, hover
    private static final Color SHADOW_HB  = new Color(0, 0, 0, 14);  // interna, hover
    private static final Color DOT_READ   = new Color(0x2D9E6B);
    private static final Color DOT_UNREAD = new Color(0xBBBBBB);
    private static final Color DOT_RIM    = new Color(255, 255, 255, 200);
    private static final Color SEL_FILL   = new Color(0, 120, 215, 40);

    private static final int ARC = 10;
    private static final int SDX = 3;
    private static final int SDY = 5;

    /**
     * Estat + callbacks de la banda de l'amfitrió compartits per cada
     * targeta construïda per aquesta fàbrica.
     *
     * <p>Substitueix l'antiga interfície {@code Listener} de 7 mètodes
     * (troballa MEDIUM de tot.txt): l'amfitrió ({@code PanelGaleriaCobertes})
     * construeix un sol {@code AmfitrioTargeta} que captura el seu estat
     * viu de selecció / focus més tres consumidors de callback, i el passa
     * a cada targeta. L'adaptador de ratolí de la targeta llegeix
     * {@link #selectedISBNs()} / {@link #focusedIdx()} /
     * {@link #currentLlibres()} per a accés de només lectura, muta
     * {@link #focusedIdx()} via l'embolcall int[], i envia
     * {@link #onCardClicked}, {@link #onCardRightClicked} i
     * {@link #onSelectionMutated} en entrar l'usuari.
     *
     * <p>L'embolcall {@code int[]} per a {@code focusedIdx} és el
     * portador d'enter mutable més senzill que sobreviu tant a la captura
     * lambda com a la mutació de l'adaptador de ratolí per targeta sense
     * recórrer a {@code AtomicInteger}.
     */
    public record AmfitrioTargeta(
            Set<Long> selectedISBNs,
            int[] focusedRef,
            java.util.function.Supplier<List<Llibre>> currentLlibres,
            Consumer<Llibre> onCardClicked,
            BiConsumer<MouseEvent, List<Llibre>> onCardRightClicked,
            Consumer<Iterable<Long>> onSelectionMutated) {

        public int focusedIdx()           { return focusedRef[0]; }
        public void posarFocusedIdx(int v)  { focusedRef[0] = v; }
    }

    private final ServeiImatgesCoberta imageService;
    private final int cardW;
    private final int cardH;
    private final int coverH;
    private final int footH;

    public FabricantTargetesCoberta(ServeiImatgesCoberta imageService,
                            int cardW, int cardH, int coverH, int footH) {
        this.imageService = imageService;
        this.cardW = cardW;
        this.cardH = cardH;
        this.coverH = coverH;
        this.footH = footH;
    }

    public JPanel build(Llibre l, int cardIdx, AmfitrioTargeta host, String lang) {
        final int capW = cardW, capH = coverH, capCardH = cardH;
        final int hue = bookHue(l.obtenirNom());
        final boolean llegit = Boolean.TRUE.equals(l.obtenirLlegit());
        final Long isbn = l.obtenirISBN();

        // Estat de la targeta — capturat en temps de construcció, mutat pels
        // listeners de ratolí (hover) i el carregador d'imatges (imgRef).
        // Pintat a paintCard().
        final BufferedImage[] imgRef = { imageService.obtenirCached(isbn) };
        final boolean[] hov = { false };

        // Captura els camps de Llibre que paintCard necessita en un record
        // perquè el mètode estàtic de paint no depengui de JPanel (segons
        // el finding MEDIUM de tot.txt — extreure el paintComponent inline
        // fa que la lògica de pintat de la imatge sigui provable per separat).
        final EstatTargeta state = new EstatTargeta(
            l, isbn, hue, llegit, imgRef, hov, capW, capH, capCardH, lang);

        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                paintCard((Graphics2D) g, state, host);
            }
            @Override public boolean isOpaque() { return false; }
        };
        card.setPreferredSize(new Dimension(capW + SDX + 3, capCardH + SDY + 4));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        double val = l.obtenirValoracio() != null ? l.obtenirValoracio() : 0.0;
        int pagines = l.obtenirPagines();
        int pagLleg = l.obtenirPaginesLlegides();
        double pct = pagines > 0 ? (double) pagLleg / pagines : 0.0;
        boolean mostrarProg = pagines > 0 && !llegit && pct > 0;
        JPanel footer = buildFooter(l, val, mostrarProg, pct, capW, lang);
        footer.setBounds(0, capH, capW, footH);
        card.add(footer);

        card.setFocusable(true);
        card.addMouseListener(new AdaptadorRatxiTargeta(card, l, isbn, cardIdx, hov, host));

        // Càrrega d'imatge asíncrona — preescala a les dimensions actuals de la targeta
        imageService.submit(l, capW, capH, () -> {
            BufferedImage now = imageService.obtenirCached(isbn);
            if (now != null) { imgRef[0] = now; card.repaint(); }
        });

        return card;
    }

    /**
     * Instantaniània de només lectura de tot el que necessita
     * {@link #paintCard} per dibuixar una targeta. Porta les referències
     * vives a {@code imgRef} i {@code hov} perquè les mutacions del
     * carregador d'imatges / mouse-Entered siguin visibles al següent
     * pintat.
     */
    private record EstatTargeta(
            Llibre l,
            Long isbn,
            int hue,
            boolean llegit,
            BufferedImage[] imgRef,
            boolean[] hov,
            int capW, int capH, int capCardH,
            String lang) {}

    /**
     * Pintat estàtic de targeta — extret del paintComponent del JPanel
     * anònim (segons la troballa MEDIUM de tot.txt). Tots els paràmetres
     * de dibuix venen del record {@link EstatTargeta}; l'única interacció
     * amb l'amfitrió és la comprovació de selecció. Es pot provar
     * aïlladament construint un EstatTargeta i un AmfitrioTargeta mock.
     */
    static void paintCard(Graphics2D g, EstatTargeta state, AmfitrioTargeta host) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int capW = state.capW(), capH = state.capH(), capCardH = state.capCardH();

        g2.setColor(UITheme.palette().bgMain());
        g2.fillRect(0, 0, capW, capCardH);

        boolean h = state.hov()[0];

        int sdY = h ? SDY + 6 : SDY;
        g2.setColor(h ? SHADOW_HA : SHADOW_A);
        g2.fillRoundRect(SDX, sdY, capW, capCardH, ARC + 2, ARC + 2);
        g2.setColor(h ? SHADOW_HB : SHADOW_B);
        g2.fillRoundRect(SDX + 2, sdY + 3, capW + 1, capCardH + 1, ARC + 4, ARC + 4);

        g2.setColor(UITheme.palette().bgPanel());
        g2.fillRoundRect(0, 0, capW, capCardH, ARC, ARC);

        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(0, 0, capW, capH + ARC, ARC, ARC));
        paintCover(g2, state.imgRef()[0], state.hue(),
            state.l().obtenirDisplayNom(state.lang()), state.l().obtenirAutor(), capW, capH);
        g2.setClip(oldClip);

        int dotD = 9, dotX = capW - dotD - 7, dotY = 7;
        g2.setColor(state.llegit() ? DOT_READ : DOT_UNREAD);
        g2.fillOval(dotX, dotY, dotD, dotD);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(DOT_RIM);
        g2.drawOval(dotX, dotY, dotD, dotD);

        boolean sel = host.selectedISBNs().contains(state.isbn());
        if (sel) {
            g2.setColor(SEL_FILL);
            g2.fillRoundRect(1, 1, capW - 2, capCardH - 2, ARC, ARC);
        }

        g2.setStroke(new BasicStroke(sel || h ? 2f : 1f));
        Color bdr;
        if (sel) bdr = UITheme.palette().accent();
        else if (h) bdr = new Color(UITheme.palette().accent().getRed(), UITheme.palette().accent().getGreen(), UITheme.palette().accent().getBlue(), 140);
        else bdr = new Color(UITheme.palette().borderClr().getRed(), UITheme.palette().borderClr().getGreen(), UITheme.palette().borderClr().getBlue(), 180);
        g2.setColor(bdr);
        g2.drawRoundRect(0, 0, capW - 1, capCardH - 1, ARC, ARC);

        g2.dispose();
    }

    /**
     * Adaptador de ratolí estàtic — extret del MouseAdapter en línia
     * (segons la troballa MEDIUM de tot.txt). Gestiona els repaints
     * de hover i la selecció / clic dret / doble clic. L'adaptador
     * no té estat més enllà de les referències que captura a la
     * construcció.
     */
    private static final class AdaptadorRatxiTargeta extends MouseAdapter {
        private final JPanel card;
        private final Llibre l;
        private final long isbn;
        private final int cardIdx;
        private final boolean[] hov;
        private final AmfitrioTargeta host;

        AdaptadorRatxiTargeta(JPanel card, Llibre l, long isbn, int cardIdx, boolean[] hov, AmfitrioTargeta host) {
            this.card = card;
            this.l = l;
            this.isbn = isbn;
            this.cardIdx = cardIdx;
            this.hov = hov;
            this.host = host;
        }

        @Override public void mouseEntered(MouseEvent e) { hov[0] = true; card.repaint(); }
        @Override public void mouseExited (MouseEvent e) { hov[0] = false; card.repaint(); }

        @Override public void mousePressed(MouseEvent e) {
            card.requestFocusInWindow();
            if (SwingUtilities.isRightMouseButton(e)) {
                Set<Long> sel = host.selectedISBNs();
                if (!sel.contains(isbn)) {
                    List<Long> prev = new ArrayList<>(sel);
                    sel.clear();
                    sel.add(isbn);
                    host.onSelectionMutated().accept(prev);
                }
                host.onCardRightClicked().accept(e, collectSelected(host));
                return;
            }
            boolean ctrl  = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK)  != 0;
            boolean shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
            Set<Long> sel = host.selectedISBNs();
            if (shift && host.focusedIdx() >= 0) {
                int lo = Math.min(cardIdx, host.focusedIdx());
                int hi = Math.max(cardIdx, host.focusedIdx());
                List<Llibre> all = host.currentLlibres().get();
                List<Long> prev = new ArrayList<>(sel);
                if (!ctrl) sel.clear();
                java.util.HashSet<Long> changed = new java.util.HashSet<>(prev);
                for (int i = lo; i <= hi && i < all.size(); i++) {
                    sel.add(all.get(i).obtenirISBN());
                    changed.add(all.get(i).obtenirISBN());
                }
                host.onSelectionMutated().accept(changed);
            } else if (ctrl) {
                if (sel.contains(isbn)) sel.remove(isbn);
                else sel.add(isbn);
                host.posarFocusedIdx(cardIdx);
                host.onSelectionMutated().accept(java.util.Collections.singletonList(isbn));
            } else {
                List<Long> prev = new ArrayList<>(sel);
                sel.clear();
                sel.add(isbn);
                host.posarFocusedIdx(cardIdx);
                host.onSelectionMutated().accept(prev);
                host.onSelectionMutated().accept(java.util.Collections.singletonList(isbn));
            }
        }

        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
                host.onCardClicked().accept(l);
        }
    }

    private static List<Llibre> collectSelected(AmfitrioTargeta host) {
        List<Llibre> out = new ArrayList<>();
        Set<Long> sel = host.selectedISBNs();
        for (Llibre l : host.currentLlibres().get())
            if (sel.contains(l.obtenirISBN())) out.add(l);
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
                int iniciarY = Math.max(fm.getAscent() + 10,
                                      (h - lines.size() * lineH - autorH) / 2 + fm.getAscent());
                g2.setColor(new Color(255, 255, 255, 235));
                for (int i = 0; i < lines.size(); i++)
                    g2.drawString(lines.get(i), 16, iniciarY + i * lineH);
                if (autor != null && !autor.isEmpty()) {
                    g2.setColor(new Color(255, 255, 255, 120));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.drawString(truncate(autor, 20), 16, iniciarY + lines.size() * lineH + 5);
                }
            }
        }
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private static JPanel buildFooter(Llibre l, double val, boolean mostrarProg, double pct, int cardW, String lang) {
        JPanel f = new JPanel();
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(8, 10, 6, 10));

        JLabel lblT = new JLabel("<html><body style='width:" + (cardW - 22) + "px; margin:0; padding:0;'><b>"
            + htmlEsc(l.obtenirDisplayNom(lang)) + "</b></body></html>");
        lblT.setFont(UITheme.fontSmall().deriveFont(Font.BOLD));
        lblT.setForeground(UITheme.palette().textDark());
        lblT.setAlignmentX(0f);
        f.add(lblT);
        f.add(Box.createVerticalStrut(2));

        JLabel lblA = new JLabel(truncate(l.obtenirAutor(), 24));
        lblA.setFont(UITheme.fontSmall());
        lblA.setForeground(UITheme.palette().textMid());
        lblA.setAlignmentX(0f);
        f.add(lblA);
        f.add(Box.createVerticalStrut(4));

        JPanel starsRow = new JPanel(new BorderLayout(4, 0));
        starsRow.setOpaque(false);
        starsRow.setAlignmentX(0f);
        starsRow.setMaximumSize(new Dimension(cardW - 20, 16));
        starsRow.add(starsLabel(val), BorderLayout.WEST);
        if (l.obtenirAny() != null && l.obtenirAny() > 0) {
            JLabel yr = new JLabel(String.valueOf(l.obtenirAny()));
            yr.setFont(UITheme.fontSmall());
            yr.setForeground(UITheme.palette().textMid());
            starsRow.add(yr, BorderLayout.EAST);
        }
        f.add(starsRow);

        if (mostrarProg) {
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
              .append(on ? "#c8920a" : (UITheme.esDark() ? "#555555" : "#dddddd"))
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
                g2.setColor(UITheme.esDark() ? new Color(0x555555) : new Color(0xEEEEEE));
                g2.fillRoundRect(0, 0, getWidth(), 3, 2, 2);
                if (p > 0) {
                    g2.setColor(UITheme.palette().accent());
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
