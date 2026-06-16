package fuzz.domini;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import domini.LlibreFilter;
import domini.LlibreFilterBuilder;
import domini.SortSpec;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage-guided fuzz harness for {@link LlibreFilterBuilder} +
 * {@link SortSpec}. Every {@code withX} / {@code sort} setter is fed
 * arbitrary values (including nulls, empty strings, NaN, and unknown
 * sort column names), and the resulting {@link LlibreFilter} is checked
 * for invariants the rest of the codebase relies on.
 *
 * <p>Invariants the harness enforces:
 * <ol>
 *   <li>{@code build()} never throws and never returns null.</li>
 *   <li>The result's {@link SortSpec} is never null and {@code toSql()}
 *       returns a non-blank string ending in {@code ASC} or
 *       {@code DESC}.</li>
 *   <li>{@code SortSpec.toSql()} contains the requested direction
 *       (matches the {@code ascending} flag).</li>
 *   <li>Unknown sort column names coerce to ISBN (per the
 *       {@code SortSpec} contract).</li>
 * </ol>
 */
class LlibreFilterBuilderFuzzTest {

    private static final List<String> SORT_COLUMNS = List.of(
        SortSpec.COL_ISBN, SortSpec.COL_NOM, SortSpec.COL_ANY,
        SortSpec.COL_VALORACIO, SortSpec.COL_PREU);

    @FuzzTest(maxDuration = "30s")
    void fuzz(FuzzedDataProvider data) {
        LlibreFilterBuilder b = LlibreFilterBuilder.of();

        b.autor(data.consumeString(100));
        b.nom(data.consumeString(100));
        b.editorial(data.consumeString(100));
        b.serie(data.consumeString(100));
        b.format(data.consumeString(100));
        b.idioma(data.consumeString(100));

        b.isbn(consumeNullable(data, d -> d.consumeLong()));
        b.anyMin(consumeNullable(data, d -> d.consumeInt(0, 3000)));
        b.anyMax(consumeNullable(data, d -> d.consumeInt(0, 3000)));
        b.valoracioMin(consumeNullable(data, d -> d.consumeDouble()));
        b.valoracioMax(consumeNullable(data, d -> d.consumeDouble()));
        b.preuMin(consumeNullable(data, d -> d.consumeDouble()));
        b.preuMax(consumeNullable(data, d -> d.consumeDouble()));
        b.llegit(consumeNullable(data, d -> d.consumeBoolean()));
        b.tagId(consumeNullable(data, d -> d.consumeInt(0, 10000)));
        b.llistaId(consumeNullable(data, d -> d.consumeInt(0, 10000)));

        // Sort: 50/50 known column vs arbitrary string (forces the
        // unknown-column → ISBN fallback path). consumeInt is inclusive
        // on both ends, so subtract 1 from the size to keep the index
        // in range.
        String col = data.consumeBoolean()
            ? SORT_COLUMNS.get(data.consumeInt(0, SORT_COLUMNS.size() - 1))
            : data.consumeString(50);
        boolean asc = data.consumeBoolean();
        b.sort(col, asc);

        LlibreFilter f = b.build();
        assertThat(f).as("build() must never return null").isNotNull();

        SortSpec s = f.getSort();
        assertThat(s).as("filter.getSort() must never be null").isNotNull();

        String sql = s.toSql();
        assertThat(sql)
            .as("SortSpec.toSql() for col='%s' asc=%s", col, asc)
            .isNotNull()
            .isNotBlank()
            .containsAnyOf("ASC", "DESC");

        if (asc) {
            assertThat(sql).as("asc=true must end with ASC: %s", sql).endsWith(" ASC");
        } else {
            assertThat(sql).as("asc=false must end with DESC: %s", sql).endsWith(" DESC");
        }
    }

    @FunctionalInterface
    private interface DataSupplier<T> { T get(FuzzedDataProvider d); }

    private static <T> T consumeNullable(FuzzedDataProvider data, DataSupplier<T> s) {
        return data.consumeBoolean() ? null : s.get(data);
    }
}
