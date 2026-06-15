package presentacio.galeria;

import domini.Llibre;
import interficie.BibliotecaWriter;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 */
public final class CoverImageService {

    private static final int CACHE_CAPACITY = 150;
    private static final int CACHE_LOAD_FACTOR = 200; // initialCapacity for LRU
    private static final float CACHE_LOAD_F = 0.75f;
    private static final int EXECUTOR_THREADS = 4;

    private final ExecutorService imageLoader = Executors.newFixedThreadPool(EXECUTOR_THREADS, r -> {
        Thread t = new Thread(r, "cover-image-loader");
        t.setDaemon(true);
        return t;
    });

    private final Map<Long, BufferedImage> imageCache = Collections.synchronizedMap(
        new LinkedHashMap<Long, BufferedImage>(CACHE_LOAD_FACTOR, CACHE_LOAD_F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> e) {
                return size() > CACHE_CAPACITY;
            }
        });

    private final Set<Long> loading = ConcurrentHashMap.newKeySet();

    private volatile BibliotecaWriter cd;

    public CoverImageService() {
        main.ShutdownHooks.register(this::shutdown);
    }

    public void setCd(BibliotecaWriter cd) {
        this.cd = cd;
    }

    /**
     * Return the cached image for {@code isbn} at the current zoom, or {@code null}
     * if not yet loaded. The caller (card paint) must tolerate a {@code null}
     * return — the loader's onLoaded callback will repaint.
     */
    public BufferedImage getCached(long isbn) {
        return imageCache.get(isbn);
    }

    /**
     * Invalidate all cached images. Called when the zoom level changes
     * (pre-scaled images are now the wrong size) or on theme switch
     * if the gradient palette needs to be re-applied.
     */
    public void clear() {
        imageCache.clear();
        loading.clear();
    }

    /**
     * Schedule background load + crop-scale for {@code l}. The {@code onLoaded}
     * callback runs on the EDT after the image lands in the cache (or the
     * load fails). The callback is also fired immediately if the image is
     * already cached, so callers can treat it as "image is ready, please repaint".
     */
    public void submit(Llibre l, int w, int h, Runnable onLoaded) {
        long isbn = l.getISBN();
        if (imageCache.containsKey(isbn)) {
            onLoaded.run();
            return;
        }
        if (!loading.add(isbn)) {
            return;
        }
        imageLoader.submit(() -> {
            BufferedImage img = loadAndScale(l, w, h);
            SwingUtilities.invokeLater(() -> {
                try {
                    if (img != null) {
                        imageCache.put(isbn, img);
                    }
                } finally {
                    loading.remove(isbn);
                    onLoaded.run();
                }
            });
        });
    }

    /** Visible for tests / future tooling: drain the executor on shutdown. */
    public void shutdown() {
        imageLoader.shutdownNow();
    }

    private BufferedImage loadAndScale(Llibre l, int w, int h) {
        BufferedImage raw = loadRaw(l);
        if (raw == null) return null;
        return cropScale(raw, w, h);
    }

    private BufferedImage loadRaw(Llibre l) {
        byte[] blob = l.getImatgeBlob();
        if (blob == null && l.hasBlob()) {
            BibliotecaWriter writer = this.cd;
            if (writer != null) blob = writer.getLlibreBlob(l.getISBN());
            else blob = domini.ControladorDomini.getInstance().getLlibreBlob(l.getISBN());
        }
        if (blob != null) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(blob));
                if (img != null) return img;
            } catch (Exception ignored) { }
        }
        String path = l.getImatge();
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

    /** Center-crop and scale src to exactly w*h. Result is TYPE_INT_RGB for fast blitting. */
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
