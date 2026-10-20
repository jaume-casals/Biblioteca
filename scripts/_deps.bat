@echo off
REM Prepara les dependències i el classpath a partir de build/deps.lock.
REM
REM Els scripts .bat NO duen cap llista de jars escrita a mà: abans
REM referenciaven jars que el lock no conté (junit-jupiter-api-5.14.4,
REM junit-platform-launcher-1.14.4, mockito-junit-jupiter-5.24.0, ...) i
REM la compilació fallava amb "package org.junit.jupiter.api does not exist".
REM Ara el lock és l'única font de veritat: es baixen els jars que falten,
REM es verifiquen els SHA-256 i es deriva el classpath.
REM
REM Exporta: CP_RUNTIME, CP_TEST, CP_FUZZ i JUNIT_STANDALONE.

setlocal
set "PY=python"
python --version >nul 2>&1 || set "PY=python3"

%PY% scripts\fetch_deps.py fetch --lock build\deps.lock --dir lib
if errorlevel 1 exit /b 1

set "CP_RUNTIME="
set "CP_TEST="
set "CP_FUZZ="
set "JUNIT_STANDALONE="

for /f "delims=" %%C in ('%PY% scripts\fetch_deps.py classpath --tag runtime --dir lib') do set "CP_RUNTIME=%%C"
for /f "delims=" %%C in ('%PY% scripts\fetch_deps.py classpath --tag test --dir lib') do set "CP_TEST=%%C"
for /f "delims=" %%C in ('%PY% scripts\fetch_deps.py classpath --tag fuzz --dir lib') do set "CP_FUZZ=%%C"
for /f "delims=" %%C in ('%PY% scripts\fetch_deps.py jar --tag test --artifact junit-platform-console-standalone --dir lib') do set "JUNIT_STANDALONE=%%C"

if not defined JUNIT_STANDALONE (
  echo ERROR: no s'ha trobat el runner de JUnit al lock (build\deps.lock).
  exit /b 1
)

endlocal & set "CP_RUNTIME=%CP_RUNTIME%" & set "CP_TEST=%CP_TEST%" & set "CP_FUZZ=%CP_FUZZ%" & set "JUNIT_STANDALONE=%JUNIT_STANDALONE%"
exit /b 0
