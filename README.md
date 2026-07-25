# AgentForge

Cloud-Native Autonomous Software Engineering Platform.

AgentForge turns a GitHub repository and a development request into a clarified requirement, an isolated autonomous run, verified code changes, and a Pull Request or failure patch.

## Repository layout

- `apps/web-console` — Vue 3 operator console.
- `services/control-plane` — Spring Boot control plane and system of record.
- `services/agent-runtime` — LangGraph orchestration and model/tool policy.
- `services/sandbox-executor` — root-capable tool executor intended for an isolated AgentRun Job.
- `infra` — local Kubernetes and deployment assets.
- `docs` — implementation plan, architecture, and generated UI references.

## Local prerequisites

Java 21, Maven 3.9+, Node 24, npm 11, Python 3.12, uv, Docker Desktop, kubectl, k3d, and Helm.

The current implementation plan is in [`docs/implementation-plan.md`](docs/implementation-plan.md). GitHub App registration and local credentials are documented in [`docs/github-app-setup.md`](docs/github-app-setup.md).

## Current executable foundation

The repository now contains the first M0 vertical slice:

- a Spring Boot control plane with persisted task/run state, Grill messages, requirement specs, transactional outbox publishing, and replayable SSE events;
- a LangGraph runtime with explicit Analyze → Plan → Implement → Verify → Repair/Deliver transitions and PostgreSQL checkpoint support;
- an authenticated, idempotent sandbox executor with workspace-confined file, shell, Git, and test tools;
- GitHub OAuth login plus GitHub App installation/repository discovery, with repository-scoped installation tokens kept inside the control plane;
- a Vue console for Projects → Grill → Run, using the real control-plane project API and retaining local fixtures only in development mode;
- versioned Kafka event envelopes, a PostgreSQL Inbox projector, retry/DLT handling, and replayable SSE projections;
- an OpenAI-compatible model gateway plus deterministic Fake Model-backed Function Calling for execution planning;
- Docker images, local dependencies, a k3d profile, a Helm chart, and a hardened per-run Job template.

The companion target application is the independent local repository `../AgentForge-DemoMall`. It contains a Spring Boot + Vue + PostgreSQL/Flyway baseline whose first intentionally missing feature is payment-status filtering.

The current Week 3–4 event and model contracts are described in [`docs/week-3-4-foundation.md`](docs/week-3-4-foundation.md).

## Run locally

Start PostgreSQL/pgvector, Kafka, Redis, and MinIO:

```powershell
./scripts/dev.ps1
```

Then start the three development processes printed by the script. The web console is served by Vite at `http://127.0.0.1:5173`.

Run every available check:

```powershell
./scripts/check.ps1
```

With k3d and Helm installed, build/import all images and install the local cluster profile:

```powershell
./scripts/k3d-up.ps1
```

Secrets in `infra/helm/agentforge/values.yaml` are local-only defaults. Any shared environment must use `secret.existingSecret`, network egress proxying, and the production isolation profile described in the implementation plan.
