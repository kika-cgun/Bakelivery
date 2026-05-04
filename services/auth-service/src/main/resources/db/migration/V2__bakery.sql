CREATE TABLE bakeries (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/Warsaw',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE users
    ADD COLUMN bakery_id UUID REFERENCES bakeries(id);

CREATE INDEX idx_users_bakery ON users(bakery_id);
