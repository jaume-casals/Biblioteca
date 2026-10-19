@echo off
setlocal EnableDelayedExpansion

REM Executa els harnesses de fuzz guiats per cobertura de Jazzer.
REM Mirror de `make fuzz-jazzer` a Linux.
REM JAZZER_FUZZ=1 commuta Jazzer del mode de regressió al mode de fuzzing.

set "CP=bin;lib\h2-2.5.252.jar;lib\mariadb-java-client-3.5.10.jar;lib\gson-2.14.0.jar;lib\apiguardian-api-1.1.2.jar;lib\junit-jupiter-api-5.14.4.jar;lib\junit-jupiter-params-5.14.4.jar;lib\junit-jupiter-engine-5.14.4.jar;lib\junit-platform-launcher-1.14.4.jar;lib\junit-platform-engine-1.14.4.jar;lib\junit-platform-commons-1.14.4.jar;lib\opentest4j-1.3.0.jar;lib\assertj-core-3.27.7.jar;lib\mockito-core-5.24.0.jar;lib\mockito-junit-jupiter-5.24.0.jar;lib\byte-buddy-1.18.15.jar;lib\byte-buddy-agent-1.18.15.jar;lib\objenesis-3.6.jar;lib\jqwik-api-1.10.2.jar;lib\jqwik-engine-1.10.2.jar;lib\jazzer-0.30.0.jar;lib\jazzer-api-0.30.0.jar;lib\jazzer-junit-0.30.0.jar"

call scripts\compile.bat
if errorlevel 1 exit /b 1

if not exist lib\jazzer-0.30.0.jar (
    if not exist lib mkdir lib
    curl -fsSL -o lib\jazzer-0.30.0.jar https://repo1.maven.org/maven2/com/code-intelligence/jazzer/0.30.0/jazzer-0.30.0.jar
)
if not exist lib\jazzer-api-0.30.0.jar (
    curl -fsSL -o lib\jazzer-api-0.30.0.jar https://repo1.maven.org/maven2/com/code-intelligence/jazzer-api/0.30.0/jazzer-api-0.30.0.jar
)
if not exist lib\jazzer-junit-0.30.0.jar (
    curl -fsSL -o lib\jazzer-junit-0.30.0.jar https://repo1.maven.org/maven2/com/code-intelligence/jazzer-junit/0.30.0/jazzer-junit-0.30.0.jar
)

set "OVERALL_RC=0"

echo === Jazzer: fuzz.herramienta.io.csv.Rfc4180FuzzTest ===
JAZZER_FUZZ=1 java -Dbiblioteca.test=true -jar lib\junit-platform-console-standalone-1.14.4.jar execute --select-class=fuzz.herramienta.io.csv.Rfc4180FuzzTest --details=tree --classpath "%CP%"
if errorlevel 1 set "OVERALL_RC=1"

echo === Jazzer: fuzz.herramienta.io.csv.TestFuzzUtilitatsCsv ===
JAZZER_FUZZ=1 java -Dbiblioteca.test=true -jar lib\junit-platform-console-standalone-1.14.4.jar execute --select-class=fuzz.herramienta.io.csv.TestFuzzUtilitatsCsv --details=tree --classpath "%CP%"
if errorlevel 1 set "OVERALL_RC=1"

exit /b %OVERALL_RC%
