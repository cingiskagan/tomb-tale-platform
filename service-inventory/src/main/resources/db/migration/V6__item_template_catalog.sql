-- =============================================================================
-- V6__item_template_catalog.sql — service-inventory
--
-- The portal creates, edits and retires templates, so a template gains what
-- the portal and the game show. base_stats maps stat names to values, and
-- version makes an edit from a stale read fail (docs/design/pathway.md).
--
-- No template exists yet, because nothing wrote this table, so the new
-- columns take no default.
-- =============================================================================

ALTER TABLE item_templates
    ADD COLUMN name       varchar(100) NOT NULL,
    ADD COLUMN item_type  varchar(30) NOT NULL,
    ADD COLUMN price      bigint NOT NULL,
    ADD COLUMN base_stats jsonb NOT NULL,
    ADD COLUMN asset_key  varchar(200) NOT NULL,
    ADD COLUMN retired    boolean NOT NULL,
    ADD COLUMN version    integer NOT NULL,
    ADD CONSTRAINT ck_item_templates_item_type CHECK (item_type IN (
        'ONE_HANDED_WEAPON', 'TWO_HANDED_WEAPON', 'SHIELD', 'HELMET', 'ARMOR',
        'GLOVES', 'BOOTS', 'AMULET', 'RING')),
    ADD CONSTRAINT ck_item_templates_price CHECK (price >= 0);
