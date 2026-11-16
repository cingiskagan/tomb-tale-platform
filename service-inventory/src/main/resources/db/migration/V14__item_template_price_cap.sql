-- =============================================================================
-- V14__item_template_price_cap.sql — service-inventory
--
-- A template's price is at most a billion coins, so a red copy's price, and a
-- sell share of it, still fit a bigint. The request DTOs check the same cap.
-- =============================================================================

ALTER TABLE item_templates
    DROP CONSTRAINT ck_item_templates_price,
    ADD CONSTRAINT ck_item_templates_price CHECK (price BETWEEN 0 AND 1000000000);
