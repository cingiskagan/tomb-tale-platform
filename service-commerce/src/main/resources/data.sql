-- =============================================================================
-- Seed data for the purchases table.
-- Loaded on every application start (spring.sql.init.mode=always).
-- Main profile uses continue-on-error=true to skip duplicates on restarts.
-- Test profile (H2 create-drop) always starts fresh.
-- =============================================================================

INSERT INTO purchases (id, player_id, item_code, quantity, unit_price, total_price, status, purchased_at, version)
VALUES (CAST('a1b2c3d4-e5f6-7890-abcd-ef1234567890' AS UUID), 'player-001', 'SWORD_IRON', 1, 150.0000, 150.0000, 'COMPLETED', TIMESTAMP '2026-01-15 10:30:00', 0);

INSERT INTO purchases (id, player_id, item_code, quantity, unit_price, total_price, status, purchased_at, version)
VALUES (CAST('b2c3d4e5-f6a7-8901-bcde-f12345678901' AS UUID), 'player-001', 'POTION_HEALTH', 5, 25.0000, 125.0000, 'COMPLETED', TIMESTAMP '2026-01-16 14:45:00', 0);

INSERT INTO purchases (id, player_id, item_code, quantity, unit_price, total_price, status, purchased_at, version)
VALUES (CAST('c3d4e5f6-a7b8-9012-cdef-123456789012' AS UUID), 'player-002', 'SHIELD_WOODEN', 1, 80.0000, 80.0000, 'PENDING', TIMESTAMP '2026-02-01 09:15:00', 0);

INSERT INTO purchases (id, player_id, item_code, quantity, unit_price, total_price, status, purchased_at, version)
VALUES (CAST('d4e5f6a7-b8c9-0123-defa-234567890123' AS UUID), 'player-003', 'ARMOR_LEATHER', 1, 300.0000, 300.0000, 'COMPLETED', TIMESTAMP '2026-02-10 18:00:00', 0);

INSERT INTO purchases (id, player_id, item_code, quantity, unit_price, total_price, status, purchased_at, version)
VALUES (CAST('e5f6a7b8-c9d0-1234-efab-345678901234' AS UUID), 'player-002', 'POTION_MANA', 10, 30.0000, 300.0000, 'REFUNDED', TIMESTAMP '2026-02-15 11:20:00', 0);
