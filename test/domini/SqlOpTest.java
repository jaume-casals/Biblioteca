package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

class SqlOpTest {

    @Test
    @DisplayName("domain(): successful op runs without throwing")
    void domainSuccess() {
        boolean[] ran = {false};
        SqlOp.domain(() -> ran[0] = true);
        assertThat(ran[0]).isTrue();
    }

    @Test
    @DisplayName("domain(): SQLException is wrapped in BibliotecaException, cause preserved")
    void domainWrapsSqlException() {
        SQLException root = new SQLException("boom");
        BibliotecaException ex = catchThrowableOfType(
            () -> SqlOp.domain(() -> { throw root; }),
            BibliotecaException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getCause()).isSameAs(root);
        assertThat(ex.getMessage()).contains("boom").contains("SQLState=").contains("code=");
    }

    @Test
    @DisplayName("domain(): non-SQLException is not caught (passes through)")
    void domainDoesNotWrapOtherExceptions() {
        RuntimeException ex = catchThrowableOfType(
            () -> SqlOp.domain(() -> { throw new IllegalStateException("nope"); }),
            RuntimeException.class);
        assertThat(ex).isInstanceOf(IllegalStateException.class);
    }
}
