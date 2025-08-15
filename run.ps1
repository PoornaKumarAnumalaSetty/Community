$ErrorActionPreference = "Stop"

if (!(Test-Path out)) {
    New-Item -ItemType Directory -Path out | Out-Null
}

if (!(Test-Path lib)) {
    Write-Host "Downloading PostgreSQL JDBC driver..."
    Invoke-WebRequest -Uri "https://jdbc.postgresql.org/download/postgresql-42.7.2.jar" -OutFile "lib/postgresql-42.7.2.jar"
}

javac -cp "lib/*" -d out -encoding UTF-8 src\*.java

Write-Host "Starting server on http://localhost:8080"
Write-Host "Make sure PostgreSQL is running and database is configured in database.properties"
java -cp "out;lib/*" Main
