@echo off
setlocal enabledelayedexpansion

if not exist out mkdir out

javac -d out -encoding UTF-8 src\Main.java
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

echo Starting server on http://localhost:8080
java -cp out Main


