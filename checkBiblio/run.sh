#!/usr/bin/env bash
# Compile, run unit/integration tests, and optionally UIAudit.
# Usage:
#   ./checkBiblio/run.sh              # tests + interactive UIAudit
#   ./checkBiblio/run.sh --test-only  # tests only (make test)
#   ./checkBiblio/run.sh --auto       # tests + UIAudit --auto
#   ./checkBiblio/run.sh --audit-only # UIAudit only (skip tests)
set -e
cd "$(dirname "$0")/.."

RUN_TESTS=1
RUN_AUDIT=1
AUDIT_ARGS=()

for arg in "$@"; do
    case "$arg" in
        --test-only)  RUN_AUDIT=0 ;;
        --audit-only) RUN_TESTS=0 ;;
        --auto)       AUDIT_ARGS+=(--auto) ;;
        -h|--help)
            echo "Usage: $0 [--test-only | --audit-only] [--auto]"
            exit 0
            ;;
        *) AUDIT_ARGS+=("$arg") ;;
    esac
done

if ! command -v java &>/dev/null; then echo "ERROR: java not found in PATH" >&2; exit 1; fi
for jar in lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar lib/gson-2.11.0.jar; do
    [ -f "$jar" ] || { echo "ERROR: missing $jar" >&2; exit 1; }
done

if [ "$RUN_TESTS" -eq 1 ]; then
    echo "══════════════════════════════════════"
    echo "  Running make test (BibliotecaTest + JUnit 5)"
    echo "══════════════════════════════════════"
    make test
    echo ""
fi

if [ "$RUN_AUDIT" -eq 0 ]; then
    exit 0
fi

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar"

# Auto-start Xvfb if no display is available
if [ -z "$DISPLAY" ]; then
    DISP=:99
    if [ -f /tmp/.X99-lock ]; then rm -f /tmp/.X99-lock; fi
    Xvfb "$DISP" -screen 0 1920x1080x24 &>/dev/null &
    XVFB_PID=$!
    export DISPLAY=$DISP
    sleep 0.5
    trap 'kill $XVFB_PID 2>/dev/null' EXIT
    echo "Started Xvfb on $DISP (PID $XVFB_PID)"
fi

echo "Compiling UIAudit..."
javac -Xlint:deprecation -cp "$CP" checkBiblio/UIAudit.java -d bin

rm -f checkBiblio/audit_report.txt
rm -f checkBiblio/screenshots/screen_*.png

echo "Starting UIAudit..."
exec java -Xmx512m -cp "$CP" checkBiblio.UIAudit "${AUDIT_ARGS[@]}"
