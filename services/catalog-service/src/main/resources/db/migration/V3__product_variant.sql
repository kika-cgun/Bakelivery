CREATE TABLE product_variants (
    id           UUID PRIMARY KEY,
    product_id   UUID NOT NULL,
    bakery_id    UUID NOT NULL,
    name         VARCHAR(80) NOT NULL,
    sku          VARCHAR(60),  -- intentionally not unique: add UNIQUE (bakery_id, sku) if SKU-as-identifier is required
    price_delta  NUMERIC(10,2) NOT NULL DEFAULT 0,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_variant_product_name UNIQUE (product_id, name)
);

CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_variants_bakery ON product_variants(bakery_id);
