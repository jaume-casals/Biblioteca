package presentacio.galeria;

import domini.Llibre;
import herramienta.i18n.I18n;
import herramienta.ui.Tokens;
import herramienta.ui.UITheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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
 *
 * <p><b>Cap component HTML.</b> El peu es pintat directament sobre la
 * targeta: un {@code JLabel} amb HTML costa entre 3 i 10 ms de construir
 * (parse + layout del document HTML) i centenars de targetes convertien
 * l'obertura de la galeria en una espera de segons. El text del títol es
 * trenca en línies un sol cop, en construir la targeta, i les estrelles
 * es dibuixen amb {@code drawString} en lloc de fer-se amb HTML.
 */
public final class FabricantTargetesCoberta {

    // Constants Color en caché — mai no assignar dins del paint path.
    private static final Color SEL_FILL   = new Color(0, 120, 215, 40);
    private static final Color STAR_ON    = new Color(0xc8920a);
    private static final Color STAR_OFF_D = new Color(0x555555);
    private static final Color STAR_OFF_L = new Color(0xdddddd);
    private static final Color BAR_DARK   = new Color(0x555555);
    private static final Color BAR_LIGHT  = new Color(0xEEEEEE);

    private static final int ARC = 10;
    private static final int SDX = 3;
    private static final int SDY = 5;
    /** TargetaAmplada de la targeta: cardW + ombra. */
    private static final int EXTRA_W = SDX + 3;
    /** TargetaAlçada de la targeta: cardH + ombra. */
    private static final int EXTRA_H = SDY + 4;

    private static final int FOOT_PAD_X = 10;
    private static final int FOOT_PAD_TOP = 8;
    private static final int MAX_TITLE_LINES = 2;
    /** Alçada de la barra de progrés del peu, en píxels. */
    private static final int ALCADA_BARRA_PROGRESS = 3;

    /** Graphics2D auxiliar per obtenir FontMetrics sense construir una finestra. */
    private static final Graphics2D METRICS_G =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

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
     *
     * <p>{@link #onImageLoaded} rep la targeta quan la seva coberta ja és
     * a la memòria cau. L'amfitrió agrupa els repintats en finestres de
     * temps: sense agrupació, cada coberta que arriba provoca la seva pròpia
     * passada de pintat de tota la galeria.
     */
    public record AmfitrioTargeta(
            Set<Long> selectedISBNs,
            int[] focusedRef,
            java.util.function.Supplier<List<Llibre>> currentLlibres,
            Consumer<Llibre> onCardClicked,
            BiConsumer<MouseEvent, List<Llibre>> onCardRightClicked,
            Consumer<Iterable<Long>> onSelectionMutated,
            Consumer<JPanel> onImageLoaded) {

        public int focusedIdx()           { return focusedRef[0]; }
        public void posarFocusedIdx(int v)  { focusedRef[0] = v; }
    }

    private final ServeiImatgesCoberta imageService;
    private final int cardW;
    private final int cardH;
    private final int coverH;

    /**
     * Fons de la targeta pre-renderitzat (ombra + cos + contorn). Dues
     * ombres arrodonides antialiasades de 140x300 píxels costen més d'1 ms
     * cada targeta i repintada: renderitzades una sola vegada per nivell de
     * zoom, el pintat es redueix a un blit.
     */
    private BufferedImage fonsNormal;
    private BufferedImage fonsHover;

    public FabricantTargetesCoberta(ServeiImatgesCoberta imageService,
                            int cardW, int cardH, int coverH) {
        this.imageService = imageService;
        this.cardW = cardW;
        this.cardH = cardH;
        this.coverH = coverH;
    }

    /** Fons de la targeta per a l'estat de hover. */
    BufferedImage fons(boolean hover) {
        if (hover) {
            if (fonsHover == null) fonsHover = buildFons(true);
            return fonsHover;
        }
        if (fonsNormal == null) fonsNormal = buildFons(false);
        return fonsNormal;
    }

    /** Invalida el fons pre-renderitzat (canvi de tema o de zoom). */
    public void invalidarFons() {
        fonsNormal = null;
        fonsHover = null;
    }

    private BufferedImage buildFons(boolean hover) {
        int w = cardW + EXTRA_W, h = cardH + EXTRA_H;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            g.setColor(UITheme.palette().bgMain());
            g.fillRect(0, 0, w, h);

            // Superfície plana amb vora: cap ombra "3D" ni relleu. El hover
            // es comunica amb una vora més marcada, no amb una ombra.
            g.setColor(UITheme.palette().bgElevated());
            g.fillRoundRect(0, 0, cardW, cardH, ARC, ARC);
            g.setStroke(new BasicStroke(hover ? 1.6f : 1f));
            g.setColor(hover ? UITheme.palette().accentBorder() : UITheme.palette().borderClr());
            g.drawRoundRect(0, 0, cardW - 1, cardH - 1, ARC, ARC);
        } finally {
            g.dispose();
        }
        return img;
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
        final BufferedImage[] placeholder = { null };
        // Tema amb què s'ha renderitzat el marcador de portada. El marcador
        // es desa a memòria cau: sense aquest camp, en canviar de tema es
        // quedava dibuixat amb els colors del tema anterior.
        final UITheme.Tema[] placeholderTema = { null };

        // Dades del peu: text ja trencat en línies i mesures de font, perquè
        // el camí de pintat no hagi de mesurar text en cada repint.
        final DadesPeu peu = buildPeu(l, capW, lang);

        // Captura els camps de Llibre que paintCard necessita en un record
        // perquè el mètode estàtic de paint no depengui de JPanel (segons
        // el finding MEDIUM de tot.txt — extreure el paintComponent inline
        // fa que la lògica de pintat de la imatge sigui provable per separat).
        final EstatTargeta state = new EstatTargeta(
            l, isbn, hue, llegit, imgRef, hov, placeholder, placeholderTema, peu, this, capW, capH, capCardH, lang);

        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                paintCard((Graphics2D) g, state, host);
            }
            // Opaque: el blit del viewport (BACKINGSTORE) només és correcte
            // si la targeta omple tota la seva superfície, sinó queden
            // restes del contingut anterior.
            @Override public boolean isOpaque() { return true; }
        };
        card.setBackground(UITheme.palette().bgMain());
        card.setPreferredSize(new Dimension(capW + EXTRA_W, capCardH + EXTRA_H));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.setFocusable(true);
        card.addMouseListener(new AdaptadorRatxiTargeta(card, l, isbn, cardIdx, hov, host));

        // Càrrega d'imatge asíncrona — preescala a les dimensions actuals de la targeta
        imageService.submit(l, capW, capH, () -> {
            BufferedImage now = imageService.obtenirCached(isbn);
            if (now != null) { imgRef[0] = now; host.onImageLoaded().accept(card); }
        });

        return card;
    }

    /**
     * Instantaniània de només lectura de tot el que necessita
     * {@link #paintCard} per dibuixar una targeta. Porta les referències
     * vives a {@code imgRef}, {@code hov} i {@code placeholder} perquè les
     * mutacions del carregador d'imatges / mouse-Entered siguin visibles al
     * següent pintat.
     */
    private record EstatTargeta(
            Llibre l,
            Long isbn,
            int hue,
            boolean llegit,
            BufferedImage[] imgRef,
            boolean[] hov,
            BufferedImage[] placeholder,
            UITheme.Tema[] placeholderTema,
            DadesPeu peu,
            FabricantTargetesCoberta fabrica,
            int capW, int capH, int capCardH,
            String lang) {}

    /**
     * Dades del peu ja calculades: línies del títol, autor truncat,
     * valoració, any i progrés. Es construeix una vegada per targeta, de
     * manera que el pintat només faci {@code drawString}.
     */
    private static final class DadesPeu {
        final String[] liniesTitol;
        final Font fontTitol;
        final FontMetrics fmTitol;
        final String autor;
        final Font fontAutor;
        final FontMetrics fmAutor;
        final double valoracio;
        final String textValoracio;
        final Integer any;
        final boolean mostraProg;
        final float pct;
        final int pagines;
        final int paginesLlegides;
        final Font fontEstrelles;
        final FontMetrics fmEstrelles;

        DadesPeu(String[] liniesTitol, Font fontTitol, FontMetrics fmTitol,
                 String autor, Font fontAutor, FontMetrics fmAutor,
                 double valoracio, String textValoracio, Integer any, boolean mostraProg, float pct,
                 int pagines, int paginesLlegides,
                 Font fontEstrelles, FontMetrics fmEstrelles) {
            this.liniesTitol = liniesTitol;
            this.fontTitol = fontTitol;
            this.fmTitol = fmTitol;
            this.autor = autor;
            this.fontAutor = fontAutor;
            this.fmAutor = fmAutor;
            this.valoracio = valoracio;
            this.textValoracio = textValoracio;
            this.any = any;
            this.mostraProg = mostraProg;
            this.pct = pct;
            this.pagines = pagines;
            this.paginesLlegides = paginesLlegides;
            this.fontEstrelles = fontEstrelles;
            this.fmEstrelles = fmEstrelles;
        }

        // Accessors: el codi de pintat consumeix aquestes dades com a mètodes
        // (peu.fontTitol(), peu.liniesTitol(), …). Els camps es mantenen per
        // al codi que els llegeix com a camps (peu.liniesTitol.length).
        String[] liniesTitol() { return liniesTitol; }
        Font fontTitol() { return fontTitol; }
        FontMetrics fmTitol() { return fmTitol; }
        /**
         * Els colors del peu es resolen en pintar, no es guarden a la
         * targeta: el text es mesura una sola vegada (que és el que costa),
         * però el color depèn del tema i ha de seguir el tema actiu.
         */
        Color colorTitol() { return UITheme.palette().textDark(); }
        String autor() { return autor; }
        Font fontAutor() { return fontAutor; }
        FontMetrics fmAutor() { return fmAutor; }
        Color colorAutor() { return UITheme.palette().textMid(); }
        double valoracio() { return valoracio; }
        String textValoracio() { return textValoracio; }
        Integer any() { return any; }
        int pagines() { return pagines; }
        int paginesLlegides() { return paginesLlegides; }
        boolean mostraProg() { return mostraProg; }
        float pct() { return pct; }
        Font fontEstrelles() { return fontEstrelles; }
        FontMetrics fmEstrelles() { return fmEstrelles; }
        Color colorEstrellaOn() { return STAR_ON; }
        Color colorEstrellaOff() { return UITheme.esDark() ? STAR_OFF_D : STAR_OFF_L; }
    }

    private static DadesPeu buildPeu(Llibre l, int cardW, String lang) {
        Font fontTitol = UITheme.fontSmall().deriveFont(Font.BOLD);
        Font fontAutor = UITheme.fontSmall();
        Font fontEstrelles = new Font("Dialog", Font.PLAIN, 11);
        FontMetrics fmTitol = metrics(fontTitol);
        FontMetrics fmAutor = metrics(fontAutor);
        FontMetrics fmEstrelles = metrics(fontEstrelles);

        String titol = l.obtenirDisplayNom(lang);
        List<String> linies = wrapText(titol, fmTitol, cardW - FOOT_PAD_X * 2 - 2, MAX_TITLE_LINES);

        // La valoració té escala 0–10 i el valor per defecte de la base de
        // dades és 0: un 0 vol dir "sense valorar", no "valoració zero". Per
        // això no es mostren estrelles buides a tots els llibres: es mostra
        // la xifra, i només quan hi ha valoració real.
        Double valoracioLliure = l.obtenirValoracio();
        double val = valoracioLliure != null ? valoracioLliure : 0.0;
        boolean teValoracio = valoracioLliure != null && valoracioLliure > 0;
        String textValoracio = teValoracio ? formatValoracio(valoracioLliure) : null;

        int pagines = l.obtenirPagines();
        int pagLleg = l.obtenirPaginesLlegides();
        double pct = pagines > 0 ? (double) pagLleg / pagines : 0.0;
        boolean llegit = Boolean.TRUE.equals(l.obtenirLlegit());
        // Progrés vàlid: pàgines conegudes i encara no acabat. El 0% real
        // (pàgines conegudes, cap llegida) també es mostra.
        boolean mostrarProg = pagines > 0 && !llegit;

        return new DadesPeu(
            linies.toArray(new String[0]), fontTitol, fmTitol,
            truncate(l.obtenirAutor(), 24), fontAutor, fmAutor,
            val, textValoracio, l.obtenirAny(), mostrarProg, (float) Math.min(1.0, Math.max(0.0, pct)),
            pagines, pagLleg,
            fontEstrelles, fmEstrelles);
    }

    /**
     * Formata la valoració amb un decimal i sense decimals innecessaris.
     *
     * <p>Es fa a mà, sense instanciar un {@code DecimalFormat} per targeta:
     * la galeria construeix centenars de targetes i crear un formatador per
     * cadascuna és un cost que es nota en biblioteques grans.
     */
    private static String formatValoracio(Double v) {
        if (v == null) return null;
        double valor = Math.round(v * 10.0) / 10.0;
        long enters = (long) valor;
        int decimals = (int) Math.round((valor - enters) * 10);
        java.text.DecimalFormatSymbols simbols =
            java.text.DecimalFormatSymbols.getInstance(java.util.Locale.getDefault());
        String sep = String.valueOf(simbols.getDecimalSeparator());
        if (decimals == 0) return String.valueOf(enters);
        return String.valueOf(enters) + sep + decimals;
    }

    private static FontMetrics metrics(Font f) {
        return METRICS_G.getFontMetrics(f);
    }

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

        boolean h = state.hov()[0];

        // Fons pre-renderitzat (ombra + cos + contorn): un blit en lloc de
        // dues ombres arrodonides antialiasades per targeta i repint.
        g2.drawImage(state.fabrica().fons(h), 0, 0, null);

        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(0, 0, capW, capH + ARC, ARC, ARC));
        paintCover(g2, state, capW, capH);
        g2.setClip(oldClip);

        paintPeu(g2, state, capW);

        paintEstat(g2, state, capW, capH);

        boolean sel = host.selectedISBNs().contains(state.isbn());
        if (sel) {
            g2.setColor(SEL_FILL);
            g2.fillRoundRect(1, 1, capW - 2, capCardH - 2, ARC, ARC);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(UITheme.palette().accent());
            g2.drawRoundRect(0, 0, capW - 1, capCardH - 1, ARC, ARC);
        }

        g2.dispose();
    }

    /**
     * Etiqueta d'estat sobre la portada.
     *
     * <p>El text és el que comunica l'estat; el color només l'acompanya. Un
     * punt de color sol no diu què vol dir, i per a una biblioteca amb
     * molts llibres l'estat ha de llegir-se d'un cop d'ull.
     */
    private static void paintEstat(Graphics2D g2, EstatTargeta state, int capW, int capH) {
        DadesPeu peu = state.peu();
        String text = null;
        Color bg = null;
        Color fg = null;
        if (state.llegit()) {
            text = I18n.t("card_status_read");
            bg = UITheme.palette().greenSoft();
            fg = UITheme.palette().green();
        } else if (peu.mostraProg() && peu.pct() > 0) {
            text = I18n.t("card_status_reading");
            bg = UITheme.palette().accentSoft();
            fg = UITheme.palette().accent();
        }
        if (text == null) return;

        Font font = fontEtiquetaEstat();
        FontMetrics fm = g2.getFontMetrics(font);
        int w = fm.stringWidth(text) + 12;
        int h = fm.getHeight() + 2;
        int x = 6, y = 6;
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setFont(font);
        g2.setColor(fg);
        g2.drawString(text, x + 6, y + fm.getAscent() + 1);
    }

    /**
     * Font de l'etiqueta d'estat.
     *
     * <p>En caché: es crea una targeta per llibre i es repinta en cada
     * hover, i derivar la font a cada pintat crea un objecte innecessari.
     */
    private static Font fontEtiquetaEstat() {
        Font f = fontEtiquetaEstatCache;
        if (f == null) {
            f = UITheme.fontSmall().deriveFont(Font.BOLD, (float) (Tokens.FONT_SIZE_SMALL - 2));
            fontEtiquetaEstatCache = f;
        }
        return f;
    }

    private static volatile Font fontEtiquetaEstatCache;

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

    private static void paintCover(Graphics2D g2, EstatTargeta state, int w, int h) {
        BufferedImage img = state.imgRef()[0];
        if (img != null) {
            g2.drawImage(img, 0, 0, null);
            return;
        }
        // Sense coberta: el gradient i el text es renderitzen una sola vegada
        // i es reutilitzen com a imatge, en lloc de refe-los a cada repint.
        // El marcador depèn del tema, així que es torna a fer quan el tema
        // canvia: si no, en mode clar quedarien les cobertes negres del
        // mode fosc.
        BufferedImage ph = state.placeholder()[0];
        UITheme.Tema tema = UITheme.obtenirTheme();
        if (ph == null || state.placeholderTema()[0] != tema) {
            ph = buildPlaceholder(state, w, h);
            state.placeholder()[0] = ph;
            state.placeholderTema()[0] = tema;
        }
        g2.drawImage(ph, 0, 0, null);
    }

    /**
     * Marcador de portada absent.
     *
     * <p>Una superfície neutra amb la icona de llibre al centre. No hi ha
     * degradat (és decoració, no informació) ni el títol repetit dins de la
     * portada: el títol ja és a sota i repetir-lo carrega la targeta i
     * duplica la informació.
     */
    private static BufferedImage buildPlaceholder(EstatTargeta state, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            g2.setColor(UITheme.palette().bgSunken());
            g2.fillRect(0, 0, w, h);
            int midaIcona = Math.max(24, Math.min(56, w / 3));
            javax.swing.Icon icona = presentacio.util.Icones.icon("llibres", midaIcona,
                UITheme.palette().textMid());
            if (icona != null) {
                icona.paintIcon(null, g2, (w - midaIcona) / 2, (h - midaIcona) / 2);
            }
        } finally {
            g2.dispose();
        }
        return out;
    }

    /**
     * Peu de la targeta.
     *
     * <p>Jerarquia: títol (fins a dues línies), autor, valoració i any, i
     * progrés. Cap element surt de la targeta: el títol s'ajusta al nombre
     * de línies permès i l'autor es retalla amb punts suspensius.
     */
    private static void paintPeu(Graphics2D g2, EstatTargeta state, int cardW) {
        DadesPeu peu = state.peu();
        int x = FOOT_PAD_X;
        int limit = cardW - FOOT_PAD_X;
        int y = state.capH() + FOOT_PAD_TOP;

        if (peu.liniesTitol.length > 0) {
            g2.setFont(peu.fontTitol);
            g2.setColor(peu.colorTitol());
            for (String line : peu.liniesTitol) {
                y += peu.fmTitol.getAscent();
                g2.drawString(line, x, y);
                y += peu.fmTitol.getDescent() + peu.fmTitol.getLeading();
            }
        }

        y += 2;
        g2.setFont(peu.fontAutor);
        g2.setColor(peu.colorAutor());
        y += peu.fmAutor.getAscent();
        g2.drawString(peu.autor, x, y);
        y += peu.fmAutor.getDescent() + peu.fmAutor.getLeading();

        // Valoració i any a la mateixa línia: la valoració és discreta i
        // només apareix quan el llibre en té.
        y += 4;
        int baseline = y + peu.fmEstrelles.getAscent();
        if (peu.textValoracio() != null) {
            g2.setFont(peu.fontEstrelles);
            g2.setColor(peu.colorEstrellaOn());
            g2.drawString("★", x, baseline);
            int cursorX = x + peu.fmEstrelles.charWidth('★') + 3;
            g2.setFont(peu.fontAutor);
            g2.setColor(peu.colorAutor());
            g2.drawString(peu.textValoracio(), cursorX, baseline);
        }
        if (peu.any() != null && peu.any() > 0) {
            String any = String.valueOf(peu.any());
            g2.setFont(peu.fontAutor);
            g2.setColor(peu.colorAutor());
            g2.drawString(any, limit - peu.fmAutor.stringWidth(any), baseline);
        }

        // Progrés: text de pàgines a la dreta i barra fina al costat, a la
        // mateixa línia. L'avanç vertical és explícit: si no, el text del
        // progrés cau damunt de l'any.
        if (peu.mostraProg()) {
            y = baseline + peu.fmAutor.getDescent() + Tokens.SPACE_1;
            g2.setFont(peu.fontAutor);
            g2.setColor(peu.colorAutor());
            String text = peu.paginesLlegides() + " / " + peu.pagines();
            int textW = g2.getFontMetrics().stringWidth(text);
            int textBaseline = y + peu.fmAutor.getAscent();
            g2.drawString(text, limit - textW, textBaseline);

            int barW = limit - x - textW - Tokens.SPACE_3;
            if (barW > Tokens.SPACE_4) {
                int barY = textBaseline - ALCADA_BARRA_PROGRESS - 1;
                g2.setColor(UITheme.esDark() ? BAR_DARK : BAR_LIGHT);
                g2.fillRoundRect(x, barY, barW, ALCADA_BARRA_PROGRESS, 2, 2);
                g2.setColor(UITheme.palette().accent());
                int omplert = (int) Math.round(barW * peu.pct());
                if (omplert > 0) {
                    g2.fillRoundRect(x, barY, Math.max(2, omplert), ALCADA_BARRA_PROGRESS, 2, 2);
                }
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static int bookHue(String nom) {
        if (nom == null || nom.isEmpty()) return 200;
        int h = 0;
        for (char c : nom.toCharArray()) h = h * 31 + c;
        return Math.floorMod(h, 360);
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
}
