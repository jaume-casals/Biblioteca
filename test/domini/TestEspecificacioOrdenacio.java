package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class TestEspecificacioOrdenacio {

    @Test
    @DisplayName("defaultAsc returns column=ISBN ascending")
    void defaultAsc() {
        EspecificacioOrdenacio s = EspecificacioOrdenacio.defaultAsc();
        assertThat(s.column()).isEqualTo(EspecificacioOrdenacio.COL_ISBN);
        assertThat(s.ascending()).isTrue();
        assertThat(s.toSql()).isEqualTo("l.`ISBN` ASC");
    }

    @Test
    @DisplayName("known columns map to qualified SQL identifiers")
    void knownColumns() {
        assertThat(new EspecificacioOrdenacio(EspecificacioOrdenacio.COL_ISBN, true).toSql()).isEqualTo("l.`ISBN` ASC");
        assertThat(new EspecificacioOrdenacio(EspecificacioOrdenacio.COL_NOM, true).toSql()).isEqualTo("l.`nom` ASC");
        assertThat(new EspecificacioOrdenacio(EspecificacioOrdenacio.COL_ANY, true).toSql()).isEqualTo("l.`any` ASC");
        assertThat(new EspecificacioOrdenacio(EspecificacioOrdenacio.COL_VALORACIO, true).toSql()).isEqualTo("l.`valoracio` ASC");
        assertThat(new EspecificacioOrdenacio(EspecificacioOrdenacio.COL_PREU, true).toSql()).isEqualTo("l.`preu` ASC");
    }

    @Test
    @DisplayName("ascending=false produces DESC")
    void descending() {
        assertThat(new EspecificacioOrdenacio("nom", false).toSql()).isEqualTo("l.`nom` DESC");
    }

    @Test
    @DisplayName("unknown column falls back to ISBN")
    void unknownColumnFallsBack() {
        assertThat(new EspecificacioOrdenacio("garbage", true).toSql()).isEqualTo("l.`ISBN` ASC");
    }

    @Test
    @DisplayName("null column falls back to ISBN")
    void nullColumnFallsBack() {
        assertThat(new EspecificacioOrdenacio(null, true).toSql()).isEqualTo("l.`ISBN` ASC");
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
        assertThat(new EspecificacioOrdenacio(col, true).toSql()).isEqualTo(expected);
    }
}
