package presentacio.util;

import javax.swing.ImageIcon;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Icones de coberta LRU per a taula i galeria (compartides a la pantalla principal). */
public final class MemoriaImatgesCoberta {
    /** Marca en caché: no hi ha coberta disponible (evita tornar a obtenir a cada repaint). */
    public static final ImageIcon NO_COVER = new ImageIcon(
        new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB));

    private static final Map<Long, ImageIcon> CACHE =
        Collections.synchronizedMap(new LinkedHashMap<>(200, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Long, ImageIcon> e) {
                return size() > 150;
            }
        });
    /**
     * ISBN →EGIN de la càrrega en vol. El token permet que una càrrega
     * obsoleta no tregui el marcador d'una càrrega més nova del mateix ISBN:
     * la retirada és condicional al token, no al simple|ISBN.
     */
    private static final Map<Long, Long> LOADING = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong TOKEN_COUNTER =
        new java.util.concurrent.atomic.AtomicLong();

    private MemoriaImatgesCoberta() {}

    public static Map<Long, ImageIcon> cache() { return CACHE; }

    /** Càrregues de coberta en vol, indexades per ISBN amb el seu token. */
    public static Map<Long, Long> loading() { return LOADING; }

    /** Token nou per a una càrrega de coberta. */
    public static long nouTokenCarga() { return TOKEN_COUNTER.incrementAndGet(); }

    public static void clear() {
        CACHE.clear();
        LOADING.clear();
    }
}