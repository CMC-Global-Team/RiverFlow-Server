-- ==============================================================================
-- MIGRATION: Fix avatar_data column size
-- ==============================================================================
-- Description: Modify avatar_data column to use LONGBLOB instead of smaller BLOB
-- This fixes the "Data truncation: Data too long for column 'avatar_data'" error
-- when uploading larger avatar images

ALTER TABLE users MODIFY COLUMN avatar_data LONGBLOB NULL COMMENT 'Avatar image binary data (BLOB)';
