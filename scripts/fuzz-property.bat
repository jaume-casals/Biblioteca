@echo off
setlocal EnableDelayedExpansion

REM Executa els tests de fuzz basats en propietats de jqwik per a la capa de domini.
REM Mirror de `make fuzz-property` a Linux.
REM
REM El classpath ve del lock (build/deps.lock) i la classe es selecciona pel
REM nom real del fitxer (test/fuzz/domini/TestPropietatsInvariantsLlibre.java):
REM el nom antic fuzz.domini.LlibreInvariantPropertiesTest no existia i
REM l'script no executava cap test.

call scripts\_deps.bat
if errorlevel 1 exit /b 1
set "CP=bin;%CP_RUNTIME%%CP_TEST%%CP_FUZZ%"

call scripts\compile.bat
if errorlevel 1 exit /b 1

echo === jqwik: fuzz.domini.TestPropietatsInvariantsLlibre ===
java -Dbiblioteca.test=true -Dbiblioteca.h2.url="jdbc:h2:mem:fuzz_prop;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" -jar %JUNIT_STANDALONE% execute --select-class=fuzz.domini.TestPropietatsInvariantsLlibre --details=tree --fail-if-no-tests --classpath "%CP%"
exit /b %ERRORLEVEL%
