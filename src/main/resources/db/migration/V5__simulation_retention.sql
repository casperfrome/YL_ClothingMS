CREATE TABLE simulation_settings (
 mode VARCHAR(20) PRIMARY KEY,
 retain_data BOOLEAN NOT NULL DEFAULT FALSE
);
INSERT INTO simulation_settings(mode,retain_data) VALUES ('ORDERS_ONLY',FALSE),('FULL_DATA',FALSE);

CREATE TABLE simulation_run (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 mode VARCHAR(20) NOT NULL,
 retain_data BOOLEAN NOT NULL DEFAULT FALSE,
 started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 ended_at DATETIME(3),
 INDEX ix_simulation_run_retained(retain_data,id)
);

ALTER TABLE customer ADD COLUMN simulation_run_id BIGINT,
 ADD CONSTRAINT fk_customer_simulation_run FOREIGN KEY(simulation_run_id) REFERENCES simulation_run(id);
ALTER TABLE sales_order ADD COLUMN simulation_run_id BIGINT,
 ADD CONSTRAINT fk_order_simulation_run FOREIGN KEY(simulation_run_id) REFERENCES simulation_run(id);
CREATE INDEX ix_order_run_due ON sales_order(simulation_run_id,status,updated_at,id);

-- The old simulator owned every order and every simulated customer. Keep that
-- ownership during upgrade; normal startup will discard this unretained run.
INSERT INTO simulation_run(mode,retain_data)
SELECT 'LEGACY',FALSE
WHERE EXISTS (SELECT 1 FROM sales_order) OR EXISTS (SELECT 1 FROM customer WHERE simulated=TRUE);
UPDATE sales_order SET simulation_run_id=(SELECT id FROM simulation_run WHERE mode='LEGACY');
UPDATE customer SET simulation_run_id=(SELECT id FROM simulation_run WHERE mode='LEGACY') WHERE simulated=TRUE;
