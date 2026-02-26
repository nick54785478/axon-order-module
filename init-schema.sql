CREATE TABLE order_view (
    order_id VARCHAR(64) NOT NULL PRIMARY KEY,
    amount DECIMAL(19,4) NOT NULL,   -- BigDecimal 對應 DECIMAL
    status VARCHAR(20) NOT NULL      -- 狀態: CREATED, SHIPPED, CANCELLED
);

CREATE TABLE payment_view (
    payment_id VARCHAR(64) NOT NULL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,   -- 對應 Order Aggregate
    amount DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL      -- 狀態: CREATED, PROCESSED, CANCELLED, REFUNDED
);

CREATE INDEX idx_payment_order_id ON payment_view(order_id);
CREATE INDEX idx_order_status ON order_view(status);
CREATE INDEX idx_payment_status ON payment_view(status);