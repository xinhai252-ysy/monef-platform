$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java 17 or newer is required. Set JAVA_HOME and add its bin directory to PATH.' }
$jar = Join-Path $projectDir 'release\monef-api-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven 3.9+ is required to build the backend.' }
    Push-Location (Join-Path $projectDir 'backend')
    try { & mvn -q package; if ($LASTEXITCODE -ne 0) { throw 'Build failed.' } } finally { Pop-Location }
    $jar = Join-Path $projectDir 'backend\target\monef-api-1.0.0.jar'
}
Write-Output 'Monef local demo: http://127.0.0.1:8080'
Write-Output 'Demo login: demo@monef.local / MonefDemo!2026'
Write-Output 'This profile uses a local H2 database and fixed demo exchange rates. Ctrl+C stops the server.'
Push-Location (Join-Path $projectDir 'backend')
try { & java -jar $jar '--spring.profiles.active=demo' } finally { Pop-Location }
