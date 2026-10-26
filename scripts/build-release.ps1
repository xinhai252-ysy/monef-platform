$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $projectDir 'frontend')
try {
    $env:VITE_PREVIEW_MODE = 'false'
    & pnpm install --frozen-lockfile
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    & pnpm test
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
    & pnpm build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally { Pop-Location; Remove-Item Env:VITE_PREVIEW_MODE -ErrorAction SilentlyContinue }
$staticDir = Join-Path $projectDir 'backend\src\main\resources\static'
New-Item -ItemType Directory -Force -Path $staticDir | Out-Null
Copy-Item -Path (Join-Path $projectDir 'frontend\dist\*') -Destination $staticDir -Recurse -Force
Push-Location (Join-Path $projectDir 'backend')
try { & mvn verify; if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' } } finally { Pop-Location }
$releaseDir = Join-Path $projectDir 'release'
New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
Copy-Item -LiteralPath (Join-Path $projectDir 'backend\target\monef-api-1.0.0.jar') -Destination $releaseDir -Force
Write-Output 'Build complete. Run scripts/start-demo.ps1 or deploy with Docker Compose.'
