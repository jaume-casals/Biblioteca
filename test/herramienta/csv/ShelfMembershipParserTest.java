package herramienta.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ShelfMembershipParserTest {

    @Test
    @DisplayName("parse: null / blank → empty list")
    void parseEmpty() {
        assertThat(ShelfMembershipParser.parse(null)).isEmpty();
        assertThat(ShelfMembershipParser.parse("")).isEmpty();
        assertThat(ShelfMembershipParser.parse("   ")).isEmpty();
    }

    @Test
    @DisplayName("parse: single entry 'Name|val|llegit'")
    void parseSingleFull() {
        var entries = ShelfMembershipParser.parse("Favorits|9.5|true");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isEqualTo(9.5);
        assertThat(entries.get(0).llegit()).isTrue();
    }

    @Test
    @DisplayName("parse: name only (no valoracio / no llegit)")
    void parseNameOnly() {
        var entries = ShelfMembershipParser.parse("Favorits");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isZero();
        assertThat(entries.get(0).llegit()).isFalse();
    }

    @Test
    @DisplayName("parse: multiple entries separated by ';'")
    void parseMultiple() {
        var entries = ShelfMembershipParser.parse("A|7.0|true;B|0|false;C|5.5|1");
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).name()).isEqualTo("A");
        assertThat(entries.get(0).llegit()).isTrue();
        assertThat(entries.get(1).name()).isEqualTo("B");
        assertThat(entries.get(1).llegit()).isFalse();
        assertThat(entries.get(2).name()).isEqualTo("C");
        assertThat(entries.get(2).llegit()).isTrue(); // "1"
    }

    @Test
    @DisplayName("parse: case-insensitive 'true' / 'false'")
    void parseBoolCaseInsensitive() {
        var entries = ShelfMembershipParser.parse("A|0|TRUE;B|0|FALSE;C|0|Yes;D|0|Y");
        assertThat(entries.get(0).llegit()).isTrue();
        assertThat(entries.get(1).llegit()).isFalse();
        assertThat(entries.get(2).llegit()).isTrue();
        assertThat(entries.get(3).llegit()).isTrue();
    }

    @Test
    @DisplayName("parse: empty entry name is skipped")
    void parseBlankName() {
        var entries = ShelfMembershipParser.parse("|0|true;Good|1|true;|0|false");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("Good");
    }

    @Test
    @DisplayName("parse: invalid valoracio falls back to 0.0")
    void parseInvalidDouble() {
        var entries = ShelfMembershipParser.parse("A|not-a-number|true");
        assertThat(entries.get(0).valoracio()).isZero();
    }

    @Test
    @DisplayName("parse: trims whitespace around name and values")
    void parseTrims() {
        var entries = ShelfMembershipParser.parse("  Spaced  |  3.5  | true  ");
        assertThat(entries.get(0).name()).isEqualTo("Spaced");
        assertThat(entries.get(0).valoracio()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        assertThat(new ShelfMembershipParser.Entry("A", 1.0, true))
            .isEqualTo(new ShelfMembershipParser.Entry("A", 1.0, true));
        assertThat(new ShelfMembershipParser.Entry("A", 1.0, true))
            .isNotEqualTo(new ShelfMembershipParser.Entry("A", 1.0, false));
    }
}
