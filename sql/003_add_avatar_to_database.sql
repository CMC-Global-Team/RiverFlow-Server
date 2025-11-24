-- Migration script: Add avatar_data and avatar_mime_type columns to users table
-- This moves avatar storage from disk files to database BLOB columns

-- Add avatar_data column to store image binary data
ALTER TABLE users ADD COLUMN avatar_data LONGBLOB COMMENT 'Avatar image binary data (BLOB)' AFTER avatar;

-- Add avatar_mime_type column to store MIME type
ALTER TABLE users ADD COLUMN avatar_mime_type VARCHAR(50) COMMENT 'MIME type of avatar image (e.g., image/png, image/jpeg)' AFTER avatar_data;

-- Add index on avatar_mime_type for efficient queries
CREATE INDEX idx_avatar_mime_type ON users(avatar_mime_type);

-- Verify the changes
DESCRIBE users;

-- Check if any avatars already exist (for migration purposes)
SELECT id, email, full_name, avatar, avatar_data, avatar_mime_type 
FROM users 
WHERE avatar IS NOT NULL 
   OR avatar_data IS NOT NULL;
