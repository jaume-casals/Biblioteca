package fuzz.herramienta;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import herramienta.csv.CsvUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage-guided fuzz harness for {@link CsvUtils#parseLine(String)}.
 *
 * <p>The fuzz body consumes an attacker-controlled string of up to 64K
 * characters and asserts the parser's basic contract: returns a
 * non-null array and every element is non-null. A secondary property
 * (encoded as a small {@code @FuzzTest} sanity check) confirms the
 * "trailing newline" stability rule on a manually constructed input.
 */
class CsvUtilsFuzzTest {

    @FuzzTest(maxDuration = "30s")
    void fuzz(FuzzedDataProvider data) {
        String s = data.consumeString(65536);
        String[] row = CsvUtils.parseLine(s);
        assertThat(row).isNotNull();
        for (String cell : row) {
            assertThat(cell).isNotNull();
        }
    }

    /**
     * The simple non-quoted case: appending a trailing newline to a
     * plain comma-separated line must not change the field count.
     * This is a manually-built property, not driven by the fuzzer.
     */
    @FuzzTest(maxDuration = "5s")
    void trailingNewlineDoesNotChangeFieldCount() {
        String s = "a,b,c";
        assertThat(CsvUtils.parseLine(s)).hasSameSizeAs(CsvUtils.parseLine(s + "\n"));
    }
}
