#!/usr/bin/env bash
# Compila, executa els tests unitaris/d'integració i, opcionalment, UIAudit.
# Executa tots els passos sol·licitats; imprimeix un resum d'errors consolidat al final.
# Ús:
#   ./checkBiblio/run.sh              # tests + UIAudit interactiu
#   ./checkBiblio/run.sh --test-only  # només tests (make test)
#   ./checkBiblio/run.sh --auto       # tests + UIAudit --auto
#   ./checkBiblio/run.sh --audit-only # només UIAudit (salta els tests)
cd "$(dirname "$0")/.."

RUN_TESTS=1
RUN_AUDIT=1
AUDIT_ARGS=()
STEP_ERRORS=()
TEST_LOG=""
FINAL_EXIT=0

for arg in "$@"; do
    case "$arg" in
        --test-only)  RUN_AUDIT=0 ;;
        --audit-only) RUN_TESTS=0 ;;
        --auto)       AUDIT_ARGS+=(--auto) ;;
        -h|--help)
            echo "Ús: $0 [--test-only | --audit-only] [--auto]"
            exit 0
            ;;
        *) AUDIT_ARGS+=("$arg") ;;
    esac
done

record_error() {
    STEP_ERRORS+=("$1")
    FINAL_EXIT=1
}

print_final_summary() {
    local audit_issues audit_totals

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

    if [ -n "$TEST_LOG" ] && [ -f "$TEST_LOG" ]; then
        local test_lines
        test_lines=$(grep -E 'FAIL|FAILED|AssertionError|Exception|ERROR:|Tests run:|Failures:|errors:' "$TEST_LOG" 2>/dev/null \
            | grep -v '^make\[' || true)
        if [ -n "$test_lines" ]; then
            echo ""
            echo "  Sortida dels tests (errors / fallades):"
            echo "$test_lines" | sed 's/^/    /'
        fi
    fi

    if [ -f checkBiblio/audit_report.txt ]; then
        audit_issues=$(grep -E '\] (FAIL|WARN|ERROR):|FATAL:|APP LAUNCH ERROR' checkBiblio/audit_report.txt 2>/dev/null \
            | grep -vE '\] FAIL: [0-9]+  WARN: [0-9]+$' || true)
        audit_totals=$(grep -E '\] FAIL: [0-9]+  WARN: [0-9]+|^\[AUTO\] Audit complete' checkBiblio/audit_report.txt 2>/dev/null | tail -2 || true)
        if [ -n "$audit_totals" ]; then
            echo ""
            echo "  Totals d'UIAudit:"
            echo "$audit_totals" | sed 's/^/    /'
        fi
        if [ -n "$audit_issues" ]; then
            echo ""
            echo "  Problemes d'UIAudit (FAIL / WARN / ERROR):"
            echo "$audit_issues" | sed 's/^/    /'
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
    [ -n "${XVFB_PID:-}" ] && kill "$XVFB_PID" 2>/dev/null || true
    [ -n "$TEST_LOG" ] && [ -f "$TEST_LOG" ] && rm -f "$TEST_LOG"
}
trap cleanup EXIT

if ! command -v java &>/dev/null; then echo "ERROR: java no es troba al PATH" >&2; exit 1; fi
for jar in lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar lib/gson-2.11.0.jar; do
    [ -f "$jar" ] || { echo "ERROR: manca $jar" >&2; exit 1; }
done

if [ "$RUN_TESTS" -eq 1 ]; then
    echo "══════════════════════════════════════"
    echo "  Executant make test (BibliotecaTest + JUnit 5)"
    echo "══════════════════════════════════════"
    TEST_LOG=$(mktemp)
    make test 2>&1 | tee "$TEST_LOG"
    TEST_RC=${PIPESTATUS[0]}
    if [ "$TEST_RC" -ne 0 ]; then
        record_error "make test ha fallat (sortida $TEST_RC)"
    fi
    echo ""
fi

if [ "$RUN_AUDIT" -eq 1 ]; then
    CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar"

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

    echo "Compilant UIAudit..."
    if ! javac -Xlint:deprecation -cp "$CP" checkBiblio/UiTestSupport.java checkBiblio/UIAudit.java checkBiblio/I18nAudit.java -d bin 2>&1; then
        record_error "La compilació d'UIAudit ha fallat (javac)"
    else
        rm -f checkBiblio/audit_report.txt

        echo "Iniciant UIAudit..."
        java -Xmx512m -cp "$CP" checkBiblio.UIAudit "${AUDIT_ARGS[@]}" 2>&1
        AUDIT_RC=$?
        if [ "$AUDIT_RC" -ne 0 ]; then
            record_error "UIAudit ha sortit amb codi $AUDIT_RC"
        fi
    fi
fi

print_final_summary
exit "$FINAL_EXIT"
