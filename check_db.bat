@echo off
echo Checking PostgreSQL Database Connection...
echo.

echo Testing connection to: community_board
psql -U postgres -d community_board -c "\dt" 2>nul
if %errorlevel% equ 0 (
    echo.
    echo ✅ Database connection successful!
    echo.
    echo Checking table structures...
    psql -U postgres -d community_board -c "\d issues" 2>nul
    echo.
    psql -U postgres -d community_board -c "\d comments" 2>nul
    echo.
    echo Checking table counts...
    psql -U postgres -d community_board -c "SELECT 'issues' as table_name, COUNT(*) as count FROM issues UNION ALL SELECT 'comments', COUNT(*) FROM comments;" 2>nul
) else (
    echo ❌ Database connection failed!
    echo.
    echo Possible issues:
    echo 1. PostgreSQL service not running
    echo 2. Database 'community_board' doesn't exist
    echo 3. Wrong password in database.properties
    echo 4. PostgreSQL not in PATH
    echo.
    echo To create database manually:
    echo psql -U postgres -c "CREATE DATABASE community_board;"
)

echo.
pause
