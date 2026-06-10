package herramienta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LangTest {

    @Test
    @DisplayName("code(): CA → ca, ES → es, EN → en")
    void codeLowercase() {
        assertThat(Lang.CA.code()).isEqualTo("ca");
        assertThat(Lang.ES.code()).isEqualTo("es");
        assertThat(Lang.EN.code()).isEqualTo("en");
    }

    @Test
    @DisplayName("fromCode: 'es' → ES")
    void fromCodeEs() {
        assertThat(Lang.fromCode("es")).isEqualTo(Lang.ES);
    }

    @Test
    @DisplayName("fromCode: 'en' → EN")
    void fromCodeEn() {
        assertThat(Lang.fromCode("en")).isEqualTo(Lang.EN);
    }

    @Test
    @DisplayName("fromCode: 'ca' / unknown / null → CA")
    void fromCodeCaDefault() {
        assertThat(Lang.fromCode("ca")).isEqualTo(Lang.CA);
        assertThat(Lang.fromCode("xx")).isEqualTo(Lang.CA);
        assertThat(Lang.fromCode(null)).isEqualTo(Lang.CA);
    }

    @Test
    @DisplayName("fromCode: mixed case")
    void fromCodeCaseInsensitive() {
        assertThat(Lang.fromCode("ES")).isEqualTo(Lang.ES);
        assertThat(Lang.fromCode("En")).isEqualTo(Lang.EN);
    }
}
