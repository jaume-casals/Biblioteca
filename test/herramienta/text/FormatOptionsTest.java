package herramienta.text;

import herramienta.i18n.I18n;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FormatOptionsTest {

    @Test
    @DisplayName("withBlank: returns 5 options, first one is empty string")
    void withBlankFirstEmpty() {
        String[] opts = FormatOptions.withBlank();
        assertThat(opts).hasSize(5);
        assertThat(opts[0]).isEmpty();
        // the rest come from I18n keys
        assertThat(opts[1]).isEqualTo(I18n.t("fmt_hardcover"));
        assertThat(opts[2]).isEqualTo(I18n.t("fmt_softcover"));
        assertThat(opts[3]).isEqualTo(I18n.t("fmt_ebook"));
        assertThat(opts[4]).isEqualTo(I18n.t("fmt_audiobook"));
    }

    @Test
    @DisplayName("withNoChange: returns 5 options, first one is the 'no change' label")
    void withNoChangeFirstIsLabel() {
        String[] opts = FormatOptions.withNoChange();
        assertThat(opts).hasSize(5);
        assertThat(opts[0]).isEqualTo(I18n.t("batch_no_change"));
        assertThat(opts[1]).isEqualTo(I18n.t("fmt_hardcover"));
        assertThat(opts[2]).isEqualTo(I18n.t("fmt_softcover"));
        assertThat(opts[3]).isEqualTo(I18n.t("fmt_ebook"));
        assertThat(opts[4]).isEqualTo(I18n.t("fmt_audiobook"));
    }

    @Test
    @DisplayName("withBlank and withNoChange return distinct first entries")
    void distinctFirstEntries() {
        assertThat(FormatOptions.withBlank()[0]).isNotEqualTo(FormatOptions.withNoChange()[0]);
    }
}
