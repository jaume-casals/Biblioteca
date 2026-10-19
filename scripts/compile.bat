@echo off
setlocal
set "CP=lib\h2-2.5.252.jar;lib\mariadb-java-client-3.5.10.jar;lib\gson-2.14.0.jar;."
if exist bin rmdir /s /q bin
mkdir bin
dir /s /b src\*.java > classes.txt
javac -g --release 25 -cp "%CP%" @classes.txt -d bin
set ERR=%ERRORLEVEL%
del classes.txt 2>nul
if exist src\LICENSE copy /Y src\LICENSE bin\LICENSE >nul
if exist src\herramienta copy /Y src\herramienta\*.properties bin\herramienta\ >nul 2>&1
exit /b %ERR%
