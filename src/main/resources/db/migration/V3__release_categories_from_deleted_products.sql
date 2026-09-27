-- Deleted products retain their order snapshots and do not reserve category membership.
ALTER TABLE product MODIFY category_id BIGINT NULL;

