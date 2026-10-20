#!/usr/bin/env python3
"""Comprova que el Class-Path del manifest d'un JAR resolgui.

El manifest Class-Path és una llista separada per ESPAIS de camins
relatius al JAR. Si un camí no resol, el JAR arrenca i mor en el primer
accés a la classe que falta (NoClassDefFoundError) — sense cap error
durant la construcció.

Ús:
    python3 scripts/verify_jar_manifest.py build/artifacts/biblioteca.jar

Retorna 0 si totes les entrades resolen, 1 si n'hi ha alguna que no.
"""

import os
import sys
import zipfile


def read_manifest(jar_path):
    with zipfile.ZipFile(jar_path) as zf:
        try:
            return zf.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
        except KeyError:
            return ""


def manifest_value(manifest, key):
    """Llegeix un atribut del manifest continuant les línies plegades."""
    # El manifest pot usar CRLF: el "\r" al final de cada línia s'ha de
    # treure abans de continuar les línies, altrament el valor quedaria
    # amb "\r" al mig i es trencaria en tokens incorrectes.
    lines = manifest.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    value = None
    for line in lines:
        if line.startswith(" ") and value is not None:
            value += line[1:]
            continue
        if ":" in line:
            k, _, v = line.partition(":")
            if k.strip().lower() == key.lower():
                value = v.strip()
    return value or ""


def main(argv):
    if len(argv) != 1:
        sys.stderr.write("Ús: python3 scripts/verify_jar_manifest.py <jar>\n")
        return 2
    jar_path = argv[0]
    if not os.path.isfile(jar_path):
        sys.stderr.write("ERROR: no existeix el JAR %s\n" % jar_path)
        return 2

    manifest = read_manifest(jar_path)
    if not manifest:
        sys.stderr.write("ERROR: %s no té META-INF/MANIFEST.MF\n" % jar_path)
        return 1

    class_path = manifest_value(manifest, "Class-Path")
    if not class_path.strip():
        print("OK: %s no declara Class-Path (JAR autònom)" % jar_path)
        return 0

    base = os.path.dirname(os.path.abspath(jar_path))
    missing = []
    for entry in class_path.split():
        target = os.path.normpath(os.path.join(base, entry))
        if not os.path.exists(target):
            missing.append((entry, target))

    if missing:
        for entry, target in missing:
            sys.stderr.write("ERROR: Class-Path '%s' no resol (s'esperava %s)\n" % (entry, target))
        return 1

    print("OK: %s — %s entrades del Class-Path resolen"
          % (jar_path, len(class_path.split())))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
