-- =============================================================================
-- V7__outbox_traceparent.sql — service-player
--
-- The W3C traceparent of the trace that wrote the row, 55 characters. The
-- publisher sends the row inside that trace, so one trace follows an event
-- across the services (ADR 0028). NULL for a row written before this column.
-- =============================================================================

ALTER TABLE outbox ADD COLUMN traceparent varchar(55);
