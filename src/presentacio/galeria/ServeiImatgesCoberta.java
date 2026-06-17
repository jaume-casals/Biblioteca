package presentacio.galeria;

import domini.Llibre;
import interficie.BookReader;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Async loader + LRU cache of pre-scaled cover images.
 *
 * <p>Keyed by {@link Llibre#getISBN()}. The cache value is the image already
 * scaled to the requested card dimensions, so the paint path can blit 1:1
 * without per-frame scaling work.
 *
 * <p>Owns the single-threaded loader pool, the in-flight set, and the
 * crop-scale math. All access from the EDT is via {@link #submit(Llibre, int, int, Runnable)};
 * background loading and cache mutation run on the executor, with the
 * repaint callback marshalled back to the EDT.
 *
 * <p>The LRU is protected by a {@link ReentrantReadWriteLock} so EDT reads
 * (the paint path, dominant) run concurrently while writes (cache miss →
 * load) take an exclusive lock. The previous {@code Collections.synchronizedMap}
 * wrapper serialised every {@code get()} against every other {@code get()},
 * which stalled the EDT on a 200-card repaint (per the tot.txt LOW finding).
 */
public final class ServeiImatgesCoberta {

    private static final int CACHE_CAPACITY = 150;
    private static final int CACHE_LOAD_FACTOR = 200; // capacitat inicial per a la LRU
    private static final float CACHE_LOAD_F = 0.75f;
    private static final int EXECUTOR_THREADS = 4;

    private final ExecutorService imageLoader = Executors.newFixedThreadPool(EXECUTOR_THREADS, r -> {
        Thread t = new Thread(r, "cover-image-loader");
        t.setDaemon(true);
        return t;
    });

    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final LinkedHashMap<Long, BufferedImage> imageCache =
        new LinkedHashMap<Long, BufferedImage>(CACHE_LOAD_FACTOR, CACHE_LOAD_F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> e) {
                return size() > CACHE_CAPACITY;
            }
        };

    private final Set<Long> loading = ConcurrentHashMap.newKeySet();

    private volatile BookReader cd;

    public ServeiImatgesCoberta() {
        main.ShutdownHooks.register(this::shutdown);
    }

    public void posarCd(BookReader cd) {
        this.cd = cd;
    }

    /**
     * Return the cached image for {@code isbn} at the current zoom, or {@code null}
     * if not yet loaded. The caller (card paint) must tolerate a {@code null}
     * return — the loader's onLoaded callback will repaint. Read-locked so
     * concurrent EDT repaints on a 200-card gallery do not serialise.
     */
    public BufferedImage obtenirCached(long isbn) {
        cacheLock.readLock().lock();
        try {
            return imageCache.get(isbn);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Invalidate all cached images. Called when the zoom level changes
     * (pre-scaled images are now the wrong size) or on theme switch
     * if the gradient palette needs to be re-applied.
     */
    public void clear() {
        cacheLock.writeLock().lock();
        try {
            imageCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
        loading.clear();
    }

    /**
     * Schedule background load + crop-scale for {@code l}. The {@code onLoaded}
     * callback runs on the EDT after the image lands in the cache (or the
     * load fails). The callback is also fired immediately if the image is
     * already cached, so callers can treat it as "image is ready, please repaint".
     */
    public void submit(Llibre l, int w, int h, Runnable onLoaded) {
        long isbn = l.obtenirISBN();
        if (containsCached(isbn)) {
            onLoaded.run();
            return;
        }
        if (!loading.add(isbn)) {
            return;
        }
        imageLoader.submit(() -> {
            BufferedImage img = carregarAndScale(l, w, h);
            SwingUtilities.invokeLater(() -> {
                try {
                    if (img != null) {
                        cacheLock.writeLock().lock();
                        try {
                            imageCache.put(isbn, img);
                        } finally {
                            cacheLock.writeLock().unlock();
                        }
                    }
                } finally {
                    loading.remove(isbn);
                    onLoaded.run();
                }
            });
        });
    }

    /** Read-locked containment check (used by submit() before scheduling). */
    private boolean containsCached(long isbn) {
        cacheLock.readLock().lock();
        try {
            return imageCache.containsKey(isbn);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /** Visible for tests / future tooling: drain the executor on shutdown. */
    public void shutdown() {
        imageLoader.shutdownNow();
    }

    // ── Mètodes utilitaris estàtics (font única de veritat del pipeline de cobertes) ─
    //
    // Abans els reexportava {@code TableCellComponents}. S'han mogut aquí
    // perquè {@code CoverImageService} sigui propietària del contracte de
    // bytes de coberta + icona escalada de principi a fi (finding MEDIUM
    // de tot.txt: els mètodes utilitaris de la façana pertanyien al
    // pipeline modern de cobertes, no a la classe antiga de reexportació).

    /** Llegeix els bytes de la coberta d'un llibre. Prova primer amb el blob
     *  en memòria, després amb el camí, i finalment carrega des de la BBDD
     *  via {@code cd}. */
    public static byte[] carregarCoverBytes(Llibre l, BookReader cd) {
        byte[] blob = l.obtenirImatgeBlob();
        if (blob == null && l.teBlob() && cd != null)
            blob = cd.obtenirLlibreBlob(l.obtenirISBN());
        if (blob != null) return blob;
        String path = l.obtenirImatge();
        if (path != null && !path.isEmpty()) {
            try { return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)); } catch (Exception ignored) {}
        }
        return null;
    }

    /** Descodifica un array de bytes de coberta i l'escala a l'alçada
     *  estàndard de la icona de fila. Retorna null si les dades són
     *  nul·les o no es poden descodificar. */
    public static javax.swing.ImageIcon scaledCover(byte[] data) {
        if (data == null) return null;
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img == null) return null;
            int h = 46;
            int w = Math.max(1, (int)(img.getWidth() * (h / (double) img.getHeight())));
            return new javax.swing.ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
        } catch (Exception ignored) { return null; }
    }

    private BufferedImage carregarAndScale(Llibre l, int w, int h) {
        BufferedImage raw = carregarRaw(l);
        if (raw == null) return null;
        return cropScale(raw, w, h);
    }

    private BufferedImage carregarRaw(Llibre l) {
        byte[] blob = l.obtenirImatgeBlob();
        if (blob == null && l.teBlob()) {
            BookReader writer = this.cd;
            if (writer != null) blob = writer.obtenirLlibreBlob(l.obtenirISBN());
            else blob = domini.ControladorDomini.getInstance().obtenirLlibreBlob(l.obtenirISBN());
        }
        if (blob != null) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(blob));
                if (img != null) return img;
            } catch (Exception ignored) { }
        }
        String path = l.obtenirImatge();
        if (path != null && !path.isEmpty()) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) return img;
                }
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Retalla centrat i escala src a w*h exactes. El resultat és
     *  TYPE_INT_RGB per a blits ràpids. */
    private static BufferedImage cropScale(BufferedImage src, int w, int h) {
        double ia = (double) src.getWidth() / src.getHeight();
        double ba = (double) w / h;
        int dw, dh, dx, dy;
        if (ia > ba) { dh = h; dw = (int)(h * ia); dx = (w - dw) / 2; dy = 0; }
        else         { dw = w; dh = (int)(w / ia); dx = 0; dy = (h - dh) / 2; }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, dx, dy, dw, dh, null);
        g.dispose();
        return out;
    }
}
