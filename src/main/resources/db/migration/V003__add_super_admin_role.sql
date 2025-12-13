-- ==============================================================================
-- FILE: V003__add_super_admin_role.sql
-- ==============================================================================
-- Add super_admin role to the users table role ENUM
-- ==============================================================================

-- Modify the role column to include super_admin
ALTER TABLE users MODIFY COLUMN role ENUM('admin', 'user', 'super_admin') NOT NULL DEFAULT 'user';

-- ==============================================================================
-- END OF MIGRATION
-- ==============================================================================
