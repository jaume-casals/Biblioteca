@echo off
setlocal
REM Executa TraceRoundtrip amb una BD H2 en memòria per fer proves puntuals d'anada i tornada.
REM El classpath ve del lock (build/deps.lock).
call scripts\_deps.bat
if errorlevel 1 exit /b 1
set "CP=bin;%CP_RUNTIME%%CP_TEST%"
java -cp "%CP%" -Dbiblioteca.test=true -Dbiblioteca.h2.url="jdbc:h2:mem:trace;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1" TraceRoundtrip
