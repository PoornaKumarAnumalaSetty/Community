@echo off
setlocal enabledelayedexpansion

if not exist out mkdir out
if not exist lib mkdir lib

if not exist lib\postgresql-42.7.2.jar (
  echo Downloading PostgreSQL JDBC driver...
  powershell -Command "Invoke-WebRequest -Uri 'https://jdbc.postgresql.org/download/postgresql-42.7.2.jar' -OutFile 'lib\postgresql-42.7.2.jar'"
)

javac -cp "lib/*" -d out -encoding UTF-8 src\*.java
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

echo Starting server on http://localhost:8080
echo Make sure PostgreSQL is running and database is configured in database.properties
java -cp "out;lib/*" Main
