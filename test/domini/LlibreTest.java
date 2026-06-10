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
class LlibreTest {

    private Llibre newBook() {
        return new Llibre(9780306406157L, "Dune", "Herbert", 1965, "desc", 9.0, 19.99, true, "/cover.jpg");
    }

    // ── Constructor / getters ─────────────────────────────────────────────

    @Test
    @DisplayName("constructor stores all nine params")
    void constructorStoresAllArgs() {
        Llibre l = newBook();
        assertThat(l.getISBN()).isEqualTo(9780306406157L);
        assertThat(l.getNom()).isEqualTo("Dune");
        assertThat(l.getAutor()).isEqualTo("Herbert");
        assertThat(l.getAny()).isEqualTo(1965);
        assertThat(l.getDescripcio()).isEqualTo("desc");
        assertThat(l.getValoracio()).isEqualTo(9.0);
        assertThat(l.getPreu()).isEqualTo(19.99);
        assertThat(l.getLlegit()).isTrue();
        assertThat(l.getImatge()).isEqualTo("/cover.jpg");
    }

    // ── Autor / Autors ───────────────────────────────────────────────────

    @Test
    @DisplayName("setAutor(null) yields empty autors list and getAutor returns ''")
    void setAutorNullClearsAutors() {
        Llibre l = newBook();
        l.setAutors(java.util.List.of("A", "B"));
        l.setAutor(null);
        assertThat(l.getAutors()).isEmpty();
        assertThat(l.getAutor()).isEqualTo("");
    }

    @Test
    @DisplayName("setAutor('') yields empty autors list")
    void setAutorBlankClearsAutors() {
        Llibre l = newBook();
        l.setAutors(java.util.List.of("A", "B"));
        l.setAutor("");
        assertThat(l.getAutors()).isEmpty();
    }

    @Test
    @DisplayName("getAutor joins autors list with ', '")
    void getAutorJoinsAutors() {
        Llibre l = newBook();
        l.setAutors(java.util.List.of("A", "B", "C"));
        assertThat(l.getAutor()).isEqualTo("A, B, C");
    }

    @Test
    @DisplayName("getAutors returns a defensive copy (mutating it does not change source)")
    void getAutorsIsDefensiveCopy() {
        Llibre l = newBook();
        l.setAutors(java.util.List.of("A", "B"));
        java.util.List<String> copy = l.getAutors();
        copy.add("C");
        assertThat(l.getAutors()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("addAutorNom with blank is a no-op")
    void addAutorNomBlankNoop() {
        Llibre l = newBook();
        l.addAutorNom("");
        l.addAutorNom(null);
        l.addAutorNom("   ");
        assertThat(l.getAutors()).isEmpty();
    }

    @Test
    @DisplayName("addAutorNom with non-blank appends and updates autor")
    void addAutorNomAppends() {
        Llibre l = newBook();
        l.addAutorNom("A");
        l.addAutorNom("B");
        assertThat(l.getAutors()).containsExactly("A", "B");
        assertThat(l.getAutor()).isEqualTo("A, B");
    }

    @Test
    @DisplayName("setAutors(null) yields empty autors list and preserves prior autor")
    void setAutorsNull() {
        Llibre l = newBook();
        l.setAutors(null);
        assertThat(l.getAutors()).isEmpty();
    }

    // ── Numeric clamping ─────────────────────────────────────────────────

    @Test
    @DisplayName("setAny rejects negative with BibliotecaException.Validation")
    void setAnyNegativeThrows() {
        Llibre l = newBook();
        assertThatThrownBy(() -> l.setAny(-1))
            .isInstanceOf(BibliotecaException.Validation.class);
    }

    @Test
    @DisplayName("setAny accepts zero (validator default)")
    void setAnyZeroAccepted() {
        Llibre l = newBook();
        l.setAny(0);
        assertThat(l.getAny()).isZero();
    }

    @Test
    @DisplayName("setPagines clamps to >= 0")
    void setPaginesClamps() {
        Llibre l = newBook();
        l.setPagines(-5);
        assertThat(l.getPagines()).isZero();
        l.setPagines(300);
        assertThat(l.getPagines()).isEqualTo(300);
    }

    @Test
    @DisplayName("setPaginesLlegides clamps to >= 0")
    void setPaginesLlegidesClamps() {
        Llibre l = newBook();
        l.setPaginesLlegides(-1);
        assertThat(l.getPaginesLlegides()).isZero();
    }

    @Test
    @DisplayName("setVolum clamps to >= 0")
    void setVolumClamps() {
        Llibre l = newBook();
        l.setVolum(-7);
        assertThat(l.getVolum()).isZero();
    }

    @Test
    @DisplayName("setExemplars clamps to >= 1")
    void setExemplarsClamps() {
        Llibre l = newBook();
        l.setExemplars(0);
        assertThat(l.getExemplars()).isEqualTo(1);
        l.setExemplars(-3);
        assertThat(l.getExemplars()).isEqualTo(1);
        l.setExemplars(5);
        assertThat(l.getExemplars()).isEqualTo(5);
    }

    // ── String setters (null → empty / null behaviour) ──────────────────

    @Test
    @DisplayName("setNotes(null) stores empty string")
    void setNotesNullIsEmpty() {
        Llibre l = newBook();
        l.setNotes(null);
        assertThat(l.getNotes()).isEqualTo("");
    }

    @Test
    @DisplayName("setEditorial(null) stores empty string")
    void setEditorialNullIsEmpty() {
        Llibre l = newBook();
        l.setEditorial(null);
        assertThat(l.getEditorial()).isEqualTo("");
    }

    @Test
    @DisplayName("setSerie(null) stores empty string")
    void setSerieNullIsEmpty() {
        Llibre l = newBook();
        l.setSerie(null);
        assertThat(l.getSerie()).isEqualTo("");
    }

    @Test
    @DisplayName("setDataCompra trims; empty/blank → null")
    void setDataCompraNormalises() {
        Llibre l = newBook();
        l.setDataCompra("  2024-01-15  ");
        assertThat(l.getDataCompra()).isEqualTo("2024-01-15");
        l.setDataCompra(null);
        assertThat(l.getDataCompra()).isNull();
        l.setDataCompra("   ");
        assertThat(l.getDataCompra()).isNull();
        l.setDataCompra("");
        assertThat(l.getDataCompra()).isNull();
    }

    @Test
    @DisplayName("setIdioma trims; empty/blank → null")
    void setIdiomaNormalises() {
        Llibre l = newBook();
        l.setIdioma("  Català  ");
        assertThat(l.getIdioma()).isEqualTo("Català");
        l.setIdioma("");
        assertThat(l.getIdioma()).isNull();
    }

    @Test
    @DisplayName("setFormat trims; empty/blank → null")
    void setFormatNormalises() {
        Llibre l = newBook();
        l.setFormat("  eBook  ");
        assertThat(l.getFormat()).isEqualTo("eBook");
    }

    @Test
    @DisplayName("setPaisOrigen trims; empty/blank → null")
    void setPaisOrigenNormalises() {
        Llibre l = newBook();
        l.setPaisOrigen("  US  ");
        assertThat(l.getPaisOrigen()).isEqualTo("US");
        l.setPaisOrigen(null);
        assertThat(l.getPaisOrigen()).isNull();
    }

    @Test
    @DisplayName("setEstat trims; empty/blank → null")
    void setEstatNormalises() {
        Llibre l = newBook();
        l.setEstat("");
        assertThat(l.getEstat()).isNull();
    }

    @Test
    @DisplayName("setLlenguaOriginal trims; empty/blank → null")
    void setLlenguaOriginalNormalises() {
        Llibre l = newBook();
        l.setLlenguaOriginal("  en  ");
        assertThat(l.getLlenguaOriginal()).isEqualTo("en");
    }

    // ── Translation fields ───────────────────────────────────────────────

    @Test
    @DisplayName("setNomCa trims; empty/blank → null")
    void setNomCaNormalises() {
        Llibre l = newBook();
        l.setNomCa("  Duna  ");
        assertThat(l.getNomCa()).isEqualTo("Duna");
        l.setNomCa("");
        assertThat(l.getNomCa()).isNull();
    }

    @Test
    @DisplayName("setNomEs / setNomEn trim; empty/blank → null")
    void setNomEsEnNormalises() {
        Llibre l = newBook();
        l.setNomEs("Dune");
        l.setNomEn("Dune");
        assertThat(l.getNomEs()).isEqualTo("Dune");
        assertThat(l.getNomEn()).isEqualTo("Dune");
        l.setNomEs(" ");
        l.setNomEn(" ");
        assertThat(l.getNomEs()).isNull();
        assertThat(l.getNomEn()).isNull();
    }

    // ── getDisplayNom ────────────────────────────────────────────────────

    @Nested
    class DisplayNom {

        @Test
        @DisplayName("returns ca nom when lang=ca and set")
        void getDisplayNomCa() {
            Llibre l = newBook();
            l.setNomCa("Duna");
            assertThat(l.getDisplayNom("ca")).isEqualTo("Duna");
        }

        @Test
        @DisplayName("returns es nom when lang=es and set")
        void getDisplayNomEs() {
            Llibre l = newBook();
            l.setNomEs("Duna");
            assertThat(l.getDisplayNom("es")).isEqualTo("Duna");
        }

        @Test
        @DisplayName("returns en nom when lang=en and set")
        void getDisplayNomEn() {
            Llibre l = newBook();
            l.setNomEn("Dune");
            assertThat(l.getDisplayNom("en")).isEqualTo("Dune");
        }

        @Test
        @DisplayName("falls back to nom when translation is null")
        void getDisplayNomFallbackNull() {
            Llibre l = newBook();
            assertThat(l.getDisplayNom("ca")).isEqualTo("Dune");
        }

        @Test
        @DisplayName("falls back to nom when translation is blank")
        void getDisplayNomFallbackBlank() {
            Llibre l = newBook();
            l.setNomCa("   ");
            assertThat(l.getDisplayNom("ca")).isEqualTo("Dune");
        }

        @Test
        @DisplayName("falls back to nom for unknown language")
        void getDisplayNomUnknownLang() {
            Llibre l = newBook();
            assertThat(l.getDisplayNom("de")).isEqualTo("Dune");
            assertThat(l.getDisplayNom("")).isEqualTo("Dune");
        }
    }

    // ── Blob / heavy-fields flags ───────────────────────────────────────

    @Test
    @DisplayName("setImatgeBlob / hasBlob round-trip")
    void blobRoundTrip() {
        Llibre l = newBook();
        byte[] data = {1, 2, 3, 4};
        l.setImatgeBlob(data);
        l.setHasBlob(true);
        assertThat(l.getImatgeBlob()).isSameAs(data);
        assertThat(l.hasBlob()).isTrue();
    }

    @Test
    @DisplayName("heavyFieldsLoaded flag round-trips")
    void heavyFieldsLoadedFlag() {
        Llibre l = newBook();
        l.setHeavyFieldsLoaded(false);
        assertThat(l.isHeavyFieldsLoaded()).isFalse();
        l.setHeavyFieldsLoaded(true);
        assertThat(l.isHeavyFieldsLoaded()).isTrue();
    }

    // ── copyOf ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("copyOf returns equal but distinct object")
    void copyOfReturnsDistinct() {
        Llibre src = newBook();
        Llibre c = Llibre.copyOf(src);
        assertThat(c).isNotSameAs(src);
        assertThat(c.getISBN()).isEqualTo(src.getISBN());
        assertThat(c.getNom()).isEqualTo(src.getNom());
    }

    @Test
    @DisplayName("copyOf deep-copies the autors list")
    void copyOfDeepCopiesAutors() {
        Llibre src = newBook();
        src.setAutors(java.util.List.of("A", "B"));
        Llibre c = Llibre.copyOf(src);
        assertThat(c.getAutors()).isEqualTo(src.getAutors());
        assertThat(c.getAutors()).isNotSameAs(src.getAutors());
        c.getAutors().add("C");
        assertThat(src.getAutors()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("copyOf deep-copies the imatgeBlob byte array")
    void copyOfDeepCopiesBlob() {
        Llibre src = newBook();
        byte[] blob = {1, 2, 3};
        src.setImatgeBlob(blob);
        Llibre c = Llibre.copyOf(src);
        assertThat(c.getImatgeBlob()).isNotSameAs(blob);
        assertThat(c.getImatgeBlob()).containsExactly(1, 2, 3);
        // Mutate the original — the copy must not change
        blob[0] = 99;
        assertThat(c.getImatgeBlob()[0]).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("copyOf with null blob keeps null on copy")
    void copyOfNullBlob() {
        Llibre src = newBook();
        src.setImatgeBlob(null);
        Llibre c = Llibre.copyOf(src);
        assertThat(c.getImatgeBlob()).isNull();
    }

    @Test
    @DisplayName("copyOf with null autors keeps empty list on copy")
    void copyOfNullAutors() {
        Llibre src = newBook();
        src.setAutors(null);
        Llibre c = Llibre.copyOf(src);
        assertThat(c.getAutors()).isEmpty();
    }

    // ── bindUpdateableFields ────────────────────────────────────────────

    @Test
    @DisplayName("bindUpdateableFields overwrites the nine core fields")
    void bindUpdateableFieldsOverwrites() {
        Llibre target = newBook();
        Llibre.bindUpdateableFields(target,
            9780141439518L, "New", "NewAuthor", 2024, "new desc", 5.0, 9.99, false, "/new.jpg");
        assertThat(target.getISBN()).isEqualTo(9780141439518L);
        assertThat(target.getNom()).isEqualTo("New");
        assertThat(target.getAutor()).isEqualTo("NewAuthor");
        assertThat(target.getAny()).isEqualTo(2024);
        assertThat(target.getDescripcio()).isEqualTo("new desc");
        assertThat(target.getValoracio()).isEqualTo(5.0);
        assertThat(target.getPreu()).isEqualTo(9.99);
        assertThat(target.getLlegit()).isFalse();
        assertThat(target.getImatge()).isEqualTo("/new.jpg");
    }

    // ── Builder ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Llibre.builder() produces a Llibre with given fields")
    void builderProducesLlibre() {
        Llibre l = Llibre.builder()
            .isbn(9780306406157L).nom("Dune").autor("Herbert").any(1965)
            .descripcio("desc").valoracio(9.0).preu(19.99).llegit(true).imatge("/x")
            .build();
        assertThat(l.getISBN()).isEqualTo(9780306406157L);
        assertThat(l.getNom()).isEqualTo("Dune");
    }

    @Test
    @DisplayName("Builder setters return this for chaining")
    void builderChaining() {
        Llibre.Builder b = Llibre.builder();
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
