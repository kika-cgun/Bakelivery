CREATE TABLE dispatch_stops (
  id UUID PRIMARY KEY,
  bakery_id UUID NOT NULL,
  date DATE NOT NULL,
  order_id UUID,
  fixed_point_id UUID REFERENCES fixed_delivery_points(id),
  customer_name VARCHAR(200) NOT NULL,
  delivery_address VARCHAR(500) NOT NULL,
  lat DOUBLE PRECISION,
  lon DOUBLE PRECISION,
  assigned_driver_id UUID,
  assigned_driver_name VARCHAR(200),
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  notes VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_stop_order UNIQUE (date, order_id)
);
CREATE INDEX idx_stop_bakery_date ON dispatch_stops(bakery_id, date);
