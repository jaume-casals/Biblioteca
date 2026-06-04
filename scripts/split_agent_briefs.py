#!/usr/bin/env python3
"""Split tot.txt into agent-briefs/*.md for parallel AI agents."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
TOT = ROOT / "tot.txt"
OUT = ROOT / "agent-briefs"

HEADER = """---
agent_brief: {id}
recommended_models: {models}
primary_paths: {paths}
---

# {title}

> Read `AGENTS.md` at repo root first (`make test` before marking work complete).
> Coordinator: `agent-briefs/01-coordinator.md` | Index: `agent-briefs/00-INDEX.md`

## Your role

{role}

## Scope

{scope}

## Out of scope

{oos}

"""


def slice_lines(lines, a, b):
    return "\n".join(lines[a:b]).strip() + "\n"


def find_line(lines, needle, start=0):
    for i in range(start, len(lines)):
        if needle in lines[i]:
            return i
    return None


def route_todo2(block: str) -> str:
    m = re.search(r"\[FILE:\s*(src/[^:\]]+)", block)
    if not m:
        return "crosscutting"
    path = m.group(1)
    if any(x in path for x in ("/domini/", "/persistencia/", "/interficie/")):
        return "domini"
    if "/presentacio/" in path:
        return "presentacio"
    if "/api/" in path or "/main/" in path:
        return "api"
    if "/herramienta/" in path:
        return "herramienta"
    return "crosscutting"


def route_priority(ln: str) -> str:
    low = ln.lower()
    if any(
        x in low
        for x in (
            "mainframepanel",
            "mainframecontrol",
            "guardarllibres",
            "detalles",
            "mostrarbiblioteca",
            "gestiollistes",
            "llistesdellibre",
            "configuraciodialog",
            "modeselector",
            "galeria",
            "tablecontroller",
            "presentacio",
        )
    ):
        return "presentacio"
    if any(
        x in low
        for x in (
            "llibrerouter",
            "httprouter",
            "httpctx",
            "configrouter",
            "tagrouter",
            "loanrouter",
            "listarouter",
            "importexportrouter",
            "backuprouter",
            "apiserver",
            "jsonmapper",
        )
    ):
        return "api"
    if "run.sh" in low or "run2.sh" in low or "uiaudit" in low:
        return "checkbiblio"
    if any(
        x in low
        for x in (
            "bookexporter",
            "bookimporter",
            "csv",
            "goodreads",
            "openlibrary",
            "shelfparser",
            "dateutils",
            "dialogoerror",
            "i18n",
            "backupservice",
            "nativecsv",
            "autocompletion",
            "install.sh",
        )
    ):
        return "herramienta"
    if any(
        x in low
        for x in (
            "llistedao",
            "serverconect",
            "migration",
            "domini",
            "controladordomini",
            "llibre.java",
        )
    ):
        return "domini"
    return "coordinator"


def write_brief(fid, title, models, paths, role, scope, oos, sections):
    body = HEADER.format(
        id=fid,
        models=models,
        paths=paths,
        title=title,
        role=role,
        scope=scope,
        oos=oos,
    )
    for title_s, content in sections:
        if content and content.strip():
            body += f"\n## {title_s}\n\n{content.strip()}\n"
    OUT.joinpath(fid).write_text(body, encoding="utf-8")


def main():
    lines = TOT.read_text(encoding="utf-8").splitlines()
    todo1_start = find_line(lines, "# SOURCE: todo1.txt")
    todo2_start = find_line(lines, "# SOURCE: todo2.txt")
    todo3_start = find_line(lines, "# SOURCE: todo3.txt")
    combined_start = find_line(lines, "# SOURCE: COMBINED_TODO.md")
    review_start = find_line(lines, "ULTRA MEGA HYPER CODE REVIEW — src/", todo1_start or 0)

    combined_body = slice_lines(lines, combined_start + 2, todo1_start)

    packages = {}
    i = review_start
    while i < todo2_start:
        if "PACKAGE:" in lines[i]:
            key = "other"
            if "domini" in lines[i]:
                key = "domini"
            elif "persistencia" in lines[i]:
                key = "persistencia"
            elif "herramienta" in lines[i]:
                key = "herramienta"
            elif "presentacio" in lines[i]:
                key = "presentacio"
            elif "api" in lines[i]:
                key = "api"
            j = i + 1
            while j < todo2_start and "PACKAGE:" not in lines[j]:
                if lines[j].startswith(" END OF REVIEW"):
                    break
                j += 1
            packages.setdefault(key, []).append(slice_lines(lines, i, j))
            i = j
        elif "SUMMARY OF HIGHEST" in lines[i]:
            j = i
            while j < todo2_start and "PACKAGE:" not in lines[j]:
                j += 1
            packages["_summary"] = slice_lines(lines, i, j)
            i = j
        else:
            i += 1

    todo2_text = slice_lines(lines, todo2_start + 2, todo3_start)
    todo2_blocks = []
    current = []
    for ln in todo2_text.splitlines():
        if ln.startswith("[FILE:"):
            if current:
                todo2_blocks.append("\n".join(current))
            current = [ln]
        elif current:
            current.append(ln)
    if current:
        todo2_blocks.append("\n".join(current))

    todo2_by_agent = {k: [] for k in ("domini", "presentacio", "api", "herramienta", "crosscutting")}
    for b in todo2_blocks:
        todo2_by_agent[route_todo2(b)].append(b)

    todo3_body = slice_lines(lines, todo3_start + 2, len(lines))

    priority_lines = [
        ln for ln in combined_body.splitlines() if re.match(r"^\[[123]\]", ln.strip())
    ]
    priority_by = {k: [] for k in ("coordinator", "domini", "presentacio", "api", "herramienta", "checkbiblio")}
    for ln in priority_lines:
        priority_by[route_priority(ln)].append(ln)

    combined_sections = combined_body.split("=" * 80)
    session_part = []
    arch_parts = []
    priority_sections = []
    for part in combined_sections:
        p = part.strip()
        if not p:
            continue
        if "Session 2" in p or p.startswith("# Combined"):
            session_part.append(p)
        elif any(
            x in p
            for x in ("HIGH-PRIORITY", "MEDIUM-PRIORITY", "LOW-PRIORITY")
        ):
            priority_sections.append(p)
        elif any(
            x in p
            for x in ("CROSS-CUTTING", "Bugs", "Dead code", "Anti-patterns", "Security")
        ):
            arch_parts.append(p)

    OUT.mkdir(exist_ok=True)

    index = """---
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
| Cross-cutting fixes | [06-todo2-crosscutting.md](06-todo2-crosscutting.md) | Either model | Multi-package todo2 items |
| QA / checkBiblio | [07-checkbiblio-qa.md](07-checkbiblio-qa.md) | Composer or Minimax | `checkBiblio/`, `run.sh`, `run2.sh` |

## Workflow

1. Optional: coordinator assigns issues and resolves overlaps.
2. Specialist: load your brief + `AGENTS.md` only.
3. Verify: `make compile && make test`.
4. Handoff: list `file:line` fixes; update coordinator session notes if closing backlog items.

## Severity

`CRITICAL` > `HIGH` > `MEDIUM` > `LOW`.

## Source

Generated from `tot.txt` by `scripts/split_agent_briefs.py`.
"""
    (OUT / "00-INDEX.md").write_text(index, encoding="utf-8")

    write_brief(
        "01-coordinator.md",
        "Coordinator / planner agent",
        "Minimax 2.7 (planning); either model for tiny edits",
        "Whole repo (triage); `agent-briefs/`, session notes, architecture lists",
        "Plan, assign, and track DONE/NOT DONE; avoid large implementations unless blocking.",
        "Cross-cutting backlog, security overview, anti-patterns, dead-code status.",
        "Package-owned implementation without checking INDEX; duplicating specialist fixes.",
        [
            ("Sessions (done — do not redo)", "\n\n".join(session_part)),
            (
                "Priority backlog [1][2][3] (full lists)",
                "\n\n---\n\n".join(priority_sections),
            ),
            ("Architecture (bugs, anti-patterns, security, dead code)", "\n\n---\n\n".join(arch_parts)),
            ("Priority [1][2][3] — general / unassigned only", "\n".join(priority_by["coordinator"])),
        ],
    )

    write_brief(
        "02-domini-persistencia.md",
        "Domain + persistence agent",
        "Minimax 2.7; Composer for straightforward fixes",
        "`src/domini/`, `src/persistencia/`, `src/interficie/`",
        "Domain model, DAO, migrations, ISBN/long, caches, ControladorDomini threading.",
        "Everything under domini/, persistencia/, interficie/ (see review + todo2 below).",
        "Swing (`presentacio/`), HTTP (`api/`) except types shared with domain.",
        [
            ("Top issues (read first)", packages.get("_summary", "")),
            ("Review — domini + interficie", "\n".join(packages.get("domini", []))),
            ("Review — persistencia", "\n".join(packages.get("persistencia", []))),
            ("todo2 deep-dive", "\n\n---\n\n".join(todo2_by_agent["domini"])),
            ("Backlog [1][2][3]", "\n".join(priority_by["domini"])),
        ],
    )

    write_brief(
        "03-presentacio-swing.md",
        "Swing UI agent",
        "**Cursor Composer** (preferred); Minimax 2.7",
        "`src/presentacio/`",
        "EDT safety, renderers (no DB on paint), dialogs, controllers, SwingWorkers for heavy IO.",
        "All of `src/presentacio/` including renderers, detalles/, listeners.",
        "DAO SQL, API routes, herramienta internals; domain rule changes without 02-domini.",
        [
            ("Top issues (read first)", packages.get("_summary", "")),
            ("Review — presentacio", "\n".join(packages.get("presentacio", []))),
            ("todo2 deep-dive", "\n\n---\n\n".join(todo2_by_agent["presentacio"])),
            ("Backlog [1][2][3]", "\n".join(priority_by["presentacio"])),
        ],
    )

    write_brief(
        "04-api-http.md",
        "HTTP API + main agent",
        "Minimax 2.7; Composer",
        "`src/api/`, `src/main/`",
        "HttpCtx lifecycle, auth/CSRF, status codes, CORS, static traversal, router validation.",
        "All of `src/api/` and `src/main/Ejecutable.java`.",
        "Swing UI; DAO implementation (coordinate with 02); CSV/ISBN in herramienta (05).",
        [
            ("Top issues (read first)", packages.get("_summary", "")),
            ("Review — api + main", "\n".join(packages.get("api", []))),
            ("todo2 deep-dive", "\n\n---\n\n".join(todo2_by_agent["api"])),
            ("Backlog [1][2][3]", "\n".join(priority_by["api"])),
        ],
    )

    write_brief(
        "05-herramienta-io.md",
        "Tools / import-export / config agent",
        "Minimax 2.7; Composer",
        "`src/herramienta/`",
        "ISBN-13, RFC4180, Calibre UTF-8, CoverService, Config save, import/export, I18n utilities.",
        "All of `src/herramienta/` (csv/, export/, Config, BackupService, validators).",
        "presentacio controllers except calling new herramienta APIs; full ResourceBundle migration without coordinator.",
        [
            ("Top issues (read first)", packages.get("_summary", "")),
            ("Review — herramienta", "\n".join(packages.get("herramienta", []))),
            ("todo2 deep-dive", "\n\n---\n\n".join(todo2_by_agent["herramienta"])),
            ("Backlog [1][2][3]", "\n".join(priority_by["herramienta"])),
        ],
    )

    cross = "\n\n---\n\n".join(todo2_by_agent["crosscutting"]) or "(All todo2 blocks routed to specialists.)"
    write_brief(
        "06-todo2-crosscutting.md",
        "Cross-cutting todo2 agent",
        "Minimax 2.7; Composer",
        "Multi-package (only if blocks listed below)",
        "Pick up todo2 items that span layers or lack a clear package owner.",
        "Only blocks in the section below; if empty, skip this brief.",
        "Large refactors owned by 02–05; solo ControladorDomini rewrites.",
        [("todo2 blocks", cross)],
    )

    write_brief(
        "07-checkbiblio-qa.md",
        "checkBiblio / QA harness agent",
        "Composer (tests); Minimax 2.7",
        "`checkBiblio/`, `run.sh`, `run2.sh`",
        "Fix StressTest/UIAudit/I18nAudit, run scripts, headless guards; implement IMP-* harness improvements.",
        "`checkBiblio/*.java`, `run.sh`, `run2.sh` (see full review below).",
        "Unrelated production `src/` except minimal hooks required by tests.",
        [
            ("checkBiblio review (full)", todo3_body),
            ("Backlog [1][2][3]", "\n".join(priority_by["checkbiblio"])),
        ],
    )

    tot_text = TOT.read_text(encoding="utf-8")
    pointer = (
        "\n\n================================================================================\n"
        " AGENT BRIEFS: see agent-briefs/00-INDEX.md (split for AI agents)\n"
        "================================================================================\n"
    )
    if "agent-briefs/00-INDEX.md" not in tot_text:
        TOT.write_text(tot_text.rstrip() + pointer, encoding="utf-8")

    for f in sorted(OUT.glob("*.md")):
        print(f"{f.name}: {f.stat().st_size} bytes")


if __name__ == "__main__":
    main()
