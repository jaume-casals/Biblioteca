package presentacio;

import javax.swing.ImageIcon;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Set<Long> LOADING = ConcurrentHashMap.newKeySet();

    private MemoriaImatgesCoberta() {}

    static Map<Long, ImageIcon> cache() { return CACHE; }
    static Set<Long> loading() { return LOADING; }

    static void clear() {
        CACHE.clear();
        LOADING.clear();
    }
}
