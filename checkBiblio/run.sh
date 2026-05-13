#!/usr/bin/env bash
# Compile and run UIAudit from the project root
set -e
cd "$(dirname "$0")/.."

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:lib/javalin-6.3.0.jar:lib/kotlin-stdlib-2.0.21.jar"

# Compile main app first (in case it's stale)
echo "Compiling main app..."
make compile -s

# Compile UIAudit
echo "Compiling UIAudit..."
javac -cp "$CP" checkBiblio/UIAudit.java -d bin

# Run — pass --auto for automated mode, nothing for interactive
echo "Starting UIAudit..."
java -cp "$CP" checkBiblio.UIAudit "$@"
