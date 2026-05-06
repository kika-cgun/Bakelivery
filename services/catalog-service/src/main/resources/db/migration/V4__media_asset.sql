CREATE TABLE media_assets (
    id            UUID PRIMARY KEY,
    bakery_id     UUID NOT NULL,
    product_id    UUID NOT NULL,
    object_key    VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    original_name VARCHAR(255),
    sort_order    INTEGER NOT NULL DEFAULT 0,
    is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_media_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_media_object_key UNIQUE (object_key)
);

CREATE INDEX idx_media_product ON media_assets(product_id);
CREATE INDEX idx_media_bakery ON media_assets(bakery_id);
CREATE UNIQUE INDEX uq_media_one_primary ON media_assets(product_id) WHERE is_primary = TRUE;
