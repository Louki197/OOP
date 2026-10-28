@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "JAVA_EXE=java"
set "JAVAC_EXE=javac"
for /d %%J in ("%ProgramFiles%\Java\jdk*") do if exist "%%J\bin\javac.exe" (
    set "JAVA_EXE=%%J\bin\java.exe"
    set "JAVAC_EXE=%%J\bin\javac.exe"
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
)

echo ========================================================
echo    CINEMA MANAGEMENT SYSTEM - BTL OOP PTIT
echo ========================================================

if not exist bin mkdir bin

dir /s /b src\main\java\*.java > sources.txt

echo [1/2] Compiling Java source files with FlatLaf and Gson...
"%JAVAC_EXE%" --release 17 -cp "lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" -encoding UTF-8 -d bin @sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    del sources.txt 2>nul
    pause
    exit /b 1
)

del sources.txt 2>nul

echo [2/2] Launching Cinema Management GUI...
echo --------------------------------------------------------
"%JAVA_EXE%" -cp "bin;lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" vn.edu.ptit.cinema.App
pause
