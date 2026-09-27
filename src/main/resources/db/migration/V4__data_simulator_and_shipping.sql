ALTER TABLE customer ADD COLUMN simulated BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX ix_customer_simulated ON customer(simulated,id);

ALTER TABLE sales_order MODIFY COLUMN status
 ENUM('PENDING_PAYMENT','CANCELLED','PENDING_SHIPMENT','REFUNDED','COMPLETED','SHIPPED') NOT NULL;
CREATE INDEX ix_order_due ON sales_order(status,updated_at,id);
