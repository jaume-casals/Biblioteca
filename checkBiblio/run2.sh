#!/usr/bin/env bash
# Compile and run StressTest from the project root
set -e
cd "$(dirname "$0")/.."

CP="bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:lib/javalin-6.3.0.jar:lib/kotlin-stdlib-2.0.21.jar"

echo "Compiling main app..."
make compile -s

echo "Compiling StressTest..."
javac -cp "$CP" checkBiblio/StressTest.java -d bin

echo "Starting StressTest..."
java -cp "$CP" checkBiblio.StressTest "$@"
