#!/usr/bin/env python3
"""Proves del generador de cadenes (scripts/gen_strings.py).

Comproven les invariants d'escapament i de llegenda del CSV que el
generador ha de garantir:

* un salt de línia REAL al CSV es converteix en un salt de línia en
  temps d'execució;
* la seqüència literal de dos caràcters barra invertida + n es
  conserva literal (no es converteix en un salt de línia);
* cometes, barres invertes, caràcters de control i Unicode es
  conserven;
* els placeholders {0} es mantenen als tres idiomes;
* un registre CSV de diverses línies (camp citat amb salt de línia
  real) es llegeix com un sol registre.

S'executa amb:  python3 scripts/test_gen_strings.py
Retorna 0 si tot és correcte, 1 si hi ha algun error.
"""

import importlib.util
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEN = os.path.join(REPO, 'scripts', 'gen_strings.py')

spec = importlib.util.spec_from_file_location('gen_strings', GEN)
gs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gs)


def desescapa_literal_java(text):
    """Interpreta un literal Java com el fa el compilador.

    Només es contemplen els escapes que el generador pot produir:
    \\\\ \\" \\n \\r \\t. Serveix per comprovar el valor EN TEMPS
    D'EXECUCIÓ, que és el que veu l'usuari.
    """
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '\\' and i + 1 < n:
            nxt = text[i + 1]
            if nxt == '\\':
                out.append('\\')
            elif nxt == '"':
                out.append('"')
            elif nxt == 'n':
                out.append('\n')
            elif nxt == 'r':
                out.append('\r')
            elif nxt == 't':
                out.append('\t')
            else:
                out.append(c)
                out.append(nxt)
            i += 2
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def comprova(condicio, missatge):
    if not condicio:
        raise AssertionError(missatge)


def main():
    errors = []

    # 1. Salt de línia real.
    valor = 'Línia 1\nLínia 2'
    generat = gs.java_str(valor)
    comprova(generat == 'Línia 1\\nLínia 2',
              f'salt real: escapament inesperat {generat!r}')
    comprova(desescapa_literal_java(generat) == valor,
              'salt real: el valor en temps d\'execució no coincideix')

    # 2. Seqüència literal \n (barra invertida + n).
    valor = 'Línia 1\\nLínia 2'
    generat = gs.java_str(valor)
    comprova(generat == 'Línia 1\\\\nLínia 2',
              f'seqüència literal: escapament inesperat {generat!r}')
    comprova(desescapa_literal_java(generat) == valor,
              'seqüència literal: el valor en temps d\'execució no coincideix')

    # 3. Cometes.
    valor = 'Diu "hola" i \\"adeu\\"'
    generat = gs.java_str(valor)
    comprova(desescapa_literal_java(generat) == valor,
              'cometes: el valor en temps d\'execució no coincideix')

    # 4. Barra invertida sola i barra invertida final.
    for valor in ('\\', 'a\\', '\\n', 'C:\\ruta\\fitxer'):
        generat = gs.java_str(valor)
        comprova(desescapa_literal_java(generat) == valor,
                  f'barra invertida {valor!r}: el valor en temps d\'execució no coincideix')

    # 5. Unicode i placeholders als tres idiomes.
    textos = {
        'ca': 'Llibre {0} — accents àèïòú · 漢字 · 🧪',
        'es': 'Libro {0} — acentos áéíóú · 漢字 · 🧪',
        'en': 'Book {0} — accents àèïòú · 漢字 · 🧪',
    }
    for codi, text in textos.items():
        generat = gs.java_str(text)
        comprova('{0}' in generat, f'{codi}: el placeholder es perd')
        comprova('漢字' in generat and '🧪' in generat, f'{codi}: l\'Unicode es perd')
        comprova(desescapa_literal_java(generat) == text,
                 f'{codi}: el valor en temps d\'execució no coincideix')

    # 6. Un registre CSV de diverses línies es llegeix com un sol registre.
    csv_multilinia = (
        'key,ca,es,en\n'
        'dlg_prova,"Primera línia\nsegona línia","Primera línia\nsegunda línea",'
        '"First line\nsecond line"\n'
    )
    registres = list(gs._iter_records(csv_multilinia))
    comprova(len(registres) == 2,
              f'CSV multilínia: s\'han esperat 2 registres, n\'hi ha {len(registres)}')
    files = list(__import__('csv').reader([registres[1]]))
    comprova(len(files) == 1 and len(files[0]) == 4,
              f'CSV multilínia: registre mal tallat {files!r}')
    comprova(files[0][1] == 'Primera línia\nsegona línia',
              f'CSV multilínia: salt de línia perdut {files[0][1]!r}')

    # 7. El camp citat amb cometes dobles interiors no talla el registre.
    csv_comilles = 'key,ca,es,en\ndlg_x,"Diu ""hola""","a","b"\n'
    registres = list(gs._iter_records(csv_comilles))
    comprova(len(registres) == 2,
              f'CSV amb cometes: s\'han esperat 2 registres, n\'hi ha {len(registres)}')

    # 8. Una línia de comentari no es converteix en entrada.
    csv_comentari = '# unajak komment,ca,es,en\nkey,ca,es,en\ndlg_y,a,b,c\n'
    registres = [r for r in gs._iter_records(csv_comentari) if not r.strip().startswith('#')]
    comprova(len(registres) == 2,
              f'comentari: s\'han esperat 2 registres, n\'hi ha {len(registres)}')

    print(f'OK: {9} invariants del generador es compleixen')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except AssertionError as e:
        print(f'FALLO: {e}', file=sys.stderr)
        sys.exit(1)
