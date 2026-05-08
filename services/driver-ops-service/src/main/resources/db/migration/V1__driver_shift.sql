CREATE TABLE driver_shifts (
    id UUID PRIMARY KEY,
    bakery_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    date DATE NOT NULL,
    route_plan_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_stop_index INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_shift UNIQUE (bakery_id, driver_id, date)
);

CREATE INDEX idx_shift_driver ON driver_shifts(driver_id, date);
