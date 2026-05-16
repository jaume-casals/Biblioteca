#!/usr/bin/env bash
# Compile and run UIAudit from the project root
set -e
cd "$(dirname "$0")/.."

# Validate required tools and JARs
if ! command -v java &>/dev/null; then echo "ERROR: java not found in PATH" >&2; exit 1; fi
for jar in lib/h2-2.3.232.jar lib/mariadb-java-client-3.3.3.jar lib/gson-2.11.0.jar lib/javalin-6.3.0.jar lib/kotlin-stdlib-2.0.21.jar; do
    [ -f "$jar" ] || { echo "ERROR: missing $jar" >&2; exit 1; }
done

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:lib/javalin-6.3.0.jar:lib/kotlin-stdlib-2.0.21.jar"

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

# Compile main app first (in case it's stale)
echo "Compiling main app..."
make compile -s

# Compile UIAudit
echo "Compiling UIAudit..."
javac -cp "$CP" checkBiblio/UIAudit.java -d bin

rm -f checkBiblio/audit_report.txt
rm -f checkBiblio/screenshots/screen_*.png

# Run — pass --auto for automated mode, nothing for interactive
echo "Starting UIAudit..."
exec java -Xmx512m -cp "$CP" checkBiblio.UIAudit "$@"
