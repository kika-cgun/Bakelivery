CREATE TABLE threads (
  id UUID PRIMARY KEY,
  bakery_id UUID NOT NULL,
  order_id UUID NOT NULL,
  customer_id UUID NOT NULL,
  driver_id UUID,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_thread_order UNIQUE (bakery_id, order_id)
);

CREATE INDEX idx_thread_bakery ON threads(bakery_id);
CREATE INDEX idx_thread_customer ON threads(customer_id);
