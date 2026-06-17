#!/usr/bin/env bash
# Compila i executa StressTest des de l'arrel del projecte.
# Executa tots els passos; imprimeix un resum d'errors consolidat al final.
# Ús:
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
            echo "Ús: $0 [StressTest args...]"
            echo "  STRESS_EXTREME=1     mode extrem (per defecte threads 100, timeout 1800s)"
            echo "  STRESS_TIMEOUT=N     segons del watchdog (per defecte 600, 1800 en extrem)"
            echo "  STRESS_THREADS=N     fils de treball (per defecte 50, 100 en extrem)"
            echo "  STRESS_INSTANCES=N   en extrem: llança N JVMs fills (per defecte 3)"
            echo "  STRESS_SOAK=N        en extrem: activitat de BD en segon pla durant N segons (0=off)"
            echo "  STRESS_FUZZ=N        en extrem: cadenes aleatòries per diàleg a la fase de fuzz (per defecte 25)"
            echo "  STRESS_MEMPROBE=0|1  en extrem: sondatge del creixement del heap (per defecte 1)"
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
    echo "  RESUM FINAL"
    echo "════════════════════════════════════════════════════════"

    if [ ${#STEP_ERRORS[@]} -eq 0 ]; then
        echo "  Passos: no s'han registrat fallades."
    else
        echo "  Fallades de pas (${#STEP_ERRORS[@]}):"
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
            echo "  Totals de StressTest:"
            echo "$stress_totals" | sed 's/^/    /'
        fi
        fail_n=$(grep 'FAIL :' checkBiblio/stress_report.txt 2>/dev/null | tail -1 | sed 's/.*FAIL : //' | tr -d ' \r' || true)
        if [ -n "$fail_n" ] && [ "$fail_n" -gt 0 ] 2>/dev/null; then
            FINAL_EXIT=1
        fi
        stress_issues=$(grep -E '✗ FAIL:|! WARN:|FATAL:|APP LAUNCH ERROR' checkBiblio/stress_report.txt 2>/dev/null || true)
        if [ -n "$stress_issues" ]; then
            echo ""
            echo "  Problemes de StressTest (FAIL / WARN / FATAL):"
            echo "$stress_issues" | sed 's/^/    /'
        fi
    fi

    echo "════════════════════════════════════════════════════════"
    if [ "$FINAL_EXIT" -eq 0 ]; then
        echo "  Resultat: PASS"
    else
        echo "  Resultat: FAIL (mira amunt)"
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

if ! command -v java &>/dev/null; then echo "ERROR: java no es troba al PATH" >&2; exit 1; fi
for jar in lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar lib/gson-2.11.0.jar; do
    [ -f "$jar" ] || { echo "ERROR: manca $jar" >&2; exit 1; }
done

# Auto-inicia Xvfb si no hi ha cap pantalla disponible
if [ -z "$DISPLAY" ]; then
    DISP=:99
    if [ -f /tmp/.X99-lock ]; then rm -f /tmp/.X99-lock; fi
    Xvfb "$DISP" -screen 0 1920x1080x24 &>/dev/null &
    XVFB_PID=$!
    export DISPLAY=$DISP
    sleep 1
    if ! kill -0 "$XVFB_PID" 2>/dev/null; then
        echo "ERROR: Xvfb no ha pogut iniciar. Instal·la xvfb o defineix DISPLAY." >&2
        exit 1
    fi
    echo "Xvfb iniciat a $DISP (PID $XVFB_PID)"
fi

if [ -n "$STRESS_EXTREME" ]; then
    TIMEOUT=${STRESS_TIMEOUT:-1800}
    STRESS_THREADS=${STRESS_THREADS:-100}
    STRESS_INSTANCES=${STRESS_INSTANCES:-3}
    STRESS_SOAK=${STRESS_SOAK:-0}        # segons; 0 = desactivat
    STRESS_FUZZ=${STRESS_FUZZ:-25}       # càrregues per diàleg a la fase de fuzz
    STRESS_MEMPROBE=${STRESS_MEMPROBE:-1} # 0/1; per defecte activat
    STRESS_JAVA_OPTS="-Dbiblioteca.stress.extreme=true -Dbiblioteca.stress.threads=${STRESS_THREADS}"
    [ "$STRESS_INSTANCES" -gt 0 ] && STRESS_JAVA_OPTS="$STRESS_JAVA_OPTS -Dbiblioteca.stress.instances=${STRESS_INSTANCES}"
    [ "$STRESS_SOAK" -gt 0 ]      && STRESS_JAVA_OPTS="$STRESS_JAVA_OPTS -Dbiblioteca.stress.soak=${STRESS_SOAK}"
    STRESS_JAVA_OPTS="$STRESS_JAVA_OPTS -Dbiblioteca.stress.fuzz=${STRESS_FUZZ}"
    [ "$STRESS_MEMPROBE" = "1" ]  && STRESS_JAVA_OPTS="$STRESS_JAVA_OPTS -Dbiblioteca.stress.memprobe=true"
    echo "STRESS_EXTREME=1 (timeout=${TIMEOUT}s, threads=${STRESS_THREADS}"
    echo "                  instances=${STRESS_INSTANCES}, soak=${STRESS_SOAK}s, fuzz=${STRESS_FUZZ})"
else
    TIMEOUT=${STRESS_TIMEOUT:-600}
    STRESS_THREADS=${STRESS_THREADS:-50}
    STRESS_JAVA_OPTS="-Dbiblioteca.stress.threads=${STRESS_THREADS}"
fi

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar"

echo "Compilant l'aplicació principal..."
if ! make compile -s 2>&1; then
    record_error "make compile ha fallat"
fi

STRESS_COMPILED=0
if [ "$FINAL_EXIT" -eq 0 ]; then
    echo "Compilant StressTest..."
    if ! javac -Xlint:deprecation -cp "$CP" checkBiblio/UiTestSupport.java checkBiblio/StressTest.java -d bin 2>&1; then
        record_error "La compilació de StressTest ha fallat (javac)"
    else
        STRESS_COMPILED=1
    fi
fi

if [ "$STRESS_COMPILED" -eq 1 ]; then
    rm -f checkBiblio/stress_report.txt

    echo "Iniciant StressTest... (timeout: ${TIMEOUT}s)"
    if command -v setsid &>/dev/null; then
        setsid java -Xmx512m $STRESS_JAVA_OPTS -cp "$CP" checkBiblio.StressTest "$@" &
    else
        java -Xmx512m $STRESS_JAVA_OPTS -cp "$CP" checkBiblio.StressTest "$@" &
    fi
    JAVA_PID=$!

    ( sleep "$TIMEOUT"
      echo "" >&2
      echo "[TIMEOUT] StressTest ha superat ${TIMEOUT}s — matant PID $JAVA_PID" >&2
      kill -- -"$JAVA_PID" 2>/dev/null || true
      kill    "$JAVA_PID"  2>/dev/null || true ) &
    WATCHDOG_PID=$!

    EXIT_CODE=0
    wait "$JAVA_PID" || EXIT_CODE=$?
    kill "$WATCHDOG_PID" 2>/dev/null || true
    wait "$WATCHDOG_PID" 2>/dev/null || true

    if [ "$EXIT_CODE" -ne 0 ]; then
        record_error "StressTest ha sortit amb codi $EXIT_CODE"
    fi
    if ! grep -q 'SUMMARY' checkBiblio/stress_report.txt 2>/dev/null; then
        record_error "StressTest no ha acabat (timeout ${TIMEOUT}s o fallada) — mira checkBiblio/stress_report.txt"
    fi
fi

print_final_summary
exit "$FINAL_EXIT"
