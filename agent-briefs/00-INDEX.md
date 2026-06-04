---
agent_brief: 00-INDEX
---

# Agent briefs index (Biblioteca)

Split from `tot.txt` for parallel AI work. Load **one** brief per session.

| Brief | File | Best for | Primary code |
|-------|------|----------|--------------|
| Coordinator | [01-coordinator.md](01-coordinator.md) | Minimax 2.7 — planning, triage | Cross-cutting, sessions, security overview |
| Domain + DB | [02-domini-persistencia.md](02-domini-persistencia.md) | Minimax / Composer | `src/domini/`, `src/persistencia/`, `src/interficie/` |
| Swing UI | [03-presentacio-swing.md](03-presentacio-swing.md) | **Composer** (UI-heavy) | `src/presentacio/` |
| HTTP API | [04-api-http.md](04-api-http.md) | Minimax / Composer | `src/api/`, `src/main/` |
| Import/export/tools | [05-herramienta-io.md](05-herramienta-io.md) | Minimax / Composer | `src/herramienta/` |
| QA / checkBiblio | [07-checkbiblio-qa.md](07-checkbiblio-qa.md) | Composer or Minimax | `checkBiblio/`, `run.sh`, `run2.sh` |

`06-todo2-crosscutting.md` is reserved for orphan todo2 items; currently all blocks are routed to 02–05 (skip unless the file lists blocks).

## Suggested prompts

**Minimax 2.7 (planner):** “Read `AGENTS.md` and `agent-briefs/01-coordinator.md`. Propose an order of work across briefs 02–05 and 07; do not implement yet.”

**Composer (implementer):** “Read `AGENTS.md` and `agent-briefs/03-presentacio-swing.md`. Fix CRITICAL/HIGH items in `src/presentacio/` only. Run `make test` when done.”

Swap `03` for `02`, `04`, `05`, or `07` depending on the task.

## Workflow

1. Optional: coordinator assigns issues and resolves overlaps.
2. Specialist: load your brief + `AGENTS.md` only.
3. Verify: `make compile && make test`.
4. Handoff: list `file:line` fixes; update coordinator session notes if closing backlog items.

## Severity

`CRITICAL` > `HIGH` > `MEDIUM` > `LOW`.

## Source

Generated from `tot.txt` by `scripts/split_agent_briefs.py`.
