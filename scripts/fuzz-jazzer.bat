@echo off
setlocal EnableDelayedExpansion

REM Executa els harnesses de fuzz guiats per cobertura de Jazzer.
REM Mirror de `make fuzz-jazzer` a Linux.
REM JAZZER_FUZZ=1 commuta Jazzer del mode de regressió al mode de fuzzing.
REM
REM El classpath ve del lock (build/deps.lock).

call scripts\_deps.bat
if errorlevel 1 exit /b 1
set "CP=bin;%CP_RUNTIME%%CP_TEST%%CP_FUZZ%"

call scripts\compile.bat
if errorlevel 1 exit /b 1

set "OVERALL_RC=0"

echo === Jazzer: fuzz.herramienta.io.csv.Rfc4180FuzzTest ===
JAZZER_FUZZ=1 java -Dbiblioteca.test=true -jar %JUNIT_STANDALONE% execute --select-class=fuzz.herramienta.io.csv.Rfc4180FuzzTest --details=tree --fail-if-no-tests --classpath "%CP%"
if errorlevel 1 set "OVERALL_RC=1"

echo === Jazzer: fuzz.herramienta.io.csv.TestFuzzUtilitatsCsv ===
JAZZER_FUZZ=1 java -Dbiblioteca.test=true -jar %JUNIT_STANDALONE% execute --select-class=fuzz.herramienta.io.csv.TestFuzzUtilitatsCsv --details=tree --fail-if-no-tests --classpath "%CP%"
if errorlevel 1 set "OVERALL_RC=1"

exit /b %OVERALL_RC%
