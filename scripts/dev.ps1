[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    docker compose up -d
    Write-Host "Dependencies are starting. Run these in separate terminals:"
    Write-Host "  mvn -pl services/control-plane spring-boot:run"
    Write-Host "  uv run --project services/agent-runtime agentforge-runtime"
    Write-Host "  npm --prefix apps/web-console run dev"
} finally {
    Pop-Location
}

