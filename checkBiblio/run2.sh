#!/usr/bin/env bash
# Compile and run StressTest from the project root
set -e
cd "$(dirname "$0")/.."

TIMEOUT=${STRESS_TIMEOUT:-300}   # seconds; override with: STRESS_TIMEOUT=120 bash run2.sh

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:lib/javalin-6.3.0.jar:lib/kotlin-stdlib-2.0.21.jar"

echo "Compiling main app..."
make compile -s

echo "Compiling StressTest..."
javac -cp "$CP" checkBiblio/StressTest.java -d bin

echo "Starting StressTest... (timeout: ${TIMEOUT}s)"
java -cp "$CP" checkBiblio.StressTest "$@" &
JAVA_PID=$!

( sleep "$TIMEOUT"; echo ""; echo "[TIMEOUT] StressTest exceeded ${TIMEOUT}s — killing PID $JAVA_PID"; kill -- -$JAVA_PID 2>/dev/null; kill "$JAVA_PID" 2>/dev/null ) &
WATCHDOG_PID=$!

wait "$JAVA_PID"
EXIT_CODE=$?
kill "$WATCHDOG_PID" 2>/dev/null
wait "$WATCHDOG_PID" 2>/dev/null
exit "$EXIT_CODE"
