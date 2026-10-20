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
            coord = m.group("coord")
            parts = coord.split(":")
            if len(parts) != 3:
                problems.append("l\u00ednia %d: coordenada incorrecte (grup:artifact:versio): %s" % (num, coord))
                continue
            group, artifact, version = parts
            tag = m.group("tag")
            # L'etiqueta es valida en el moment de llegir-la: una etiqueta
            # amb error (p. ex. "runtine") feia que el jar es baixés i
            # es verifiqués correctament, però no quedava en cap
            # classpath — la dependència faltava en silenci.
            if tag not in TAGS:
                problems.append("l\u00ednia %d: etiqueta desconeguda %r (v\u00e0lides: %s)"
                                % (num, tag, ", ".join(TAGS)))
                continue
            entries.append({
                "group": group,
                "artifact": artifact,
                "version": version,
                "sha256": m.group("sha"),
                "tag": tag,
            })
    if problems:
        for p in problems:
            sys.stderr.write("ERROR: " + p + "\n")
        raise SystemExit(2)
    return entries


def write_lock(path, entries):
    """Escriu el lock conservant-ne l'estructura.

    La capçalera, els encapçalaments de secció i els comentaris lliures
    es reprodueixen tal qual: només canvien les línies d'entrada whose
    versió o SHA-256 han canviat. Abans el fitxer es regenerava des de
    zero i es perdien els comentaris escrits a mà (la nota sobre el H2 de
    referència de build/deps.lock).

    L'escriptura és atòmica (fitxer temporal + os.replace): un
    `deps-update` interromput ja no deixa un lock truncat al repositori.
    """
    by_key = {}
    for e in entries:
        by_key[(e["group"], e["artifact"], e["tag"])] = e

    lines = []
    seen = set()
    if os.path.exists(path):
        with open(path, encoding="utf-8") as fh:
            for raw in fh:
                line = raw.rstrip("\n")
                stripped = line.strip()
                m = None
                if stripped and not stripped.startswith("#"):
                    m = LINE_RE.match(stripped)
                if m:
                    parts = m.group("coord").split(":")
                    if len(parts) != 3:
                        lines.append(line)
                        continue
                    key = (parts[0], parts[1], m.group("tag"))
                    e = by_key.get(key)
                    if e is None:
                        continue  # entrada eliminada del lock
                    lines.append("%s:%s:%s %s %s"
                                 % (e["group"], e["artifact"], e["version"], e["sha256"], e["tag"]))
                    seen.add(key)
                else:
                    lines.append(line)
    else:
        lines = ["# build/deps.lock \u2014 font \u00fanica de veritat de les depend\u00e8ncies.",
                 "# Format: grup:artifact:versio sha256 etiqueta   (runtime | test | fuzz)",
                 "# Regenera amb: python3 scripts/fetch_deps.py update",
                 ""]

    # Entrades que no existien al lock original: s'afegeixen al final,
    # sota l'encapçalament de la seva etiqueta.
    missing = [e for e in entries
               if (e["group"], e["artifact"], e["tag"]) not in seen]
    for tag in TAGS:
        tagged = [e for e in missing if e["tag"] == tag]
        if not tagged:
            continue
        lines.append("# --- %s ---" % tag)
        for e in tagged:
            lines.append("%s:%s:%s %s %s"
                         % (e["group"], e["artifact"], e["version"], e["sha256"], e["tag"]))
        lines.append("")

    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")
        fh.flush()
        os.fsync(fh.fileno())
    os.replace(tmp, path)


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
    print(args.sep.join(out))
    return 0


def cmd_jar(args, entries):
    """Imprimeix el camí d'un artifacte del lock (un per línia).

    Serveix per als scripts .bat de Windows, que necessiten el camí del
    runner de JUnit sense escriure'l a mà.
    """
    found = []
    for e in entries:
        if args.tag in ("all", e["tag"]):
            if not args.artifact or args.artifact == e["artifact"]:
                found.append(os.path.join(args.dir, jar_name(e)))
    if not found:
        sys.stderr.write("ERROR: no s'ha trobat l'artifacte %r (etiqueta %s)\n"
                         % (args.artifact, args.tag))
        return 2
    for p in found:
        print(p)
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
    failed = []
    for e in entries:
        if e["tag"] == "legacy":
            print("PIN   %s:%s:%s (refer\u00e8ncia, no s'actualitza)" % (e["group"], e["artifact"], e["version"]))
            continue
        try:
            rel = latest_release(e["group"], e["artifact"])
        except Exception as exc:  # noqa: BLE001
            sys.stderr.write("WARN  %s:%s no s'ha pogut consultar: %s\n"
                             % (e["group"], e["artifact"], exc))
            failed.append("%s:%s no s'ha pogut consultar: %s" % (e["group"], e["artifact"], exc))
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
            failed.append("%s:%s -> %s no s'ha pogut baixar: %s" % (e["group"], e["artifact"], rel, exc))
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
        if failed:
            print("")
            print("Consultes fallides:")
            for f in failed:
                print("  " + f)
        # Amb --check el codi de sortida informa del resultat: 1 si hi ha
        # versions noves (el lock s'ha de revisar) o si alguna consulta
        # de metadades ha fallat (no es pot saber si hi ha versions noves).
        # Abans retornava 0 sempre, de manera que la CI quedava verda
        # tant si hi havia actualitzacions com si no.
        return 1 if (changed or failed) else 0
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
    ap.add_argument("op", choices=["fetch", "verify", "update", "classpath", "jar"])
    ap.add_argument("--lock", default=DEFAULT_LOCK)
    ap.add_argument("--dir", default=DEFAULT_DIR)
    ap.add_argument("--tag", default="runtime", choices=list(TAGS) + ["all"])
    ap.add_argument("--artifact", default="", help="amb 'jar': nom de l'artifacte a localitzar")
    ap.add_argument("--sep", default=os.pathsep,
                    help="separador del classpath (per defecte, el del sistema operatiu)")
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
    if args.op == "jar":
        return cmd_jar(args, entries)
    if args.op == "fetch":
        return cmd_fetch(args, entries)
    if args.op == "verify":
        return cmd_verify(args, entries)
    return cmd_update(args, entries)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
