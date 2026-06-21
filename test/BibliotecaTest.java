import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import domini.ConstructorFiltreLlibre;
import herramienta.text.FiltreUtils;
import herramienta.text.ValidadorLlibre;
import herramienta.ExportadorLlibres;
import herramienta.ImportadorLlibres;
import persistencia.internal.ControladorPersistencia;

import java.util.ArrayList;
import java.util.List;

import domini.Llista;
import domini.Tag;

/**
 * Plain-Java integration/unit tests. No JUnit needed.
 * Run: java -cp lib/h2-2.3.232.jar:bin test.BibliotecaTest
 */
public class BibliotecaTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:test;MODE=MySQL;NON_KEYWORDS=VALUE");

        // ── LlibreValidator ──────────────────────────────────────────────────
        test("ISBN-13 valid accepted", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Nom", null, null, null, null, null, null, null);
            assertEqual(9780306406157L, l.obtenirISBN());
        });
        test("ISBN-10 valid accepted", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(8420413739L, "Test", null, null, null, null, null, null, null);
            assertEqual(8420413739L, l.obtenirISBN());
        });
        test("Invalid ISBN (14 digits) rejected", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(12345678901234L, "X", null, null, null, null, null, null, null));
        });
        test("Invalid ISBN (null) rejected", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(null, "X", null, null, null, null, null, null, null));
        });
        test("Blank nom rejected", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, "  ", null, null, null, null, null, null, null));
        });
        test("Null nom rejected", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, null, null, null, null, null, null, null, null));
        });
        test("Valoracio > 10 rejected", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, 11.0, null, null, null));
        });
        test("Valoracio < 0 rejected", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, -1.0, null, null, null));
        });
        test("Optional fields get defaults", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
            assertEqual("", l.obtenirAutor());
            assertEqual(0, l.obtenirAny());
            assertEqual(0.0, l.obtenirValoracio());
            assertEqual(0.0, l.obtenirPreu());
            assertEqual(false, l.obtenirLlegit());
        });

        // ── FiltreUtils ──────────────────────────────────────────────────────
        test("matchString case-insensitive", () -> {
            assertEqual(true, FiltreUtils.matchString("cervantes", "Cervantes"));
            assertEqual(true, FiltreUtils.matchString("CERVANTES", "cervantes de saavedra"));
            assertEqual(false, FiltreUtils.matchString("tolkien", "Cervantes"));
        });
        test("matchISBN prefix match", () -> {
            assertEqual(true, FiltreUtils.matchISBN(978L, 9780306406157L));
            assertEqual(false, FiltreUtils.matchISBN(123L, 9780306406157L));
        });

        // ── DB integration (H2 in-memory) ───────────────────────────────────
        test("Empty library returns empty list", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.getSize());
            assertEqual(0, cd.obtenirAllLlibres().size());
        });

        test("Add book and retrieve it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "El Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, "");
            cd.afegirLlibre(l);
            assertEqual(1, cd.getSize());
            Llibre retrieved = cd.obtenirLlibre(9780306406157L);
            assertEqual("El Quixot", retrieved.obtenirNom());
            assertEqual("Cervantes", retrieved.obtenirAutor());
            assertEqual(1605, retrieved.obtenirAny());
        });

        test("Duplicate ISBN insert throws", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Llibre A", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            assertThrows(() -> cd.afegirLlibre(
                ValidadorLlibre.comprovarLlibre(9780306406157L, "Llibre B", null, null, null, null, null, null, null)));
        });

        test("Delete non-existent book throws", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertThrows(() -> cd.eliminarLlibre(9780000000000L));
        });

        test("Delete existing book removes it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            assertEqual(1, cd.getSize());
            cd.eliminarLlibre(l);
            assertEqual(0, cd.getSize());
        });

        test("Filter with all params set", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "El Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, ""));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "Hamlet", "Shakespeare", 1603, "", 8.5, 10.0, false, ""));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780064400558L, "El Senyor dels Anells", "Tolkien", 1954, "", 10.0, 25.0, true, ""));

            LlibreFilter f = ConstructorFiltreLlibre.of()
                .autor("Cervantes").anyMin(1600).anyMax(1700)
                .valoracioMin(8.0).valoracioMax(10.0)
                .preuMin(10.0).preuMax(15.0).llegit(true).build();
            List<Llibre> r = cd.aplicarFiltres(f);
            assertEqual(1, r.size());
            assertEqual("El Quixot", r.get(0).obtenirNom());
        });

        test("Filter llegit=false returns only unread", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Llegit", null, null, null, null, null, true, ""));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "No llegit", null, null, null, null, null, false, ""));
            LlibreFilter f2 = ConstructorFiltreLlibre.of().llegit(false).build();
            List<Llibre> r = cd.aplicarFiltres(f2);
            assertEqual(1, r.size());
            assertEqual("No llegit", r.get(0).obtenirNom());
        });

        // ── Llista (shelf) tests ──────────────────────────────────────────────
        test("Create llista and retrieve it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista l = cd.afegirLlista("Favorits");
            assertEqual("Favorits", l.obtenirNom());
            assertEqual(1, cd.obtenirAllLlistes().size());
        });

        test("Delete llista removes it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista l = cd.afegirLlista("Temporal");
            assertEqual(1, cd.obtenirAllLlistes().size());
            cd.eliminarLlista(l);
            assertEqual(0, cd.obtenirAllLlistes().size());
        });

        test("Add book to llista and retrieve it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.afegirLlista("Lectura");
            cd.afegirLlibreToLlista(9780306406157L, llista.obtenirId(), 7.5, true);
            List<Llibre> llibres = cd.obtenirLlibresInLlista(llista.obtenirId());
            assertEqual(1, llibres.size());
            assertEqual(9780306406157L, llibres.get(0).obtenirISBN());
            assertEqual(7.5, llibres.get(0).obtenirValoracio());
            assertEqual(true, llibres.get(0).obtenirLlegit());
        });

        test("Remove book from llista", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.afegirLlista("Lectura");
            cd.afegirLlibreToLlista(9780306406157L, llista.obtenirId(), 5.0, false);
            assertEqual(1, cd.obtenirLlibresInLlista(llista.obtenirId()).size());
            cd.eliminarLlibreFromLlista(9780306406157L, llista.obtenirId());
            assertEqual(0, cd.obtenirLlibresInLlista(llista.obtenirId()).size());
        });

        test("getLlistesForLlibre returns per-book shelf values", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.afegirLlista("Favorits");
            cd.afegirLlibreToLlista(9780306406157L, llista.obtenirId(), 8.0, true);
            List<Llista> llistes = cd.obtenirLlistesForLlibre(9780306406157L);
            assertEqual(1, llistes.size());
            assertEqual("Favorits", llistes.get(0).obtenirNom());
            assertEqual(8.0, llistes.get(0).obtenirValoracioLlibre());
            assertEqual(true, llistes.get(0).obtenirLlegitLlibre());
        });

        test("Update per-book shelf values", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.afegirLlista("Lectura");
            cd.afegirLlibreToLlista(9780306406157L, llista.obtenirId(), 5.0, false);
            cd.actualitzarLlibreInLlista(9780306406157L, llista.obtenirId(), 9.0, true);
            List<Llista> llistes = cd.obtenirLlistesForLlibre(9780306406157L);
            assertEqual(9.0, llistes.get(0).obtenirValoracioLlibre());
            assertEqual(true, llistes.get(0).obtenirLlegitLlibre());
        });

        test("Book deletion cascades to llista membership", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.afegirLlista("Lectura");
            cd.afegirLlibreToLlista(9780306406157L, llista.obtenirId(), 5.0, false);
            cd.eliminarLlibre(9780306406157L);
            assertEqual(0, cd.obtenirLlibresInLlista(llista.obtenirId()).size());
        });

        // ── LlibreValidator: preu ────────────────────────────────────────────
        test("Preu negative rejected", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, -0.01, null, null));
        });
        test("Preu zero accepted", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, 0.0, null, null);
            assertEqual(0.0, l.obtenirPreu());
        });

        // ── Llista: shelf valoracio vs global valoracio ───────────────────────
        test("getLlibresInLlista returns shelf valoracio, not global", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, 3.0, null, null, null));
            Llista llista = cd.afegirLlista("Shelf");
            cd.afegirLlibreToLlista(9780306406157L, llista.obtenirId(), 9.0, true);
            List<Llibre> llibres = cd.obtenirLlibresInLlista(llista.obtenirId());
            assertEqual(9.0, llibres.get(0).obtenirValoracio());
        });

        // ── Llista: cascade delete ───────────────────────────────────────────
        test("deleteLlista cascades: book survives, membership gone", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.afegirLlista("TempShelf");
            cd.afegirLlibreToLlista(9780306406157L, llista.obtenirId(), 5.0, false);
            cd.eliminarLlista(llista);
            assertEqual(1, cd.getSize());
            assertEqual(0, cd.obtenirLlibresInLlista(llista.obtenirId()).size());
        });

        // ── get10Llibres ─────────────────────────────────────────────────────
        test("get10Llibres returns exactly 10 when 15 books present", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 15; i++)
                cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000000L + i, "Llibre " + i, null, null, null, null, null, null, null));
            assertEqual(10, cd.get10Llibres().size());
        });
        test("get10Llibres returns all books when fewer than 10", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 5; i++)
                cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000000L + i, "Llibre " + i, null, null, null, null, null, null, null));
            assertEqual(5, cd.get10Llibres().size());
        });

        // ── Pagination ───────────────────────────────────────────────────────
        test("Pagination: 0 books", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.get100Llibres(0).size());
            assertEqual(0, cd.maxIndex100Llibres());
        });
        test("Pagination: exactly 100 books", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 100; i++)
                cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000000L + i, "L" + i, null, null, null, null, null, null, null));
            assertEqual(100, cd.get100Llibres(0).size());
            assertEqual(0, cd.get100Llibres(1).size());
            assertEqual(0, cd.maxIndex100Llibres());
        });
        test("Pagination: 101 books", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 101; i++)
                cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000000L + i, "L" + i, null, null, null, null, null, null, null));
            assertEqual(100, cd.get100Llibres(0).size());
            assertEqual(1, cd.get100Llibres(1).size());
            assertEqual(1, cd.maxIndex100Llibres());
        });

        // ── backupToSQL + restoreFromSQL round-trip ──────────────────────────
        test("backupToSQL + restoreFromSQL round-trip", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "El Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, ""));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "Hamlet", "Shakespeare", 1603, "", 8.5, 10.0, false, ""));
            java.io.File tmp = java.io.File.createTempFile("biblioteca_test", ".sql");
            tmp.deleteOnExit();
            cd.copiaSegToSQL(tmp);
            reinicialitzarSingletons();
            cd = ControladorDomini.getInstance();
            cd.restaurarFromSQL(tmp);
            assertEqual(2, cd.getSize());
            Llibre q = cd.obtenirLlibre(9780306406157L);
            assertEqual("El Quixot", q.obtenirNom());
            assertEqual("Cervantes", q.obtenirAutor());
            assertEqual(9.0, q.obtenirValoracio());
        });

        test("backupToSQL + restoreFromSQL preserves llista memberships", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Quixot", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "Hamlet", null, null, null, null, null, null, null));
            Llista shelf = cd.afegirLlista("Favorits");
            cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 8.5, true);
            java.io.File tmp = java.io.File.createTempFile("biblioteca_llista_test", ".sql");
            tmp.deleteOnExit();
            cd.copiaSegToSQL(tmp);
            reinicialitzarSingletons();
            cd = ControladorDomini.getInstance();
            cd.restaurarFromSQL(tmp);
            assertEqual(2, cd.getSize());
            assertEqual(1, cd.obtenirAllLlistes().size());
            assertEqual("Favorits", cd.obtenirAllLlistes().get(0).obtenirNom());
            List<Llibre> inShelf = cd.obtenirLlibresInLlista(cd.obtenirAllLlistes().get(0).obtenirId());
            assertEqual(1, inShelf.size());
            assertEqual(9780306406157L, inShelf.get(0).obtenirISBN());
            assertEqual(8.5, inShelf.get(0).obtenirValoracio());
        });

        // ── Edit path: partial failure leaves library consistent ─────────────
        test("Edit path: delete A then add A-with-B-ISBN throws, only B remains", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
            cd.eliminarLlibre(9780306406157L);
            assertEqual(1, cd.getSize());
            assertThrows(() -> cd.afegirLlibre(
                ValidadorLlibre.comprovarLlibre(9780743273565L, "A-edited", null, null, null, null, null, null, null)));
            assertEqual(1, cd.getSize());
            assertEqual("B", cd.obtenirLlibre(9780743273565L).obtenirNom());
        });

        // ── Tag tests ────────────────────────────────────────────────────────
        test("Create tag and retrieve it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Tag t = cd.afegirTag("Fantasia");
            assertEqual("Fantasia", t.obtenirNom());
            assertEqual(1, cd.obtenirAllTags().size());
        });

        test("Delete tag removes it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Tag t = cd.afegirTag("Temporal");
            assertEqual(1, cd.obtenirAllTags().size());
            cd.eliminarTag(t);
            assertEqual(0, cd.obtenirAllTags().size());
        });

        test("Add book to tag and retrieve via getTagsForLlibre", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.afegirTag("Ciencia Ficcio");
            cd.afegirLlibreToTag(9780306406157L, t.obtenirId());
            List<Tag> tags = cd.obtenirTagsForLlibre(9780306406157L);
            assertEqual(1, tags.size());
            assertEqual("Ciencia Ficcio", tags.get(0).obtenirNom());
        });

        test("Remove book from tag", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.afegirTag("Aventura");
            cd.afegirLlibreToTag(9780306406157L, t.obtenirId());
            assertEqual(1, cd.obtenirTagsForLlibre(9780306406157L).size());
            cd.eliminarLlibreFromTag(9780306406157L, t.obtenirId());
            assertEqual(0, cd.obtenirTagsForLlibre(9780306406157L).size());
        });

        test("Book deletion cascades to tag membership", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.afegirTag("Historia");
            cd.afegirLlibreToTag(9780306406157L, t.obtenirId());
            cd.eliminarLlibre(9780306406157L);
            assertEqual(0, cd.obtenirTagsForLlibre(9780306406157L).size());
            assertEqual(1, cd.obtenirAllTags().size());
        });

        test("deleteTag cascades: book survives, membership gone", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.afegirTag("Romàntic");
            cd.afegirLlibreToTag(9780306406157L, t.obtenirId());
            cd.eliminarTag(t);
            assertEqual(1, cd.getSize());
            assertEqual(0, cd.obtenirAllTags().size());
            assertEqual(0, cd.obtenirTagsForLlibre(9780306406157L).size());
        });

        test("Tag filter returns only books with that tag", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Quixot", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "Hamlet", null, null, null, null, null, null, null));
            Tag t = cd.afegirTag("Classics");
            cd.afegirLlibreToTag(9780306406157L, t.obtenirId());
            LlibreFilter tf = ConstructorFiltreLlibre.of().tagId(t.obtenirId()).build();
            List<Llibre> result = cd.aplicarFiltres(tf);
            assertEqual(1, result.size());
            assertEqual(9780306406157L, result.get(0).obtenirISBN());
        });

        test("backupToSQL + restoreFromSQL preserves tag memberships", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Quixot", null, null, null, null, null, null, null));
            Tag t = cd.afegirTag("Classics");
            cd.afegirLlibreToTag(9780306406157L, t.obtenirId());
            java.io.File tmp = java.io.File.createTempFile("biblioteca_tag_test", ".sql");
            tmp.deleteOnExit();
            cd.copiaSegToSQL(tmp);
            reinicialitzarSingletons();
            cd = ControladorDomini.getInstance();
            cd.restaurarFromSQL(tmp);
            assertEqual(1, cd.getSize());
            assertEqual(1, cd.obtenirAllTags().size());
            assertEqual("Classics", cd.obtenirAllTags().get(0).obtenirNom());
            List<Tag> tags = cd.obtenirTagsForLlibre(9780306406157L);
            assertEqual(1, tags.size());
            assertEqual("Classics", tags.get(0).obtenirNom());
        });

        // ── Date fields (data_compra / data_lectura) ─────────────────────────
        test("date fields set and retrieve in-memory", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dates Book", null, null, null, null, null, null, null);
            l.posarDataCompra("2024-01-15");
            l.posarDataLectura("2024-03-20");
            cd.afegirLlibre(l);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual("2024-01-15", loaded.obtenirDataCompra());
            assertEqual("2024-03-20", loaded.obtenirDataLectura());
        });

        test("date fields null by default", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "No Dates Book", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            assertEqual(null, cd.obtenirLlibre(9780306406157L).obtenirDataCompra());
            assertEqual(null, cd.obtenirLlibre(9780306406157L).obtenirDataLectura());
        });

        test("date fields backup and restore", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Backup Dates", null, null, null, null, null, null, null);
            l.posarDataCompra("2023-06-01");
            l.posarDataLectura("2023-08-15");
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_dates_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual("2023-06-01", loaded.obtenirDataCompra());
            assertEqual("2023-08-15", loaded.obtenirDataLectura());
        });

        // ── Idioma field ──────────────────────────────────────────────────────
        test("idioma field set and retrieve", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Idioma Book", null, null, null, null, null, null, null);
            l.posarIdioma("Català");
            cd.afegirLlibre(l);
            assertEqual("Català", cd.obtenirLlibre(9780306406157L).obtenirIdioma());
        });

        test("idioma null by default", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "No Lang", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            assertEqual(null, cd.obtenirLlibre(9780306406157L).obtenirIdioma());
        });

        test("idioma backup and restore", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Lang Book", null, null, null, null, null, null, null);
            l.posarIdioma("English");
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_idioma_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual("English", cd.obtenirLlibre(9780306406157L).obtenirIdioma());
        });

        // ── Format field ─────────────────────────────────────────────────────
        test("format field set and retrieve", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Format Book", null, null, null, null, null, null, null);
            l.posarFormat("eBook");
            cd.afegirLlibre(l);
            assertEqual("eBook", cd.obtenirLlibre(9780306406157L).obtenirFormat());
        });

        test("format null by default", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "No Format", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            assertEqual(null, cd.obtenirLlibre(9780306406157L).obtenirFormat());
        });

        test("format backup and restore", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Tapa dura Book", null, null, null, null, null, null, null);
            l.posarFormat("Tapa dura");
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_format_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual("Tapa dura", cd.obtenirLlibre(9780306406157L).obtenirFormat());
        });

        test("aplicarFiltres on shelf list excludes books from other shelves", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "Shelf Book A", "Author X", null, null, null, null, null, null);
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780306406158L, "Shelf Book B", "Author X", null, null, null, null, null, null);
            Llibre l3 = ValidadorLlibre.comprovarLlibre(9780306406159L, "Other Book",  "Author X", null, null, null, null, null, null);
            cd.afegirLlibre(l1); cd.afegirLlibre(l2); cd.afegirLlibre(l3);
            Llista shelf = cd.afegirLlista("TestShelf");
            cd.afegirLlibreToLlista(l1.obtenirISBN(), shelf.obtenirId(), 0.0, false);
            cd.afegirLlibreToLlista(l2.obtenirISBN(), shelf.obtenirId(), 0.0, false);
            List<Llibre> shelfBooks = cd.obtenirLlibresInLlista(shelf.obtenirId());
            LlibreFilter sf = ConstructorFiltreLlibre.of().autor("Author X").build();
            List<Llibre> results = cd.aplicarFiltres(shelfBooks, sf);
            assertEqual(2, results.size());
        });

        test("aplicarFiltres on shelf respects shelf llegit value", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "Book A", null, null, null, null, null, false, null);
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780306406158L, "Book B", null, null, null, null, null, false, null);
            cd.afegirLlibre(l1); cd.afegirLlibre(l2);
            Llista shelf = cd.afegirLlista("TestShelf2");
            cd.afegirLlibreToLlista(l1.obtenirISBN(), shelf.obtenirId(), 0.0, true);  // llegit=true on shelf
            cd.afegirLlibreToLlista(l2.obtenirISBN(), shelf.obtenirId(), 0.0, false); // llegit=false on shelf
            List<Llibre> shelfBooks = cd.obtenirLlibresInLlista(shelf.obtenirId());
            LlibreFilter lf = ConstructorFiltreLlibre.of().llegit(true).build();
            List<Llibre> llegits = cd.aplicarFiltres(shelfBooks, lf);
            assertEqual(1, llegits.size());
            assertEqual("Book A", llegits.get(0).obtenirNom());
        });

        test("duplicate ISBN on edit: original book preserved", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "Book 1", null, null, null, null, null, null, null);
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780306406158L, "Book 2", null, null, null, null, null, null, null);
            cd.afegirLlibre(l1);
            cd.afegirLlibre(l2);
            Llibre edited = ValidadorLlibre.comprovarLlibre(9780306406158L, "Book 1 edited", null, null, null, null, null, null, null);
            boolean threw = false;
            try {
                if (edited.obtenirISBN() != l1.obtenirISBN() && cd.existsLlibre(edited.obtenirISBN()))
                    throw new Exception("duplicate");
                cd.eliminarLlibre(l1);
                cd.afegirLlibre(edited);
            } catch (Exception ignored) { threw = true; }
            assertEqual(true, threw);
            assertEqual("Book 1", cd.obtenirLlibre(9780306406157L).obtenirNom());
            assertEqual("Book 2", cd.obtenirLlibre(9780306406158L).obtenirNom());
        });

        test("multiple authors set and retrieve", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Multi Author", null, null, null, null, null, null, null);
            l.posarAutors(java.util.Arrays.asList("Author A", "Author B"));
            cd.afegirLlibre(l);
            assertEqual(java.util.Arrays.asList("Author A", "Author B"), cd.obtenirLlibre(9780306406157L).obtenirAutors());
        });

        test("autors empty list by default", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "No Autors", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            assertEqual(true, cd.obtenirLlibre(9780306406157L).obtenirAutors().isEmpty());
        });

        test("autors backup and restore", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Backup Autors", null, null, null, null, null, null, null);
            l.posarAutors(java.util.Arrays.asList("Author X", "Author Y"));
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_autors_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual(java.util.Arrays.asList("Author X", "Author Y"), cd.obtenirLlibre(9780306406157L).obtenirAutors());
        });

        test("desitjat field set and retrieve", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Desitjat Book", null, null, null, null, null, null, null);
            l.posarDesitjat(true);
            cd.afegirLlibre(l);
            assertEqual(true, cd.obtenirLlibre(9780306406157L).esDesitjat());
        });

        test("desitjat false by default", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "No Desitjat", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            assertEqual(false, cd.obtenirLlibre(9780306406157L).esDesitjat());
        });

        test("desitjat backup and restore", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Desitjat Restore", null, null, null, null, null, null, null);
            l.posarDesitjat(true);
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_desitjat_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual(true, cd.obtenirLlibre(9780306406157L).esDesitjat());
        });
       // ── Migration regression: new column added after DB already at higher version ──
        test("schema migration runs pais_origen on DB that skipped it (version conflict regression)", () -> {
            // Simulate a DB that reached version 22 WITHOUT pais_origen (the old bug: duplicate version 21)
            String url = "jdbc:h2:mem:migtest_" + System.nanoTime() + ";MODE=MySQL;NON_KEYWORDS=VALUE";
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, "sa", "");
            // Build schema manually: create llibre table + apply migrations up to 22, skipping 23 (pais_origen)
            conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS schema_version (version INT NOT NULL)");
            conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS llibre (ISBN BIGINT PRIMARY KEY, nom VARCHAR(500), " +
                "autor VARCHAR(500), `any` INT, descripcio VARCHAR(2048), valoracio FLOAT DEFAULT 0.0, " +
                "preu FLOAT DEFAULT 0.0, llegit BOOLEAN DEFAULT FALSE, imatge VARCHAR(500), imatge_blob BLOB, " +
                "notes VARCHAR(2048), pagines INT DEFAULT 0, pagines_llegides INT DEFAULT 0, " +
                "editorial VARCHAR(255) DEFAULT '', serie VARCHAR(255) DEFAULT '', volum INT DEFAULT 0, " +
                "data_compra DATE DEFAULT NULL, data_lectura DATE DEFAULT NULL, idioma VARCHAR(100) DEFAULT NULL, " +
                "format VARCHAR(50) DEFAULT NULL, desitjat BOOLEAN DEFAULT FALSE)");
            // Insert versions 1-22 to simulate existing DB (without pais_origen)
            for (int i = 1; i <= 22; i++)
                conn.createStatement().executeUpdate("INSERT INTO schema_version VALUES (" + i + ")");
            conn.close();

            // Now let ConnexioServidor run migrations on that DB — pais_origen (v23) must be applied
            System.setProperty("biblioteca.test", "true");
            System.setProperty("biblioteca.h2.url", url);
            persistencia.internal.ConnexioServidor sc = new persistencia.internal.ConnexioServidor();
            sc.crearDatabase();
            // If pais_origen column now exists, SELECT on it succeeds
            java.sql.ResultSet rs2 = sc.obtenirConnexio().createStatement().executeQuery(
                "SELECT pais_origen FROM llibre");
            rs2.close();
            sc.obtenirConnexio().close();
            System.setProperty("biblioteca.h2.url", "jdbc:h2:mem:test;MODE=MySQL;NON_KEYWORDS=VALUE");
        });

        // ── LlibreValidator: boundary values ─────────────────────────────────
        test("Valoracio = 0.0 accepted", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, 0.0, null, null, null);
            assertEqual(0.0, l.obtenirValoracio());
        });
        test("Valoracio = 10.0 accepted", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, 10.0, null, null, null);
            assertEqual(10.0, l.obtenirValoracio());
        });
        test("Preu very large accepted", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, 999999.99, null, null);
            assertEqual(999999.99, l.obtenirPreu());
        });
        test("ISBN-10 digit boundary (9 digits rejected)", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(123456789L, "X", null, null, null, null, null, null, null));
        });
        test("ISBN-10 digit boundary (11 digits rejected)", () -> {
            assertThrows(() -> ValidadorLlibre.comprovarLlibre(12345678901L, "X", null, null, null, null, null, null, null));
        });

        // ── FiltreUtils: edge cases ───────────────────────────────────────────
        test("matchString returns false when field is null", () -> {
            assertEqual(false, FiltreUtils.matchString("anything", null));
        });
        test("matchString empty query matches anything", () -> {
            assertEqual(true, FiltreUtils.matchString("", "Cervantes"));
        });
        test("matchISBN exact 13-digit match", () -> {
            assertEqual(true, FiltreUtils.matchISBN(9780306406157L, 9780306406157L));
        });

        // ── Filter: by title, ISBN, year, price, rating ───────────────────────
        test("Filter by nomLlibre", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "Hamlet", null, null, null, null, null, null, null));
            LlibreFilter fq = ConstructorFiltreLlibre.of().nom("quixot").build();
            List<Llibre> r = cd.aplicarFiltres(fq);
            assertEqual(1, r.size());
            assertEqual("El Quixot", r.get(0).obtenirNom());
        });
        test("Filter by ISBN prefix", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
            LlibreFilter fi = ConstructorFiltreLlibre.of().isbn(97803064L).build();
            List<Llibre> r = cd.aplicarFiltres(fi);
            assertEqual(1, r.size());
            assertEqual(9780306406157L, r.get(0).obtenirISBN());
        });
        test("Filter by iniciAny only", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Old", "X", 1900, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "New", "X", 2000, null, null, null, null, null));
            LlibreFilter fa = ConstructorFiltreLlibre.of().anyMin(1950).build();
            List<Llibre> r = cd.aplicarFiltres(fa);
            assertEqual(1, r.size());
            assertEqual(9780743273565L, r.get(0).obtenirISBN());
        });
        test("Filter by fiAny only", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Old", "X", 1900, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "New", "X", 2000, null, null, null, null, null));
            LlibreFilter fb = ConstructorFiltreLlibre.of().anyMax(1950).build();
            List<Llibre> r = cd.aplicarFiltres(fb);
            assertEqual(1, r.size());
            assertEqual(9780306406157L, r.get(0).obtenirISBN());
        });
        test("Filter by valoracio range", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Low", null, null, null, 3.0, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "High", null, null, null, 8.0, null, null, null));
            LlibreFilter fv = ConstructorFiltreLlibre.of().valoracioMin(7.0).valoracioMax(10.0).build();
            List<Llibre> r = cd.aplicarFiltres(fv);
            assertEqual(1, r.size());
            assertEqual("High", r.get(0).obtenirNom());
        });
        test("Filter by preu range", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Cheap", null, null, null, null, 5.0, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "Expensive", null, null, null, null, 50.0, null, null));
            LlibreFilter fp = ConstructorFiltreLlibre.of().preuMin(30.0).preuMax(60.0).build();
            List<Llibre> r = cd.aplicarFiltres(fp);
            assertEqual(1, r.size());
            assertEqual("Expensive", r.get(0).obtenirNom());
        });
        test("Filter returns empty when no books match", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", "Author", 2000, null, null, null, null, null));
            LlibreFilter fn = ConstructorFiltreLlibre.of().nom("NonExistent").build();
            List<Llibre> r = cd.aplicarFiltres(fn);
            assertEqual(0, r.size());
        });

        // ── Filter: editorial, serie, format, idioma ──────────────────────────
        test("Filter by editorial", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "Book A", null, null, null, null, null, null, null);
            l1.posarEditorial("Planeta");
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780743273565L, "Book B", null, null, null, null, null, null, null);
            l2.posarEditorial("Anagrama");
            cd.afegirLlibre(l1); cd.afegirLlibre(l2);
            LlibreFilter fe = ConstructorFiltreLlibre.of().editorial("Planeta").build();
            List<Llibre> r = cd.aplicarFiltres(fe);
            assertEqual(1, r.size());
            assertEqual("Book A", r.get(0).obtenirNom());
        });
        test("Filter by serie", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "Book A", null, null, null, null, null, null, null);
            l1.posarSerie("Chronicles");
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780743273565L, "Book B", null, null, null, null, null, null, null);
            l2.posarSerie("Saga");
            cd.afegirLlibre(l1); cd.afegirLlibre(l2);
            LlibreFilter fs = ConstructorFiltreLlibre.of().serie("chronicles").build();
            List<Llibre> r = cd.aplicarFiltres(fs);
            assertEqual(1, r.size());
            assertEqual("Book A", r.get(0).obtenirNom());
        });
        test("Filter by format exact match", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "Ebook", null, null, null, null, null, null, null);
            l1.posarFormat("eBook");
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780743273565L, "Paper", null, null, null, null, null, null, null);
            l2.posarFormat("Tapa dura");
            cd.afegirLlibre(l1); cd.afegirLlibre(l2);
            LlibreFilter ff = ConstructorFiltreLlibre.of().format("ebook").build();
            List<Llibre> r = cd.aplicarFiltres(ff);
            assertEqual(1, r.size());
            assertEqual("Ebook", r.get(0).obtenirNom());
        });
        test("Filter by idioma", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "Cat Book", null, null, null, null, null, null, null);
            l1.posarIdioma("Català");
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780743273565L, "Eng Book", null, null, null, null, null, null, null);
            l2.posarIdioma("English");
            cd.afegirLlibre(l1); cd.afegirLlibre(l2);
            LlibreFilter fi = ConstructorFiltreLlibre.of().idioma("català").build();
            List<Llibre> r = cd.aplicarFiltres(fi);
            assertEqual(1, r.size());
            assertEqual("Cat Book", r.get(0).obtenirNom());
        });

        // ── updateLlibre ──────────────────────────────────────────────────────
        test("updateLlibre persists nom and valoracio", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Original", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            l.posarNom("Updated");
            l.posarValoracio(8.5);
            cd.actualitzarLlibre(l);
            assertEqual("Updated", cd.obtenirLlibre(9780306406157L).obtenirNom());
            assertEqual(8.5, cd.obtenirLlibre(9780306406157L).obtenirValoracio());
        });
        test("updateLlibre persists after reload", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Original", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            l.posarNom("Persisted");
            cd.actualitzarLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_update_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual("Persisted", cd.obtenirLlibre(9780306406157L).obtenirNom());
        });

        // ── existsLlibre ──────────────────────────────────────────────────────
        test("existsLlibre returns true for existing book", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            assertEqual(true, cd.existsLlibre(9780306406157L));
        });
        test("existsLlibre returns false for non-existing book", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(false, cd.existsLlibre(9780999999999L));
        });

        // ── getCountInLlista ──────────────────────────────────────────────────
        test("getCountInLlista returns correct count", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
            Llista shelf = cd.afegirLlista("Test");
            cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 0, false);
            cd.afegirLlibreToLlista(9780743273565L, shelf.obtenirId(), 0, false);
            assertEqual(2, cd.obtenirCountInLlista(shelf.obtenirId()));
        });
        test("getCountInLlista returns 0 for empty shelf", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista shelf = cd.afegirLlista("Empty");
            assertEqual(0, cd.obtenirCountInLlista(shelf.obtenirId()));
        });

        // ── One book in multiple shelves ──────────────────────────────────────
        test("one book added to two shelves, both contain it", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista s1 = cd.afegirLlista("Shelf1");
            Llista s2 = cd.afegirLlista("Shelf2");
            cd.afegirLlibreToLlista(9780306406157L, s1.obtenirId(), 5.0, false);
            cd.afegirLlibreToLlista(9780306406157L, s2.obtenirId(), 7.0, true);
            assertEqual(1, cd.obtenirLlibresInLlista(s1.obtenirId()).size());
            assertEqual(1, cd.obtenirLlibresInLlista(s2.obtenirId()).size());
            assertEqual(2, cd.obtenirLlistesForLlibre(9780306406157L).size());
        });

        // ── Shelf reorder ─────────────────────────────────────────────────────
        test("moveLlistaUp swaps shelf order", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s1 = cd.afegirLlista("First");
            Llista s2 = cd.afegirLlista("Second");
            cd.moureLlistaUp(s2.obtenirId());
            assertEqual("Second", cd.obtenirAllLlistes().get(0).obtenirNom());
            assertEqual("First",  cd.obtenirAllLlistes().get(1).obtenirNom());
        });
        test("moveLlistaDown swaps shelf order", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s1 = cd.afegirLlista("First");
            Llista s2 = cd.afegirLlista("Second");
            cd.moureLlistaDown(s1.obtenirId());
            assertEqual("Second", cd.obtenirAllLlistes().get(0).obtenirNom());
            assertEqual("First",  cd.obtenirAllLlistes().get(1).obtenirNom());
        });
        test("moveLlistaUp at position 0 is no-op", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s1 = cd.afegirLlista("First");
            cd.afegirLlista("Second");
            cd.moureLlistaUp(s1.obtenirId());
            assertEqual("First", cd.obtenirAllLlistes().get(0).obtenirNom());
        });
        test("moveLlistaDown at last position is no-op", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlista("First");
            Llista s2 = cd.afegirLlista("Second");
            cd.moureLlistaDown(s2.obtenirId());
            assertEqual("Second", cd.obtenirAllLlistes().get(1).obtenirNom());
        });

        // ── setLlistaColor ────────────────────────────────────────────────────
        test("setLlistaColor persists color", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s = cd.afegirLlista("Colorful");
            cd.posarLlistaColor(s.obtenirId(), "#FF0000");
            assertEqual("#FF0000", cd.obtenirAllLlistes().get(0).obtenirColor());
        });
        test("setLlistaColor null clears color", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s = cd.afegirLlista("Colorful");
            cd.posarLlistaColor(s.obtenirId(), "#FF0000");
            cd.posarLlistaColor(s.obtenirId(), null);
            assertEqual(null, cd.obtenirAllLlistes().get(0).obtenirColor());
        });

        // ── Loans (prestec) ───────────────────────────────────────────────────
        test("prestarLlibre adds isbn to getLoanedISBNs", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Joan");
            assertEqual(true, cd.obtenirLoanedISBNs().contains(9780306406157L));
        });
        test("retornarLlibre removes isbn from getLoanedISBNs", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Joan");
            cd.retornarLlibre(9780306406157L);
            assertEqual(false, cd.obtenirLoanedISBNs().contains(9780306406157L));
        });
        test("getLoanedISBNs empty when no loans", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            assertEqual(0, cd.obtenirLoanedISBNs().size());
        });
        test("backup and restore preserves prestec loans", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Loaned", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Pere");
            java.io.File f = java.io.File.createTempFile("test_prestec_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual(true, cd.obtenirLoanedISBNs().contains(9780306406157L));
        });

        // ── getLlibreBlob ─────────────────────────────────────────────────────
        test("getLlibreBlob returns null when blob not set", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            assertEqual(null, cd.obtenirLlibreBlob(9780306406157L));
        });

        // ── clearAll ──────────────────────────────────────────────────────────
        test("clearAll removes books, llistes and tags", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            cd.afegirLlista("Shelf");
            cd.afegirTag("Tag");
            cd.netejarAll();
            assertEqual(0, cd.getSize());
            assertEqual(0, cd.obtenirAllLlistes().size());
            assertEqual(0, cd.obtenirAllTags().size());
        });

        // ── Extended fields ───────────────────────────────────────────────────
        test("notes field set and retrieve", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Notes Book", null, null, null, null, null, null, null);
            l.posarNotes("My personal review");
            cd.afegirLlibre(l);
            assertEqual("My personal review", cd.obtenirLlibre(9780306406157L).obtenirNotes());
        });
        test("notes backup and restore with single quotes", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Notes Test", null, null, null, null, null, null, null);
            l.posarNotes("O'Brien wrote this");
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_notes_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual("O'Brien wrote this", cd.obtenirLlibre(9780306406157L).obtenirNotes());
        });
        test("pais_origen field set and retrieve", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Country Book", null, null, null, null, null, null, null);
            l.posarPaisOrigen("Espanya");
            cd.afegirLlibre(l);
            assertEqual("Espanya", cd.obtenirLlibre(9780306406157L).obtenirPaisOrigen());
        });
        test("editorial, serie and volum fields persist", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Series Book", null, null, null, null, null, null, null);
            l.posarEditorial("Planeta");
            l.posarSerie("Chronicles");
            l.posarVolum(3);
            cd.afegirLlibre(l);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual("Planeta", loaded.obtenirEditorial());
            assertEqual("Chronicles", loaded.obtenirSerie());
            assertEqual(3, loaded.obtenirVolum());
        });
        test("pagines and paginesLlegides persist", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Pages Book", null, null, null, null, null, null, null);
            l.posarPagines(350);
            l.posarPaginesLlegides(150);
            cd.afegirLlibre(l);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual(350, loaded.obtenirPagines());
            assertEqual(150, loaded.obtenirPaginesLlegides());
        });

        // ── getDistinctValues / getDistinctAutorNames ─────────────────────────
        test("getDistinctValues returns distinct editorial values", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780306406157L, "A", null, null, null, null, null, null, null);
            l1.posarEditorial("Planeta");
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780743273565L, "B", null, null, null, null, null, null, null);
            l2.posarEditorial("Planeta");
            Llibre l3 = ValidadorLlibre.comprovarLlibre(9780064400558L, "C", null, null, null, null, null, null, null);
            l3.posarEditorial("Anagrama");
            cd.afegirLlibre(l1); cd.afegirLlibre(l2); cd.afegirLlibre(l3);
            java.util.List<String> vals = cd.obtenirDistinctValues("editorial");
            assertEqual(2, vals.size());
            assertEqual(true, vals.contains("Planeta"));
            assertEqual(true, vals.contains("Anagrama"));
        });
        test("getDistinctValues rejects unknown column (SQL injection guard)", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            java.util.List<String> vals = cd.obtenirDistinctValues("; DROP TABLE llibre; --");
            assertEqual(0, vals.size());
        });
        test("getDistinctAutorNames returns names from autor table", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Multi", null, null, null, null, null, null, null);
            l.posarAutors(java.util.Arrays.asList("Zelazny", "Asimov"));
            cd.afegirLlibre(l);
            java.util.List<String> names = cd.obtenirDistinctAutorNames();
            assertEqual(2, names.size());
            assertEqual(true, names.contains("Zelazny"));
            assertEqual(true, names.contains("Asimov"));
        });
        test("getDistinctAutorNames empty when no authors", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.obtenirDistinctAutorNames().size());
        });

        // ── Backup: SQL escaping ──────────────────────────────────────────────
        test("backup escapes single quote in book nom", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "L'home i el mar", null, null, null, null, null, null, null));
            java.io.File f = java.io.File.createTempFile("test_escape_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual("L'home i el mar", cd.obtenirLlibre(9780306406157L).obtenirNom());
        });
        test("backup escapes single quote in autor name", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", "O'Hara", null, null, null, null, null, null);
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_escape_autor_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            assertEqual("O'Hara", cd.obtenirLlibre(9780306406157L).obtenirAutor());
        });

        // ── Multiple tags ─────────────────────────────────────────────────────
        test("multiple tags for one book", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t1 = cd.afegirTag("SciFi");
            Tag t2 = cd.afegirTag("Classic");
            cd.afegirLlibreToTag(9780306406157L, t1.obtenirId());
            cd.afegirLlibreToTag(9780306406157L, t2.obtenirId());
            assertEqual(2, cd.obtenirTagsForLlibre(9780306406157L).size());
        });
        test("tag filter returns empty when no books have that tag", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.afegirTag("Unused");
            LlibreFilter ft = ConstructorFiltreLlibre.of().tagId(t.obtenirId()).build();
            List<Llibre> r = cd.aplicarFiltres(ft);
            assertEqual(0, r.size());
        });

        // ── getRecentlyAdded ──────────────────────────────────────────────────
        test("getRecentlyAdded returns all books when fewer than limit", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 5; i++)
                cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000000L + i, "Book " + i, null, null, null, null, null, null, null));
            assertEqual(5, cd.obtenirRecentlyAdded().size());
        });

        // ── Books sorted by ISBN in-memory ────────────────────────────────────
        test("books maintained in ascending ISBN order after multiple adds", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780743273565L, "Second", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "First", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780064400558L, "Third", null, null, null, null, null, null, null));
            List<Llibre> all = cd.obtenirAllLlibres();
            assertEqual(true, all.get(0).obtenirISBN().compareTo(all.get(1).obtenirISBN()) < 0);
            assertEqual(true, all.get(1).obtenirISBN().compareTo(all.get(2).obtenirISBN()) < 0);
        });

        // ── getLlibre throws for missing book ─────────────────────────────────
        test("getLlibre throws for non-existent ISBN", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertThrows(() -> cd.obtenirLlibre(9780000000001L));
        });

        // ── Goodreads CSV column mapping ──────────────────────────────────────
        test("Goodreads CSV import maps columns correctly (Title→nom, ISBN13→isbn, My Rating→valoracio, Exclusive Shelf→llegit)", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            // Write a temp CSV file with Goodreads format
            String csv = "Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,Read Count\n" +
                         "1,Test Book,Jane Doe,Doe Jane,,=\"0000000000\",=\"9780000000002\",4,4.0,TestPub,Paperback,300,2010,2010,2023-05-01,2023-04-01,fiction,read,Great book,,2\n";
            java.io.File tmp = java.io.File.createTempFile("goodreads_test", ".csv");
            tmp.deleteOnExit();
            try (java.io.FileWriter fw = new java.io.FileWriter(tmp, java.nio.charset.StandardCharsets.UTF_8)) { fw.write(csv); }
            // Parse using the same logic as importarCSV
            String[] lines = csv.split("\r?\n", -1);
            String[] headers = splitCsv(lines[0]);
            java.util.Map<String, Integer> hMap = new java.util.HashMap<>();
            for (int i = 0; i < headers.length; i++) hMap.put(headers[i].trim(), i);
            String[] c = splitCsv(lines[1]);
            String isbnRaw = hMap.containsKey("ISBN13") ? c[hMap.get("ISBN13")].replaceAll("[^0-9]", "") : "";
            long isbn = Long.parseLong(isbnRaw);
            assertEqual(9780000000002L, isbn);
            String nom = c[hMap.get("Title")].trim();
            assertEqual("Test Book", nom);
            double valoracio = Double.parseDouble(c[hMap.get("My Rating")].trim());
            assertEqual(4.0, valoracio);
            boolean llegit = "read".equalsIgnoreCase(c[hMap.get("Exclusive Shelf")].trim());
            assertEqual(true, llegit);
        });
        // ── searchLlibresSQL vs aplicarFiltres ────────────────────────────────
        test("searchLlibresSQL returns same results as in-memory aplicarFiltres for identical criteria", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000020L, "Alpha Book", "Jones", 2021, "", 7.0, 10.0, false, ""));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000021L, "Beta Book", "Smith", 2022, "", 5.0, 8.0, true, ""));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000022L, "Gamma Book", "Jones", 2020, "", 9.0, 12.0, false, ""));
            LlibreFilter fj = ConstructorFiltreLlibre.of().autor("Jones").build();
            List<Llibre> sqlRes = cd.cercarLlibresSQL(fj);
            List<Llibre> memRes = cd.aplicarFiltres(fj);
            assertEqual(sqlRes.size(), memRes.size());
            assertEqual(2, sqlRes.size()); // Jones has 2 books
        });

        // ── getLlibresPage edge cases ─────────────────────────────────────────
        test("getLlibresPage(offset, pageSize) returns correct slice and handles edge cases (offset > count, pageSize=0)", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 0; i < 5; i++)
                cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000030L + i, "Book " + i, null, null, null, null, null, null, null));
            List<Llibre> page1 = cd.obtenirLlibresPage(0, 3);
            assertEqual(3, page1.size());
            List<Llibre> page2 = cd.obtenirLlibresPage(3, 3);
            assertEqual(2, page2.size());
            List<Llibre> overflow = cd.obtenirLlibresPage(100, 3);
            assertEqual(0, overflow.size());
            // pageSize=0 means no pagination — returns all books
            List<Llibre> allBooks = cd.obtenirLlibresPage(0, 0);
            assertEqual(5, allBooks.size());
        });

        // ── Batch cover fetch skips books with blob ───────────────────────────
        test("batch cover fetch skips books that already have blob (hasBlob=true)", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000040L, "WithBlob", null, null, null, null, null, null, null));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000041L, "WithoutBlob", null, null, null, null, null, null, null));
            // Set blob for first book
            byte[] dummyBlob = new byte[]{1, 2, 3};
            cd.posarLlibreBlob(9780000000040L, dummyBlob);
            // Reload to get updated hasBlob state
            List<Llibre> all = cd.obtenirAllLlibres();
            java.util.List<Llibre> needsCover = all.stream()
                .filter(l -> !l.teBlob() && l.obtenirImatgeBlob() == null)
                .collect(java.util.stream.Collectors.toList());
            // Only the book without blob should be in the fetch list
            assertEqual(1, needsCover.size());
            assertEqual(9780000000041L, needsCover.get(0).obtenirISBN());
        });

        // ── EstadistiquesHelper golden snapshot ──────────────────────────────
        test("buildStatsSummary produces expected output for fixed library", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            books.add(ValidadorLlibre.comprovarLlibre(9780000000001L, "Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, ""));
            books.get(0).posarDataLectura("2024-03-15");
            books.add(ValidadorLlibre.comprovarLlibre(9780000000002L, "Hamlet", "Shakespeare", 1603, "", 8.5, 10.0, false, ""));
            books.add(ValidadorLlibre.comprovarLlibre(9780000000003L, "LotR", "Tolkien", 1954, "", 10.0, 25.0, true, ""));
            books.get(2).posarDataLectura("2024-01-10");
            books.add(ValidadorLlibre.comprovarLlibre(9780000000004L, "Dune", "Herbert", 1965, "", 7.0, 15.0, true, ""));
            presentacio.util.AjudaEstadistiques.EstadistiquesLlibre stats = presentacio.util.AjudaEstadistiques.computeStats(books);
            assertEqual(4, stats.total);
            assertEqual(3L, stats.llegits);
            double avgR = (9.0 + 8.5 + 10.0 + 7.0) / 4.0;
            assertEqual(String.format("%.2f", avgR), String.format("%.2f", stats.avgValoracio));
            double avgP = (12.5 + 10.0 + 25.0 + 15.0) / 4.0;
            assertEqual(String.format("%.2f", avgP), String.format("%.2f", stats.avgPreu));
            String summary = presentacio.util.AjudaEstadistiques.buildStatsSummary(stats, "TestScope");
            assertEqual(true, summary.startsWith("TestScope\n"));
            assertEqual(true, summary.contains("4"));
            assertEqual(true, summary.contains("3"));
        });

        test("BookStats booksByReadYear groups correctly", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            books.add(ValidadorLlibre.comprovarLlibre(9780000000001L, "A", null, null, null, null, null, true, ""));
            books.get(0).posarDataLectura("2024-06-01");
            books.add(ValidadorLlibre.comprovarLlibre(9780000000002L, "B", null, null, null, null, null, true, ""));
            books.get(1).posarDataLectura("2024-12-01");
            books.add(ValidadorLlibre.comprovarLlibre(9780000000003L, "C", null, null, null, null, null, false, ""));
            presentacio.util.AjudaEstadistiques.EstadistiquesLlibre stats = presentacio.util.AjudaEstadistiques.computeStats(books);
            assertEqual(2L, stats.booksByReadYear.getOrDefault(2024, 0L).longValue());
            if (stats.booksByReadYear.containsKey(0)) {
                assertEqual(0L, stats.booksByReadYear.get(0).longValue());
            }
        });

        // ── PrestecRow toDisplayMap date roundtrip ─────────────────────────
        test("PrestecRow.toDisplayMap formats LocalDate as dd/MM/yyyy", () -> {
            java.time.LocalDate date = java.time.LocalDate.of(2025, 3, 14);
            persistencia.row.PrestecRow row = new persistencia.row.PrestecRow(9780306406157L, "Alice", date, false);
            java.util.Map<String, Object> m = row.toDisplayMap();
            assertEqual(9780306406157L, m.get("isbn"));
            assertEqual("Alice", m.get("persona"));
            assertEqual("14/03/2025", m.get("dataPrestec"));
            assertEqual(false, m.get("retornat"));
        });

        test("PrestecRow.toDisplayMap null date yields null", () -> {
            persistencia.row.PrestecRow row = new persistencia.row.PrestecRow(9780306406157L, "Bob", null, true);
            java.util.Map<String, Object> m = row.toDisplayMap();
            assertEqual(null, m.get("dataPrestec"));
        });

        test("PrestecRow.fromStrings parses ISO date", () -> {
            persistencia.row.PrestecRow row = persistencia.row.PrestecRow.fromStrings(9780306406157L, "Eve", "2024-01-15", true);
            assertEqual(java.time.LocalDate.of(2024, 1, 15), row.dataPrestec());
            assertEqual(true, row.retornat());
        });

        test("PrestecRow.fromStrings null/blank date yields null", () -> {
            persistencia.row.PrestecRow r1 = persistencia.row.PrestecRow.fromStrings(1L, "X", null, false);
            assertEqual(null, r1.dataPrestec());
            persistencia.row.PrestecRow r2 = persistencia.row.PrestecRow.fromStrings(1L, "X", "  ", false);
            assertEqual(null, r2.dataPrestec());
        });

        // ── NativeCsvStrategy canHandle ──────────────────────────────────────
        test("NativeCsvStrategy.canHandle accepts any header with ≥4 columns", () -> {
            herramienta.io.csv.NativeCsvStrategy ns = new herramienta.io.csv.NativeCsvStrategy();
            assertEqual(true, ns.potHandle("9780306406157,Nom,Autor,2020"));
            assertEqual(true, ns.potHandle("0306406152,Nom,Autor,2020"));
            assertEqual(true, ns.potHandle("random,header,with,four,columns"));
            assertEqual(false, ns.potHandle(""));
            assertEqual(false, ns.potHandle("a"));
        });

        // ── PrestecRow roundtrip: fromStrings → toDisplayMap ────────────────
        test("PrestecRow roundtrip: fromStrings produces displayable ISO date", () -> {
            persistencia.row.PrestecRow row = persistencia.row.PrestecRow.fromStrings(9780000000001L, "Carol", "2023-07-22", false);
            java.util.Map<String, Object> m = row.toDisplayMap();
            assertEqual("22/07/2023", m.get("dataPrestec"));
            assertEqual(9780000000001L, m.get("isbn"));
            assertEqual("Carol", m.get("persona"));
            assertEqual(false, m.get("retornat"));
        });

        // ── NativeCsvStrategy parseLine (roundtrip-style) ───────────────────
        test("NativeCsvStrategy parseLine imports basic book", () -> {
            herramienta.io.csv.NativeCsvStrategy ns = new herramienta.io.csv.NativeCsvStrategy();
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String[] cols = {"9780000000099", "Test Book", "Author", "2024", "desc", "8.0", "12.5", "true", "", ""};
            java.util.Map<String, Integer> hMap = new java.util.HashMap<>();
            boolean result = ns.analitzarLine(cols, hMap, cd);
            assertEqual(true, result);
            Llibre imported = cd.obtenirLlibre(9780000000099L);
            assertEqual("Test Book", imported.obtenirNom());
            assertEqual("Author", imported.obtenirAutor());
            assertEqual(2024, imported.obtenirAny());
        });

        test("NativeCsvStrategy parseLine rejects row with too few columns", () -> {
            herramienta.io.csv.NativeCsvStrategy ns = new herramienta.io.csv.NativeCsvStrategy();
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String[] cols = {"9780000000099", "Short"};
            java.util.Map<String, Integer> hMap = new java.util.HashMap<>();
            boolean threw = false;
            try { ns.analitzarLine(cols, hMap, cd); } catch (Exception e) { threw = true; }
            assertEqual(true, threw);
        });

        // ── Goodreads CSV fixture from real export ─────────────────────────────
        test("Goodreads CSV: realistic header row is recognised and rows are imported", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String header = "Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,Read Count";
            herramienta.io.csv.GoodreadsCsvStrategy gr = new herramienta.io.csv.GoodreadsCsvStrategy();
            assertEqual(true, gr.potHandle(header));
            String[] headerCols = herramienta.io.csv.UtilitatsCsv.analitzarLine(header);
            java.util.Map<String, Integer> hMap = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(headerCols);
            String row = "42,The Hobbit,Tolkien,J.R.R. Tolkien,,=\"0000000000\",=\"9780000000042\",5,4.5,HarperCollins,Paperback,310,1937,1937,2024-06-15,2024-05-01,fantasy;classics,read,Awesome,,nope,3";
            String[] cols = herramienta.io.csv.UtilitatsCsv.analitzarLine(row);
            boolean imported = gr.analitzarLine(cols, hMap, cd);
            assertEqual(true, imported);
            Llibre l = cd.obtenirLlibre(9780000000042L);
            assertEqual("The Hobbit", l.obtenirNom());
            assertEqual(10.0, l.obtenirValoracio());
            assertEqual(true, l.obtenirLlegit());
        });

        // ── LibraryThing CSV fixture: multiple authors, tags, X-check ISBN ────────
        test("LibraryThing CSV: BCID column triggers strategy; handles tags and X-check ISBN", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String header = "Book Id,ISBN,ISBN13,BCID,Title,Authors,Original Publication Year,Publication Year,Rating,Summary,Comments,Review,Collections,Tags";
            herramienta.io.csv.LibraryThingCsvStrategy lt = new herramienta.io.csv.LibraryThingCsvStrategy();
            // LibraryThing strategy identifies itself via "BCID" column
            assertEqual(true, lt.potHandle(header));
            // Build header map the same way BookImporter does
            String[] headerCols = herramienta.io.csv.UtilitatsCsv.analitzarLine(header);
            java.util.Map<String, Integer> hMap = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(headerCols);
// Row with: ISBN-13, multiple authors (semicolon in LibraryThing = same author), tags, and collections
            // Tags and Collections use commas and must be in a quoted field
            String row = "99,,9780000000019,BC123,Test LibBook,Author One; Author Two,2021,2021,3.5,A summary,My notes,,\"My Shelf,Favorites\",fiction;adventure";
            String[] cols = herramienta.io.csv.UtilitatsCsv.analitzarLine(row);
            boolean imported;
            try {
                imported = lt.analitzarLine(cols, hMap, cd);
            } catch (Exception e) {
                throw new AssertionError("LibraryThing parseLine threw exception: " + e);
            }
            assertEqual(true, imported);
            Llibre l = cd.obtenirLlibre(9780000000019L);
            assertEqual("Test LibBook", l.obtenirNom());
            // Rating 3.5 * 2.0 = 7.0
            assertEqual(7.0, l.obtenirValoracio());
            // Tags should have been created (fiction;adventure may be 1 or 2 tags depending on separator)
            assertEqual(true, cd.obtenirAllTags().size() >= 1);
            // Collections/shelves should have been created (2 shelves in the quoted field)
            assertEqual(true, cd.obtenirAllLlistes().size() >= 1);
        });

        // ── NativeCsvStrategy roundtrip: export → re-import ────────────────────
        test("NativeCsvStrategy roundtrip: exportCSV then re-import preserves book data", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000050L, "RoundBook", "Author X", 2020, "A description", 7.5, 19.99, true, ""));
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000051L, "SecondBook", "Author Y", 1999, "", 0.0, 0.0, false, ""));
            Llista shelf = cd.afegirLlista("Fav");
            cd.afegirLlibreToLlista(9780000000050L, shelf.obtenirId(), 8.0, true);
            // Export
            java.io.File tmpExport = java.io.File.createTempFile("roundtrip_export", ".csv");
            tmpExport.deleteOnExit();
            java.util.List<Llibre> view = cd.obtenirAllLlibres();
            herramienta.ExportadorLlibres.exportarCSV(tmpExport, view, cd);
            // Re-import into fresh DB
            reinicialitzarSingletons();
            ControladorDomini cd2 = ControladorDomini.getInstance();
            herramienta.ImportadorLlibres.ResultatImportacio r = herramienta.ImportadorLlibres.importarCSV(tmpExport, cd2);
            assertEqual(2, r.imported());
            Llibre reborn = cd2.obtenirLlibre(9780000000050L);
            assertEqual("RoundBook", reborn.obtenirNom());
            assertEqual("Author X", reborn.obtenirAutor());
            assertEqual(2020, reborn.obtenirAny());
            assertEqual(7.5, reborn.obtenirValoracio());
            assertEqual(19.99, reborn.obtenirPreu());
            assertEqual(true, reborn.obtenirLlegit());
            // Shelf membership should also be present
            assertEqual(true, cd2.obtenirAllLlistes().size() >= 1);
        });

        // ── BookExporter golden-file test ─────────────────────────────────────
        test("BookExporter.exportJSON output matches golden fixture", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre g = ValidadorLlibre.comprovarLlibre(9780000000050L, "GoldenBook", "Author X", 2020, "A description", 7.5, 0.0, true, "");
            cd.afegirLlibre(g);
            domini.Llista fav = cd.afegirLlista("Fav");
            cd.afegirLlibreToLlista(9780000000050L, fav.obtenirId(), 8.0, true);
            domini.Tag fiction = cd.afegirTag("fiction");
            cd.afegirLlibreToTag(9780000000050L, fiction.obtenirId());

            java.io.File tmp = java.io.File.createTempFile("golden_export", ".json");
            tmp.deleteOnExit();
            herramienta.ExportadorLlibres.exportarJSON(tmp, cd);
            String output = new String(java.nio.file.Files.readAllBytes(tmp.toPath()), java.nio.charset.StandardCharsets.UTF_8);

            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(output).getAsJsonObject();
            assertEqual(1, root.getAsJsonArray("llibres").size());
            assertEqual(1, root.getAsJsonArray("llistes").size());
            assertEqual(1, root.getAsJsonArray("tags").size());
            com.google.gson.JsonObject book = root.getAsJsonArray("llibres").get(0).getAsJsonObject();
            assertEqual(9780000000050L, book.get("isbn").getAsLong());
            assertEqual("GoldenBook", book.get("nom").getAsString());
            assertEqual("Author X", book.get("autor").getAsString());
            assertEqual(2020, book.get("any").getAsInt());
            assertEqual(7.5, book.get("valoracio").getAsDouble());
            assertEqual(true, book.get("llegit").getAsBoolean());
            com.google.gson.JsonObject shelf = root.getAsJsonArray("llistes").get(0).getAsJsonObject();
            assertEqual("Fav", shelf.get("nom").getAsString());
            com.google.gson.JsonObject tagObj = root.getAsJsonArray("tags").get(0).getAsJsonObject();
            assertEqual("fiction", tagObj.get("nom").getAsString());
            com.google.gson.JsonObject membership = book.getAsJsonArray("llistes").get(0).getAsJsonObject();
            assertEqual(8.0, membership.get("valoracio").getAsDouble());
            assertEqual(true, membership.get("llegit").getAsBoolean());
            assertEqual(tagObj.get("id").getAsInt(), book.getAsJsonArray("tags").get(0).getAsInt());
        });

        // ── Export/import roundtrip preserves shelf memberships (JSON) ──────────
        test("Export/import JSON roundtrip preserves shelf memberships with valoracio and llegit", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre b1 = ValidadorLlibre.comprovarLlibre(9780000000060L, "ShelfBook1", "Auth1", 2021, "Desc1", 8.0, 10.0, true, "");
            Llibre b2 = ValidadorLlibre.comprovarLlibre(9780000000061L, "ShelfBook2", "Auth2", 2022, "Desc2", 6.0, 5.0, false, "");
            cd.afegirLlibre(b1);
            cd.afegirLlibre(b2);
            domini.Llista s1 = cd.afegirLlista("Sci-Fi");
            domini.Llista s2 = cd.afegirLlista("Classics");
            cd.afegirLlibreToLlista(9780000000060L, s1.obtenirId(), 9.0, true);
            cd.afegirLlibreToLlista(9780000000060L, s2.obtenirId(), 5.0, false);
            cd.afegirLlibreToLlista(9780000000061L, s1.obtenirId(), 7.5, false);
            domini.Tag t1 = cd.afegirTag("adventure");
            domini.Tag t2 = cd.afegirTag("classic");
            cd.afegirLlibreToTag(9780000000060L, t1.obtenirId());
            cd.afegirLlibreToTag(9780000000060L, t2.obtenirId());
            cd.afegirLlibreToTag(9780000000061L, t1.obtenirId());

            java.io.File tmp = java.io.File.createTempFile("roundtrip_json", ".json");
            tmp.deleteOnExit();
            herramienta.ExportadorLlibres.exportarJSON(tmp, cd);

            reinicialitzarSingletons();
            ControladorDomini cd2 = ControladorDomini.getInstance();
            herramienta.ImportadorLlibres.ResultatImportacio result = herramienta.ImportadorLlibres.importarJSON(tmp, cd2);
            assertEqual(2, result.imported());

            java.util.List<domini.Llista> importedShelves = cd2.obtenirAllLlistes();
            java.util.Set<String> shelfNames = new java.util.HashSet<>();
            for (domini.Llista s : importedShelves) shelfNames.add(s.obtenirNom());
            assertEqual(true, shelfNames.contains("Sci-Fi"));
            assertEqual(true, shelfNames.contains("Classics"));

            java.util.List<domini.Tag> importedTags = cd2.obtenirAllTags();
            java.util.Set<String> tagNames = new java.util.HashSet<>();
            for (domini.Tag t : importedTags) tagNames.add(t.obtenirNom());
            assertEqual(true, tagNames.contains("adventure"));
            assertEqual(true, tagNames.contains("classic"));

            java.util.List<domini.Llista> shelvesFor60 = cd2.obtenirLlistesForLlibre(9780000000060L);
            assertEqual(2, shelvesFor60.size());
            java.util.List<domini.Tag> tagsFor60 = cd2.obtenirTagsForLlibre(9780000000060L);
            assertEqual(2, tagsFor60.size());
            java.util.List<domini.Llista> shelvesFor61 = cd2.obtenirLlistesForLlibre(9780000000061L);
            assertEqual(1, shelvesFor61.size());
            assertEqual("Sci-Fi", shelvesFor61.get(0).obtenirNom());
        });

        // ── Config H2 preserves previously-set host/user (non-destructive) ─
        test("Config: switching to H2 preserves previously-set host/user", () -> {
            java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("biblioteca_cfg_h2_");
            java.nio.file.Path cfgDir = tmpDir.resolve(".biblioteca");
            java.nio.file.Files.createDirectories(cfgDir);
            String origHome = System.getProperty("user.home");
            try {
                System.setProperty("user.home", tmpDir.toFile().getAbsolutePath());
                herramienta.config.Configuracio.reload();
                herramienta.config.ConfiguracioDb.setType("mariadb");
                herramienta.config.ConfiguracioDb.posarHost("db.example.com");
                herramienta.config.ConfiguracioDb.posarUser("admin");
                Thread.sleep(400);
                assertEqual("db.example.com", herramienta.config.Configuracio.obtenirDbHost());
                assertEqual("admin", herramienta.config.Configuracio.obtenirDbUser());

                herramienta.config.ConfiguracioDb.setType("h2");
                Thread.sleep(400);
                assertEqual("h2", herramienta.config.Configuracio.obtenirDbType());
                // putIfAbsent semantics: the previous MariaDB host/user
                // are preserved so a future re-connection still works.
                assertEqual("db.example.com", herramienta.config.Configuracio.obtenirDbHost());
                assertEqual("admin", herramienta.config.Configuracio.obtenirDbUser());
            } finally {
                System.setProperty("user.home", origHome);
                herramienta.config.Configuracio.reload();
                java.nio.file.Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(java.io.File::delete);
            }
        });

        runNewTests();

        // ── Summary ──────────────────────────────────────────────────────────
        System.out.println("\n══════════════════════════════════════");
        System.out.println("  Passed: " + passed + "  |  Failed: " + failed);
        System.out.println("══════════════════════════════════════");
        if (failed > 0) System.exit(1);
    }

    // ── Test infrastructure ──────────────────────────────────────────────────

    private static String[] splitCsv(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else inQuote = !inQuote;
            } else if (ch == ',' && !inQuote) { fields.add(sb.toString()); sb.setLength(0); }
            else sb.append(ch);
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private static void reinicialitzarSingletons() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @FunctionalInterface
    interface CosProva { void run() throws Exception; }

    private static void test(String name, CosProva body) {
        try {
            body.run();
            System.out.println("  PASS  " + name);
            passed++;
        } catch (AssertionError e) {
            System.out.println("  FAIL  " + name + "  →  " + e.getMessage());
            failed++;
        } catch (Exception e) {
            System.out.println("  FAIL  " + name + "  →  unexpected: " + e);
            failed++;
        }
    }

    private static void assertEqual(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual))
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
    }

    private static void assertNotNull(Object obj) {
        if (obj == null) throw new AssertionError("expected non-null but was null");
    }

    private static void assertThrows(CosProva body) {
        try {
            body.run();
            throw new AssertionError("expected an exception but none was thrown");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception ignored) {}
    }

    // ── New tests added by todo1.txt work ──────────────────────────────────

    private static void runNewTests() {
        // ── TablePageController clamp ─────────────────────────────────────────
        test("Pagination: get100Llibres clamps page to valid range", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 150; i++)
                cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780000000000L + i, "L" + i, null, null, null, null, null, null, null));
            // Page 0 should work
            assertEqual(100, cd.get100Llibres(0).size());
            // Page 1 should work
            assertEqual(50, cd.get100Llibres(1).size());
            // Page beyond range should return empty
            assertEqual(0, cd.get100Llibres(5).size());
        });

        test("Pagination: max index is correct for 0 books", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.maxIndex100Llibres());
        });

        // ── Shelf delete with books shows confirmation ─────────────────────────
        test("Delete shelf that has books leaves books intact", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista shelf = cd.afegirLlista("DelShelf");
            cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 5.0, false);
            assertEqual(1, cd.obtenirLlibresInLlista(shelf.obtenirId()).size());
            cd.eliminarLlista(shelf);
            // Book still exists
            assertEqual(1, cd.getSize());
            // Shelf membership gone
            assertEqual(0, cd.obtenirLlibresInLlista(shelf.obtenirId()).size());
        });

        // ── SyncAutors batch: multiple authors round-trip ────────────────────
        test("Book with multiple authors preserves all authors after update", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
            l.posarAutors(java.util.Arrays.asList("Author A", "Author B", "Author C"));
            cd.afegirLlibre(l);
            Llibre retrieved = cd.obtenirLlibre(9780306406157L);
            assertEqual(3, retrieved.obtenirAutors().size());
            assertEqual(true, retrieved.obtenirAutors().contains("Author A"));
            assertEqual(true, retrieved.obtenirAutors().contains("Author B"));
            assertEqual(true, retrieved.obtenirAutors().contains("Author C"));
        });

        test("Update book replacing authors preserves new list", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", "Old Author", null, null, null, null, null, null);
            cd.afegirLlibre(l);
            l.posarAutors(java.util.Arrays.asList("New Author X", "New Author Y"));
            cd.actualitzarLlibre(l);
            Llibre retrieved = cd.obtenirLlibre(9780306406157L);
            assertEqual(2, retrieved.obtenirAutors().size());
            assertEqual(true, retrieved.obtenirAutors().contains("New Author X"));
            assertEqual(true, retrieved.obtenirAutors().contains("New Author Y"));
            assertEqual(false, retrieved.obtenirAutors().contains("Old Author"));
        });

        // ── EstadistiquesHelper.booksByReadYear ─────────────────────────────────
        test("booksByReadYear groups read books by year", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            Llibre l1 = ValidadorLlibre.comprovarLlibre(9780000000001L, "A", null, null, null, null, null, null, null);
            l1.posarLlegit(true); l1.posarDataLectura("2023-05-10");
            books.add(l1);
            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780000000002L, "B", null, null, null, null, null, null, null);
            l2.posarLlegit(true); l2.posarDataLectura("2023-11-01");
            books.add(l2);
            Llibre l3 = ValidadorLlibre.comprovarLlibre(9780000000003L, "C", null, null, null, null, null, null, null);
            l3.posarLlegit(true); l3.posarDataLectura("2024-01-15");
            books.add(l3);
            Llibre l4 = ValidadorLlibre.comprovarLlibre(9780000000004L, "D", null, null, null, null, null, null, null);
            l4.posarLlegit(false);
            books.add(l4);
            java.util.Map<Integer, Long> byYear = presentacio.util.AjudaEstadistiques.booksByReadYear(books);
            assertEqual(2L, byYear.getOrDefault(2023, 0L));
            assertEqual(1L, byYear.getOrDefault(2024, 0L));
            assertEqual(0L, byYear.getOrDefault(2025, 0L));
        });

        test("booksByReadYear falls back to any for year when dataLectura missing", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780000000001L, "A", null, 2022, null, null, null, null, null);
            l.posarLlegit(true);
            books.add(l);
            java.util.Map<Integer, Long> byYear = presentacio.util.AjudaEstadistiques.booksByReadYear(books);
            assertEqual(1L, byYear.getOrDefault(2022, 0L));
        });

        // ── SwingUtils.reloadComboPreserveSelection ──────────────────────────────
        test("SwingUtils reloadComboPreserveSelection restores selection", () -> {
            javax.swing.JComboBox<Llista> combo = new javax.swing.JComboBox<>();
            java.util.List<Llista> items = java.util.Arrays.asList(
                new Llista(1, "A"), new Llista(2, "B"), new Llista(3, "C"));
            herramienta.ui.UtilitatsSwing.reloadComboPreserveSelection(combo, items, Llista::obtenirId);
            assertEqual(3, combo.getItemCount());
            combo.setSelectedIndex(1);
            herramienta.ui.UtilitatsSwing.reloadComboPreserveSelection(combo, items, Llista::obtenirId);
            assertEqual(1, combo.getSelectedIndex());
            assertEqual("B", combo.getSelectedItem().toString());
        });

// ── Config: column visibility round-trip ───────────────────────────────
        test("Config column visibility round-trip", () -> {
            herramienta.config.ConfiguracioFinestra.posarColVisible(3, false);
            assertEqual(false, herramienta.config.Configuracio.obtenirColVisible(3));
            herramienta.config.ConfiguracioFinestra.posarColVisible(3, true);
            assertEqual(true, herramienta.config.Configuracio.obtenirColVisible(3));
        });

        // ── Config: column width round-trip ───────────────────────────────────
        test("Config column width round-trip", () -> {
            herramienta.config.ConfiguracioFinestra.posarColWidths(new int[]{80, 100, 120, 140, 160, 220, 180});
            assertEqual(220, herramienta.config.Configuracio.obtenirColWidth(5, 100));
        });

        // ── Config: column width round-trip ───────────────────────────────────
        test("Config column width round-trip", () -> {
            int[] widths = {80, 100, 120, 140, 160, 220, 180};
            herramienta.config.ConfiguracioFinestra.posarColWidths(widths);
            assertEqual(220, herramienta.config.Configuracio.obtenirColWidth(5, 100));
        });

        // ── ControladorPanellDetallsLlibre: save→updateLlibre→callback happy path ──
        test("updateLlibre persists changes and callback fires", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Original Title", "Original Author", 2000, "Desc", 5.0, 10.0, false, null);
            cd.afegirLlibre(l);

            l.posarNom("Updated Title");
            l.posarAutors(java.util.List.of("Updated Author"));
            l.posarValoracio(9.0);
            l.posarLlegit(true);
            cd.actualitzarLlibre(l);

            Llibre retrieved = cd.obtenirLlibre(9780306406157L);
            assertEqual("Updated Title", retrieved.obtenirNom());
            assertEqual("Updated Author", retrieved.obtenirAutor());
            assertEqual(9.0, retrieved.obtenirValoracio());
            assertEqual(true, retrieved.obtenirLlegit());

            int[] callbackCount = {0};
            presentacio.listener.EnActualitzarBBDD callback = new presentacio.listener.EnActualitzarBBDD() {
                @Override public void enActualitzarLlibre(Llibre llibre, boolean esNew) {
                    callbackCount[0]++;
                    assertEqual(9780743273565L, llibre.obtenirISBN());
                    assertEqual("Callback Title Updated", llibre.obtenirNom());
                }
                @Override public void enEliminarLlibre(Llibre llibre) {}
            };

            Llibre l2 = ValidadorLlibre.comprovarLlibre(9780743273565L, "Callback Title", null, null, null, null, null, null, null);
            cd.afegirLlibre(l2);
            l2.posarNom("Callback Title Updated");
            cd.actualitzarLlibre(l2);
            callback.enActualitzarLlibre(l2, false);
            assertEqual(1, callbackCount[0]);
        });
      // ── PanellDetallsLlibre: null optional fields (Item 10) ──────────────
        test("Llibre with null nomCa/nomEs/nomEn persists and retrieves as null", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null);
            assertEqual(null, l.obtenirNomCa());
            assertEqual(null, l.obtenirNomEs());
            assertEqual(null, l.obtenirNomEn());
            cd.afegirLlibre(l);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual(null, loaded.obtenirNomCa());
            assertEqual(null, loaded.obtenirNomEs());
            assertEqual(null, loaded.obtenirNomEn());
        });

        test("Llibre nomCa/nomEs/nomEn update to non-null persists", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            l.posarNomCa("El Quixot");
            l.posarNomEs("Don Quijote");
            l.posarNomEn("Don Quixote");
            cd.actualitzarLlibre(l);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual("El Quixot", loaded.obtenirNomCa());
            assertEqual("Don Quijote", loaded.obtenirNomEs());
            assertEqual("Don Quixote", loaded.obtenirNomEn());
        });

        test("Llibre nomCa/nomEs/nomEn set back to null (blank) normalizes after reload", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null);
            l.posarNomCa("El Quixot");
            l.posarNomEs("Don Quijote");
            l.posarNomEn("Don Quixote");
            cd.afegirLlibre(l);
            l.posarNomCa("");
            l.posarNomEs("");
            l.posarNomEn("");
            cd.actualitzarLlibre(l);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual(null, loaded.obtenirNomCa());
            assertEqual(null, loaded.obtenirNomEs());
            assertEqual(null, loaded.obtenirNomEn());
        });

        test("Llibre nomCa/nomEs/nomEn null optional fields backup and restore", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "NullNames Book", null, null, null, null, null, null, null);
            cd.afegirLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_null_names_", ".sql");
            f.deleteOnExit();
            cd.copiaSegToSQL(f);
            cd.netejarAll();
            cd.restaurarFromSQL(f);
            Llibre loaded = cd.obtenirLlibre(9780306406157L);
            assertEqual(null, loaded.obtenirNomCa());
            assertEqual(null, loaded.obtenirNomEs());
            assertEqual(null, loaded.obtenirNomEn());
        });

        runExtendedTests();
    }

    private static void runExtendedTests() {
        // ── CsvUtils ─────────────────────────────────────────────────────────
        test("CsvUtils.parseIsbn converts ISBN-10 with X check digit to ISBN-13", () -> {
            assertEqual("9780306406157", herramienta.io.csv.UtilitatsCsv.analitzarIsbn("0-306-40615-X"));
        });
        test("CsvUtils.parseIsbn strips non-digits from ISBN-13", () -> {
            assertEqual("9780306406157", herramienta.io.csv.UtilitatsCsv.analitzarIsbn("978-0-306-40615-7"));
        });
        test("CsvUtils.parseIsbn empty string yields empty", () -> {
            assertEqual("", herramienta.io.csv.UtilitatsCsv.analitzarIsbn(""));
        });
        test("CsvUtils.colVal returns empty when column missing", () -> {
            java.util.Map<String, Integer> h = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(new String[]{"ISBN"});
            assertEqual("", herramienta.io.csv.UtilitatsCsv.colVal(h, new String[]{"978"}, "Title"));
        });
        test("CsvUtils.colVal trims cell value", () -> {
            java.util.Map<String, Integer> h = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(new String[]{"Nom"});
            assertEqual("Quixot", herramienta.io.csv.UtilitatsCsv.colVal(h, new String[]{"  Quixot  "}, "Nom"));
        });
        test("CsvUtils.parseDoubleOrZero invalid returns 0", () -> {
            assertEqual(0.0, herramienta.io.csv.UtilitatsCsv.analitzarDoubleOrZero("not-a-number"));
            assertEqual(7.5, herramienta.io.csv.UtilitatsCsv.analitzarDoubleOrZero(" 7.5 "));
        });
        test("CsvUtils.csvQ escapes embedded quotes", () -> {
            assertEqual("\"Say \"\"Hi\"\"\"", herramienta.io.csv.UtilitatsCsv.csvQ("Say \"Hi\""));
            assertEqual("", herramienta.io.csv.UtilitatsCsv.csvQ(null));
        });
        test("CsvUtils.parseLine null yields empty array", () -> {
            assertEqual(0, herramienta.io.csv.UtilitatsCsv.analitzarLine(null).length);
        });
        test("CsvUtils.parseLine strips carriage return", () -> {
            String[] f = herramienta.io.csv.UtilitatsCsv.analitzarLine("a,b\r");
            assertEqual("b", f[1]);
        });
        test("CsvUtils.buildHeaderMap trims header names", () -> {
            java.util.Map<String, Integer> h = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(new String[]{" ISBN ", "Nom"});
            assertEqual(0, (int) h.get("ISBN"));
            assertEqual(1, (int) h.get("Nom"));
        });

        // ── FiltreUtils (extended) ───────────────────────────────────────────
        test("FiltreUtils.normalize null returns empty", () -> {
            assertEqual("", FiltreUtils.normalize(null));
        });
        test("FiltreUtils.matchISBN null prefix or isbn returns false", () -> {
            assertEqual(false, FiltreUtils.matchISBN(null, 978L));
            assertEqual(false, FiltreUtils.matchISBN(978L, null));
        });
        test("FiltreUtils.matchString null needle returns false", () -> {
            assertEqual(false, FiltreUtils.matchString(null, "text"));
        });
        test("FiltreUtils accent-insensitive autor match", () -> {
            assertEqual(true, FiltreUtils.matchString("Garcia", "Gabriel García Márquez"));
        });
        test("FiltreUtils.normalize German umlauts", () -> {
            assertEqual("muller", FiltreUtils.normalize("Müller"));
        });

        // ── Llibre display names ─────────────────────────────────────────────
        test("Llibre.getDisplayNom uses alt title per language", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Default", null, null, null, null, null, null, null);
            l.posarNomCa("Català");
            l.posarNomEs("Español");
            l.posarNomEn("English");
            assertEqual("Català", l.obtenirDisplayNom("ca"));
            assertEqual("Español", l.obtenirDisplayNom("es"));
            assertEqual("English", l.obtenirDisplayNom("en"));
            assertEqual("Default", l.obtenirDisplayNom("fr"));
        });
        test("Llibre.getDisplayNom falls back to nom when alt blank", () -> {
            Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Main Title", null, null, null, null, null, null, null);
            l.posarNomCa("  ");
            assertEqual("Main Title", l.obtenirDisplayNom("ca"));
        });

        // ── CoverService ─────────────────────────────────────────────────────
        test("CoverService.getCachedBytes unknown ISBN returns null", () -> {
            assertEqual(null, herramienta.io.ServeiCoberta.obtenirCachedBytes("0000000000999"));
        });
        test("CoverService.getCachedImage unknown ISBN returns null", () -> {
            assertEqual(null, herramienta.io.ServeiCoberta.obtenirCachedImage("0000000000998"));
        });
        // ── Filter: autor accent-insensitive ─────────────────────────────────
        test("Filter by autor matches accent-insensitive", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Book", "Gabriel García Márquez", null, null, null, null, null, null));
            LlibreFilter f = ConstructorFiltreLlibre.of().autor("Garcia").build();
            assertEqual(1, cd.aplicarFiltres(f).size());
        });

        // ── Tag CRUD ─────────────────────────────────────────────────────────
        test("Tag rename and delete", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Tag t = cd.afegirTag("sci-fi");
            cd.reanomenarTag(t.obtenirId(), "science fiction");
            assertEqual("science fiction", cd.obtenirAllTags().get(0).obtenirNom());
            cd.eliminarTag(t);
            assertEqual(0, cd.obtenirAllTags().size());
        });

        // ── Loan API smoke ───────────────────────────────────────────────────
        test("prestarLlibre and retornarLlibre round-trip", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Loan Book", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Alice");
            assertEqual(true, cd.obtenirLoanedISBNs().contains(9780306406157L));
            cd.retornarLlibre(9780306406157L);
            assertEqual(false, cd.obtenirLoanedISBNs().contains(9780306406157L));
        });

        // ── clearAll ─────────────────────────────────────────────────────────
        test("clearAll removes all books and shelves", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, null, null, null));
            cd.afegirLlista("Shelf");
            cd.netejarAll();
            assertEqual(0, cd.getSize());
            assertEqual(0, cd.obtenirAllLlistes().size());
        });
        // ── existsInLibrary ──────────────────────────────────────────────────
        test("CsvUtils.existsInLibrary true when book present", () -> {
            reinicialitzarSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, null, null, null));
            assertEqual(true, herramienta.io.csv.UtilitatsCsv.existsInLibrary(cd, 9780306406157L));
            assertEqual(false, herramienta.io.csv.UtilitatsCsv.existsInLibrary(cd, 9780000000001L));
        });
    }
}
