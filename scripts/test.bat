@echo off
setlocal EnableDelayedExpansion
set "CP=bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar;lib\apiguardian-api-1.1.2.jar;lib\junit-jupiter-api-5.11.4.jar;lib\junit-jupiter-params-5.11.4.jar;lib\junit-jupiter-engine-5.11.4.jar;lib\junit-platform-launcher-1.11.4.jar;lib\junit-platform-engine-1.11.4.jar;lib\junit-platform-commons-1.11.4.jar;lib\opentest4j-1.3.0.jar;lib\assertj-core-3.26.3.jar;lib\mockito-core-5.14.2.jar;lib\mockito-junit-jupiter-5.14.2.jar;lib\byte-buddy-1.15.4.jar;lib\byte-buddy-agent-1.15.4.jar;lib\objenesis-3.3.jar"
if exist bin rmdir /s /q bin
call scripts\compile.bat
if errorlevel 1 exit /b 1
if exist scripts\patch_tests_after_web_removal.py (
  python scripts\patch_tests_after_web_removal.py 2>nul || python3 scripts\patch_tests_after_web_removal.py 2>nul
)
dir /s /b test\*.java > test_classes.txt
javac -g -cp "%CP%" @test_classes.txt -d bin
if errorlevel 1 (
  del test_classes.txt
  exit /b 1
)
del test_classes.txt

set "OVERALL_RC=0"

echo === Executant BibliotecaTest (plain) ===
java -cp "%CP%" BibliotecaTest
if errorlevel 1 set "OVERALL_RC=1"

REM Descobreix i executa cada classe de test de JUnit 5. La convenció de noms
REM és que una classe de test és qualsevol *.java sota test\ el nom del qual
REM acaba en "Test" i que NO és el runner plain-Java "BibliotecaTest". Cada
REM classe obté la seva pròpia BD H2 en memòria perquè el patró de
REM reinicialització @BeforeEach dels tests existents funcioni.
REM `dir /s /b` retorna camins absoluts, i l'extracció de subcadenes
REM `set X=!Y:*\test\=!` de cmd.exe és fràgil, així que la conversió es fa
REM en Python via scripts\_build_test_classes.py.
set "DB_COUNTER=0"
REM Usa un ajudant de Python per enumerar les classes de test de JUnit 5.
REM Això evita l'extracció de subcadenes `set X=!Y:*\test\=!` fràgil de
REM cmd.exe (que es comporta de manera inconsistent quan el camí conté la
REM subcadena literal \test\ múltiples vegades o el camí usa barres
REM normals/invertides).
if exist test\_test_classes.txt del test\_test_classes.txt
for /f "delims=" %%C in ('dir /s /b test\*.java ^| findstr /i "Test\.java$"') do (
  set "FNAME=%%~nC"
  if /i not "!FNAME!"=="BibliotecaTest" echo %%C>> test\_test_classes.txt
)
REM Converteix camins absoluts a noms de classe FQ via Python. El separador
REM de directori es fa coincidir explícitament perquè os.sep és '/' a
REM Windows però la sortida de `dir` usa '\'.
if exist test\_test_classes.txt (
  python scripts\_build_test_classes.py 2>nul
  if errorlevel 1 python3 scripts\_build_test_classes.py 2>nul
)
for /f "usebackq delims=" %%L in ("test\_test_classes.txt") do (
  set "CLS=%%L"
  if not "!CLS!"=="" (
    set /a DB_COUNTER+=1
    set "DB_URL=jdbc:h2:mem:dyn_!DB_COUNTER!;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
    echo === Executant !CLS! (JUnit 5) ===
    java -Dbiblioteca.test=true -Dbiblioteca.h2.url="!DB_URL!" -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=!CLS! --details=summary --classpath "%CP%"
    if errorlevel 1 set "OVERALL_RC=1"
  )
)
if exist test\_test_classes.txt del test\_test_classes.txt

exit /b %OVERALL_RC%
