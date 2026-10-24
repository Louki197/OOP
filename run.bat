@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo    CINEMA MANAGEMENT SYSTEM - BTL OOP PTIT
echo ========================================================

if not exist bin mkdir bin

dir /s /b src\main\java\*.java > sources.txt

echo [1/2] Compiling Java source files with FlatLaf and Gson...
javac -cp "lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" -encoding UTF-8 -d bin @sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    del sources.txt 2>nul
    pause
    exit /b 1
)

del sources.txt 2>nul

echo [2/2] Launching Cinema Management GUI...
echo --------------------------------------------------------
java -cp "bin;lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" vn.edu.ptit.cinema.App
pause
