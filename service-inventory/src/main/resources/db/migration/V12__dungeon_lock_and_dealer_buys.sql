-- =============================================================================
-- V12__dungeon_lock_and_dealer_buys.sql — service-inventory
--
-- The dungeon lock is a lease on the character's row: the run's id and the
-- time it lasts until, both set or both empty. While it holds, nothing crosses
-- the dungeon's border, such as a dealer buy (docs/design/inventory-design.md,
-- The dungeon lock). A buy writes its coin change under its trade id.
-- =============================================================================

ALTER TABLE character_inventories
    ADD COLUMN dungeon_run_id     uuid,
    ADD COLUMN dungeon_lock_until timestamp(6) with time zone,
    ADD CONSTRAINT ck_character_inventories_dungeon_lock
        CHECK ((dungeon_run_id IS NULL) = (dungeon_lock_until IS NULL));

ALTER TABLE coin_history
    DROP CONSTRAINT ck_coin_history_reason,
    ADD CONSTRAINT ck_coin_history_reason CHECK (reason IN ('STARTING_COINS', 'DEALER_BUY'));
