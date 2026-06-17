#!/usr/bin/env python3
"""
Elimina o corregeix tests que encara fan referència a codi web/API eliminat
(Biblioteca només Swing).

Executa des de l'arrel del repositori després de baixar els canvis
d'eliminació web:

    python scripts/patch_tests_after_web_removal.py
    make test

Edita in situ test/BibliotecaTest.java i test/BibliotecaJUnit5Test.java.
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

# If any of these appear inside a test method/lambda, remove the whole block.
BANNED_IN_METHOD = (
    "api.",
    "ApiServer",
    "HttpRouter",
    "HttpCtx",
    "JsonMapper",
    "LlibreRouter.sniffImageMime",
    "persistencia.LibraryGraph",
    "LibraryGraph",
    "ModeSelectorDialog",
    "resolveMode",
    "setLastMode",
    "getLastMode",
)

METHOD_SIG = re.compile(
    r"(?m)^[ \t]*"
    r"(?:(?:public|private|protected|static|final|synchronized)\s+)*"
    r"(?:<[^>]+>\s+)?"
    r"(?:void|[\w<>,\[\].?]+)\s+"
    r"(?P<name>\w+)\s*\([^;]*\)\s*(?:throws\s+[\w.,\s]+)?\s*\{"
)

TEST_CALL = re.compile(r"(?m)^[ \t]*test\s*\(")


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


def _include_leading_annotations_and_comments(text: str, sig_start: int) -> int:
    pos = sig_start
    while pos > 0:
        line_start = text.rfind("\n", 0, pos - 1)
        line_start = 0 if line_start < 0 else line_start + 1
        line = text[line_start:pos]
        stripped = line.strip()
        if stripped == "" or stripped.startswith("//") or stripped.startswith("@"):
            pos = line_start
            continue
        break
    return pos


def _parse_double_quoted_string(text: str, i: int) -> tuple[int, str] | None:
    while i < len(text) and text[i].isspace():
        i += 1
    if i >= len(text) or text[i] != '"':
        return None
    i += 1
    start = i
    while i < len(text):
        if text[i] == "\\":
            i += 2
            continue
        if text[i] == '"':
            return i + 1, text[start:i]
        i += 1
    return None


def _method_spans(text: str) -> list[tuple[int, int, str]]:
    spans: list[tuple[int, int, str]] = []
    for m in METHOD_SIG.finditer(text):
        open_brace = m.end() - 1
        close = _matching_brace(text, open_brace)
        if close < 0:
            continue
        start = _include_leading_annotations_and_comments(text, m.start())
        end = close + 1
        while end < len(text) and text[end] in "\r\n":
            end += 1
        spans.append((start, end, m.group("name")))
    return spans


def _test_lambda_spans(text: str) -> list[tuple[int, int, str]]:
    """BibliotecaTest.java: test(\"name\", () -> { ... }); inside main()."""
    spans: list[tuple[int, int, str]] = []
    for m in TEST_CALL.finditer(text):
        start = m.start()
        i = m.end()
        parsed = _parse_double_quoted_string(text, i)
        if not parsed:
            continue
        i, name = parsed
        arrow = re.search(r"\(\)\s*->\s*\{", text[i:])
        if not arrow:
            continue
        brace = i + arrow.end() - 1
        close = _matching_brace(text, brace)
        if close < 0:
            continue
        end = close + 1
        while end < len(text) and text[end].isspace():
            end += 1
        if text[end : end + 2] == ");":
            end += 2
        while end < len(text) and text[end] in "\r\n":
            end += 1
        start = _include_leading_annotations_and_comments(text, start)
        spans.append((start, end, name))
    return spans


def _is_junit_test_method(name: str, body: str, file_name: str) -> bool:
    if file_name != "BibliotecaJUnit5Test.java":
        return False
    return "@Test" in body or name.startswith("test")


def _chunk_has_banned(chunk: str) -> bool:
    return any(b in chunk for b in BANNED_IN_METHOD)


def _remove_spans(text: str, spans: list[tuple[int, int, str]]) -> tuple[str, list[str]]:
    removed: list[str] = []
    for start, end, name in reversed(spans):
        chunk = text[start:end]
        if not _chunk_has_banned(chunk):
            continue
        text = text[:start] + text[end:]
        removed.append(name)
    return text, removed


def patch_file(path: Path) -> list[str]:
    if not path.is_file():
        return [f"skip (missing): {path.relative_to(ROOT)}"]

    original = path.read_text(encoding="utf-8")
    text = original
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)

    all_removed: list[str] = []

    if path.name == "BibliotecaTest.java":
        lambda_spans = _test_lambda_spans(text)
        text, removed = _remove_spans(text, lambda_spans)
        all_removed.extend(removed)

    if path.name == "BibliotecaJUnit5Test.java":
        method_spans = [
            (s, e, n)
            for s, e, n in _method_spans(text)
            if _is_junit_test_method(n, text[s:e], path.name)
        ]
        text, removed = _remove_spans(text, method_spans)
        all_removed.extend(removed)

    if all_removed:
        for name in all_removed:
            text = re.sub(
                rf"(?m)^[ \t]*{re.escape(name)}\(\);\s*\r?\n",
                "",
                text,
            )

    if text != original:
        path.write_text(text, encoding="utf-8", newline="\n")

    rel = path.relative_to(ROOT)
    if not all_removed and text == original:
        return [f"ok (no obsolete tests): {rel}"]
    if not all_removed:
        return [f"ok (import fixes only): {rel}"]
    return [f"patched {rel}: removed {len(all_removed)} block(s): {', '.join(all_removed)}"]


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
