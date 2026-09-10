-- =============================================================================
-- V2__characters_public_id_default.sql — service-player
--
-- Gives characters.public_id the same DEFAULT that players.public_id already
-- had in the V1 baseline. The two columns serve the same purpose (ADR 0003:
-- a public UUID separate from the database key) and should behave the same
-- way, whoever does the inserting.
--
-- This changes nothing for Hibernate: GameCharacter.prePersist still sets the
-- UUID before every JPA insert, and Hibernate's validate does not check
-- defaults. It matters for every other writer — a migration, a fixture, a
-- psql session — which would otherwise hit the NOT NULL constraint.
--
-- ONCE APPLIED, THIS FILE IS IMMUTABLE. Change the schema by adding V3.
-- =============================================================================

ALTER TABLE characters
    ALTER COLUMN public_id SET DEFAULT gen_random_uuid();
