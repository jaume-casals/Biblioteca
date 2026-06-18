package herramienta;

import interficie.EscritorLlibre;

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
 * Descàrrega unificada de cobertes: pool acotat, memòria cau L1, memòria cau L2
 * al disc sota ~/.biblioteca/covers/.
 */
public final class ServeiCoberta {
    private static final int MAX_PARALLEL = 6;
    private static final int L1_MAX = 200;
    /** Cercador d'OL d'un sol fil. El limitador de 300ms d'OpenLibraryClient
     *  limita naturalment el rendiment a ≤3,3 crides/s, de manera que un
     *  sol fil n'hi ha prou — fils addicionals només es bloquejarien al
     *  limitador. El multithreading de les escriptures JDBC és el guany;
     *  les cerques OL eren el coll d'ampolla només perquè compartien pool
     *  amb les escriptures. */
    public static final ExecutorService FETCHER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cover-fetch");
        t.setDaemon(true);
        return t;
    });
    /** Pool per a escriptures de blob de coberta JDBC — dimensionat pel
     *  nombre de cobertes, independent del límit de taxa d'OL, de manera
     *  que les escriptures no fan cua darrere les cerques. */
    public static final ExecutorService WRITE_POOL = Executors.newFixedThreadPool(MAX_PARALLEL, r -> {
        Thread t = new Thread(r, "cover-write");
        t.setDaemon(true);
        return t;
    });
    /** @deprecated Usa {@link #FETCHER} per a cerques d'OL i
     *  {@link #WRITE_POOL} per a escriptures JDBC. Es conserva com a àlies
     *  de WRITE_POOL per compatibilitat enrere amb consumidors que abans
     *  enviaven una sola tasca combinada de cerca+escriptura. */
    @Deprecated
    public static final ExecutorService POOL = WRITE_POOL;

    private static final Object L1_LOCK = new Object();
    private static final Map<String, byte[]> L1 = new java.util.LinkedHashMap<>(L1_MAX, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, byte[]> eldest) { return size() > L1_MAX; }
    };
    private static final Path DISK_DIR = Path.of(System.getProperty("user.home"), ".biblioteca", "covers");

    private ServeiCoberta() {}

    static { main.ShutdownHooks.register(ServeiCoberta::shutdown); }

    public static byte[] obtenirCachedBytes(String isbn) {
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

    public static BufferedImage obtenirCachedImage(String isbn) {
        byte[] b = obtenirCachedBytes(isbn);
        if (b == null) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(b));
        } catch (Exception e) {
            return null;
        }
    }

    /** Obté els bytes de la coberta per a {@code isbn}, desa el blob a la BBDD, invoca el callback en cas d'èxit. */
    public static void fetchAsync(EscritorLlibre cd, String isbn, Runnable onDone) {
        byte[] cached = obtenirCachedBytes(isbn);
        if (cached != null) {
            escriureCoverAsync(cd, isbn, cached, onDone);
            return;
        }
        FETCHER.submit(() -> {
            byte[] data = null;
            try { data = ClientOpenLibrary.fetchCoverByISBN(isbn); } catch (Exception ignored) {}
            if (data != null && data.length > 0) {
                cacheBytes(isbn, data);
                escriureCoverAsync(cd, isbn, data, onDone);
            } else if (onDone != null) {
                onDone.run();
            }
        });
    }

    /** Envia una cerca de coberta i una escriptura a la BBDD per a
     *  {@code isbn} a través dels dos pools: la crida HTTP a OL va a
     *  {@link #FETCHER} (un sol fil, amb limitador), l'escriptura JDBC
     *  va a {@link #WRITE_POOL} (multifil, dimensionat pel nombre de
     *  cobertes). {@code onComplete} s'invoca al fil WRITE_POOL amb
     *  {@code true} si s'ha emmagatzemat una coberta, {@code false} si
     *  OL no tenia coberta o l'escriptura ha fallat. */
    public static void submitCoverFetch(interficie.EscritorBiblioteca cd, String isbn, java.util.function.Consumer<Boolean> onComplete) {
        byte[] cached = obtenirCachedBytes(isbn);
        if (cached != null) {
            submitWrite(cd, isbn, cached, onComplete);
            return;
        }
        FETCHER.submit(() -> {
            byte[] data = null;
            try { data = ClientOpenLibrary.fetchCoverByISBN(isbn); } catch (Exception ignored) {}
            if (data != null && data.length > 0) {
                cacheBytes(isbn, data);
                submitWrite(cd, isbn, data, onComplete);
            } else {
                onComplete.accept(false);
            }
        });
    }

    /** Planifica una escriptura JDBC de {@code data} per a {@code isbn} a
     *  {@link #WRITE_POOL}, cridant {@code onDone} (al fil WRITE_POOL)
     *  quan acabi. */
    private static void escriureCoverAsync(EscritorLlibre cd, String isbn, byte[] data, Runnable onDone) {
        long lIsbn;
        try { lIsbn = Long.parseLong(isbn); }
        catch (Exception e) { if (onDone != null) onDone.run(); return; }
        WRITE_POOL.submit(() -> {
            try { cd.posarLlibreBlob(lIsbn, data); } catch (Exception ignored) {}
            if (onDone != null) onDone.run();
        });
    }

    private static void submitWrite(interficie.EscritorBiblioteca cd, String isbn, byte[] data, java.util.function.Consumer<Boolean> onComplete) {
        long lIsbn;
        try { lIsbn = Long.parseLong(isbn); }
        catch (Exception e) { onComplete.accept(false); return; }
        WRITE_POOL.submit(() -> {
            boolean ok = false;
            try { cd.posarLlibreBlob(lIsbn, data); ok = true; } catch (Exception ignored) {}
            onComplete.accept(ok);
        });
    }

    /** Tanca el pool de descàrrega de cobertes. Es crida des d'un ganxo de tancada. */
    public static void shutdown() {
        FETCHER.shutdownNow();
        WRITE_POOL.shutdownNow();
    }

    public static void cacheBytes(String isbn, byte[] data) {
        putL1(isbn, data);
        try {
            Path target = DISK_DIR.resolve(isbn + ".jpg");
            // Salta l'escriptura al disc si ja hi ha una coberta
            // anterior en caché al mateix camí. El contingut en bytes
            // el fixa la resposta d'OpenLibrary, de manera que una
            // nova cerca normalment dóna els mateixos bytes; reescriure
            // seria malbaratament d'I/O + activitat de metadades del
            // sistema de fitxers.
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
