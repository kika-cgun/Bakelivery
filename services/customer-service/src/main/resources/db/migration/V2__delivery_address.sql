CREATE TABLE delivery_addresses (
    id            UUID PRIMARY KEY,
    customer_id   UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    bakery_id     UUID NOT NULL,
    label         VARCHAR(100),
    street        VARCHAR(200) NOT NULL,
    postal_code   VARCHAR(20)  NOT NULL,
    city          VARCHAR(100) NOT NULL,
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_addresses_customer ON delivery_addresses(customer_id);
CREATE INDEX idx_delivery_addresses_bakery ON delivery_addresses(bakery_id);
