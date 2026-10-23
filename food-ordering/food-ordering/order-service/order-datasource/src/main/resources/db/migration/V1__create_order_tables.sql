-- order-service schema (REQUIREMENTS §5.4, §6). UUIDs are BINARY(16) to keep InnoDB's clustered
-- index compact. MySQL DDL is not transactional, so every statement is safe to re-run.

CREATE TABLE IF NOT EXISTS orders
(
    id               BINARY(16)     NOT NULL,
    tracking_id      BINARY(16)     NOT NULL,
    customer_id      BINARY(16)     NOT NULL,
    restaurant_id    BINARY(16)     NOT NULL,
    price            DECIMAL(12, 2) NOT NULL,
    order_status     VARCHAR(16)    NOT NULL,
    street           VARCHAR(255)   NOT NULL,
    postal_code      VARCHAR(16)    NOT NULL,
    city             VARCHAR(255)   NOT NULL,
    notes            JSON           NULL,
    failure_messages JSON           NOT NULL,
    version          INT            NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_tracking_id UNIQUE (tracking_id)
);

CREATE TABLE IF NOT EXISTS order_items
(
    order_id     BINARY(16)     NOT NULL,
    id           BIGINT         NOT NULL,
    product_id   BINARY(16)     NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    price        DECIMAL(12, 2) NOT NULL,
    quantity     INT            NOT NULL,
    sub_total    DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (order_id, id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- Materialized views, written only by Kafka listeners (REQUIREMENTS §5.4).
CREATE TABLE IF NOT EXISTS customer_view
(
    id      BINARY(16) NOT NULL,
    version BIGINT     NOT NULL,
    CONSTRAINT pk_customer_view PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS restaurant_product_view
(
    restaurant_id     BINARY(16)     NOT NULL,
    product_id        BINARY(16)     NOT NULL,
    name              VARCHAR(255)   NOT NULL,
    price             DECIMAL(12, 2) NOT NULL,
    available         BOOLEAN        NOT NULL,
    restaurant_active BOOLEAN        NOT NULL,
    version           BIGINT         NOT NULL,
    CONSTRAINT pk_restaurant_product_view PRIMARY KEY (restaurant_id, product_id)
);

-- Outboxes, one per saga step (REQUIREMENTS §6.1, §6.2). The unique key makes a duplicate saga
-- step fail on insert; version is the optimistic lock. payload is TEXT, not JSON: MySQL's JSON
-- type stores numbers as doubles and drops their scale (12.50 becomes 12.5), and amounts must
-- reach the publisher exactly as written.
CREATE TABLE IF NOT EXISTS payment_outbox
(
    id            BINARY(16)  NOT NULL,
    saga_id       BINARY(16)  NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    processed_at  DATETIME(6) NULL,
    type          VARCHAR(64) NOT NULL,
    payload       TEXT        NOT NULL,
    order_status  VARCHAR(16) NOT NULL,
    saga_status   VARCHAR(16) NOT NULL,
    outbox_status VARCHAR(16) NOT NULL,
    version       INT         NOT NULL,
    CONSTRAINT pk_payment_outbox PRIMARY KEY (id),
    CONSTRAINT uk_payment_outbox_saga UNIQUE (type, saga_id, saga_status),
    INDEX idx_payment_outbox_status (type, outbox_status, created_at)
);

CREATE TABLE IF NOT EXISTS restaurant_approval_outbox
(
    id            BINARY(16)  NOT NULL,
    saga_id       BINARY(16)  NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    processed_at  DATETIME(6) NULL,
    type          VARCHAR(64) NOT NULL,
    payload       TEXT        NOT NULL,
    order_status  VARCHAR(16) NOT NULL,
    saga_status   VARCHAR(16) NOT NULL,
    outbox_status VARCHAR(16) NOT NULL,
    version       INT         NOT NULL,
    CONSTRAINT pk_restaurant_approval_outbox PRIMARY KEY (id),
    CONSTRAINT uk_restaurant_approval_outbox_saga UNIQUE (type, saga_id, saga_status),
    INDEX idx_restaurant_approval_outbox_status (type, outbox_status, created_at)
);
