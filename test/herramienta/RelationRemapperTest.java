package herramienta;

import domini.ControladorDomini;
import domini.Llista;
import domini.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.ControladorPersistencia;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link RelationRemapper}.
 * Verifies that the remapper caches by name, creates new entries when no
 * match exists, and never re-resolves after first lookup.
 */
class RelationRemapperTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:relation_remapper;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    // ── ShelfIdRemapper ─────────────────────────────────────────────────

    @Test
    @DisplayName("ShelfIdRemapper: returns existing id when name matches")
    void shelfResolveExisting() {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista existing = cd.addLlista("Favorits");
        var rem = new RelationRemapper.ShelfIdRemapper(cd);
        assertThat(rem.resolve("Favorits")).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("ShelfIdRemapper: creates new shelf for unknown name")
    void shelfCreateNew() {
        ControladorDomini cd = ControladorDomini.getInstance();
        var rem = new RelationRemapper.ShelfIdRemapper(cd);
        int id1 = rem.resolve("Favorits");
        int id2 = rem.resolve("Wishlist");
        assertThat(id1).isNotEqualTo(id2);
        assertThat(cd.getAllLlistes()).extracting(Llista::getNom)
            .containsExactlyInAnyOrder("Favorits", "Wishlist");
    }

    @Test
    @DisplayName("ShelfIdRemapper: same name resolves to same id on repeated calls (cached)")
    void shelfCacheHit() {
        ControladorDomini cd = ControladorDomini.getInstance();
        var rem = new RelationRemapper.ShelfIdRemapper(cd);
        int id1 = rem.resolve("Favorits");
        int id2 = rem.resolve("Favorits");
        int id3 = rem.resolve("Favorits");
        assertThat(id1).isEqualTo(id2).isEqualTo(id3);
        // Only one shelf created
        assertThat(cd.getAllLlistes()).hasSize(1);
    }

    // ── TagIdRemapper ───────────────────────────────────────────────────

    @Test
    @DisplayName("TagIdRemapper: returns existing id when name matches")
    void tagResolveExisting() {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag existing = cd.addTag("Sci-Fi");
        var rem = new RelationRemapper.TagIdRemapper(cd);
        assertThat(rem.resolve("Sci-Fi")).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("TagIdRemapper: creates new tag for unknown name")
    void tagCreateNew() {
        ControladorDomini cd = ControladorDomini.getInstance();
        var rem = new RelationRemapper.TagIdRemapper(cd);
        int id1 = rem.resolve("Sci-Fi");
        int id2 = rem.resolve("Fantasy");
        assertThat(id1).isNotEqualTo(id2);
        assertThat(cd.getAllTags()).extracting(Tag::getNom)
            .containsExactlyInAnyOrder("Sci-Fi", "Fantasy");
    }

    @Test
    @DisplayName("TagIdRemapper: same name resolves to same id on repeated calls (cached)")
    void tagCacheHit() {
        ControladorDomini cd = ControladorDomini.getInstance();
        var rem = new RelationRemapper.TagIdRemapper(cd);
        int id1 = rem.resolve("Sci-Fi");
        int id2 = rem.resolve("Sci-Fi");
        assertThat(id1).isEqualTo(id2);
        assertThat(cd.getAllTags()).hasSize(1);
    }

    @Test
    @DisplayName("TagIdRemapper: constructor seeds cache from existing tags")
    void tagSeedsCache() {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.addTag("PreExisting");
        // No rem.resolve yet — but the cache should already have it
        var rem = new RelationRemapper.TagIdRemapper(cd);
        int id = rem.resolve("PreExisting");
        // No new tag created
        assertThat(cd.getAllTags()).hasSize(1);
        // id is positive
        assertThat(id).isPositive();
    }
}
