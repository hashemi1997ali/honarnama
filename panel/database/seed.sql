BEGIN;

WITH sample(name, icon, brief, color, priority) AS (
    VALUES
        ('Painting', '', 'Original paintings and works on canvas.', '#5C6BC0', 1),
        ('Sculpture', '', 'Decorative and collectible sculptures.', '#8D6E63', 2),
        ('Photography', '', 'Fine-art and documentary photography.', '#546E7A', 3),
        ('Handicrafts', '', 'Handmade objects created by local artists.', '#00897B', 4)
)
INSERT INTO category (name, icon, draft, brief, color, priority, created_at, last_update)
SELECT
    sample.name,
    sample.icon,
    0,
    sample.brief,
    sample.color,
    sample.priority,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT
FROM sample
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE category.name = sample.name
);

WITH sample(name, price, price_discount, stock, description, status) AS (
    VALUES
        ('Azure Horizon', 4800000::NUMERIC, 4200000::NUMERIC, 3::BIGINT, 'A calm abstract landscape in layered shades of blue.', 'READY STOCK'),
        ('Golden Geometry', 6200000::NUMERIC, 0::NUMERIC, 1::BIGINT, 'A geometric composition inspired by light and architecture.', 'READY STOCK'),
        ('Quiet Garden', 3900000::NUMERIC, 3500000::NUMERIC, 2::BIGINT, 'A botanical study with soft colors and fine detail.', 'READY STOCK'),
        ('Ceramic Balance', 2700000::NUMERIC, 0::NUMERIC, 5::BIGINT, 'A handmade ceramic form designed for modern interiors.', 'READY STOCK'),
        ('City in Rain', 5100000::NUMERIC, 0::NUMERIC, 2::BIGINT, 'A fine-art photograph capturing reflections after rain.', 'READY STOCK'),
        ('Woven Memory', 1850000::NUMERIC, 1600000::NUMERIC, 8::BIGINT, 'A decorative handwoven piece made with natural fibers.', 'READY STOCK')
)
INSERT INTO product (
    name, image, price, price_discount, stock, draft, description, status, created_at, last_update
)
SELECT
    sample.name,
    '',
    sample.price,
    sample.price_discount,
    sample.stock,
    0,
    sample.description,
    sample.status,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT
FROM sample
WHERE NOT EXISTS (
    SELECT 1 FROM product WHERE product.name = sample.name
);

WITH links(product_name, category_name) AS (
    VALUES
        ('Azure Horizon', 'Painting'),
        ('Golden Geometry', 'Painting'),
        ('Quiet Garden', 'Painting'),
        ('Ceramic Balance', 'Sculpture'),
        ('City in Rain', 'Photography'),
        ('Woven Memory', 'Handicrafts')
)
INSERT INTO product_category (product_id, category_id)
SELECT product.id, category.id
FROM links
JOIN product ON product.name = links.product_name
JOIN category ON category.name = links.category_name
ON CONFLICT (product_id, category_id) DO NOTHING;

WITH sample(title, brief_content, full_content, status) AS (
    VALUES
        (
            'Welcome to Honarnama',
            'Discover original artwork from emerging artists.',
            'Honarnama brings artwork, artist news, orders, and auctions together in one marketplace.',
            'FEATURED'
        ),
        (
            'How to Care for Original Artwork',
            'Simple steps can preserve color and texture for years.',
            'Keep artwork away from direct sunlight and moisture, and use suitable framing and cleaning methods.',
            'FEATURED'
        ),
        (
            'Understanding Art Auctions',
            'A short introduction to placing a thoughtful bid.',
            'Review the artwork details, decide on a budget, and place bids before the auction closes.',
            'NORMAL'
        )
)
INSERT INTO news_info (
    title, brief_content, full_content, image, draft, status, created_at, last_update
)
SELECT
    sample.title,
    sample.brief_content,
    sample.full_content,
    '',
    0,
    sample.status,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT
FROM sample
WHERE NOT EXISTS (
    SELECT 1 FROM news_info WHERE news_info.title = sample.title
);

WITH sample(name, description, start_offset, end_offset, start_price) AS (
    VALUES
        ('Midnight Blue Study', 'A sample auction for an abstract blue composition.', INTERVAL '1 day', INTERVAL '14 days', 3000000::NUMERIC),
        ('Small Bronze Form', 'A sample auction for a limited sculptural study.', INTERVAL '2 hours', INTERVAL '7 days', 4500000::NUMERIC)
)
INSERT INTO product_auction (
    name, image, description, start_date, end_date, start_price
)
SELECT
    sample.name,
    '',
    sample.description,
    now() - sample.start_offset,
    now() + sample.end_offset,
    sample.start_price
FROM sample
WHERE NOT EXISTS (
    SELECT 1 FROM product_auction WHERE product_auction.name = sample.name
);

INSERT INTO product_order (
    code, buyer, address, email, shipping, date_ship, phone, comment,
    status, total_fees, tax, created_at, last_update
)
SELECT
    'DEMO-1001',
    'Demo Customer',
    'Sample address — not a real order',
    'demo@example.com',
    'Standard shipping',
    (EXTRACT(EPOCH FROM (clock_timestamp() + INTERVAL '3 days')) * 1000)::BIGINT,
    '0000000000',
    'Sample order generated by panel/database/seed.php',
    'WAITING',
    4200000,
    0,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT
WHERE NOT EXISTS (
    SELECT 1 FROM product_order WHERE code = 'DEMO-1001'
);

INSERT INTO product_order_detail (
    order_id, product_id, product_name, amount, price_item, created_at, last_update
)
SELECT
    product_order.id,
    product.id,
    product.name,
    1,
    4200000,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT
FROM product_order
JOIN product ON product.name = 'Azure Horizon'
WHERE product_order.code = 'DEMO-1001'
  AND NOT EXISTS (
      SELECT 1
      FROM product_order_detail
      WHERE product_order_detail.order_id = product_order.id
        AND product_order_detail.product_name = product.name
  );

COMMIT;
