#!/usr/bin/env bash
# scripts/fuzz-nightly.sh
# Executa cada harness de Jazzer en paral·lel fins que un falli (o Ctrl+C).
# Cada harness itera independentment — l'anotació @FuzzTest(maxDuration="30s")
# limita cada invocació de Jazzer, i després es torna a llançar amb el
# corpus acumulat per a la iteració següent.
#
# Logs: /tmp/jazzer/YYYYMMDD-HHMMSS.log (datat) i
#       /tmp/jazzer/latest.log (enllaç simbòlic al més recent)
# Fallades: es mouen a fuzz-corpus/crashes/ (gitignored) amb marca de temps.
#
# Ús:
#   scripts/fuzz-nightly.sh                 # en primer pla
#   nohup scripts/fuzz-nightly.sh &         # en segon pla, sobreviu a la sortida de la shell
#   tail -f /tmp/jazzer/latest.log          # segueix el progrés
#
# Codis de sortida:
#   0 = aturat netament (Ctrl+C, kill, o cap harness ha fallat)
#   1 = una execució de Jazzer ha trobat una fallada; mira /tmp/jazzer/latest.log i
#       fuzz-corpus/crashes/

set -u
cd "$(dirname "$0")/.."

LOGDIR=/tmp/jazzer
TS=$(date +%Y%m%d-%H%M%S)
LOG=$LOGDIR/$TS.log
LATEST=$LOGDIR/latest.log
CRASH_DIR=fuzz-corpus/crashes
mkdir -p "$LOGDIR" "$CRASH_DIR"
# Fitxer marcador: quan és present, tots els bucles en segon pla surten.
STOP=$LOGDIR/.stop
rm -f "$STOP"

# Llista de harnesses — mantén-la sincronitzada amb la diana fuzz-jazzer del Makefile.
HARNESSES=(
    fuzz.herramienta.Rfc4180FuzzTest
    fuzz.herramienta.CsvUtilsFuzzTest
    fuzz.domini.LlibreFilterBuilderFuzzTest
    fuzz.domini.SortSpecFuzzTest
    fuzz.domini.ShelfParserFuzzTest
)

CRASHED_CLS=""
CRASHED_LOG=""

run_one() {
    local cls=$1
    while [ ! -f "$STOP" ]; do
        printf '\n=== %s %s ===\n' "$(date -Iseconds)" "$cls" >> "$LOG"
        if make fuzz-jazzer JARZER_SEL="$cls" >> "$LOG" 2>&1; then
            continue
        fi
        # Fallada — el primer que escriu guanya.
        if [ -z "$CRASHED_CLS" ]; then
            CRASHED_CLS=$cls
            CRASHED_LOG=$LOG
            printf '\n!!! CRASH a %s a les %s — mira els fitxers de fallada a %s/\n' \
                "$cls" "$(date -Iseconds)" "$CRASH_DIR" >> "$LOG"
            find . -maxdepth 1 -name 'crash-*' -exec mv {} "$CRASH_DIR/" \; 2>/dev/null
            ls -la "$CRASH_DIR" >> "$LOG" 2>&1
            touch "$STOP"
        fi
        return 1
    done
}

ln -sfn "$TS.log" "$LATEST"
printf 'inici fuzz-nightly: %s, log=%s, harnesses=%d\n' \
    "$(date -Iseconds)" "$LOG" "${#HARNESSES[@]}" | tee -a "$LOG"

# Llança cada harness en segon pla. El control de feines de bash gestiona
# la invocació paral·lela; simplement fem `wait` per esperar-ne qualsevol.
PIDS=()
for h in "${HARNESSES[@]}"; do
    run_one "$h" &
    PIDS+=($!)
done

trap 'printf "\naturant (log=%s)...\n" "$LOG" | tee -a "$LOG"; touch "$STOP"' INT TERM
wait "${PIDS[@]}" 2>/dev/null
rm -f "$STOP"

printf '\nfi fuzz-nightly: %s\n' "$(date -Iseconds)" >> "$LOG"

if [ -n "$CRASHED_CLS" ]; then
    printf '\n!!! CRASH a %s — mira %s i %s/\n' \
        "$CRASHED_CLS" "$CRASHED_LOG" "$CRASH_DIR"
    exit 1
fi
printf 'aturat netament: log=%s\n' "$LOG"
