package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link Llibre}.
 * Mirrors domini.Llibre — covers every public setter/getter, edge cases
 * (null/blank handling, clamping, copyOf deep clone, getDisplayNom fallback).
 */
class TestLlibre {

    private Llibre newBook() {
        return new Llibre(9780306406157L, "Dune", "Herbert", 1965, "desc", 9.0, 19.99, true, "/cover.jpg");
    }

    // ── Constructor / getters ─────────────────────────────────────────────

    @Test
    @DisplayName("constructor stores all nine params")
    void constructorStoresAllArgs() {
        Llibre l = newBook();
        assertThat(l.obtenirISBN()).isEqualTo(9780306406157L);
        assertThat(l.obtenirNom()).isEqualTo("Dune");
        assertThat(l.obtenirAutor()).isEqualTo("Herbert");
        assertThat(l.obtenirAny()).isEqualTo(1965);
        assertThat(l.obtenirDescripcio()).isEqualTo("desc");
        assertThat(l.obtenirValoracio()).isEqualTo(9.0);
        assertThat(l.obtenirPreu()).isEqualTo(19.99);
        assertThat(l.obtenirLlegit()).isTrue();
        assertThat(l.obtenirImatge()).isEqualTo("/cover.jpg");
    }

    // ── Autor / Autors ───────────────────────────────────────────────────

    @Test
    @DisplayName("setAutors(null) clears autors list")
    void posarAutorsNullClearsAutors() {
        Llibre l = newBook();
        l.posarAutors(java.util.List.of("A", "B"));
        l.posarAutors(null);
        assertThat(l.obtenirAutors()).isEmpty();
    }

    @Test
    @DisplayName("setAutors(emptyList()) clears autors list")
    void posarAutorsEmptyClearsAutors() {
        Llibre l = newBook();
        l.posarAutors(java.util.List.of("A", "B"));
        l.posarAutors(java.util.List.of());
        assertThat(l.obtenirAutors()).isEmpty();
    }

    @Test
    @DisplayName("getAutor joins autors list with ', '")
    void obtenirAutorJoinsAutors() {
        Llibre l = newBook();
        l.posarAutors(java.util.List.of("A", "B", "C"));
        assertThat(l.obtenirAutor()).isEqualTo("A, B, C");
    }

    @Test
    @DisplayName("getAutors returns a defensive copy (mutating it does not change source)")
    void obtenirAutorsIsDefensiveCopy() {
        Llibre l = newBook();
        l.posarAutors(java.util.List.of("A", "B"));
        java.util.List<String> copy = l.obtenirAutors();
        copy.add("C");
        assertThat(l.obtenirAutors()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("addAutorNom with blank is a no-op")
    void afegirAutorNomBlankNoop() {
        Llibre l = newBook();
        l.afegirAutorNom("");
        l.afegirAutorNom(null);
        l.afegirAutorNom("   ");
        assertThat(l.obtenirAutors()).isEmpty();
    }

    @Test
    @DisplayName("addAutorNom with non-blank appends and updates autor")
    void afegirAutorNomAppends() {
        Llibre l = newBook();
        l.afegirAutorNom("A");
        l.afegirAutorNom("B");
        assertThat(l.obtenirAutors()).containsExactly("A", "B");
        assertThat(l.obtenirAutor()).isEqualTo("A, B");
    }

    @Test
    @DisplayName("setAutors(null) yields empty autors list and clears autor")
    void posarAutorsNull() {
        Llibre l = newBook();
        l.posarAutors(null);
        assertThat(l.obtenirAutors()).isEmpty();
        assertThat(l.obtenirAutor()).isEmpty();
    }

    // ── Numeric clamping ─────────────────────────────────────────────────

    @Test
    @DisplayName("setAny rejects negative with BibliotecaException.Validation")
    void posarAnyNegativeThrows() {
        Llibre l = newBook();
        assertThatThrownBy(() -> l.posarAny(-1))
            .isInstanceOf(BibliotecaException.Validacio.class);
    }

    @Test
    @DisplayName("setAny accepts zero (validator default)")
    void posarAnyZeroAccepted() {
        Llibre l = newBook();
        l.posarAny(0);
        assertThat(l.obtenirAny()).isZero();
    }

    @Test
    @DisplayName("setPagines clamps to >= 0")
    void posarPaginesClamps() {
        Llibre l = newBook();
        l.posarPagines(-5);
        assertThat(l.obtenirPagines()).isZero();
        l.posarPagines(300);
        assertThat(l.obtenirPagines()).isEqualTo(300);
    }

    @Test
    @DisplayName("setPaginesLlegides clamps to >= 0")
    void posarPaginesLlegidesClamps() {
        Llibre l = newBook();
        l.posarPaginesLlegides(-1);
        assertThat(l.obtenirPaginesLlegides()).isZero();
    }

    @Test
    @DisplayName("setVolum clamps to >= 0")
    void posarVolumClamps() {
        Llibre l = newBook();
        l.posarVolum(-7);
        assertThat(l.obtenirVolum()).isZero();
    }

    @Test
    @DisplayName("setExemplars clamps to >= 1")
    void posarExemplarsClamps() {
        Llibre l = newBook();
        l.posarExemplars(0);
        assertThat(l.obtenirExemplars()).isEqualTo(1);
        l.posarExemplars(-3);
        assertThat(l.obtenirExemplars()).isEqualTo(1);
        l.posarExemplars(5);
        assertThat(l.obtenirExemplars()).isEqualTo(5);
    }

    // ── String setters (null → empty / null behaviour) ──────────────────

    @Test
    @DisplayName("setNotes(null) stores empty string")
    void posarNotesNullIsEmpty() {
        Llibre l = newBook();
        l.posarNotes(null);
        assertThat(l.obtenirNotes()).isEqualTo("");
    }

    @Test
    @DisplayName("setEditorial(null) stores empty string")
    void posarEditorialNullIsEmpty() {
        Llibre l = newBook();
        l.posarEditorial(null);
        assertThat(l.obtenirEditorial()).isEqualTo("");
    }

    @Test
    @DisplayName("setSerie(null) stores empty string")
    void posarSerieNullIsEmpty() {
        Llibre l = newBook();
        l.posarSerie(null);
        assertThat(l.obtenirSerie()).isEqualTo("");
    }

    @Test
    @DisplayName("setDataCompra trims; empty/blank → null")
    void posarDataCompraNormalises() {
        Llibre l = newBook();
        l.posarDataCompra("  2024-01-15  ");
        assertThat(l.obtenirDataCompra()).isEqualTo("2024-01-15");
        l.posarDataCompra(null);
        assertThat(l.obtenirDataCompra()).isNull();
        l.posarDataCompra("   ");
        assertThat(l.obtenirDataCompra()).isNull();
        l.posarDataCompra("");
        assertThat(l.obtenirDataCompra()).isNull();
    }

    @Test
    @DisplayName("setIdioma trims; empty/blank → null")
    void posarIdiomaNormalises() {
        Llibre l = newBook();
        l.posarIdioma("  Català  ");
        assertThat(l.obtenirIdioma()).isEqualTo("Català");
        l.posarIdioma("");
        assertThat(l.obtenirIdioma()).isNull();
    }

    @Test
    @DisplayName("setFormat trims; empty/blank → null")
    void posarFormatNormalises() {
        Llibre l = newBook();
        l.posarFormat("  eBook  ");
        assertThat(l.getFormat()).isEqualTo("eBook");
    }

    @Test
    @DisplayName("setPaisOrigen trims; empty/blank → null")
    void posarPaisOrigenNormalises() {
        Llibre l = newBook();
        l.posarPaisOrigen("  US  ");
        assertThat(l.obtenirPaisOrigen()).isEqualTo("US");
        l.posarPaisOrigen(null);
        assertThat(l.obtenirPaisOrigen()).isNull();
    }

    @Test
    @DisplayName("setEstat trims; empty/blank → null")
    void posarEstatNormalises() {
        Llibre l = newBook();
        l.posarEstat("");
        assertThat(l.obtenirEstat()).isNull();
    }

    @Test
    @DisplayName("setLlenguaOriginal trims; empty/blank → null")
    void posarLlenguaOriginalNormalises() {
        Llibre l = newBook();
        l.posarLlenguaOriginal("  en  ");
        assertThat(l.obtenirLlenguaOriginal()).isEqualTo("en");
    }

    // ── Translation fields ───────────────────────────────────────────────

    @Test
    @DisplayName("setNomCa trims; empty/blank → null")
    void posarNomCaNormalises() {
        Llibre l = newBook();
        l.posarNomCa("  Duna  ");
        assertThat(l.obtenirNomCa()).isEqualTo("Duna");
        l.posarNomCa("");
        assertThat(l.obtenirNomCa()).isNull();
    }

    @Test
    @DisplayName("setNomEs / setNomEn trim; empty/blank → null")
    void posarNomEsEnNormalises() {
        Llibre l = newBook();
        l.posarNomEs("Dune");
        l.posarNomEn("Dune");
        assertThat(l.obtenirNomEs()).isEqualTo("Dune");
        assertThat(l.obtenirNomEn()).isEqualTo("Dune");
        l.posarNomEs(" ");
        l.posarNomEn(" ");
        assertThat(l.obtenirNomEs()).isNull();
        assertThat(l.obtenirNomEn()).isNull();
    }

    // ── getDisplayNom ────────────────────────────────────────────────────

    @Nested
    class NomMostrat {

        @Test
        @DisplayName("returns ca nom when lang=ca and set")
        void obtenirDisplayNomCa() {
            Llibre l = newBook();
            l.posarNomCa("Duna");
            assertThat(l.obtenirDisplayNom("ca")).isEqualTo("Duna");
        }

        @Test
        @DisplayName("returns es nom when lang=es and set")
        void obtenirDisplayNomEs() {
            Llibre l = newBook();
            l.posarNomEs("Duna");
            assertThat(l.obtenirDisplayNom("es")).isEqualTo("Duna");
        }

        @Test
        @DisplayName("returns en nom when lang=en and set")
        void obtenirDisplayNomEn() {
            Llibre l = newBook();
            l.posarNomEn("Dune");
            assertThat(l.obtenirDisplayNom("en")).isEqualTo("Dune");
        }

        @Test
        @DisplayName("falls back to nom when translation is null")
        void obtenirDisplayNomFallbackNull() {
            Llibre l = newBook();
            assertThat(l.obtenirDisplayNom("ca")).isEqualTo("Dune");
        }

        @Test
        @DisplayName("falls back to nom when translation is blank")
        void obtenirDisplayNomFallbackBlank() {
            Llibre l = newBook();
            l.posarNomCa("   ");
            assertThat(l.obtenirDisplayNom("ca")).isEqualTo("Dune");
        }

        @Test
        @DisplayName("falls back to nom for unknown language")
        void obtenirDisplayNomUnknownLang() {
            Llibre l = newBook();
            assertThat(l.obtenirDisplayNom("de")).isEqualTo("Dune");
            assertThat(l.obtenirDisplayNom("")).isEqualTo("Dune");
        }
    }

    // ── Blob / heavy-fields flags ───────────────────────────────────────

    @Test
    @DisplayName("setImatgeBlob / hasBlob round-trip")
    void blobRoundTrip() {
        Llibre l = newBook();
        byte[] data = {1, 2, 3, 4};
        l.posarImatgeBlob(data);
        l.posarHasBlob(true);
        assertThat(l.obtenirImatgeBlob()).isSameAs(data);
        assertThat(l.teBlob()).isTrue();
    }

    @Test
    @DisplayName("heavyFieldsLoaded flag round-trips")
    void heavyFieldsLoadedFlag() {
        Llibre l = newBook();
        l.posarHeavyFieldsLoaded(false);
        assertThat(l.esHeavyFieldsLoaded()).isFalse();
        l.posarHeavyFieldsLoaded(true);
        assertThat(l.esHeavyFieldsLoaded()).isTrue();
    }

    // ── copyOf ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("copyOf returns equal but distinct object")
    void copiarOfReturnsDistinct() {
        Llibre src = newBook();
        Llibre c = Llibre.copyOf(src);
        assertThat(c).isNotSameAs(src);
        assertThat(c.obtenirISBN()).isEqualTo(src.obtenirISBN());
        assertThat(c.obtenirNom()).isEqualTo(src.obtenirNom());
    }

    @Test
    @DisplayName("copyOf deep-copies the autors list")
    void copiarOfDeepCopiesAutors() {
        Llibre src = newBook();
        src.posarAutors(java.util.List.of("A", "B"));
        Llibre c = Llibre.copyOf(src);
        assertThat(c.obtenirAutors()).isEqualTo(src.obtenirAutors());
        assertThat(c.obtenirAutors()).isNotSameAs(src.obtenirAutors());
        c.obtenirAutors().add("C");
        assertThat(src.obtenirAutors()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("copyOf deep-copies the imatgeBlob byte array")
    void copiarOfDeepCopiesBlob() {
        Llibre src = newBook();
        byte[] blob = {1, 2, 3};
        src.posarImatgeBlob(blob);
        Llibre c = Llibre.copyOf(src);
        assertThat(c.obtenirImatgeBlob()).isNotSameAs(blob);
        assertThat(c.obtenirImatgeBlob()).containsExactly(1, 2, 3);
        // Mutate the original — the copy must not change
        blob[0] = 99;
        assertThat(c.obtenirImatgeBlob()[0]).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("copyOf with null blob keeps null on copy")
    void copiarOfNullBlob() {
        Llibre src = newBook();
        src.posarImatgeBlob(null);
        Llibre c = Llibre.copyOf(src);
        assertThat(c.obtenirImatgeBlob()).isNull();
    }

    @Test
    @DisplayName("copyOf with null autors keeps empty list on copy")
    void copiarOfNullAutors() {
        Llibre src = newBook();
        src.posarAutors(null);
        Llibre c = Llibre.copyOf(src);
        assertThat(c.obtenirAutors()).isEmpty();
    }

    // ── bindUpdateableFields ────────────────────────────────────────────

    @Test
    @DisplayName("bindUpdateableFields overwrites the nine core fields")
    void vincularUpdateableFieldsOverwrites() {
        Llibre target = newBook();
        Llibre.vincularUpdateableFields(target,
            9780141439518L, "New", "NewAuthor", 2024, "new desc", 5.0, 9.99, false, "/new.jpg");
        assertThat(target.obtenirISBN()).isEqualTo(9780141439518L);
        assertThat(target.obtenirNom()).isEqualTo("New");
        assertThat(target.obtenirAutor()).isEqualTo("NewAuthor");
        assertThat(target.obtenirAny()).isEqualTo(2024);
        assertThat(target.obtenirDescripcio()).isEqualTo("new desc");
        assertThat(target.obtenirValoracio()).isEqualTo(5.0);
        assertThat(target.obtenirPreu()).isEqualTo(9.99);
        assertThat(target.obtenirLlegit()).isFalse();
        assertThat(target.obtenirImatge()).isEqualTo("/new.jpg");
    }

    // ── Builder ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Llibre.builder() produces a Llibre with given fields")
    void builderProducesLlibre() {
        Llibre l = Llibre.builder()
            .isbn(9780306406157L).nom("Dune").autor("Herbert").any(1965)
            .descripcio("desc").valoracio(9.0).preu(19.99).llegit(true).imatge("/x")
            .build();
        assertThat(l.obtenirISBN()).isEqualTo(9780306406157L);
        assertThat(l.obtenirNom()).isEqualTo("Dune");
    }

    @Test
    @DisplayName("Builder setters return this for chaining")
    void builderChaining() {
        Llibre.Constructor b = Llibre.builder();
        assertThat(b.isbn(1L)).isSameAs(b);
        assertThat(b.nom("n")).isSameAs(b);
        assertThat(b.autor("a")).isSameAs(b);
        assertThat(b.any(1)).isSameAs(b);
        assertThat(b.descripcio("d")).isSameAs(b);
        assertThat(b.valoracio(1.0)).isSameAs(b);
        assertThat(b.preu(1.0)).isSameAs(b);
        assertThat(b.llegit(true)).isSameAs(b);
        assertThat(b.imatge("i")).isSameAs(b);
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString contains key fields and is non-null")
    void toStringContainsFields() {
        Llibre l = newBook();
        String s = l.toString();
        assertThat(s).contains("Llibre");
        assertThat(s).contains("Dune");
        assertThat(s).contains("Herbert");
        assertThat(s).contains("1965");
    }
}
