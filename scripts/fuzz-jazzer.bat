@echo off
setlocal EnableDelayedExpansion

REM Run the Jazzer coverage-guided fuzz harnesses.
REM Mirrors `make fuzz-jazzer` on Linux.
REM JAZZER_FUZZ=1 switches Jazzer from regression mode to fuzzing mode.

set "CP=bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar;lib\apiguardian-api-1.1.2.jar;lib\junit-jupiter-api-5.11.4.jar;lib\junit-jupiter-params-5.11.4.jar;lib\junit-jupiter-engine-5.11.4.jar;lib\junit-platform-launcher-1.11.4.jar;lib\junit-platform-engine-1.11.4.jar;lib\junit-platform-commons-1.11.4.jar;lib\opentest4j-1.3.0.jar;lib\assertj-core-3.26.3.jar;lib\mockito-core-5.14.2.jar;lib\mockito-junit-jupiter-5.14.2.jar;lib\byte-buddy-1.15.4.jar;lib\byte-buddy-agent-1.15.4.jar;lib\objenesis-3.3.jar;lib\jqwik-api-1.9.0.jar;lib\jqwik-engine-1.9.0.jar;lib\jazzer-0.24.0.jar;lib\jazzer-api-0.24.0.jar;lib\jazzer-junit-0.24.0.jar"

call scripts\compile.bat
if errorlevel 1 exit /b 1

if not exist lib\jazzer-0.24.0.jar (
    if not exist lib mkdir lib
    curl -fsSL -o lib\jazzer-0.24.0.jar https://repo1.maven.org/maven2/com/code-intelligence/jazzer/0.24.0/jazzer-0.24.0.jar
)
if not exist lib\jazzer-api-0.24.0.jar (
    curl -fsSL -o lib\jazzer-api-0.24.0.jar https://repo1.maven.org/maven2/com/code-intelligence/jazzer-api/0.24.0/jazzer-api-0.24.0.jar
)
if not exist lib\jazzer-junit-0.24.0.jar (
    curl -fsSL -o lib\jazzer-junit-0.24.0.jar https://repo1.maven.org/maven2/com/code-intelligence/jazzer-junit/0.24.0/jazzer-junit-0.24.0.jar
)

set "OVERALL_RC=0"

echo === Jazzer: fuzz.herramienta.Rfc4180FuzzTest ===
JAZZER_FUZZ=1 java -Dbiblioteca.test=true -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=fuzz.herramienta.Rfc4180FuzzTest --details=tree --classpath "%CP%"
if errorlevel 1 set "OVERALL_RC=1"

echo === Jazzer: fuzz.herramienta.CsvUtilsFuzzTest ===
JAZZER_FUZZ=1 java -Dbiblioteca.test=true -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=fuzz.herramienta.CsvUtilsFuzzTest --details=tree --classpath "%CP%"
if errorlevel 1 set "OVERALL_RC=1"

exit /b %OVERALL_RC%
