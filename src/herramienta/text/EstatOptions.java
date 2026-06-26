package herramienta.text;

import herramienta.i18n.I18n;

/**
 * Llistes compartides d'opcions d'estat físic del llibre per als quadres
 * combinats. Font única de veritat per evitar la deriva de claus d'I18n.
 */
public final class EstatOptions {

    private static final String[] ESTAT_KEYS =
            {"estat_nou", "estat_bo", "estat_usat", "estat_deteriorat"};

    private EstatOptions() {}

    /** Retorna opcions d'estat amb una primera entrada en blanc (per a "cap selecció"). */
    public static String[] withBlank() {
        String[] out = new String[ESTAT_KEYS.length + 1];
        out[0] = "";
        for (int i = 0; i < ESTAT_KEYS.length; i++) out[i + 1] = I18n.t(ESTAT_KEYS[i]);
        return out;
    }
}
