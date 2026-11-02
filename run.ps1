# PhuketLow Bot - start script
# Usage: .\run.ps1

# 1) If token is already in env - use it
# 2) Otherwise read from .env file (project root, in .gitignore)
if (-not $env:TELEGRAM_BOT_TOKEN) {
    $envFile = Join-Path $PSScriptRoot ".env"
    if (Test-Path $envFile) {
        Get-Content $envFile | ForEach-Object {
            if ($_ -match '^\s*TELEGRAM_BOT_TOKEN\s*=\s*(.+)$') {
                $env:TELEGRAM_BOT_TOKEN = $Matches[1].Trim().Trim('"').Trim("'")
            }
        }
    }
}

if (-not $env:TELEGRAM_BOT_TOKEN) {
    Write-Error "Token not found. Set TELEGRAM_BOT_TOKEN env var or create .env file:"
    Write-Error "  TELEGRAM_BOT_TOKEN=your_token_from_BotFather"
    exit 1
}

# Kill old bot processes
taskkill /F /IM java.exe 2>$null | Out-Null
Start-Sleep -Seconds 2

# Start
Write-Host "Starting PhuketLow Bot..." -ForegroundColor Cyan
.\gradlew.bat run
