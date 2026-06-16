package fuzz.domini;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import domini.ShelfParser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage-guided fuzz harness for {@link ShelfParser#parseShelfEntries}.
 *
 * <p>Input format (documented): {@code name|val|llegit;name|val|llegit;...}
 * — semicolons separate entries, pipes separate fields within an entry.
 * The parser is defensive on null/blank input but the field-level
 * contracts (number parsing, boolean parsing, quoting) deserve real
 * coverage.
 *
 * <p>Invariants enforced:
 * <ol>
 *   <li>Never throws on arbitrary UTF-8 input (incl. empty, all-pipes,
 *       embedded NULs, unpaired quotes, NaN / Infinity doubles).</li>
 *   <li>Returned list is non-null.</li>
 *   <li>Round-trip {@code joinShelfEntries(parseShelfEntries(s))} parses
 *       back to a list of the same size — guards against join losses
 *       on quotes, pipes, semicolons in names.</li>
 *   <li>Every entry's {@code nom} is non-null and non-blank
 *       (the parser drops blank-named entries silently).</li>
 * </ol>
 */
class ShelfParserFuzzTest {

    @FuzzTest(maxDuration = "30s")
    void fuzz(FuzzedDataProvider data) {
        String raw = data.consumeRemainingAsString();

        List<ShelfParser.ShelfEntry> entries = ShelfParser.parseShelfEntries(raw);
        assertThat(entries)
            .as("parseShelfEntries must never return null for raw='%s'", raw)
            .isNotNull();

        // Every returned entry must have a non-blank name.
        for (int i = 0; i < entries.size(); i++) {
            ShelfParser.ShelfEntry e = entries.get(i);
            assertThat(e.nom())
                .as("entry #%d has null name (raw='%s')", i, raw)
                .isNotNull();
            assertThat(e.nom())
                .as("entry #%d has blank name (raw='%s')", i, raw)
                .isNotBlank();
        }

        // Round-trip: join → re-parse → same count.
        String rejoined = ShelfParser.joinShelfEntries(entries);
        List<ShelfParser.ShelfEntry> reparsed = ShelfParser.parseShelfEntries(rejoined);
        assertThat(reparsed.size())
            .as("round-trip size mismatch: %d vs %d (raw='%s', rejoined='%s')",
                entries.size(), reparsed.size(), raw, rejoined)
            .isEqualTo(entries.size());
    }
}
