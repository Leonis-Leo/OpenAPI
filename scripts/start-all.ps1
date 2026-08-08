param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$JavaExe = 'C:\Program Files\Java\jdk-17\bin\java.exe'
if (-not (Test-Path -LiteralPath $JavaExe)) {
    $JavaExe = 'E:\Java\bin\java.exe'
}
$MavenCmd = 'E:\Maven\bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $MavenCmd)) {
    $MavenCmd = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
}
$NpmCmd = 'E:\Node.js\npm.cmd'
$MySqlExe = 'E:\MySQL8\bin\mysqld.exe'
$RedisExe = 'E:\redis\redis-server.exe'
$RedisConfig = 'E:\redis\redis.windows.conf'
$RabbitSbin = 'E:\RabbitMQ\rabbitmq_server-4.3.4\sbin'
$RabbitServer = Join-Path $RabbitSbin 'rabbitmq-server.bat'
$BackendJar = Join-Path $ProjectRoot 'openapi-backend\target\openapi-backend-0.0.1-SNAPSHOT.jar'
$GatewayJar = Join-Path $ProjectRoot 'openapi-gateway\target\openapi-gateway-0.0.1-SNAPSHOT.jar'
$LogRoot = Join-Path $ProjectRoot 'runtime-logs'

New-Item -ItemType Directory -Path $LogRoot -Force | Out-Null

function Test-Port([int]$Port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Wait-Port([int]$Port, [string]$Name, [int]$TimeoutSeconds = 30) {
    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        if (Test-Port $Port) {
            Write-Host "[OK] $Name listening on $Port" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "$Name did not start on port $Port. Check $LogRoot."
}

function Start-ManagedProcess(
    [string]$Name,
    [string]$FilePath,
    [string[]]$ArgumentList,
    [string]$WorkingDirectory,
    [int]$Port
) {
    if (Test-Port $Port) {
        Write-Host "[SKIP] $Name is already running on $Port" -ForegroundColor Yellow
        return
    }
    if (-not (Test-Path -LiteralPath $FilePath)) {
        throw "$Name executable not found: $FilePath"
    }

    $stdout = Join-Path $LogRoot "$Name.log"
    $stderr = Join-Path $LogRoot "$Name.err.log"
    Start-Process -WindowStyle Hidden -FilePath $FilePath -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $stdout -RedirectStandardError $stderr | Out-Null
    Wait-Port $Port $Name
}

function Stop-ProjectProcess([int]$Port, [string]$JarName) {
    $connection = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if (-not $connection) {
        return
    }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)"
    if ($process.CommandLine -like "*$JarName*") {
        Write-Host "[BUILD] stopping $JarName on $Port" -ForegroundColor Yellow
        Stop-Process -Id $connection.OwningProcess -Force
        Start-Sleep -Seconds 2
    }
}

Write-Host '=== OpenAPI Platform: start all ===' -ForegroundColor Cyan

if ($Build) {
    Write-Host '[BUILD] mvn package -DskipTests' -ForegroundColor Cyan
    Stop-ProjectProcess 8101 'openapi-backend-0.0.1-SNAPSHOT.jar'
    Stop-ProjectProcess 8080 'openapi-gateway-0.0.1-SNAPSHOT.jar'
    Push-Location $ProjectRoot
    try {
        if (-not $MavenCmd) {
            throw 'Maven executable not found. Expected E:\Maven\bin\mvn.cmd.'
        }
        & $MavenCmd package -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            throw 'Maven build failed.'
        }
    }
    finally {
        Pop-Location
    }
}

# MySQL: prefer the registered service; fall back to the E: drive installation
# when the current shell cannot control Windows services without elevation.
if (-not (Test-Port 3306)) {
    try {
        Start-Service -Name MySQL -ErrorAction Stop
    }
    catch {
        Write-Host '[INFO] MySQL service could not be started; using E:\MySQL8\my.ini directly.' -ForegroundColor Yellow
    }
    Start-Sleep -Seconds 3
}
if (-not (Test-Port 3306)) {
    Start-ManagedProcess 'mysql' $MySqlExe @('--defaults-file=E:\MySQL8\my.ini', '--console') 'E:\MySQL8' 3306
}
else {
    Write-Host '[SKIP] MySQL is already running on 3306' -ForegroundColor Yellow
}

Start-ManagedProcess 'redis' $RedisExe @($RedisConfig) 'E:\redis' 6379

if (-not (Test-Port 5672)) {
    if (-not (Test-Path -LiteralPath $RabbitServer)) {
        throw "RabbitMQ launcher not found: $RabbitServer"
    }
    $env:ERLANG_HOME = 'E:\Erlang27'
    Start-Process -WindowStyle Hidden -FilePath $RabbitServer -ArgumentList '-detached' `
        -WorkingDirectory $RabbitSbin -RedirectStandardOutput (Join-Path $LogRoot 'rabbitmq.log') `
        -RedirectStandardError (Join-Path $LogRoot 'rabbitmq.err.log') | Out-Null
    Wait-Port 5672 'rabbitmq'
}
else {
    Write-Host '[SKIP] RabbitMQ is already running on 5672' -ForegroundColor Yellow
}

Start-ManagedProcess 'backend' $JavaExe @('-jar', $BackendJar) $ProjectRoot 8101
Start-ManagedProcess 'gateway' $JavaExe @('-jar', $GatewayJar) $ProjectRoot 8080
Start-ManagedProcess 'frontend' $NpmCmd @('run', 'dev', '--', '--host', '127.0.0.1') `
    (Join-Path $ProjectRoot 'openapi-web') 5173

Write-Host ''
Write-Host '=== OpenAPI Platform is ready ===' -ForegroundColor Green
Write-Host 'Frontend : http://127.0.0.1:5173'
Write-Host 'Backend  : http://127.0.0.1:8101'
Write-Host 'Swagger  : http://127.0.0.1:8101/swagger-ui.html'
Write-Host 'Gateway  : http://127.0.0.1:8080'
Write-Host "Logs     : $LogRoot"
