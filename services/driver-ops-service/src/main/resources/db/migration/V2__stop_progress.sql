CREATE TABLE stop_progress (
    id UUID PRIMARY KEY,
    bakery_id UUID NOT NULL,
    shift_id UUID NOT NULL REFERENCES driver_shifts(id),
    dispatch_stop_id UUID NOT NULL,
    route_stop_id UUID NOT NULL,
    sequence_number INT NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMPTZ,
    skipped_reason VARCHAR(500),
    proof_object_key VARCHAR(500),
    eta_seconds INT
);

CREATE INDEX idx_progress_shift ON stop_progress(shift_id, sequence_number);
