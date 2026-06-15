---
name: loop
description: Goal-driven loop workflow for the Biblioteca project — iterate plan→act→verify against `make test` until DONE, BLOCKED, or STUCK. Use when the user says "loop", "iterate until", "keep trying", "make tests pass", "fix until green", "research in this project", or invokes a multi-step autonomous task on this codebase.
---

# Loop workflow (Biblioteca)

The `loop` subagent runs a goal-driven iteration cycle, scoped to the Biblioteca project. It already knows the three-layer MVC, the Makefile, the no-delete rule, the schema-migration rule, and the `make test` bar.

## Modes

- **goal** — generic goal-achievement on this codebase.
- **build** — run `make test`, read failures, fix, repeat until green. **This is the most useful mode here.**
- **research** — find every reference (cite `path:line`), gather evidence, answer.

The agent infers the mode. Be explicit if ambiguous.

## Recipes

### Build/test/refine

> Use the loop agent in build mode: run `make test`, fix any failures, repeat until all tests pass. Touch only what's needed to make the suite green; do not refactor unrelated code.

### Generic goal

> Use the loop agent: add a "duplicate row" action to `MostrarBibliotecaControl`. Follow the three-layer pattern (`presentacio/`, `domini/`, `persistencia/`), update `tot.txt`, and end with `make test` green.

### Research

> Use the loop agent in research mode: list every place in `src/` that still calls `DriverManager.getConnection(...)` directly. Cite `path:line` for each.

### Migrations

> Use the loop agent: add a `last_login` column to the `users` table. Add a new entry to `ServerConect.MIGRATIONS`. Do not edit the `CREATE_TABLE` literal. End with `make test` green.

## Hard rules the agent will enforce

- **Plan before code.** It will state a 2–4 bullet plan before the first edit.
- **`make test` must pass** to declare `DONE`.
- **No file deletion** without explicit user confirmation in the current message.
- **No code comments** unless you ask.
- **Migrations go in `ServerConect.MIGRATIONS`**, not by editing `CREATE_TABLE`.
- **`path:line` citations** for every code claim.
- **3 failed attempts on the same root cause = `BLOCKED`**. The agent stops and reports.

## Stop conditions

The agent ends with exactly one line:

- `DONE: <one-line summary>`
- `BLOCKED: <root cause> | <evidence> | <what I tried>`
- `STUCK: <reason>`

If you see `DONE`, check the report includes evidence `make test` passed (or the relevant subset). If it only says "I think it works", push back and ask for the command output.

## When to break a task up

A single `loop` call can run out of context on very large refactors. Signs to break up:

- The agent is making unrelated edits → split by file/module.
- The agent keeps flipping between two areas → make one area the goal, the other a follow-up.
- The task spans both `domini/` schema changes and `presentacio/` UI work → do schema first in one call, UI in another.

## Limits

- The agent has no interactive `rm` permission — it will ask before deleting.
- It does not commit. It will leave a clean working tree (or staged edits) for you to review and commit.
- It will not run the GUI in this headless environment; it relies on `make test` for verification.
</content>
</invoke>