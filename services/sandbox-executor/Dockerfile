FROM python:3.12.11-slim-bookworm
COPY --from=ghcr.io/astral-sh/uv:0.11.24 /uv /uvx /bin/
RUN apt-get update && \
    apt-get install --yes --no-install-recommends git ca-certificates && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY pyproject.toml uv.lock ./
COPY services/agent-runtime/pyproject.toml services/agent-runtime/pyproject.toml
COPY services/sandbox-executor/pyproject.toml services/sandbox-executor/pyproject.toml
COPY services/agent-runtime/src services/agent-runtime/src
COPY services/sandbox-executor/src services/sandbox-executor/src
RUN uv sync --project services/sandbox-executor --locked --no-dev
ENV PATH="/workspace/.venv/bin:$PATH"
EXPOSE 8082
# Runs as root only inside the isolated AgentRun pod; no host, Docker, or Kubernetes socket is mounted.
ENTRYPOINT ["agentforge-executor"]

