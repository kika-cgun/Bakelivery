CREATE TABLE driver_territories (
  id UUID PRIMARY KEY,
  bakery_id UUID NOT NULL,
  driver_id UUID NOT NULL,
  driver_name VARCHAR(200) NOT NULL,
  fixed_point_id UUID NOT NULL REFERENCES fixed_delivery_points(id) ON DELETE CASCADE,
  affinity_score INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_territory UNIQUE (bakery_id, driver_id, fixed_point_id)
);
CREATE INDEX idx_territory_bakery_driver ON driver_territories(bakery_id, driver_id);
