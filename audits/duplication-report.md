# Code Duplication Audit — Biblioteca

_Generated 2026-04-25. All file paths relative to `src/`._

---

## Summary table

| # | Finding | Category | Severity | Effort |
|---|---------|----------|----------|--------|
| F1 | `ResultSet → Llibre` mapping repeated 3× | Structural | **8/10** | Medium |
| F2 | Format options array exact copy | Data | **7/10** | Trivial |
| F3 | `lookupByTitle` ≈ `lookupByAutor` | Near-duplicate | **7/10** | Low |
| F4 | `editar()` field enable/disable repeated 18× twice | Structural | **6/10** | Low |
| F5 | `crearLlibre()` vs `editar()` save block field collection | Near-duplicate | **6/10** | Medium |
| F6 | `ControladorDomini.deleteLlibre(Llibre)` vs `deleteLlibre(Long)` | Near-duplicate | **5/10** | Trivial |
| F7 | `applyTheme()` re-applies all button styles twice | Structural | **5/10** | Low |
| F8 | `UIManager.put` calls in `Ejecutable` duplicate `applyUIManager()` | Data | **4/10** | Low |
| F9 | `MainFrameControl.aplicarFiltres()` wrappers add no logic | Structural | **4/10** | Trivial |
| F10 | `LlistesDelLlibreDialog` ≈ `TagsDelLlibreDialog` boilerplate | Structural | **3/10** | High |

---

## F1 — `ResultSet → Llibre` mapping repeated 3× · **8/10**

### Where
`persistencia/ServerConect.java`

| Method | Approx lines | Notes |
|--------|-------------|-------|
| `getAllLlibres()` | 253–270 | batch loads autors after main loop |
| `getRecentlyAdded(int n)` | 444–461 | identical, no autors batch |
| `getLlibresInLlista(int llistaId)` | 518–533 | `valoracio`/`llegit` come from `ll.*` not `l.*` |

### Duplication %
`getAllLlibres` and `getRecentlyAdded`: **~95 %** identical (18 setter lines word-for-word).
`getLlibresInLlista`: **~85 %** (columns 6,8 differ).

### Fix — extract private helper

```java
// Add to ServerConect.java
private Llibre mapLlibre(ResultSet rs) throws java.sql.SQLException {
    Llibre l = new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
        rs.getInt(4), rs.getString(5), rs.getDouble(6),
        rs.getDouble(7), rs.getBoolean(8), rs.getString(9));
    l.setHasBlob(rs.getBoolean(10));
    l.setNotes(rs.getString(11));
    l.setPagines(rs.getInt(12));
    l.setPaginesLlegides(rs.getInt(13));
    l.setEditorial(rs.getString(14));
    l.setSerie(rs.getString(15));
    l.setVolum(rs.getInt(16));
    l.setDataCompra(rs.getString(17));
    l.setDataLectura(rs.getString(18));
    l.setIdioma(rs.getString(19));
    l.setFormat(rs.getString(20));
    l.setDesitjat(rs.getBoolean(21));
    return l;
}
```

Replace all three `while (rs.next())` blocks with `mapLlibre(rs)`.
Also extract the SELECT column list as a constant:

```java
private static final String LLIBRE_COLS =
    "ISBN, nom, autor, `any`, descripcio, valoracio, preu, llegit, imatge, " +
    "(imatge_blob IS NOT NULL) AS has_blob, notes, pagines, pagines_llegides, " +
    "editorial, serie, volum, data_compra, data_lectura, idioma, format, desitjat";
// getLlibresInLlista uses ll.valoracio, ll.llegit — needs its own prefix variant
```

---

## F2 — Format options array exact copy · **7/10**

### Where
- `presentacio/detalles/vista/DetallesLlibrePanel.java:98`
- `presentacio/detalles/vista/GuardarLlibresDialogo.java:225`

```java
// BOTH files contain:
new String[]{"", "Tapa dura", "Tapa blanda", "eBook", "Audiollibre"}
```

### Risk
Adding a new format type requires two edits. Easy to miss one.

### Fix — constant in `UITheme` or new `Consts` class

```java
// herramienta/Consts.java  (new file, ~5 lines)
package herramienta;
public final class Consts {
    private Consts() {}
    public static final String[] FORMATS =
        {"", "Tapa dura", "Tapa blanda", "eBook", "Audiollibre"};
}
```

Then in both dialog constructors:
```java
comboFormat = new JComboBox<>(herramienta.Consts.FORMATS);
```

---

## F3 — `lookupByTitle` ≈ `lookupByAutor` · **7/10**

### Where
`herramienta/OpenLibraryClient.java`

- `lookupByTitle(String title)` lines 38–55
- `lookupByAutor(String autor)` lines 58–75

Both methods:
1. URL-encode the input
2. Fetch `search.json?<PARAM>=<value>&limit=1&fields=title,author_name,first_publish_year,isbn`
3. Parse the same 4 fields from JSON
4. Catch the same exceptions and put `"error"` key

**Only difference:** URL parameter name (`title=` vs `author=`).

### Duplication %
**~92 %**

### Fix — extract private helper

```java
private static Map<String, String> lookupBySearchParam(String paramName, String value) {
    Map<String, String> r = new HashMap<>();
    try {
        String encoded = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        String json = fetch("https://openlibrary.org/search.json?" + paramName + "=" + encoded
            + "&limit=1&fields=title,author_name,first_publish_year,isbn");
        put(r, "title", extractString(json, "title"));
        put(r, "autor", extractArrayFirst(json, "author_name"));
        put(r, "isbn",  extractArrayFirst(json, "isbn"));
        Matcher m = Pattern.compile("\"first_publish_year\"\\s*:\\s*(\\d{4})").matcher(json);
        if (m.find()) r.put("any", m.group(1));
    } catch (Exception e) {
        r.put("error", e.getMessage());
    }
    return r;
}

public static Map<String, String> lookupByTitle(String title) {
    return lookupBySearchParam("title", title);
}

public static Map<String, String> lookupByAutor(String autor) {
    return lookupBySearchParam("author", autor);
}
```

---

## F4 — `editar()` enables 18 fields, then disables same 18 · **6/10**

### Where
`presentacio/detalles/control/DetallesLlibrePanelControl.java`

Lines 144–166: `setEnabled(true)` on 18 components (Any, Autor, ISBN, Descripcio, Nom, Portada, Preu, Valoracio, Editorial, Serie, Volum, DataCompra, DataLectura, Idioma, ComboFormat, ChckDesitjat, ChckLlegit, BtnSeleccionarImatge, TextNotes, TextPagines, TextPaginesLlegides = actually 21 fields).

Lines 200–222: identical list with `setEnabled(false)`.

### Fix — extract helper

```java
private void setFieldsEditable(boolean editable) {
    vista.getTextAny().setEnabled(editable);
    vista.getTextAutor().setEnabled(editable);
    vista.getTextISBN().setEnabled(editable);
    vista.getTextDescripcio().setEnabled(editable);
    vista.getTextNom().setEnabled(editable);
    vista.getTextPortada().setEnabled(editable);
    vista.getTextPreu().setEnabled(editable);
    vista.getTextValoracio().setEnabled(editable);
    vista.getTextEditorial().setEnabled(editable);
    vista.getTextSerie().setEnabled(editable);
    vista.getTextVolum().setEnabled(editable);
    vista.getTextDataCompra().setEnabled(editable);
    vista.getTextDataLectura().setEnabled(editable);
    vista.getTextIdioma().setEnabled(editable);
    vista.getComboFormat().setEnabled(editable);
    vista.getChckDesitjat().setEnabled(editable);
    vista.getChckLlegit().setEnabled(editable);
    vista.getBtnSeleccionarImatge().setEnabled(editable);
    vista.getTextNotes().setEnabled(editable);
    vista.getTextPagines().setEnabled(editable);
    vista.getTextPaginesLlegides().setEnabled(editable);
}
```

Replace both blocks:
```java
// enable path
if (this.vista.getBtnEditar().getText().equals("Editar")) {
    setFieldsEditable(true);
    this.vista.getBtnEditar().setText("Guardar");
}
// disable path (after successful save)
setFieldsEditable(false);
this.vista.getBtnEditar().setText("Editar");
```

---

## F5 — `crearLlibre()` vs `editar()` save block: same field collection · **6/10**

### Where
- `GuardarLlibresDialogoControl.java:135–172` (`crearLlibre()`)
- `DetallesLlibrePanelControl.java:167–199` (save block inside `editar()`)

Both read identical fields from UI (ISBN, nom, autor, any, descripcio, valoracio, preu, llegit, portada, editorial, serie, volum, dataCompra, dataLectura, idioma, format, desitjat, notes, pagines, paginesLlegides, autors list, imatgeBlob), call `LlibreValidator.checkLlibre()`, then set extended fields on the result.

### Duplication %
**~70 %** (different field sources: `vista` vs `dialeg`, slightly different null handling).

### Fix — extract to `LlibreFormReader` utility

```java
// herramienta/LlibreFormReader.java  (new utility, ~40 lines)
package herramienta;
import domini.Llibre;
import java.util.*;
import java.util.stream.*;

public final class LlibreFormReader {
    private LlibreFormReader() {}

    /**
     * Builds a validated Llibre from raw string/boolean field values.
     * Throws IllegalArgumentException on validation failure.
     */
    public static Llibre build(
            String isbnTxt, String nom, String autorTxt, String anyTxt,
            String descripcio, String valTxt, String preuTxt,
            boolean llegit, String portada,
            String editorial, String serie, String volumTxt,
            String dataCompra, String dataLectura, String idioma, String format,
            boolean desitjat, String notes, String paginesTot, String paginesLleg,
            byte[] imatgeBlob) {

        Long isbn     = isbnTxt.isEmpty()  ? null : Long.parseLong(isbnTxt);
        Integer any   = anyTxt.isEmpty()   ? null : Integer.parseInt(anyTxt);
        Double val    = valTxt.isEmpty()   ? null : Double.parseDouble(valTxt);
        Double preu   = preuTxt.isEmpty()  ? null : Double.parseDouble(preuTxt);

        Llibre l = LlibreValidator.checkLlibre(isbn, nom, autorTxt, any,
            descripcio, val, preu, llegit, portada);

        List<String> autors = Arrays.stream(autorTxt.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        l.setAutors(autors);
        l.setEditorial(editorial);
        l.setSerie(serie);
        try { l.setVolum(Integer.parseInt(volumTxt)); } catch (NumberFormatException ignored) {}
        l.setDataCompra(dataCompra);
        l.setDataLectura(dataLectura);
        l.setIdioma(idioma.isEmpty() ? null : idioma);
        l.setFormat(format.isEmpty() ? null : format);
        l.setDesitjat(desitjat);
        l.setNotes(notes);
        try { l.setPagines(Integer.parseInt(paginesTot)); } catch (NumberFormatException ignored) {}
        try { l.setPaginesLlegides(Integer.parseInt(paginesLleg)); } catch (NumberFormatException ignored) {}
        if (l.getPagines() > 0 && l.getPaginesLlegides() > l.getPagines())
            l.setPaginesLlegides(l.getPagines());
        l.setImatgeBlob(imatgeBlob);
        return l;
    }
}
```

Both callers then reduce to a single `LlibreFormReader.build(...)` call.

---

## F6 — `ControladorDomini.deleteLlibre(Llibre)` vs `deleteLlibre(Long)` · **5/10**

### Where
`domini/ControladorDomini.java:115–133`

```java
// deleteLlibre(Llibre) — lines 115–123
public void deleteLlibre(Llibre l) throws Exception {
    cp.eliminarLlibre(l);
    int pos = Collections.binarySearch(bib, l, compararISBN);
    if (pos < 0) throw new Exception("...");
    bib.remove(pos);
}

// deleteLlibre(Long) — lines 125–133
public void deleteLlibre(Long ISBN) throws Exception {
    cp.eliminarLlibre(ISBN);
    int pos = Collections.binarySearch(bib,
        new Llibre(ISBN, "", "autor", 13, "descripcio", 1.0, 3.0, false, ""), compararISBN);
    if (pos < 0) throw new Exception("...");
    bib.remove(pos);
}
```

Both: call persistence, binary-search bib, throw if missing, remove. Only difference: persistence call and search key.

### Fix — delegate one to the other

```java
public void deleteLlibre(Llibre l) throws Exception {
    deleteLlibre(l.getISBN());
}

public void deleteLlibre(Long ISBN) throws Exception {
    cp.eliminarLlibre(ISBN);
    Llibre key = new Llibre(ISBN, "", "autor", 13, "", 1.0, 0.0, false, "");
    int pos = Collections.binarySearch(bib, key, compararISBN);
    if (pos < 0)
        throw new Exception("El llibre amb ISBN: " + ISBN + " no existeix a la base de dades");
    bib.remove(pos);
}
```

---

## F7 — `applyTheme()` applies button styles twice · **5/10**

### Where
`presentacio/MostrarBibliotecaPanel.java`

Lines ~754–769 (before Nimbus reinstall):
```java
UITheme.styleAccentButton(bttnFiltrar);
UITheme.styleSecondaryButton(bttnQuitarFiltros);
UITheme.styleSecondaryButton(btnExportCSV);
// ... 11 more style calls ...
```

Lines ~795–815 (after `SwingUtilities.updateComponentTreeUI(w)`):
```java
UITheme.styleAccentButton(bttnFiltrar);   // ← exact repeat
UITheme.styleSecondaryButton(bttnQuitarFiltros);
// ... 11 more style calls ...
```

### Why both exist
Nimbus reinstall resets button colors; custom styles must be re-applied after `updateComponentTreeUI`. The first block is redundant.

### Fix — delete the first block (lines ~754–769)
Keep only the post-`updateComponentTreeUI` block. The visual result is identical; one block fewer to maintain.

---

## F8 — `UIManager.put` calls duplicated between `Ejecutable` and `applyUIManager()` · **4/10**

### Where
- `main/Ejecutable.java:44–53` (8 `UIManager.put` calls before `setLookAndFeel`)
- `herramienta/UITheme.java:133–141` (`applyUIManager()`, 8 calls)

Overlap:
```
nimbusBase, nimbusBlueGrey, control, text, nimbusFocus,
nimbusSelectionBackground, nimbusSelectedText
```

`Ejecutable` also sets `defaultFont` and `Table.alternateRowColor` which `applyUIManager()` handles differently.

### Fix — move all pre-LAF setup into `UITheme.applyUIManager()` and add `defaultFont`

```java
// UITheme.applyUIManager() — add missing keys:
UIManager.put("defaultFont",             FONT_BASE);
UIManager.put("Table.alternateRowColor", TABLE_ALT);
```

Then in `Ejecutable.java`, replace the 8 explicit `UIManager.put` calls with:
```java
UITheme.applyUIManager();
UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
UIManager.put("Table.alternateRowColor", UITheme.TABLE_ALT); // must come AFTER setLookAndFeel
```

---

## F9 — `MainFrameControl.aplicarFiltres()` wrappers add no logic · **4/10**

### Where
`presentacio/MainFrameControl.java:138–152`

```java
protected ArrayList<Llibre> aplicarFiltres(String nomAutor, ..., Boolean llegit) {
    return cLlibres.aplicarFiltres(nomAutor, ..., llegit, null);  // ← just adds null tagId
}
protected ArrayList<Llibre> aplicarFiltres(String nomAutor, ..., Boolean llegit, Integer tagId) {
    return cLlibres.aplicarFiltres(nomAutor, ..., llegit, tagId); // ← pure pass-through
}
```

The second overload is a pure pass-through. The first only adds a `null` tagId. Both exist only because `MostrarBibliotecaControl` calls `MainFrameControl.getInstance(null).aplicarFiltres(...)` rather than the domain directly.

### Fix
Delete both wrappers. Change call sites in `MostrarBibliotecaControl.filtrar()` (line ~1051):
```java
// Before:
setTable(MainFrameControl.getInstance(null).aplicarFiltres(
    nomAutor, nomLlibre, ISBN, iniciAny, fiAny, valoracioMin, valoracioMax, preuMin, preuMax, llegit, tagId));
// After:
setTable(ControladorDomini.getInstance().aplicarFiltres(
    nomAutor, nomLlibre, ISBN, iniciAny, fiAny, valoracioMin, valoracioMax, preuMin, preuMax, llegit, tagId));
```

---

## F10 — `LlistesDelLlibreDialog` ≈ `TagsDelLlibreDialog` boilerplate · **3/10**

### Where
- `presentacio/detalles/vista/LlistesDelLlibreDialog.java` (202 lines)
- `presentacio/detalles/vista/TagsDelLlibreDialog.java` (180 lines)

Both dialogs:
- Take `(Window owner, Llibre llibre)` constructor
- Build a `JTable` + `JScrollPane` in `BorderLayout.CENTER`
- Have `reloadComboAdd()` that preserves selection
- Have `reload()` that repopulates `tableModel` from domain
- Have an "add" button that calls domain then `reload()`
- Have a "remove selected" button

**Key differences:** LlistesDelLlibreDialog has 3 columns (nom, valoració, llegit) and editable cells; TagsDelLlibreDialog has 1 column (etiqueta), read-only.

### Why effort is HIGH
The domain operations are different enough (llista membership with valoracio/llegit vs simple tag link) that a shared abstract base class would need significant generics. The benefit is low given these two dialogs are unlikely to grow.

### Partial fix — extract `reloadComboAdd()` pattern
Both have identical combo-preserve logic:
```java
private <T> void reloadCombo(JComboBox<T> combo, java.util.function.Supplier<List<T>> loader,
                             java.util.function.ToIntFunction<T> idOf) {
    T prev = combo.getSelectedItem() != null ? (T) combo.getSelectedItem() : null;
    combo.removeAllItems();
    for (T item : loader.get()) combo.addItem(item);
    if (prev != null) {
        int prevId = idOf.applyAsInt(prev);
        for (int i = 0; i < combo.getItemCount(); i++)
            if (idOf.applyAsInt(combo.getItemAt(i)) == prevId) { combo.setSelectedIndex(i); break; }
    }
}
```

Not worth extracting unless a third similar dialog is added. Mark as **deferred**.

---

## Prioritized action list

| Priority | Finding | File(s) | Effort |
|----------|---------|---------|--------|
| 1 | F2 — Format constants | DetallesLlibrePanel, GuardarLlibresDialogo | ~10 min |
| 2 | F3 — lookupByTitle/Autor merge | OpenLibraryClient | ~15 min |
| 3 | F6 — deleteLlibre delegation | ControladorDomini | ~5 min |
| 4 | F4 — setFieldsEditable helper | DetallesLlibrePanelControl | ~10 min |
| 5 | F9 — remove MainFrameControl wrappers | MainFrameControl, MostrarBibliotecaControl | ~10 min |
| 6 | F7 — remove redundant style block | MostrarBibliotecaPanel | ~5 min |
| 7 | F1 — mapLlibre() helper + LLIBRE_COLS constant | ServerConect | ~30 min |
| 8 | F8 — consolidate UIManager setup | Ejecutable, UITheme | ~15 min |
| 9 | F5 — LlibreFormReader utility | GuardarLlibresDialogoControl, DetallesLlibrePanelControl | ~45 min |
| 10 | F10 — LlistesDelLlibreDialog / TagsDelLlibreDialog | Both dialog files | Deferred |

**All fixes except F5 and F10 are drop-in with no architectural change.**
Run `make test` after each fix. Expected: 63/63.
