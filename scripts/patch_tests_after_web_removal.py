#!/usr/bin/env python3
"""
Drop or fix tests that still reference removed web/API code (Swing-only Biblioteca).

Run from repo root after pulling web-removal changes:

    python scripts/patch_tests_after_web_removal.py
    make test

Edits test/BibliotecaTest.java and test/BibliotecaJUnit5Test.java in place.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEST_FILES = [
    ROOT / "test" / "BibliotecaTest.java",
    ROOT / "test" / "BibliotecaJUnit5Test.java",
]

REPLACEMENTS = (
    ("interficie.EnActualizarBBDD", "presentacio.listener.EnActualizarBBDD"),
)

# If any of these appear inside a test method, remove the whole method.
BANNED_IN_METHOD = (
    "api.",
    "ApiServer",
    "HttpRouter",
    "HttpCtx",
    "JsonMapper",
    "LlibreRouter.sniffImageMime",
    "persistencia.LibraryGraph",
    "ModeSelectorDialog",
    "resolveMode",
    "setLastMode",
    "getLastMode",
)

METHOD_START = re.compile(
    r"(?m)^(?P<indent>[ \t]*)"
    r"(?:(?:@\w+(?:\([^)]*\))?\s*)+)"  # optional annotations (@Test, etc.)
    r"(?:(?:public|private|protected|static|final|synchronized)\s+)*"
    r"(?:<[^>]+>\s+)?"
    r"(?:void|[\w<>,\[\].?]+)\s+"
    r"(?P<name>\w+)\s*\([^;]*\)\s*(?:throws\s+[\w.,\s]+)?\s*\{"
)


def _skip_string_or_comment(text: str, i: int) -> int:
    if i >= len(text):
        return i
    c = text[i]
    if c == '"':
        i += 1
        while i < len(text):
            if text[i] == "\\":
                i += 2
                continue
            if text[i] == '"':
                return i + 1
            i += 1
        return len(text)
    if c == "'":
        i += 1
        while i < len(text):
            if text[i] == "\\":
                i += 2
                continue
            if text[i] == "'":
                return i + 1
            i += 1
        return len(text)
    if c == "/" and i + 1 < len(text):
        if text[i + 1] == "/":
            nl = text.find("\n", i)
            return len(text) if nl < 0 else nl + 1
        if text[i + 1] == "*":
            end = text.find("*/", i + 2)
            return len(text) if end < 0 else end + 2
    return i + 1


def _matching_brace(text: str, open_brace: int) -> int:
    depth = 0
    i = open_brace
    while i < len(text):
        if text[i] in "\"'":
            i = _skip_string_or_comment(text, i)
            continue
        if text[i] == "/" and i + 1 < len(text) and text[i + 1] in "/*":
            i = _skip_string_or_comment(text, i)
            continue
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def _method_spans(text: str) -> list[tuple[int, int, str]]:
    spans: list[tuple[int, int, str]] = []
    for m in METHOD_START.finditer(text):
        open_brace = m.end() - 1
        close = _matching_brace(text, open_brace)
        if close < 0:
            continue
        # Include leading annotation lines above the method signature
        start = m.start()
        line_start = text.rfind("\n", 0, start)
        line_start = 0 if line_start < 0 else line_start + 1
        before = text[line_start:start]
        if before.strip().startswith("@"):
            start = line_start
        end = close + 1
        # Trailing blank lines after method
        while end < len(text) and text[end] in "\r\n":
            end += 1
        spans.append((start, end, m.group("name")))
    return spans


def _is_test_method(name: str, body: str, file_name: str) -> bool:
    if file_name == "BibliotecaTest.java":
        return name.startswith("test")
    if file_name == "BibliotecaJUnit5Test.java":
        return "@Test" in body or name.startswith("test")
    return name.startswith("test")


def patch_file(path: Path) -> list[str]:
    if not path.is_file():
        return [f"skip (missing): {path.relative_to(ROOT)}"]

    original = path.read_text(encoding="utf-8")
    text = original
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)

    removed: list[str] = []
    spans = _method_spans(text)
    # Remove from end to start so indices stay valid
    for start, end, name in reversed(spans):
        chunk = text[start:end]
        if not _is_test_method(name, chunk, path.name):
            continue
        if not any(b in chunk for b in BANNED_IN_METHOD):
            continue
        text = text[:start] + text[end:]
        removed.append(name)

    if removed:
        for name in removed:
            text = re.sub(
                rf"(?m)^[ \t]*{re.escape(name)}\(\);\s*\r?\n",
                "",
                text,
            )

    if text != original:
        path.write_text(text, encoding="utf-8", newline="\n")

    rel = path.relative_to(ROOT)
    if not removed and text == original:
        return [f"ok (no obsolete tests): {rel}"]
    if not removed:
        return [f"ok (import fixes only): {rel}"]
    return [f"patched {rel}: removed {len(removed)} method(s): {', '.join(reversed(removed))}"]


def main() -> int:
    any_missing = False
    lines: list[str] = []
    for path in TEST_FILES:
        if not path.is_file():
            any_missing = True
            lines.append(f"MISSING: {path.relative_to(ROOT)} (copy your local test/ tree, then re-run)")
            continue
        lines.extend(patch_file(path))

    for line in lines:
        print(line)

    if any_missing:
        print("\nNo test files were patched. Ensure test/BibliotecaTest.java exists locally.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
