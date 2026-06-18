package herramienta;

/**
 * Vista tipada de lectura/escriptura del petit sub-emmagatzematge "filtre / valors
 * per defecte" de {@link Config} (actualment només {@code defaultImgDir}; les
 * API de desar/eliminar preset es queden a {@link Config} directament perquè
 * abasten 17 claus d'una sola vegada).
 */
public final class ConfiguracioFiltre {
    private ConfiguracioFiltre() {}

    public static String defaultImgDir()           { return Configuracio.obtenirDefaultImgDir(); }
    public static void posarDefaultImgDir(String dir) { Configuracio.posarDefaultImgDir(dir); }
}
