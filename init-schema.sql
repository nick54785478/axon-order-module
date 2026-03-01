CREATE TABLE `order_view` (
    `order`_id VARCHAR(64) NOT NULL PRIMARY KEY,
    `amount` DECIMAL(19,4) NOT NULL,   -- BigDecimal 對應 DECIMAL
    `status` VARCHAR(20) NOT NULL      -- 狀態: CREATED, SHIPPED, CANCELLED
);

CREATE TABLE `payment_view` (
    `payment_id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `order_id` VARCHAR(64) NOT NULL,   -- 對應 Order Aggregate
    `amount` DECIMAL(19,4) NOT NULL,
    `status` VARCHAR(20) NOT NULL      -- 狀態: CREATED, PROCESSED, CANCELLED, REFUNDED
);

CREATE INDEX idx_payment_order_id ON payment_view(order_id);
CREATE INDEX idx_order_status ON order_view(status);
CREATE INDEX idx_payment_status ON payment_view(status);


-- 建立產品查詢視圖表
CREATE TABLE `product_view` (
  `product_id` VARCHAR(64) NOT NULL COMMENT '產品唯一識別碼',
  `name` VARCHAR(255) NOT NULL COMMENT '產品名稱',
  `price` DECIMAL(19, 2) NOT NULL COMMENT '產品單價',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '目前可用庫存',
  PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 建議建立索引以優化價格或名稱查詢
CREATE INDEX idx_product_price ON product_view(price);