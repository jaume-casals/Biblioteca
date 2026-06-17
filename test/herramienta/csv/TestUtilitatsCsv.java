package herramienta.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TestUtilitatsCsv {

    // ── parseLine ────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseLine: simple comma-separated line")
    void analitzarSimple() {
        assertThat(UtilitatsCsv.analitzarLine("a,b,c")).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("parseLine: empty string → single empty field")
    void analitzarEmpty() {
        assertThat(UtilitatsCsv.analitzarLine("")).containsExactly("");
    }

    @Test
    @DisplayName("parseLine: null → empty array")
    void analitzarNull() {
        assertThat(UtilitatsCsv.analitzarLine(null)).isEmpty();
    }

    @Test
    @DisplayName("parseLine: trailing comma → empty trailing field")
    void analitzarTrailingComma() {
        assertThat(UtilitatsCsv.analitzarLine("a,b,")).containsExactly("a", "b", "");
    }

    @Test
    @DisplayName("parseLine: leading comma → empty leading field")
    void analitzarLeadingComma() {
        assertThat(UtilitatsCsv.analitzarLine(",a,b")).containsExactly("", "a", "b");
    }

    @Test
    @DisplayName("parseLine: double-quoted field with embedded comma")
    void analitzarQuotedComma() {
        assertThat(UtilitatsCsv.analitzarLine("a,\"b,c\",d")).containsExactly("a", "b,c", "d");
    }

    @Test
    @DisplayName("parseLine: doubled quote inside a quoted field")
    void analitzarDoubledQuote() {
        assertThat(UtilitatsCsv.analitzarLine("\"a\"\"b\",c")).containsExactly("a\"b", "c");
    }

    @Test
    @DisplayName("parseLine: whitespace around fields is trimmed")
    void analitzarTrims() {
        assertThat(UtilitatsCsv.analitzarLine("  a  ,\tb\t , c ")).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("parseLine: BOM is preserved (documented behavior)")
    void analitzarBom() {
        String withBom = "\uFEFF" + "isbn,nom";
        String[] r = UtilitatsCsv.analitzarLine(withBom);
        assertThat(r[0]).contains("isbn");
    }

    @Test
    @DisplayName("parseLine: CR removed")
    void analitzarRemovesCr() {
        assertThat(UtilitatsCsv.analitzarLine("a,b,c\r")).containsExactly("a", "b", "c");
    }

    // ── colVal / colValOpt ──────────────────────────────────────────────

    @Test
    @DisplayName("colVal: returns trimmed value for present column")
    void colValPresent() {
        Map<String, Integer> h = UtilitatsCsv.buildHeaderMap(new String[]{"ISBN", "Title"});
        String[] row = {"978", "Dune"};
        assertThat(UtilitatsCsv.colVal(h, row, "Title")).isEqualTo("Dune");
    }

    @Test
    @DisplayName("colVal: missing column returns empty string")
    void colValMissing() {
        Map<String, Integer> h = UtilitatsCsv.buildHeaderMap(new String[]{"ISBN"});
        String[] row = {"978"};
        assertThat(UtilitatsCsv.colVal(h, row, "Title")).isEmpty();
    }

    @Test
    @DisplayName("colVal: index past row length returns empty string")
    void colValOutOfRange() {
        Map<String, Integer> h = UtilitatsCsv.buildHeaderMap(new String[]{"A", "B", "C"});
        String[] row = {"x"};
        assertThat(UtilitatsCsv.colVal(h, row, "C")).isEmpty();
    }

    @Test
    @DisplayName("colValOpt: present column → Optional.of(value)")
    void colValOptPresent() {
        Map<String, Integer> h = UtilitatsCsv.buildHeaderMap(new String[]{"ISBN"});
        String[] row = {"978"};
        assertThat(UtilitatsCsv.colValOpt(h, row, "ISBN")).contains("978");
    }

    @Test
    @DisplayName("colValOpt: present column with empty value → Optional.of(\"\")")
    void colValOptEmpty() {
        Map<String, Integer> h = UtilitatsCsv.buildHeaderMap(new String[]{"X"});
        String[] row = {""};
        assertThat(UtilitatsCsv.colValOpt(h, row, "X")).contains("");
    }

    @Test
    @DisplayName("colValOpt: missing column → Optional.empty()")
    void colValOptMissing() {
        Map<String, Integer> h = UtilitatsCsv.buildHeaderMap(new String[]{"A"});
        String[] row = {"x"};
        assertThat(UtilitatsCsv.colValOpt(h, row, "B")).isEmpty();
    }

    // ── parseDoubleOrZero ──────────────────────────────────────────────

    @Test
    @DisplayName("parseDoubleOrZero: valid double")
    void analitzarDoubleValid() {
        assertThat(UtilitatsCsv.analitzarDoubleOrZero("3.14")).isEqualTo(3.14);
        assertThat(UtilitatsCsv.analitzarDoubleOrZero(" 5.5 ")).isEqualTo(5.5);
    }

    @Test
    @DisplayName("parseDoubleOrZero: null / blank / invalid → 0.0")
    void analitzarDoubleInvalid() {
        assertThat(UtilitatsCsv.analitzarDoubleOrZero(null)).isZero();
        assertThat(UtilitatsCsv.analitzarDoubleOrZero("")).isZero();
        assertThat(UtilitatsCsv.analitzarDoubleOrZero("   ")).isZero();
        assertThat(UtilitatsCsv.analitzarDoubleOrZero("not a number")).isZero();
    }

    // ── buildHeaderMap ─────────────────────────────────────────────────

    @Test
    @DisplayName("buildHeaderMap: maps column name to index, trimmed")
    void buildHeaderMap() {
        Map<String, Integer> m = UtilitatsCsv.buildHeaderMap(new String[]{"  A  ", "B", "C"});
        assertThat(m).containsEntry("A", 0);
        assertThat(m).containsEntry("B", 1);
        assertThat(m).containsEntry("C", 2);
    }

    // ── csvQ ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("csvQ: plain text wrapped in quotes")
    void csvQPlain() {
        assertThat(UtilitatsCsv.csvQ("hello")).isEqualTo("\"hello\"");
    }

    @Test
    @DisplayName("csvQ: embedded quotes are doubled")
    void csvQEscapes() {
        assertThat(UtilitatsCsv.csvQ("a\"b")).isEqualTo("\"a\"\"b\"");
    }

    @Test
    @DisplayName("csvQ: null → empty")
    void csvQNull() {
        assertThat(UtilitatsCsv.csvQ(null)).isEmpty();
    }

    // ── parseIsbn ───────────────────────────────────────────────────────

    @Test
    @DisplayName("parseIsbn: ISBN-10 with X → ISBN-13")
    void analitzarIsbn10X() {
        assertThat(UtilitatsCsv.analitzarIsbn("019853110X")).isEqualTo("9780198531104");
    }

    @Test
    @DisplayName("parseIsbn: ISBN-13 with dashes → 13 digits")
    void analitzarIsbn13() {
        assertThat(UtilitatsCsv.analitzarIsbn("978-0-306-40615-7")).isEqualTo("9780306406157");
    }

    @Test
    @DisplayName("parseIsbn: empty / blank → empty")
    void analitzarIsbnEmpty() {
        assertThat(UtilitatsCsv.analitzarIsbn("")).isEmpty();
        assertThat(UtilitatsCsv.analitzarIsbn("   ")).isEmpty();
    }
}
