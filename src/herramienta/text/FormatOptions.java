package herramienta.text;

import herramienta.i18n.I18n;

/**
 * Llistes compartides d'opcions de format per als quadres combinats de
 * format de llibre. Usades pel diàleg de Llibre Nou, el Panell de Detalls i
 * el diàleg d'Edició per Lots. L'única font de veritat evita la deriva de
 * claus d'I18n.
 */
public final class FormatOptions {
    private FormatOptions() {}

    private static final String[] FMT_KEYS = {"fmt_hardcover", "fmt_softcover", "fmt_ebook", "fmt_audiobook"};

    /** Construeix la llista d'opcions de format amb {@code head} com a primera entrada. */
    private static String[] withFirst(String head) {
        String[] out = new String[FMT_KEYS.length + 1];
        out[0] = head;
        for (int i = 0; i < FMT_KEYS.length; i++) out[i + 1] = I18n.t(FMT_KEYS[i]);
        return out;
    }

    /** Retorna opcions de format amb una primera entrada en blanc (per a "cap selecció"). */
    public static String[] withBlank() {
        return withFirst("");
    }

    /** Retorna opcions de format amb una primera entrada "sense canvis" (per a l'edició per lots). */
    public static String[] withNoChange() {
        return withFirst(I18n.t("batch_no_change"));
    }
}

