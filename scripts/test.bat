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

echo === Running BibliotecaTest (plain) ===
java -cp "%CP%" BibliotecaTest
if errorlevel 1 set "OVERALL_RC=1"

REM Discover and run every JUnit 5 test class. The naming convention is that a
REM test class is any *.java under test\ whose name ends in "Test" and that is
REM NOT the plain-Java runner "BibliotecaTest". Each class gets its own H2
REM in-memory DB so the @BeforeEach reset pattern in the existing tests works.
REM We run the discovery from inside test\ so the captured paths are
REM relative — otherwise %%~dpC yields absolute paths and the package
REM derivation below strips nothing, producing broken class names.
set "DB_COUNTER=0"
REM Use a Python helper to enumerate JUnit 5 test classes. This avoids
REM the brittle `set X=!Y:*\test\=!` substring extraction in cmd.exe
REM (which behaves inconsistently when the path contains the literal
REM substring \test\ multiple times or the path uses forward/back slashes).
if exist test\_test_classes.txt del test\_test_classes.txt
for /f "delims=" %%C in ('dir /s /b test\*.java ^| findstr /i "Test\.java$"') do (
  set "FNAME=%%~nC"
  if /i not "!FNAME!"=="BibliotecaTest" echo %%C>> test\_test_classes.txt
)
REM Convert absolute paths to FQ class names via Python
if exist test\_test_classes.txt (
  python -c "import os, sys, pathlib; lines=open(r'test\_test_classes.txt', encoding='utf-8').read().splitlines(); out=[os.path.join(os.path.dirname(p), os.path.splitext(os.path.basename(p))[0]).replace(os.sep, '.') for p in lines if p]; open(r'test\_test_classes.txt','w', encoding='utf-8').write('\n'.join(out))" 2>nul
  if errorlevel 1 (
    python3 -c "import os, sys, pathlib; lines=open(r'test\_test_classes.txt', encoding='utf-8').read().splitlines(); out=[os.path.join(os.path.dirname(p), os.path.splitext(os.path.basename(p))[0]).replace(os.sep, '.') for p in lines if p]; open(r'test\_test_classes.txt','w', encoding='utf-8').write('\n'.join(out))" 2>nul
  )
)
for /f "usebackq delims=" %%L in ("test\_test_classes.txt") do (
  set "CLS=%%L"
  if not "!CLS!"=="" (
    set /a DB_COUNTER+=1
    set "DB_URL=jdbc:h2:mem:dyn_!DB_COUNTER!;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
    echo === Running !CLS! (JUnit 5) ===
    java -Dbiblioteca.test=true -Dbiblioteca.h2.url="!DB_URL!" -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=!CLS! --details=summary --classpath "%CP%"
    if errorlevel 1 set "OVERALL_RC=1"
  )
)
if exist test\_test_classes.txt del test\_test_classes.txt

exit /b %OVERALL_RC%
