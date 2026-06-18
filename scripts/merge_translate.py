#!/usr/bin/env python3
"""Merge translate.txt + translate1.txt into translate_merged.txt (TSV).

Schema (one header row, tab-separated):
    KIND | FILE | LINE | LANG | ORIGINAL | SUGGESTED | NOTES

KIND values:
    COMMENT_BLOCK   Javadoc /** ... */ or block /* ... */
    COMMENT_LINE    Single-line // comment (incl. // ── X ── headers)
    STRING          String literal in source
    IDENTIFIER      Class / method / field / variable name
    LICENSE_HEADER  Public-domain / attribution block

LANG values (when applicable):
    en | mixed | section-header | ca

The ## META section at the bottom preserves translate1.txt's free-form
sections (SUMMARY, RECOMMENDED DIRECTORY RENAMES, RECOMMENDED FILE RENAMES,
JUDGMENT CALLS / EDGE CASES, FILE INVENTORY) verbatim — they don't fit
the row model.
"""
from __future__ import annotations
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TXT_OLD = ROOT / "translate.txt"
TXT_NEW = ROOT / "translate1.txt"
OUT = ROOT / "translate_merged.txt"

# ---------------------------------------------------------------- helpers

def esc(s: str) -> str:
    """Make a field safe for single-line TSV output."""
    if s is None:
        return ""
    s = s.replace("\t", " ").replace("\r", " ").replace("\n", " ")
    s = re.sub(r"\s+", " ", s).strip()
    return s


# ---------------------------------------------------------------- translate.txt

def parse_translate_old() -> tuple[list[str], list[dict]]:
    """Return (preamble_lines, records) from translate.txt."""
    preamble: list[str] = []
    records: list[dict] = []
    current_file = ""
    in_preamble = True

    row_re = re.compile(
        r"^(BLOCK|LINE|STRING)\s+(L\d+(?:-\d+)?)\s+\[(\w+)\s*\]\s+(.*)$"
    )
    file_re = re.compile(r"^## (\S+)\s*$")

    with TXT_OLD.open(encoding="utf-8") as f:
        for raw in f:
            line = raw.rstrip("\n")
            if in_preamble:
                if line.startswith("## ") or row_re.match(line):
                    in_preamble = False
                else:
                    preamble.append(line)
                    continue
            if not line.strip():
                continue
            m_file = file_re.match(line)
            if m_file:
                current_file = m_file.group(1)
                continue
            m_row = row_re.match(line)
            if not m_row:
                continue
            kind_raw, ln, lang, text = m_row.groups()
            kind = {
                "BLOCK": "COMMENT_BLOCK",
                "LINE": "COMMENT_LINE",
                "STRING": "STRING",
            }[kind_raw]
            records.append({
                "kind": kind,
                "file": current_file,
                "line": ln.lstrip("L"),
                "lang": lang.strip().lower(),
                "original": esc(text),
                "suggested": "-",
                "notes": "",
            })
    return preamble, records


# ---------------------------------------------------------------- translate1.txt

# Record marker: [path(:line-spec)?]  rest of line
# Line-spec may be a single number, a range, or a comma-separated list of
# either ("164, 169, 186" or "1-3, 5-7") — translate1.txt uses all three shapes.
REC_RE = re.compile(r"^\[([^\]]+?)(?::([\d, \-]+))?\]\s+(.*)$")
# Section heading: ## === NAME ===
META_HEAD_RE = re.compile(r"^## === (.+?) ===\s*$")
# Sub-field:    Suggested/Language/Occurrences/Context/Note
SUB_RE = re.compile(r"^\s+(Suggested|Language|Occurrences|Context|Note):\s*(.*)$")


# Section-heading → default KIND. Content can refine based on shape.
SECTION_DEFAULT_KIND: dict[str, str] = {
    "IDENTIFIERS (class/method/field/variable names)": "IDENTIFIER",
    "COMMENTS": "COMMENT_BLOCK",
    "STRING LITERALS (user-facing)": "STRING",
    "LICENSE HEADERS / BOILERPLATE": "LICENSE_HEADER",
}


def refine_kind(default: str, rest: str) -> str:
    """Override default KIND based on the rest of the record line."""
    s = rest.lstrip()
    # Quoted text → STRING regardless of section
    if s.startswith(('"', "'", "“")):
        return "STRING"
    # Empty body — keep default
    if not s:
        return default
    # In a COMMENTS section, anything that looks like a signature is still
    # a comment block (the comment annotates a code line).
    # In an IDENTIFIERS section, anything with `(kind)` is a clear identifier.
    if default == "IDENTIFIER" and "(" in s:
        return "IDENTIFIER"
    return default


def parse_records_in_section(body: list[str], default_kind: str) -> list[dict]:
    """Parse records from a DATA section body."""
    records: list[dict] = []
    current: dict | None = None

    def flush() -> None:
        nonlocal current
        if current is not None:
            records.append(current)
            current = None

    for raw in body:
        line = raw.rstrip("\n")
        m_rec = REC_RE.match(line)
        if m_rec:
            flush()
            file, ln, rest = m_rec.groups()
            current = {
                "kind": refine_kind(default_kind, rest),
                "file": file.strip(),
                "line": ln if ln is not None else "-",
                "lang": "",
                "original": esc(rest),
                "suggested": "",
                "notes": "",
            }
            continue
        if current is None:
            continue
        stripped = line.strip()
        if not stripped:
            continue
        m_sub = SUB_RE.match(line)
        if not m_sub:
            # continuation line — append to notes (rare)
            current["notes"] = esc((current["notes"] + " " + stripped).strip()) \
                if current["notes"] else esc(stripped)
            continue
        key, val = m_sub.group(1), m_sub.group(2).strip()
        if key == "Suggested":
            current["suggested"] = esc(val)
        elif key == "Language":
            lang = val.lower()
            if lang.startswith("mixed"):
                lang = "mixed"
            elif lang == "english":
                lang = "en"
            elif lang == "catalan":
                lang = "ca"
            elif lang == "spanish":
                lang = "es"
            current["lang"] = lang
        elif key == "Occurrences":
            current["notes"] = esc("occ=" + val)
        elif key == "Context":
            current["notes"] = esc((current["notes"] + " " + val).strip()) \
                if current["notes"] else esc(val)
        elif key == "Note":
            current["notes"] = esc((current["notes"] + " " + val).strip()) \
                if current["notes"] else esc(val)

    flush()
    for r in records:
        if not r["suggested"]:
            r["suggested"] = "-"
    return records


def parse_translate_new() -> tuple[list[dict], list[tuple[str, list[str]]]]:
    """Return (records, meta_sections).

    Sections are classified as DATA (contains record markers) or META (prose
    only). Only META sections are returned for verbatim preservation.
    """
    # First pass: collect section headings + bodies
    sections: list[tuple[str, list[str]]] = []  # (heading, body_lines)
    current_heading: str | None = None
    current_body: list[str] = []

    with TXT_NEW.open(encoding="utf-8") as f:
        for raw in f:
            line = raw.rstrip("\n")
            m_head = META_HEAD_RE.match(line)
            if m_head:
                if current_heading is not None:
                    sections.append((current_heading, current_body))
                current_heading = m_head.group(1)
                current_body = []
            else:
                if current_heading is None:
                    # preamble before first ## === === heading — skip
                    continue
                current_body.append(line)
        if current_heading is not None:
            sections.append((current_heading, current_body))

    # Second pass: classify each section and parse accordingly
    all_records: list[dict] = []
    meta_sections: list[tuple[str, list[str]]] = []
    for heading, body in sections:
        has_records = any(REC_RE.match(l) for l in body)
        if has_records:
            default = SECTION_DEFAULT_KIND.get(heading, "LICENSE_HEADER")
            all_records.extend(parse_records_in_section(body, default))
        else:
            # strip leading/trailing blank lines for cleaner output
            while body and not body[0].strip():
                body.pop(0)
            while body and not body[-1].strip():
                body.pop()
            meta_sections.append((heading, body))

    return all_records, meta_sections


# ---------------------------------------------------------------- write output

def write_merged(preamble: list[str], old_records: list[dict],
                 new_records: list[dict],
                 meta: list[tuple[str, list[str]]]) -> None:
    cols = ["KIND", "FILE", "LINE", "LANG", "ORIGINAL", "SUGGESTED", "NOTES"]

    with OUT.open("w", encoding="utf-8") as f:
        f.write("# translate_merged.txt — non-Catalan text in the Biblioteca codebase\n")
        f.write("#\n")
        f.write("# Auto-merged from translate.txt (comment/string inventory) and\n")
        f.write("# translate1.txt (identifier / string-suggestion plan). Both files\n")
        f.write("# are kept untouched; this is the unified single source of truth.\n")
        f.write("#\n")
        f.write("# TSV columns:\n")
        f.write("#   KIND      COMMENT_BLOCK | COMMENT_LINE | STRING | IDENTIFIER | LICENSE_HEADER\n")
        f.write("#   FILE      source path relative to repo root\n")
        f.write("#   LINE      line number, or a range (e.g. 5-8)\n")
        f.write("#   LANG      en | mixed | section-header | ca   (blank for IDENTIFIER rows)\n")
        f.write("#   ORIGINAL  the non-Catalan text as it appears in source\n")
        f.write("#   SUGGESTED proposed Catalan form, or \"-\" if none / \"keep\" if no change\n")
        f.write("#   NOTES     context / occurrence counts / edge-case flags\n")
        f.write("#\n")
        f.write("# Meta sections (SUMMARY / RENAMES / JUDGMENT / FILE INVENTORY) from\n")
        f.write("# translate1.txt are preserved verbatim in the ## META block at the\n")
        f.write("# bottom of this file.\n")
        f.write("#\n")
        f.write("# Filter examples:\n")
        f.write("#   rg -F $'\\tIDENTIFIER\\t' translate_merged.txt\n")
        f.write("#   rg '^STRING\\t' translate_merged.txt\n")
        f.write("\n")
        f.write("\t".join(cols) + "\n")

        f.write(f"# --- from translate.txt ({len(old_records)} rows) ---\n")
        for r in old_records:
            f.write("\t".join([
                r["kind"], r["file"], r["line"], r["lang"],
                r["original"], r["suggested"], r["notes"],
            ]) + "\n")

        f.write(f"\n# --- from translate1.txt ({len(new_records)} rows) ---\n")
        for r in new_records:
            f.write("\t".join([
                r["kind"], r["file"], r["line"], r["lang"],
                r["original"], r["suggested"], r["notes"],
            ]) + "\n")

        f.write("\n# === META (verbatim from translate1.txt) ===\n\n")
        for heading, body in meta:
            f.write(f"## === {heading} ===\n")
            f.write("\n".join(body) + "\n\n")


# ---------------------------------------------------------------- main

def main() -> None:
    preamble, old_records = parse_translate_old()
    new_records, meta = parse_translate_new()
    write_merged(preamble, old_records, new_records, meta)

    total = len(old_records) + len(new_records)
    print(f"translate.txt      : {len(old_records):4d} data rows")
    print(f"translate1.txt      : {len(new_records):4d} data rows")
    print(f"META sections       : {len(meta):4d}")
    print(f"translate_merged.txt: {total:4d} data rows + {len(meta)} meta sections")
    print()
    from collections import Counter
    kinds = Counter(r["kind"] for r in old_records + new_records)
    for k, v in sorted(kinds.items(), key=lambda kv: -kv[1]):
        print(f"  {k:18s} {v:4d}")
    # Spot-check: rows with non-empty Suggested from translate1.txt
    sug = sum(1 for r in new_records if r["suggested"] not in ("", "-"))
    print(f"\n  rows w/ non-default Suggested: {sug}")


if __name__ == "__main__":
    main()
