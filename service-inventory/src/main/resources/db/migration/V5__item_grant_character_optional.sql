-- =============================================================================
-- V5__item_grant_character_optional.sql — service-inventory
--
-- A purchase names only the player, and inventory gives it to the player's
-- only character. A grant refused because the player has none or several
-- names no character.
-- =============================================================================

ALTER TABLE item_grants ALTER COLUMN character_id DROP NOT NULL;
