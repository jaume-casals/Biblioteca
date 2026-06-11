import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import domini.LlibreFilterBuilder;
import herramienta.FiltreUtils;
import herramienta.LlibreValidator;
import herramienta.BookExporter;
import herramienta.BookImporter;
import persistencia.ControladorPersistencia;

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
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Nom", null, null, null, null, null, null, null);
            assertEqual(9780306406157L, l.getISBN());
        });
        test("ISBN-10 valid accepted", () -> {
            Llibre l = LlibreValidator.checkLlibre(8420413739L, "Test", null, null, null, null, null, null, null);
            assertEqual(8420413739L, l.getISBN());
        });
        test("Invalid ISBN (14 digits) rejected", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(12345678901234L, "X", null, null, null, null, null, null, null));
        });
        test("Invalid ISBN (null) rejected", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(null, "X", null, null, null, null, null, null, null));
        });
        test("Blank nom rejected", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(9780306406157L, "  ", null, null, null, null, null, null, null));
        });
        test("Null nom rejected", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(9780306406157L, null, null, null, null, null, null, null, null));
        });
        test("Valoracio > 10 rejected", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, 11.0, null, null, null));
        });
        test("Valoracio < 0 rejected", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, -1.0, null, null, null));
        });
        test("Optional fields get defaults", () -> {
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
            assertEqual("", l.getAutor());
            assertEqual(0, l.getAny());
            assertEqual(0.0, l.getValoracio());
            assertEqual(0.0, l.getPreu());
            assertEqual(false, l.getLlegit());
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
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.getSize());
            assertEqual(0, cd.getAllLlibres().size());
        });

        test("Add book and retrieve it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "El Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, "");
            cd.addLlibre(l);
            assertEqual(1, cd.getSize());
            Llibre retrieved = cd.getLlibre(9780306406157L);
            assertEqual("El Quixot", retrieved.getNom());
            assertEqual("Cervantes", retrieved.getAutor());
            assertEqual(1605, retrieved.getAny());
        });

        test("Duplicate ISBN insert throws", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Llibre A", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            assertThrows(() -> cd.addLlibre(
                LlibreValidator.checkLlibre(9780306406157L, "Llibre B", null, null, null, null, null, null, null)));
        });

        test("Delete non-existent book throws", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertThrows(() -> cd.deleteLlibre(9780000000000L));
        });

        test("Delete existing book removes it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            assertEqual(1, cd.getSize());
            cd.deleteLlibre(l);
            assertEqual(0, cd.getSize());
        });

        test("Filter with all params set", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "El Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "Hamlet", "Shakespeare", 1603, "", 8.5, 10.0, false, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780064400558L, "El Senyor dels Anells", "Tolkien", 1954, "", 10.0, 25.0, true, ""));

            LlibreFilter f = LlibreFilterBuilder.of()
                .autor("Cervantes").anyMin(1600).anyMax(1700)
                .valoracioMin(8.0).valoracioMax(10.0)
                .preuMin(10.0).preuMax(15.0).llegit(true).build();
            List<Llibre> r = cd.aplicarFiltres(f);
            assertEqual(1, r.size());
            assertEqual("El Quixot", r.get(0).getNom());
        });

        test("Filter llegit=false returns only unread", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Llegit", null, null, null, null, null, true, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "No llegit", null, null, null, null, null, false, ""));
            LlibreFilter f2 = LlibreFilterBuilder.of().llegit(false).build();
            List<Llibre> r = cd.aplicarFiltres(f2);
            assertEqual(1, r.size());
            assertEqual("No llegit", r.get(0).getNom());
        });

        // ── Llista (shelf) tests ──────────────────────────────────────────────
        test("Create llista and retrieve it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista l = cd.addLlista("Favorits");
            assertEqual("Favorits", l.getNom());
            assertEqual(1, cd.getAllLlistes().size());
        });

        test("Delete llista removes it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista l = cd.addLlista("Temporal");
            assertEqual(1, cd.getAllLlistes().size());
            cd.deleteLlista(l);
            assertEqual(0, cd.getAllLlistes().size());
        });

        test("Add book to llista and retrieve it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.addLlista("Lectura");
            cd.addLlibreToLlista(9780306406157L, llista.getId(), 7.5, true);
            List<Llibre> llibres = cd.getLlibresInLlista(llista.getId());
            assertEqual(1, llibres.size());
            assertEqual(9780306406157L, llibres.get(0).getISBN());
            assertEqual(7.5, llibres.get(0).getValoracio());
            assertEqual(true, llibres.get(0).getLlegit());
        });

        test("Remove book from llista", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.addLlista("Lectura");
            cd.addLlibreToLlista(9780306406157L, llista.getId(), 5.0, false);
            assertEqual(1, cd.getLlibresInLlista(llista.getId()).size());
            cd.removeLlibreFromLlista(9780306406157L, llista.getId());
            assertEqual(0, cd.getLlibresInLlista(llista.getId()).size());
        });

        test("getLlistesForLlibre returns per-book shelf values", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.addLlista("Favorits");
            cd.addLlibreToLlista(9780306406157L, llista.getId(), 8.0, true);
            List<Llista> llistes = cd.getLlistesForLlibre(9780306406157L);
            assertEqual(1, llistes.size());
            assertEqual("Favorits", llistes.get(0).getNom());
            assertEqual(8.0, llistes.get(0).getValoracioLlibre());
            assertEqual(true, llistes.get(0).getLlegitLlibre());
        });

        test("Update per-book shelf values", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.addLlista("Lectura");
            cd.addLlibreToLlista(9780306406157L, llista.getId(), 5.0, false);
            cd.updateLlibreInLlista(9780306406157L, llista.getId(), 9.0, true);
            List<Llista> llistes = cd.getLlistesForLlibre(9780306406157L);
            assertEqual(9.0, llistes.get(0).getValoracioLlibre());
            assertEqual(true, llistes.get(0).getLlegitLlibre());
        });

        test("Book deletion cascades to llista membership", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.addLlista("Lectura");
            cd.addLlibreToLlista(9780306406157L, llista.getId(), 5.0, false);
            cd.deleteLlibre(9780306406157L);
            assertEqual(0, cd.getLlibresInLlista(llista.getId()).size());
        });

        // ── LlibreValidator: preu ────────────────────────────────────────────
        test("Preu negative rejected", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, -0.01, null, null));
        });
        test("Preu zero accepted", () -> {
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, 0.0, null, null);
            assertEqual(0.0, l.getPreu());
        });

        // ── Llista: shelf valoracio vs global valoracio ───────────────────────
        test("getLlibresInLlista returns shelf valoracio, not global", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, 3.0, null, null, null));
            Llista llista = cd.addLlista("Shelf");
            cd.addLlibreToLlista(9780306406157L, llista.getId(), 9.0, true);
            List<Llibre> llibres = cd.getLlibresInLlista(llista.getId());
            assertEqual(9.0, llibres.get(0).getValoracio());
        });

        // ── Llista: cascade delete ───────────────────────────────────────────
        test("deleteLlista cascades: book survives, membership gone", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista llista = cd.addLlista("TempShelf");
            cd.addLlibreToLlista(9780306406157L, llista.getId(), 5.0, false);
            cd.deleteLlista(llista);
            assertEqual(1, cd.getSize());
            assertEqual(0, cd.getLlibresInLlista(llista.getId()).size());
        });

        // ── get10Llibres ─────────────────────────────────────────────────────
        test("get10Llibres returns exactly 10 when 15 books present", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 15; i++)
                cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "Llibre " + i, null, null, null, null, null, null, null));
            assertEqual(10, cd.get10Llibres().size());
        });
        test("get10Llibres returns all books when fewer than 10", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 5; i++)
                cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "Llibre " + i, null, null, null, null, null, null, null));
            assertEqual(5, cd.get10Llibres().size());
        });

        // ── Pagination ───────────────────────────────────────────────────────
        test("Pagination: 0 books", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.get100Llibres(0).size());
            assertEqual(0, cd.maxIndex100Llibres());
        });
        test("Pagination: exactly 100 books", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 100; i++)
                cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "L" + i, null, null, null, null, null, null, null));
            assertEqual(100, cd.get100Llibres(0).size());
            assertEqual(0, cd.get100Llibres(1).size());
            assertEqual(0, cd.maxIndex100Llibres());
        });
        test("Pagination: 101 books", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 101; i++)
                cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "L" + i, null, null, null, null, null, null, null));
            assertEqual(100, cd.get100Llibres(0).size());
            assertEqual(1, cd.get100Llibres(1).size());
            assertEqual(1, cd.maxIndex100Llibres());
        });

        // ── backupToSQL + restoreFromSQL round-trip ──────────────────────────
        test("backupToSQL + restoreFromSQL round-trip", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "El Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "Hamlet", "Shakespeare", 1603, "", 8.5, 10.0, false, ""));
            java.io.File tmp = java.io.File.createTempFile("biblioteca_test", ".sql");
            tmp.deleteOnExit();
            cd.backupToSQL(tmp);
            resetSingletons();
            cd = ControladorDomini.getInstance();
            cd.restoreFromSQL(tmp);
            assertEqual(2, cd.getSize());
            Llibre q = cd.getLlibre(9780306406157L);
            assertEqual("El Quixot", q.getNom());
            assertEqual("Cervantes", q.getAutor());
            assertEqual(9.0, q.getValoracio());
        });

        test("backupToSQL + restoreFromSQL preserves llista memberships", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Quixot", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "Hamlet", null, null, null, null, null, null, null));
            Llista shelf = cd.addLlista("Favorits");
            cd.addLlibreToLlista(9780306406157L, shelf.getId(), 8.5, true);
            java.io.File tmp = java.io.File.createTempFile("biblioteca_llista_test", ".sql");
            tmp.deleteOnExit();
            cd.backupToSQL(tmp);
            resetSingletons();
            cd = ControladorDomini.getInstance();
            cd.restoreFromSQL(tmp);
            assertEqual(2, cd.getSize());
            assertEqual(1, cd.getAllLlistes().size());
            assertEqual("Favorits", cd.getAllLlistes().get(0).getNom());
            List<Llibre> inShelf = cd.getLlibresInLlista(cd.getAllLlistes().get(0).getId());
            assertEqual(1, inShelf.size());
            assertEqual(9780306406157L, inShelf.get(0).getISBN());
            assertEqual(8.5, inShelf.get(0).getValoracio());
        });

        // ── Edit path: partial failure leaves library consistent ─────────────
        test("Edit path: delete A then add A-with-B-ISBN throws, only B remains", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
            cd.deleteLlibre(9780306406157L);
            assertEqual(1, cd.getSize());
            assertThrows(() -> cd.addLlibre(
                LlibreValidator.checkLlibre(9780743273565L, "A-edited", null, null, null, null, null, null, null)));
            assertEqual(1, cd.getSize());
            assertEqual("B", cd.getLlibre(9780743273565L).getNom());
        });

        // ── Tag tests ────────────────────────────────────────────────────────
        test("Create tag and retrieve it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Tag t = cd.addTag("Fantasia");
            assertEqual("Fantasia", t.getNom());
            assertEqual(1, cd.getAllTags().size());
        });

        test("Delete tag removes it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Tag t = cd.addTag("Temporal");
            assertEqual(1, cd.getAllTags().size());
            cd.deleteTag(t);
            assertEqual(0, cd.getAllTags().size());
        });

        test("Add book to tag and retrieve via getTagsForLlibre", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.addTag("Ciencia Ficcio");
            cd.addLlibreToTag(9780306406157L, t.getId());
            List<Tag> tags = cd.getTagsForLlibre(9780306406157L);
            assertEqual(1, tags.size());
            assertEqual("Ciencia Ficcio", tags.get(0).getNom());
        });

        test("Remove book from tag", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.addTag("Aventura");
            cd.addLlibreToTag(9780306406157L, t.getId());
            assertEqual(1, cd.getTagsForLlibre(9780306406157L).size());
            cd.removeLlibreFromTag(9780306406157L, t.getId());
            assertEqual(0, cd.getTagsForLlibre(9780306406157L).size());
        });

        test("Book deletion cascades to tag membership", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.addTag("Historia");
            cd.addLlibreToTag(9780306406157L, t.getId());
            cd.deleteLlibre(9780306406157L);
            assertEqual(0, cd.getTagsForLlibre(9780306406157L).size());
            assertEqual(1, cd.getAllTags().size());
        });

        test("deleteTag cascades: book survives, membership gone", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.addTag("Romàntic");
            cd.addLlibreToTag(9780306406157L, t.getId());
            cd.deleteTag(t);
            assertEqual(1, cd.getSize());
            assertEqual(0, cd.getAllTags().size());
            assertEqual(0, cd.getTagsForLlibre(9780306406157L).size());
        });

        test("Tag filter returns only books with that tag", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Quixot", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "Hamlet", null, null, null, null, null, null, null));
            Tag t = cd.addTag("Classics");
            cd.addLlibreToTag(9780306406157L, t.getId());
            LlibreFilter tf = LlibreFilterBuilder.of().tagId(t.getId()).build();
            List<Llibre> result = cd.aplicarFiltres(tf);
            assertEqual(1, result.size());
            assertEqual(9780306406157L, result.get(0).getISBN());
        });

        test("backupToSQL + restoreFromSQL preserves tag memberships", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Quixot", null, null, null, null, null, null, null));
            Tag t = cd.addTag("Classics");
            cd.addLlibreToTag(9780306406157L, t.getId());
            java.io.File tmp = java.io.File.createTempFile("biblioteca_tag_test", ".sql");
            tmp.deleteOnExit();
            cd.backupToSQL(tmp);
            resetSingletons();
            cd = ControladorDomini.getInstance();
            cd.restoreFromSQL(tmp);
            assertEqual(1, cd.getSize());
            assertEqual(1, cd.getAllTags().size());
            assertEqual("Classics", cd.getAllTags().get(0).getNom());
            List<Tag> tags = cd.getTagsForLlibre(9780306406157L);
            assertEqual(1, tags.size());
            assertEqual("Classics", tags.get(0).getNom());
        });

        // ── Date fields (data_compra / data_lectura) ─────────────────────────
        test("date fields set and retrieve in-memory", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Dates Book", null, null, null, null, null, null, null);
            l.setDataCompra("2024-01-15");
            l.setDataLectura("2024-03-20");
            cd.addLlibre(l);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual("2024-01-15", loaded.getDataCompra());
            assertEqual("2024-03-20", loaded.getDataLectura());
        });

        test("date fields null by default", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "No Dates Book", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            assertEqual(null, cd.getLlibre(9780306406157L).getDataCompra());
            assertEqual(null, cd.getLlibre(9780306406157L).getDataLectura());
        });

        test("date fields backup and restore", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Backup Dates", null, null, null, null, null, null, null);
            l.setDataCompra("2023-06-01");
            l.setDataLectura("2023-08-15");
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_dates_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual("2023-06-01", loaded.getDataCompra());
            assertEqual("2023-08-15", loaded.getDataLectura());
        });

        // ── Idioma field ──────────────────────────────────────────────────────
        test("idioma field set and retrieve", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Idioma Book", null, null, null, null, null, null, null);
            l.setIdioma("Català");
            cd.addLlibre(l);
            assertEqual("Català", cd.getLlibre(9780306406157L).getIdioma());
        });

        test("idioma null by default", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "No Lang", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            assertEqual(null, cd.getLlibre(9780306406157L).getIdioma());
        });

        test("idioma backup and restore", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Lang Book", null, null, null, null, null, null, null);
            l.setIdioma("English");
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_idioma_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual("English", cd.getLlibre(9780306406157L).getIdioma());
        });

        // ── Format field ─────────────────────────────────────────────────────
        test("format field set and retrieve", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Format Book", null, null, null, null, null, null, null);
            l.setFormat("eBook");
            cd.addLlibre(l);
            assertEqual("eBook", cd.getLlibre(9780306406157L).getFormat());
        });

        test("format null by default", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "No Format", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            assertEqual(null, cd.getLlibre(9780306406157L).getFormat());
        });

        test("format backup and restore", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Tapa dura Book", null, null, null, null, null, null, null);
            l.setFormat("Tapa dura");
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_format_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual("Tapa dura", cd.getLlibre(9780306406157L).getFormat());
        });

        test("aplicarFiltres on shelf list excludes books from other shelves", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "Shelf Book A", "Author X", null, null, null, null, null, null);
            Llibre l2 = LlibreValidator.checkLlibre(9780306406158L, "Shelf Book B", "Author X", null, null, null, null, null, null);
            Llibre l3 = LlibreValidator.checkLlibre(9780306406159L, "Other Book",  "Author X", null, null, null, null, null, null);
            cd.addLlibre(l1); cd.addLlibre(l2); cd.addLlibre(l3);
            Llista shelf = cd.addLlista("TestShelf");
            cd.addLlibreToLlista(l1.getISBN(), shelf.getId(), 0.0, false);
            cd.addLlibreToLlista(l2.getISBN(), shelf.getId(), 0.0, false);
            List<Llibre> shelfBooks = cd.getLlibresInLlista(shelf.getId());
            LlibreFilter sf = LlibreFilterBuilder.of().autor("Author X").build();
            List<Llibre> results = cd.aplicarFiltres(shelfBooks, sf);
            assertEqual(2, results.size());
        });

        test("aplicarFiltres on shelf respects shelf llegit value", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "Book A", null, null, null, null, null, false, null);
            Llibre l2 = LlibreValidator.checkLlibre(9780306406158L, "Book B", null, null, null, null, null, false, null);
            cd.addLlibre(l1); cd.addLlibre(l2);
            Llista shelf = cd.addLlista("TestShelf2");
            cd.addLlibreToLlista(l1.getISBN(), shelf.getId(), 0.0, true);  // llegit=true on shelf
            cd.addLlibreToLlista(l2.getISBN(), shelf.getId(), 0.0, false); // llegit=false on shelf
            List<Llibre> shelfBooks = cd.getLlibresInLlista(shelf.getId());
            LlibreFilter lf = LlibreFilterBuilder.of().llegit(true).build();
            List<Llibre> llegits = cd.aplicarFiltres(shelfBooks, lf);
            assertEqual(1, llegits.size());
            assertEqual("Book A", llegits.get(0).getNom());
        });

        test("duplicate ISBN on edit: original book preserved", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "Book 1", null, null, null, null, null, null, null);
            Llibre l2 = LlibreValidator.checkLlibre(9780306406158L, "Book 2", null, null, null, null, null, null, null);
            cd.addLlibre(l1);
            cd.addLlibre(l2);
            Llibre edited = LlibreValidator.checkLlibre(9780306406158L, "Book 1 edited", null, null, null, null, null, null, null);
            boolean threw = false;
            try {
                if (edited.getISBN() != l1.getISBN() && cd.existsLlibre(edited.getISBN()))
                    throw new Exception("duplicate");
                cd.deleteLlibre(l1);
                cd.addLlibre(edited);
            } catch (Exception ignored) { threw = true; }
            assertEqual(true, threw);
            assertEqual("Book 1", cd.getLlibre(9780306406157L).getNom());
            assertEqual("Book 2", cd.getLlibre(9780306406158L).getNom());
        });

        test("multiple authors set and retrieve", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Multi Author", null, null, null, null, null, null, null);
            l.setAutors(java.util.Arrays.asList("Author A", "Author B"));
            cd.addLlibre(l);
            assertEqual(java.util.Arrays.asList("Author A", "Author B"), cd.getLlibre(9780306406157L).getAutors());
        });

        test("autors empty list by default", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "No Autors", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            assertEqual(true, cd.getLlibre(9780306406157L).getAutors().isEmpty());
        });

        test("autors backup and restore", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Backup Autors", null, null, null, null, null, null, null);
            l.setAutors(java.util.Arrays.asList("Author X", "Author Y"));
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_autors_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual(java.util.Arrays.asList("Author X", "Author Y"), cd.getLlibre(9780306406157L).getAutors());
        });

        test("desitjat field set and retrieve", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Desitjat Book", null, null, null, null, null, null, null);
            l.setDesitjat(true);
            cd.addLlibre(l);
            assertEqual(true, cd.getLlibre(9780306406157L).isDesitjat());
        });

        test("desitjat false by default", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "No Desitjat", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            assertEqual(false, cd.getLlibre(9780306406157L).isDesitjat());
        });

        test("desitjat backup and restore", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Desitjat Restore", null, null, null, null, null, null, null);
            l.setDesitjat(true);
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_desitjat_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual(true, cd.getLlibre(9780306406157L).isDesitjat());
        });

        // ── OpenLibraryClient error contract ─────────────────────────────────
        test("OpenLibraryClient.lookupByISBN returns non-null map with error key on network error", () -> {
            herramienta.OpenLibraryClient.testBaseUrl = "http://localhost:1";
            try {
                java.util.Map<String, String> result = herramienta.OpenLibraryClient.lookupByISBN("9780306406157");
                assertNotNull(result);
                assertEqual(true, result.containsKey("error"));
            } finally {
                herramienta.OpenLibraryClient.testBaseUrl = null;
            }
        });
        test("OpenLibraryClient.lookupByTitle returns non-null map with error key on network error", () -> {
            herramienta.OpenLibraryClient.testBaseUrl = "http://localhost:1";
            try {
                java.util.Map<String, String> result = herramienta.OpenLibraryClient.lookupByTitle("Test Title");
                assertNotNull(result);
                assertEqual(true, result.containsKey("error"));
            } finally {
                herramienta.OpenLibraryClient.testBaseUrl = null;
            }
        });

        test("OpenLibraryClient retries on network error and returns error map", () -> {
            herramienta.OpenLibraryClient.testBaseUrl  = "http://localhost:1";
            herramienta.OpenLibraryClient.testMaxRetries  = 2;
            herramienta.OpenLibraryClient.testRetryBaseMs = 0;
            try {
                long t0 = System.currentTimeMillis();
                java.util.Map<String, String> r = herramienta.OpenLibraryClient.lookupByISBN("1234567890");
                long elapsed = System.currentTimeMillis() - t0;
                assertNotNull(r);
                assertEqual(true, r.containsKey("error"));
                // 2 retries with 0ms base → should complete fast (< 2s)
                assertEqual(true, elapsed < 2000);
            } finally {
                herramienta.OpenLibraryClient.testBaseUrl    = null;
                herramienta.OpenLibraryClient.testMaxRetries  = -1;
                herramienta.OpenLibraryClient.testRetryBaseMs = -1;
            }
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

            // Now let ServerConect run migrations on that DB — pais_origen (v23) must be applied
            System.setProperty("biblioteca.test", "true");
            System.setProperty("biblioteca.h2.url", url);
            persistencia.ServerConect sc = new persistencia.ServerConect();
            sc.createDatabase();
            // If pais_origen column now exists, SELECT on it succeeds
            java.sql.ResultSet rs2 = sc.getConnection().createStatement().executeQuery(
                "SELECT pais_origen FROM llibre");
            rs2.close();
            sc.getConnection().close();
            System.setProperty("biblioteca.h2.url", "jdbc:h2:mem:test;MODE=MySQL;NON_KEYWORDS=VALUE");
        });

        // ── LlibreValidator: boundary values ─────────────────────────────────
        test("Valoracio = 0.0 accepted", () -> {
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, 0.0, null, null, null);
            assertEqual(0.0, l.getValoracio());
        });
        test("Valoracio = 10.0 accepted", () -> {
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, 10.0, null, null, null);
            assertEqual(10.0, l.getValoracio());
        });
        test("Preu very large accepted", () -> {
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, 999999.99, null, null);
            assertEqual(999999.99, l.getPreu());
        });
        test("ISBN-10 digit boundary (9 digits rejected)", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(123456789L, "X", null, null, null, null, null, null, null));
        });
        test("ISBN-10 digit boundary (11 digits rejected)", () -> {
            assertThrows(() -> LlibreValidator.checkLlibre(12345678901L, "X", null, null, null, null, null, null, null));
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
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "Hamlet", null, null, null, null, null, null, null));
            LlibreFilter fq = LlibreFilterBuilder.of().nom("quixot").build();
            List<Llibre> r = cd.aplicarFiltres(fq);
            assertEqual(1, r.size());
            assertEqual("El Quixot", r.get(0).getNom());
        });
        test("Filter by ISBN prefix", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
            LlibreFilter fi = LlibreFilterBuilder.of().isbn(97803064L).build();
            List<Llibre> r = cd.aplicarFiltres(fi);
            assertEqual(1, r.size());
            assertEqual(9780306406157L, r.get(0).getISBN());
        });
        test("Filter by iniciAny only", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Old", "X", 1900, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "New", "X", 2000, null, null, null, null, null));
            LlibreFilter fa = LlibreFilterBuilder.of().anyMin(1950).build();
            List<Llibre> r = cd.aplicarFiltres(fa);
            assertEqual(1, r.size());
            assertEqual(9780743273565L, r.get(0).getISBN());
        });
        test("Filter by fiAny only", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Old", "X", 1900, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "New", "X", 2000, null, null, null, null, null));
            LlibreFilter fb = LlibreFilterBuilder.of().anyMax(1950).build();
            List<Llibre> r = cd.aplicarFiltres(fb);
            assertEqual(1, r.size());
            assertEqual(9780306406157L, r.get(0).getISBN());
        });
        test("Filter by valoracio range", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Low", null, null, null, 3.0, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "High", null, null, null, 8.0, null, null, null));
            LlibreFilter fv = LlibreFilterBuilder.of().valoracioMin(7.0).valoracioMax(10.0).build();
            List<Llibre> r = cd.aplicarFiltres(fv);
            assertEqual(1, r.size());
            assertEqual("High", r.get(0).getNom());
        });
        test("Filter by preu range", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Cheap", null, null, null, null, 5.0, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "Expensive", null, null, null, null, 50.0, null, null));
            LlibreFilter fp = LlibreFilterBuilder.of().preuMin(30.0).preuMax(60.0).build();
            List<Llibre> r = cd.aplicarFiltres(fp);
            assertEqual(1, r.size());
            assertEqual("Expensive", r.get(0).getNom());
        });
        test("Filter returns empty when no books match", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", "Author", 2000, null, null, null, null, null));
            LlibreFilter fn = LlibreFilterBuilder.of().nom("NonExistent").build();
            List<Llibre> r = cd.aplicarFiltres(fn);
            assertEqual(0, r.size());
        });

        // ── Filter: editorial, serie, format, idioma ──────────────────────────
        test("Filter by editorial", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "Book A", null, null, null, null, null, null, null);
            l1.setEditorial("Planeta");
            Llibre l2 = LlibreValidator.checkLlibre(9780743273565L, "Book B", null, null, null, null, null, null, null);
            l2.setEditorial("Anagrama");
            cd.addLlibre(l1); cd.addLlibre(l2);
            LlibreFilter fe = LlibreFilterBuilder.of().editorial("Planeta").build();
            List<Llibre> r = cd.aplicarFiltres(fe);
            assertEqual(1, r.size());
            assertEqual("Book A", r.get(0).getNom());
        });
        test("Filter by serie", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "Book A", null, null, null, null, null, null, null);
            l1.setSerie("Chronicles");
            Llibre l2 = LlibreValidator.checkLlibre(9780743273565L, "Book B", null, null, null, null, null, null, null);
            l2.setSerie("Saga");
            cd.addLlibre(l1); cd.addLlibre(l2);
            LlibreFilter fs = LlibreFilterBuilder.of().serie("chronicles").build();
            List<Llibre> r = cd.aplicarFiltres(fs);
            assertEqual(1, r.size());
            assertEqual("Book A", r.get(0).getNom());
        });
        test("Filter by format exact match", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "Ebook", null, null, null, null, null, null, null);
            l1.setFormat("eBook");
            Llibre l2 = LlibreValidator.checkLlibre(9780743273565L, "Paper", null, null, null, null, null, null, null);
            l2.setFormat("Tapa dura");
            cd.addLlibre(l1); cd.addLlibre(l2);
            LlibreFilter ff = LlibreFilterBuilder.of().format("ebook").build();
            List<Llibre> r = cd.aplicarFiltres(ff);
            assertEqual(1, r.size());
            assertEqual("Ebook", r.get(0).getNom());
        });
        test("Filter by idioma", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "Cat Book", null, null, null, null, null, null, null);
            l1.setIdioma("Català");
            Llibre l2 = LlibreValidator.checkLlibre(9780743273565L, "Eng Book", null, null, null, null, null, null, null);
            l2.setIdioma("English");
            cd.addLlibre(l1); cd.addLlibre(l2);
            LlibreFilter fi = LlibreFilterBuilder.of().idioma("català").build();
            List<Llibre> r = cd.aplicarFiltres(fi);
            assertEqual(1, r.size());
            assertEqual("Cat Book", r.get(0).getNom());
        });

        // ── updateLlibre ──────────────────────────────────────────────────────
        test("updateLlibre persists nom and valoracio", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Original", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            l.setNom("Updated");
            l.setValoracio(8.5);
            cd.updateLlibre(l);
            assertEqual("Updated", cd.getLlibre(9780306406157L).getNom());
            assertEqual(8.5, cd.getLlibre(9780306406157L).getValoracio());
        });
        test("updateLlibre persists after reload", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Original", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            l.setNom("Persisted");
            cd.updateLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_update_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual("Persisted", cd.getLlibre(9780306406157L).getNom());
        });

        // ── existsLlibre ──────────────────────────────────────────────────────
        test("existsLlibre returns true for existing book", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            assertEqual(true, cd.existsLlibre(9780306406157L));
        });
        test("existsLlibre returns false for non-existing book", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(false, cd.existsLlibre(9780999999999L));
        });

        // ── getCountInLlista ──────────────────────────────────────────────────
        test("getCountInLlista returns correct count", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
            Llista shelf = cd.addLlista("Test");
            cd.addLlibreToLlista(9780306406157L, shelf.getId(), 0, false);
            cd.addLlibreToLlista(9780743273565L, shelf.getId(), 0, false);
            assertEqual(2, cd.getCountInLlista(shelf.getId()));
        });
        test("getCountInLlista returns 0 for empty shelf", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista shelf = cd.addLlista("Empty");
            assertEqual(0, cd.getCountInLlista(shelf.getId()));
        });

        // ── One book in multiple shelves ──────────────────────────────────────
        test("one book added to two shelves, both contain it", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista s1 = cd.addLlista("Shelf1");
            Llista s2 = cd.addLlista("Shelf2");
            cd.addLlibreToLlista(9780306406157L, s1.getId(), 5.0, false);
            cd.addLlibreToLlista(9780306406157L, s2.getId(), 7.0, true);
            assertEqual(1, cd.getLlibresInLlista(s1.getId()).size());
            assertEqual(1, cd.getLlibresInLlista(s2.getId()).size());
            assertEqual(2, cd.getLlistesForLlibre(9780306406157L).size());
        });

        // ── Shelf reorder ─────────────────────────────────────────────────────
        test("moveLlistaUp swaps shelf order", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s1 = cd.addLlista("First");
            Llista s2 = cd.addLlista("Second");
            cd.moveLlistaUp(s2.getId());
            assertEqual("Second", cd.getAllLlistes().get(0).getNom());
            assertEqual("First",  cd.getAllLlistes().get(1).getNom());
        });
        test("moveLlistaDown swaps shelf order", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s1 = cd.addLlista("First");
            Llista s2 = cd.addLlista("Second");
            cd.moveLlistaDown(s1.getId());
            assertEqual("Second", cd.getAllLlistes().get(0).getNom());
            assertEqual("First",  cd.getAllLlistes().get(1).getNom());
        });
        test("moveLlistaUp at position 0 is no-op", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s1 = cd.addLlista("First");
            cd.addLlista("Second");
            cd.moveLlistaUp(s1.getId());
            assertEqual("First", cd.getAllLlistes().get(0).getNom());
        });
        test("moveLlistaDown at last position is no-op", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlista("First");
            Llista s2 = cd.addLlista("Second");
            cd.moveLlistaDown(s2.getId());
            assertEqual("Second", cd.getAllLlistes().get(1).getNom());
        });

        // ── setLlistaColor ────────────────────────────────────────────────────
        test("setLlistaColor persists color", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s = cd.addLlista("Colorful");
            cd.setLlistaColor(s.getId(), "#FF0000");
            assertEqual("#FF0000", cd.getAllLlistes().get(0).getColor());
        });
        test("setLlistaColor null clears color", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llista s = cd.addLlista("Colorful");
            cd.setLlistaColor(s.getId(), "#FF0000");
            cd.setLlistaColor(s.getId(), null);
            assertEqual(null, cd.getAllLlistes().get(0).getColor());
        });

        // ── Loans (prestec) ───────────────────────────────────────────────────
        test("prestarLlibre adds isbn to getLoanedISBNs", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Joan");
            assertEqual(true, cd.getLoanedISBNs().contains(9780306406157L));
        });
        test("retornarLlibre removes isbn from getLoanedISBNs", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Joan");
            cd.retornarLlibre(9780306406157L);
            assertEqual(false, cd.getLoanedISBNs().contains(9780306406157L));
        });
        test("getLoanedISBNs empty when no loans", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            assertEqual(0, cd.getLoanedISBNs().size());
        });
        test("backup and restore preserves prestec loans", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Loaned", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Pere");
            java.io.File f = java.io.File.createTempFile("test_prestec_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual(true, cd.getLoanedISBNs().contains(9780306406157L));
        });

        // ── getLlibreBlob ─────────────────────────────────────────────────────
        test("getLlibreBlob returns null when blob not set", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            assertEqual(null, cd.getLlibreBlob(9780306406157L));
        });

        // ── clearAll ──────────────────────────────────────────────────────────
        test("clearAll removes books, llistes and tags", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            cd.addLlista("Shelf");
            cd.addTag("Tag");
            cd.clearAll();
            assertEqual(0, cd.getSize());
            assertEqual(0, cd.getAllLlistes().size());
            assertEqual(0, cd.getAllTags().size());
        });

        // ── Extended fields ───────────────────────────────────────────────────
        test("notes field set and retrieve", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Notes Book", null, null, null, null, null, null, null);
            l.setNotes("My personal review");
            cd.addLlibre(l);
            assertEqual("My personal review", cd.getLlibre(9780306406157L).getNotes());
        });
        test("notes backup and restore with single quotes", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Notes Test", null, null, null, null, null, null, null);
            l.setNotes("O'Brien wrote this");
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_notes_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual("O'Brien wrote this", cd.getLlibre(9780306406157L).getNotes());
        });
        test("pais_origen field set and retrieve", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Country Book", null, null, null, null, null, null, null);
            l.setPaisOrigen("Espanya");
            cd.addLlibre(l);
            assertEqual("Espanya", cd.getLlibre(9780306406157L).getPaisOrigen());
        });
        test("editorial, serie and volum fields persist", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Series Book", null, null, null, null, null, null, null);
            l.setEditorial("Planeta");
            l.setSerie("Chronicles");
            l.setVolum(3);
            cd.addLlibre(l);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual("Planeta", loaded.getEditorial());
            assertEqual("Chronicles", loaded.getSerie());
            assertEqual(3, loaded.getVolum());
        });
        test("pagines and paginesLlegides persist", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Pages Book", null, null, null, null, null, null, null);
            l.setPagines(350);
            l.setPaginesLlegides(150);
            cd.addLlibre(l);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual(350, loaded.getPagines());
            assertEqual(150, loaded.getPaginesLlegides());
        });

        // ── getDistinctValues / getDistinctAutorNames ─────────────────────────
        test("getDistinctValues returns distinct editorial values", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l1 = LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null);
            l1.setEditorial("Planeta");
            Llibre l2 = LlibreValidator.checkLlibre(9780743273565L, "B", null, null, null, null, null, null, null);
            l2.setEditorial("Planeta");
            Llibre l3 = LlibreValidator.checkLlibre(9780064400558L, "C", null, null, null, null, null, null, null);
            l3.setEditorial("Anagrama");
            cd.addLlibre(l1); cd.addLlibre(l2); cd.addLlibre(l3);
            java.util.List<String> vals = cd.getDistinctValues("editorial");
            assertEqual(2, vals.size());
            assertEqual(true, vals.contains("Planeta"));
            assertEqual(true, vals.contains("Anagrama"));
        });
        test("getDistinctValues rejects unknown column (SQL injection guard)", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            java.util.List<String> vals = cd.getDistinctValues("; DROP TABLE llibre; --");
            assertEqual(0, vals.size());
        });
        test("getDistinctAutorNames returns names from autor table", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Multi", null, null, null, null, null, null, null);
            l.setAutors(java.util.Arrays.asList("Zelazny", "Asimov"));
            cd.addLlibre(l);
            java.util.List<String> names = cd.getDistinctAutorNames();
            assertEqual(2, names.size());
            assertEqual(true, names.contains("Zelazny"));
            assertEqual(true, names.contains("Asimov"));
        });
        test("getDistinctAutorNames empty when no authors", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.getDistinctAutorNames().size());
        });

        // ── Backup: SQL escaping ──────────────────────────────────────────────
        test("backup escapes single quote in book nom", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "L'home i el mar", null, null, null, null, null, null, null));
            java.io.File f = java.io.File.createTempFile("test_escape_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual("L'home i el mar", cd.getLlibre(9780306406157L).getNom());
        });
        test("backup escapes single quote in autor name", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Test", "O'Hara", null, null, null, null, null, null);
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_escape_autor_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            assertEqual("O'Hara", cd.getLlibre(9780306406157L).getAutor());
        });

        // ── Multiple tags ─────────────────────────────────────────────────────
        test("multiple tags for one book", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t1 = cd.addTag("SciFi");
            Tag t2 = cd.addTag("Classic");
            cd.addLlibreToTag(9780306406157L, t1.getId());
            cd.addLlibreToTag(9780306406157L, t2.getId());
            assertEqual(2, cd.getTagsForLlibre(9780306406157L).size());
        });
        test("tag filter returns empty when no books have that tag", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Tag t = cd.addTag("Unused");
            LlibreFilter ft = LlibreFilterBuilder.of().tagId(t.getId()).build();
            List<Llibre> r = cd.aplicarFiltres(ft);
            assertEqual(0, r.size());
        });

        // ── getRecentlyAdded ──────────────────────────────────────────────────
        test("getRecentlyAdded returns all books when fewer than limit", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 5; i++)
                cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "Book " + i, null, null, null, null, null, null, null));
            assertEqual(5, cd.getRecentlyAdded().size());
        });

        // ── Books sorted by ISBN in-memory ────────────────────────────────────
        test("books maintained in ascending ISBN order after multiple adds", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "Second", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "First", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780064400558L, "Third", null, null, null, null, null, null, null));
            List<Llibre> all = cd.getAllLlibres();
            assertEqual(true, all.get(0).getISBN().compareTo(all.get(1).getISBN()) < 0);
            assertEqual(true, all.get(1).getISBN().compareTo(all.get(2).getISBN()) < 0);
        });

        // ── getLlibre throws for missing book ─────────────────────────────────
        test("getLlibre throws for non-existent ISBN", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertThrows(() -> cd.getLlibre(9780000000001L));
        });

        // ── Goodreads CSV column mapping ──────────────────────────────────────
        test("Goodreads CSV import maps columns correctly (Title→nom, ISBN13→isbn, My Rating→valoracio, Exclusive Shelf→llegit)", () -> {
            resetSingletons();
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
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780000000020L, "Alpha Book", "Jones", 2021, "", 7.0, 10.0, false, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780000000021L, "Beta Book", "Smith", 2022, "", 5.0, 8.0, true, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780000000022L, "Gamma Book", "Jones", 2020, "", 9.0, 12.0, false, ""));
            LlibreFilter fj = LlibreFilterBuilder.of().autor("Jones").build();
            List<Llibre> sqlRes = cd.searchLlibresSQL(fj);
            List<Llibre> memRes = cd.aplicarFiltres(fj);
            assertEqual(sqlRes.size(), memRes.size());
            assertEqual(2, sqlRes.size()); // Jones has 2 books
        });

        // ── getLlibresPage edge cases ─────────────────────────────────────────
        test("getLlibresPage(offset, pageSize) returns correct slice and handles edge cases (offset > count, pageSize=0)", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 0; i < 5; i++)
                cd.addLlibre(LlibreValidator.checkLlibre(9780000000030L + i, "Book " + i, null, null, null, null, null, null, null));
            List<Llibre> page1 = cd.getLlibresPage(0, 3);
            assertEqual(3, page1.size());
            List<Llibre> page2 = cd.getLlibresPage(3, 3);
            assertEqual(2, page2.size());
            List<Llibre> overflow = cd.getLlibresPage(100, 3);
            assertEqual(0, overflow.size());
            // pageSize=0 means no pagination — returns all books
            List<Llibre> allBooks = cd.getLlibresPage(0, 0);
            assertEqual(5, allBooks.size());
        });

        // ── Batch cover fetch skips books with blob ───────────────────────────
        test("batch cover fetch skips books that already have blob (hasBlob=true)", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780000000040L, "WithBlob", null, null, null, null, null, null, null));
            cd.addLlibre(LlibreValidator.checkLlibre(9780000000041L, "WithoutBlob", null, null, null, null, null, null, null));
            // Set blob for first book
            byte[] dummyBlob = new byte[]{1, 2, 3};
            cd.setLlibreBlob(9780000000040L, dummyBlob);
            // Reload to get updated hasBlob state
            List<Llibre> all = cd.getAllLlibres();
            java.util.List<Llibre> needsCover = all.stream()
                .filter(l -> !l.hasBlob() && l.getImatgeBlob() == null)
                .collect(java.util.stream.Collectors.toList());
            // Only the book without blob should be in the fetch list
            assertEqual(1, needsCover.size());
            assertEqual(9780000000041L, needsCover.get(0).getISBN());
        });

        // ── EstadistiquesHelper golden snapshot ──────────────────────────────
        test("buildStatsSummary produces expected output for fixed library", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            books.add(LlibreValidator.checkLlibre(9780000000001L, "Quixot", "Cervantes", 1605, "", 9.0, 12.5, true, ""));
            books.get(0).setDataLectura("2024-03-15");
            books.add(LlibreValidator.checkLlibre(9780000000002L, "Hamlet", "Shakespeare", 1603, "", 8.5, 10.0, false, ""));
            books.add(LlibreValidator.checkLlibre(9780000000003L, "LotR", "Tolkien", 1954, "", 10.0, 25.0, true, ""));
            books.get(2).setDataLectura("2024-01-10");
            books.add(LlibreValidator.checkLlibre(9780000000004L, "Dune", "Herbert", 1965, "", 7.0, 15.0, true, ""));
            presentacio.EstadistiquesHelper.BookStats stats = presentacio.EstadistiquesHelper.computeStats(books);
            assertEqual(4, stats.total);
            assertEqual(3L, stats.llegits);
            double avgR = (9.0 + 8.5 + 10.0 + 7.0) / 4.0;
            assertEqual(String.format("%.2f", avgR), String.format("%.2f", stats.avgValoracio));
            double avgP = (12.5 + 10.0 + 25.0 + 15.0) / 4.0;
            assertEqual(String.format("%.2f", avgP), String.format("%.2f", stats.avgPreu));
            String summary = presentacio.EstadistiquesHelper.buildStatsSummary(stats, "TestScope");
            assertEqual(true, summary.startsWith("TestScope\n"));
            assertEqual(true, summary.contains("4"));
            assertEqual(true, summary.contains("3"));
        });

        test("BookStats booksByReadYear groups correctly", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            books.add(LlibreValidator.checkLlibre(9780000000001L, "A", null, null, null, null, null, true, ""));
            books.get(0).setDataLectura("2024-06-01");
            books.add(LlibreValidator.checkLlibre(9780000000002L, "B", null, null, null, null, null, true, ""));
            books.get(1).setDataLectura("2024-12-01");
            books.add(LlibreValidator.checkLlibre(9780000000003L, "C", null, null, null, null, null, false, ""));
            presentacio.EstadistiquesHelper.BookStats stats = presentacio.EstadistiquesHelper.computeStats(books);
            assertEqual(2L, stats.booksByReadYear.getOrDefault(2024, 0L).longValue());
            if (stats.booksByReadYear.containsKey(0)) {
                assertEqual(0L, stats.booksByReadYear.get(0).longValue());
            }
        });

        // ── PrestecRow toDisplayMap date roundtrip ─────────────────────────
        test("PrestecRow.toDisplayMap formats LocalDate as dd/MM/yyyy", () -> {
            java.time.LocalDate date = java.time.LocalDate.of(2025, 3, 14);
            persistencia.PrestecRow row = new persistencia.PrestecRow(9780306406157L, "Alice", date, false);
            java.util.Map<String, Object> m = row.toDisplayMap();
            assertEqual(9780306406157L, m.get("isbn"));
            assertEqual("Alice", m.get("persona"));
            assertEqual("14/03/2025", m.get("dataPrestec"));
            assertEqual(false, m.get("retornat"));
        });

        test("PrestecRow.toDisplayMap null date yields null", () -> {
            persistencia.PrestecRow row = new persistencia.PrestecRow(9780306406157L, "Bob", null, true);
            java.util.Map<String, Object> m = row.toDisplayMap();
            assertEqual(null, m.get("dataPrestec"));
        });

        test("PrestecRow.fromStrings parses ISO date", () -> {
            persistencia.PrestecRow row = persistencia.PrestecRow.fromStrings(9780306406157L, "Eve", "2024-01-15", true);
            assertEqual(java.time.LocalDate.of(2024, 1, 15), row.dataPrestec());
            assertEqual(true, row.retornat());
        });

        test("PrestecRow.fromStrings null/blank date yields null", () -> {
            persistencia.PrestecRow r1 = persistencia.PrestecRow.fromStrings(1L, "X", null, false);
            assertEqual(null, r1.dataPrestec());
            persistencia.PrestecRow r2 = persistencia.PrestecRow.fromStrings(1L, "X", "  ", false);
            assertEqual(null, r2.dataPrestec());
        });

        // ── NativeCsvStrategy canHandle ──────────────────────────────────────
        test("NativeCsvStrategy.canHandle accepts any header with ≥4 columns", () -> {
            herramienta.csv.NativeCsvStrategy ns = new herramienta.csv.NativeCsvStrategy();
            assertEqual(true, ns.canHandle("9780306406157,Nom,Autor,2020"));
            assertEqual(true, ns.canHandle("0306406152,Nom,Autor,2020"));
            assertEqual(true, ns.canHandle("random,header,with,four,columns"));
            assertEqual(false, ns.canHandle(""));
            assertEqual(false, ns.canHandle("a"));
        });

        // ── PrestecRow roundtrip: fromStrings → toDisplayMap ────────────────
        test("PrestecRow roundtrip: fromStrings produces displayable ISO date", () -> {
            persistencia.PrestecRow row = persistencia.PrestecRow.fromStrings(9780000000001L, "Carol", "2023-07-22", false);
            java.util.Map<String, Object> m = row.toDisplayMap();
            assertEqual("22/07/2023", m.get("dataPrestec"));
            assertEqual(9780000000001L, m.get("isbn"));
            assertEqual("Carol", m.get("persona"));
            assertEqual(false, m.get("retornat"));
        });

        // ── NativeCsvStrategy parseLine (roundtrip-style) ───────────────────
        test("NativeCsvStrategy parseLine imports basic book", () -> {
            herramienta.csv.NativeCsvStrategy ns = new herramienta.csv.NativeCsvStrategy();
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String[] cols = {"9780000000099", "Test Book", "Author", "2024", "desc", "8.0", "12.5", "true", "", ""};
            java.util.Map<String, Integer> hMap = new java.util.HashMap<>();
            boolean result = ns.parseLine(cols, hMap, cd);
            assertEqual(true, result);
            Llibre imported = cd.getLlibre(9780000000099L);
            assertEqual("Test Book", imported.getNom());
            assertEqual("Author", imported.getAutor());
            assertEqual(2024, imported.getAny());
        });

        test("NativeCsvStrategy parseLine rejects row with too few columns", () -> {
            herramienta.csv.NativeCsvStrategy ns = new herramienta.csv.NativeCsvStrategy();
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String[] cols = {"9780000000099", "Short"};
            java.util.Map<String, Integer> hMap = new java.util.HashMap<>();
            boolean threw = false;
            try { ns.parseLine(cols, hMap, cd); } catch (Exception e) { threw = true; }
            assertEqual(true, threw);
        });

        // ── Goodreads CSV fixture from real export ─────────────────────────────
        test("Goodreads CSV: realistic header row is recognised and rows are imported", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String header = "Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,Read Count";
            herramienta.csv.GoodreadsCsvStrategy gr = new herramienta.csv.GoodreadsCsvStrategy();
            assertEqual(true, gr.canHandle(header));
            String[] headerCols = herramienta.csv.CsvUtils.parseLine(header);
            java.util.Map<String, Integer> hMap = herramienta.csv.CsvUtils.buildHeaderMap(headerCols);
            String row = "42,The Hobbit,Tolkien,J.R.R. Tolkien,,=\"0000000000\",=\"9780000000042\",5,4.5,HarperCollins,Paperback,310,1937,1937,2024-06-15,2024-05-01,fantasy;classics,read,Awesome,,nope,3";
            String[] cols = herramienta.csv.CsvUtils.parseLine(row);
            boolean imported = gr.parseLine(cols, hMap, cd);
            assertEqual(true, imported);
            Llibre l = cd.getLlibre(9780000000042L);
            assertEqual("The Hobbit", l.getNom());
            assertEqual(10.0, l.getValoracio());
            assertEqual(true, l.getLlegit());
        });

        // ── LibraryThing CSV fixture: multiple authors, tags, X-check ISBN ────────
        test("LibraryThing CSV: BCID column triggers strategy; handles tags and X-check ISBN", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            String header = "Book Id,ISBN,ISBN13,BCID,Title,Authors,Original Publication Year,Publication Year,Rating,Summary,Comments,Review,Collections,Tags";
            herramienta.csv.LibraryThingCsvStrategy lt = new herramienta.csv.LibraryThingCsvStrategy();
            // LibraryThing strategy identifies itself via "BCID" column
            assertEqual(true, lt.canHandle(header));
            // Build header map the same way BookImporter does
            String[] headerCols = herramienta.csv.CsvUtils.parseLine(header);
            java.util.Map<String, Integer> hMap = herramienta.csv.CsvUtils.buildHeaderMap(headerCols);
// Row with: ISBN-13, multiple authors (semicolon in LibraryThing = same author), tags, and collections
            // Tags and Collections use commas and must be in a quoted field
            String row = "99,,9780000000019,BC123,Test LibBook,Author One; Author Two,2021,2021,3.5,A summary,My notes,,\"My Shelf,Favorites\",fiction;adventure";
            String[] cols = herramienta.csv.CsvUtils.parseLine(row);
            boolean imported;
            try {
                imported = lt.parseLine(cols, hMap, cd);
            } catch (Exception e) {
                throw new AssertionError("LibraryThing parseLine threw exception: " + e);
            }
            assertEqual(true, imported);
            Llibre l = cd.getLlibre(9780000000019L);
            assertEqual("Test LibBook", l.getNom());
            // Rating 3.5 * 2.0 = 7.0
            assertEqual(7.0, l.getValoracio());
            // Tags should have been created (fiction;adventure may be 1 or 2 tags depending on separator)
            assertEqual(true, cd.getAllTags().size() >= 1);
            // Collections/shelves should have been created (2 shelves in the quoted field)
            assertEqual(true, cd.getAllLlistes().size() >= 1);
        });

        // ── NativeCsvStrategy roundtrip: export → re-import ────────────────────
        test("NativeCsvStrategy roundtrip: exportCSV then re-import preserves book data", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780000000050L, "RoundBook", "Author X", 2020, "A description", 7.5, 19.99, true, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780000000051L, "SecondBook", "Author Y", 1999, "", 0.0, 0.0, false, ""));
            Llista shelf = cd.addLlista("Fav");
            cd.addLlibreToLlista(9780000000050L, shelf.getId(), 8.0, true);
            // Export
            java.io.File tmpExport = java.io.File.createTempFile("roundtrip_export", ".csv");
            tmpExport.deleteOnExit();
            java.util.List<Llibre> view = cd.getAllLlibres();
            herramienta.BookExporter.exportCSV(tmpExport, view, cd);
            // Re-import into fresh DB
            resetSingletons();
            ControladorDomini cd2 = ControladorDomini.getInstance();
            herramienta.BookImporter.ImportResult r = herramienta.BookImporter.importCSV(tmpExport, cd2);
            assertEqual(2, r.imported());
            Llibre reborn = cd2.getLlibre(9780000000050L);
            assertEqual("RoundBook", reborn.getNom());
            assertEqual("Author X", reborn.getAutor());
            assertEqual(2020, reborn.getAny());
            assertEqual(7.5, reborn.getValoracio());
            assertEqual(19.99, reborn.getPreu());
            assertEqual(true, reborn.getLlegit());
            // Shelf membership should also be present
            assertEqual(true, cd2.getAllLlistes().size() >= 1);
        });

        // ── BookExporter golden-file test ─────────────────────────────────────
        test("BookExporter.exportJSON output matches golden fixture", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre g = LlibreValidator.checkLlibre(9780000000050L, "GoldenBook", "Author X", 2020, "A description", 7.5, 0.0, true, "");
            cd.addLlibre(g);
            domini.Llista fav = cd.addLlista("Fav");
            cd.addLlibreToLlista(9780000000050L, fav.getId(), 8.0, true);
            domini.Tag fiction = cd.addTag("fiction");
            cd.addLlibreToTag(9780000000050L, fiction.getId());

            java.io.File tmp = java.io.File.createTempFile("golden_export", ".json");
            tmp.deleteOnExit();
            herramienta.BookExporter.exportJSON(tmp, cd);
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
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre b1 = LlibreValidator.checkLlibre(9780000000060L, "ShelfBook1", "Auth1", 2021, "Desc1", 8.0, 10.0, true, "");
            Llibre b2 = LlibreValidator.checkLlibre(9780000000061L, "ShelfBook2", "Auth2", 2022, "Desc2", 6.0, 5.0, false, "");
            cd.addLlibre(b1);
            cd.addLlibre(b2);
            domini.Llista s1 = cd.addLlista("Sci-Fi");
            domini.Llista s2 = cd.addLlista("Classics");
            cd.addLlibreToLlista(9780000000060L, s1.getId(), 9.0, true);
            cd.addLlibreToLlista(9780000000060L, s2.getId(), 5.0, false);
            cd.addLlibreToLlista(9780000000061L, s1.getId(), 7.5, false);
            domini.Tag t1 = cd.addTag("adventure");
            domini.Tag t2 = cd.addTag("classic");
            cd.addLlibreToTag(9780000000060L, t1.getId());
            cd.addLlibreToTag(9780000000060L, t2.getId());
            cd.addLlibreToTag(9780000000061L, t1.getId());

            java.io.File tmp = java.io.File.createTempFile("roundtrip_json", ".json");
            tmp.deleteOnExit();
            herramienta.BookExporter.exportJSON(tmp, cd);

            resetSingletons();
            ControladorDomini cd2 = ControladorDomini.getInstance();
            herramienta.BookImporter.ImportResult result = herramienta.BookImporter.importJSON(tmp, cd2);
            assertEqual(2, result.imported());

            java.util.List<domini.Llista> importedShelves = cd2.getAllLlistes();
            java.util.Set<String> shelfNames = new java.util.HashSet<>();
            for (domini.Llista s : importedShelves) shelfNames.add(s.getNom());
            assertEqual(true, shelfNames.contains("Sci-Fi"));
            assertEqual(true, shelfNames.contains("Classics"));

            java.util.List<domini.Tag> importedTags = cd2.getAllTags();
            java.util.Set<String> tagNames = new java.util.HashSet<>();
            for (domini.Tag t : importedTags) tagNames.add(t.getNom());
            assertEqual(true, tagNames.contains("adventure"));
            assertEqual(true, tagNames.contains("classic"));

            java.util.List<domini.Llista> shelvesFor60 = cd2.getLlistesForLlibre(9780000000060L);
            assertEqual(2, shelvesFor60.size());
            java.util.List<domini.Tag> tagsFor60 = cd2.getTagsForLlibre(9780000000060L);
            assertEqual(2, tagsFor60.size());
            java.util.List<domini.Llista> shelvesFor61 = cd2.getLlistesForLlibre(9780000000061L);
            assertEqual(1, shelvesFor61.size());
            assertEqual("Sci-Fi", shelvesFor61.get(0).getNom());
        });

        // ── Config H2 doesn't store stale host/user ───────────────────────────
        test("Config: switching to H2 clears stale host/user values", () -> {
            java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("biblioteca_cfg_h2_");
            java.nio.file.Path cfgDir = tmpDir.resolve(".biblioteca");
            java.nio.file.Files.createDirectories(cfgDir);
            String origHome = System.getProperty("user.home");
            try {
                System.setProperty("user.home", tmpDir.toFile().getAbsolutePath());
                herramienta.Config.reload();
                herramienta.DbConfig.setType("mariadb");
                herramienta.DbConfig.setHost("db.example.com");
                herramienta.DbConfig.setUser("admin");
                Thread.sleep(400);
                assertEqual("db.example.com", herramienta.Config.getDbHost());
                assertEqual("admin", herramienta.Config.getDbUser());

                herramienta.DbConfig.setType("h2");
                Thread.sleep(400);
                assertEqual("h2", herramienta.Config.getDbType());
                assertEqual("localhost", herramienta.Config.getDbHost());
                assertEqual("user", herramienta.Config.getDbUser());
            } finally {
                System.setProperty("user.home", origHome);
                herramienta.Config.reload();
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

    private static void resetSingletons() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    @FunctionalInterface
    interface TestBody { void run() throws Exception; }

    private static void test(String name, TestBody body) {
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

    private static void assertThrows(TestBody body) {
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
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            for (int i = 1; i <= 150; i++)
                cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "L" + i, null, null, null, null, null, null, null));
            // Page 0 should work
            assertEqual(100, cd.get100Llibres(0).size());
            // Page 1 should work
            assertEqual(50, cd.get100Llibres(1).size());
            // Page beyond range should return empty
            assertEqual(0, cd.get100Llibres(5).size());
        });

        test("Pagination: max index is correct for 0 books", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            assertEqual(0, cd.maxIndex100Llibres());
        });

        // ── Shelf delete with books shows confirmation ─────────────────────────
        test("Delete shelf that has books leaves books intact", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
            Llista shelf = cd.addLlista("DelShelf");
            cd.addLlibreToLlista(9780306406157L, shelf.getId(), 5.0, false);
            assertEqual(1, cd.getLlibresInLlista(shelf.getId()).size());
            cd.deleteLlista(shelf);
            // Book still exists
            assertEqual(1, cd.getSize());
            // Shelf membership gone
            assertEqual(0, cd.getLlibresInLlista(shelf.getId()).size());
        });

        // ── SyncAutors batch: multiple authors round-trip ────────────────────
        test("Book with multiple authors preserves all authors after update", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
            l.setAutors(java.util.Arrays.asList("Author A", "Author B", "Author C"));
            cd.addLlibre(l);
            Llibre retrieved = cd.getLlibre(9780306406157L);
            assertEqual(3, retrieved.getAutors().size());
            assertEqual(true, retrieved.getAutors().contains("Author A"));
            assertEqual(true, retrieved.getAutors().contains("Author B"));
            assertEqual(true, retrieved.getAutors().contains("Author C"));
        });

        test("Update book replacing authors preserves new list", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Test", "Old Author", null, null, null, null, null, null);
            cd.addLlibre(l);
            l.setAutors(java.util.Arrays.asList("New Author X", "New Author Y"));
            cd.updateLlibre(l);
            Llibre retrieved = cd.getLlibre(9780306406157L);
            assertEqual(2, retrieved.getAutors().size());
            assertEqual(true, retrieved.getAutors().contains("New Author X"));
            assertEqual(true, retrieved.getAutors().contains("New Author Y"));
            assertEqual(false, retrieved.getAutors().contains("Old Author"));
        });

        // ── EstadistiquesHelper.booksByReadYear ─────────────────────────────────
        test("booksByReadYear groups read books by year", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            Llibre l1 = LlibreValidator.checkLlibre(9780000000001L, "A", null, null, null, null, null, null, null);
            l1.setLlegit(true); l1.setDataLectura("2023-05-10");
            books.add(l1);
            Llibre l2 = LlibreValidator.checkLlibre(9780000000002L, "B", null, null, null, null, null, null, null);
            l2.setLlegit(true); l2.setDataLectura("2023-11-01");
            books.add(l2);
            Llibre l3 = LlibreValidator.checkLlibre(9780000000003L, "C", null, null, null, null, null, null, null);
            l3.setLlegit(true); l3.setDataLectura("2024-01-15");
            books.add(l3);
            Llibre l4 = LlibreValidator.checkLlibre(9780000000004L, "D", null, null, null, null, null, null, null);
            l4.setLlegit(false);
            books.add(l4);
            java.util.Map<Integer, Long> byYear = presentacio.EstadistiquesHelper.booksByReadYear(books);
            assertEqual(2L, byYear.getOrDefault(2023, 0L));
            assertEqual(1L, byYear.getOrDefault(2024, 0L));
            assertEqual(0L, byYear.getOrDefault(2025, 0L));
        });

        test("booksByReadYear falls back to any for year when dataLectura missing", () -> {
            java.util.List<Llibre> books = new java.util.ArrayList<>();
            Llibre l = LlibreValidator.checkLlibre(9780000000001L, "A", null, 2022, null, null, null, null, null);
            l.setLlegit(true);
            books.add(l);
            java.util.Map<Integer, Long> byYear = presentacio.EstadistiquesHelper.booksByReadYear(books);
            assertEqual(1L, byYear.getOrDefault(2022, 0L));
        });

        // ── SwingUtils.reloadComboPreserveSelection ──────────────────────────────
        test("SwingUtils reloadComboPreserveSelection restores selection", () -> {
            javax.swing.JComboBox<Llista> combo = new javax.swing.JComboBox<>();
            java.util.List<Llista> items = java.util.Arrays.asList(
                new Llista(1, "A"), new Llista(2, "B"), new Llista(3, "C"));
            herramienta.SwingUtils.reloadComboPreserveSelection(combo, items, Llista::getId);
            assertEqual(3, combo.getItemCount());
            combo.setSelectedIndex(1);
            herramienta.SwingUtils.reloadComboPreserveSelection(combo, items, Llista::getId);
            assertEqual(1, combo.getSelectedIndex());
            assertEqual("B", combo.getSelectedItem().toString());
        });

// ── Config: column visibility round-trip ───────────────────────────────
        test("Config column visibility round-trip", () -> {
            herramienta.WindowConfig.setColVisible(3, false);
            assertEqual(false, herramienta.Config.getColVisible(3));
            herramienta.WindowConfig.setColVisible(3, true);
            assertEqual(true, herramienta.Config.getColVisible(3));
        });

        // ── Config: column width round-trip ───────────────────────────────────
        test("Config column width round-trip", () -> {
            herramienta.WindowConfig.setColWidths(new int[]{80, 100, 120, 140, 160, 220, 180});
            assertEqual(220, herramienta.Config.getColWidth(5, 100));
        });

        // ── Config: column width round-trip ───────────────────────────────────
        test("Config column width round-trip", () -> {
            int[] widths = {80, 100, 120, 140, 160, 220, 180};
            herramienta.WindowConfig.setColWidths(widths);
            assertEqual(220, herramienta.Config.getColWidth(5, 100));
        });

        // ── DetallesLlibrePanelControl: save→updateLlibre→callback happy path ──
        test("updateLlibre persists changes and callback fires", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Original Title", "Original Author", 2000, "Desc", 5.0, 10.0, false, null);
            cd.addLlibre(l);

            l.setNom("Updated Title");
            l.setAutors(java.util.List.of("Updated Author"));
            l.setValoracio(9.0);
            l.setLlegit(true);
            cd.updateLlibre(l);

            Llibre retrieved = cd.getLlibre(9780306406157L);
            assertEqual("Updated Title", retrieved.getNom());
            assertEqual("Updated Author", retrieved.getAutor());
            assertEqual(9.0, retrieved.getValoracio());
            assertEqual(true, retrieved.getLlegit());

            int[] callbackCount = {0};
            presentacio.listener.EnActualizarBBDD callback = new presentacio.listener.EnActualizarBBDD() {
                @Override public void onBookUpdated(Llibre llibre, boolean isNew) {
                    callbackCount[0]++;
                    assertEqual(9780743273565L, llibre.getISBN());
                    assertEqual("Callback Title Updated", llibre.getNom());
                }
                @Override public void onBookDeleted(Llibre llibre) {}
            };

            Llibre l2 = LlibreValidator.checkLlibre(9780743273565L, "Callback Title", null, null, null, null, null, null, null);
            cd.addLlibre(l2);
            l2.setNom("Callback Title Updated");
            cd.updateLlibre(l2);
            callback.onBookUpdated(l2, false);
            assertEqual(1, callbackCount[0]);
        });
        // ── OpenLibrarySearchTask error handling (Item 9) ────────────────────
        test("OpenLibrarySearchTask: error map from OpenLibraryClient causes SearchResult with error key", () -> {
            herramienta.OpenLibraryClient.testBaseUrl = "http://localhost:1";
            herramienta.OpenLibraryClient.testMaxRetries = 1;
            herramienta.OpenLibraryClient.testRetryBaseMs = 0;
            try {
                java.util.Map<String, String> meta = herramienta.OpenLibraryClient.lookupByISBN("9780306406157");
                assertNotNull(meta);
                assertEqual(true, meta.containsKey("error"));
                presentacio.detalles.control.OpenLibrarySearchTask.SearchResult result =
                    new presentacio.detalles.control.OpenLibrarySearchTask.SearchResult(meta, null);
                assertEqual(true, result.meta.containsKey("error"));
                assertEqual(null, result.coverBlob);
            } finally {
                herramienta.OpenLibraryClient.testBaseUrl = null;
                herramienta.OpenLibraryClient.testMaxRetries = -1;
                herramienta.OpenLibraryClient.testRetryBaseMs = -1;
            }
        });

        test("OpenLibrarySearchTask: title search error map propagates to SearchResult", () -> {
            herramienta.OpenLibraryClient.testBaseUrl = "http://localhost:1";
            herramienta.OpenLibraryClient.testMaxRetries = 1;
            herramienta.OpenLibraryClient.testRetryBaseMs = 0;
            try {
                java.util.Map<String, String> meta = herramienta.OpenLibraryClient.lookupByTitle("Some Title");
                assertNotNull(meta);
                assertEqual(true, meta.containsKey("error"));
                presentacio.detalles.control.OpenLibrarySearchTask.SearchResult result =
                    new presentacio.detalles.control.OpenLibrarySearchTask.SearchResult(meta, null);
                assertEqual(true, result.meta.containsKey("error"));
            } finally {
                herramienta.OpenLibraryClient.testBaseUrl = null;
                herramienta.OpenLibraryClient.testMaxRetries = -1;
                herramienta.OpenLibraryClient.testRetryBaseMs = -1;
            }
        });

        test("OpenLibrarySearchTask: author search error map propagates to SearchResult", () -> {
            herramienta.OpenLibraryClient.testBaseUrl = "http://localhost:1";
            herramienta.OpenLibraryClient.testMaxRetries = 1;
            herramienta.OpenLibraryClient.testRetryBaseMs = 0;
            try {
                java.util.Map<String, String> meta = herramienta.OpenLibraryClient.lookupByAutor("Some Author");
                assertNotNull(meta);
                assertEqual(true, meta.containsKey("error"));
                presentacio.detalles.control.OpenLibrarySearchTask.SearchResult result =
                    new presentacio.detalles.control.OpenLibrarySearchTask.SearchResult(meta, null);
                assertEqual(true, result.meta.containsKey("error"));
                assertEqual(null, result.coverBlob);
            } finally {
                herramienta.OpenLibraryClient.testBaseUrl = null;
                herramienta.OpenLibraryClient.testMaxRetries = -1;
                herramienta.OpenLibraryClient.testRetryBaseMs = -1;
            }
        });

        // ── DetallesLlibrePanel: null optional fields (Item 10) ──────────────
        test("Llibre with null nomCa/nomEs/nomEn persists and retrieves as null", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null);
            assertEqual(null, l.getNomCa());
            assertEqual(null, l.getNomEs());
            assertEqual(null, l.getNomEn());
            cd.addLlibre(l);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual(null, loaded.getNomCa());
            assertEqual(null, loaded.getNomEs());
            assertEqual(null, loaded.getNomEn());
        });

        test("Llibre nomCa/nomEs/nomEn update to non-null persists", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            l.setNomCa("El Quixot");
            l.setNomEs("Don Quijote");
            l.setNomEn("Don Quixote");
            cd.updateLlibre(l);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual("El Quixot", loaded.getNomCa());
            assertEqual("Don Quijote", loaded.getNomEs());
            assertEqual("Don Quixote", loaded.getNomEn());
        });

        test("Llibre nomCa/nomEs/nomEn set back to null (blank) normalizes after reload", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "El Quixot", null, null, null, null, null, null, null);
            l.setNomCa("El Quixot");
            l.setNomEs("Don Quijote");
            l.setNomEn("Don Quixote");
            cd.addLlibre(l);
            l.setNomCa("");
            l.setNomEs("");
            l.setNomEn("");
            cd.updateLlibre(l);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual(null, loaded.getNomCa());
            assertEqual(null, loaded.getNomEs());
            assertEqual(null, loaded.getNomEn());
        });

        test("Llibre nomCa/nomEs/nomEn null optional fields backup and restore", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "NullNames Book", null, null, null, null, null, null, null);
            cd.addLlibre(l);
            java.io.File f = java.io.File.createTempFile("test_null_names_", ".sql");
            f.deleteOnExit();
            cd.backupToSQL(f);
            cd.clearAll();
            cd.restoreFromSQL(f);
            Llibre loaded = cd.getLlibre(9780306406157L);
            assertEqual(null, loaded.getNomCa());
            assertEqual(null, loaded.getNomEs());
            assertEqual(null, loaded.getNomEn());
        });

        runExtendedTests();
    }

    private static void runExtendedTests() {
        // ── CsvUtils ─────────────────────────────────────────────────────────
        test("CsvUtils.parseIsbn converts ISBN-10 with X check digit to ISBN-13", () -> {
            assertEqual("9780306406157", herramienta.csv.CsvUtils.parseIsbn("0-306-40615-X"));
        });
        test("CsvUtils.parseIsbn strips non-digits from ISBN-13", () -> {
            assertEqual("9780306406157", herramienta.csv.CsvUtils.parseIsbn("978-0-306-40615-7"));
        });
        test("CsvUtils.parseIsbn empty string yields empty", () -> {
            assertEqual("", herramienta.csv.CsvUtils.parseIsbn(""));
        });
        test("CsvUtils.colVal returns empty when column missing", () -> {
            java.util.Map<String, Integer> h = herramienta.csv.CsvUtils.buildHeaderMap(new String[]{"ISBN"});
            assertEqual("", herramienta.csv.CsvUtils.colVal(h, new String[]{"978"}, "Title"));
        });
        test("CsvUtils.colVal trims cell value", () -> {
            java.util.Map<String, Integer> h = herramienta.csv.CsvUtils.buildHeaderMap(new String[]{"Nom"});
            assertEqual("Quixot", herramienta.csv.CsvUtils.colVal(h, new String[]{"  Quixot  "}, "Nom"));
        });
        test("CsvUtils.parseDoubleOrZero invalid returns 0", () -> {
            assertEqual(0.0, herramienta.csv.CsvUtils.parseDoubleOrZero("not-a-number"));
            assertEqual(7.5, herramienta.csv.CsvUtils.parseDoubleOrZero(" 7.5 "));
        });
        test("CsvUtils.csvQ escapes embedded quotes", () -> {
            assertEqual("\"Say \"\"Hi\"\"\"", herramienta.csv.CsvUtils.csvQ("Say \"Hi\""));
            assertEqual("", herramienta.csv.CsvUtils.csvQ(null));
        });
        test("CsvUtils.parseLine null yields empty array", () -> {
            assertEqual(0, herramienta.csv.CsvUtils.parseLine(null).length);
        });
        test("CsvUtils.parseLine strips carriage return", () -> {
            String[] f = herramienta.csv.CsvUtils.parseLine("a,b\r");
            assertEqual("b", f[1]);
        });
        test("CsvUtils.buildHeaderMap trims header names", () -> {
            java.util.Map<String, Integer> h = herramienta.csv.CsvUtils.buildHeaderMap(new String[]{" ISBN ", "Nom"});
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
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Default", null, null, null, null, null, null, null);
            l.setNomCa("Català");
            l.setNomEs("Español");
            l.setNomEn("English");
            assertEqual("Català", l.getDisplayNom("ca"));
            assertEqual("Español", l.getDisplayNom("es"));
            assertEqual("English", l.getDisplayNom("en"));
            assertEqual("Default", l.getDisplayNom("fr"));
        });
        test("Llibre.getDisplayNom falls back to nom when alt blank", () -> {
            Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Main Title", null, null, null, null, null, null, null);
            l.setNomCa("  ");
            assertEqual("Main Title", l.getDisplayNom("ca"));
        });

        // ── CoverService ─────────────────────────────────────────────────────
        test("CoverService.getCachedBytes unknown ISBN returns null", () -> {
            assertEqual(null, herramienta.CoverService.getCachedBytes("0000000000999"));
        });
        test("CoverService.getCachedImage unknown ISBN returns null", () -> {
            assertEqual(null, herramienta.CoverService.getCachedImage("0000000000998"));
        });
        // ── Filter: autor accent-insensitive ─────────────────────────────────
        test("Filter by autor matches accent-insensitive", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Book", "Gabriel García Márquez", null, null, null, null, null, null));
            LlibreFilter f = LlibreFilterBuilder.of().autor("Garcia").build();
            assertEqual(1, cd.aplicarFiltres(f).size());
        });

        // ── Tag CRUD ─────────────────────────────────────────────────────────
        test("Tag rename and delete", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            Tag t = cd.addTag("sci-fi");
            cd.renameTag(t.getId(), "science fiction");
            assertEqual("science fiction", cd.getAllTags().get(0).getNom());
            cd.deleteTag(t);
            assertEqual(0, cd.getAllTags().size());
        });

        // ── Loan API smoke ───────────────────────────────────────────────────
        test("prestarLlibre and retornarLlibre round-trip", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Loan Book", null, null, null, null, null, null, null));
            cd.prestarLlibre(9780306406157L, "Alice");
            assertEqual(true, cd.getLoanedISBNs().contains(9780306406157L));
            cd.retornarLlibre(9780306406157L);
            assertEqual(false, cd.getLoanedISBNs().contains(9780306406157L));
        });

        // ── clearAll ─────────────────────────────────────────────────────────
        test("clearAll removes all books and shelves", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, null, null, null));
            cd.addLlista("Shelf");
            cd.clearAll();
            assertEqual(0, cd.getSize());
            assertEqual(0, cd.getAllLlistes().size());
        });
        // ── existsInLibrary ──────────────────────────────────────────────────
        test("CsvUtils.existsInLibrary true when book present", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, null, null, null));
            assertEqual(true, herramienta.csv.CsvUtils.existsInLibrary(cd, 9780306406157L));
            assertEqual(false, herramienta.csv.CsvUtils.existsInLibrary(cd, 9780000000001L));
        });
    }
}
