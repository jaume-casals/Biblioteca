package herramienta;

import interficie.BookWriter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

/**
 * Unified cover-fetching: bounded pool, L1 memory cache, L2 disk cache under ~/.biblioteca/covers/.
 */
public final class CoverService {
    private static final int MAX_PARALLEL = 6;
    private static final int L1_MAX = 200;
    /** Shared cover-fetch pool. Visible to other consumers (e.g.
     *  ExportController.fetchMissingCovers) so they don't spin
     *  up a second pool for the same OL endpoint. */
    public static final ExecutorService POOL = Executors.newFixedThreadPool(MAX_PARALLEL, r -> {
        Thread t = new Thread(r, "cover-fetch");
        t.setDaemon(true);
        return t;
    });

    private static final Object L1_LOCK = new Object();
    private static final Map<String, byte[]> L1 = new java.util.LinkedHashMap<>(L1_MAX, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, byte[]> eldest) { return size() > L1_MAX; }
    };
    private static final Path DISK_DIR = Path.of(System.getProperty("user.home"), ".biblioteca", "covers");

    private CoverService() {}

    static { main.ShutdownHooks.register(CoverService::shutdown); }

    public static byte[] getCachedBytes(String isbn) {
        synchronized (L1_LOCK) {
            byte[] mem = L1.get(isbn);
            if (mem != null) return mem;
        }
        try {
            Path f = DISK_DIR.resolve(isbn + ".jpg");
            if (Files.isRegularFile(f)) {
                byte[] disk = Files.readAllBytes(f);
                synchronized (L1_LOCK) { putL1(isbn, disk); }
                return disk;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static BufferedImage getCachedImage(String isbn) {
        byte[] b = getCachedBytes(isbn);
        if (b == null) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(b));
        } catch (Exception e) {
            return null;
        }
    }

    /** Fetch cover bytes for {@code isbn}, save to DB blob, invoke callback on success. */
    public static void fetchAsync(BookWriter cd, String isbn, Runnable onDone) {
        byte[] cached = getCachedBytes(isbn);
        if (cached != null) {
            try {
                long lIsbn = Long.parseLong(isbn);
                cd.setLlibreBlob(lIsbn, cached);
            } catch (Exception ignored) {}
            if (onDone != null) onDone.run();
            return;
        }
        POOL.submit(() -> {
            try {
                byte[] data = OpenLibraryClient.fetchCoverByISBN(isbn);
                if (data != null && data.length > 0) {
                    cacheBytes(isbn, data);
                    long lIsbn = Long.parseLong(isbn);
                    cd.setLlibreBlob(lIsbn, data);
                }
                if (onDone != null) onDone.run();
            } catch (Exception ignored) {}
        });
    }

    /** Shutdown the cover-fetch pool. Called from a shutdown hook. */
    public static void shutdown() {
        POOL.shutdownNow();
    }

    public static void cacheBytes(String isbn, byte[] data) {
        putL1(isbn, data);
        try {
            Path target = DISK_DIR.resolve(isbn + ".jpg");
            // Skip the disk write if a previous cover is already cached
            // at the same path. The byte-content is hashed by the
            // OpenLibrary response so a re-fetch typically yields the
            // same bytes; re-writing would be wasted I/O + filesystem
            // metadata churn.
            if (Files.exists(target) && Files.size(target) == data.length) return;
            Files.createDirectories(DISK_DIR);
            Files.write(target, data);
        } catch (Exception ignored) {}
    }

    private static void putL1(String isbn, byte[] data) {
        synchronized (L1_LOCK) { L1.put(isbn, data); }
    }

    public static int parallelism() { return MAX_PARALLEL; }
}
