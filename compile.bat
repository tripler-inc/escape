@echo off
if not exist out mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\com\escape\Main.java
if errorlevel 1 (
    echo.
    echo Compilation FAILED.
    pause
) else (
    echo.
    echo Compilation successful. Run run.bat to start the game.
)
