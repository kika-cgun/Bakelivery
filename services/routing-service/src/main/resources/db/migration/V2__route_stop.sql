CREATE TABLE route_stops (
    id                UUID PRIMARY KEY,
    bakery_id         UUID NOT NULL,
    route_plan_id     UUID NOT NULL REFERENCES route_plans(id) ON DELETE CASCADE,
    dispatch_stop_id  UUID NOT NULL,
    sequence_number   INT NOT NULL,
    lat               DOUBLE PRECISION NOT NULL,
    lon               DOUBLE PRECISION NOT NULL,
    customer_name     VARCHAR(200) NOT NULL,
    delivery_address  VARCHAR(500) NOT NULL,
    affinity_score    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    eta_seconds       INT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_route_stop_plan ON route_stops(route_plan_id, sequence_number);
