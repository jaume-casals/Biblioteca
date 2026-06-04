#!/usr/bin/env bash
# Compile and run StressTest from the project root.
# Runs all steps; prints a consolidated error summary at the end.
# Usage:
#   ./checkBiblio/run2.sh
#   STRESS_EXTREME=1 ./checkBiblio/run2.sh
#   STRESS_TIMEOUT=900 STRESS_THREADS=80 ./checkBiblio/run2.sh
cd "$(dirname "$0")/.."

STEP_ERRORS=()
FINAL_EXIT=0
JAVA_PID=
WATCHDOG_PID=

for arg in "$@"; do
    case "$arg" in
        -h|--help)
            echo "Usage: $0 [StressTest args...]"
            echo "  STRESS_EXTREME=1     extreme mode (default threads 100)"
            echo "  STRESS_TIMEOUT=N     watchdog seconds (default 600)"
            echo "  STRESS_THREADS=N     worker threads (default 50, 100 if extreme)"
            exit 0
            ;;
    esac
done

record_error() {
    STEP_ERRORS+=("$1")
    FINAL_EXIT=1
}

print_final_summary() {
    local stress_issues stress_totals fail_n

    echo ""
    echo "════════════════════════════════════════════════════════"
    echo "  FINAL SUMMARY"
    echo "════════════════════════════════════════════════════════"

    if [ ${#STEP_ERRORS[@]} -eq 0 ]; then
        echo "  Steps: no failures recorded."
    else
        echo "  Step failures (${#STEP_ERRORS[@]}):"
        local err
        for err in "${STEP_ERRORS[@]}"; do
            echo "    ✗ $err"
        done
    fi

    if [ -f checkBiblio/stress_report.txt ]; then
        stress_totals=$(grep -E 'SUMMARY|PASS :|FAIL :|WARN :|TOTAL:' checkBiblio/stress_report.txt 2>/dev/null \
            | tail -6 || true)
        if [ -n "$stress_totals" ]; then
            echo ""
            echo "  StressTest totals:"
            echo "$stress_totals" | sed 's/^/    /'
        fi
        fail_n=$(grep 'FAIL :' checkBiblio/stress_report.txt 2>/dev/null | tail -1 | sed 's/.*FAIL : //' | tr -d ' \r' || true)
        if [ -n "$fail_n" ] && [ "$fail_n" -gt 0 ] 2>/dev/null; then
            FINAL_EXIT=1
        fi
        stress_issues=$(grep -E '✗ FAIL:|! WARN:|SCREENSHOT FAILED|FATAL:|APP LAUNCH ERROR' checkBiblio/stress_report.txt 2>/dev/null || true)
        if [ -n "$stress_issues" ]; then
            echo ""
            echo "  StressTest issues (FAIL / WARN / FATAL):"
            echo "$stress_issues" | sed 's/^/    /'
        fi
    fi

    echo "════════════════════════════════════════════════════════"
    if [ "$FINAL_EXIT" -eq 0 ]; then
        echo "  Result: PASS"
    else
        echo "  Result: FAIL (see above)"
    fi
    echo "════════════════════════════════════════════════════════"
}

cleanup() {
    [ -n "$WATCHDOG_PID" ] && kill "$WATCHDOG_PID" 2>/dev/null || true
    [ -n "$JAVA_PID"     ] && kill -- -"$JAVA_PID" 2>/dev/null || true
    [ -n "$JAVA_PID"     ] && kill    "$JAVA_PID"  2>/dev/null || true
    [ -n "${XVFB_PID:-}" ] && kill "$XVFB_PID" 2>/dev/null || true
    wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

if ! command -v java &>/dev/null; then echo "ERROR: java not found in PATH" >&2; exit 1; fi
for jar in lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar lib/gson-2.11.0.jar; do
    [ -f "$jar" ] || { echo "ERROR: missing $jar" >&2; exit 1; }
done

# Auto-start Xvfb if no display is available
if [ -z "$DISPLAY" ]; then
    DISP=:99
    if [ -f /tmp/.X99-lock ]; then rm -f /tmp/.X99-lock; fi
    Xvfb "$DISP" -screen 0 1920x1080x24 &>/dev/null &
    XVFB_PID=$!
    export DISPLAY=$DISP
    sleep 1
    if ! kill -0 "$XVFB_PID" 2>/dev/null; then
        echo "ERROR: Xvfb failed to start. Install xvfb or set DISPLAY." >&2
        exit 1
    fi
    echo "Started Xvfb on $DISP (PID $XVFB_PID)"
fi

if [ -n "$STRESS_EXTREME" ]; then
    TIMEOUT=${STRESS_TIMEOUT:-600}
    STRESS_THREADS=${STRESS_THREADS:-100}
    STRESS_JAVA_OPTS="-Dbiblioteca.stress.extreme=true -Dbiblioteca.stress.threads=${STRESS_THREADS}"
    echo "STRESS_EXTREME=1 (timeout=${TIMEOUT}s, threads=${STRESS_THREADS})"
else
    TIMEOUT=${STRESS_TIMEOUT:-600}
    STRESS_THREADS=${STRESS_THREADS:-50}
    STRESS_JAVA_OPTS="-Dbiblioteca.stress.threads=${STRESS_THREADS}"
fi

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar"

echo "Compiling main app..."
if ! make compile -s 2>&1; then
    record_error "make compile failed"
fi

STRESS_COMPILED=0
if [ "$FINAL_EXIT" -eq 0 ]; then
    echo "Compiling StressTest..."
    if ! javac -Xlint:deprecation -cp "$CP" checkBiblio/StressTest.java -d bin 2>&1; then
        record_error "StressTest compile failed (javac)"
    else
        STRESS_COMPILED=1
    fi
fi

if [ "$STRESS_COMPILED" -eq 1 ]; then
    rm -f checkBiblio/stress_report.txt
    rm -f checkBiblio/screenshots/stress_*.png

    echo "Starting StressTest... (timeout: ${TIMEOUT}s)"
    if command -v setsid &>/dev/null; then
        setsid java -Xmx512m $STRESS_JAVA_OPTS -cp "$CP" checkBiblio.StressTest "$@" &
    else
        java -Xmx512m $STRESS_JAVA_OPTS -cp "$CP" checkBiblio.StressTest "$@" &
    fi
    JAVA_PID=$!

    ( sleep "$TIMEOUT"
      echo "" >&2
      echo "[TIMEOUT] StressTest exceeded ${TIMEOUT}s — killing PID $JAVA_PID" >&2
      kill -- -"$JAVA_PID" 2>/dev/null || true
      kill    "$JAVA_PID"  2>/dev/null || true ) &
    WATCHDOG_PID=$!

    EXIT_CODE=0
    wait "$JAVA_PID" || EXIT_CODE=$?
    kill "$WATCHDOG_PID" 2>/dev/null || true
    wait "$WATCHDOG_PID" 2>/dev/null || true

    if [ "$EXIT_CODE" -ne 0 ]; then
        record_error "StressTest exited with code $EXIT_CODE"
    fi
    if ! grep -q 'SUMMARY' checkBiblio/stress_report.txt 2>/dev/null; then
        record_error "StressTest did not finish (timeout ${TIMEOUT}s or crash) — see checkBiblio/stress_report.txt"
    fi
fi

print_final_summary
exit "$FINAL_EXIT"
