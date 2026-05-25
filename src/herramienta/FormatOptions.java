package herramienta;

/**
 * Shared format option lists for book format combos.
 * Used by New-Book Dialog, Details Panel, and Batch-Edit Dialog.
 * Single source of truth avoids I18n key drift.
 */
public final class FormatOptions {
    private FormatOptions() {}

    /** Returns format options with a blank first entry (for "no selection"). */
    public static String[] withBlank() {
        return new String[]{"", I18n.t("fmt_hardcover"), I18n.t("fmt_softcover"),
                I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")};
    }

    /** Returns format options with a "no change" first entry (for batch edit). */
    public static String[] withNoChange() {
        return new String[]{I18n.t("batch_no_change"), I18n.t("fmt_hardcover"),
                I18n.t("fmt_softcover"), I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")};
    }
}