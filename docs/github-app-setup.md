# GitHub App setup

AgentForge uses one GitHub App for both user OAuth login and repository installation access. OAuth user tokens discover installations and repositories; repository operations use one-hour installation tokens scoped to the selected repository.

## GitHub App settings

- Homepage URL: `http://agentforge.localhost:8080`
- Callback URL: `http://agentforge.localhost:8080/login/oauth2/code/github`
- Setup URL: `http://agentforge.localhost:8080/projects`
- Request user authorization during installation: enabled
- Webhooks: disabled for the local M0 profile
- Repository permissions: Contents `Read and write`, Pull requests `Read and write`, Metadata `Read-only`

## Local configuration

Set these values outside source control:

```powershell
$env:AGENTFORGE_DEV_AUTH = "false"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID = "<client-id>"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET = "<client-secret>"
$env:GITHUB_APP_ID = "<app-id>"
$env:GITHUB_APP_SLUG = "<app-slug>"
$env:GITHUB_APP_PRIVATE_KEY = Get-Content -Raw C:\path\to\app.private-key.pem
```

For Helm, create an ignored `github.local.yaml` (or keep it outside the repository):

```yaml
config:
  devAuth: false
  githubAppId: "<app-id>"
  githubAppSlug: "<app-slug>"
secret:
  githubClientId: "<client-id>"
  githubClientSecret: "<client-secret>"
  githubPrivateKey: |-
    -----BEGIN RSA PRIVATE KEY-----
    <private-key-body>
    -----END RSA PRIVATE KEY-----
```

Deploy it with `./scripts/k3d-up.ps1 -ValuesFile ./github.local.yaml`. The script reuses that values file for the Helm upgrade and waits for all restarted workloads. For shared environments, use `secret.existingSecret` instead. Never place the private key in committed values.

The login entry point is `/oauth2/authorization/github`. After login, `/api/v1/github/installations` and `/api/v1/github/installations/{id}/repositories` expose only metadata. Installation access tokens are created server-side and are never returned by a public controller.
