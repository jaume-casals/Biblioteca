package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BibliotecaExceptionTest {

    @Test
    @DisplayName("single-arg ctor: message set, code=UNKNOWN, cause=null")
    void singleArg() {
        BibliotecaException e = new BibliotecaException("oops");
        assertThat(e.getMessage()).isEqualTo("oops");
        assertThat(e.code()).isEqualTo(BibliotecaException.Code.UNKNOWN);
        assertThat(e.getCause()).isNull();
    }

    @Test
    @DisplayName("two-arg ctor: message + cause preserved; code=UNKNOWN")
    void twoArg() {
        Throwable root = new RuntimeException("root");
        BibliotecaException e = new BibliotecaException("wrapped", root);
        assertThat(e.getMessage()).isEqualTo("wrapped");
        assertThat(e.getCause()).isSameAs(root);
        assertThat(e.code()).isEqualTo(BibliotecaException.Code.UNKNOWN);
    }

    @Test
    @DisplayName("NotFound uses NOT_FOUND code")
    void notFoundCode() {
        assertThat(new BibliotecaException.NotFound("missing").code())
            .isEqualTo(BibliotecaException.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("Duplicate uses DUPLICATE code")
    void duplicateCode() {
        assertThat(new BibliotecaException.Duplicate("dup").code())
            .isEqualTo(BibliotecaException.Code.DUPLICATE);
    }

    @Test
    @DisplayName("Validation uses VALIDATION code")
    void validationCode() {
        assertThat(new BibliotecaException.Validation("bad").code())
            .isEqualTo(BibliotecaException.Code.VALIDATION);
    }

    @Test
    @DisplayName("subclass exception is also a BibliotecaException (polymorphism)")
    void subclassPolymorphism() {
        BibliotecaException e = new BibliotecaException.NotFound("x");
        assertThat(e).isInstanceOf(BibliotecaException.class);
        assertThat(e).isInstanceOf(RuntimeException.class);
    }
}
