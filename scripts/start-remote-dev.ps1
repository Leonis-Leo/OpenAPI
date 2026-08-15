param(
    [switch]$Build,
    [string]$ServerHost = $env:OPENAPI_DEV_SERVER_HOST
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ServerHost)) {
    Write-Host '[ERROR] Missing server host. Set OPENAPI_DEV_SERVER_HOST or pass -ServerHost.' -ForegroundColor Red
    exit 1
}

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
    Write-Host "  ssh -N -L 3306:127.0.0.1:3306 -L 6379:127.0.0.1:6379 -L 5672:127.0.0.1:5672 ubuntu@$ServerHost"
    exit 1
}

$startScript = Join-Path $PSScriptRoot 'start-all.ps1'
if ($Build) {
    & $startScript -SkipMiddleware -Build
}
else {
    & $startScript -SkipMiddleware
}
