-- =============================================================================
-- V6__purchase_cancellation_reason.sql — service-commerce
--
-- Why a purchase was cancelled, such as UNKNOWN_ITEM_CODE from inventory's
-- item.grant-rejected. Commerce stores the code as it arrives and does not
-- interpret it. NULL for a purchase that is not cancelled, or that an admin
-- cancelled. 50 characters, like inventory's own reason column.
-- =============================================================================

ALTER TABLE purchases ADD COLUMN cancellation_reason varchar(50);
