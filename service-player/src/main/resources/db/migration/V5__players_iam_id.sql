-- =============================================================================
-- V5__players_iam_id.sql — service-player
--
-- Keycloak replaces Zitadel (ADR 0024). The token carries the player's
-- publicId itself, and the row keeps Keycloak's own user id, the token's sub,
-- for later calls into Keycloak's admin API. It never leaves this service.
--
-- Every existing row belongs to a Zitadel account that no longer exists, so
-- the rows go. Nothing is deployed, so these are dev rows only.
-- =============================================================================

DELETE FROM characters;
DELETE FROM players;

ALTER TABLE players RENAME COLUMN zitadel_user_id TO iam_id;
ALTER TABLE players RENAME CONSTRAINT uq_players_zitadel_user_id TO uq_players_iam_id;
