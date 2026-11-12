@echo off
setlocal
cd /d "%~dp0"
call compile.bat --no-pause
if errorlevel 1 (
    pause
    exit /b 1
)
set "JAVA_EXE=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if "%JAVA_EXE%"=="java" for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do if exist "%%~J\bin\java.exe" set "JAVA_EXE=%%~J\bin\java.exe"
"%JAVA_EXE%" -cp bin engine.Main
if errorlevel 1 (
    echo Game could not start.
    pause
)
