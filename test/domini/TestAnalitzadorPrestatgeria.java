package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestAnalitzadorPrestatgeria {

    @Test
    @DisplayName("parseShelfEntries: null → empty list")
    void analitzarNull() {
        assertThat(AnalitzadorPrestatgeria.analitzarShelfEntries(null)).isEmpty();
    }

    @Test
    @DisplayName("parseShelfEntries: blank → empty list")
    void analitzarBlank() {
        assertThat(AnalitzadorPrestatgeria.analitzarShelfEntries("")).isEmpty();
        assertThat(AnalitzadorPrestatgeria.analitzarShelfEntries("   ")).isEmpty();
    }

    @Test
    @DisplayName("parseShelfEntries: single entry, no valoracio/llegit")
    void analitzarSingleBare() {
        var entries = AnalitzadorPrestatgeria.analitzarShelfEntries("Favorits");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nom()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isZero();
        assertThat(entries.get(0).llegit()).isFalse();
    }

    @Test
    @DisplayName("parseShelfEntries: single entry with valoracio and llegit=true")
    void analitzarSingleFull() {
        var entries = AnalitzadorPrestatgeria.analitzarShelfEntries("Favorits|9.5|true");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nom()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isEqualTo(9.5);
        assertThat(entries.get(0).llegit()).isTrue();
    }

    @Test
    @DisplayName("parseShelfEntries: multiple entries separated by ';'")
    void analitzarMultiple() {
        var entries = AnalitzadorPrestatgeria.analitzarShelfEntries("A|7.0|true;B|0.0|false;C|5.5|1");
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).nom()).isEqualTo("A");
        assertThat(entries.get(0).valoracio()).isEqualTo(7.0);
        assertThat(entries.get(0).llegit()).isTrue();
        assertThat(entries.get(1).nom()).isEqualTo("B");
        assertThat(entries.get(1).llegit()).isFalse();
        assertThat(entries.get(2).nom()).isEqualTo("C");
        assertThat(entries.get(2).valoracio()).isEqualTo(5.5);
        assertThat(entries.get(2).llegit()).isTrue(); // "1" → true
    }

    @Test
    @DisplayName("parseShelfEntries: empty entry name is skipped")
    void analitzarSkipsBlankNames() {
        var entries = AnalitzadorPrestatgeria.analitzarShelfEntries("|0|true;Good|1|true;|0|false");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nom()).isEqualTo("Good");
    }

    @Test
    @DisplayName("parseShelfEntries: case-insensitive 'true' / 'TRUE'")
    void analitzarBoolCaseInsensitive() {
        var entries = AnalitzadorPrestatgeria.analitzarShelfEntries("A|0|TRUE;B|0|True;C|0|FALSE;D|0|false");
        assertThat(entries.get(0).llegit()).isTrue();
        assertThat(entries.get(1).llegit()).isTrue();
        assertThat(entries.get(2).llegit()).isFalse();
        assertThat(entries.get(3).llegit()).isFalse();
    }

    @Test
    @DisplayName("parseShelfEntries: invalid valoracio falls back to 0.0")
    void analitzarInvalidValoracio() {
        var entries = AnalitzadorPrestatgeria.analitzarShelfEntries("A|not-a-number|true");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).valoracio()).isZero();
    }

    @Test
    @DisplayName("parseShelfEntries: trims whitespace around names")
    void analitzarTrimsNames() {
        var entries = AnalitzadorPrestatgeria.analitzarShelfEntries("  Spaced Out  |0|false");
        assertThat(entries.get(0).nom()).isEqualTo("Spaced Out");
    }

    @Test
    @DisplayName("joinShelfEntries: empty list → empty string")
    void joinEmpty() {
        assertThat(AnalitzadorPrestatgeria.joinShelfEntries(java.util.List.of())).isEmpty();
    }

    @Test
    @DisplayName("joinShelfEntries: single entry produces 'name|val|llegit'")
    void joinSingle() {
        var s = AnalitzadorPrestatgeria.joinShelfEntries(java.util.List.of(
            new AnalitzadorPrestatgeria.ShelfEntry("A", 7.5, true)));
        assertThat(s).isEqualTo("A|7.5|true");
    }

    @Test
    @DisplayName("joinShelfEntries: multiple entries separated by ';'")
    void joinMultiple() {
        var s = AnalitzadorPrestatgeria.joinShelfEntries(java.util.List.of(
            new AnalitzadorPrestatgeria.ShelfEntry("A", 7.0, true),
            new AnalitzadorPrestatgeria.ShelfEntry("B", 0.0, false)));
        assertThat(s).isEqualTo("A|7.0|true;B|0.0|false");
    }

    @Test
    @DisplayName("joinShelfEntries: round-trips through parseShelfEntries")
    void joinParseRoundtrip() {
        var entries = java.util.List.of(
            new AnalitzadorPrestatgeria.ShelfEntry("Sci-Fi", 8.5, true),
            new AnalitzadorPrestatgeria.ShelfEntry("Reference", 0.0, false));
        var joined = AnalitzadorPrestatgeria.joinShelfEntries(entries);
        var back = AnalitzadorPrestatgeria.analitzarShelfEntries(joined);
        assertThat(back).hasSize(2);
        assertThat(back.get(0).nom()).isEqualTo("Sci-Fi");
        assertThat(back.get(0).valoracio()).isEqualTo(8.5);
        assertThat(back.get(0).llegit()).isTrue();
        assertThat(back.get(1).nom()).isEqualTo("Reference");
        assertThat(back.get(1).llegit()).isFalse();
    }
}
