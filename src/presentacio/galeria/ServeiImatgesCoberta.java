package presentacio.galeria;

import domini.Llibre;
import persistencia.contract.LectorLlibre;

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
 * Carregador asíncron + memòria cau LRU d'imatges de coberta pre-escalades.
 *
 * <p>Indexada per {@link Llibre#getISBN()}. El valor de la memòria cau és
 * la imatge ja escalada a les dimensions de targeta sol·licitades, de
 * manera que el camí de pintat pot fer blit 1:1 sense treball
 * d'escalat per frame.
 *
 * <p>Posseeix el pool de loader d'un sol fil, el conjunt en vol i la
 * matemàtica de retall-escalat. Tot l'accés des de l'EDT és via
 * {@link #submit(Llibre, int, int, Runnable)}; la càrrega en segon
 * pla i la mutació de la memòria cau s'executen a l'executor, amb el
 * callback de repaint enviat de tornada a l'EDT.
 *
 * <p>La memòria cau està protegida per un {@link ReentrantReadWriteLock} de
 * manera que les lectures EDT (el camí de pintat, dominant) s'executen
 * concurrentment mentre que les escriptures (cache miss → load)
 * prenen un lock exclusiu. L'embolcall anterior
 * {@code Collections.synchronizedMap} serialitzava cada {@code get()}
 * contra cada altre {@code get()}, cosa que blocava l'EDT en un
 * repaint de 200 targetes (segons la troballa LOW de tot.txt).
 *
 * <p>El {@link LinkedHashMap} s'ordena per inserció
 * ({@code accessOrder=false}) perquè un {@code get()} amb
 * {@code accessOrder=true} mutaria estructuralment la llista
 * ({@code afterNodeAccess}), corrompent les lectures concurrents
 * protegides pel lock de lectura — el finding MEDIUM de tot.txt
 * sobre aquesta classe.
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
        new LinkedHashMap<Long, BufferedImage>(CACHE_LOAD_FACTOR, CACHE_LOAD_F, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> e) {
                return size() > CACHE_CAPACITY;
            }
        };

    private final Set<Long> loading = ConcurrentHashMap.newKeySet();

    private volatile LectorLlibre cd;

    public ServeiImatgesCoberta() {
        main.ShutdownHooks.register(this::shutdown);
    }

    public void posarCd(LectorLlibre cd) {
        this.cd = cd;
    }

    /**
     * Retorna la imatge en caché per a {@code isbn} al zoom actual, o
     * {@code null} si encara no s'ha carregat. El caller (pintat de
     * targeta) ha de tolerar un retorn {@code null} — el callback
     * onLoaded del carregador farà el repaint. Bloquejat en lectura
     * perquè els repaints EDT concurrents en una galeria de 200
     * targetes no es serialitzin.
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
     * Invalida totes les imatges en caché. Es crida quan canvia el
     * nivell de zoom (les imatges pre-escalades ara tenen la mida
     * incorrecta) o en canviar de tema si cal re-aplicar la paleta
     * de gradient.
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
     * Planifica la càrrega en segon pla + retall-escalat per a {@code l}.
     * El callback {@code onLoaded} s'executa a l'EDT després que la
     * imatge arribi a la memòria cau (o la càrrega falli). El callback
     * també es dispara immediatament si la imatge ja està en caché,
     * de manera que els callers el poden tractar com "la imatge està
     * a punt, si us plau, repaint".
     */
    public void submit(Llibre l, int w, int h, Runnable onLoaded) {
        long isbn = l.obtenirISBN();
        cacheLock.readLock().lock();
        boolean alreadyCached;
        try {
            // Un sol get() — la memòria cau mai emmagatzema valors null,
            // de manera que la prova de presència equival a !=null (finding
            // LOW de tot.txt: un containsKey+get són dues cerques).
            alreadyCached = imageCache.get(isbn) != null;
        } finally {
            cacheLock.readLock().unlock();
        }
        if (alreadyCached) {
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

    /** Visible per a tests / eines futures: buida l'executor en tancar. */
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
    public static byte[] carregarCoverBytes(Llibre l, LectorLlibre cd) {
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
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data)) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(bais);
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
            LectorLlibre writer = this.cd;
            if (writer != null) blob = writer.obtenirLlibreBlob(l.obtenirISBN());
            else blob = domini.ControladorDomini.getInstance().obtenirLlibreBlob(l.obtenirISBN());
        }
        if (blob != null) {
            // try-with-resources garanteix el tancament del flux encara
            // que ImageIO.read() llenci (no ho fa en el camí feliç).
            try (ByteArrayInputStream bais = new ByteArrayInputStream(blob)) {
                BufferedImage img = ImageIO.read(bais);
                if (img != null) return img;
            } catch (Exception ignored) { }
        }
        String path = l.obtenirImatge();
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) {
                try (javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(f)) {
                    BufferedImage img = ImageIO.read(iis);
                    if (img != null) return img;
                } catch (Exception ignored) { }
            }
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
