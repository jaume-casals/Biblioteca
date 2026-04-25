# Testing Evaluation Report
_Generated 2026-04-25 — Biblioteca project_

---

## Executive Summary

Single test file: `test/BibliotecaTest.java` (707 lines, 63 tests). Plain Java, no framework.  
Coverage is solid for CRUD happy paths and backup/restore but has **zero coverage** for loans, shelf reorder, and several ControladorDomini methods. Infrastructure has one brittle ordering dependency and a weak `assertThrows` helper.

---

## 1. TEST COVERAGE

### Estimated coverage

| Layer | Method coverage | Notes |
|---|---|---|
| `LlibreValidator` | ~100% | All branches hit |
| `FiltreUtils` | ~60% | Null-param branches untested |
| `ControladorDomini` | ~74% | 10 public methods never called |
| `ServerConect` / `ControladorPersistencia` | ~65% | Loan, blob, reorder paths absent |
| Presentation / UI | 0% | No tests exist |

No code-coverage tool is configured — percentages are manual method-count estimates.

### Integration test presence
All 63 tests are **integration tests** against H2 in-memory. No pure unit tests (no isolated class testing without DB).

### E2E coverage
Zero. No headless Swing test, no scripted UI flow.

### Integration coverage map

```
ControladorDomini public methods (28 total):

  COVERED (18):
    getAllLlibres, get10Llibres, get100Llibres, maxIndex100Llibres, getSize
    addLlibre, deleteLlibre(Llibre), deleteLlibre(Long), getLlibre, existsLlibre
    aplicarFiltres (3 overloads), backupToSQL, restoreFromSQL, clearAll
    getAllLlistes, addLlista, deleteLlista, getLlibresInLlista, getLlistesForLlibre
    addLlibreToLlista, removeLlibreFromLlista, updateLlibreInLlista
    getAllTags, addTag, deleteTag, getTagsForLlibre, addLlibreToTag, removeLlibreFromTag

  NOT COVERED (10):
    moveLlistaUp, moveLlistaDown
    getCountInLlista, setLlistaColor
    prestarLlibre, retornarLlibre, getLoanedISBNs
    getRecentlyAdded
    getLlibreBlob
    getDbSizeBytes
```

---

## 2. TEST QUALITY

### F-Q1 — Brittle test order dependency
**Importance: 7/10**

`test("Empty library returns empty list")` at line 76 does **not** call `resetSingletons()`. It relies on the DB being empty because only validator/filter tests ran before it. If any test is inserted before line 76 that touches the DB, this test will fail.

```java
// BibliotecaTest.java:76
test("Empty library returns empty list", () -> {
    // BUG: no resetSingletons() here
    ControladorDomini cd = ControladorDomini.getInstance();
    assertEqual(0, cd.getSize());
```

**Fix:** Add `resetSingletons()` as first line of that test body.

---

### F-Q2 — `assertThrows` swallows all exceptions equally
**Importance: 5/10**

`assertThrows` (line 699) catches any non-`AssertionError` exception. It cannot distinguish a correct `IllegalArgumentException` from an unexpected `NullPointerException` or `SQLException`. A test that throws for the wrong reason passes silently.

```java
// BibliotecaTest.java:699
private static void assertThrows(TestBody body) {
    try {
        body.run();
        throw new AssertionError("expected an exception but none was thrown");
    } catch (AssertionError e) { throw e; }
    catch (Exception ignored) {}  // any exception = pass
}
```

**Fix:** Add an overload that checks the exception class:

```java
private static void assertThrows(Class<? extends Exception> type, TestBody body) {
    try {
        body.run();
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing thrown");
    } catch (AssertionError e) { throw e; }
    catch (Exception e) {
        if (!type.isInstance(e))
            throw new AssertionError("expected " + type.getSimpleName()
                + " but got " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
```

Usage:
```java
test("Invalid ISBN rejected", () ->
    assertThrows(IllegalArgumentException.class, () ->
        LlibreValidator.checkLlibre(12345678901234L, "X", null, null, null, null, null, null, null)));
```

---

### F-Q3 — `resetSingletons()` null-safety gap
**Importance: 4/10**

`ControladorPersistencia.resetForTest()` (line 22–27) calls `inst.sc.clearAllData()` only if `inst != null`. On the first call (before any DB interaction), `inst` is null, so no clear happens. Harmless now (first test uses a pre-clean DB), but fragile if test order changes.

```java
// ControladorPersistencia.java:22
public static void resetForTest() {
    if (inst != null) {
        try { inst.sc.clearAllData(); } catch (Exception ignored) {}
    }
    inst = null;
}
```

The `ignored` swallowing is also dangerous — a clearAllData failure would leave dirty state that silently fails subsequent tests.

**Fix:**

```java
public static void resetForTest() {
    if (inst != null) {
        try { inst.sc.clearAllData(); }
        catch (Exception e) { throw new RuntimeException("resetForTest clearAllData failed", e); }
    }
    inst = null;
}
```

---

### F-Q4 — Duplicate-ISBN-on-edit test tests a manual replica, not actual code
**Importance: 4/10**

`test("duplicate ISBN on edit: original book preserved", line 581)` manually replicates the pre-check logic from `DetallesLlibrePanelControl.editar()`. It does not call the control. If the control's guard logic changes, this test still passes (testing a different code path).

```java
// BibliotecaTest.java:589
boolean threw = false;
try {
    if (edited.getISBN() != l1.getISBN() && cd.existsLlibre(edited.getISBN()))
        throw new Exception("duplicate");          // simulated logic, not real code
    cd.deleteLlibre(l1);
    cd.addLlibre(edited);
} catch (Exception ignored) { threw = true; }
```

**Note:** Fixing this requires making `DetallesLlibrePanelControl` testable (headless mode). Mark as _Unable to verify in current setup_ — a future refactor should extract the edit logic into `ControladorDomini.editarLlibre(oldISBN, newLlibre)` so it can be tested directly.

---

### F-Q5 — No test infrastructure for parameterized data
**Importance: 3/10**

Fields like `idioma`, `format`, `desitjat`, `dataCompra` each have 3 near-identical tests (set+retrieve, null default, backup+restore). Any new field requires 3 more copy-pasted test blocks.

**Fix (optional):** Extract a helper:

```java
private static void testFieldRoundTrip(String name, java.util.function.Consumer<Llibre> setter,
        java.util.function.Function<Llibre, Object> getter, Object expected) {
    test(name + " set and retrieve", () -> {
        resetSingletons();
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, name, null, null, null, null, null, null, null);
        setter.accept(l);
        cd.addLlibre(l);
        assertEqual(expected, getter.apply(cd.getLlibre(9780306406157L)));
    });
    // ... backup round-trip variant
}
```

---

## 3. TEST PATTERNS

### F-P1 — Zero pyramid: all tests are integration tests
**Importance: 6/10**

Every test goes through `ControladorDomini → ControladorPersistencia → ServerConect → H2`. A failure in `ServerConect.syncAutors` will surface as a failure in `test("multiple authors set and retrieve")` with no clear indication of which layer is broken. Filter logic (`aplicarFiltres`) is tested through the domain but could be tested as a pure function.

**Fix:** Add pure unit tests for `aplicarFiltres` and `FiltreUtils` that do not touch the DB:

```java
// Pure unit test — no DB needed
test("aplicarFiltres ISBN prefix filter", () -> {
    ArrayList<Llibre> pool = new ArrayList<>();
    pool.add(LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
    pool.add(LlibreValidator.checkLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
    ControladorDomini cd = ControladorDomini.getInstance();
    ArrayList<Llibre> r = cd.aplicarFiltres(pool, null, null, 978L, null, null, null, null, null, null, null, null);
    assertEqual(2, r.size());
    r = cd.aplicarFiltres(pool, null, null, 9780306L, null, null, null, null, null, null, null, null);
    assertEqual(1, r.size());
});
```

---

### F-P2 — `aplicarFiltres` ISBN path never tested
**Importance: 6/10**

The `Long ISBN` parameter in `aplicarFiltres` (ControladorDomini.java:70) calls `FiltreUtils.matchISBN`. No test in the suite exercises this code path.

```java
// ControladorDomini.java:70
&& (ISBN == null || FiltreUtils.matchISBN(ISBN, l.getISBN()))
```

**Fix:**

```java
test("aplicarFiltres by ISBN prefix", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null));
    cd.addLlibre(LlibreValidator.checkLlibre(9780743273565L, "B", null, null, null, null, null, null, null));
    ArrayList<Llibre> r = cd.aplicarFiltres(null, null, 97803L, null, null, null, null, null, null, null);
    assertEqual(1, r.size());
    assertEqual(9780306406157L, r.get(0).getISBN());
});
```

---

### F-P3 — `get100Llibres` out-of-bounds not guarded
**Importance: 7/10**

`ControladorDomini.get100Llibres(int index)` (line 94) calls `bib.subList(100 * index, ...)`. If `100 * index > bib.size()`, `subList` throws `IndexOutOfBoundsException`. The method is called from `MostrarBibliotecaControl` which uses `maxIndex100Llibres()` as a guard, but there is no defensive check in the method itself and no test for this path.

```java
// ControladorDomini.java:93
public ArrayList<Llibre> get100Llibres(int index) {
    return new ArrayList<>(bib.subList(100 * index, Math.min(100 * index + 100, bib.size())));
    // throws if 100 * index > bib.size()
}
```

**Fix (code):**

```java
public ArrayList<Llibre> get100Llibres(int index) {
    int from = Math.min(100 * index, bib.size());
    return new ArrayList<>(bib.subList(from, Math.min(from + 100, bib.size())));
}
```

**Fix (test):**

```java
test("get100Llibres out-of-bounds index returns empty", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    for (int i = 1; i <= 5; i++)
        cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "L"+i, null, null, null, null, null, null, null));
    assertEqual(0, cd.get100Llibres(1).size());  // currently throws
    assertEqual(0, cd.get100Llibres(99).size());
});
```

---

## 4. MISSING TESTS

### F-M1 — Loan tracking: zero coverage
**Importance: 8/10**

`prestarLlibre`, `retornarLlibre`, `getLoanedISBNs` (ControladorDomini.java:338–347) have no tests. These touch `ServerConect.addPrestec`, `returnPrestec`, `getAllPrestecs` (used in backup). The backup writes `INSERT INTO prestec` rows (backupToSQL:232) but no test verifies loans survive a backup+restore cycle.

```java
test("Loan round-trip: lend and return", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
    cd.prestarLlibre(9780306406157L, "Joan Doe");
    assertEqual(true, cd.getLoanedISBNs().contains(9780306406157L));
    cd.retornarLlibre(9780306406157L);
    assertEqual(false, cd.getLoanedISBNs().contains(9780306406157L));
});

test("backupToSQL + restoreFromSQL preserves active loans", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null));
    cd.prestarLlibre(9780306406157L, "Marta");
    java.io.File f = java.io.File.createTempFile("test_prestec_", ".sql");
    f.deleteOnExit();
    cd.backupToSQL(f);
    resetSingletons();
    cd = ControladorDomini.getInstance();
    cd.restoreFromSQL(f);
    assertEqual(true, cd.getLoanedISBNs().contains(9780306406157L));
});
```

---

### F-M2 — Shelf reorder: zero coverage
**Importance: 6/10**

`moveLlistaUp` / `moveLlistaDown` (ControladorDomini.java:297–305) and `swapLlistesOrdre` have no tests. The swap logic reassigns all orders (lines 315–317) then swaps the target pair (lines 322–324) — an unusual double-write that is easy to get wrong.

```java
test("moveLlistaUp swaps order", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    Llista a = cd.addLlista("A");
    Llista b = cd.addLlista("B");
    Llista c = cd.addLlista("C");
    // default order: A(0), B(1), C(2)
    cd.moveLlistaUp(b.getId()); // B should move before A
    ArrayList<Llista> llistes = cd.getAllLlistes();
    // After re-query, B should be first
    resetSingletons();
    cd = ControladorDomini.getInstance();
    llistes = cd.getAllLlistes();
    // Verify B is now before A (order is persisted and reloaded)
    int posA = -1, posB = -1;
    for (int i = 0; i < llistes.size(); i++) {
        if (llistes.get(i).getId() == a.getId()) posA = i;
        if (llistes.get(i).getId() == b.getId()) posB = i;
    }
    assertEqual(true, posB < posA);
});
```

---

### F-M3 — `sqlEsc` single-quote injection not tested
**Importance: 7/10**

`ControladorDomini.sqlEsc` (line 256) replaces `'` with `''`. If this breaks, a book title like `"L'Étranger"` generates malformed SQL in `backupToSQL`, and `restoreFromSQL` would throw or silently corrupt data. No test exercises apostrophes in any string field.

```java
test("backupToSQL escapes single quotes in title", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "L'Étranger", "Camus", null, null, null, null, null, null));
    java.io.File f = java.io.File.createTempFile("test_esc_", ".sql");
    f.deleteOnExit();
    cd.backupToSQL(f);
    resetSingletons();
    cd = ControladorDomini.getInstance();
    cd.restoreFromSQL(f);
    assertEqual("L'Étranger", cd.getLlibre(9780306406157L).getNom());
    assertEqual("Camus", cd.getLlibre(9780306406157L).getAutor());
});
```

---

### F-M4 — Duplicate tag name (UNIQUE constraint) not tested
**Importance: 5/10**

`tag.nom` has a `UNIQUE` constraint (ServerConect.java:62). `createTag` with a duplicate name throws `SQLException`. `ControladorDomini.addTag` propagates it. No test covers this.

```java
test("createTag with duplicate name throws", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    cd.addTag("Fantasia");
    assertThrows(() -> cd.addTag("Fantasia"));
});
```

---

### F-M5 — `FiltreUtils` null inputs not tested
**Importance: 4/10**

`FiltreUtils.matchString` and `matchISBN` both return `false` when either argument is null (lines 6–7, 11–12). This behaviour is documented only in code. No regression test.

```java
// FiltreUtils.java:6
public static boolean matchISBN(Long prefix, Long isbn) {
    if (prefix == null || isbn == null) return false;   // ← untested branch

// FiltreUtils.java:11
public static boolean matchString(String needle, String haystack) {
    if (needle == null || haystack == null) return false; // ← untested branch
```

**Fix:**

```java
test("matchString null inputs return false", () -> {
    assertEqual(false, FiltreUtils.matchString(null, "hello"));
    assertEqual(false, FiltreUtils.matchString("hello", null));
    assertEqual(false, FiltreUtils.matchString(null, null));
});
test("matchISBN null inputs return false", () -> {
    assertEqual(false, FiltreUtils.matchISBN(null, 9780306406157L));
    assertEqual(false, FiltreUtils.matchISBN(978L, null));
});
```

---

### F-M6 — `LlibreValidator` boundary ISBN lengths not tested
**Importance: 4/10**

The validator rejects ISBNs that are not 10 or 13 digits. Tests cover 14 digits and null but not 9 (too short), 11 (in between), or 12 (just below 13).

```java
test("ISBN 9 digits rejected", () ->
    assertThrows(() -> LlibreValidator.checkLlibre(123456789L, "X", null, null, null, null, null, null, null)));
test("ISBN 12 digits rejected", () ->
    assertThrows(() -> LlibreValidator.checkLlibre(123456789012L, "X", null, null, null, null, null, null, null)));
test("Valoracio exactly 0 accepted", () -> {
    Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, 0.0, null, null, null);
    assertEqual(0.0, l.getValoracio());
});
test("Valoracio exactly 10 accepted", () -> {
    Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, 10.0, null, null, null);
    assertEqual(10.0, l.getValoracio());
});
```

---

### F-M7 — Malformed date string silently becomes NULL
**Importance: 5/10**

`ServerConect.afegirLlibre` (line 307) catches `IllegalArgumentException` from `java.sql.Date.valueOf(dc)` and calls `ps.setNull`. So a book with `dataCompra = "not-a-date"` stores NULL silently. No test verifies this fallback; the data loss is invisible.

```java
test("malformed data_compra stored as null, not crash", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Bad Date", null, null, null, null, null, null, null);
    l.setDataCompra("not-a-date");
    cd.addLlibre(l);  // must not throw
    assertEqual(null, cd.getLlibre(9780306406157L).getDataCompra());
});
```

---

### F-M8 — `getRecentlyAdded` never tested
**Importance: 3/10**

`ControladorDomini.getRecentlyAdded()` (line 327) queries `data_afegit DESC LIMIT 20`. Not tested — the ORDER BY and LIMIT could silently break.

```java
test("getRecentlyAdded returns last inserted books", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    for (int i = 1; i <= 25; i++)
        cd.addLlibre(LlibreValidator.checkLlibre(9780000000000L + i, "Book"+i, null, null, null, null, null, null, null));
    ArrayList<Llibre> recent = cd.getRecentlyAdded();
    assertEqual(true, recent.size() <= 20);
    assertEqual(true, recent.size() > 0);
});
```

---

## 5. IMPROVEMENT PLAN (PRIORITY ORDER)

| Priority | Finding | Action | Effort |
|---|---|---|---|
| 1 | **F-P3** get100Llibres out-of-bounds (7/10) | Guard method + add test | 20 min |
| 2 | **F-M3** sqlEsc apostrophe (7/10) | Add round-trip test with `'` in title | 15 min |
| 3 | **F-Q1** brittle first test (7/10) | Add `resetSingletons()` to first test | 2 min |
| 4 | **F-M1** Loan tracking (8/10) | Add 2 tests for lend/return + backup | 30 min |
| 5 | **F-P2** aplicarFiltres ISBN path (6/10) | Add 1 filter-by-ISBN test | 10 min |
| 6 | **F-P1** Zero pyramid (6/10) | Extract `aplicarFiltres` pure unit tests | 30 min |
| 7 | **F-M2** Shelf reorder (6/10) | Add moveLlistaUp/Down tests | 25 min |
| 8 | **F-Q2** assertThrows (5/10) | Add typed overload | 10 min |
| 9 | **F-M4** Duplicate tag name (5/10) | 1 test | 5 min |
| 10 | **F-M7** Malformed date fallback (5/10) | 1 test | 5 min |
| 11 | **F-M5** FiltreUtils null (4/10) | 2 tests | 5 min |
| 12 | **F-Q3** resetSingletons safety (4/10) | Rethrow instead of swallow | 5 min |
| 13 | **F-M6** ISBN boundary (4/10) | 4 tests | 10 min |
| 14 | **F-M8** getRecentlyAdded (3/10) | 1 test | 5 min |
| 15 | **F-Q4** Edit control test (4/10) | Requires extracting edit logic to domain first | High |

Total estimated effort to reach meaningful coverage of uncovered paths: **~3 hours**.

---

## 6. WHAT DOES NOT NEED TESTS

- Swing/UI layer (`presentacio/`) — Swing is not testable without a display or a test framework like AssertJ-Swing. Not worth the complexity for a personal tool.
- `ServerConect.findLibDir`, `loadDriverFromLib` — filesystem/classpath probing; environment-dependent, no useful assertion possible.
- `autoBackup` — daemon thread; tested implicitly via `backupToSQL`.
- `Config` getters/setters — simple property file I/O; no logic to break.
