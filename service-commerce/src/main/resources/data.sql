-- =============================================================================
-- Seed data for the purchases table.
-- Loaded on every application start (spring.sql.init.mode=always).
-- Main profile uses continue-on-error=true to skip duplicates on restarts.
-- Test profile (H2 create-drop) always starts fresh.
--
-- player_id holds a player's publicId (ADR 0014). These three name no real
-- player: commerce cannot check that they do, which is exactly the property
-- the seed rows are here to exercise. They are shaped to be recognisable as
-- seed data rather than to look like something service-player issued.
--
-- id is generated — the column is an identity now — so only public_id is
-- given, and it is what makes a restart skip a row it already inserted.
-- =============================================================================

INSERT INTO purchases (public_id, player_id, item_code, quantity, unit_price, total_price, status, created_at, updated_at, version)
VALUES (CAST('a1b2c3d4-e5f6-7890-abcd-ef1234567890' AS UUID), CAST('aaaaaaaa-0000-4000-8000-000000000001' AS UUID), 'SWORD_IRON', 1, 150.0000, 150.0000, 'COMPLETED', TIMESTAMP '2026-01-15 10:30:00', TIMESTAMP '2026-01-15 10:30:00', 0);

INSERT INTO purchases (public_id, player_id, item_code, quantity, unit_price, total_price, status, created_at, updated_at, version)
VALUES (CAST('b2c3d4e5-f6a7-8901-bcde-f12345678901' AS UUID), CAST('aaaaaaaa-0000-4000-8000-000000000001' AS UUID), 'POTION_HEALTH', 5, 25.0000, 125.0000, 'COMPLETED', TIMESTAMP '2026-01-16 14:45:00', TIMESTAMP '2026-01-16 14:45:00', 0);

INSERT INTO purchases (public_id, player_id, item_code, quantity, unit_price, total_price, status, created_at, updated_at, version)
VALUES (CAST('c3d4e5f6-a7b8-9012-cdef-123456789012' AS UUID), CAST('aaaaaaaa-0000-4000-8000-000000000002' AS UUID), 'SHIELD_WOODEN', 1, 80.0000, 80.0000, 'PENDING', TIMESTAMP '2026-02-01 09:15:00', TIMESTAMP '2026-02-01 09:15:00', 0);

INSERT INTO purchases (public_id, player_id, item_code, quantity, unit_price, total_price, status, created_at, updated_at, version)
VALUES (CAST('d4e5f6a7-b8c9-0123-defa-234567890123' AS UUID), CAST('aaaaaaaa-0000-4000-8000-000000000003' AS UUID), 'ARMOR_LEATHER', 1, 300.0000, 300.0000, 'COMPLETED', TIMESTAMP '2026-02-10 18:00:00', TIMESTAMP '2026-02-10 18:00:00', 0);

INSERT INTO purchases (public_id, player_id, item_code, quantity, unit_price, total_price, status, created_at, updated_at, version)
VALUES (CAST('e5f6a7b8-c9d0-1234-efab-345678901234' AS UUID), CAST('aaaaaaaa-0000-4000-8000-000000000002' AS UUID), 'POTION_MANA', 10, 30.0000, 300.0000, 'REFUNDED', TIMESTAMP '2026-02-15 11:20:00', TIMESTAMP '2026-02-15 11:20:00', 0);
