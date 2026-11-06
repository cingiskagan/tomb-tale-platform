-- =============================================================================
-- V8__purchase_character_id.sql — service-commerce
--
-- The character that receives the purchase's items, by its publicId. Like
-- player_id, it has no foreign key, because commerce has no rights in the
-- player schema. Inventory checks that the player owns it.
--
-- THIS MIGRATION DELETES EVERY ROW. The existing purchases name no character,
-- and nothing is deployed.
-- =============================================================================

DELETE FROM purchases;

ALTER TABLE purchases ADD COLUMN character_id uuid NOT NULL;
