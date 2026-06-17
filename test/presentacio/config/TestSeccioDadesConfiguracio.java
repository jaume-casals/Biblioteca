package presentacio.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TestSeccioDadesConfiguracio {

    @Test
    @DisplayName("sanitizeProfileName: 'café' (non-ASCII) → 'caf_'")
    void sanitizeNonAscii() {
        assertThat(ConfiguracioDataSection.sanitizeProfileName("café")).isEqualTo("caf_");
    }

    @Test
    @DisplayName("sanitizeProfileName: 'hello world' → 'hello_world'")
    void sanitizeSpace() {
        assertThat(ConfiguracioDataSection.sanitizeProfileName("hello world")).isEqualTo("hello_world");
    }

    @Test
    @DisplayName("sanitizeProfileName: alphanumeric, dash and underscore pass through")
    void sanitizePassThrough() {
        assertThat(ConfiguracioDataSection.sanitizeProfileName("my-profile_1")).isEqualTo("my-profile_1");
    }

    @Test
    @DisplayName("sanitizeProfileName: empty string stays empty")
    void sanitizeEmpty() {
        assertThat(ConfiguracioDataSection.sanitizeProfileName("")).isEmpty();
    }

    @Test
    @DisplayName("sanitizeProfileName: null → empty string")
    void sanitizeNull() {
        assertThat(ConfiguracioDataSection.sanitizeProfileName(null)).isEmpty();
    }

    @Test
    @DisplayName("sanitizeProfileName: leading/trailing whitespace is trimmed first")
    void sanitizeTrims() {
        assertThat(ConfiguracioDataSection.sanitizeProfileName("  my_profile  ")).isEqualTo("my_profile");
    }

    @Test
    @DisplayName("sanitizeProfileName: punctuation is replaced with '_'")
    void sanitizePunctuation() {
        assertThat(ConfiguracioDataSection.sanitizeProfileName("a.b,c;d")).isEqualTo("a_b_c_d");
    }

    @ParameterizedTest
    @CsvSource({
        "'café', 'caf_'",
        "'hello world', 'hello_world'",
        "'my-profile_1', 'my-profile_1'",
        "'', ''",
        "'  spaced  ', 'spaced'",
        "'a.b/c', 'a_b_c'"
    })
    @DisplayName("sanitizeProfileName parameterized")
    void sanitizeParameterized(String input, String expected) {
        assertThat(ConfiguracioDataSection.sanitizeProfileName(input)).isEqualTo(expected);
    }
}
