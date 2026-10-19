#!/usr/bin/env python3
"""Gestiona les depend\u00e8ncies del projecte a partir de build/deps.lock.

El fitxer de lock \u00e9s l'\u00fanica font de veritat de les versions. Cada l\u00ednia \u00e9s:

    grup:artifact:versio sha256 etiqueta

on `etiqueta` \u00e9s `runtime`, `test` o `fuzz` i decideix a quina classpath
pertany el jar. Els SHA-256 garanteixen que el jar que es compila i s'executa
\u00e9s exactament el que s'ha verificat.

Operacions:
    fetch     baixa els jars que falten i verifica el SHA-256 de tots
    verify    verifica el SHA-256 dels jars ja presents (no baixa res)
    update    consulta Maven Central, actualitza versions i sha256 del lock
    classpath imprimeix el classpath de l'etiqueta demanada (per al Makefile)
"""

import argparse
import hashlib
import os
import re
import sys
import urllib.error
import urllib.request

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_LOCK = os.path.join(REPO, "build", "deps.lock")
DEFAULT_DIR = os.path.join(REPO, "lib")
MAVEN = "https://repo1.maven.org/maven2"
TAGS = ("runtime", "test", "fuzz", "legacy")
LINE_RE = re.compile(r"^(?P<coord>[^\s]+)\s+(?P<sha>[0-9a-f]{64})\s+(?P<tag>\S+)\s*$")


def read_lock(path):
    entries = []
    problems = []
    with open(path, encoding="utf-8") as fh:
        for num, raw in enumerate(fh, 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            m = LINE_RE.match(line)
            if not m:
                problems.append("l\u00ednia %d mal formada: %s" % (num, line))
                continue
            group, artifact, version = m.group("coord").split(":")
            entries.append({
                "group": group,
                "artifact": artifact,
                "version": version,
                "sha256": m.group("sha"),
                "tag": m.group("tag"),
            })
    if problems:
        for p in problems:
            sys.stderr.write("ERROR: " + p + "\n")
        raise SystemExit(2)
    return entries


def write_lock(path, entries):
    lines = ["# build/deps.lock \u2014 font \u00fanica de veritat de les depend\u00e8ncies.",
             "# Format: grup:artifact:versio sha256 etiqueta   (runtime | test | fuzz)",
             "# Regenera amb: python3 scripts/fetch_deps.py update",
             ""]
    for tag in TAGS:
        tagged = [e for e in entries if e["tag"] == tag]
        if not tagged:
            continue
        lines.append("# --- %s ---" % tag)
        for e in tagged:
            coord = "%s:%s:%s" % (e["group"], e["artifact"], e["version"])
            lines.append("%s %s %s" % (coord, e["sha256"], e["tag"]))
        lines.append("")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))


def jar_name(entry):
    return "%s-%s.jar" % (entry["artifact"], entry["version"])


def coord_path(entry):
    return "%s/%s/%s" % (entry["group"].replace(".", "/"), entry["artifact"], entry["version"])


def download(entry, dest_dir):
    url = "%s/%s/%s" % (MAVEN, coord_path(entry), jar_name(entry))
    dest = os.path.join(dest_dir, jar_name(entry))
    tmp = dest + ".part"
    with urllib.request.urlopen(url, timeout=120) as resp, open(tmp, "wb") as out:
        while True:
            chunk = resp.read(262144)
            if not chunk:
                break
            out.write(chunk)
    os.replace(tmp, dest)
    return dest


def sha256_of(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for block in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


PRERELEASE_RE = re.compile(r"(?i)(M\d+|RC\d*|alpha|beta|SNAPSHOT)")


def latest_release(group, artifact):
    """Versio released més recent, ignorant les prerelease.

    El camp <release> de Maven Central pot apuntar a una prerelease
    (slf4j 2.1.0-alpha1, assertj 4.0.0-M1): per a una base de codi no es
    volen oferir versions prerelease sense revisar l'API.
    """
    url = "%s/%s/%s/maven-metadata.xml" % (MAVEN, group.replace(".", "/"), artifact)
    with urllib.request.urlopen(url, timeout=60) as resp:
        body = resp.read().decode("utf-8", "replace")
    versions = re.findall(r"<version>([^<]+)</version>", body)
    stable = [v.strip() for v in versions if not PRERELEASE_RE.search(v)]
    if stable:
        return stable[-1]
    m = re.search(r"<release>([^<]+)</release>", body)
    if m:
        return m.group(1).strip()
    m = re.search(r"<latest>([^<]+)</latest>", body)
    return m.group(1).strip() if m else None


def cmd_classpath(args, entries):
    out = []
    for e in entries:
        if args.tag in ("all", e["tag"]) or (args.tag == "all"):
            out.append(os.path.join(args.dir, jar_name(e)))
    print(":".join(out))
    return 0


def cmd_fetch(args, entries):
    os.makedirs(args.dir, exist_ok=True)
    failures = []
    for e in entries:
        path = os.path.join(args.dir, jar_name(e))
        if not os.path.exists(path):
            if args.skip_missing:
                print("SKIP  %s (absent, no baixa)" % jar_name(e))
                continue
            print("FETCH %s" % jar_name(e))
            try:
                download(e, args.dir)
            except Exception as exc:  # noqa: BLE001
                failures.append("%s: %s" % (jar_name(e), exc))
                continue
        actual = sha256_of(path)
        if actual != e["sha256"]:
            failures.append("%s: sha256 %s != esperado %s" % (jar_name(e), actual, e["sha256"]))
        else:
            print("OK    %s" % jar_name(e))
    for f in failures:
        sys.stderr.write("ERROR: " + f + "\n")
    return 1 if failures else 0


def cmd_verify(args, entries):
    failures = []
    for e in entries:
        path = os.path.join(args.dir, jar_name(e))
        if not os.path.exists(path):
            failures.append("%s: absent" % jar_name(e))
            continue
        actual = sha256_of(path)
        if actual != e["sha256"]:
            failures.append("%s: sha256 %s != esperado %s" % (jar_name(e), actual, e["sha256"]))
    for f in failures:
        sys.stderr.write("ERROR: " + f + "\n")
    if not failures:
        print("OK    %d depend\u00e8ncies verificades" % len(entries))
    return 1 if failures else 0


def cmd_update(args, entries):
    changed = []
    for e in entries:
        if e["tag"] == "legacy":
            print("PIN   %s:%s:%s (refer\u00e8ncia, no s'actualitza)" % (e["group"], e["artifact"], e["version"]))
            continue
        try:
            rel = latest_release(e["group"], e["artifact"])
        except Exception as exc:  # noqa: BLE001
            sys.stderr.write("WARN  %s:%s no s'ha pogut consultar: %s\n"
                             % (e["group"], e["artifact"], exc))
            continue
        if rel is not None and rel.startswith(e["version"] + "-"):
            # Mateixa versi\u00f3 en una variant d'arquitectura (p. ex. -jdk5).
            print("SAME  %s:%s:%s" % (e["group"], e["artifact"], e["version"]))
            continue
        if rel is None or rel == e["version"]:
            print("SAME  %s:%s:%s" % (e["group"], e["artifact"], e["version"]))
            continue
        old = e["version"]
        e["version"] = rel
        if args.check:
            print("NEW   %s:%s %s -> %s" % (e["group"], e["artifact"], old, rel))
            changed.append("%s:%s %s -> %s" % (e["group"], e["artifact"], old, rel))
            e["version"] = old
            continue
        try:
            download(e, args.dir)
        except Exception as exc:  # noqa: BLE001
            sys.stderr.write("ERROR %s:%s -> %s no s'ha pogut baixar: %s\n"
                             % (e["group"], e["artifact"], rel, exc))
            e["version"] = old
            continue
        e["sha256"] = sha256_of(os.path.join(args.dir, jar_name(e)))
        changed.append("%s:%s %s -> %s" % (e["group"], e["artifact"], old, rel))
        print("UP    %s:%s %s -> %s" % (e["group"], e["artifact"], old, rel))
    if args.check:
        if changed:
            print("")
            print("Versions noves disponibles (el lock no s'ha modificat):")
            for c in changed:
                print("  " + c)
        else:
            print("")
            print("Cap versio nova disponible.")
        return 0
    write_lock(args.lock, entries)
    if changed:
        print("")
        print("Canvis al lock:")
        for c in changed:
            print("  " + c)
        print("Revisa els canvis d'API abans de commitar.")
    else:
        print("")
        print("Cap versio nova disponible.")
    return 0


def main(argv):
    ap = argparse.ArgumentParser(description="Gestiona les depend\u00e8ncies de Biblioteca.")
    ap.add_argument("op", choices=["fetch", "verify", "update", "classpath"])
    ap.add_argument("--lock", default=DEFAULT_LOCK)
    ap.add_argument("--dir", default=DEFAULT_DIR)
    ap.add_argument("--tag", default="runtime", choices=list(TAGS) + ["all"])
    ap.add_argument("--skip-missing", action="store_true",
                    help="no baixar els jars que faltin (nom\u00e9s verificaci\u00f3)")
    ap.add_argument("--check", action="store_true",
                    help="amb 'update': informa de les versions noves sense tocar el lock")
    args = ap.parse_args(argv)

    if not os.path.exists(args.lock):
        sys.stderr.write("ERROR: no existeix el lock %s\n" % args.lock)
        return 2
    entries = read_lock(args.lock)
    if args.op == "classpath":
        return cmd_classpath(args, entries)
    if args.op == "fetch":
        return cmd_fetch(args, entries)
    if args.op == "verify":
        return cmd_verify(args, entries)
    return cmd_update(args, entries)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
