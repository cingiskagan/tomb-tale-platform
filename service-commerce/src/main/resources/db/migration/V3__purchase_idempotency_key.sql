-- =============================================================================
-- V3__purchase_idempotency_key.sql — service-commerce
--
-- The key that makes a repeated request or event produce one purchase. The
-- caller picks it: a fixed grant code for a one-time grant, or a new value
-- for each order. NULL means the purchase is not deduplicated.
-- =============================================================================

ALTER TABLE purchases ADD COLUMN idempotency_key varchar(64);
ALTER TABLE purchases ADD CONSTRAINT uq_purchases_player_id_idempotency_key UNIQUE (player_id, idempotency_key);
