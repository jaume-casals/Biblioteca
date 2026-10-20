package presentacio.panells;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;

import domini.Llibre;
import herramienta.config.Configuracio;
import herramienta.ui.UITheme;
import herramienta.config.ConfiguracioUi;
import persistencia.contract.EscritorBiblioteca;
import presentacio.galeria.FabricantTargetesCoberta;
import presentacio.galeria.ServeiImatgesCoberta;
import presentacio.galeria.PopupZoomCoberta;
import presentacio.layouts.WrapLayout;

/**
 * Graella de cobertes de llibres, la vista de galeria de la biblioteca.
 *
 * <p>Composició:
 * <ul>
 *   <li>{@link ServeiImatgesCoberta} — memòria cau LRU asíncrona d'imatges
 *       + retall-escalat.</li>
 *   <li>{@link FabricantTargetesCoberta} — pinta cada targeta, connecta
 *       els esdeveniments de ratolí i selecció a través d'un
 *       {@link FabricantTargetesCoberta.AmfitrioTargeta}.</li>
 *   <li>{@link PopupZoomCoberta} — superposició de zoom en passar el ratolí
 *       que es mostra al costat d'una targeta.</li>
 * </ul>
 *
 * <p>Aquesta classe posseeix el panell de desplaçament, el wrap layout,
 * l'estat de selecció / focus, les dreceres de teclat i l'aplicació de
 * tema. La feina visual i d'imatges real viu en els tres col·laboradors
 * anteriors.
 */
public class PanelGaleriaCobertes extends JPanel {

    private record NivellZoom(int cardW, int coverH) {}
    /**
     * Nivells de densitat de la galeria.
     *
     * <p>El nivell per defecte és el 190 px de targeta: prou ample per a un
     * títol de dues línies i prou compacte per veure molts llibres. Els
     * nivells són orientatius i l'usuari els pot canviar amb Ctrl+±.
     */
    private static final NivellZoom[] ZOOM_LEVELS = {
        new NivellZoom(150, 225),
        new NivellZoom(170, 255),
        new NivellZoom(190, 285),
        new NivellZoom(210, 315),
        new NivellZoom(230, 345),
    };
    private static final int   FOOT_H    = 92;
    private static final int   GAP       = 14;
    /** Finestra d agrupació dels repintats de cobertes que acaben d'arribar. */
    private static final int   REPINT_MS = 40;

    private final ServeiImatgesCoberta imageService = new ServeiImatgesCoberta();
    private final PopupZoomCoberta    zoomPopup    = new PopupZoomCoberta();

    private int zoomLevel = Configuracio.obtenirGalleryZoom();
    private int CARD_W;
    private int COVER_H;
    private int CARD_H;
    private FabricantTargetesCoberta cardFactory;

    private final JPanel wrap;
    private JScrollPane scroll;

    private final Set<Long> selectedISBNs = new LinkedHashSet<>();
    private final java.util.HashMap<Long, JPanel> cardMap = new java.util.HashMap<>();
    private List<Llibre> currentLlibres;
    private final int[] focusedRef = { -1 };

    /**
     * Targetes que ja tenen coberta i esperen repint. Les cobertes arriben
     * de una en una, i un repint per coberta provocaria una passada completa
     * de pintat de la galeria per cada imatge. S'agrupen en finestres de
     * {@link #REPINT_MS} ms.
     */
    private final Set<JPanel> targetesPendents = new LinkedHashSet<>();
    private final javax.swing.Timer timerRepint = new javax.swing.Timer(REPINT_MS, e -> flushRepints());

    private Consumer<Llibre> onCardClick;
    private BiConsumer<MouseEvent, List<Llibre>> onRightClick;
    private Consumer<List<Llibre>> onDeleteSelected;
    private EscritorBiblioteca cd;
    private final FabricantTargetesCoberta.AmfitrioTargeta cardHost;

    public PanelGaleriaCobertes() {
        setLayout(new BorderLayout());
        setBackground(UITheme.palette().bgMain());

        aplicarZoom(zoomLevel);

        // Contenidor de la graella. Swing ja només pinta els fills que
        // toquen el clip, de manera que no cal cap paintChildren propi:
        // una implementació pròpia es saltava els estats internes de
        // Swing (memòria cau, banderes de repint) i les targetes quedaven
        // sense pintar a la pantalla fins que el ratolí passava per damunt.
        wrap = new JPanel(new WrapLayout(FlowLayout.LEADING, GAP, GAP));
        wrap.setBackground(UITheme.palette().bgMain());

        JScrollPane scroll = new JScrollPane(wrap);
        this.scroll = scroll;
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(60);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UITheme.palette().bgMain());
        // BACKINGSTORE blita el contingut en lloc de repintar-lo sencer a
        // cada pas de desplaçament. Les targetes pinten tota la seva
        // superfície (són opaques), de manera que el blit no deixa artefactes.
        scroll.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        add(scroll, BorderLayout.CENTER);

        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                wrap.revalidate();
            }
        });

        scroll.addMouseWheelListener(e -> {
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                // Segons el contracte d'AWT, la roda ennegada (cap avall,
                // cap al usuari) dona una rotació positiva i la cap amunt una
                // negativa. La cap amunt amplia, igual que Ctrl+=/Ctrl+-.
                adjustZoom(e.getWheelRotation() < 0 ? 1 : -1);
                // Consumit: amb Ctrl no es desplaça el contingut.
                e.consume();
            }
            // Sense Ctrl no hi ha res a fer: el JScrollPane ja desplaça el
            // contingut en la direcció correcta. Adjustar aquí el valor de
            // la barra sumava un segon desplaçament al del comportament per
            // defecte, i la roda feia saltar el contingut el doble de llarg.
        });

        setFocusable(true);
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "galDelete");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),  "galRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,  0),  "galLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,  0),  "galDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,    0),  "galUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),  "galEnter");
        bindAction("galDelete", e -> {
            List<Llibre> sel = obtenirSelectedLlibres();
            if (!sel.isEmpty() && onDeleteSelected != null) onDeleteSelected.accept(sel);
        });
        bindAction("galRight", e -> moureKeyboard(1));
        bindAction("galLeft", e -> moureKeyboard(-1));
        bindAction("galDown", e -> moureKeyboard(computeCols()));
        bindAction("galUp", e -> moureKeyboard(-computeCols()));
        bindAction("galEnter", e -> {
            if (focusedRef[0] >= 0 && currentLlibres != null
                    && focusedRef[0] < currentLlibres.size() && onCardClick != null)
                onCardClick.accept(currentLlibres.get(focusedRef[0]));
        });

        cardHost = new FabricantTargetesCoberta.AmfitrioTargeta(
            selectedISBNs,
            focusedRef,
            () -> currentLlibres,
            l -> { if (onCardClick != null) onCardClick.accept(l); },
            (e, sel) -> { if (onRightClick != null) onRightClick.accept(e, sel); },
            changedIsbns -> repaintCards(changedIsbns),
            this::marcarRepint);
    }

    /**
     * Marca la targeta com a pendent de repint. El repint real es fa en
     * finestres de temps, de manera que les cobertes que arriben juntes
     * comparteixen una sola passada de pintat.
     *
     * <p>Les targetes fora de pantalla no es repinten: quan l'usuari hi
     * arribi, es pintaran amb la coberta que ja ha arribat. Sense això,
     * cada coberta carregada provocaria una passada de repint de la
     * galeria sencera.
     */
    private void marcarRepint(JPanel targeta) {
        if (targeta == null) return;
        if (!esVisible(targeta)) return;
        targetesPendents.add(targeta);
        if (!timerRepint.isRunning()) timerRepint.start();
    }

    /** Indica si la targeta toca la regió visible del viewport. */
    private boolean esVisible(JPanel targeta) {
        if (scroll == null) return true;
        JViewport viewport = scroll.getViewport();
        if (viewport == null) return true;
        Point vista = viewport.getViewPosition();
        Dimension extent = viewport.getExtentSize();
        Rectangle b = targeta.getBounds();
        return b.x < vista.x + extent.width
            && b.x + b.width > vista.x
            && b.y < vista.y + extent.height
            && b.y + b.height > vista.y;
    }

    private void flushRepints() {
        if (targetesPendents.isEmpty()) {
            timerRepint.stop();
            return;
        }
        Set<JPanel> pendents = new LinkedHashSet<>(targetesPendents);
        targetesPendents.clear();
        for (JPanel c : pendents) c.repaint();
    }

    private void bindAction(String key, java.util.function.Consumer<ActionEvent> body) {
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                body.accept(e);
            }
        });
    }

    public void posarCd(EscritorBiblioteca cd) {
        this.cd = cd;
        imageService.posarCd(cd);
    }

    public void posarOnCardClick(Consumer<Llibre> cb)  { this.onCardClick = cb; }
    public void posarOnRightClick(BiConsumer<MouseEvent, List<Llibre>> cb) { this.onRightClick = cb; }
    public void posarOnDeleteSelected(Consumer<List<Llibre>> cb) { this.onDeleteSelected = cb; }

    public List<Llibre> obtenirSelectedLlibres() {
        if (currentLlibres == null || selectedISBNs.isEmpty()) return Collections.emptyList();
        List<Llibre> result = new ArrayList<>();
        for (Llibre l : currentLlibres)
            if (selectedISBNs.contains(l.obtenirISBN())) result.add(l);
        return result;
    }

    public void selectAll() {
        if (currentLlibres == null) return;
        for (Llibre l : currentLlibres) selectedISBNs.add(l.obtenirISBN());
        wrap.repaint();
    }

    public void clearSelection() {
        List<Long> prev = new ArrayList<>(selectedISBNs);
        selectedISBNs.clear();
        repaintCards(prev);
    }

    public void adjustZoom(int delta) {
        int newZoom = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, zoomLevel + delta));
        if (newZoom == zoomLevel) return;
        zoomLevel = newZoom;
        ConfiguracioUi.posarGalleryZoom(zoomLevel);
        aplicarZoom(zoomLevel);
        imageService.clear();
        rebuildPreservingSelection();
    }

    private void rebuildPreservingSelection() {
        if (currentLlibres == null) return;
        Set<Long> savedSelection = new LinkedHashSet<>(selectedISBNs);
        int savedFocus = focusedRef[0];
        actualitzarLlibres(currentLlibres);
        selectedISBNs.addAll(savedSelection);
        if (savedFocus >= 0 && savedFocus < currentLlibres.size()) {
            focusedRef[0] = savedFocus;
        }
        revalidate();
        repaint();
    }

    private void aplicarZoom(int level) {
        CARD_W  = ZOOM_LEVELS[level].cardW();
        COVER_H = ZOOM_LEVELS[level].coverH();
        CARD_H  = COVER_H + FOOT_H;
        cardFactory = new FabricantTargetesCoberta(imageService, CARD_W, CARD_H, COVER_H);
    }

    private int computeCols() {
        int vw = wrap.getWidth();
        int cellW = CARD_W + 3 + 3 + GAP; // cardW + shadow ~ 3 + 3 + gap
        return Math.max(1, vw / cellW);
    }

    private void moureKeyboard(int delta) {
        if (currentLlibres == null || currentLlibres.isEmpty()) return;
        int n = currentLlibres.size();
        if (focusedRef[0] < 0) focusedRef[0] = 0;
        else focusedRef[0] = Math.max(0, Math.min(n - 1, focusedRef[0] + delta));
        selectedISBNs.clear();
        selectedISBNs.add(currentLlibres.get(focusedRef[0]).obtenirISBN());
        wrap.repaint();
        JPanel card = cardMap.get(currentLlibres.get(focusedRef[0]).obtenirISBN());
        if (card != null) card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
    }

    /** Llibres que la galeria mostra ara mateix. */
    public List<Llibre> obtenirLlibresVisibles() {
        return currentLlibres;
    }

    public void actualitzarLlibres(List<Llibre> llibres) {        this.currentLlibres = llibres;
        selectedISBNs.clear();
        focusedRef[0] = -1;
        cardMap.clear();
        targetesPendents.clear();
        zoomPopup.hide();
        wrap.setBackground(UITheme.palette().bgMain());
        wrap.removeAll();
        if (llibres != null) {
            final String lang = Configuracio.obtenirLang();
            for (int i = 0; i < llibres.size(); i++) {
                Llibre l = llibres.get(i);
                JPanel card = cardFactory.build(l, i, cardHost, lang);
                cardMap.put(l.obtenirISBN(), card);
                wrap.add(card);
            }
        }
        wrap.revalidate();
        wrap.repaint();
    }

    /**
     * Aplica el tema nou a la galeria.
     *
     * <p>No es tornen a construir les targetes: el canvi de tema només
     * afecta al fons pre-renderitzat i al marcador de portada, i tots dos es
     * tornen a pintar. Reconstruir-les bloquejava el fil d'interfície amb
     * centenars de llibres i la finestra quedava a mitges pintar, amb zones
     * del tema antic i zones del tema nou alhora.
     *
     * <p>Tampoc es buida la memòria cau de cobertes: les cobertes són fotos
     * i no depenen del tema. Buidar-la tornaria a baixar totes les imatges.
     */
    public void aplicarTheme() {
        setBackground(UITheme.palette().bgMain());
        wrap.setBackground(UITheme.palette().bgMain());
        if (cardFactory != null) cardFactory.invalidarFons();
        wrap.revalidate();
        wrap.repaint();
    }

    // ── Targeted repaint helpers ──────────────────────────────────────────────

    private void repaintCards(Iterable<Long> isbns) {
        for (Long isbn : isbns) {
            JPanel c = cardMap.get(isbn);
            if (c != null) c.repaint();
        }
    }
}
