package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class SortSpecTest {

    @Test
    @DisplayName("defaultAsc returns column=ISBN ascending")
    void defaultAsc() {
        SortSpec s = SortSpec.defaultAsc();
        assertThat(s.column()).isEqualTo(SortSpec.COL_ISBN);
        assertThat(s.ascending()).isTrue();
        assertThat(s.toSql()).isEqualTo("l.`ISBN` ASC");
    }

    @Test
    @DisplayName("known columns map to qualified SQL identifiers")
    void knownColumns() {
        assertThat(new SortSpec(SortSpec.COL_ISBN, true).toSql()).isEqualTo("l.`ISBN` ASC");
        assertThat(new SortSpec(SortSpec.COL_NOM, true).toSql()).isEqualTo("l.`nom` ASC");
        assertThat(new SortSpec(SortSpec.COL_ANY, true).toSql()).isEqualTo("l.`any` ASC");
        assertThat(new SortSpec(SortSpec.COL_VALORACIO, true).toSql()).isEqualTo("l.`valoracio` ASC");
        assertThat(new SortSpec(SortSpec.COL_PREU, true).toSql()).isEqualTo("l.`preu` ASC");
    }

    @Test
    @DisplayName("ascending=false produces DESC")
    void descending() {
        assertThat(new SortSpec("nom", false).toSql()).isEqualTo("l.`nom` DESC");
    }

    @Test
    @DisplayName("unknown column falls back to ISBN")
    void unknownColumnFallsBack() {
        assertThat(new SortSpec("garbage", true).toSql()).isEqualTo("l.`ISBN` ASC");
    }

    @Test
    @DisplayName("null column falls back to ISBN")
    void nullColumnFallsBack() {
        assertThat(new SortSpec(null, true).toSql()).isEqualTo("l.`ISBN` ASC");
    }

    @ParameterizedTest
    @CsvSource({
        "ISBN,    l.`ISBN` ASC",
        "nom,     l.`nom` ASC",
        "any,     l.`any` ASC",
        "valoracio, l.`valoracio` ASC",
        "preu,    l.`preu` ASC"
    })
    @DisplayName("toSql returns the expected column+direction per known key")
    void toSqlParameterized(String col, String expected) {
        assertThat(new SortSpec(col, true).toSql()).isEqualTo(expected);
    }
}
