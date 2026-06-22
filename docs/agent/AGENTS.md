# AGENTS.md

## Plan before code (required)

**Do not write or edit production code until you have a short plan the user can follow.** This is the highest-priority rule for every task, including small fixes.

Before the first line of code:

1. **Understand** — Read relevant files and trace how the feature or bug fits the MVC layers (`presentacio/`, `domini/`, `persistencia/`).
2. **Plan** — State goal, files to touch, approach, risks (migrations, singletons, tests), and how you will verify (`make test`).
3. **Confirm scope** — If requirements are ambiguous or the change is large, align with the user before implementing.

Only then implement. If exploration reveals the plan was wrong, stop, revise the plan, then continue.

Trivial exceptions (user explicitly asks to skip planning): one-line typo, comment-only edits, or a change they fully specified file-by-file.

## Backlog

The single source of truth for open work is **`tot.txt`** at the repo root. It includes current DONE / OPEN status for every phase, bug, anti-pattern, and cross-cutting item.

## Mistake log

Read **`docs/agent/MISTAKES.md`** at the start of any non-trivial task. It is a personal memory of past mistakes (escape conventions, generator vs hand-edited files, deletion guardrails, vague-report handling) — short, table-based, optimized to scan fast. Append new entries at the bottom.

## Loop delegation

The `loop` subagent (`.opencode/agent/loop.md`) and the `loop` skill (`.opencode/skill/loop/SKILL.md`) exist for autonomous, multi-step, verify-driven work. The agent already knows the project rules (MVC layers, no-delete, schema migrations, `make test` bar). Delegate to it instead of doing the work inline.

**Delegate when the task is:**
- Multi-step — 3+ file edits, a chain of fix-and-verify cycles, or a refactor across layers.
- Autonomous — no need to re-prompt the user between steps.
- Verification-driven — `make test` (or a focused subset) is the success signal.

**Do not delegate when:**
- It's a single, small edit or a one-line fix — do it inline.
- The plan needs user approval first — get the plan, then maybe delegate.
- The user wants a read-only answer — use the `explore` agent or the `loop` subagent in `research` mode.
- The task is "loop" / "iterate until" / "fix until green" / "make tests pass" — these are explicit triggers; delegate immediately.

**How to invoke:** call the task tool with `subagent_type: "loop"` and the natural-language task as the prompt. The agent infers the mode (`goal` / `build` / `research`); be explicit if ambiguous — e.g. *"loop in build mode: run `make test`, fix any failures, repeat until green."*

**Trust the hard stops:** the agent ends with exactly one of `DONE: …`, `BLOCKED: …`, `STUCK: …`. `DONE` is authoritative only if the report shows verification (e.g. `make test` output). `BLOCKED` and `STUCK` need a follow-up decision from you. Never accept "I think it works".

For very large refactors, split into multiple `loop` calls by module or layer rather than one huge run — a single call can exhaust context.

## Concurrent subagents (parallel work in one turn)

Main agent only exists during a turn. Between user prompts it is dead — no work happens. To run work in parallel, **launch multiple subagents in a single message** (one user turn, multiple `task` tool calls). Each subagent runs concurrently; main waits for all to return before responding.

**When to fan out:**
- `loop` (build/fix) + `explore` (read-only research for the next refactor).
- Two `loop` calls on **disjoint** file sets or modules.
- `loop` + `general` reviewer (one builds, one reads the diff and reports).

**Don't:**
- Two `loop` calls editing the same files — they race and corrupt state. Split by module first.
- Treat parallelism as a substitute for planning — concurrent subagents still need a clear scope each.

**Example prompt shape:** "Run two tasks in parallel: (1) `loop` in build mode to fix the `Llibre` CRUD bugs in `domini/Llibre.java` + `persistencia/LlibreDAO.java`; (2) `explore` to list every place in `presentacio/` that still calls the deprecated `ControladorDomini.refresh()` overload. Report both."

**Limits:** token cost multiplies with concurrency. 2–3 subagents per turn is usually the ceiling before context returns get unwieldy.

## Build & Test Commands

The Makefile is the source of truth on Linux/macOS. On Windows, use the
matching `.bat` files in `scripts/` (they use the same classpath layout
with `;` separators and `dir /s /b` instead of `find`).

### Linux / macOS

```bash
make compile          # compile src/ → bin/ (uses classes.txt, not glob)
make test             # runs BibliotecaTest (plain Java) + BibliotecaJUnit5Test (JUnit 5)
make clean            # rm -rf bin/*
make run              # clean + compile + run GUI
make run-only         # run without recompile
./checkBiblio/run.sh           # tests + UIAudit (uses Xvfb if no DISPLAY)
./checkBiblio/run2.sh          # compile + StressTest (uses Xvfb if no DISPLAY)
```

### Windows (cmd / PowerShell)

```bat
scripts\compile.bat            # compile src/ → bin\ (uses classes.txt, not glob)
scripts\test.bat               # runs BibliotecaTest (plain Java) + BibliotecaJUnit5Test (JUnit 5)
scripts\trace_run.bat          # ad-hoc TraceRoundtrip with H2 in-memory DB
powershell -File checkBiblio\run.ps1            # tests + UIAudit (no Xvfb needed)
powershell -File checkBiblio\run2.ps1           # compile + StressTest
```

Run from the project root. `test.bat` calls `compile.bat` first, so a clean
clone only needs `scripts\test.bat`. Requires `python` in PATH for the
post-removal test patch (it is a no-op if already applied). The `run.ps1`
/ `run2.ps1` PowerShell scripts mirror the Linux `checkBiblio/run*.sh`
scripts; they assume a real Windows display (UIAudit/StressTest use
`java.awt.Robot` and abort on `GraphicsEnvironment.isHeadless()`).

**Always run `make test` (Linux) or `scripts\test.bat` (Windows) before
reporting a task complete. All tests must pass.**

After removing web/API (`src/api/`, mode picker), local `test/*.java` may still reference deleted types. `make test` / `scripts\test.bat` runs `scripts/patch_tests_after_web_removal.py` first (idempotent); or run it manually once per machine.

Manual classpath: `lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:.`

## Test System Properties

- `-Dbiblioteca.test=true` — makes DB errors throw instead of showing GUI dialogs
- `-Dbiblioteca.h2.url="jdbc:h2:mem:junit5;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"` — H2 in-memory URL for JUnit 5 tests
- Between test groups, call `ControladorDomini.resetForTest()` and `ControladorPersistencia.resetForTest()` to reset singletons

## Database Migrations

Schema versioned in `ServerConect.MIGRATIONS` (version via `schema_version` table). **Never alter the `CREATE_TABLE` string** — add new migrations as new entries in the array instead.

## Config

All runtime config in `~/.biblioteca/config.properties` (dbType, darkMode, fontSize, window geometry, column widths, etc.)

## Architecture Notes

- Three-layer MVC: `presentacio/` (Swing), `domini/` (business logic), `persistencia/` (JDBC)
- `MainFrameControl` is the root singleton; `MostrarBibliotecaControl` manages the main table
- Each screen has a `*Panel` (Swing layout) + `*Control` (listeners + domain calls)
- `test/` is gitignored — tests are local-only

## File deletion

**Never delete or remove a file without asking the user first and getting explicit approval.** This includes the Delete tool, shell `rm`/`del`, `git rm`, and cleanup of generated or temporary files. Name the file(s) and reason, then wait for confirmation. Only skip asking when the user has explicitly requested that specific deletion in the current message.

## Misc

- Language: Catalan/Spanish mixed
- `lib/junit-platform-console-standalone-1.11.4.jar` used for JUnit 5 console launch
- `opencode.json` sets `instructions: ["AGENTS.md"]`, `lsp`, and bash `rm` permissions — most config is in Makefile
- **Scripts**: keep them simple and to the point. Upgrade later if needed — don't pre-build flexibility.
