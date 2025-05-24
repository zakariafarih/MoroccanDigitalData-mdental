-- New file: db/migration/V3__create_outbox_table.sql
CREATE TABLE IF NOT EXISTS outbox (
                                      id UUID PRIMARY KEY,
                                      aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox(created_at);