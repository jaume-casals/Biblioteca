@echo off
setlocal
REM El classpath ve del lock (build/deps.lock), no d'una llista escrita a mà.
call scripts\_deps.bat
if errorlevel 1 exit /b 1
set "CP=bin;%CP_RUNTIME%"
if exist bin rmdir /s /q bin
mkdir bin
dir /s /b src\*.java > classes.txt
javac -g --release 25 -cp "%CP%" @classes.txt -d bin
set ERR=%ERRORLEVEL%
del classes.txt 2>nul
if exist src\LICENSE copy /Y src\LICENSE bin\LICENSE >nul
if exist src\herramienta copy /Y src\herramienta\*.properties bin\herramienta\ >nul 2>&1
exit /b %ERR%
