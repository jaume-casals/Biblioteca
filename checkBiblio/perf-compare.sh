#!/usr/bin/env bash
# Compara el banc de rendiment entre la base i l'estat optimitzat.
#
#   ./checkBiblio/perf-compare.sh [--runs N] [args de PerfBench]
#
# La base es construeix en una CÒPIA del working tree on es reverteixen
# només les optimitzacions de rendiment (vegeu checkBiblio/perf-baseline.py).
# El repositori no es toca mai.
#
# Perquè el resultat sigui fiable, cada versió s'executa en --runs JVM
# independents (5 per defecte) i es resumeix amb la mediana de les medianes
# i el mínim dels millors: una sola execució és massa sensible a la càrrega
# de la màquina.
#
# Nota: el working tree conté feina en curs que no és d'aquesta tasca
# (galeria de cobertes, i18n, validació de còpies de seguretat). La base la
# conserva, de manera que la diferència mesurada és exactament el canvi
# d'aquesta tasca.
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD"

RUNS=5
if [ "${1:-}" = "--runs" ]; then RUNS="$2"; shift 2; fi

BASE_DIR="$(mktemp -d /tmp/perfbase.XXXXXX)"
cleanup() { rm -rf "$BASE_DIR"; }
trap cleanup EXIT

tar -cf - --exclude=.git --exclude=bin --exclude=target \
    --exclude=./build/.build-tmp\* --exclude=./build/reports . \
  | (cd "$BASE_DIR" && tar -xf -)
python3 checkBiblio/perf-baseline.py "$BASE_DIR"

build_bench() {
  local dir="$1" tag="$2"
  ( cd "$dir" && make compile >/dev/null 2>&1 ) || { echo "FALLA la compilació ($tag)"; ( cd "$dir" && make compile ); exit 1; }
  ( cd "$dir" && javac -cp "bin:lib/*" -d bin checkBiblio/PerfBench.java )
}

# Les execucions s'alternen base/opt perquè una deriva lenta de la màquina
# (càrrega, temperatura, freqüència) afecti les dues versions per igual.
build_bench "$BASE_DIR" base
build_bench "$ROOT" opt
: > /tmp/perfbench-base.raw
: > /tmp/perfbench-opt.raw
for i in $(seq 1 "$RUNS"); do
  echo "--- base execució $i/$RUNS ---" >> /tmp/perfbench-base.raw
  ( cd "$BASE_DIR" && java -Dbiblioteca.test=true -cp "bin:lib/*" PerfBench "$@" ) \
    | grep -E "^(llista|domini|pagina|cerca|actualitza|importa|cobertures|MAP|SQL )|^    per-pagina" >> /tmp/perfbench-base.raw
  echo "--- opt execució $i/$RUNS ---" >> /tmp/perfbench-opt.raw
  ( cd "$ROOT" && java -Dbiblioteca.test=true -cp "bin:lib/*" PerfBench "$@" ) \
    | grep -E "^(llista|domini|pagina|cerca|actualitza|importa|cobertures|MAP|SQL )|^    per-pagina" >> /tmp/perfbench-opt.raw
done

echo ""
python3 - "$RUNS" <<'PY'
import re, sys, statistics
runs = int(sys.argv[1])

def parse(path):
    out = {}
    perpage = []
    for line in open(path, encoding='utf-8'):
        m = re.match(r'^(\S[^=]*?)\s+millor=\s*([0-9.]+) ms(?:\s+mediana=\s*([0-9.]+) ms)?', line)
        if m:
            label = m.group(1).strip()
            out.setdefault(label, {'millor': [], 'mediana': []})
            out[label]['millor'].append(float(m.group(2)))
            if m.group(3):
                out[label]['mediana'].append(float(m.group(3)))
        pm = re.match(r'^\s+per-pagina \(ms\): ([0-9. |]+)$', line)
        if pm:
            vals = [float(x) for x in pm.group(1).replace('|', ' ').split()]
            perpage.append((sum(vals), max(vals), len(vals)))
    return out, perpage

base, base_pp = parse('/tmp/perfbench-base.raw')
opt, opt_pp = parse('/tmp/perfbench-opt.raw')

print(f"Resum de {runs} execucions per versio (H2 en memoria, 5000 llibres)")
print()
print(f"{'flux':<44}{'base millor':>13}{'opt millor':>13}{'var':>9}{'base med':>11}{'opt med':>10}")
print("-" * 100)
for label in base:
    if label not in opt:
        continue
    b_min = min(base[label]['millor']); o_min = min(opt[label]['millor'])
    b_med = statistics.median(base[label]['mediana']) if base[label]['mediana'] else float('nan')
    o_med = statistics.median(opt[label]['mediana']) if opt[label]['mediana'] else float('nan')
    var = (o_min / b_min - 1) * 100 if b_min else 0.0
    print(f"{label:<44}{b_min:>13.3f}{o_min:>13.3f}{var:>8.1f}%{b_med:>11.3f}{o_med:>10.3f}")

if base_pp and opt_pp:
    b_sum = min(p[0] for p in base_pp); o_sum = min(p[0] for p in opt_pp)
    b_max = min(p[1] for p in base_pp); o_max = min(p[1] for p in opt_pp)
    n = base_pp[0][2]
    print()
    print(f"Recorregut de les {n} pagines (mesura minima de {runs} execucions):")
    print(f"  suma de les {n} pagines: base {b_sum:8.1f} ms   opt {o_sum:8.1f} ms   "
          f"{(o_sum / b_sum - 1) * 100:+.1f}%")
    print(f"  pagina mes lenta:       base {b_max:8.1f} ms   opt {o_max:8.1f} ms   "
          f"{(o_max / b_max - 1) * 100:+.1f}%")
PY
