-- =============================================================================
-- V6__author_not_null.sql — service-player
--
-- Every row names its author: a player's publicId or a SystemActor id
-- (ADR 0013). Since V5, every row came from a token that carried one, so no
-- row is deleted.
-- =============================================================================

ALTER TABLE players ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE players ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE characters ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE characters ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE outbox ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE outbox ALTER COLUMN updated_by SET NOT NULL;
