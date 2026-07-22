[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    mvn -q test
    uv run --project services/agent-runtime pytest services/agent-runtime/tests -q
    uv run --project services/sandbox-executor pytest services/sandbox-executor/tests -q
    npm --prefix apps/web-console run type-check
    npm --prefix apps/web-console run test:unit
    npm --prefix apps/web-console run lint
    npm --prefix apps/web-console run build-only
    docker compose config --quiet

    if (Get-Command helm -ErrorAction SilentlyContinue) {
        helm lint infra/helm/agentforge
        helm template agentforge infra/helm/agentforge | Out-Null
    } else {
        Write-Warning "Helm is not installed; skipped chart rendering checks."
    }
} finally {
    Pop-Location
}
