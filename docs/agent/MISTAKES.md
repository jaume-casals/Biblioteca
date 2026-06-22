# MISTAKES

Append new entries at the bottom. Scan headers first; read full section only if relevant.

## Escapes (CSV / .properties / Java source)

CSV in this repo uses `\n` (2 chars) to mean *newline* — it's not a real byte, it's the escape. Same for `\t`, `\"`, `\\`.

| source bytes | .properties reads as | javac reads as |
|---|---|---|
| `\n` (2)  | newline | newline |
| `\\n` (3) | `\n` (2 literal chars) | `\n` (2 literal chars) |
| `\"` (2)  | `"` | `"` |
| `\\\"` (3) | `\"` (2 literal) | `\"` (2 literal) |

**Rule for any regen script**: preserve `\X` escapes (2 chars) as `\X` (2 chars) in the output. Do NOT pre-double all backslashes — that converts every escape into a literal.

## I18n.java is hand-edited

`src/herramienta/i18n/I18n.java` = generator output + heavy hand-edits. Custom parts:
- regex arg substitution in `t()`
- `langIndex()` with unknown-lang fallback
- `aplicarSwingOptionPane()`
- manual keys: `dlg_restore_done_with_note`, `tip_filter_substring`, `dlg_fetch_portades_done_partial`, `dlg_perfil_nom_invalid[_title]`, `filter_dash`

**Never** `python3 scripts/gen_strings.py` to regen it without diffing first — wipes everything above. Safer: edit in place for one-line fixes; only regen the 3 `.properties` files.

## Guardrails

- `rm` / `del` → state file + reason, wait for OK (per AGENTS.md). Even for files I just created.
- After any regen → `git diff --stat`. Drop of 50+ lines = custom code wiped, abort.
- Before any regen → try on 1 line, `od -c` to verify bytes changed as intended.

## Vague user reports

"I saw X in some text" → ask for exact text/path before grepping. `/n` in user speech usually means the literal `\n` (backslash+n) in UI text — not a slash.

Trigger to ask: report < 1 sentence, no file/line, mentions multiple places.

## Python escape levels in scripts

`'\\n'` (in Python source) = 1-char newline byte. `'\\\\n'` = 2-char `\n`. `'\\\\\\"'` = 3-char `\"`. When writing a find-and-replace, both old AND new must differ — equal `\\` count in both columns = no-op, script silently does nothing.

If `od -c` after the edit shows unchanged bytes → escape levels wrong in the script, not the logic. Fix the literals.

## Token waste in my own output

- **No postamble.** Never end a response with "I've updated X. The fix involved Y..." — user reads the diff, not the prose. State result in ≤ 1 sentence.
- **No "I think / probably / maybe"** in technical answers. Either I know or I read first.
- **Don't re-read files I just wrote/edited** to "verify" — `git diff` is cheaper. Read only when source-of-truth is unclear.
- **Batch parallel reads** with one multi-tool-call message, not serial. Never `cd && cmd` — use `workdir=`.
- **`replaceAll` over N single edits** when the pattern is unique per file. One Edit > N Edits.
- **Don't Glob/Grep + Read for files I just touched** — `git status` + `git diff` shows what changed.
- **Skip the explanation** unless the user asked "why" or the change is non-obvious. Code comments are also forbidden unless asked.

## Bad code patterns

- **No comments in code** (system rule). Even Javadoc on new public methods unless the file already has them.
- **No invented method signatures.** Before calling `obj.foo()` on a class I haven't read, grep for `foo` in that class file. Same for constructors.
- **Match existing style**: indentation (tabs vs spaces — this repo uses tabs), naming (camelCase for Java, snake_case for Python), accessor prefix (Catalan: `obtenirX`, `definirX`).
- **No defensive null checks the project doesn't use.** Read 2-3 sibling files first to learn the style. Don't add try/catch around things that throw unchecked throughout the codebase.
- **No premature abstraction.** Don't extract a helper for one call site. Don't introduce an interface for a single impl.
- **`replaceAll` with `replaceAll`** only when regex is needed; default to `replace` (literal) — faster and safer for `\\` / `.` / `(`.

## Common LLM errors

- **Hallucinated paths / APIs.** If unsure a file/class exists, Glob or Grep before referencing. Never "this should be at X" without verifying.
- **Run-n-fix loop without tests.** After every non-trivial change: `make test` before reporting done. Don't batch 5 edits then test once — one bad edit masks the others.
- **Plan-then-implement skipped** for non-trivial tasks. AGENTS.md requires it. Trivial exceptions: typo, comment-only, user fully specified file-by-file.
- **Asking the user what they meant** is cheaper than grepping for 10 min on a vague report. When report is < 1 sentence, no file/line, says "and others" → ask first, don't grep.
- **Trusting my own compile output** without reading the errors. `make test` exit 0 ≠ green if I ignored warnings. Read the bottom of stderr.
- **Concurrency races**: two subagents editing the same file → corrupt state. Split by module first.

## `gen_strings.py` overwrites hand-edited `I18n.java`

Running `python3 scripts/gen_strings.py` to "see what it produces" silently overwrites `src/herramienta/i18n/I18n.java` and wipes the hand-edited parts (regex arg substitution, `langIndex()` unknown-lang fallback, `aplicarSwingOptionPane()`, `herramienta.config.Configuracio` import, manual keys listed above). `git diff --stat` may look small (≈30+/70−) and tempt you to keep it — DON'T. Always `git checkout -- src/herramienta/i18n/I18n.java` after any exploratory run, even if the script "succeeded".

Safe alternative for one-line string fixes: edit the CSV AND the `T(...)` line in `I18n.java` by hand. For batch `.properties` regen only, run the script and revert the `.java` change (`git checkout -- src/herramienta/i18n/I18n.java`).

## Test class without `-Dbiblioteca.h2.url` leaks into the live DB

Any `*Test.java` that calls `ControladorDomini.getInstance()` / `ControladorPersistencia.getInstance()` MUST have the in-memory URL initializer — otherwise it falls through to `Configuracio.obtenirDbType()` and writes to `~/.biblioteca/biblioteca.mv.db` (the user's real library).

Required pattern (mirrors `BibliotecaJUnit5Test.java:36-40`):

```java
class MyTest {
    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:<unique_name>;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }
    ...
}
```

The `static {}` runs on class load — BEFORE any JUnit runner — so it's the only defense when IDE / standalone `java -cp ... MyTest` runs a single method without the `-D` flags the Makefile loop sets. `make test` sets the property globally, so this leak only manifests when bypassing it.

Audit command: `cd test && for f in $(grep -L "biblioteca.h2.url" $(find . -name "*.java")); do grep -l "ControladorDomini\|afegirLlibre\|afegirTag\|afegirLlista\|getInstance()\|netejarAll" "$f" 2>/dev/null; done` — every output line is a leak waiting to happen. Real bug example: `test/DominiPersistenciaJUnit5Test.java` was missing the block and was silently writing "Effective Java" / "Clean Code" / "Fluent Python" / a literal "Test"/"Author" entry + tags + shelves into the live H2 file whenever the class was run outside `make test`.

## `printf` on boxed nullables swallows data silently

`AnalitzadorPrestatgeria.exportarToCsv` called `pw.printf("%d", l.obtenirAny())` directly on a getter that returns boxed `Integer` / `Double` / `Boolean`. Any null field → NPE → caught by the per-row `try/catch` → row dropped, no warning. The CSV ended up with header only; user saw the "Export completed" success dialog and an empty file. **Rule**: coalesce nulls to defaults (0 / 0.0 / false) before `printf` for any nullable getter. Same audit applies to `String.valueOf(l.obtenirAny())` in `ExportadorLlibres.exportarPDF` (line 193, before the fix) — `String.valueOf(null)` returns the literal `"null"`, which gets embedded in the printed output.

## `make test` discovery uses `*Test.java` — names ending `Junit5.java` / `Safe.java` get silently skipped

`Makefile` line ~100: `find ./test/ -name "*Test.java"`. Files named `TestXxxJunit5.java`, `TestXxxSafe.java`, `TestXxxPertinençaPrestatgeria.java` etc. never run. The existing `TestRestaurarSqlJunit5.java` was already silently broken (not running) before this entry. **Rule when adding a JUnit 5 test class**: confirm the basename ends with `Test.java` (`XxxTest.java` or `XxxJunit5Test.java`, not `XxxJunit5.java`). Verify with `find ./test -name "*Test.java" | grep <new file>` — if it is missing from the output, the file is invisible to `make test`.
