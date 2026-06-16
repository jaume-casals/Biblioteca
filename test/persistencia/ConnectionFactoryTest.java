package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link ConnectionConfig}. The
 * {@code ConnectionFactory} wrapper was inlined into
 * {@code ServerConect.createDatabase} by the LOW-tot refactor; we
 * cover the surviving public surface (the record ctor + accessor
 * record components) here.
 */
class ConnectionFactoryTest {

    @Test
    @DisplayName("new ConnectionConfig: returns a record with the given fields")
    void withConfigReturnsConfig() {
        var c = new ConnectionConfig("mariadb", "h", "u", "p", "profile", null);
        assertThat(c.dbType()).isEqualTo("mariadb");
        assertThat(c.host()).isEqualTo("h");
        assertThat(c.user()).isEqualTo("u");
        assertThat(c.password()).isEqualTo("p");
        assertThat(c.profile()).isEqualTo("profile");
        assertThat(c.testUrl()).isNull();
    }

    @Test
    @DisplayName("new ConnectionConfig: null dbType throws NPE via the compact ctor")
    void withConfigNullDbType() {
        assertThatThrownBy(() -> new ConnectionConfig(null, "h", "u", "p", "pr", null))
            .isInstanceOf(NullPointerException.class);
    }
}
