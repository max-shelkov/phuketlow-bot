# PhuketLow Bot - start script
# Usage: .\run.ps1

# 1) Read all variables from .env (project root, in .gitignore)
#    if they are not already set in the environment
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.+)$') {
            $name = $Matches[1]
            $value = $Matches[2].Trim().Trim('"').Trim("'")
            if (-not (Test-Path "env:$name")) {
                Set-Item "env:$name" $value
            }
        }
    }
}

if (-not $env:TELEGRAM_BOT_TOKEN) {
    Write-Error "Token not found. Set TELEGRAM_BOT_TOKEN env var or create .env file:"
    Write-Error "  TELEGRAM_BOT_TOKEN=your_token_from_BotFather"
    exit 1
}

# Start
Write-Host "Starting PhuketLow Bot..." -ForegroundColor Cyan
.\gradlew.bat run
