CREATE TABLE fixed_delivery_points (
  id UUID PRIMARY KEY,
  bakery_id UUID NOT NULL,
  name VARCHAR(200) NOT NULL,
  address VARCHAR(500) NOT NULL,
  lat DOUBLE PRECISION,
  lon DOUBLE PRECISION,
  delivery_days SMALLINT NOT NULL DEFAULT 127,
  default_notes VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_fdp_bakery ON fixed_delivery_points(bakery_id);
