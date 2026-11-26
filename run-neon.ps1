param([int]$Port = 8080)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$neonEnvPath = Join-Path $projectRoot '.env.local'
$backendEnvPath = Join-Path $PSScriptRoot '.env'

if (-not (Test-Path $neonEnvPath)) {
    throw "Neon environment file not found: $neonEnvPath. Run 'neon env pull' from the project root."
}
if (-not (Test-Path $backendEnvPath)) {
    throw "Backend environment file not found: $backendEnvPath. Copy .env.example to .env and set JWT_SECRET."
}

function Read-EnvFile([string]$path) {
    $values = @{}
    foreach ($line in Get-Content $path) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $values[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
        }
    }
    return $values
}

$neonEnv = Read-EnvFile $neonEnvPath
$backendEnv = Read-EnvFile $backendEnvPath
$connectionString = $neonEnv['DATABASE_URL_UNPOOLED']
if ([string]::IsNullOrWhiteSpace($connectionString)) {
    throw 'DATABASE_URL_UNPOOLED is missing from .env.local. Run neon env pull for the linked branch.'
}
if ([string]::IsNullOrWhiteSpace($backendEnv['JWT_SECRET']) -or
    $backendEnv['JWT_SECRET'] -eq 'REPLACE_WITH_A_RANDOM_BASE64_KEY') {
    throw 'Set a real JWT_SECRET in backend/.env before starting the API.'
}

try {
    $databaseUri = [Uri]$connectionString
    if ($databaseUri.Scheme -notin @('postgres', 'postgresql')) {
        throw 'Expected a postgres:// or postgresql:// connection URL.'
    }
    if ($databaseUri.Host -match '-pooler\.') {
        throw 'DATABASE_URL_UNPOOLED unexpectedly points at a pooled endpoint.'
    }
    $credentials = $databaseUri.UserInfo -split ':', 2
    if ($credentials.Count -ne 2) {
        throw 'The Neon connection URL does not contain a username and password.'
    }
    $databaseName = [Uri]::UnescapeDataString($databaseUri.AbsolutePath.TrimStart('/'))
    if ([string]::IsNullOrWhiteSpace($databaseName)) {
        throw 'The Neon connection URL does not contain a database name.'
    }
} catch {
    throw "Could not parse the Neon direct connection URL: $($_.Exception.Message)"
}

$env:PGHOST = $databaseUri.Host
$env:PGPORT = if ($databaseUri.IsDefaultPort) { '5432' } else { "$($databaseUri.Port)" }
$env:PGDATABASE = $databaseName
$env:PGUSER = [Uri]::UnescapeDataString($credentials[0])
$env:PGPASSWORD = [Uri]::UnescapeDataString($credentials[1])
$env:PGSSLMODE = 'require'
$env:JWT_SECRET = $backendEnv['JWT_SECRET']

foreach ($key in @('APP_ADMIN_EMAIL', 'APP_ADMIN_PASSWORD', 'APP_ADMIN_NAME')) {
    if ($backendEnv.ContainsKey($key)) {
        [Environment]::SetEnvironmentVariable($key, $backendEnv[$key], 'Process')
    }
}

Push-Location $projectRoot
try {
    docker build -t spendwise-api-neon -f backend\Dockerfile backend
    if ($LASTEXITCODE -ne 0) {
        throw 'Backend Docker build failed.'
    }
    docker run --rm --name spendwise-api-neon -p "${Port}:8080" `
        -e PGHOST -e PGPORT -e PGDATABASE -e PGUSER -e PGPASSWORD -e PGSSLMODE `
        -e JWT_SECRET -e APP_ADMIN_EMAIL -e APP_ADMIN_PASSWORD -e APP_ADMIN_NAME `
        spendwise-api-neon
} finally {
    Pop-Location
}
