package presentacio;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import domini.Llibre;
import herramienta.Config;
import herramienta.UITheme;
import herramienta.UiConfig;
import interficie.BibliotecaWriter;
import presentacio.galeria.CoverCardFactory;
import presentacio.galeria.CoverImageService;
import presentacio.galeria.CoverZoomPopup;
import presentacio.layouts.WrapLayout;

/**
 * Grid of book covers, the gallery view of the library.
 *
 * <p>Composition:
 * <ul>
 *   <li>{@link CoverImageService} — async LRU image cache + crop-scale.</li>
 *   <li>{@link CoverCardFactory} — paints each card, wires mouse + selection
 *       events back through a {@link CoverCardFactory.Listener}.</li>
 *   <li>{@link CoverZoomPopup} — hover-zoom overlay shown next to a card.</li>
 * </ul>
 *
 * <p>This class owns the scroll pane, the wrap layout, the selection /
 * focus state, the keyboard bindings, and the theme application. The
 * actual visual + image work lives in the three collaborators above.
 */
public class GaleriaCobertesPanel extends JPanel {

    private record ZoomLevel(int cardW, int coverH) {}
    private static final ZoomLevel[] ZOOM_LEVELS = {
        new ZoomLevel(100, 150),
        new ZoomLevel(120, 180),
        new ZoomLevel(140, 210),
        new ZoomLevel(170, 255),
        new ZoomLevel(210, 315),
    };
    private static final int   FOOT_H    = 84;
    private static final int   GAP       = 14;

    private final CoverImageService imageService = new CoverImageService();
    private final CoverZoomPopup    zoomPopup    = new CoverZoomPopup();

    private int zoomLevel = Config.getGalleryZoom();
    private int CARD_W;
    private int COVER_H;
    private int CARD_H;
    private CoverCardFactory cardFactory;

    private final JPanel wrap;

    private final Set<Long> selectedISBNs = new LinkedHashSet<>();
    private final java.util.HashMap<Long, JPanel> cardMap = new java.util.HashMap<>();
    private List<Llibre> currentLlibres;
    private int focusedIdx = -1;

    private Consumer<Llibre> onCardClick;
    private BiConsumer<MouseEvent, List<Llibre>> onRightClick;
    private Consumer<List<Llibre>> onDeleteSelected;
    private BibliotecaWriter cd;

    public GaleriaCobertesPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.palette().bgMain());

        applyZoom(zoomLevel);

        wrap = new JPanel(new WrapLayout(FlowLayout.LEADING, GAP, GAP));
        wrap.setBackground(UITheme.palette().bgMain());

        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(60);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UITheme.palette().bgMain());
        // SIMPLE_SCROLL_MODE forces full repaint on scroll — avoids blit artifacts with non-opaque cards
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        add(scroll, BorderLayout.CENTER);

        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                wrap.revalidate();
            }
        });

        scroll.addMouseWheelListener(e -> {
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                adjustZoom(e.getWheelRotation() < 0 ? 1 : -1);
            } else {
                JScrollBar vsb = scroll.getVerticalScrollBar();
                vsb.setValue(vsb.getValue() + e.getUnitsToScroll() * vsb.getUnitIncrement());
            }
        });

        setFocusable(true);
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "galDelete");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),  "galRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,  0),  "galLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,  0),  "galDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,    0),  "galUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),  "galEnter");
        getActionMap().put("galDelete", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                List<Llibre> sel = getSelectedLlibres();
                if (!sel.isEmpty() && onDeleteSelected != null) onDeleteSelected.accept(sel);
            }
        });
        getActionMap().put("galRight", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { moveKeyboard(1); }
        });
        getActionMap().put("galLeft", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { moveKeyboard(-1); }
        });
        getActionMap().put("galDown", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { moveKeyboard(computeCols()); }
        });
        getActionMap().put("galUp", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { moveKeyboard(-computeCols()); }
        });
        getActionMap().put("galEnter", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (focusedIdx >= 0 && currentLlibres != null
                        && focusedIdx < currentLlibres.size() && onCardClick != null)
                    onCardClick.accept(currentLlibres.get(focusedIdx));
            }
        });
    }

    public void setCd(BibliotecaWriter cd) {
        this.cd = cd;
        imageService.setCd(cd);
    }

    public void setOnCardClick(Consumer<Llibre> cb)  { this.onCardClick = cb; }
    public void setOnRightClick(BiConsumer<MouseEvent, List<Llibre>> cb) { this.onRightClick = cb; }
    public void setOnDeleteSelected(Consumer<List<Llibre>> cb) { this.onDeleteSelected = cb; }

    public List<Llibre> getSelectedLlibres() {
        if (currentLlibres == null || selectedISBNs.isEmpty()) return Collections.emptyList();
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
        int newZoom = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, zoomLevel + delta));
        if (newZoom == zoomLevel) return;
        zoomLevel = newZoom;
        UiConfig.setGalleryZoom(zoomLevel);
        applyZoom(zoomLevel);
        imageService.clear();
        if (currentLlibres != null) {
            Set<Long> savedSelection = new LinkedHashSet<>(selectedISBNs);
            int savedFocus = focusedIdx;
            updateLlibres(currentLlibres);
            selectedISBNs.addAll(savedSelection);
            if (savedFocus >= 0 && savedFocus < currentLlibres.size()) {
                focusedIdx = savedFocus;
            }
            revalidate();
            repaint();
        }
    }

    private void applyZoom(int level) {
        CARD_W  = ZOOM_LEVELS[level].cardW();
        COVER_H = ZOOM_LEVELS[level].coverH();
        CARD_H  = COVER_H + FOOT_H;
        cardFactory = new CoverCardFactory(imageService, CARD_W, CARD_H, COVER_H, FOOT_H);
    }

    private int computeCols() {
        int vw = wrap.getWidth();
        int cellW = CARD_W + 3 + 3 + GAP; // cardW + shadow ~ 3 + 3 + gap
        return Math.max(1, vw / cellW);
    }

    private void moveKeyboard(int delta) {
        if (currentLlibres == null || currentLlibres.isEmpty()) return;
        int n = currentLlibres.size();
        if (focusedIdx < 0) focusedIdx = 0;
        else focusedIdx = Math.max(0, Math.min(n - 1, focusedIdx + delta));
        selectedISBNs.clear();
        selectedISBNs.add(currentLlibres.get(focusedIdx).getISBN());
        wrap.repaint();
        JPanel card = cardMap.get(currentLlibres.get(focusedIdx).getISBN());
        if (card != null) card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
    }

    public void updateLlibres(List<Llibre> llibres) {
        this.currentLlibres = llibres;
        selectedISBNs.clear();
        focusedIdx = -1;
        cardMap.clear();
        zoomPopup.hide();
        wrap.setBackground(UITheme.palette().bgMain());
        wrap.removeAll();
        if (llibres != null) {
            for (int i = 0; i < llibres.size(); i++) {
                Llibre l = llibres.get(i);
                JPanel card = cardFactory.build(l, i, new CardListener(l, i));
                cardMap.put(l.getISBN(), card);
                wrap.add(card);
            }
        }
        wrap.revalidate();
        wrap.repaint();
    }

    public void applyTheme() {
        setBackground(UITheme.palette().bgMain());
        wrap.setBackground(UITheme.palette().bgMain());
        if (currentLlibres != null) updateLlibres(currentLlibres);
    }

    // ── Targeted repaint helpers ──────────────────────────────────────────────

    private void repaintCards(Iterable<Long> isbns) {
        for (Long isbn : isbns) {
            JPanel c = cardMap.get(isbn);
            if (c != null) c.repaint();
        }
    }

    // ── CardListener: bridge from CoverCardFactory to host state ──────────────

    private final class CardListener implements CoverCardFactory.Listener {
        private final Llibre l;
        private final int cardIdx;

        CardListener(Llibre l, int cardIdx) {
            this.l = l;
            this.cardIdx = cardIdx;
        }

        @Override public Set<Long> selectedISBNs() { return selectedISBNs; }
        @Override public int focusedIdx() { return focusedIdx; }
        @Override public void setFocusedIdx(int idx) { focusedIdx = idx; }
        @Override public List<Llibre> currentLlibres() { return currentLlibres; }

        @Override public void onCardClicked(Llibre l) {
            if (onCardClick != null) onCardClick.accept(l);
        }

        @Override public void onCardRightClicked(MouseEvent e, List<Llibre> selected) {
            if (onRightClick != null) onRightClick.accept(e, selected);
        }

        @Override public void onSelectionMutated(Iterable<Long> changedIsbns) {
            repaintCards(changedIsbns);
        }
    }
}
