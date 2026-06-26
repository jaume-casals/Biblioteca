package herramienta.io;

import herramienta.api.ClientOpenLibrary;
import herramienta.config.Configuracio;
import persistencia.contract.EscritorLlibre;

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

    private static final java.util.logging.Logger LOG =
        java.util.logging.Logger.getLogger(ServeiCoberta.class.getName());

    private static final Object L1_LOCK = new Object();
    private static final Map<String, byte[]> L1 = new java.util.LinkedHashMap<>(L1_MAX, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, byte[]> eldest) { return size() > L1_MAX; }
    };
    private static Path diskDir() { return Configuracio.bibliotecaDir().resolve("covers"); }

    private ServeiCoberta() {}

    static { main.ShutdownHooks.register(ServeiCoberta::shutdown); }

    public static byte[] obtenirCachedBytes(String isbn) {
        byte[] mem;
        synchronized (L1_LOCK) {
            mem = L1.get(isbn);
        }
        if (mem != null) return mem;
        Path f = diskDir().resolve(isbn + ".jpg");
        if (!Files.isRegularFile(f)) return null;
        byte[] disk;
        try {
            disk = Files.readAllBytes(f);
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.FINE, "obtenirCachedBytes: error llegint la caché al disc per a " + isbn, e);
            return null;
        }
        putL1(isbn, disk);
        return disk;
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

    /** Envia una cerca de coberta i una escriptura a la BBDD per a
     *  {@code isbn} a través dels dos pools: la crida HTTP a OL va a
     *  {@link #FETCHER} (un sol fil, amb limitador), l'escriptura JDBC
     *  va a {@link #WRITE_POOL} (multifil, dimensionat pel nombre de
     *  cobertes). {@code onComplete} s'invoca al fil WRITE_POOL amb
     *  {@code true} si s'ha emmagatzemat una coberta, {@code false} si
     *  OL no tenia coberta o l'escriptura ha fallat. */
    public static void submitCoverFetch(persistencia.contract.EscritorBiblioteca cd, String isbn, java.util.function.Consumer<Boolean> onComplete) {
        enqueueFetch(cd, isbn, () -> onComplete.accept(false),
            cached -> submitWrite(cd, isbn, cached, onComplete));
    }

    /** Implementació comuna a {@link #submitCoverFetch}.
     *  Consulta la caché (disc/L1); si encert, delega a {@code onCacheHit} amb
     *  els bytes. Si falla, envia una cerca a OL a {@link #FETCHER}; quan torna,
     *  desa a la caché i delega a {@code onFetched}. Si OL no retorna res,
     *  delega a {@code onMiss}. */
    private static void enqueueFetch(EscritorLlibre cd, String isbn,
                                     Runnable onMiss,
                                     java.util.function.Consumer<byte[]> onCacheHit) {
        byte[] cached = obtenirCachedBytes(isbn);
        if (cached != null) {
            onCacheHit.accept(cached);
            return;
        }
        FETCHER.submit(() -> {
            byte[] data = null;
            try { data = ClientOpenLibrary.fetchCoverByISBN(isbn); } catch (Exception ignored) {}
            if (data != null && data.length > 0) {
                cacheBytes(isbn, data);
                onCacheHit.accept(data);
            } else {
                onMiss.run();
            }
        });
    }

    /** Planifica una escriptura JDBC de {@code data} per a {@code isbn} a
     *  {@link #WRITE_POOL}, cridant {@code onComplete} (al fil WRITE_POOL)
     *  quan acabi. */
    private static void submitWrite(persistencia.contract.EscritorBiblioteca cd, String isbn, byte[] data, java.util.function.Consumer<Boolean> onComplete) {
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
            Path target = diskDir().resolve(isbn + ".jpg");
            // Comprova el contingut per hash SHA-256 per evitar reescriure
            // quan OpenLibrary re-encoda el JPEG (mateixa longitud però
            // bytes diferents — la heurística anterior només comparava
            // longitud, la qual cosa tractava dues imatges diferents com
            // idèntiques).
            if (Files.exists(target) && hashesMatch(target, data)) return;
            Files.createDirectories(diskDir());
            Files.write(target, data);
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING,
                "cacheBytes: no s'ha pogut escriure la coberta a la caché per a " + isbn
                    + " (" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
                    + "); la propera lectura repetirà la descàrrega", e);
        }
    }

    /** Compara el contingut d'un fitxer amb un buffer per SHA-256.
     *  Missatges amb "isEqual" per evitar atacs de temporització
     *  teòrics si el camí de caché s'exposés mai remotament. */
    private static boolean hashesMatch(Path path, byte[] data) {
        try {
            byte[] disk = Files.readAllBytes(path);
            java.security.MessageDigest md1 = java.security.MessageDigest.getInstance("SHA-256");
            java.security.MessageDigest md2 = java.security.MessageDigest.getInstance("SHA-256");
            return java.security.MessageDigest.isEqual(md1.digest(disk), md2.digest(data));
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.FINE, "hashesMatch: no s'ha pogut comparar la caché de coberta", e);
            return false;
        }
    }

    private static void putL1(String isbn, byte[] data) {
        synchronized (L1_LOCK) { L1.put(isbn, data); }
    }

    public static int parallelism() { return MAX_PARALLEL; }
}
