@echo off
setlocal EnableDelayedExpansion

REM Executa els tests de fuzz basats en propietats de jqwik per a la capa de domini.
REM Mirror de `make fuzz-property` a Linux.

set "CP=bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar;lib\apiguardian-api-1.1.2.jar;lib\junit-jupiter-api-5.11.4.jar;lib\junit-jupiter-params-5.11.4.jar;lib\junit-jupiter-engine-5.11.4.jar;lib\junit-platform-launcher-1.11.4.jar;lib\junit-platform-engine-1.11.4.jar;lib\junit-platform-commons-1.11.4.jar;lib\opentest4j-1.3.0.jar;lib\assertj-core-3.26.3.jar;lib\mockito-core-5.14.2.jar;lib\mockito-junit-jupiter-5.14.2.jar;lib\byte-buddy-1.15.4.jar;lib\byte-buddy-agent-1.15.4.jar;lib\objenesis-3.3.jar;lib\jqwik-api-1.9.0.jar;lib\jqwik-engine-1.9.0.jar;lib\jazzer-0.24.0.jar;lib\jazzer-api-0.24.0.jar;lib\jazzer-junit-0.24.0.jar"

call scripts\compile.bat
if errorlevel 1 exit /b 1

if not exist lib\jqwik-api-1.9.0.jar (
    if not exist lib mkdir lib
    curl -fsSL -o lib\jqwik-api-1.9.0.jar https://repo1.maven.org/maven2/net/jqwik/jqwik-api/1.9.0/jqwik-api-1.9.0.jar
)
if not exist lib\jqwik-engine-1.9.0.jar (
    curl -fsSL -o lib\jqwik-engine-1.9.0.jar https://repo1.maven.org/maven2/net/jqwik/jqwik-engine/1.9.0/jqwik-engine-1.9.0.jar
)

echo === jqwik: fuzz.domini.LlibreInvariantPropertiesTest ===
java -Dbiblioteca.test=true -Dbiblioteca.h2.url="jdbc:h2:mem:fuzz_prop;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" -jar lib\junit-platform-console-standalone-1.11.4.jar execute --select-class=fuzz.domini.LlibreInvariantPropertiesTest --details=tree --classpath "%CP%"
exit /b %ERRORLEVEL%
