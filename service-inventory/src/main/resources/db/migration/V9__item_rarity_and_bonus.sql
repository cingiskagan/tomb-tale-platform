-- =============================================================================
-- V9__item_rarity_and_bonus.sql — service-inventory
--
-- Every item has a rarity, gray by default, and the bonus stats it rolled,
-- none by default. Copies stack only with the same template and rarity and
-- no bonus (docs/design/inventory-design.md, Items).
-- =============================================================================

ALTER TABLE item_instances
    ADD COLUMN rarity varchar(10) NOT NULL DEFAULT 'GRAY',
    ADD COLUMN bonus  jsonb NOT NULL DEFAULT '{}',
    ADD CONSTRAINT ck_item_instances_rarity
        CHECK (rarity IN ('GRAY', 'GREEN', 'BLUE', 'PURPLE', 'ORANGE', 'RED'));
