#!/usr/bin/env bash
# scripts/fuzz-nightly.sh
# Run every Jazzer harness in parallel until one crashes (or Ctrl+C).
# Each harness iterates independently — the @FuzzTest(maxDuration="30s")
# annotation caps each Jazzer invocation, then we re-launch with the
# accumulated corpus for the next iteration.
#
# Logs: /tmp/jazzer/YYYYMMDD-HHMMSS.log (dated) and
#       /tmp/jazzer/latest.log (symlink to most recent)
# Crashes: moved to fuzz-corpus/crashes/ (gitignored) with timestamp.
#
# Usage:
#   scripts/fuzz-nightly.sh                 # foreground
#   nohup scripts/fuzz-nightly.sh &         # background, survive shell exit
#   tail -f /tmp/jazzer/latest.log          # watch progress
#
# Exit codes:
#   0 = stopped cleanly (Ctrl+C, kill, or no harness crashed)
#   1 = a Jazzer run found a crash; see /tmp/jazzer/latest.log and
#       fuzz-corpus/crashes/

set -u
cd "$(dirname "$0")/.."

LOGDIR=/tmp/jazzer
TS=$(date +%Y%m%d-%H%M%S)
LOG=$LOGDIR/$TS.log
LATEST=$LOGDIR/latest.log
CRASH_DIR=fuzz-corpus/crashes
mkdir -p "$LOGDIR" "$CRASH_DIR"
# Marker file: when present, every background loop bails out.
STOP=$LOGDIR/.stop
rm -f "$STOP"

# Harness list — keep in sync with Makefile fuzz-jazzer target.
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
        # Crash — first writer wins.
        if [ -z "$CRASHED_CLS" ]; then
            CRASHED_CLS=$cls
            CRASHED_LOG=$LOG
            printf '\n!!! CRASH in %s at %s — see crash files in %s/\n' \
                "$cls" "$(date -Iseconds)" "$CRASH_DIR" >> "$LOG"
            find . -maxdepth 1 -name 'crash-*' -exec mv {} "$CRASH_DIR/" \; 2>/dev/null
            ls -la "$CRASH_DIR" >> "$LOG" 2>&1
            touch "$STOP"
        fi
        return 1
    done
}

ln -sfn "$TS.log" "$LATEST"
printf 'fuzz-nightly start: %s, log=%s, harnesses=%d\n' \
    "$(date -Iseconds)" "$LOG" "${#HARNESSES[@]}" | tee -a "$LOG"

# Launch each harness in the background. Bash's job control handles the
# parallel invocation; we just `wait` for any of them to exit.
PIDS=()
for h in "${HARNESSES[@]}"; do
    run_one "$h" &
    PIDS+=($!)
done

trap 'printf "\nstopping (log=%s)...\n" "$LOG" | tee -a "$LOG"; touch "$STOP"' INT TERM
wait "${PIDS[@]}" 2>/dev/null
rm -f "$STOP"

printf '\nfuzz-nightly end: %s\n' "$(date -Iseconds)" >> "$LOG"

if [ -n "$CRASHED_CLS" ]; then
    printf '\n!!! CRASH in %s — see %s and %s/\n' \
        "$CRASHED_CLS" "$CRASHED_LOG" "$CRASH_DIR"
    exit 1
fi
printf 'stopped cleanly: log=%s\n' "$LOG"
