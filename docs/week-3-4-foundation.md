# Week 3–4 foundation

This slice establishes reliable event projection and deterministic model-driven planning.

## Event flow

1. The control-plane transaction stores `RUN_QUEUED` in `outbox_events`.
2. `OutboxPublisher` wraps the stored payload with Event Envelope v1 and publishes it to `agentforge.run.commands.v1`, keyed by `runId`.
3. Runtime-originated events use `agentforge.run.events.v1`.
4. `RunEventConsumer` inserts `(consumer, eventId)` into `inbox_events` with `ON CONFLICT DO NOTHING`.
5. Only the first delivery transitions the run and appends the replayable SSE event.
6. Transient failures are retried three times; invalid envelopes and exhausted records go to `<topic>.DLT`.

The contract files are:

- `contracts/events/event-envelope-v1.schema.json`
- `contracts/events/run-status-changed-v1.schema.json`

The database transaction commits before Kafka acknowledges the record. A crash between those operations causes a safe redelivery because the Inbox key suppresses the duplicate projection.

The local k3d profile reaches Docker Desktop-published PostgreSQL, Kafka, Redis, and MinIO ports through `host.docker.internal`. Kafka's `HOST` advertised listener uses the same hostname.

## Model gateway

`AGENTFORGE_MODEL_PROVIDER=fake` is the default local and test mode. It deterministically returns the `submit_execution_plan` function call and records predictable usage values.

To use an OpenAI-compatible Chat Completions endpoint:

```powershell
$env:AGENTFORGE_MODEL_PROVIDER = "openai-compatible"
$env:AGENTFORGE_MODEL_BASE_URL = "https://api.openai.com/v1"
$env:AGENTFORGE_MODEL_API_KEY = "<secret>"
$env:AGENTFORGE_MODEL = "<model-id>"
```

The adapter sends messages, function tools, explicit `tool_choice`, and parses `tool_calls`, `finish_reason`, and token usage. The API key is supplied only to the Runtime and is never included in graph state or run events.

The internal contract endpoint `POST /internal/v1/model/complete` exists for adapter verification. Normal execution uses the same gateway from the LangGraph Plan node.
