CREATE TABLE schedule_runs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id       UUID NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('RUNNING','SUCCESS','FAILED','CANCELLED')),
    started_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMPTZ,
    result_location   TEXT,
    error_message     TEXT,
    records_processed INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_schedule_runs_schedule_id ON schedule_runs(schedule_id);
CREATE INDEX idx_schedule_runs_status ON schedule_runs(status);
CREATE INDEX idx_schedule_runs_started_at ON schedule_runs(started_at);
