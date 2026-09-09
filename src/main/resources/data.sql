INSERT INTO users (nickname, email, created_at)
SELECT '피우리', 'piwoori@example.com', NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'piwoori@example.com'
);

INSERT INTO products (name, price, image_url)
SELECT '에어팟 프로', 150000, 'https://example.com/airpods.jpg'
    WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE name = '에어팟 프로'
      AND price = 150000
);

INSERT INTO wishlist_items (user_id, product_id, created_at)
SELECT u.id, p.id, NOW()
FROM users u
         JOIN products p
              ON p.name = '에어팟 프로'
                  AND p.price = 150000
WHERE u.email = 'piwoori@example.com'
  AND NOT EXISTS (
    SELECT 1
    FROM wishlist_items wi
    WHERE wi.user_id = u.id
      AND wi.product_id = p.id
);