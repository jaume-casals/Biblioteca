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

    /** Retorna opcions de format amb una primera entrada en blanc (per a "cap selecció"). */
    public static String[] withBlank() {
        return new String[]{"", I18n.t("fmt_hardcover"), I18n.t("fmt_softcover"),
                I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")};
    }

    /** Retorna opcions de format amb una primera entrada "sense canvis" (per a l'edició per lots). */
    public static String[] withNoChange() {
        return new String[]{I18n.t("batch_no_change"), I18n.t("fmt_hardcover"),
                I18n.t("fmt_softcover"), I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")};
    }
}

