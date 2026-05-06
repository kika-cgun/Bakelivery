CREATE TABLE products (
    id              UUID PRIMARY KEY,
    bakery_id       UUID NOT NULL,
    category_id     UUID,
    sku             VARCHAR(60),  -- intentionally not unique: SKUs are optional user-managed labels; add UNIQUE (bakery_id, sku) here if uniqueness becomes a product requirement
    slug            VARCHAR(160) NOT NULL,
    name            VARCHAR(160) NOT NULL,
    description     TEXT,
    base_price      NUMERIC(10,2) NOT NULL,
    available_days  SMALLINT NOT NULL DEFAULT 127,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_bakery_slug UNIQUE (bakery_id, slug),
    CONSTRAINT chk_available_days CHECK (available_days >= 0 AND available_days <= 127),
    CONSTRAINT chk_base_price_nonneg CHECK (base_price >= 0),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_products_bakery ON products(bakery_id);
CREATE INDEX idx_products_bakery_active ON products(bakery_id, active);
CREATE INDEX idx_products_category ON products(category_id);
