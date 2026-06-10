package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ShelfParserTest {

    @Test
    @DisplayName("parseShelfEntries: null → empty list")
    void parseNull() {
        assertThat(ShelfParser.parseShelfEntries(null)).isEmpty();
    }

    @Test
    @DisplayName("parseShelfEntries: blank → empty list")
    void parseBlank() {
        assertThat(ShelfParser.parseShelfEntries("")).isEmpty();
        assertThat(ShelfParser.parseShelfEntries("   ")).isEmpty();
    }

    @Test
    @DisplayName("parseShelfEntries: single entry, no valoracio/llegit")
    void parseSingleBare() {
        var entries = ShelfParser.parseShelfEntries("Favorits");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nom()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isZero();
        assertThat(entries.get(0).llegit()).isFalse();
    }

    @Test
    @DisplayName("parseShelfEntries: single entry with valoracio and llegit=true")
    void parseSingleFull() {
        var entries = ShelfParser.parseShelfEntries("Favorits|9.5|true");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nom()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isEqualTo(9.5);
        assertThat(entries.get(0).llegit()).isTrue();
    }

    @Test
    @DisplayName("parseShelfEntries: multiple entries separated by ';'")
    void parseMultiple() {
        var entries = ShelfParser.parseShelfEntries("A|7.0|true;B|0.0|false;C|5.5|1");
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
    void parseSkipsBlankNames() {
        var entries = ShelfParser.parseShelfEntries("|0|true;Good|1|true;|0|false");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nom()).isEqualTo("Good");
    }

    @Test
    @DisplayName("parseShelfEntries: case-insensitive 'true' / 'TRUE'")
    void parseBoolCaseInsensitive() {
        var entries = ShelfParser.parseShelfEntries("A|0|TRUE;B|0|True;C|0|FALSE;D|0|false");
        assertThat(entries.get(0).llegit()).isTrue();
        assertThat(entries.get(1).llegit()).isTrue();
        assertThat(entries.get(2).llegit()).isFalse();
        assertThat(entries.get(3).llegit()).isFalse();
    }

    @Test
    @DisplayName("parseShelfEntries: invalid valoracio falls back to 0.0")
    void parseInvalidValoracio() {
        var entries = ShelfParser.parseShelfEntries("A|not-a-number|true");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).valoracio()).isZero();
    }

    @Test
    @DisplayName("parseShelfEntries: trims whitespace around names")
    void parseTrimsNames() {
        var entries = ShelfParser.parseShelfEntries("  Spaced Out  |0|false");
        assertThat(entries.get(0).nom()).isEqualTo("Spaced Out");
    }

    @Test
    @DisplayName("joinShelfEntries: empty list → empty string")
    void joinEmpty() {
        assertThat(ShelfParser.joinShelfEntries(java.util.List.of())).isEmpty();
    }

    @Test
    @DisplayName("joinShelfEntries: single entry produces 'name|val|llegit'")
    void joinSingle() {
        var s = ShelfParser.joinShelfEntries(java.util.List.of(
            new ShelfParser.ShelfEntry("A", 7.5, true)));
        assertThat(s).isEqualTo("A|7.5|true");
    }

    @Test
    @DisplayName("joinShelfEntries: multiple entries separated by ';'")
    void joinMultiple() {
        var s = ShelfParser.joinShelfEntries(java.util.List.of(
            new ShelfParser.ShelfEntry("A", 7.0, true),
            new ShelfParser.ShelfEntry("B", 0.0, false)));
        assertThat(s).isEqualTo("A|7.0|true;B|0.0|false");
    }

    @Test
    @DisplayName("joinShelfEntries: round-trips through parseShelfEntries")
    void joinParseRoundtrip() {
        var entries = java.util.List.of(
            new ShelfParser.ShelfEntry("Sci-Fi", 8.5, true),
            new ShelfParser.ShelfEntry("Reference", 0.0, false));
        var joined = ShelfParser.joinShelfEntries(entries);
        var back = ShelfParser.parseShelfEntries(joined);
        assertThat(back).hasSize(2);
        assertThat(back.get(0).nom()).isEqualTo("Sci-Fi");
        assertThat(back.get(0).valoracio()).isEqualTo(8.5);
        assertThat(back.get(0).llegit()).isTrue();
        assertThat(back.get(1).nom()).isEqualTo("Reference");
        assertThat(back.get(1).llegit()).isFalse();
    }
}
