package herramienta.io.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestAnalitzadorPertinençaPrestatgeria {

    @Test
    @DisplayName("parse: null / blank → empty list")
    void analitzarEmpty() {
        assertThat(AnalitzadorPertinençaPrestatgeria.parse(null)).isEmpty();
        assertThat(AnalitzadorPertinençaPrestatgeria.parse("")).isEmpty();
        assertThat(AnalitzadorPertinençaPrestatgeria.parse("   ")).isEmpty();
    }

    @Test
    @DisplayName("parse: single entry 'Name|val|llegit'")
    void analitzarSingleFull() {
        var entries = AnalitzadorPertinençaPrestatgeria.parse("Favorits|9.5|true");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isEqualTo(9.5);
        assertThat(entries.get(0).llegit()).isTrue();
    }

    @Test
    @DisplayName("parse: name only (no valoracio / no llegit)")
    void analitzarNameOnly() {
        var entries = AnalitzadorPertinençaPrestatgeria.parse("Favorits");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("Favorits");
        assertThat(entries.get(0).valoracio()).isZero();
        assertThat(entries.get(0).llegit()).isFalse();
    }

    @Test
    @DisplayName("parse: multiple entries separated by ';'")
    void analitzarMultiple() {
        var entries = AnalitzadorPertinençaPrestatgeria.parse("A|7.0|true;B|0|false;C|5.5|1");
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
    void analitzarBoolCaseInsensitive() {
        var entries = AnalitzadorPertinençaPrestatgeria.parse("A|0|TRUE;B|0|FALSE;C|0|Yes;D|0|Y");
        assertThat(entries.get(0).llegit()).isTrue();
        assertThat(entries.get(1).llegit()).isFalse();
        assertThat(entries.get(2).llegit()).isTrue();
        assertThat(entries.get(3).llegit()).isTrue();
    }

    @Test
    @DisplayName("parse: empty entry name is skipped")
    void analitzarBlankName() {
        var entries = AnalitzadorPertinençaPrestatgeria.parse("|0|true;Good|1|true;|0|false");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("Good");
    }

    @Test
    @DisplayName("parse: invalid valoracio falls back to 0.0")
    void analitzarInvalidDouble() {
        var entries = AnalitzadorPertinençaPrestatgeria.parse("A|not-a-number|true");
        assertThat(entries.get(0).valoracio()).isZero();
    }

    @Test
    @DisplayName("parse: trims whitespace around name and values")
    void analitzarTrims() {
        var entries = AnalitzadorPertinençaPrestatgeria.parse("  Spaced  |  3.5  | true  ");
        assertThat(entries.get(0).name()).isEqualTo("Spaced");
        assertThat(entries.get(0).valoracio()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        assertThat(new AnalitzadorPertinençaPrestatgeria.Entrada("A", 1.0, true))
            .isEqualTo(new AnalitzadorPertinençaPrestatgeria.Entrada("A", 1.0, true));
        assertThat(new AnalitzadorPertinençaPrestatgeria.Entrada("A", 1.0, true))
            .isNotEqualTo(new AnalitzadorPertinençaPrestatgeria.Entrada("A", 1.0, false));
    }
}
