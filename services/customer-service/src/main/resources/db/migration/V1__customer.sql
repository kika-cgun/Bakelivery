CREATE TABLE customers (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL UNIQUE,
    bakery_id       UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    phone           VARCHAR(40),
    company_name    VARCHAR(200),
    vat_id          VARCHAR(50),
    billing_address VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_bakery ON customers(bakery_id);
CREATE INDEX idx_customers_user ON customers(user_id);
