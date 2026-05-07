CREATE TABLE route_plans (
    id                    UUID PRIMARY KEY,
    bakery_id             UUID NOT NULL,
    driver_id             UUID NOT NULL,
    date                  DATE NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_distance_meters DOUBLE PRECISION,
    total_duration_seconds DOUBLE PRECISION,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_plan UNIQUE (bakery_id, driver_id, date)
);
CREATE INDEX idx_plan_bakery_date ON route_plans(bakery_id, date);
CREATE INDEX idx_plan_driver_date ON route_plans(driver_id, date);
