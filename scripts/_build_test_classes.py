"""
Ajudant per a scripts\test.bat.
Llegeix test\_test_classes.txt (cada línia és un camí absolut a un
fitxer .java de test) i hi torna a escriure un nom de classe
completament qualificat per línia. S'elimina el prefix ``\\test\`` i
qualsevol separador de camí restant es converteix a punts.

Ús: python scripts\_build_test_classes.py
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
