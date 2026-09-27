CREATE TABLE category (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(60) NOT NULL UNIQUE
);
CREATE TABLE channel (
 id BIGINT PRIMARY KEY, code VARCHAR(20) NOT NULL UNIQUE, name VARCHAR(30) NOT NULL UNIQUE
);
CREATE TABLE product (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(40) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL, description TEXT NOT NULL, category_id BIGINT NOT NULL,
 deleted BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 CONSTRAINT fk_product_category FOREIGN KEY(category_id) REFERENCES category(id)
);
CREATE TABLE product_color (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, product_id BIGINT NOT NULL, name VARCHAR(40) NOT NULL,
 hex1 CHAR(7) NOT NULL, hex2 CHAR(7), deleted BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT fk_color_product FOREIGN KEY(product_id) REFERENCES product(id),
 INDEX ix_color_product(product_id,deleted)
);
CREATE TABLE product_image (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, color_id BIGINT NOT NULL, url VARCHAR(255) NOT NULL,
 sort_order INT NOT NULL DEFAULT 0,
 CONSTRAINT fk_image_color FOREIGN KEY(color_id) REFERENCES product_color(id)
);
CREATE TABLE sku (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, color_id BIGINT NOT NULL,
 size ENUM('S','M','L','XL','XXL') NOT NULL,
 UNIQUE KEY uk_sku(color_id,size),
 CONSTRAINT fk_sku_color FOREIGN KEY(color_id) REFERENCES product_color(id)
);
CREATE TABLE listing (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, sku_id BIGINT NOT NULL, channel_id BIGINT NOT NULL,
 price DECIMAL(10,2) NOT NULL CHECK(price > 0), on_sale BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_listing(sku_id,channel_id),
 CONSTRAINT fk_listing_sku FOREIGN KEY(sku_id) REFERENCES sku(id),
 CONSTRAINT fk_listing_channel FOREIGN KEY(channel_id) REFERENCES channel(id),
 INDEX ix_listing_sale(on_sale,channel_id)
);
CREATE TABLE customer (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, phone CHAR(11) NOT NULL UNIQUE,
 nickname VARCHAR(40) NOT NULL, gender ENUM('UNKNOWN','MALE','FEMALE') NOT NULL DEFAULT 'UNKNOWN',
 birthday DATE, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
CREATE TABLE user_channel (
 user_id BIGINT NOT NULL, channel_id BIGINT NOT NULL, PRIMARY KEY(user_id,channel_id),
 CONSTRAINT fk_uc_user FOREIGN KEY(user_id) REFERENCES customer(id),
 CONSTRAINT fk_uc_channel FOREIGN KEY(channel_id) REFERENCES channel(id)
);
CREATE TABLE sales_order (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, order_no VARCHAR(50) NOT NULL UNIQUE,
 user_id BIGINT NOT NULL, listing_id BIGINT NOT NULL, channel_id BIGINT NOT NULL,
 product_code VARCHAR(40) NOT NULL, product_name VARCHAR(100) NOT NULL, color_name VARCHAR(40) NOT NULL,
 hex1 CHAR(7) NOT NULL, hex2 CHAR(7), size VARCHAR(5) NOT NULL, channel_name VARCHAR(30) NOT NULL,
 unit_price DECIMAL(10,2) NOT NULL, quantity INT NOT NULL CHECK(quantity BETWEEN 1 AND 3),
 total_price DECIMAL(12,2) NOT NULL,
 status ENUM('PENDING_PAYMENT','CANCELLED','PENDING_SHIPMENT','REFUNDED','COMPLETED') NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 CONSTRAINT fk_order_user FOREIGN KEY(user_id) REFERENCES customer(id),
 CONSTRAINT fk_order_listing FOREIGN KEY(listing_id) REFERENCES listing(id),
 CONSTRAINT fk_order_channel FOREIGN KEY(channel_id) REFERENCES channel(id),
 INDEX ix_order_user_channel(user_id,channel_id,id),
 INDEX ix_order_status(status,id)
);

