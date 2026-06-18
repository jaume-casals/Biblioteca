#!/usr/bin/env python3
"""Re-audit scan: find non-Catalan text in src/**/*.java current state.

Walks every .java file under src/, extracts comments and string literals,
flags any that contain English or Spanish words. Outputs TSV for review.

Skip list (per tot.txt):
  - test/ (out of scope, also gitignored)
  - i18n catalog (I18n.java) — by design
  - SQL strings (catch via SELECT/INSERT/UPDATE/DELETE/CREATE/ALTER/VARCHAR/etc.)
  - public-domain / attribution blocks
  - Third-party class refs (java.*, javax.*, swing, awt) — filtered
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src"

# Skip these files entirely
SKIP_FILES = {
    SRC / "herramienta" / "I18n.java",  # i18n catalog by design
}

# Comment / string patterns
BLOCK_COMMENT_RE = re.compile(r"/\*(.*?)\*/", re.DOTALL)
LINE_COMMENT_RE = re.compile(r"//(.*)$", re.MULTILINE)
STRING_RE = re.compile(r'"((?:\\.|[^"\\])*)"')

# English / Spanish stopword detector — must contain at least one of these
# strong indicators to be flagged (avoids false positives on words like
# "main", "import", "class", etc. which are common in code).
EN_MARKERS = re.compile(
    r"\b(?:"
    r"the|this|that|these|those|with|from|for|and|but|or|"
    r"returns?|return|used|using|should|must|may|will|can|"
    r"unknown|null|empty|missing|invalid|valid|"
    r"failed|failure|error|warning|"
    r"create|update|delete|remove|add|insert|get|set|"
    r"called|calls|invoked|runs|executes?|"
    r"when|while|after|before|"
    r"file|directory|folder|path|"
    r"string|number|integer|boolean|array|list|map|set|"
    r"window|dialog|panel|button|menu|table|column|row|"
    r"NOTE|TODO|FIXME|XXX|HACK"
    r")\b",
    re.IGNORECASE,
)

# Spanish-only markers (not shared with Catalan). Project allows Catalan/Spanish mix
# per CLAUDE.md / AGENTS.md, but the explicit goal of this audit is to find
# untranslated English. We only flag Spanish words that are CLEARLY Spanish and
# would never appear in Catalan.
ES_MARKERS = re.compile(
    r"\b(?:"
    r"el\s+que|lo\s+que|"
    r"hacer|también|"
    r"año|"
    r"ejemplo|"
    r"devuelve|recibe|"
    r"usuario|"
    r"clic|"
    r"guardar|cargar|"
    r"Error:\s*[^:]"  # "Error: ..." with non-Catalan continuation
    r")\b",
    re.IGNORECASE,
)

# Filter: SQL keywords / JDBC URLs / hex / format patterns — skip
SQL_LIKE = re.compile(
    r"\b(?:SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|JOIN|ON|"
    r"CREATE\s+TABLE|ALTER\s+TABLE|DROP\s+TABLE|"
    r"PRIMARY\s+KEY|FOREIGN\s+KEY|REFERENCES|"
    r"VARCHAR|INTEGER|TEXT|BLOB|REAL|"
    r"jdbc:|MODE=|NON_KEYWORDS=|DB_CLOSE_DELAY|"
    r"GROUP\s+BY|ORDER\s+BY|LIMIT|"
    r"NULLS\s+FIRST|"
    r"VALUES\s*\(|"
    r"COALESCE|COUNT\s*\(|"
    r"AUTOCOMPLETE|"
    r"\?\s*[,)]|"
    r"@Override|@param|@return|@throws|@deprecated"
    r")\b",
    re.IGNORECASE,
)


def line_of(text: str, pos: int, base_line: int = 1) -> int:
    """Return 1-indexed line number of pos in text."""
    return text.count("\n", 0, pos) + base_line


def looks_non_catalan(s: str) -> bool:
    """Heuristic: returns True if `s` looks English/Spanish."""
    # Strip very short / pure-symbol / pure-identifier
    s_clean = s.strip()
    if len(s_clean) < 3:
        return False
    # URL / file path / SQL
    if SQL_LIKE.search(s_clean):
        return False
    # Punctuation-only / numbers / pure identifiers
    if re.fullmatch(r"[\W_0-9]+", s_clean):
        return False
    # Heuristic: must have a space (real English/Spanish words)
    if " " not in s_clean and not re.search(r"[A-Z]{2,}", s_clean):
        return False
    # Check word markers
    has_en = bool(EN_MARKERS.search(s_clean))
    has_es = bool(ES_MARKERS.search(s_clean))
    return has_en or has_es


def extract_block_comments(src: str):
    """Yield (line_no, comment_text) for each /* ... */ block."""
    for m in BLOCK_COMMENT_RE.finditer(src):
        text = m.group(1).strip()
        if not text:
            continue
        line_no = line_of(src, m.start())
        yield line_no, text


def extract_line_comments(src: str):
    """Yield (line_no, comment_text) for each // line comment (skip empty)."""
    for i, line in enumerate(src.splitlines(), 1):
        m = LINE_COMMENT_RE.search(line)
        if m:
            text = m.group(1).strip()
            if text and len(text) >= 4:
                yield i, text


def extract_strings(src: str):
    """Yield (line_no, str_text) for each "..." string literal."""
    for m in STRING_RE.finditer(src):
        text = m.group(1)
        if not text:
            continue
        # Skip very short / pure-symbol / format strings
        if len(text) < 4:
            continue
        if re.fullmatch(r"[%0-9.\-#+\s]*[a-z]", text):
            continue
        if re.fullmatch(r"[\W_0-9]+", text):
            continue
        line_no = line_of(src, m.start())
        yield line_no, text


def scan_file(path: Path):
    try:
        src = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return
    rel = path.relative_to(ROOT).as_posix()

    for line_no, text in extract_block_comments(src):
        if looks_non_catalan(text):
            yield ("BLOCK", rel, line_no, text)

    for line_no, text in extract_line_comments(src):
        if looks_non_catalan(text):
            yield ("LINE", rel, line_no, text)

    for line_no, text in extract_strings(src):
        if looks_non_catalan(text):
            yield ("STRING", rel, line_no, text)


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "--help":
        print(__doc__)
        return
    out_path = ROOT / "audit_redo.tsv"
    count = {"BLOCK": 0, "LINE": 0, "STRING": 0}
    total = 0
    with out_path.open("w", encoding="utf-8") as f:
        f.write("KIND\tFILE\tLINE\tTEXT\n")
        for path in sorted(SRC.rglob("*.java")):
            if path in SKIP_FILES:
                continue
            for kind, rel, line_no, text in scan_file(path):
                f.write(f"{kind}\t{rel}\t{line_no}\t{text}\n")
                count[kind] = count.get(kind, 0) + 1
                total += 1
    print(f"Wrote {out_path} ({total} hits)")
    for k, v in sorted(count.items()):
        print(f"  {k:6s} {v}")


if __name__ == "__main__":
    main()
