package herramienta;

import domini.Llibre;
import domini.LlibreFilter;
import domini.LlibreFilterBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class FiltreUtilsTest {

    // ── matchISBN ────────────────────────────────────────────────────────

    @Test
    @DisplayName("matchISBN: prefix match is a string-prefix on decimal form")
    void matchIsbnPrefix() {
        assertThat(FiltreUtils.matchISBN(978L, 9780306406157L)).isTrue();
        assertThat(FiltreUtils.matchISBN(123L, 9780306406157L)).isFalse();
    }

    @Test
    @DisplayName("matchISBN: full 13-digit match")
    void matchIsbnFull() {
        assertThat(FiltreUtils.matchISBN(9780306406157L, 9780306406157L)).isTrue();
    }

    @Test
    @DisplayName("matchISBN: null inputs return false")
    void matchIsbnNullSafe() {
        assertThat(FiltreUtils.matchISBN(null, 9780306406157L)).isFalse();
        assertThat(FiltreUtils.matchISBN(978L, null)).isFalse();
        assertThat(FiltreUtils.matchISBN(null, null)).isFalse();
    }

    // ── matchString ─────────────────────────────────────────────────────

    @Test
    @DisplayName("matchString: case-insensitive contains")
    void matchStringCaseInsensitive() {
        assertThat(FiltreUtils.matchString("cervantes", "Cervantes de Saavedra")).isTrue();
        assertThat(FiltreUtils.matchString("TOLKIEN", "tolkien")).isTrue();
    }

    @Test
    @DisplayName("matchString: empty query matches anything")
    void matchStringEmpty() {
        assertThat(FiltreUtils.matchString("", "anything")).isTrue();
    }

    @Test
    @DisplayName("matchString: null haystack → false")
    void matchStringNullHaystack() {
        assertThat(FiltreUtils.matchString("x", null)).isFalse();
    }

    @Test
    @DisplayName("matchString: null needle → false")
    void matchStringNullNeedle() {
        assertThat(FiltreUtils.matchString(null, "x")).isFalse();
    }

    @Test
    @DisplayName("matchString: accent-insensitive (Garcia ↔ García)")
    void matchStringAccentInsensitive() {
        assertThat(FiltreUtils.matchString("Garcia", "Gabriel García Márquez")).isTrue();
        assertThat(FiltreUtils.matchString("garcia", "García")).isTrue();
    }

    // ── matchStringContainsAll ─────────────────────────────────────────

    @Test
    @DisplayName("matchStringContainsAll: all words must appear (any order)")
    void containsAllMultiWord() {
        // Both "ring" and "lord" appear in the haystack
        assertThat(FiltreUtils.matchStringContainsAll("lord ring", "The Lord of the Rings")).isTrue();
        assertThat(FiltreUtils.matchStringContainsAll("ring lord", "The Lord of the Rings")).isTrue();
        // All single word: any order
        assertThat(FiltreUtils.matchStringContainsAll("rings", "The Lord of the Rings")).isTrue();
    }

    @Test
    @DisplayName("matchStringContainsAll: missing word → false")
    void containsAllMissing() {
        assertThat(FiltreUtils.matchStringContainsAll("tolkien rowling", "The Lord of the Rings")).isFalse();
    }

    @Test
    @DisplayName("matchStringContainsAll: empty words are skipped")
    void containsAllEmpty() {
        assertThat(FiltreUtils.matchStringContainsAll("  tolkien  ", "The Lord of the Rings by Tolkien")).isTrue();
    }

    @Test
    @DisplayName("matchStringContainsAll: null inputs return false")
    void containsAllNull() {
        assertThat(FiltreUtils.matchStringContainsAll(null, "x")).isFalse();
        assertThat(FiltreUtils.matchStringContainsAll("x", null)).isFalse();
    }

    // ── normalize ───────────────────────────────────────────────────────

    @Test
    @DisplayName("normalize: strips Catalan/Spanish diacritics")
    void normalizeStripsDiacritics() {
        assertThat(FiltreUtils.normalize("Català")).isEqualTo("catala");
        assertThat(FiltreUtils.normalize("àéíòú")).isEqualTo("aeiou");
        assertThat(FiltreUtils.normalize("Müller")).isEqualTo("muller");
    }

    @Test
    @DisplayName("normalize: lowercase + NFD")
    void normalizeLowercase() {
        assertThat(FiltreUtils.normalize("ABC")).isEqualTo("abc");
    }

    @Test
    @DisplayName("normalize: null → empty")
    void normalizeNull() {
        assertThat(FiltreUtils.normalize(null)).isEmpty();
    }

    // ── matches (in-memory predicate) ───────────────────────────────────

    private Llibre makeBook() {
        Llibre l = new Llibre(9780306406157L, "Dune", "Frank Herbert", 1965,
            "desc", 9.0, 19.99, true, "/x");
        l.setEditorial("Chilton");
        l.setSerie("Dune");
        l.setFormat("Tapa dura");
        l.setIdioma("English");
        return l;
    }

    @Test
    @DisplayName("matches: empty filter matches everything")
    void matchesEmpty() {
        Llibre l = makeBook();
        assertThat(FiltreUtils.matches(l, LlibreFilter.empty(), null, null)).isTrue();
    }

    @Test
    @DisplayName("matches: nom mismatch → false")
    void matchesNomFails() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().nom("Nonexistent").build();
        assertThat(FiltreUtils.matches(l, f, null, null)).isFalse();
    }

    @Test
    @DisplayName("matches: anyMin > book.any → false")
    void matchesAnyMinFails() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().anyMin(2000).build();
        assertThat(FiltreUtils.matches(l, f, null, null)).isFalse();
    }

    @Test
    @DisplayName("matches: anyMax < book.any → false")
    void matchesAnyMaxFails() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().anyMax(1900).build();
        assertThat(FiltreUtils.matches(l, f, null, null)).isFalse();
    }

    @Test
    @DisplayName("matches: valoracioMin > book.valoracio → false")
    void matchesValoracioMinFails() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().valoracioMin(9.5).build();
        assertThat(FiltreUtils.matches(l, f, null, null)).isFalse();
    }

    @Test
    @DisplayName("matches: llegit mismatch → false")
    void matchesLlegitFails() {
        Llibre l = makeBook();
        l.setLlegit(true);
        LlibreFilter f = LlibreFilterBuilder.of().llegit(false).build();
        assertThat(FiltreUtils.matches(l, f, null, null)).isFalse();
    }

    @Test
    @DisplayName("matches: tagISBNs set + book isbn not in set → false")
    void matchesTagSetFails() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().tagId(1).build();
        assertThat(FiltreUtils.matches(l, f, Set.of(999L), null)).isFalse();
    }

    @Test
    @DisplayName("matches: tagISBNs set + book isbn in set → true")
    void matchesTagSetPasses() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().tagId(1).build();
        assertThat(FiltreUtils.matches(l, f, Set.of(9780306406157L), null)).isTrue();
    }

    @Test
    @DisplayName("matches: llistaISBNs set + book isbn in set → true")
    void matchesLlistaSetPasses() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().llistaId(1).build();
        assertThat(FiltreUtils.matches(l, f, null, Set.of(9780306406157L))).isTrue();
    }

    @Test
    @DisplayName("matches: format is exact (case-insensitive)")
    void matchesFormat() {
        Llibre l = makeBook();
        LlibreFilter f1 = LlibreFilterBuilder.of().format("TAPA DURA").build();
        assertThat(FiltreUtils.matches(l, f1, null, null)).isTrue();
        LlibreFilter f2 = LlibreFilterBuilder.of().format("ebook").build();
        assertThat(FiltreUtils.matches(l, f2, null, null)).isFalse();
    }

    @Test
    @DisplayName("matches: idioma is accent-insensitive contains")
    void matchesIdioma() {
        Llibre l = makeBook();
        LlibreFilter f = LlibreFilterBuilder.of().idioma("english").build();
        assertThat(FiltreUtils.matches(l, f, null, null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "Cervantes, Cervantes de Saavedra, true",
        "TOLKIEN, tolkien, true",
        "tolkien, Cervantes, false"
    })
    @DisplayName("matchString parameterized")
    void matchStringParameterized(String q, String h, boolean expected) {
        assertThat(FiltreUtils.matchString(q, h)).isEqualTo(expected);
    }
}
