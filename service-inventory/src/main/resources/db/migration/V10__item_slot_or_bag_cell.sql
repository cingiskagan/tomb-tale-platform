-- =============================================================================
-- V10__item_slot_or_bag_cell.sql — service-inventory
--
-- An item sits in one equipment slot or in one bag cell, never both. Each is
-- unique per character and checked at the commit, so a swap of two items
-- doesn't collide halfway. A unique constraint ignores NULL, so empty slots
-- and cells never collide (docs/design/inventory-design.md, Equipment, Bag).
--
-- THIS MIGRATION DELETES EVERY ITEM. They have no cell, and nothing is
-- deployed.
-- =============================================================================

DELETE FROM item_instances;

ALTER TABLE item_instances
    ADD COLUMN equipment_slot varchar(20),
    ADD COLUMN bag_position   integer,
    ADD CONSTRAINT ck_item_instances_slot_or_cell CHECK ((equipment_slot IS NULL) <> (bag_position IS NULL)),
    ADD CONSTRAINT ck_item_instances_equipment_slot CHECK (equipment_slot IN (
        'MAIN_HAND', 'OFF_HAND', 'HEAD', 'CHEST', 'HANDS', 'FEET', 'NECK', 'RING_1', 'RING_2')),
    ADD CONSTRAINT ck_item_instances_bag_position CHECK (bag_position >= 0),
    ADD CONSTRAINT uq_item_instances_character_slot
        UNIQUE (character_inventory_id, equipment_slot) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT uq_item_instances_character_cell
        UNIQUE (character_inventory_id, bag_position) DEFERRABLE INITIALLY DEFERRED;
