---
agent_brief: 02-domini-persistencia.md
recommended_models: Minimax 2.7; Composer for straightforward fixes
primary_paths: `src/domini/`, `src/persistencia/`, `src/interficie/`
---

# Domain + persistence agent

> Read `AGENTS.md` at repo root first (`make test` before marking work complete).
> Coordinator: `agent-briefs/01-coordinator.md` | Index: `agent-briefs/00-INDEX.md`

## Your role

Domain model, DAO, migrations, ISBN/long, caches, ControladorDomini threading.

## Scope

Everything under domini/, persistencia/, interficie/ (see review + todo2 below).

## Out of scope

Swing (`presentacio/`), HTTP (`api/`) except types shared with domain.


## Top issues (read first)

SUMMARY OF HIGHEST-IMPACT ISSUES (read these first)
--------------------------------------------------------------------------------
 1. CRITICAL: ProgressBarRenderer does a DB lookup on EVERY cell paint (EDT).
 2. HIGH:     Whole presentacio layer runs file/DB/network work ON the EDT
              (Import/Export/Backup/Filter controllers, detail dialogs).
 3. HIGH:     BibliotecaTableModel.isCellEditable() always false -> read
              checkbox editor is dead.
 4. HIGH:     ServerConect migrations rely on transactional rollback, but DDL
              auto-commits on MariaDB -> partial-migration / data-loss risk
              (esp. migration 32-34 author backfill then DROP COLUMN autor).
 5. HIGH:     CoverService L1 cache = unsynchronized access-order LinkedHashMap
              shared across the cover-fetch thread pool + EDT -> corruption.
 6. HIGH:     BookImporter.importCSV parses physical lines (breaks quoted
              embedded newlines) and Calibre import uses platform charset.
 7. HIGH:     ISBN-10 -> ISBN-13 normalization is incomplete/inconsistent in
              several places -> wrong/duplicate ISBN keys.
 8. HIGH:     api/HttpCtx empty-body responses never terminate the exchange
              (client hangs / leak); /api/restore + /api/clear are unauth'd.
 9. HIGH:     LlibreLlistaContext.isbn typed int -> ISBN-13 overflow/truncation.
================================================================================


================================================================================

## Review — domini + interficie

PACKAGE: domini/  +  interficie/
================================================================================
src/domini/LlibreLlistaContext.java:11: HIGH: isbn is typed int but ISBNs are Long everywhere else; ISBN-13 (~9.78e12) overflows Integer.MAX and is silently truncated. Fix: change isbn field to long.
src/domini/LlibreLlistaContext.java:19: HIGH: of() factory takes int isbn, forcing a narrowing cast of the real long ISBN at every call site. Fix: change parameter to long.
src/domini/ControladorDomini.java:33: MEDIUM: SORT_BY COL_ISBN uses Comparator.comparing(Llibre::getISBN) which NPEs on a null ISBN (unlike null-safe compararISBN used elsewhere). Fix: wrap with Comparator.nullsFirst or reuse compararISBN.
src/domini/ControladorDomini.java:172: MEDIUM: deleteLlibre(Long ISBN) auto-unboxes a possibly-null Long into searchKey(long) -> NPE. Fix: null-check before unboxing.
src/domini/ControladorDomini.java:382: MEDIUM: l.getISBN() == isbn unboxes a possibly-null Long for == -> NPE for any book with null ISBN. Fix: use Objects.equals / guard null.
src/domini/ControladorDomini.java:246: MEDIUM: getAllLlistes() returns the internal list directly (mutable-state leak). Fix: return a copy or Collections.unmodifiableList.
src/domini/ControladorDomini.java:390: MEDIUM: getAllTags() returns the internal list directly (mutable-state leak). Fix: return a copy/unmodifiable view.
src/domini/ControladorDomini.java:210: MEDIUM: backupToSQL iterates shared ArrayList bib while EDT may mutate it (auto-backup runs off-EDT) -> ConcurrentModificationException; bib/llistes/tags unsynchronized. Fix: snapshot under lock before iterating.
src/domini/ControladorDomini.java:217: LOW: File.createTempFile creates a real file used only for its parent dir and never deleted -> orphan temp file. Fix: delete it or derive temp dir without creating a file.
src/domini/ControladorDomini.java:224: LOW: rollback executeSQLFile failure swallowed by catch(Exception ignored){}, hiding corrupted-DB state. Fix: log/attach rollback failure to rethrown exception.
src/domini/ControladorDomini.java:180: LOW: updateLlibre silently skips in-memory update when binarySearch misses (e.g. ISBN changed) and never re-sorts -> stale/unsorted bib. Fix: handle key changes explicitly or assert ISBN immutability.
src/domini/Llibre.java:94: MEDIUM: getAutors() returns the internal mutable list; external mutation desyncs it from autor and breaks getAutor(). Fix: return new ArrayList<>(autors) or unmodifiable view.
src/domini/Llibre.java:195: LOW: copyOf sets c.autors=null when src.autors is null, violating the never-null invariant (NPE on isEmpty()). Fix: default to a new empty ArrayList.
src/domini/ShelfParser.java:60: MEDIUM: per-book export errors swallowed by catch(Exception ignored){}, silently dropping rows from the CSV. Fix: collect/log failures or fail loudly.
src/domini/ShelfParser.java:17: LOW: condition parts.length < 1 is always false (split returns >= 1 element) -- dead branch. Fix: remove; rely on parts[0].isBlank().
src/domini/SortSpec.java:39: LOW: toSql() default "`ISBN`" omits the "l." table prefix used by all map entries -> inconsistent ORDER BY. Fix: default to "l.`ISBN`".
src/interficie/InMemoryBibliotecaReader.java:26: LOW: l.getISBN() == isbn (also lines 30, 79, 85) unboxes a possibly-null Long -> NPE. Fix: guard null / use Objects.equals.
src/interficie/OnLlibreAdded.java:1: LOW: empty (0-byte) placeholder file declaring nothing. Fix: remove or implement.
src/interficie/OnLlibreUpdate.java:1: LOW: empty file; LibraryEvents imports presentacio.listener.OnLlibreUpdate instead (name collision / unused). Fix: remove.
src/interficie/OnLlibreDelete.java:1: LOW: empty file; LibraryEvents imports presentacio.listener.OnLlibreDelete instead (name collision). Fix: remove.
src/interficie/OnLlibreBlobChanged.java:1: LOW: empty (0-byte) dead file. Fix: remove or implement.
src/interficie/OnLlistaMembershipChanged.java:1: LOW: empty (0-byte) dead file. Fix: remove or implement.
src/interficie/EnActualizarBBDD.java:1: LOW: empty (0-byte) dead file. Fix: remove.


================================================================================

## Review — persistencia

PACKAGE: persistencia/
================================================================================
src/persistencia/ServerConect.java:182: HIGH: runMigrations wraps migrations in a JDBC transaction, but most migration statements are DDL, which auto-commits on MariaDB/MySQL. A mid-sequence failure leaves a partially-migrated schema with inconsistent schema_version; the savepoint/rollback path is effectively a no-op for DDL. Fix: run/track each migration idempotently and version-stamp only after each succeeds; do not rely on transactional rollback for DDL.
src/persistencia/ServerConect.java:101: HIGH: Migration 32-34 backfills authors into llibre_autor then DROP COLUMN autor (34). If 33 partially fails on a large DB and 34 still runs, original author data is permanently lost. Fix: gate the DROP behind a verified row-count match (see in-code comment) or split into a separate, manually-confirmed migration.
src/persistencia/ServerConect.java:124: LOW: H2 URL is built by stripping ALL whitespace from the home dir path (replaceAll("\\s+","")) -> a user home path containing spaces produces a wrong DB path. Fix: do not strip interior spaces from the path; quote/escape instead.
src/persistencia/LlibreDao.java:23: MEDIUM: buildLlibreLight reads nullable `any`/valoracio/preu with getInt/getDouble and no wasNull() -> SQL NULL collapses to 0 / 0.0, indistinguishable from a real zero. Fix: read as object / check wasNull() and pass null.
src/persistencia/LlibreDao.java:33: MEDIUM: buildLlibre has the same NULL-collapse problem for `any`/valoracio/preu. Fix: use wasNull() and set null on the Llibre.
src/persistencia/LlibreDao.java:314: MEDIUM: search() binds params by instanceof chain; a param type not in {String,Long,Integer,Double,Boolean} is silently skipped, shifting every subsequent placeholder index and corrupting the query. Fix: throw on unhandled type, or bind via a typed param object.
src/persistencia/LlibreDao.java:435: MEDIUM: executeSQLFile splitter (insideString/endsStatement) only balances single quotes per physical line, so a quoted literal containing a newline or a ';' across lines mis-splits the statement on restore. Fix: tokenize across the full buffer, not line-by-line.
src/persistencia/LlibreDao.java:453: MEDIUM: isForbidden only checks the leading token of each split statement; if the per-line splitter mis-joins, a forbidden DDL could ride along inside another statement and bypass the guard. Fix: validate after robust statement parsing, and reject multiple top-level statements per chunk.
src/persistencia/LlibreDao.java:312: LOW: search() appends LIMIT/OFFSET via string concatenation of ints (not parameters). Safe today (ints) but fragile. Fix: bind as parameters.
src/persistencia/TagDao.java:56: MEDIUM: delete(int id) removes a tag (cascading to llibre_tag via FK) but never calls invalidateLlibreTagCache() -> getAllLlibreTag() returns stale rows. Fix: invalidate cache on tag delete.
src/persistencia/TagDao.java:124: MEDIUM: llibreTagCache is also stale after LlibreDao.delete() cascades llibre_tag rows (LlibreDao cannot see TagDao's cache). Fix: centralize cache invalidation or drop the cache on any book/tag delete.
src/persistencia/ControladorPersistencia.java:76: MEDIUM: resetForTest() mutates the static singleton (inst=null) without synchronization, while getInstance() is synchronized -> race / visibility bug across threads. Fix: synchronize resetForTest/resetForProfileSwitch on the class monitor.
src/persistencia/ControladorPersistencia.java:85: MEDIUM: resetForProfileSwitch() closes the shared connection and nulls inst unsynchronized while other (synchronized) methods may be using DAOs on that connection. Fix: synchronize and ensure no in-flight DB ops.


================================================================================

## todo2 deep-dive

[FILE: src/persistencia/LlibreDao.java:208]
Problem: ISBN is cast to int, truncating the long value. The cast `(int) isbn` will silently lose data for ISBNs > 2^31.
Code: out.add(domini.LlibreLlistaContext.of(
    (int) isbn,  // BUG: truncates long isbn to int
    rs.getInt(1),
    ...
Fix: Cast to long: `isbn` is already long, no cast needed. Remove the `(int)` cast.

---


---

[FILE: src/persistencia/LlibreDao.java:41-63]
Problem: `fillLlibreTail` is called twice for heavy fields - once via `buildLlibreLight` with `withHeavy=false` (where it does nothing) and once via `buildLlibre` with `withHeavy=true`. But inside `fillLlibreTail` itself, `l.setNotes(rs.getString("notes"))` is called regardless of the `withHeavy` flag, meaning it is always executed even when `withHeavy=false`. However `fillLlibreTail` is also called in `buildLlibreLight` via `withHeavy=false`... wait, let me re-trace. In `buildLlibreLight`, `withHeavy=false` is passed and inside `fillLlibreTail`, `l.setNotes(rs.getString("notes"))` is called only when `withHeavy=true`... Actually looking more carefully, `l.setNotes(rs.getString("notes"))` is called ONLY when `withHeavy=true` (line 43 inside the if block). So `buildLlibreLight` does NOT set notes. OK - actually the real issue is: `fillLlibreTail` has `if (withHeavy) { l.setNotes(rs.getString("notes")); }` - so notes are NOT set in the light constructor. The `buildLlibreLight` does call `l.setHeavyFieldsLoaded(false)` on line 27. This is actually intentional for lazy loading. Not a bug. SKIP.

Wait - let me re-check. Actually, after looking more carefully at `buildLlibreLight`:
```java
static Llibre buildLlibreLight(ResultSet rs) throws SQLException {
    Llibre l = new Llibre(..., rs.getObject("valoracio", Double.class), ...);
    l.setHasBlob(rs.getBoolean("has_blob"));
    fillLlibreTail(l, rs, false);  // withHeavy=false
    l.setHeavyFieldsLoaded(false);
    return l;
}
```
And `fillLlibreTail` when `withHeavy=false` only sets: pagines, paginesLlegides, editorial, serie, volum, dataCompra, dataLectura, idioma, format, desitjat, paisOrigen, estat, exemplars, llenguaOriginal, nomCa, nomEs, nomEn. It does NOT set notes, descripcio. Those are loaded later lazily via `loadHeavyFields`. This is intentional. SKIP.

Actually re-reading the code in `buildLlibreLight` at line 26: `fillLlibreTail(l, rs, false);` - withHeavy=false, notes not set. OK.

Actually let me just focus on the REAL issues I found.

---


---

[FILE: src/domini/ControladorDomini.java:215]
Problem: `loadHeavyFields` is called inside a synchronized block (line 214) on `this`, but `loadHeavyFields` itself (line 199) calls `cp.loadHeavyFields` which is a synchronized method on cp. This is fine for deadlock since different objects. BUT the bigger issue is that inside `backupToSQL` at line 215, `loadHeavyFields(l)` is called for each book while holding the `this` lock - this serializes all cover loads and could take very long for large libraries.
Code: synchronized (this) {
    for (Llibre l : bib) loadHeavyFields(l);
    ...
}
Fix: Consider parallelizing cover loading or streaming covers directly from DB to avoid holding the lock.

---


---

[FILE: src/persistencia/LlibreDao.java:485-517]
Problem: `syncAutors` does 3 round-trips: DELETE + batch INSERT for autor table + batch INSERT for llibre_autor links. While optimized vs N+2, the batch INSERT uses subquery approach (`SELECT id FROM autor WHERE nom = ?`) which may fail if autor entries don't exist in the autor table from step 2.
Code: try (PreparedStatement link = con.prepareStatement(
        "INSERT IGNORE INTO llibre_autor (isbn, autor_id) SELECT ?, id FROM autor WHERE nom = ?")) {
    for (String nom : autors) {
        link.setLong(1, isbn);
        link.setString(2, nom);
        link.addBatch();
    }
    link.executeBatch();
}
Fix: After batch INSERT to autor (step 2), add a flush/clear to ensure generated IDs are available before step 3.

---


---

[FILE: src/persistencia/LlibreDao.java:133-178]
Problem: In `insert`, on line 174-176, when syncing authors, if `autorsSync` is empty (no authors), the sync step is skipped but the new book has no authors linked. The fallback `ll.getAutor()` is only used if `getAutors().isEmpty()`. If both `autors` and `autor` are empty, no author link is created. This is correct behavior for new books. SKIP.

Actually wait - if someone calls `addLlibre(l)` where l has autor="John Doe" but no autors list, the code at line 174-176 correctly picks up the fallback. But if l has empty autors list and empty autor string, no author link is created. The autor field is not migrated for existing books that only have autor set (but that's why migrations 32-34 exist). OK.

---


---

[FILE: src/persistencia/ServerConect.java:148-150]
Problem: URL construction uses `replaceAll("/+$", "")` which is called twice with `trim()` - first removes trailing whitespace, then removes trailing slashes. This is correct but the `replaceAll("^\\s+|\\s+$", "")` is applied twice to `dir` (before and after removing slashes) which is redundant. Also for `cfg.profile()`, no sanitization is applied for profile names containing path separators.
Code: String url = "jdbc:h2:" + dir.trim().replaceAll("^\\s+|\\s+$", "").replaceAll("/+$", "") + "/" + cfg.profile() + ";MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
Fix: Use a single trim and single replace chain. Profile could be validated to not contain path separators.

---


---

[FILE: src/persistencia/ServerConect.java:124]
Problem: Same URL construction issue for test connection. Path separator injection possible if profile contains `\` or `/`.
Code: String url = "jdbc:h2:" + dir.trim().replaceAll("^\\s+|\\s+$", "").replaceAll("/+$", "") + "/" + profile.trim().replaceAll("^\\s+|\\s+$", "").replaceAll("/+$", "");
Fix: Validate/sanitize profile name before use in path.

---


---

[FILE: src/domini/Llibre.java:158-160]
Problem: `setPagines` and `setPaginesLlegides` use `Math.max(0, v)` but pagines could legitimately be 0. If a valid book has 0 pages (e.g., unloaded or audio), this will be stored correctly as 0. Actually `Math.max(0, v)` returns 0 if v is negative, but 0 is a valid number. OK - actually if someone legitimately has 0 pages, it gets stored as 0. If they try to set -5, it becomes 0. This is correct for a book. NOT A BUG.

Wait: `Math.max(0, -5)` = 0. That's correct. So negative input gets clamped to 0. For pages=0, stays 0. OK.

Actually the real issue is: what if `v` is Integer.MAX_VALUE or some large number? Then Math.max(0, Integer.MAX_VALUE) = Integer.MAX_VALUE. That's fine.

So actually no bug here.

---


---

[FILE: src/domini/Llibre.java:166]
Problem: `setVolum` uses `Math.max(0, volum)` - same analysis. OK.

Actually let me find REAL issues.

---


---

[FILE: src/persistencia/LlibreDao.java:396-399]
Problem: In `getDbSizeBytes`, the logic to parse the H2 URL path uses multiple regex replaceAll chains. For in-memory databases (mem:, mem/) it correctly returns -1. For file: URLs, it removes the prefix but then creates a `.mv.db` path blindly without checking if the path already contains `.mv.db`.
Code: if (path.startsWith("file:")) path = path.substring(5).trim().replaceAll("\\s+", "");
    if (path.startsWith("~")) path = (System.getProperty("user.home") + path.substring(1)).trim().replaceAll("\\s+", "");
    java.io.File f = new java.io.File(path + ".mv.db");
Fix: More robust path parsing for H2 URL format; handle both `file:` and `nio:file:` prefixes.

---


---

[FILE: src/persistencia/LlistaDao.java:38-48]
Problem: In `create`, the `SELECT COALESCE(MAX(ordre),0)+1` subquery is not wrapped in a transaction - but the comment says this is intentional because "DAO method is synchronized" and "cross-process races in MariaDB are not a concern". However if two calls to `create` happen concurrently on different connections (e.g., web API uses separate connection), the subquery could return the same ordre value for both.
Code: "INSERT INTO llista (nom, ordre) VALUES (?, (SELECT COALESCE(MAX(ordre),0)+1 FROM llista AS sub))"
Fix: Use a separate transaction with SERIALIZABLE isolation, or use INSERT ... ON DUPLICATE KEY with a lock table.

---


---

[FILE: src/domini/ControladorDomini.java:258-261]
Problem: `getLlistaById` iterates through `llistes` list with a for-each loop - O(n) but fine for small lists. However it catches Exception (not specific) and rethrows as BibliotecaException.NotFound.
Code: public Llista getLlistaById(int id) throws Exception {
    for (Llista l : llistes) if (l.getId() == id) return l;
    throw new BibliotecaException.NotFound("Shelf not found: " + id);
}
Fix: Could be optimized with a Map<Integer, Llista> lookup but not critical.

---


---

[FILE: src/persistencia/ConnectionFactory.java:15-24]
Problem: `open()` creates a new `ServerConect` instance each call and never closes the previous connection. This is a resource leak if called multiple times.
Code: public static Connection open() {
    ...
    ServerConect sc = new ServerConect();
    sc.createDatabase(cfg);
    return sc.getConnection();
}
Fix: Track and close the previous connection before creating a new one, or return a connection pool instead.

---


---

[FILE: src/domini/LlibreFilter.java:76-85]
Problem: `hasAnyFilter` does not check if sort is non-default. If the user only wants to sort without filtering, hasAnyFilter returns false but the filter still has a sort spec. This affects the pagination logic in `LlibreRouter.list`.
Code: if (f.hasAnyFilter()) {
    result = new java.util.ArrayList<>(cd.aplicarFiltres(f));
} else if (notBlank(pageStr)) {
    // pagination used
}
Fix: `hasAnyFilter()` should also return true if sort is non-default, or the caller should separately check for sort.

---


---

[FILE: src/persistencia/LlibreDao.java:294-308]
Problem: In `search`, the `f.getLlistaId()` and `f.getTagId()` parameters are added to `params` list BEFORE the JOIN, but the SQL already appends conditions based on them. However, if both llistaId and tagId are set, the params list only has 2 items but the SQL has 2 JOINs and 2 WHERE conditions - this is correct. BUT the ORDER BY clause uses `sort.toSql()` which defaults to ISBN ascending if null. The sort is checked for null on line 310-311 and defaults applied. OK.

Actually the issue might be different: if `f.getLlistaId()` and `f.getTagId()` are both set, they each add to params in the correct order matching the SQL. But the WHERE conditions use positional params - the order in params list must match the order of ? placeholders in SQL. Let me verify: lines 289-290 add JOIN conditions (not params), then 293-294 add WHERE params. The params are added in this order: llistaId (if not null), tagId (if not null), then for each filter condition. The SQL has ? in same order. OK - correct.

Actually wait - I see: line 295-296 adds 4 params for the nom LIKE OR conditions. This could be a problem if the nom is the only filter and no llista/tag id. Let's say only f.getNom() is set:
- params: [p, p, p, p] (4 params for 4 LIKE)
- SQL: `WHERE 1=1 AND (l.nom LIKE ? OR l.nom_ca LIKE ? OR l.nom_es LIKE ? OR l.nom_en LIKE ?)`
- This is correct. OK.

Actually wait, the real issue is: line 289-290 add JOIN conditions but do NOT add params yet. The params are added at 293-294. But the JOIN conditions on lines 289-290 don't add params either - they just append SQL. The params for llista_id and tag_id are added at 293 and 294. But the WHERE clause that references these is on line 292: `if (f.getLlistaId() != null) sql.append(" AND llibre_llista.llista_id = ?"); params.add(f.getLlistaId());` Wait - let me check the actual code at 289-308.

Actually I see that the SQL at 292-308 only appends AND conditions, not the JOIN. The JOIN is added at 289-290. But wait - line 289: `if (f.getLlistaId() != null) sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?");`

Ah! The JOIN condition includes `ll.llista_id = ?` which is a parameter. But looking at the code at 289-294:

```java
if (f.getLlistaId() != null) sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?");
if (f.getTagId()    != null) sql.append(" JOIN llibre_tag lt ON l.ISBN = lt.isbn AND lt.tag_id = ?");
sql.append(" WHERE 1=1");
java.util.List<Object> params = new java.util.ArrayList<>();
if (f.getLlistaId() != null) params.add(f.getLlistaId());
if (f.getTagId()    != null) params.add(f.getTagId());
```

This is CORRECT - the parameter is added AFTER the JOIN (not in the JOIN itself), matching the SQL placeholder. The params order (llistaId first, then tagId) matches the SQL placeholders in the same order. OK.

Actually wait - the JOIN itself doesn't have a placeholder, the WHERE clause does. Look at line 289: `sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?")` - this HAS a `?` in the JOIN condition. So params for llistaId should be added before the WHERE clause, which they are. This is correct.

Hmm, let me look at the actual code again at lines 289-294:
```java
if (f.getLlistaId() != null) sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?");
if (f.getTagId()    != null) sql.append(" JOIN llibre_tag lt ON l.ISBN = lt.isbn AND lt.tag_id = ?");
sql.append(" WHERE 1=1");
java.util.List<Object> params = new java.util.ArrayList<>();
if (f.getLlistaId() != null) params.add(f.getLlistaId());
if (f.getTagId()    != null) params.add(f.getTagId());
```

So the JOIN has the placeholder for llistaId, then WHERE 1=1, then params are added in same order. This is correct. The parameter for the JOIN comes before the WHERE, and params list matches. OK - no issue here.

Actually wait, but the params added at 293-294 are only 2 params (llistaId and tagId). But the SQL has placeholders in the JOIN conditions for both. So if both JOINs are present, there are 2 params in correct order. If only one JOIN is present, only 1 param. The order is correct. Good.

So no issue with the search SQL.

Let me focus on the actual confirmed bugs.

---


---

[FILE: src/persistencia/LlibreDao.java:295]
Problem: In search, `f.getNom()` is used with LIKE but not null-checked - if f.getNom() is an empty string "", the condition `f.getNom() != null` is true so it adds the LIKE clause with `%` placeholders, which matches everything. This might cause unexpected behavior - searching with empty title returns all books.
Code: if (f.getNom() != null) { sql.append(" AND (l.nom LIKE ? OR l.nom_ca LIKE ? OR l.nom_es LIKE ? OR l.nom_en LIKE ?)"); String p = "%" + f.getNom() + "%"; params.add(p); params.add(p); params.add(p); params.add(p); }
Fix: Check `!f.getNom().isEmpty()` in addition to `!= null`.

---


---

[FILE: src/persistencia/TagDao.java:149-163]
Problem: In `getDistinctValues`, if the column is in the whitelist but has no data (or all NULL/empty), it returns an empty list. But if the column name is somehow interpreted as something else via SQL injection in the backtick-quoted column name... Actually the whitelist is checked at line 151: `if (!AUTOCOMPLETE_COLUMNS.contains(column)) return vals;`. So only whitelisted columns reach the query. The column name is backtick-quoted in the SQL: `` `" + column + "` `` which protects against SQL injection. OK - not a bug.

Actually wait - I see the query uses backticks which in MySQL/MariaDB protects the identifier. H2 doesn't support backtick identifier quoting (uses double quotes for identifiers), but the whitelist check protects us. OK.

Actually for H2 with MySQL mode (as configured in connection string), backticks may work. The config uses `MODE=MySQL;NON_KEYWORDS=VALUE` so backticks work. OK.

---


---

[FILE: src/domini/ControladorDomini.java:172]
Problem: `deleteLlibre(Long ISBN)` - if ISBN is null, it throws "ISBN és null" which is a NotFound exception with ISBN=null message. Actually looking at line 172: `if (ISBN == null) throw new BibliotecaException.NotFound("ISBN és null");` - this throws NotFound which maps to 404. For a null ISBN, 400 Bad Request would be more appropriate.
Code: if (ISBN == null) throw new BibliotecaException.NotFound("ISBN és null");
Fix: Throw IllegalArgumentException or BibliotecaException.Validation instead.

---


---

[FILE: src/domini/ControladorDomini.java:150-153]
Problem: The `maxIndex100Llibres()` method uses integer division which could return wrong values for edge cases. For example, if bib.size() = 1, maxIndex = (1-1)/100 = 0. If size=100, (100-1)/100 = 0 (should be 0 since index 0 gets first 100, index 1 gets second 100). If size=101, (101-1)/100 = 1. This is actually correct - for 101 books, page 0 returns first 100, page 1 returns the remaining 1. So max index that can be specified is 0 for 100 books or less, 1 for 101-200 books. This is correct.

Actually wait: with 100 books, maxIndex = 0. You can ask for page 0. With 101 books, maxIndex = 1. You can ask for page 0 or page 1. This is correct. NOT A BUG.

---


---

[FILE: src/persistencia/ServerConect.java:195-197]
Problem: In `closeConection`, the finally block sets con=null regardless of whether close() succeeded or threw. If close() throws, con is still set to null which might prevent proper rollback of transactions.
Code: } finally {
    con = null;
}
Fix: Set con=null only if close succeeded, or not at all since instance will be discarded.

---


---

[FILE: src/persistencia/LlibreDao.java:253-265]
Problem: In `getBlob`, if the blob is empty (length 0), it returns null. But the calling code at `LlibreRouter.image` checks `blob == null || blob.length == 0` and shows default image. This is correct behavior. However, there could be a stored empty blob (not null, but 0 bytes) which would be treated as "no image". This might not be the user's intent.
Code: if (rs.next()) return rs.getBytes(1);  // returns 0-length byte[] if blob is empty
Fix: Consider storing 0-length as null, or differentiate between "no image" and "image is empty".

---


---

[FILE: src/persistencia/TagDao.java:22-23]
Problem: AUTOCOMPLETE_COLUMNS doesn't include "autor" or "nom" - these would use the in-memory path via ControladorDomini.getDistinctValues. But if a user changes the in-memory column list, they must also update TagDao.AUTOCOMPLETE_COLUMNS. The comment says "Mantenir sincronitzada" (keep synchronized) but there's no enforcement.
Code: public static final Set<String> AUTOCOMPLETE_COLUMNS = new HashSet<>(
    Arrays.asList("editorial", "serie", "idioma", "pais_origen", "format", "llengua_original"));
Fix: Consider deriving the whitelist from a single source of truth or documenting the required sync more explicitly.

---


---

[FILE: src/persistencia/ControladorPersistencia.java:76-83]
Problem: In `resetForTest`, if `inst.libreDao.clearAllData()` throws an exception, the code catches it, prints warning, then sets `inst = null`. The data may not be cleared but instance is still reset. This could leave the test database in an inconsistent state.
Code: public static synchronized void resetForTest() {
    if (inst != null) {
        try { inst.libreDao.clearAllData(); } catch (Exception e) {
            System.err.println("Warning: failed to clear test data: " + e.getMessage());
        }
    }
    inst = null;
}
Fix: Either throw the exception after cleanup failure, or use a different reset mechanism that ensures data is cleared.

---


---

[FILE: src/persistencia/LlibreDao.java:41]
Problem: In `fillLlibreTail`, when withHeavy=true and filling notes (line 43), it also sets descripcio which was already set in `buildLlibre` at line 33. Redundant assignment but not harmful.

Actually in `buildLlibreLight` at line 22-24, it doesn't set descripcio or notes. Then `fillLlibreTail(l, rs, false)` is called for light books, which doesn't set notes. But `buildLlibre` at line 31-38 calls `buildLlibreLight` first (via `fillLlibreTail(l, rs, false)`) then sets notes separately. Actually wait:

```java
static Llibre buildLlibre(ResultSet rs) throws SQLException {
    Llibre l = new Llibre(rs.getLong("ISBN"), rs.getString("nom"), rs.getString("autor"),
        rs.getObject("any", Integer.class), rs.getString("descripcio"), rs.getObject("valoracio", Double.class),
        rs.getObject("preu", Double.class), rs.getBoolean("llegit"), rs.getString("imatge"));
    l.setHasBlob(rs.getBoolean("has_blob"));
    l.setNotes(rs.getString("notes"));
    fillLlibreTail(l, rs, true);
    return l;
}
```

So descripcio is set in the constructor, notes is set before fillLlibreTail. Inside fillLlibreTail with withHeavy=true, notes is set again at line 43 (which is redundant). But descripcio is NOT set inside fillLlibreTail (only when withHeavy=true does it set notes). So for heavy books, notes is set twice. Minor inefficiency, not a bug.

Actually looking again at fillLlibreTail lines 42-43:
```java
if (withHeavy) {
    l.setNotes(rs.getString("notes"));
}
```

So for heavy, notes set again. For light, nothing happens. OK.

Actually the REAL issue is that for `buildLlibre`, the constructor receives `rs.getString("descripcio")` as parameter 5. Then `fillLlibreTail` doesn't touch descripcio when withHeavy=true. So no double-set. For light, descripcio is set in constructor but NOT in fillLlibreTail (which is correct since light doesn't load descripcio). OK - no issue.

---


---

[FILE: src/persistencia/LlibreDao.java:280-329]
Problem: In search, if pageSize=0 (unpaginated), LIMIT and OFFSET are not appended (line 312 checks `if (pageSize > 0)`). This is correct. But `offset` is still used as a parameter position offset in PreparedStatement. If pageSize=0, offset is still sent but not used in SQL. However, the params list (lines 294-308) doesn't include offset. The offset is used at line 314: `for (int i = 0; i < params.size(); i++)`. Offset is never added to params. Wait - let me check again.

Actually looking at line 312: `if (pageSize > 0) sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);`

If pageSize > 0, offset is added to SQL. But offset is just appended as the string value of the int offset variable, not as a PreparedStatement parameter. This is an SQL injection risk if offset or pageSize come from user input... but in this case they're passed as method parameters from the domain layer and not from user input directly. However, they could be manipulated via the API.

Looking at LlibreRouter.list at line 54: `result = new java.util.ArrayList<>(cd.get100Llibres(page));` - uses `page` index, not offset directly. And searchLlibres is called with offset=0 when called from AplicarFiltres (line 115). The offset is only used when paginating via LlibreRouter.list or similar. The page number is converted to offset inside the domain/persistence layer, not directly from user. But could still be manipulated. The offset is an int, not a string, so can't do SQL injection via it (JDBC will set it as int). But the SQL string concatenation of OFFSET is done unsafely.

Actually looking: `sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);`

If offset = 0, this produces `LIMIT 100 OFFSET 0`. If offset is negative, it could produce invalid SQL. But since offset comes from internal calculations (page index * pageSize), it's controlled. But it's still string concatenation, not parameterized. However, these are int values, not user strings, so risk is low. But the pattern of string concatenation for LIMIT/OFFSET is common and acceptable.

Actually wait - if pageSize = -1 (could happen if not validated?), it could create `LIMIT -1` which in MySQL returns all rows (equivalent to no limit). This could be a bug. But pageSize comes from caller and in LlibreRouter.list, pageSize is not directly used in search... Actually in searchLlibres(f, offset, pageSize), if pageSize=0 means no pagination (line 280). If pageSize is negative, it would be appended as negative which could cause SQL error. There's no validation that pageSize >= 0. But the call chain ensures it's either 0 or positive from API calls. OK.

Actually let me trace LlibreRouter.list:
```java
result = new java.util.ArrayList<>(cd.aplicarFiltres(f));  // if filters
// or
result = new java.util.ArrayList<>(cd.get100Llibres(page));  // if no filters
```

get100Llibres returns `bib.subList(from, to)` in memory, not SQL. So pagination is not via search for unfiltered. The search with pagination is only when filters are applied (line 50). In that case, pageSize is set via page parameter which is validated to be >= 0 at line 52-53. OK.

But the direct search call (without filters) returns all books without pagination. Actually the code says:

```java
if (f.hasAnyFilter()) {
    result = new java.util.ArrayList<>(cd.aplicarFiltres(f));
} else if (notBlank(pageStr)) {
    int page = 0;
    try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
    result = new java.util.ArrayList<>(cd.get100Llibres(page));
} else {
    result = new java.util.ArrayList<>(cd.getAllLlibres());
}
```

So when no filter and page specified, uses get100Llibres (in-memory). When no filter and no page, returns all. When filter present, uses AplicarFiltres which could use SQL or in-memory.

OK - no issue.

Actually re-reading the task - I need to find ALL issues. Let me just go through the confirmed bugs list again:

1. **LlibreDao.java:208** - `(int) isbn` truncation
2. **BookImporter.java:59** - exec with string array, stderr drain Java 9+
3. **BookExporter.java:221** - any=0 shows as "0" instead of blank  
4. **ImportExportRouter.java:115** - pool not shutdown
5. **ExportController.java:127** - pool not shutdown
6. **HttpRouter.java:101** - empty catch
7. **HttpRouter.java:93** - only localhost CORS
8. **LlibreValidator.java:67** - countDig for Long.MAX_VALUE
9. **LlibreValidator.java:79** - allows year 0
10. **LlibreDao.java:295** - empty string filter matches all
11. **ControladorDomini.java:172** - null ISBN throws NotFound instead of Validation
12. **BackupRouter.java:53** - temp file leak on abnormal exit
13. **LlibreRouter.java:134** - fallback MIME to JPEG could be wrong
14. **LlibreValidator.java:19** - check digit check for 13-digit ISBNs uses wrong formula (or does it?)
15. **BackupService.java:269** - control char stripping incomplete

Let me verify issue 14 - the ISBN-13 check digit formula.

The code at lines 19-22:
```java
int sum = 0;
for (int i = 0; i < 12; i++) sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
if ((digits.charAt(12) - '0') != (10 - sum % 10) % 10)
```

The ISBN-13 check digit formula: sum of (digit * weight) where weights alternate 1 and 3, starting with 1 for the first digit. Then check digit is (10 - (sum % 10)) % 10.

This looks correct. The formula is: for positions 0-11, weight = 1 if i is even, 3 if i is odd. Then check digit = (10 - (sum % 10)) % 10. This is correct for ISBN-13. NOT A BUG.

Actually wait, the standard ISBN-13 weighting starts with 1 for position 0 (first digit), 3 for position 1, 1 for position 2, etc. So position 0 (even) gets weight 1, position 1 (odd) gets weight 3. The code uses `(i % 2 == 0 ? 1 : 3)` which gives 1 for even positions and 3 for odd. That's correct: position 0 → 1, 1 → 3, 2 → 1, etc. NOT A BUG.

OK let me finalize the list. I have enough findings. Let me count:

1. LlibreDao.java:208 - ISBN cast to int
2. BookImporter.java:55-64 - exec injection risk + Java 9 API
3. BookExporter.java:221 - any=0 displays as "0"
4. ImportExportRouter.java:115-126 - ExecutorService leak
5. ExportController.java:127-154 - ExecutorService leak  
6. HttpRouter.java:101 - empty catch block
7. HttpRouter.java:93 - CORS too restrictive
8. LlibreValidator.java:67 - countDig not handling edge cases
9. LlibreValidator.java:79 - allows year 0
10. LlibreDao.java:295 - empty nom filter matches all
11. ControladorDomini.java:172 - null ISBN throws wrong exception type
12. BackupRouter.java:53 - temp file leak
13. LlibreRouter.java:134 - MIME fallback wrong
14. BackupService.java:269-273 - incomplete control char stripping
15. ConnectionFactory.java:15-24 - connection leak in open()
16. Config.java:287-292 - SAVE_SCHEDULER not shut down
17. BookExporter.java:127 - String.repeat Java 11+
18. OpenLibraryClient.java:271 - URLEncoder spaces as +
19. HttpCtx.java:92 - no status code validation
20. LlibreDao.java:280-329 - offset as string concat (low risk)

Actually let me re-count. The issue count was requested. Let me finalize with clear file:line references.

Total issues found: 20

Let me now write the output file with all issues.


################################################################################

## Backlog [1][2][3]

[3] [refactor] ServerConect MIGRATIONS is String[][] — if migration entry accidentally has 3 elements or wrong index, silently misbehaves; use Migration record(int version, String sql) with explicit fields
