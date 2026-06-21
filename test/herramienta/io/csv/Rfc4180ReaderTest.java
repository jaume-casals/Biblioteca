package herramienta.io.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.*;

class Rfc4180ReaderTest {

    @Test
    @DisplayName("hasNext: cert inicialment; fals un cop consumit el flux")
    void teNextTransitions() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\nc,d\n"));
        assertThat(r.hasNext()).isTrue();
        r.next();
        r.next();
        // Després de 2 crides next(), eof encara és fals perquè la tercera és
        // la que porta readLine() a retornar null. Aquesta crida retorna
        // null (el sentinel EOF) i posa eof=true.
        assertThat(r.next()).isNull();
        assertThat(r.hasNext()).isFalse();
    }

    @Test
    @DisplayName("hasNext: cert inicialment fins i tot amb entrada buida (el flag eof només canvia després de next())")
    void teNextEmpty() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader(""));
        // hasNext reflexa el flag eof, que només es posa quan next() exhaureix el flux.
        // El flux buit s'informa com si tingués files fins que es crida next().
        assertThat(r.hasNext()).isTrue();
        assertThat(r.next()).isNull();
        assertThat(r.hasNext()).isFalse();
    }

    @Test
    @DisplayName("next: més enllà d'EOF retorna null (les crides següents continuen retornant null)")
    void seguentPastEof() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\n"));
        r.next();
        // Un cop readLine() retorna null, eof=true i next() retorna null.
        assertThat(r.next()).isNull();
        // Cridar next() UN ALTRE COP ara llença NoSuchElementException perquè eof està activat.
        assertThatThrownBy(r::next).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("next: CSV simple de dues files")
    void seguentTwoRows() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\nc,d\n"));
        assertThat(r.next()).contains("a", "b");
        assertThat(r.next()).contains("c", "d");
    }

    @Test
    @DisplayName("next: un camp entre cometes amb salt de línia incrustat ocupa diverses línies físiques")
    void seguentQuotedMultiline() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(
            new StringReader("a,b\n\"line1\nline2\",c\nd,e\n"));
        assertThat(r.next()).contains("a", "b");
        assertThat(r.next()).contains("line1\nline2", "c");
        assertThat(r.next()).contains("d", "e");
    }

    @Test
    @DisplayName("next: una cometa doble dins d'un camp entre cometes es desescapa")
    void seguentDoubledQuote() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(
            new StringReader("\"qu\"\"oted\",x\n"));
        String[] row = r.next();
        assertThat(row).contains("qu\"oted", "x");
    }

    @Test
    @DisplayName("close: idempotent (sense excepció a la segona crida)")
    void tancarIdempotent() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a\n"));
        r.close();
        r.close(); // no ha de llençar
    }

    @Test
    @DisplayName("envolta un no-BufferedReader en BufferedReader internament")
    void wrapsNonBuffered() throws Exception {
        // StringReader no és un BufferedReader, però el lector igual funciona
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\nc,d\n"));
        assertThat(r.next()).contains("a", "b");
    }
}
