-- =============================================================================
-- V4__purchase_author_not_null.sql — service-commerce
--
-- Every purchase names its author: a player's publicId or a SystemActor id
-- (ADR 0013). The rows written before that have no author. Nothing is
-- deployed, so they are deleted, not credited to an invented one.
-- =============================================================================

DELETE FROM purchases;

ALTER TABLE purchases ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN updated_by SET NOT NULL;
