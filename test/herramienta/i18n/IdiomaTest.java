package herramienta.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class IdiomaTest {

    @Test
    @DisplayName("code(): CA → ca, ES → es, EN → en")
    void codeLowercase() {
        assertThat(Idioma.CA.code()).isEqualTo("ca");
        assertThat(Idioma.ES.code()).isEqualTo("es");
        assertThat(Idioma.EN.code()).isEqualTo("en");
    }

    @Test
    @DisplayName("fromCode: 'es' → ES")
    void fromCodeEs() {
        assertThat(Idioma.fromCode("es")).isEqualTo(Idioma.ES);
    }

    @Test
    @DisplayName("fromCode: 'en' → EN")
    void fromCodeEn() {
        assertThat(Idioma.fromCode("en")).isEqualTo(Idioma.EN);
    }

    @Test
    @DisplayName("fromCode: 'ca' / unknown / null → CA")
    void fromCodeCaDefault() {
        assertThat(Idioma.fromCode("ca")).isEqualTo(Idioma.CA);
        assertThat(Idioma.fromCode("xx")).isEqualTo(Idioma.CA);
        assertThat(Idioma.fromCode(null)).isEqualTo(Idioma.CA);
    }

    @Test
    @DisplayName("fromCode: mixed case")
    void fromCodeCaseInsensitive() {
        assertThat(Idioma.fromCode("ES")).isEqualTo(Idioma.ES);
        assertThat(Idioma.fromCode("En")).isEqualTo(Idioma.EN);
    }
}
