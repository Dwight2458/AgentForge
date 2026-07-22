FROM python:3.12.11-slim-bookworm
COPY --from=ghcr.io/astral-sh/uv:0.11.24 /uv /uvx /bin/
WORKDIR /workspace
COPY pyproject.toml uv.lock ./
COPY services/agent-runtime/pyproject.toml services/agent-runtime/pyproject.toml
COPY services/sandbox-executor/pyproject.toml services/sandbox-executor/pyproject.toml
COPY services/agent-runtime/src services/agent-runtime/src
COPY services/sandbox-executor/src services/sandbox-executor/src
RUN uv sync --project services/agent-runtime --locked --no-dev && \
    useradd --system --uid 10001 --create-home agentforge && \
    chown -R agentforge:agentforge /workspace
ENV PATH="/workspace/.venv/bin:$PATH"
USER 10001
EXPOSE 8081
ENTRYPOINT ["agentforge-runtime"]

