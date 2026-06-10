package herramienta.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.*;

class Rfc4180ReaderTest {

    @Test
    @DisplayName("hasNext: true initially; false after the stream is consumed")
    void hasNextTransitions() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\nc,d\n"));
        assertThat(r.hasNext()).isTrue();
        r.next();
        r.next();
        // After 2 next() calls, eof is still false because the third call is
        // the one that drives readLine() to return null. That call returns
        // null (the EOF sentinel) and sets eof=true.
        assertThat(r.next()).isNull();
        assertThat(r.hasNext()).isFalse();
    }

    @Test
    @DisplayName("hasNext: true initially even on empty input (eof flag only flips after next())")
    void hasNextEmpty() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader(""));
        // hasNext reflects eof flag, which is set only when next() exhausts the stream.
        // The empty stream is reported as having rows until next() is called.
        assertThat(r.hasNext()).isTrue();
        assertThat(r.next()).isNull();
        assertThat(r.hasNext()).isFalse();
    }

    @Test
    @DisplayName("next: past EOF returns null (subsequent calls keep returning null)")
    void nextPastEof() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\n"));
        r.next();
        // Once readLine() returns null, eof=true and next() returns null.
        assertThat(r.next()).isNull();
        // Calling next() AGAIN now throws NoSuchElementException because eof is set.
        assertThatThrownBy(r::next).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("next: simple two-row CSV")
    void nextTwoRows() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\nc,d\n"));
        assertThat(r.next()).contains("a", "b");
        assertThat(r.next()).contains("c", "d");
    }

    @Test
    @DisplayName("next: quoted field with embedded newline spans multiple physical lines")
    void nextQuotedMultiline() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(
            new StringReader("a,b\n\"line1\nline2\",c\nd,e\n"));
        assertThat(r.next()).contains("a", "b");
        assertThat(r.next()).contains("line1\nline2", "c");
        assertThat(r.next()).contains("d", "e");
    }

    @Test
    @DisplayName("next: doubled quote inside a quoted field is un-escaped")
    void nextDoubledQuote() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(
            new StringReader("\"qu\"\"oted\",x\n"));
        String[] row = r.next();
        assertThat(row).contains("qu\"oted", "x");
    }

    @Test
    @DisplayName("close: idempotent (no exception on second call)")
    void closeIdempotent() throws Exception {
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a\n"));
        r.close();
        r.close(); // should not throw
    }

    @Test
    @DisplayName("wraps non-BufferedReader in BufferedReader internally")
    void wrapsNonBuffered() throws Exception {
        // StringReader isn't a BufferedReader, but the reader still works
        Rfc4180Reader r = new Rfc4180Reader(new StringReader("a,b\nc,d\n"));
        assertThat(r.next()).contains("a", "b");
    }
}
