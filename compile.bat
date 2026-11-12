@echo off
setlocal
cd /d "%~dp0"
set "JAVAC_EXE=javac"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
if "%JAVAC_EXE%"=="javac" for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do if exist "%%~J\bin\javac.exe" set "JAVAC_EXE=%%~J\bin\javac.exe"
if not exist "bin" mkdir "bin"
"%JAVAC_EXE%" -Xlint:all -d bin src\engine\*.java src\managers\*.java src\states\*.java src\entities\*.java src\farming\*.java src\time\*.java src\inventory\*.java src\inputs\*.java src\validation\*.java
set "RESULT=%ERRORLEVEL%"
if "%RESULT%"=="0" (
    echo Compilation successful.
) else (
    echo Compilation failed. Install a JDK 17 or newer and check the errors above.
)
if not "%~1"=="--no-pause" pause
exit /b %RESULT%
