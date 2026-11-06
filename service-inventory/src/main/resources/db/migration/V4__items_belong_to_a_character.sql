-- =============================================================================
-- V4__items_belong_to_a_character.sql — service-inventory
--
-- A character owns its items, so a stack points at its character's row in
-- place of the player. An item grant records the character the purchase
-- names with no foreign key, because a rejected one may name a character
-- inventory doesn't know.
--
-- THIS MIGRATION DELETES EVERY STACK AND ITEM GRANT. They name no character,
-- and nothing is deployed.
-- =============================================================================

DELETE FROM item_instances;
DELETE FROM item_grants;

DROP INDEX idx_item_instances_player_template;

ALTER TABLE item_instances
    DROP COLUMN player_id,
    ADD COLUMN character_inventory_id bigint NOT NULL,
    ADD CONSTRAINT fk_item_instances_character_inventory
        FOREIGN KEY (character_inventory_id) REFERENCES character_inventories (id);

-- Serves: a character's stacks of one template, which every item grant reads.
CREATE INDEX idx_item_instances_character_template ON item_instances (character_inventory_id, template_id);

ALTER TABLE item_grants ADD COLUMN character_id uuid NOT NULL;
