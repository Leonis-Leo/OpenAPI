param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'

$env:OPENAPI_DB_HOST = '127.0.0.1'
$env:OPENAPI_REDIS_HOST = '127.0.0.1'
$env:OPENAPI_RABBITMQ_HOST = '127.0.0.1'

function Test-Port([int]$Port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

$missing = @()
foreach ($port in 3306, 6379, 5672) {
    if (-not (Test-Port $port)) {
        $missing += $port
    }
}

if ($missing.Count -gt 0) {
    Write-Host '[ERROR] SSH tunnel ports are not listening: ' -ForegroundColor Red -NoNewline
    Write-Host ($missing -join ', ') -ForegroundColor Red
    Write-Host ''
    Write-Host 'Please start the SSH tunnel in another terminal first:'
    Write-Host '  ssh -N -L 3306:127.0.0.1:3306 -L 6379:127.0.0.1:6379 -L 5672:127.0.0.1:5672 ubuntu@129.204.33.174'
    exit 1
}

$startScript = Join-Path $PSScriptRoot 'start-all.ps1'
if ($Build) {
    & $startScript -SkipMiddleware -Build
}
else {
    & $startScript -SkipMiddleware
}
