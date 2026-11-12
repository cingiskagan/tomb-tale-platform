-- =============================================================================
-- V13__dealer_sells.sql — service-inventory
--
-- A sell to a dealer writes its coin change under its trade id, so a sell
-- sent twice finds the first one (docs/design/inventory-design.md, Prices).
-- =============================================================================

ALTER TABLE coin_history
    DROP CONSTRAINT ck_coin_history_reason,
    ADD CONSTRAINT ck_coin_history_reason CHECK (reason IN ('STARTING_COINS', 'DEALER_BUY', 'DEALER_SELL'));
