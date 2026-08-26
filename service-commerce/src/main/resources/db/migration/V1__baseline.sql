-- =============================================================================
-- V1__baseline.sql — service-commerce
--
-- Generated with pg_dump from the schema that Hibernate's ddl-auto: update had
-- built, then cleaned by hand. From this migration onward Flyway owns the
-- schema and Hibernate only validates it (spring.jpa.hibernate.ddl-auto).
--
-- ONCE APPLIED, THIS FILE IS IMMUTABLE. Flyway stores a checksum of its
-- contents and refuses to start when they no longer match. Change the schema
-- by adding V2, V3, ... — never by editing this file.
--
-- Table and column names are unqualified: both services still share the
-- `public` schema, and commit B2 moves each into its own. A hardcoded
-- `public.` prefix here would break that.
--
-- purchases_status_check is Hibernate's own doing — @Enumerated(EnumType.STRING)
-- makes it emit a CHECK listing every enum constant. It is a real safety net
-- and it stays, but note the consequence: adding a value to PurchaseStatus now
-- needs a migration that alters this constraint, not just a Java change.
-- =============================================================================

CREATE TABLE purchases (
    id           uuid NOT NULL,
    player_id    varchar(255) NOT NULL,
    item_code    varchar(100) NOT NULL,
    quantity     integer NOT NULL,
    unit_price   numeric(19, 4) NOT NULL,
    total_price  numeric(19, 4) NOT NULL,
    status       varchar(20) NOT NULL,
    purchased_at timestamp(6) with time zone NOT NULL,
    version      integer,

    CONSTRAINT purchases_pkey PRIMARY KEY (id),
    CONSTRAINT purchases_status_check
        CHECK (status IN ('PENDING', 'COMPLETED', 'REFUNDED', 'CANCELLED'))
);
