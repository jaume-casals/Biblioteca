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
    /** Single-thread OL fetcher. OpenLibraryClient's 300 ms
     *  rate limiter naturally caps throughput at ≤3.3 calls/s, so
     *  a single thread is enough — additional threads would just
     *  block on the rate limiter. Multi-threading the JDBC writes
     *  is the win; the OL fetches were the bottleneck only because
     *  they were sharing a pool with the writes. */
    public static final ExecutorService FETCHER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cover-fetch");
        t.setDaemon(true);
        return t;
    });
    /** Pool for JDBC cover-blob writes — sized by cover count,
     *  independent of the OL rate limit, so writes don't queue
     *  behind the fetches. */
    public static final ExecutorService WRITE_POOL = Executors.newFixedThreadPool(MAX_PARALLEL, r -> {
        Thread t = new Thread(r, "cover-write");
        t.setDaemon(true);
        return t;
    });
    /** @deprecated Use {@link #FETCHER} for OL fetches and
     *  {@link #WRITE_POOL} for JDBC writes. Kept as an alias of
     *  WRITE_POOL for backward compatibility with callers that
     *  used to submit a single combined fetch+write task. */
    @Deprecated
    public static final ExecutorService POOL = WRITE_POOL;

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
            writeCoverAsync(cd, isbn, cached, onDone);
            return;
        }
        FETCHER.submit(() -> {
            byte[] data = null;
            try { data = OpenLibraryClient.fetchCoverByISBN(isbn); } catch (Exception ignored) {}
            if (data != null && data.length > 0) {
                cacheBytes(isbn, data);
                writeCoverAsync(cd, isbn, data, onDone);
            } else if (onDone != null) {
                onDone.run();
            }
        });
    }

    /** Submit a cover fetch and DB write for {@code isbn} across
     *  the two pools: the OL HTTP call goes to {@link #FETCHER}
     *  (single-thread, rate-limited), the JDBC write goes to
     *  {@link #WRITE_POOL} (multi-thread, sized by cover count).
     *  {@code onComplete} is invoked on the WRITE_POOL thread
     *  with {@code true} if a cover was stored, {@code false}
     *  if OL had no cover or the write failed. */
    public static void submitCoverFetch(interficie.BibliotecaWriter cd, String isbn, java.util.function.Consumer<Boolean> onComplete) {
        byte[] cached = getCachedBytes(isbn);
        if (cached != null) {
            submitWrite(cd, isbn, cached, onComplete);
            return;
        }
        FETCHER.submit(() -> {
            byte[] data = null;
            try { data = OpenLibraryClient.fetchCoverByISBN(isbn); } catch (Exception ignored) {}
            if (data != null && data.length > 0) {
                cacheBytes(isbn, data);
                submitWrite(cd, isbn, data, onComplete);
            } else {
                onComplete.accept(false);
            }
        });
    }

    /** Schedule a JDBC write of {@code data} for {@code isbn} on
     *  {@link #WRITE_POOL}, calling {@code onDone} (on the WRITE_POOL
     *  thread) when finished. */
    private static void writeCoverAsync(BookWriter cd, String isbn, byte[] data, Runnable onDone) {
        long lIsbn;
        try { lIsbn = Long.parseLong(isbn); }
        catch (Exception e) { if (onDone != null) onDone.run(); return; }
        WRITE_POOL.submit(() -> {
            try { cd.setLlibreBlob(lIsbn, data); } catch (Exception ignored) {}
            if (onDone != null) onDone.run();
        });
    }

    private static void submitWrite(interficie.BibliotecaWriter cd, String isbn, byte[] data, java.util.function.Consumer<Boolean> onComplete) {
        long lIsbn;
        try { lIsbn = Long.parseLong(isbn); }
        catch (Exception e) { onComplete.accept(false); return; }
        WRITE_POOL.submit(() -> {
            boolean ok = false;
            try { cd.setLlibreBlob(lIsbn, data); ok = true; } catch (Exception ignored) {}
            onComplete.accept(ok);
        });
    }

    /** Shutdown the cover-fetch pool. Called from a shutdown hook. */
    public static void shutdown() {
        FETCHER.shutdownNow();
        WRITE_POOL.shutdownNow();
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
