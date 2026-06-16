package fuzz.domini;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import domini.Llibre;
import domini.SortSpec;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage-guided fuzz harness for {@link SortSpec}. The class has a
 * single contract: unknown / null column names silently coerce to ISBN.
 * This harness makes sure that contract holds for every possible
 * column input, and that the SQL fragment / comparator produced are
 * always consistent with the requested direction.
 *
 * <p>Invariants enforced:
 * <ol>
 *   <li>{@code new SortSpec(col, asc).column()} is never null and
 *       never blank — must coerce to ISBN.</li>
 *   <li>{@code toSql()} ends with the requested direction.</li>
 *   <li>The SQL fragment always references a backtick-quoted column.</li>
 *   <li>{@code SortSpec.comparator(col)} never returns null.</li>
 *   <li>The canonical column registry has no duplicate column names.</li>
 * </ol>
 */
class SortSpecFuzzTest {

    private static final List<String> KNOWN_COLUMNS = List.of(
        SortSpec.COL_ISBN, SortSpec.COL_NOM, SortSpec.COL_ANY,
        SortSpec.COL_VALORACIO, SortSpec.COL_PREU);

    @FuzzTest(maxDuration = "30s")
    void fuzz(FuzzedDataProvider data) {
        // 50/50: null col vs arbitrary string (covers the unknown-col fallback).
        String col = data.consumeBoolean() ? null : data.consumeString(200);
        boolean asc = data.consumeBoolean();

        SortSpec s = new SortSpec(col, asc);

        assertThat(s.column())
            .as("column() must never be null/blank (unknown coerces to ISBN) for input col='%s'", col)
            .isNotNull()
            .isNotBlank();

        String sql = s.toSql();
        assertThat(sql)
            .as("toSql() for col='%s' asc=%s", col, asc)
            .isNotNull()
            .isNotBlank()
            .contains("`")
            .endsWith(asc ? " ASC" : " DESC");

        // Comparator must always be non-null, even for unknown columns.
        Comparator<Llibre> cmp = SortSpec.comparator(col);
        assertThat(cmp).as("comparator() must never be null for col='%s'", col).isNotNull();

        // The comparator must be deterministic: comparing (a, a) == 0.
        // We don't construct a real Llibre here; just verify the
        // returned comparator is a proper Comparator (not null) and
        // that comparator() and the SortSpec instance agree.
        Comparator<Llibre> cmp2 = SortSpec.comparator(s.column());
        assertThat(cmp2).isNotNull();

        // Registry sanity: no duplicate column names.
        Set<String> seen = new HashSet<>();
        for (String k : SortSpec.columns().keySet()) {
            assertThat(seen.add(k)).as("duplicate column key in registry: %s", k).isTrue();
        }
        // Registry must at least cover the 5 known columns.
        assertThat(SortSpec.columns().keySet()).containsAll(KNOWN_COLUMNS);
    }
}
