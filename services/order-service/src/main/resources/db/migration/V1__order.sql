CREATE TABLE orders (
  id UUID PRIMARY KEY,
  bakery_id UUID NOT NULL,
  customer_id UUID NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PLACED',
  total_amount DECIMAL(10,2) NOT NULL,
  delivery_address_id UUID NOT NULL,
  delivery_address VARCHAR(500) NOT NULL,
  notes VARCHAR(500),
  idempotency_key VARCHAR(36) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_order_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_order_bakery ON orders(bakery_id);
CREATE INDEX idx_order_customer ON orders(customer_id, bakery_id);
