# Fuzzing

Two complementary fuzzers live under `test/fuzz/`. Use them when a hand-
written unit test would have to enumerate the inputs (a closed-form
contract) and you want to demonstrate the property holds across the
input space.

## When to use which

| Tool         | Strength                                                | Use for                                       |
|--------------|---------------------------------------------------------|-----------------------------------------------|
| jqwik        | Round-trip invariants, algebraic properties, generators | State-shaped code (CD, Llibre, Llista, Tag)   |
| Jazzer       | Adversarial / coverage-guided, all `String` / `byte[]`  | Stream / parser code (CSV, JSON, SQL execute) |

They do not overlap. Pick jqwik when the failure is a logical
invariant ("after add+delete the size is back to N"); pick Jazzer when
the failure is an uncaught exception, NPE, OOM, or stack overflow on
crafted input.

## Adding a property (jqwik)

1. Add a method in `test/fuzz/domini/LlibreInvariantPropertiesTest.java`
   (or a new class in `test/fuzz/domini/`). Annotate it with
   `@Property(tries = 20)` and one `@ForAll` per parameter.
2. Reset both singletons in `@BeforeEach` / `@AfterEach`, exactly like
   `test/domini/StressDominiTest` does. The property runs inside its
   own H2 instance, so state from one try must not leak into the next.
3. Use tight generators (`@LongRange`, `@StringLength`, `@NotEmpty`).
   Avoid unbounded strings — 60 chars is plenty for round-trip tests.
4. Keep each try under ~50 ms. `tries = 20` × 5 properties finishes
   well inside the 5 s budget; bump `tries` only if the property is
   in-memory.

Run with `make fuzz-property` (or `scripts\fuzz-property.bat` on
Windows). The same class is also picked up by `make test` in
regression-only mode.

## Adding a harness (Jazzer)

1. Add a class in `test/fuzz/herramienta/` (parsers, I/O) or
   `test/fuzz/domini/` (pure-function logic). Annotate the method
   with `@FuzzTest(maxDuration = "30s")`. `maxDuration` is the runtime
   cap; bump it for harnesses that need more time to reach deep
   branches.
2. The single parameter is `FuzzedDataProvider data`. Call
   `data.consumeRemainingAsBytes()` for byte-stream targets,
   `data.consumeString(65536)` for parser targets, or primitive
   combinators (`data.consumeInt(0, 100)`, `data.consumeBoolean()`)
   for API fuzzing.
3. Let only the contract exceptions escape (`IOException`,
   `NoSuchElementException`). Assert no NPE / OOM / StackOverflow.
4. For an infinite-loop cap, count iterations in the harness body and
   return after, say, 10_000 — otherwise a buggy `hasNext()` will hang
   the fuzzer.
5. **Add seed corpus** to `.cifuzz-corpus/fuzz.<package>.<Class>/fuzz/`
   — at least a few representative inputs. Without seeds Jazzer
   starts blind and coverage growth stalls within a few hundred runs.

Run with `make fuzz-jazzer` (or `scripts\fuzz-jazzer.bat`). The
`JAZZER_FUZZ=1` env var switches Jazzer from regression mode (one
run, deterministic) to coverage-guided fuzzing. Without the env var
the `@FuzzTest` runs as a normal JUnit test, so the same suite is
safe to run under `make test`.

To run a single harness:
```
make fuzz-jazzer JARZER_SEL=fuzz.domini.SortSpecFuzzTest
```

## Nightly run

`scripts/fuzz-nightly.sh` runs every harness in parallel, each
iterating with the accumulated corpus, until one crashes or you hit
Ctrl+C. Logs land in `/tmp/jazzer/YYYYMMDD-HHMMSS.log` (with
`/tmp/jazzer/latest.log` symlinked to the most recent). Crashes are
moved to `fuzz-corpus/crashes/`.

```
# Foreground (Ctrl+C to stop)
scripts/fuzz-nightly.sh

# Background, watch the log
nohup scripts/fuzz-nightly.sh &
tail -f /tmp/jazzer/latest.log
```

Each iteration of a single harness is capped at 30 s by the
`@FuzzTest(maxDuration = "30s")` annotation; bump it for harnesses
that need longer to reach deep branches.

## Triaging a finding

When Jazzer prints a crash, the inputs are saved under
`fuzz-corpus/<package>.<Class>/<method>/crash-<sha>.bin` (or the
configured `inputs/` dir). To reproduce:

```
JAZZER_FUZZ=0 java -jar lib/junit-platform-console-standalone-1.11.4.jar \
    execute --select-class=fuzz.herramienta.Rfc4180FuzzTest \
    --classpath bin:$(TEST_CP) \
    -Djazzer.internal.arg.0=repro \
    -Djazzer.internal.arg.1=fuzz-corpus/.../crash-XXX.bin
```

Or simpler: write a regular `@Test` that calls the harness body with
the seed bytes, and run it under `make test` to debug.

## Files

- `test/fuzz/domini/LlibreInvariantPropertiesTest.java` — 5 jqwik
  properties on the domini facade.
- `test/fuzz/herramienta/Rfc4180FuzzTest.java` — Jazzer harness for
  the streaming CSV reader.
- `test/fuzz/herramienta/CsvUtilsFuzzTest.java` — Jazzer harness for
  `CsvUtils.parseLine`.
- `test/fuzz/domini/LlibreFilterBuilderFuzzTest.java` — Jazzer
  harness for the `LlibreFilterBuilder` + `SortSpec` round-trip.
- `test/fuzz/domini/SortSpecFuzzTest.java` — Jazzer harness for
  `SortSpec` (column coercion, SQL direction, comparator).
- `test/fuzz/domini/ShelfParserFuzzTest.java` — Jazzer harness for
  `ShelfParser.parseShelfEntries` + round-trip with `joinShelfEntries`.
- `scripts/fuzz-nightly.sh` — parallel runner, dated logs, crash
  capture, clean Ctrl+C.
- `lib/jqwik-*.jar`, `lib/jazzer-*.jar` — downloaded by
  `make fuzz-deps`, .gitignored.
- `.cifuzz-corpus/` — Jazzer's persistent corpus, .gitignored.
- `fuzz-corpus/` — Crash files (moved here on detection),
  .gitignored.

## House rules

This project does not delete files. If a fuzzer finds a finding in
production code (`src/...`), do not `rm` the offending file — file
the bug, write a regression test, and let the maintainer fix it. See
`AGENTS.md` § "File deletion".
