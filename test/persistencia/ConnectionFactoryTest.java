package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link ConnectionFactory}.
 * ConnectionFactory.open() is hard to test in isolation because it reaches
 * into Config + system properties + ServerConect. We at least cover
 * the simple {@code withConfig} constructor delegate.
 */
class ConnectionFactoryTest {

    @Test
    @DisplayName("withConfig: returns a ConnectionConfig with the given fields")
    void withConfigReturnsConfig() {
        var c = ConnectionFactory.withConfig("mariadb", "h", "u", "p", "profile", null);
        assertThat(c.dbType()).isEqualTo("mariadb");
        assertThat(c.host()).isEqualTo("h");
        assertThat(c.user()).isEqualTo("u");
        assertThat(c.password()).isEqualTo("p");
        assertThat(c.profile()).isEqualTo("profile");
        assertThat(c.testUrl()).isNull();
    }

    @Test
    @DisplayName("withConfig: null dbType throws NPE via ConnectionConfig's compact ctor")
    void withConfigNullDbType() {
        assertThatThrownBy(() -> ConnectionFactory.withConfig(null, "h", "u", "p", "pr", null))
            .isInstanceOf(NullPointerException.class);
    }
}
