package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConnectionConfigTest {

    @Test
    @DisplayName("constructor: non-null dbType accepted")
    void ctorAccepts() {
        var c = new ConnectionConfig("h2", "localhost", "sa", "", "default", null);
        assertThat(c.dbType()).isEqualTo("h2");
        assertThat(c.host()).isEqualTo("localhost");
        assertThat(c.user()).isEqualTo("sa");
        assertThat(c.password()).isEqualTo("");
        assertThat(c.profile()).isEqualTo("default");
        assertThat(c.testUrl()).isNull();
    }

    @Test
    @DisplayName("constructor: null dbType throws NPE")
    void ctorNullDbType() {
        assertThatThrownBy(() -> new ConnectionConfig(null, "h", "u", "p", "pr", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("dbType");
    }

    @Test
    @DisplayName("toString masks the password")
    void toStringMasksPassword() {
        var c = new ConnectionConfig("mariadb", "localhost", "user", "topsecret", "default", null);
        String s = c.toString();
        assertThat(s).doesNotContain("topsecret");
        assertThat(s).contains("***");
        assertThat(s).contains("dbType=mariadb");
        assertThat(s).contains("user=user");
    }

    @Test
    @DisplayName("toString handles empty password")
    void toStringEmptyPassword() {
        var c = new ConnectionConfig("h2", "localhost", "sa", "", "default", null);
        String s = c.toString();
        assertThat(s).contains("password=***");
    }

    @Test
    @DisplayName("record equality is value-based")
    void recordEquals() {
        var a = new ConnectionConfig("h2", "h", "u", "p", "pr", null);
        var b = new ConnectionConfig("h2", "h", "u", "p", "pr", null);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
