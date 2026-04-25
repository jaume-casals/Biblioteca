# Architecture Analysis Report
_Generated 2026-04-25 — Biblioteca project_

---

## 1. Pattern and Layer Summary

**Pattern used:** Three-layer MVC with singletons at each boundary.

```
┌─────────────────────────────────────────────────────────────────────┐
│  main/Ejecutable                                                     │
│      ↓ init ControladorDomini (blocking, on main thread)            │
│      ↓ then EventQueue.invokeLater → SwingUI                        │
│                                                                      │
│  presentacio/            ← Swing GUI (Panel + Control per screen)   │
│      MainFrameControl                                                │
│      MostrarBibliotecaControl  ←─── herramienta/(Config,UITheme,…)  │
│      GaleriaCobertesPanel                                           │
│      detalles/control/DetallesLlibrePanelControl                    │
│      detalles/control/GuardarLlibresDialogoControl                  │
│          ↓  via ControladorDomini.getInstance()                     │
│                                                                      │
│  domini/                 ← Business logic, in-memory sorted bib     │
│      ControladorDomini                                               │
│          ↓                                                           │
│  persistencia/           ← JDBC layer                               │
│      ControladorPersistencia (thin wrapper)                          │
│      ServerConect (all SQL)                                          │
│          ↓                                                           │
│  H2 embedded / MariaDB   ← external                                 │
│                                                                      │
│  herramienta/            ← cross-cutting (Config, UITheme, …)       │
│  interficie/EnActualizarBBDD  ← callback interface (2 methods)      │
└─────────────────────────────────────────────────────────────────────┘

External API:
  herramienta/OpenLibraryClient  →  https://openlibrary.org  (HTTP)
```

**Separation of concerns:** Partially present. Package boundaries are clear but several hard violations exist (detailed below).

**Modularity rating: 5/10**  
Good: 4 named packages, Panel/Control split per screen, utility classes isolated.  
Bad: 1620-line God class, circular singleton calls, persistence layer spawns UI dialogs, no interfaces at layer boundaries, no `UPDATE` path (delete+re-add as mutation).

---

## 2. FINDINGS

---

### A-1 — `MostrarBibliotecaControl` is a 1620-line God class
**Importance: 8/10**

File: `src/presentacio/MostrarBibliotecaControl.java`

Single class responsible for:
- Table model management and 7 inner renderer/editor classes
- CSV import (`importarCSV`, `parseCSVLine`)
- CSV export (`exportarCSV`)
- Filter state collection + application (`filtrar`, `quitarFiltros`, `aplicarSearchBar`, `collectFilterState`, `applyFilterState`)
- Filter preset management (`carregarPreset`, `desarPreset`, `esborrarPreset`, `refreshComboPresets`)
- Statistics dialog construction (`mostrarEstadistiques`, `buildStatsSummary`) — ~120 lines of Swing dialog building
- Shelf management (`refreshComboLlistes`, `onLlistaSelected`, `obrirGestioLlistes`)
- Tag combo management (`refreshComboTags`)
- Backup/restore UI (`backupBD`, `restaurarBD`)
- Loan UI (`prestarLlibre`)
- Gallery interactions (`showGaleriaContextMenu`, `eliminarLlibresGaleria`, `afegirLlibresGaleriaALlista`)
- Column visibility (`toggleColumn`, `applyColumnVisibility`)
- Pagination (`showPage`)

**Fix:** Extract these responsibilities into focused classes:

```
MostrarBibliotecaControl   ← keeps: table population, pagination, shelf/filter wiring
CsvImportExport            ← importarCSV, exportarCSV, parseCSVLine
FilterStateManager         ← collectFilterState, applyFilterState, preset CRUD
EstadistiquesDialog        ← mostrarEstadistiques, buildStatsSummary
CoverCellRenderer          ← move to presentacio/renderer/
SearchHighlightRenderer    ← move to presentacio/renderer/
ProgressBarRenderer        ← move to presentacio/renderer/
LlegitCheckBoxRenderer+Editor ← move to presentacio/renderer/
```

No code change needed to make this work today — it's a refactor. Start with the easiest extraction: `parseCSVLine` (pure function, zero deps) can move to `herramienta/CsvUtils.java` immediately:

```java
// herramienta/CsvUtils.java  (new file)
package herramienta;
import java.util.*;
public final class CsvUtils {
    private CsvUtils() {}
    public static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQ) {
                if (ch == '"' && i+1 < line.length() && line.charAt(i+1) == '"') { sb.append('"'); i++; }
                else if (ch == '"') inQ = false;
                else sb.append(ch);
            } else if (ch == '"') { inQ = true; }
            else if (ch == ',') { fields.add(sb.toString()); sb = new StringBuilder(); }
            else sb.append(ch);
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }
}
```

---

### A-2 — Circular dependency: `MainFrameControl` ↔ `MostrarBibliotecaControl`
**Importance: 8/10**

`MainFrameControl` creates `MostrarBibliotecaControl` (line 97-98):
```java
// MainFrameControl.java:97
mostrarControl = new MostrarBibliotecaControl(
    this.vista.getMostrarBibliotecaPanel(), cLlibres.getAllLlibres(), this);
```

`MostrarBibliotecaControl` calls back into `MainFrameControl` via its singleton in at least 8 places:
```java
// MostrarBibliotecaControl.java:327
Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(...)

// MostrarBibliotecaControl.java:600
Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(isbn);  // inside RowFilter
// MostrarBibliotecaControl.java:783, 1126, 1212, 1263, 1360, 1380, 1529
```

`getInstance(null)` passing `null` is a documented pattern ("passes null after first call" is literally a bug in todo.txt). The null is required because only the creator (`Ejecutable`) has a `MainFramePanel` reference, but the singleton is already constructed.

**Immediate safe fix:** Extract `getLlibreIsbn` to be callable directly from `ControladorDomini`:

```java
// MostrarBibliotecaControl.java — replace MainFrameControl.getInstance(null).getLlibreIsbn(isbn) with:
try {
    Llibre l = ControladorDomini.getInstance().getLlibre(isbn);
    // ...
} catch (Exception ignored) {}
```

This applies to: lines 327, 600, 783, 1126, 1212, 1263, 1360, 1380, 1529. `getLlibreIsbn` in `MainFrameControl` is a thin wrapper around `cLlibres.getLlibre(ISBN)` that eats exceptions — reproduce inline. Eliminates the circular call entirely.

---

### A-3 — `ServerConect` imports `herramienta.DialogoError` (persistence → UI)
**Importance: 7/10**

`ServerConect.java` catches `SQLException` in 20+ methods and calls:
```java
new DialogoError("Error message", e).showErrorMessage();
```

This means the persistence layer creates Swing dialogs. Consequences:
- `ServerConect` cannot run headless or in tests without a display
- `biblioteca.test` system property hack (`Boolean.getBoolean("biblioteca.test")`) exists specifically because of this
- The JDBC layer decides what UI to show, not the caller

**Fix:** Throw instead, let presentation handle UI. Change all silent-catch methods to throw:

```java
// Before (ServerConect.java:280):
} catch (SQLException e) {
    new DialogoError("Error al agafar tots els llibres", e).showErrorMessage();
}
return biblio;

// After:
} catch (SQLException e) {
    throw new RuntimeException("getAllLlibres failed", e);
}
```

For methods that currently return empty lists on error (e.g., `getAllLlibres`, `getAllTags`), the callers already exist and just need a try/catch at the presentation layer. `ControladorPersistencia` is the right place to catch and re-throw with context:

```java
// ControladorPersistencia.java
public ArrayList<Llibre> getAllLlibres() {
    try { return sc.getAllLlibres(); }
    catch (RuntimeException e) { throw e; }  // let it propagate
}
```

Presentation controllers (`MostrarBibliotecaControl`, etc.) already have `new DialogoError(e).showErrorMessage()` at the UI boundary — they just need to catch.

The `biblioteca.test` hack in `ServerConect.createDatabase()` (line 113) can also be removed once `DialogoError` calls are gone.

---

### A-4 — Delete+re-add used as UPDATE: destroys FK relationships
**Importance: 9/10**

No `UPDATE` statement exists for the `llibre` table anywhere in the codebase. Every edit goes through:

```java
// DetallesLlibrePanelControl.java:197
cLlibres.deleteLlibre(llibre);   // → DELETE FROM llibre WHERE ISBN=?
cLlibres.addLlibre(a);           // → INSERT INTO llibre ...
```

`deleteLlibre` triggers `ON DELETE CASCADE` on:
- `llibre_llista` — **shelf memberships are destroyed**
- `llibre_tag` — **tag associations are destroyed**
- `prestec` — **loan records are destroyed**
- `llibre_autor` — **multi-author links are destroyed**

Same pattern in `LlegitCheckBoxEditor` (MostrarBibliotecaControl.java:1267):
```java
ControladorDomini.getInstance().deleteLlibre(l);
ControladorDomini.getInstance().addLlibre(updated);
```

Toggling the read/unread checkbox on any book in the table **silently deletes all its shelf memberships, tags, and loans**.

**Fix:** Add a proper `updateLlibre` that issues an `UPDATE` statement, then wire it through the stack:

```java
// ServerConect.java — new method
public synchronized void updateLlibre(Llibre l) throws SQLException {
    PreparedStatement ps = con.prepareStatement(
        "UPDATE llibre SET nom=?, autor=?, `any`=?, descripcio=?, valoracio=?, preu=?," +
        " llegit=?, imatge=?, notes=?, pagines=?, pagines_llegides=?, editorial=?," +
        " serie=?, volum=?, data_compra=?, data_lectura=?, idioma=?, format=?, desitjat=?" +
        " WHERE ISBN=?");
    ps.setString(1,  l.getNom());
    ps.setString(2,  l.getAutor() != null ? l.getAutor() : "");
    ps.setInt(3,     l.getAny() != null ? l.getAny() : 0);
    ps.setString(4,  l.getDescripcio() != null ? l.getDescripcio() : "");
    ps.setDouble(5,  l.getValoracio() != null ? l.getValoracio() : 0.0);
    ps.setDouble(6,  l.getPreu() != null ? l.getPreu() : 0.0);
    ps.setBoolean(7, Boolean.TRUE.equals(l.getLlegit()));
    ps.setString(8,  l.getImatge() != null ? l.getImatge() : "");
    ps.setString(9,  l.getNotes());
    ps.setInt(10, l.getPagines());
    ps.setInt(11, l.getPaginesLlegides());
    ps.setString(12, l.getEditorial());
    ps.setString(13, l.getSerie());
    ps.setInt(14, l.getVolum());
    String dc = l.getDataCompra(), dl = l.getDataLectura();
    if (dc != null) { try { ps.setDate(15, java.sql.Date.valueOf(dc)); } catch (IllegalArgumentException e) { ps.setNull(15, java.sql.Types.DATE); } }
    else ps.setNull(15, java.sql.Types.DATE);
    if (dl != null) { try { ps.setDate(16, java.sql.Date.valueOf(dl)); } catch (IllegalArgumentException e) { ps.setNull(16, java.sql.Types.DATE); } }
    else ps.setNull(16, java.sql.Types.DATE);
    if (l.getIdioma() != null) ps.setString(17, l.getIdioma()); else ps.setNull(17, java.sql.Types.VARCHAR);
    if (l.getFormat() != null) ps.setString(18, l.getFormat()); else ps.setNull(18, java.sql.Types.VARCHAR);
    ps.setBoolean(19, l.getDesitjat());
    ps.setLong(20, l.getISBN());
    ps.execute();
    syncAutors(l.getISBN(), l.getAutors());
}
```

Wire it:
```java
// ControladorPersistencia.java
public void actualitzarLlibre(Llibre l) throws java.sql.SQLException { sc.updateLlibre(l); }

// ControladorDomini.java
public void updateLlibre(Llibre nou, long oldISBN) throws Exception {
    if (nou.getISBN() != oldISBN) {
        if (existsLlibre(nou.getISBN()))
            throw new Exception("ISBN " + nou.getISBN() + " ja existeix");
        // ISBN changed: must delete+reinsert (FK cascade unavoidable)
        cp.eliminarLlibre(oldISBN);
        cp.afegirLlibre(nou);
    } else {
        cp.actualitzarLlibre(nou);
    }
    int pos = Collections.binarySearch(bib, new Llibre(oldISBN,"","",0,"",0.0,0.0,false,""), compararISBN);
    if (pos >= 0) bib.set(pos, nou);
}
```

**The `LlegitCheckBoxEditor` fix is simpler — just call `updateLlibre` with only `llegit` changed:**
```java
// LlegitCheckBoxEditor action listener (MostrarBibliotecaControl.java:1254):
Llibre l = ControladorDomini.getInstance().getLlibre(Long.parseLong(isbn));
if (l == null) return;
Llibre updated = /* copy l */ ...;
updated.setLlegit(newLlegit);
ControladorDomini.getInstance().updateLlibre(updated, l.getISBN());
```

---

### A-5 — Domain lookup inside table renderer: O(n²) repaint path
**Importance: 7/10**

Two inner renderers in `MostrarBibliotecaControl` call `MainFrameControl.getInstance(null).getLlibreIsbn(isbn)` per row per paint:

```java
// MostrarBibliotecaControl.java:1211 — ProgressBarRenderer
Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(isbn);

// MostrarBibliotecaControl.java:598-607 — RowFilter (called on every search bar keystroke)
Llibre l = MainFrameControl.getInstance(null).getLlibreIsbn(isbn);
```

`getLlibreIsbn` → `ControladorDomini.getLlibre(isbn)` → `Collections.binarySearch(bib, ...)`. For 100 visible rows this means 100 binary searches per render frame. During typing in the search bar this fires on every keystroke.

The data is already available: `biblio` (the `ArrayList<Llibre>`) is held by `MostrarBibliotecaControl`. Build a lookup map once when the table is set:

```java
// MostrarBibliotecaControl.java — add field:
private final java.util.HashMap<Long, Llibre> biblioByISBN = new java.util.HashMap<>();

// In setTable():
biblioByISBN.clear();
if (llibres != null) for (Llibre l : llibres) biblioByISBN.put(l.getISBN(), l);

// In renderers and RowFilter — replace:
// MainFrameControl.getInstance(null).getLlibreIsbn(isbn)
// with:
biblioByISBN.get(isbn)
```

This also eliminates the circular call to `MainFrameControl`.

---

### A-6 — `ControladorDomini.getAllLlibres()` returns internal list by reference
**Importance: 6/10**

```java
// ControladorDomini.java:85
public ArrayList<Llibre> getAllLlibres() {
    return bib;  // ← the internal sorted list
}
```

`MostrarBibliotecaControl.biblio` is assigned this reference at line 1388:
```java
biblio = ControladorDomini.getInstance().getAllLlibres();
```

`eliminarFila` then calls:
```java
if (biblio != null) biblio.removeIf(b -> b.getISBN().equals(l.getISBN()));
```

When `biblio` is the live `bib` reference, this bypasses `ControladorDomini.deleteLlibre`'s binary-search index maintenance. Currently harmless because `deleteLlibre` is called first (which removes the book correctly), so `removeIf` finds nothing. But any future reordering of calls would silently corrupt the sorted list.

**Fix:** Return a defensive copy, or make the field package-private and document it:

```java
// ControladorDomini.java:85
public ArrayList<Llibre> getAllLlibres() {
    return new ArrayList<>(bib);
}
```

If performance is a concern (large lists), return `Collections.unmodifiableList(bib)` instead.

---

### A-7 — `Config` is entirely static: untestable global state
**Importance: 5/10**

`herramienta/Config.java` is 100% static methods reading/writing `~/.biblioteca/config.properties`. Every class that uses it (`MostrarBibliotecaControl`, `ServerConect`, `UITheme`, `GaleriaCobertesPanel`, etc.) has an invisible dependency on a global singleton file. Consequences:
- Cannot have multiple config profiles without major rework (an open todo item)
- Cannot test code paths that branch on config values
- `ServerConect` reads `Config.getDbType()` at connection time — config is baked in at startup

**Fix:** This is a medium-sized refactor. Minimum viable: add a `setPropertiesForTest(Properties p)` method so tests can inject config without touching the filesystem:

```java
// Config.java — add:
private static Properties testOverride = null;

public static void setPropertiesForTest(Properties p) { testOverride = p; }

private static String get(String key, String defaultVal) {
    if (testOverride != null) return testOverride.getProperty(key, defaultVal);
    return props.getProperty(key, defaultVal);
}
// Replace all direct props.getProperty(...) calls with get(...)
```

Long-term: convert to an injectable instance. Required for multiple DB profile support.

---

### A-8 — `OpenLibraryClient` uses regex JSON parsing
**Importance: 5/10**

```java
// OpenLibraryClient.java:94
Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
```

Used to extract `title`, `autor`, `any`, `descripcio` from JSON responses. Regex JSON parsing is fragile against:
- Nested objects (description is sometimes `{"value": "..."}` not a string)
- Unicode escapes (`é`)
- Arrays where the first match is wrong key

Also: `lookupByTitle` and `lookupByAutor` are ~92% identical (identified in duplication report). Both issues addressed together:

```java
// OpenLibraryClient.java — replace both methods with:
private static Map<String, String> searchBy(String param, String value) {
    Map<String, String> r = new HashMap<>();
    try {
        String encoded = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        String json = fetch("https://openlibrary.org/search.json?" + param + "=" + encoded
            + "&limit=1&fields=title,author_name,first_publish_year,isbn");
        put(r, "title", extractString(json, "title"));
        put(r, "autor", extractArrayFirst(json, "author_name"));
        put(r, "isbn",  extractArrayFirst(json, "isbn"));
        Matcher m = Pattern.compile("\"first_publish_year\"\\s*:\\s*(\\d{4})").matcher(json);
        if (m.find()) r.put("any", m.group(1));
    } catch (Exception e) { r.put("error", e.getMessage()); }
    return r;
}
public static Map<String, String> lookupByTitle(String title) { return searchBy("title", title); }
public static Map<String, String> lookupByAutor(String autor) { return searchBy("author", autor); }
```

For proper JSON parsing, H2's jar is already on the classpath but has no JSON utility. The standard library has none. Options: add `org.json` (45 KB), or keep regex and document the limitation.

---

### A-9 — `Ejecutable` init blocks EDT unnecessarily
**Importance: 4/10**

```java
// Ejecutable.java:38
domini.ControladorDomini.getInstance();  // ← DB init on MAIN thread, before EDT starts
```

The comment says "Init DB on main thread so the EDT is never blocked waiting for disk I/O" — but H2 startup on a cold SSD can take 200-500 ms, and the window is invisible during this time. The todo.txt has a matching item: "When opening app it doesn't open until everything is loaded".

**Fix:** Show a loading frame first, then load DB asynchronously:

```java
// Ejecutable.java
EventQueue.invokeLater(() -> {
    // Apply theme first — fast
    if (Config.isDarkMode()) UITheme.setDark(true);
    UITheme.rebuildFonts(Config.getFontSize());
    // ... UIManager setup ...
    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

    MainFramePanel vista = new MainFramePanel();
    vista.showLoadingSpinner(true);
    vista.setVisible(true);

    new javax.swing.SwingWorker<Void, Void>() {
        @Override protected Void doInBackground() {
            ControladorDomini.getInstance();  // DB init off EDT
            return null;
        }
        @Override protected void done() {
            vista.showLoadingSpinner(false);
            MainFrameControl.getInstance(vista).setVisible(true);
        }
    }.execute();
});
```

---

### A-10 — `ControladorPersistencia` is a pure pass-through: dead abstraction
**Importance: 4/10**

Every method in `ControladorPersistencia` (91 lines) is:
```java
public void eliminarLlibre(long ISBN) throws java.sql.SQLException { sc.deleteLlibre(ISBN); }
public ArrayList<Tag> getAllTags() { return sc.getAllTags(); }
// ... 40 more identical delegations
```

The class adds zero logic. `ControladorDomini` could hold a `ServerConect` directly. The only value would be an interface for swapping persistence implementations — but no interface exists.

**Options:**
1. Delete `ControladorPersistencia`, have `ControladorDomini` own `ServerConect` directly.
2. Keep it but extract a `LlibreRepository` interface so it earns its existence:
```java
// interficie/LlibreRepository.java (new)
public interface LlibreRepository {
    ArrayList<Llibre> getAllLlibres();
    void afegirLlibre(Llibre l) throws SQLException;
    void eliminarLlibre(long isbn) throws SQLException;
    void updateLlibre(Llibre l) throws SQLException;
    // ... etc
}
// ControladorPersistencia implements LlibreRepository
// ControladorDomini depends on LlibreRepository (injected or via getter)
```

This enables mocking for tests and opens the door for an in-memory test repository.

---

## 3. ANTI-PATTERN SUMMARY

| Anti-pattern | Location | Severity |
|---|---|---|
| God class | `MostrarBibliotecaControl` (1620 lines) | High |
| Circular singleton | `MainFrameControl` ↔ `MostrarBibliotecaControl` | High |
| Delete+re-add as UPDATE | `DetallesLlibrePanelControl:197`, `LlegitCheckBoxEditor:1267` | **Critical (data loss)** |
| Cross-layer UI call | `ServerConect` → `DialogoError` (persistence → UI) | Medium |
| Domain lookup in renderer | `ProgressBarRenderer:1211`, `RowFilter:600` | Medium |
| Expose internal mutable list | `ControladorDomini.getAllLlibres()` returns `bib` | Medium |
| Global static config | `herramienta/Config` — all static, untestable | Medium |
| Dead abstraction | `ControladorPersistencia` — 100% pass-through | Low |
| Regex JSON parsing | `OpenLibraryClient` | Low |
| Blocking startup | `Ejecutable.java:38` — DB init before window shown | Low |

---

## 4. DEPENDENCY FLOW VIOLATIONS

```
EXPECTED:        presentacio → domini → persistencia

ACTUAL VIOLATIONS:
  ServerConect   →  herramienta.DialogoError   (persistence → UI util)
  ServerConect   →  herramienta.Config         (persistence → config — acceptable)
  MostrarBibliotecaControl → MainFrameControl  (sibling circular call via singleton)

MISSING:
  No interface between ControladorDomini and ControladorPersistencia
  No interface between ControladorPersistencia and ServerConect
  No UPDATE path in any layer
```

---

## 5. IMPROVEMENT PRIORITY ORDER

| Priority | Finding | Effort | Impact |
|---|---|---|---|
| 1 | **A-4** Add `updateLlibre` — fix data loss on edit | 2h | Fixes shelf/tag/loan loss on every book edit |
| 2 | **A-2** Replace `MainFrameControl.getInstance(null)` with `ControladorDomini.getInstance().getLlibre()` in renderers | 30 min | Eliminates circular dep + renderer bug |
| 3 | **A-5** `biblioByISBN` map in `MostrarBibliotecaControl` | 20 min | O(1) renderer lookup |
| 4 | **A-3** Remove `DialogoError` from `ServerConect`, throw instead | 1h | Clean layer separation |
| 5 | **A-6** `getAllLlibres()` return defensive copy | 5 min | Safety, no perf issue at current scale |
| 6 | **A-8** Merge `lookupByTitle`/`lookupByAutor` into one | 15 min | DRY |
| 7 | **A-9** Async DB init in `Ejecutable` | 30 min | Visible startup improvement |
| 8 | **A-7** `Config` test injection method | 15 min | Enables config-dependent tests |
| 9 | **A-10** Extract `LlibreRepository` interface | 45 min | Enables mock persistence |
| 10 | **A-1** Extract `CsvUtils`, `EstadistiquesDialog` etc. | 3h | Code organisation |
