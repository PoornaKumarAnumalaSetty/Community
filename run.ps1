$ErrorActionPreference = "Stop"

if (!(Test-Path out)) {
    New-Item -ItemType Directory -Path out | Out-Null
}

javac -d out -encoding UTF-8 src\Main.java

Write-Host "Starting server on http://localhost:8080"
java -cp out Main


