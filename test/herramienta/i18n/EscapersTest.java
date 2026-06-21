package herramienta.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EscapersTest {

    @Nested
    class Html {

        @Test
        @DisplayName("html: escapes & < > \" '")
        void htmlEscapesAll() {
            assertThat(Escapers.html("<script>alert(\"xss\")</script>"))
                .isEqualTo("&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;");
        }

        @Test
        @DisplayName("html: & is escaped first")
        void htmlAmpersand() {
            assertThat(Escapers.html("Tom & Jerry")).isEqualTo("Tom &amp; Jerry");
            assertThat(Escapers.html("&amp;")).isEqualTo("&amp;amp;"); // double-encoding intentional
        }

        @Test
        @DisplayName("html: null → empty string")
        void htmlNull() {
            assertThat(Escapers.html(null)).isEmpty();
        }

        @Test
        @DisplayName("html: empty string → empty")
        void htmlEmpty() {
            assertThat(Escapers.html("")).isEmpty();
        }

        @Test
        @DisplayName("html: single-quote becomes &#39;")
        void htmlSingleQuote() {
            assertThat(Escapers.html("don't")).isEqualTo("don&#39;t");
        }
    }

    @Nested
    class Sql {

        @Test
        @DisplayName("sql: doubles single quotes (caller adds outer quotes)")
        void sqlDoublesQuotes() {
            assertThat(Escapers.sql("O'Brien")).isEqualTo("O''Brien");
        }

        @Test
        @DisplayName("sql: null → empty string")
        void sqlNull() {
            assertThat(Escapers.sql(null)).isEmpty();
        }

        @Test
        @DisplayName("sql: text with no quotes is unchanged")
        void sqlNoQuotes() {
            assertThat(Escapers.sql("plain text")).isEqualTo("plain text");
        }
    }

    @Nested
    class Json {

        @Test
        @DisplayName("json: escapes backslash and quote")
        void jsonEscapesQuoteAndBackslash() {
            assertThat(Escapers.json("a\"b")).isEqualTo("a\\\"b");
            assertThat(Escapers.json("a\\b")).isEqualTo("a\\\\b");
        }

        @Test
        @DisplayName("json: escapes newline, carriage-return, tab, backspace, form-feed")
        void jsonEscapesControls() {
            assertThat(Escapers.json("hello\nworld")).isEqualTo("hello\\nworld");
            assertThat(Escapers.json("a\rb")).isEqualTo("a\\rb");
            assertThat(Escapers.json("a\tb")).isEqualTo("a\\tb");
            assertThat(Escapers.json("a\bb")).isEqualTo("a\\bb");
            assertThat(Escapers.json("a\fb")).isEqualTo("a\\fb");
        }

        @Test
        @DisplayName("json: escapes non-printable < 0x20 as \\uXXXX")
        void jsonEscapesLowControls() {
            assertThat(Escapers.json("\u0001")).isEqualTo("\\u0001");
            assertThat(Escapers.json("\u001f")).isEqualTo("\\u001f");
        }

        @Test
        @DisplayName("json: null → empty string")
        void jsonNull() {
            assertThat(Escapers.json(null)).isEmpty();
        }

        @Test
        @DisplayName("json: 0x7f and above pass through unchanged (UTF-8 / extended)")
        void jsonPassesExtended() {
            assertThat(Escapers.json("€")).isEqualTo("€");
        }
    }

    @Nested
    class Csv {

        @Test
        @DisplayName("csv: wraps in quotes")
        void csvWraps() {
            assertThat(Escapers.csv("hello")).isEqualTo("\"hello\"");
        }

        @Test
        @DisplayName("csv: doubles internal quotes")
        void csvDoublesInternal() {
            assertThat(Escapers.csv("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        }

        @Test
        @DisplayName("csv: null → empty string")
        void csvNull() {
            assertThat(Escapers.csv(null)).isEmpty();
        }

        @Test
        @DisplayName("csv: empty string → empty quoted pair")
        void csvEmpty() {
            assertThat(Escapers.csv("")).isEqualTo("\"\"");
        }
    }
}
