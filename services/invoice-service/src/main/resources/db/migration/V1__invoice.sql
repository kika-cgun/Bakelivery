CREATE TABLE invoices (
  id UUID PRIMARY KEY,
  bakery_id UUID NOT NULL,
  order_id UUID NOT NULL,
  customer_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  object_key VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_invoice_order UNIQUE (order_id)
);
