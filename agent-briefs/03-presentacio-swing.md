---
agent_brief: 03-presentacio-swing.md
recommended_models: **Cursor Composer** (preferred); Minimax 2.7
primary_paths: `src/presentacio/`
---

# Swing UI agent

> Read `AGENTS.md` at repo root first (`make test` before marking work complete).
> Coordinator: `agent-briefs/01-coordinator.md` | Index: `agent-briefs/00-INDEX.md`

## Your role

EDT safety, renderers (no DB on paint), dialogs, controllers, SwingWorkers for heavy IO.

## Scope

All of `src/presentacio/` including renderers, detalles/, listeners.

## Out of scope

DAO SQL, API routes, herramienta internals; domain rule changes without 02-domini.


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

## Review — presentacio

PACKAGE: presentacio/  (incl. renderers/, listener/, detalles/)
================================================================================
src/presentacio/renderers/ProgressBarRenderer.java:25: CRITICAL: Renderer calls MainFrameControl.getInstance().getLlibreIsbn(isbn) -> a DB/store lookup on EVERY cell paint, on the EDT. Fix: pass the Llibre via the model (getBookAt); never query the DB in a renderer.
src/presentacio/renderers/ProgressBarRenderer.java:24: MEDIUM: uses COLUMNA_ISBN=1 (model index) as a view column in t.getValueAt(row,1); if the Cover column is hidden, view col 1 is not ISBN. Fix: read via model index + convertRowIndexToModel.
src/presentacio/renderers/CoverCellRenderer.java:57: HIGH: books with no cover (img==null) are removed from coverLoading but never cached, so every repaint re-submits getLlibreIsbn()+blob load -> endless DB/IO churn. Fix: cache an empty/sentinel marker for misses.
src/presentacio/renderers/CoverCellRenderer.java:41: MEDIUM: COLUMNA_ISBN=1 (model index) used as a view column; breaks when Cover column hidden/reordered. Fix: convert to model index explicitly.
src/presentacio/renderers/CoverCellRenderer.java:54: LOW: captured row index r is reused in a later invokeLater repaint; after sort/scroll r may point at a different row. Fix: repaint by isbn lookup or repaint whole column.
src/presentacio/renderers/SearchHighlightRenderer.java:28: MEDIUM: COLUMNA_ISBN=1 (model index) used as a view column for the loaned-book highlight; wrong column when Cover hidden. Fix: use model.getValueAt / convertColumnIndexToView.
src/presentacio/BibliotecaTableModel.java:57: HIGH: isCellEditable() always returns false, so the LlegitCheckBoxEditor on COL_LLEGIT never activates -> clicking the read/unread checkbox does nothing. Fix: return true for COL_LLEGIT.
src/presentacio/BibliotecaTableModel.java:59: MEDIUM: getValueAt rebuilds the entire row Object[] (rowToValues) on every single cell access -> O(columns) allocation per cell each repaint. Fix: index the field per column directly.
src/presentacio/TableController.java:105: MEDIUM: addRowSorterListener is attached before the model is set; the later setModel() auto-creates a NEW RowSorter, silently discarding the listener -> sort-column persistence stops working after first data load. Fix: install model first or re-attach after each setModel.
src/presentacio/TableController.java:88: LOW: Ctrl-click author filter reads t.getValueAt(row, COL_AUTOR) with COL_AUTOR (model index) as a view column. Fix: resolve via model index.
src/presentacio/MainFrameControl.java:63: HIGH: btnNouLlibre listener runs obrirNouLlibreDialeg on new Thread(...), constructing+setVisible()ing a modal Swing dialog off the EDT (also lines 68, 74). Fix: build/show dialogs on the EDT; only background work off-EDT.
src/presentacio/MainFrameControl.java:198: HIGH: obrirNouLlibreDialeg constructs GuardarLlibresDialogo and setVisible(true) on a non-EDT thread. Fix: marshal dialog creation/show onto the EDT.
src/presentacio/MainFrameControl.java:210: HIGH: getLlibreIsbn delegates to cLlibres.getLlibre (DB) and is called from renderers on the paint path -> per-paint DB queries. Fix: avoid DB lookups in render paths.
src/presentacio/detalles/control/DetallesLlibrePanelControl.java:157: HIGH: startImatgeWorker submits the loader then immediately calls imageFuture.get() inside invokeLater on the EDT -> blocks the EDT until image/blob loads, defeating the async design. Fix: use SwingWorker / call back via invokeLater after get() completes off-EDT.
src/presentacio/detalles/control/DetallesLlibrePanelControl.java:57: MEDIUM: cLlibres.loadHeavyFields(l) runs synchronously on the EDT during dialog construction. Fix: load on a background thread before showing.
src/presentacio/detalles/control/DetallesLlibrePanelControl.java:86: MEDIUM: getDistinctAutorNames()/getDistinctValues() (DB queries) run on the EDT during construction for autocomplete (lines 86-89). Fix: fetch off-EDT and attach when ready.
src/presentacio/ImportController.java:35: HIGH: importarCSV runs BookImporter.importCSV (file IO + DB writes) synchronously on the EDT. Fix: run in a SwingWorker with progress.
src/presentacio/ImportController.java:62: HIGH: importarCalibre runs the sqlite3 subprocess + DB import on the EDT. Fix: move to a background worker.
src/presentacio/ImportController.java:76: HIGH: importarJSON parses file + writes DB on the EDT. Fix: background worker.
src/presentacio/ExportController.java:35: HIGH: exportCSV/exportJSON/exportHTML/exportPDF perform file IO + DB reads on the EDT (lines 35, 45, 60, 67). Fix: run exports in a SwingWorker.
src/presentacio/ExportController.java:103: MEDIUM: fetchMissingCovers writes covers via cd.setLlibreBlob from 8 concurrent pool threads; concurrent writes through a possibly non-thread-safe connection can corrupt/serialize unexpectedly. Fix: serialize DB writes or use a connection-per-thread pool.
src/presentacio/BackupController.java:36: HIGH: backupToSQL runs on the EDT (blocking full DB dump). Fix: background worker with progress dialog.
src/presentacio/BackupController.java:53: HIGH: restoreFromSQL runs on the EDT (blocking full restore). Fix: background worker.
src/presentacio/FilterController.java:131: MEDIUM: filtrar() calls aplicarFiltres(f) (DB query) on the EDT; janks for large libraries. Fix: run the DB filter in a SwingWorker.
src/presentacio/ShelfController.java:73: MEDIUM: refreshComboLlistes issues several DB calls synchronously on the EDT. Fix: batch/cache and load off-EDT.
src/presentacio/ShelfController.java:47: LOW: onDragToShelf swallows per-isbn exceptions to System.err with no user feedback. Fix: collect and report failures.
src/presentacio/ConfiguracioDialog.java:521: MEDIUM: on empty host/user the validation 'return' only exits the Config.withBatch lambda, not the save handler -> execution continues to fire onThemeChange, prompt restart, and dispose(); a failed validation still closes the dialog. Fix: validate before withBatch and abort the whole handler on failure.
src/presentacio/ConfiguracioDialog.java:405: LOW: cd.getDbSizeBytes() (DB query) runs on the EDT during dialog construction. Fix: fetch off-EDT.
src/presentacio/GaleriaCobertesPanel.java:160: LOW: moveKeyboard calls card.scrollRectToVisible(card.getBounds()); getBounds() is in parent coordinates but scrollRectToVisible expects component-local coords -> scrolls to the wrong spot. Fix: pass new Rectangle(0,0,card.getWidth(),card.getHeight()).
src/presentacio/GaleriaCobertesPanel.java:258: LOW: hideZoomPopup removes the overlay from the layered pane but never repaints -> visual ghost. Fix: repaint the layered pane region after removal.
src/presentacio/detalles/control/OpenLibrarySearchTask.java:96: LOW: done() overwrites title/author fields unconditionally (setText) while other fields are only filled when empty -> clobbers user-typed title/author. Fix: guard title/autor with isEmpty() like the others.
src/presentacio/ModeSelectorDialog.java:76: LOW: window-close sets result=SWING but ESC just dispose()s leaving result=CANCELLED -> inconsistent dismissal semantics. Fix: make ESC and window-close behave the same.


================================================================================

## todo2 deep-dive

[FILE: src/presentacio/ExportController.java:127-154]
Problem: The ExecutorService `pool` is used but never shut down. The `pool.shutdown()` is only called inside `SwingUtilities.invokeLater` callback after all covers are fetched, but if an exception occurs before that point, the pool never shuts down. Also `pool` is a local variable - if done >= total before all tasks complete (due to exception), pool never shut down.
Code: java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8,
    r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    // pool submitted to but no guaranteed shutdown
Fix: Use try-finally or track better; use a single shared executor in CoverService instead of creating per-operation.

---


---

[FILE: src/presentacio/ImportController.java:134-157]
Problem: `fetchThread` is a daemon thread that updates the UI when done. If the user closes the dialog before the thread completes, `cancelled.set(true)` is set (line 136) and the update is skipped. But if the dialog is closed and then another dialog is opened quickly, the old `fetchThread` might still run and try to update the new dialog. This is unlikely but possible.
Code: Thread fetchThread = new Thread(() -> {
    ...
    SwingUtilities.invokeLater(() -> {
        if (!dialeg.isVisible()) return;  // checks visibility but dialog might have been replaced
        ...
    });
});
Fix: Use a WeakReference to the dialog, or use a CancellationToken pattern.

---


---

[FILE: src/presentacio/MainFrameControl.java:182-189]
Problem: `getInstance(panel, cd)` creates a new instance if `instance == null` AND `panel != null`, but doesn't check if `instance` already exists with a DIFFERENT panel. This could lead to using an old panel or the new panel being ignored.
Code: public static MainFrameControl getInstance(MainFramePanel panel, BibliotecaWriter cd) {
    if (instance == null && panel != null) instance = new MainFrameControl(panel, cd);
    return instance;
}
Fix: Either throw if instance exists and panel differs, or add a check to reuse existing instance.

---

## Backlog [1][2][3]

[1] [refactor] UITheme styleAccentButton()/styleSecondaryButton()/styleLabel() used exclusively in presentacio — consider UIComponents factory in presentacio using UITheme constants — NOT DONE
[1] [refactor] AutoCompletion class in herramienta but only used in DetallesLlibrePanel — consider whether FieldAutoComplete.attach() can replace all usages and delete AutoCompletion — DONE: AutoCompletion dead (Completer.attach never called), noted in session notes
[1] [refactor] MainFrameControl actualitzarLlibre() calls addLlibreToLlista when nuevo=true and currentLlistaId != null — shelf auto-assignment buried in callback; make explicit — NOT DONE
[1] [refactor] MainFrameControl getInstance() has 3 overloads with complex guard logic — use proper initialization-on-demand holder or document call order clearly — NOT DONE
[1] [refactor] MainFramePanel extends JFrame directly — MVC split means panel should be JPanel embedded in JFrame owned by MainFrameControl; mix of layout and window lifecycle couples view
[1] [refactor] MainFramePanel title hardcoded as "Biblioteca" — use I18n key or constant
[1] [refactor] MainFramePanel statusBar is JLabel — for screen readers has no accessible role or live region; use JPanel with accessible description
[1] [refactor] GuardarLlibresDialogo addFieldEntry/addComboEntry/addCheckEntry builder methods near-identical to DetallesLlibrePanel — extract shared FormEntryBuilder utility class
[1] [refactor] GuardarLlibresDialogo setSize(600, 720) at end of constructor overrides setPreferredSize — use pack() + enforce minimumSize in componentResized listener
[1] [refactor] GuardarLlibresDialogo missing fields vs DetallesLlibrePanel: no paisOrigen, estat, notes, nomCa/nomEs/nomEn, exemplars — new-book dialog silently drops these; add at minimum notes field
[2] [clean] MainFrameControl mostrarLlegitsRecentment() hardcodes "Cap llibre marcat com a llegit." and "Llegits" instead of I18n keys
[2] [clean] MostrarBibliotecaControl galeria context menu has hard-coded Catalan strings ("Obrir detalls", "Eliminar", "Copiar ISBN", "Confirmar eliminació") — use I18n.t() keys
[2] [clean] MostrarBibliotecaControl undoDelete() shows JOptionPane confirmation ("restaurat.") — hardcoded Catalan string not in I18n; move to I18n
[2] [clean] GestioLlistesDialog no confirmation on shelf delete — data loss risk if user misclicks; add confirmation dialog like book delete
[2] [clean] MainFramePanel title hardcoded as "Biblioteca" — use I18n key or constant; if app name changes only one place needs updating
[2] [clean] MainFramePanel MostrarBibliotecaPanel constructed eagerly in field declaration before constructor configures frame — lazy-init inside constructor
[2] [clean] MainFramePanel statusBar is JLabel — for screen readers has no accessible role or live region; use JPanel with accessible description
[2] [clean] GuardarLlibresDialogo missing fields vs DetallesLlibrePanel: no paisOrigen, estat, notes, nomCa/nomEs/nomEn, exemplars — new-book dialog silently drops these; add at minimum notes field
[2] [clean] GuardarLlibresDialogo comboFormat items string array built inline from I18n.t() calls — if format options change this array and the one in DetallesLlibrePanel must be updated in sync; extract to shared getFormatOptions() helper
