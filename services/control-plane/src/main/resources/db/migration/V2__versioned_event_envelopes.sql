ALTER TABLE outbox_events
    ADD COLUMN envelope_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE outbox_events
    ADD COLUMN correlation_id VARCHAR(128);

UPDATE outbox_events
SET correlation_id = aggregate_id::text
WHERE correlation_id IS NULL;

ALTER TABLE outbox_events
    ALTER COLUMN correlation_id SET NOT NULL;

CREATE INDEX idx_inbox_processed_at ON inbox_events(processed_at);
