package domini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Assignació de la llista d'autors a partir del text concatenat que retorna
 * la consulta SQL ({@code GROUP_CONCAT} amb separador {@code ", "}).
 *
 * <p>El mètode ha de ser equivalent a {@code posarAutors(text.split(", ", -1))}
 * però sense tornar a concatenar els noms: el text rebut ja és la
 * concatenació dels seus elements.
 */
class TestLlibreAutorsDeText {

    private Llibre llibre() {
        return new Llibre(9780306406157L, "Dune", "Herbert", 1965, "desc", 9.0, 19.99, true, "");
    }

    @Test
    @DisplayName("Un sol autor")
    void unSolAutor() {
        Llibre l = llibre();
        l.posarAutorsDeText("Herbert");
        assertThat(l.obtenirAutors()).containsExactly("Herbert");
        assertThat(l.obtenirAutor()).isEqualTo("Herbert");
    }

    @Test
    @DisplayName("Dos autors")
    void dosAutors() {
        Llibre l = llibre();
        l.posarAutorsDeText("Herbert, McCarthy");
        assertThat(l.obtenirAutors()).containsExactly("Herbert", "McCarthy");
        assertThat(l.obtenirAutor()).isEqualTo("Herbert, McCarthy");
    }

    @Test
    @DisplayName("Tres autors")
    void tresAutors() {
        Llibre l = llibre();
        l.posarAutorsDeText("A, B, C");
        assertThat(l.obtenirAutors()).containsExactly("A", "B", "C");
        assertThat(l.obtenirAutor()).isEqualTo("A, B, C");
    }

    @Test
    @DisplayName("És equivalent a dividir i concatenar de nou")
    void equivalentASplitIJoin() {
        String[] casos = {"A", "A, B", "A, B, C", "A,, B", "A, "};
        for (String text : casos) {
            Llibre viaText = llibre();
            viaText.posarAutorsDeText(text);
            Llibre viaSplit = llibre();
            viaSplit.posarAutors(java.util.Arrays.asList(text.split(", ", -1)));

            assertThat(viaText.obtenirAutors())
                .as("llista d'autors per a \"%s\"", text)
                .isEqualTo(viaSplit.obtenirAutors());
            assertThat(viaText.obtenirAutor())
                .as("text concatenat per a \"%s\"", text)
                .isEqualTo(viaSplit.obtenirAutor());
        }
    }

    @Test
    @DisplayName("Text buit o null deixen la llista buida")
    void textBuitONull() {
        Llibre buit = llibre();
        buit.posarAutorsDeText("");
        assertThat(buit.obtenirAutors()).isEmpty();
        assertThat(buit.obtenirAutor()).isEmpty();

        Llibre nul = llibre();
        nul.posarAutorsDeText(null);
        assertThat(nul.obtenirAutors()).isEmpty();
        assertThat(nul.obtenirAutor()).isEmpty();
    }

    @Test
    @DisplayName("La llista d'autors reflecteix el text assignat")
    void llistaReflecteixElText() {
        Llibre l = llibre();
        l.posarAutorsDeText("A, B");
        assertThat(l.obtenirAutors()).containsExactly("A", "B");
        assertThat(l.obtenirAutors()).isUnmodifiable();
    }
}
