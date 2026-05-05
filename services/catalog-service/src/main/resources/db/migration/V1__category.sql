CREATE TABLE categories (
    id           UUID PRIMARY KEY,
    bakery_id    UUID NOT NULL,
    name         VARCHAR(120) NOT NULL,
    slug         VARCHAR(120) NOT NULL,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_category_bakery_slug UNIQUE (bakery_id, slug)
);

CREATE INDEX idx_categories_bakery ON categories(bakery_id);
