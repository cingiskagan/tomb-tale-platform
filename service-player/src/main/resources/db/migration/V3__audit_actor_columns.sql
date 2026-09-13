-- =============================================================================
-- V3__audit_actor_columns.sql — service-player
--
-- Adds the two audit columns that complete BaseEntity. created_at and
-- updated_at already existed; created_by and updated_by did not, so nothing
-- recorded which principal caused a row to change.
--
-- Both are nullable on purpose. A player's first write is the transaction that
-- creates that player, and the publicId naming the author is the one being
-- inserted, so it cannot be read back yet. See ADR 0013.
-- =============================================================================

ALTER TABLE players ADD COLUMN created_by uuid;
ALTER TABLE players ADD COLUMN updated_by uuid;

ALTER TABLE characters ADD COLUMN created_by uuid;
ALTER TABLE characters ADD COLUMN updated_by uuid;
