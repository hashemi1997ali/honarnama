BEGIN;

CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT),
    last_update BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT)
);

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_update TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    image VARCHAR(255) NOT NULL,
    price NUMERIC(14, 2) NOT NULL CHECK (price >= 0),
    price_discount NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (price_discount >= 0),
    stock BIGINT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    draft SMALLINT NOT NULL DEFAULT 1 CHECK (draft IN (0, 1)),
    description TEXT NOT NULL DEFAULT '',
    status VARCHAR(32) NOT NULL DEFAULT 'READY STOCK',
    created_at BIGINT NOT NULL,
    last_update BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(255) NOT NULL,
    draft SMALLINT NOT NULL DEFAULT 1 CHECK (draft IN (0, 1)),
    brief VARCHAR(300) NOT NULL DEFAULT '',
    color VARCHAR(16) NOT NULL DEFAULT '#4db151',
    priority INTEGER NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    last_update BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS product_category (
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

CREATE TABLE IF NOT EXISTS product_image (
    product_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (product_id, name)
);

CREATE TABLE IF NOT EXISTS news_info (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    brief_content VARCHAR(500) NOT NULL DEFAULT '',
    full_content TEXT NOT NULL DEFAULT '',
    image VARCHAR(255) NOT NULL,
    draft SMALLINT NOT NULL DEFAULT 1 CHECK (draft IN (0, 1)),
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' CHECK (status IN ('NORMAL', 'FEATURED')),
    created_at BIGINT NOT NULL,
    last_update BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS product_order (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    buyer VARCHAR(120) NOT NULL,
    address VARCHAR(500) NOT NULL,
    email VARCHAR(254) NOT NULL,
    shipping VARCHAR(100) NOT NULL,
    date_ship BIGINT NOT NULL,
    phone VARCHAR(50) NOT NULL,
    comment TEXT NOT NULL DEFAULT '',
    status VARCHAR(32) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'PROCESSED', 'CANCEL')),
    total_fees NUMERIC(16, 2) NOT NULL DEFAULT 0 CHECK (total_fees >= 0),
    tax NUMERIC(7, 3) NOT NULL DEFAULT 0 CHECK (tax >= 0),
    created_at BIGINT NOT NULL,
    last_update BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS product_order_detail (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES product_order(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES product(id) ON DELETE SET NULL,
    product_name VARCHAR(160) NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    price_item NUMERIC(14, 2) NOT NULL CHECK (price_item >= 0),
    created_at BIGINT NOT NULL,
    last_update BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS product_auction (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    image VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    start_price NUMERIC(14, 2) NOT NULL CHECK (start_price >= 0),
    winner_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    winner_username VARCHAR(64),
    winner_price NUMERIC(14, 2) CHECK (winner_price >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_update TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date > start_date)
);

CREATE TABLE IF NOT EXISTS bid (
    id BIGSERIAL PRIMARY KEY,
    product_auction_id BIGINT NOT NULL REFERENCES product_auction(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    bid_price NUMERIC(14, 2) NOT NULL CHECK (bid_price >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_update TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_auction_id, user_id)
);

CREATE TABLE IF NOT EXISTS config (
    code VARCHAR(64) PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS currency (
    code VARCHAR(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS product_draft_id_idx ON product(draft, id DESC);
CREATE INDEX IF NOT EXISTS category_draft_priority_idx ON category(draft, priority);
CREATE INDEX IF NOT EXISTS news_info_draft_status_id_idx ON news_info(draft, status, id DESC);
CREATE INDEX IF NOT EXISTS product_order_status_id_idx ON product_order(status, id DESC);
CREATE INDEX IF NOT EXISTS product_order_detail_order_id_idx ON product_order_detail(order_id);
CREATE INDEX IF NOT EXISTS product_auction_end_date_idx ON product_auction(end_date DESC);

INSERT INTO config (code, value) VALUES
    ('CURRENCY', 'EUR'),
    ('TAX', '0'),
    ('FEATURED_NEWS', '5'),
    ('SHIPPING', '["Standard shipping","Express shipping"]'),
    ('EMAIL_NOTIF_ON_ORDER', 'FALSE'),
    ('EMAIL_NOTIF_ON_ORDER_PROCESS', 'FALSE'),
    ('EMAIL_REPLY_TO', ''),
    ('EMAIL_BCC_RECEIVER', '[]')
ON CONFLICT (code) DO NOTHING;

INSERT INTO currency (code, name) VALUES
    ('EUR', 'Euro')
ON CONFLICT (code) DO NOTHING;

UPDATE config SET value = 'EUR' WHERE code = 'CURRENCY';
DELETE FROM currency WHERE code <> 'EUR';

COMMIT;
