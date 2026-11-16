-- =============================================================================
-- V15__bonus_lines.sql — service-inventory
--
-- A bonus is a list of lines, one per roll, so a stat can roll more than once
-- (docs/design/inventory-design.md, Bonus stats). Each stat a stored bonus
-- held becomes one line.
-- =============================================================================

UPDATE item_instances
SET bonus = (SELECT coalesce(jsonb_agg(jsonb_build_object('stat', key, 'value', value) ORDER BY key), '[]')
             FROM jsonb_each(bonus));

ALTER TABLE item_instances
    ALTER COLUMN bonus SET DEFAULT '[]';

UPDATE stand_entries
SET bonus = (SELECT coalesce(jsonb_agg(jsonb_build_object('stat', key, 'value', value) ORDER BY key), '[]')
             FROM jsonb_each(bonus))
WHERE bonus IS NOT NULL;
