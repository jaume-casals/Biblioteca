"""
Helper for scripts\test.bat.
Reads test\_test_classes.txt (each line is an absolute path to a test .java
file) and writes back one fully-qualified class name per line. The
``\\test\`` prefix is stripped and any remaining path separators are
converted to dots.

Usage: python scripts\_build_test_classes.py
"""
import os
import re
import sys

SRC = os.path.join('test', '_test_classes.txt')

def main():
    if not os.path.isfile(SRC):
        return 0
    with open(SRC, encoding='utf-8') as f:
        paths = [line.strip() for line in f if line.strip()]
    out = []
    sep = re.compile(r'[\\/]')
    # Locate the last ``\test\`` or ``/test/`` segment in the path and
    # keep only the portion after it.
    test_sep = re.compile(r'[\\/]test[\\/]')
    for p in paths:
        m = test_sep.search(p)
        if m:
            rel = p[m.end():]
        else:
            rel = os.path.basename(p)
        if rel.lower().endswith('.java'):
            rel = rel[:-5]
        rel = sep.sub('.', rel).strip('.')
        if rel:
            out.append(rel)
    with open(SRC, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out))
    return 0

if __name__ == '__main__':
    sys.exit(main())
