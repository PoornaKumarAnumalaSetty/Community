Write-Host "Checking PostgreSQL Database Connection..." -ForegroundColor Green
Write-Host ""

Write-Host "Testing connection to: community_board" -ForegroundColor Yellow
try {
    $result = psql -U postgres -d community_board -c "\dt" 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Database connection successful!" -ForegroundColor Green
        Write-Host ""
        
        Write-Host "Checking table structures..." -ForegroundColor Yellow
        Write-Host "--- Issues Table ---" -ForegroundColor Cyan
        psql -U postgres -d community_board -c "\d issues" 2>$null
        
        Write-Host "--- Comments Table ---" -ForegroundColor Cyan
        psql -U postgres -d community_board -c "\d comments" 2>$null
        
        Write-Host ""
        Write-Host "Checking table counts..." -ForegroundColor Yellow
        psql -U postgres -d community_board -c "SELECT 'issues' as table_name, COUNT(*) as count FROM issues UNION ALL SELECT 'comments', COUNT(*) FROM comments;" 2>$null
        
    }
    else {
        throw "Connection failed"
    }
}
catch {
    Write-Host "❌ Database connection failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Possible issues:" -ForegroundColor Yellow
    Write-Host "1. PostgreSQL service not running" -ForegroundColor White
    Write-Host "2. Database 'community_board' doesn't exist" -ForegroundColor White
    Write-Host "3. Wrong password in database.properties" -ForegroundColor White
    Write-Host "4. PostgreSQL not in PATH" -ForegroundColor White
    Write-Host ""
    Write-Host "To create database manually:" -ForegroundColor Cyan
    Write-Host "psql -U postgres -c 'CREATE DATABASE community_board;'" -ForegroundColor White
}

Write-Host ""
Read-Host "Press Enter to continue"
