CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    github_installation_id BIGINT NOT NULL,
    repository_full_name VARCHAR(255) NOT NULL UNIQUE,
    clone_url VARCHAR(1000) NOT NULL,
    default_branch VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    request_text TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tasks_project_created ON tasks(project_id, created_at DESC);

CREATE TABLE grill_messages (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_grill_messages_task_created ON grill_messages(task_id, created_at);

CREATE TABLE requirement_specs (
    task_id UUID PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
    goal TEXT NOT NULL,
    acceptance_criteria TEXT NOT NULL,
    constraints TEXT NOT NULL,
    test_plan TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    status VARCHAR(32) NOT NULL,
    max_minutes INTEGER NOT NULL,
    max_repair_rounds INTEGER NOT NULL,
    max_tool_calls INTEGER NOT NULL,
    max_tokens INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agent_runs_created ON agent_runs(created_at DESC);

CREATE TABLE run_events (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
    sequence_no BIGINT NOT NULL,
    type VARCHAR(128) NOT NULL,
    agent VARCHAR(128),
    summary TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(run_id, sequence_no)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(128) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(occurred_at) WHERE published_at IS NULL;

CREATE TABLE inbox_events (
    consumer VARCHAR(128) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(consumer, event_id)
);

