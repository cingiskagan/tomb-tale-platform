-- =============================================================================
-- V9__purchase_drops_character_id.sql — service-commerce
--
-- A purchase names only the player again. Until the player can pick the
-- character that claims it, inventory gives the items to the player's only
-- character.
-- =============================================================================

ALTER TABLE purchases DROP COLUMN character_id;
