@echo off
setlocal EnableDelayedExpansion

REM Executa les dues suites de fuzz — propietats de jqwik + harnesses de Jazzer.
REM Mirror de `make fuzz` a Linux.

call scripts\fuzz-property.bat
set "PROP_RC=%ERRORLEVEL%"

call scripts\fuzz-jazzer.bat
set "JAZZER_RC=%ERRORLEVEL%"

if not "%PROP_RC%"=="0" exit /b %PROP_RC%
if not "%JAZZER_RC%"=="0" exit /b %JAZZER_RC%
exit /b 0
