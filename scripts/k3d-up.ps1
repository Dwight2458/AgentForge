[CmdletBinding()]
param(
    [string]$ValuesFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$clusterName = "agentforge"
$resolvedValuesFile = $null
if ($ValuesFile) {
    $resolvedValuesFile = (Resolve-Path -LiteralPath $ValuesFile -ErrorAction Stop).Path
}

function Assert-NativeSuccess {
    param([Parameter(Mandatory)][string]$Operation)

    if ($LASTEXITCODE -ne 0) {
        throw "$Operation failed with exit code $LASTEXITCODE."
    }
}

function Repair-WindowsKubeconfigEndpoint {
    if ([System.Environment]::OSVersion.Platform -ne [System.PlatformID]::Win32NT) {
        return
    }

    $publishedPorts = @(docker port "k3d-$clusterName-serverlb" 6443/tcp)
    Assert-NativeSuccess "Reading the k3d API port"
    $portMatch = [regex]::Match(($publishedPorts | Select-Object -First 1), ':(?<port>\d+)$')
    if (-not $portMatch.Success) {
        throw "Could not determine the published Kubernetes API port from: $($publishedPorts -join ', ')"
    }

    $kubeconfigCluster = "k3d-$clusterName"
    $knownClusters = @(kubectl config get-clusters)
    Assert-NativeSuccess "Reading kubeconfig clusters"
    if ($kubeconfigCluster -notin $knownClusters) {
        throw "Kubeconfig does not contain the expected cluster '$kubeconfigCluster'."
    }

    kubectl config set-cluster $kubeconfigCluster --server="https://127.0.0.1:$($portMatch.Groups['port'].Value)" | Out-Null
    Assert-NativeSuccess "Updating the Windows kubeconfig API endpoint"
    kubectl config use-context $kubeconfigCluster | Out-Null
    Assert-NativeSuccess "Selecting the AgentForge kubeconfig context"
}

function Wait-KubernetesApi {
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        kubectl --request-timeout=5s get --raw=/readyz 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            return
        }
        Start-Sleep -Seconds 1
    }

    throw "The Kubernetes API did not become ready within 30 seconds."
}

function Wait-AgentForgeUrl {
    param([Parameter(Mandatory)][string]$Uri)

    $curlCommand = if ([System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT) { "curl.exe" } else { "curl" }
    if (-not (Get-Command $curlCommand -ErrorAction SilentlyContinue)) {
        throw "Required command '$curlCommand' is not installed."
    }

    $lastObservation = "No response received."
    for ($attempt = 1; $attempt -le 90; $attempt++) {
        $content = & $curlCommand --silent --show-error --fail --max-time 5 $Uri 2>&1
        $curlExitCode = $LASTEXITCODE
        if ($curlExitCode -eq 0 -and ($content -join "`n").Contains("<title>AgentForge</title>")) {
            return
        }
        $lastObservation = "curl exit code ${curlExitCode}: $($content -join ' ')"
        # Traefik and ServiceLB can take a few seconds after the pods become ready.
        Start-Sleep -Seconds 1
    }

    throw "AgentForge did not return a valid page from $Uri within 90 seconds. Last result: $lastObservation"
}

foreach ($tool in @("docker", "k3d", "helm")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Required command '$tool' is not installed."
    }
}

Push-Location $repoRoot
try {
    docker compose up -d
    Assert-NativeSuccess "Starting Docker Compose dependencies"

    $clusters = @(k3d cluster list --no-headers)
    Assert-NativeSuccess "Listing k3d clusters"
    if (-not ($clusters | Select-String -Pattern "^$clusterName\s")) {
        k3d cluster create --config infra/k3d/cluster.yaml
        Assert-NativeSuccess "Creating the k3d cluster"
    }

    Repair-WindowsKubeconfigEndpoint
    Wait-KubernetesApi

    $images = @(
        @{ Name = "agentforge/control-plane:dev"; File = "services/control-plane/Dockerfile" },
        @{ Name = "agentforge/agent-runtime:dev"; File = "services/agent-runtime/Dockerfile" },
        @{ Name = "agentforge/sandbox-executor:dev"; File = "services/sandbox-executor/Dockerfile" },
        @{ Name = "agentforge/web-console:dev"; File = "apps/web-console/Dockerfile" }
    )
    foreach ($image in $images) {
        docker build --file $image.File --tag $image.Name .
        Assert-NativeSuccess "Building image $($image.Name)"
        k3d image import --cluster $clusterName $image.Name
        Assert-NativeSuccess "Importing image $($image.Name)"
    }

    $helmArguments = @(
        "upgrade", "--install", "agentforge", "infra/helm/agentforge",
        "--namespace", "agentforge", "--create-namespace"
    )
    if ($resolvedValuesFile) {
        $helmArguments += @("--values", $resolvedValuesFile)
    }
    & helm @helmArguments
    Assert-NativeSuccess "Installing the AgentForge Helm release"

    # Local images deliberately reuse the :dev tag. Helm will not change the
    # pod template when only the imported image contents changed, so explicitly
    # roll every workload to make K3s resolve the freshly imported image.
    kubectl rollout restart deployment --namespace agentforge
    Assert-NativeSuccess "Restarting AgentForge deployments"
    $deployments = @(
        "agentforge-control-plane",
        "agentforge-agent-runtime",
        "agentforge-web-console"
    )
    foreach ($deployment in $deployments) {
        kubectl rollout status "deployment/$deployment" --namespace agentforge --timeout=5m
        Assert-NativeSuccess "Waiting for deployment $deployment"
    }

    kubectl get deployments,pods,ingress -n agentforge
    Assert-NativeSuccess "Reading deployed AgentForge resources"
    $agentForgeUrl = "http://agentforge.localhost:8080"
    Wait-AgentForgeUrl $agentForgeUrl
    Write-Host "AgentForge is available at $agentForgeUrl"
} finally {
    Pop-Location
}
