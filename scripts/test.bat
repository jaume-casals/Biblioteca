@echo off
setlocal
set "CP=bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar;lib\apiguardian-api-1.1.2.jar;lib\junit-jupiter-api-5.11.4.jar;lib\junit-jupiter-params-5.11.4.jar;lib\junit-jupiter-engine-5.11.4.jar;lib\junit-platform-launcher-1.11.4.jar;lib\junit-platform-engine-1.11.4.jar;lib\junit-platform-commons-1.11.4.jar;lib\opentest4j-1.3.0.jar;lib\assertj-core-3.26.3.jar"
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
echo === Running BibliotecaTest (plain) ===
java -cp "%CP%" BibliotecaTest
set RC1=%ERRORLEVEL%
echo === Running BibliotecaJUnit5Test (JUnit 5) ===
java -Dbiblioteca.test=true -Dbiblioteca.h2.url="jdbc:h2:mem:junit5;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=BibliotecaJUnit5Test --details=summary --classpath "%CP%"
set RC2=%ERRORLEVEL%
echo === Running DominiPersistenciaJUnit5Test (JUnit 5) ===
java -Dbiblioteca.test=true -Dbiblioteca.h2.url="jdbc:h2:mem:domini_persistencia_junit5;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=DominiPersistenciaJUnit5Test --details=summary --classpath "%CP%"
set RC3=%ERRORLEVEL%
echo === Running RestoreSqlJUnit5Test (JUnit 5) ===
java -Dbiblioteca.test=true -Dbiblioteca.h2.url="jdbc:h2:mem:restore_sql_junit5;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=RestoreSqlJUnit5Test --details=summary --classpath "%CP%"
set RC4=%ERRORLEVEL%
if not "%RC1%"=="0" exit /b %RC1%
if not "%RC2%"=="0" exit /b %RC2%
if not "%RC3%"=="0" exit /b %RC3%
if not "%RC4%"=="0" exit /b %RC4%
exit /b 0
