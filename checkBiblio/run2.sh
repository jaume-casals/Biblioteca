#!/usr/bin/env bash
# Compile and run StressTest from the project root
set -e
cd "$(dirname "$0")/.."

# Validate required tools and JARs
if ! command -v java &>/dev/null; then echo "ERROR: java not found in PATH" >&2; exit 1; fi
for jar in lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar lib/gson-2.11.0.jar lib/javalin-6.3.0.jar lib/kotlin-stdlib-2.0.21.jar; do
    [ -f "$jar" ] || { echo "ERROR: missing $jar" >&2; exit 1; }
done

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

TIMEOUT=${STRESS_TIMEOUT:-300}   # seconds; override with: STRESS_TIMEOUT=120 bash run2.sh

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:lib/javalin-6.3.0.jar:lib/kotlin-stdlib-2.0.21.jar"

echo "Compiling main app..."
make compile -s

echo "Compiling StressTest..."
javac -cp "$CP" checkBiblio/StressTest.java -d bin

JAVA_PID=
WATCHDOG_PID=
cleanup() {
    [ -n "$WATCHDOG_PID" ] && kill "$WATCHDOG_PID"  2>/dev/null || true
    [ -n "$JAVA_PID"     ] && kill -- -"$JAVA_PID"  2>/dev/null || true
    [ -n "$JAVA_PID"     ] && kill    "$JAVA_PID"   2>/dev/null || true
    wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

rm -f checkBiblio/stress_report.txt
rm -f checkBiblio/screenshots/stress_*.png

echo "Starting StressTest... (timeout: ${TIMEOUT}s)"
setsid java -Xmx512m -cp "$CP" checkBiblio.StressTest "$@" &
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
exit "$EXIT_CODE"
