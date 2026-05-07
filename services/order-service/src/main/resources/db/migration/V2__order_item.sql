CREATE TABLE order_items (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  bakery_id UUID NOT NULL,
  product_id UUID NOT NULL,
  product_name VARCHAR(160) NOT NULL,
  variant_id UUID,
  variant_name VARCHAR(160),
  unit_price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  line_total DECIMAL(10,2) NOT NULL
);
