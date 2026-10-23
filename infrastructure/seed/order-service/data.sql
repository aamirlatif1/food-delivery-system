-- Synthetic restaurant data for order-service's local database (REQUIREMENTS §9: infrastructure/seed).
-- In the running system restaurant-service publishes this on restaurant-topic and order-service
-- builds restaurant_product_view from it (§5.4); until then this script stands in for those events.
-- Loaded by the order-service-seed container in docker-compose.yml after Flyway has created the
-- tables. Safe to re-run: every row is an upsert, so a re-run resets the demo data.
--
-- Ids match order-service/http/orders.http.

-- Restaurants
SET @pizzeria  = UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000004'); -- active
SET @sushi_bar = UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000010'); -- active
SET @burger    = UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000020'); -- inactive: orders are rejected with 422

INSERT INTO restaurant_product_view (restaurant_id, product_id, name, price, available, restaurant_active, version)
VALUES
    -- Pizzeria Napoli
    (@pizzeria, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000005'), 'Pizza Margherita', 25.00, TRUE, TRUE, 1),
    (@pizzeria, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000006'), 'Pizza Diavola', 27.50, TRUE, TRUE, 1),
    (@pizzeria, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000007'), 'Tiramisu', 7.50, TRUE, TRUE, 1),
    -- Sold out: an order for it is rejected with 422.
    (@pizzeria, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000008'), 'Calzone', 18.00, FALSE, TRUE, 1),
    -- Sushi Bar Tokyo
    (@sushi_bar, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000011'), 'Salmon Nigiri (8 pcs)', 16.90, TRUE, TRUE, 1),
    (@sushi_bar, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000012'), 'Vegetable Maki (12 pcs)', 12.40, TRUE, TRUE, 1),
    (@sushi_bar, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000013'), 'Miso Soup', 4.50, TRUE, TRUE, 1),
    -- Burger Corner (closed)
    (@burger, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000021'), 'Cheeseburger', 11.90, TRUE, FALSE, 1),
    (@burger, UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000022'), 'Fries', 4.20, TRUE, FALSE, 1)
AS seed
ON DUPLICATE KEY UPDATE
    name              = seed.name,
    price             = seed.price,
    available         = seed.available,
    restaurant_active = seed.restaurant_active,
    version           = seed.version;

-- Customers: an order needs its customer in customer_view as well (from customer-topic later).
INSERT INTO customer_view (id, version)
VALUES
    (UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000001'), 1),
    (UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000002'), 1),
    (UUID_TO_BIN('0192f0c1-0000-7000-8000-000000000003'), 1)
AS seed
ON DUPLICATE KEY UPDATE version = seed.version;
