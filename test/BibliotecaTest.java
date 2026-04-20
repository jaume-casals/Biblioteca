package test;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.FiltreUtils;
import herramienta.LlibreValidator;
import persistencia.ControladorPersistencia;

import java.util.ArrayList;

import domini.Llista;

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
            "jdbc:h2:mem:test;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");

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

            ArrayList<Llibre> r = cd.aplicarFiltres(
                "Cervantes", null, null,
                1600, 1700,
                8.0, 10.0,
                10.0, 15.0,
                true);
            assertEqual(1, r.size());
            assertEqual("El Quixot", r.get(0).getNom());
        });

        test("Filter llegit=false returns only unread", () -> {
            resetSingletons();
            ControladorDomini cd = ControladorDomini.getInstance();
            cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Llegit", null, null, null, null, null, true, ""));
            cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "No llegit", null, null, null, null, null, false, ""));
            ArrayList<Llibre> r = cd.aplicarFiltres(null, null, null, null, null, null, null, null, null, false);
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
            ArrayList<Llibre> llibres = cd.getLlibresInLlista(llista.getId());
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
            ArrayList<Llista> llistes = cd.getLlistesForLlibre(9780306406157L);
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
            ArrayList<Llista> llistes = cd.getLlistesForLlibre(9780306406157L);
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
            ArrayList<Llibre> llibres = cd.getLlibresInLlista(llista.getId());
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
            assertEqual(1, cd.maxIndex100Llibres());
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
            ArrayList<Llibre> inShelf = cd.getLlibresInLlista(cd.getAllLlistes().get(0).getId());
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

        // ── Summary ──────────────────────────────────────────────────────────
        System.out.println("\n══════════════════════════════════════");
        System.out.println("  Passed: " + passed + "  |  Failed: " + failed);
        System.out.println("══════════════════════════════════════");
        if (failed > 0) System.exit(1);
    }

    // ── Test infrastructure ──────────────────────────────────────────────────

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

    private static void assertThrows(TestBody body) {
        try {
            body.run();
            throw new AssertionError("expected an exception but none was thrown");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception ignored) {}
    }
}
