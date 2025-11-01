-- ==============================================================================
-- FILE: mysql_schema.sql
-- ==============================================================================
-- MySQL Database Schema for Mindmap Online Real-time System
-- Version: 1.0
-- Description: User management, authentication, authorization, payments, packages
-- ==============================================================================

-- Drop existing database if exists (use with caution in production)
-- DROP DATABASE IF EXISTS mindmap_system;

-- Create database
CREATE DATABASE IF NOT EXISTS mindmap_system 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE mindmap_system;

-- ==============================================================================
-- USERS TABLE
-- ==============================================================================
-- Description: Store user information, support both email and OAuth login
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL COMMENT 'NULL for OAuth users',
    full_name VARCHAR(255) NOT NULL,
    avatar VARCHAR(500) NULL COMMENT 'URL to user avatar',
    role ENUM('admin', 'user') NOT NULL DEFAULT 'user',
    status ENUM('active', 'suspended', 'deleted') NOT NULL DEFAULT 'active',
    
    -- OAuth fields
    oauth_provider ENUM('email', 'google', 'github') NOT NULL DEFAULT 'email',
    oauth_id VARCHAR(255) NULL COMMENT 'ID from OAuth provider',
    
    -- Email verification
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMP NULL,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    
    INDEX idx_email (email),
    INDEX idx_oauth (oauth_provider, oauth_id),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- EMAIL VERIFICATIONS TABLE
-- ==============================================================================
-- Description: Manage email verification tokens
CREATE TABLE email_verifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PASSWORD RESETS TABLE
-- ==============================================================================
-- Description: Manage password reset tokens
CREATE TABLE password_resets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- REFRESH TOKENS TABLE
-- ==============================================================================
-- Description: Store JWT refresh tokens for secure authentication
CREATE TABLE refresh_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP NULL,
    device_info VARCHAR(500) NULL COMMENT 'User agent, IP, etc.',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PACKAGES TABLE
-- ==============================================================================
-- Description: Service packages created by admin (Free, Pro, Enterprise, etc.)
CREATE TABLE packages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    
    -- Pricing
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Price in USD',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    duration_days INT UNSIGNED NOT NULL DEFAULT 30 COMMENT 'Package duration in days',
    
    -- Limits
    max_mindmaps INT UNSIGNED NOT NULL DEFAULT 10 COMMENT '0 = unlimited',
    max_collaborators INT UNSIGNED NOT NULL DEFAULT 5 COMMENT '0 = unlimited',
    max_storage_mb INT UNSIGNED NOT NULL DEFAULT 100 COMMENT 'Storage limit in MB',
    
    -- Features (stored as JSON for flexible checkbox configuration)
    features JSON NULL COMMENT 'Package features: {"real_time": true, "export_pdf": true, "templates": true, ...}',
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0 COMMENT 'Order to display on pricing page',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_slug (slug),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PACKAGE FEATURES TABLE
-- ==============================================================================
-- Description: Define available features that can be assigned to packages
CREATE TABLE package_features (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    feature_key VARCHAR(100) NOT NULL UNIQUE COMMENT 'e.g., real_time, export_pdf, ai_suggestions',
    feature_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'general' COMMENT 'e.g., collaboration, export, advanced',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_feature_key (feature_key),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- USER SUBSCRIPTIONS TABLE
-- ==============================================================================
-- Description: Track user subscriptions to packages
CREATE TABLE user_subscriptions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    package_id BIGINT UNSIGNED NOT NULL,
    payment_id BIGINT UNSIGNED NULL COMMENT 'Reference to payment record',
    
    -- Subscription period
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    
    -- Status
    status ENUM('active', 'expired', 'cancelled', 'pending') NOT NULL DEFAULT 'pending',
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Cancellation
    cancelled_at TIMESTAMP NULL,
    cancellation_reason TEXT NULL,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE RESTRICT,
    INDEX idx_user_id (user_id),
    INDEX idx_package_id (package_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PAYMENTS TABLE
-- ==============================================================================
-- Description: Store payment transactions (QR Banking, PayPal, etc.)
CREATE TABLE payments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    package_id BIGINT UNSIGNED NOT NULL,
    
    -- Payment details
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_method ENUM('qr_banking', 'paypal', 'stripe', 'manual') NOT NULL,
    payment_status ENUM('pending', 'completed', 'failed', 'refunded', 'cancelled') NOT NULL DEFAULT 'pending',
    
    -- Transaction info
    transaction_id VARCHAR(255) NULL COMMENT 'External transaction ID from payment gateway',
    payment_gateway_response JSON NULL COMMENT 'Full response from payment gateway',
    
    -- QR Banking specific
    qr_code_url VARCHAR(500) NULL COMMENT 'URL to generated QR code',
    bank_transaction_ref VARCHAR(255) NULL COMMENT 'Bank transaction reference',
    
    -- Payment metadata
    metadata JSON NULL COMMENT 'Additional payment information',
    
    -- Admin actions
    verified_by BIGINT UNSIGNED NULL COMMENT 'Admin who verified the payment',
    verified_at TIMESTAMP NULL,
    notes TEXT NULL COMMENT 'Admin notes about the payment',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE RESTRICT,
    FOREIGN KEY (verified_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_status (payment_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- AUDIT LOG TABLE
-- ==============================================================================
-- Description: Track important system actions for security and debugging
CREATE TABLE audit_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL,
    action VARCHAR(100) NOT NULL COMMENT 'e.g., user.login, user.register, payment.completed',
    entity_type VARCHAR(50) NULL COMMENT 'e.g., user, payment, subscription',
    entity_id BIGINT UNSIGNED NULL,
    
    -- Request info
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    
    -- Details
    details JSON NULL COMMENT 'Additional context about the action',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- SYSTEM SETTINGS TABLE
-- ==============================================================================
-- Description: Store system-wide configuration (key-value pairs)
CREATE TABLE system_settings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    setting_type ENUM('string', 'number', 'boolean', 'json') NOT NULL DEFAULT 'string',
    description TEXT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Can be accessed by frontend',
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_setting_key (setting_key),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- INITIAL DATA
-- ==============================================================================

-- Insert default admin user (password: Admin@123 - hash generated with bcrypt)
INSERT INTO users (email, password_hash, full_name, role, email_verified, email_verified_at) VALUES
('admin@mindmap.com', '$2b$10$rGHcY6Y6KdGj5pXZOXK4xOGQjqZ.x5m3tV8HxNQXJNc9LYvL6LY8i', 'System Administrator', 'admin', TRUE, NOW());

-- Insert default package features
INSERT INTO package_features (feature_key, feature_name, description, category) VALUES
('real_time_collaboration', 'Real-time Collaboration', 'Edit mindmaps together in real-time', 'collaboration'),
('unlimited_mindmaps', 'Unlimited Mindmaps', 'Create unlimited number of mindmaps', 'storage'),
('unlimited_collaborators', 'Unlimited Collaborators', 'Invite unlimited collaborators', 'collaboration'),
('export_pdf', 'Export to PDF', 'Export mindmaps as PDF files', 'export'),
('export_png', 'Export to PNG', 'Export mindmaps as PNG images', 'export'),
('export_json', 'Export to JSON', 'Export mindmaps as JSON data', 'export'),
('custom_templates', 'Custom Templates', 'Create and use custom templates', 'advanced'),
('version_history', 'Version History', 'Access full version history', 'advanced'),
('ai_suggestions', 'AI Suggestions', 'Get AI-powered mindmap suggestions', 'advanced'),
('priority_support', 'Priority Support', '24/7 priority customer support', 'support'),
('custom_branding', 'Custom Branding', 'Remove branding and add your own', 'advanced'),
('api_access', 'API Access', 'Access to REST API', 'developer');

-- Insert default packages
INSERT INTO packages (name, description, slug, price, currency, duration_days, max_mindmaps, max_collaborators, max_storage_mb, features, is_active, display_order) VALUES
-- Free Plan
('Free', 'Perfect for getting started', 'free', 0.00, 'USD', 365, 3, 2, 50, 
JSON_OBJECT(
    'real_time_collaboration', true,
    'unlimited_mindmaps', false,
    'unlimited_collaborators', false,
    'export_pdf', false,
    'export_png', true,
    'export_json', true,
    'custom_templates', false,
    'version_history', false,
    'ai_suggestions', false,
    'priority_support', false,
    'custom_branding', false,
    'api_access', false
), TRUE, 1),

-- Pro Plan
('Pro', 'For professionals and small teams', 'pro', 9.99, 'USD', 30, 50, 10, 500,
JSON_OBJECT(
    'real_time_collaboration', true,
    'unlimited_mindmaps', false,
    'unlimited_collaborators', false,
    'export_pdf', true,
    'export_png', true,
    'export_json', true,
    'custom_templates', true,
    'version_history', true,
    'ai_suggestions', true,
    'priority_support', false,
    'custom_branding', false,
    'api_access', false
), TRUE, 2),

-- Enterprise Plan
('Enterprise', 'For large organizations', 'enterprise', 49.99, 'USD', 30, 0, 0, 0,
JSON_OBJECT(
    'real_time_collaboration', true,
    'unlimited_mindmaps', true,
    'unlimited_collaborators', true,
    'export_pdf', true,
    'export_png', true,
    'export_json', true,
    'custom_templates', true,
    'version_history', true,
    'ai_suggestions', true,
    'priority_support', true,
    'custom_branding', true,
    'api_access', true
), TRUE, 3);

-- Insert default system settings
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public) VALUES
('site_name', 'Mindmap System', 'string', 'Website name', TRUE),
('max_upload_size_mb', '10', 'number', 'Maximum file upload size in MB', FALSE),
('email_verification_required', 'true', 'boolean', 'Require email verification for new users', FALSE),
('maintenance_mode', 'false', 'boolean', 'Enable maintenance mode', TRUE),
('default_package_id', '1', 'number', 'Default package ID for new users', FALSE);

-- ==============================================================================
-- VIEWS FOR COMMON QUERIES
-- ==============================================================================

-- View: Active user subscriptions with package details
CREATE VIEW v_active_subscriptions AS
SELECT 
    us.id AS subscription_id,
    us.user_id,
    u.email,
    u.full_name,
    p.id AS package_id,
    p.name AS package_name,
    p.slug AS package_slug,
    p.features AS package_features,
    us.start_date,
    us.end_date,
    us.status,
    us.auto_renew,
    DATEDIFF(us.end_date, NOW()) AS days_remaining
FROM user_subscriptions us
INNER JOIN users u ON us.user_id = u.id
INNER JOIN packages p ON us.package_id = p.id
WHERE us.status = 'active' 
AND us.end_date > NOW()
AND u.status = 'active';

-- View: Payment summary for reports
CREATE VIEW v_payment_summary AS
SELECT 
    p.id AS payment_id,
    p.user_id,
    u.email,
    u.full_name,
    pkg.name AS package_name,
    p.amount,
    p.currency,
    p.payment_method,
    p.payment_status,
    p.transaction_id,
    p.created_at,
    p.completed_at
FROM payments p
INNER JOIN users u ON p.user_id = u.id
INNER JOIN packages pkg ON p.package_id = pkg.id;

-- ==============================================================================
-- STORED PROCEDURES
-- ==============================================================================

DELIMITER //

-- Procedure: Check if user has feature access
CREATE PROCEDURE sp_check_user_feature(
    IN p_user_id BIGINT UNSIGNED,
    IN p_feature_key VARCHAR(100),
    OUT p_has_access BOOLEAN
)
BEGIN
    DECLARE v_features JSON;
    
    -- Get active subscription features
    SELECT p.features INTO v_features
    FROM user_subscriptions us
    INNER JOIN packages p ON us.package_id = p.id
    WHERE us.user_id = p_user_id
    AND us.status = 'active'
    AND us.end_date > NOW()
    ORDER BY us.end_date DESC
    LIMIT 1;
    
    -- Check if feature exists and is enabled
    IF v_features IS NOT NULL THEN
        SET p_has_access = JSON_EXTRACT(v_features, CONCAT('$.', p_feature_key)) = true;
    ELSE
        SET p_has_access = FALSE;
    END IF;
END //

-- Procedure: Get user package limits
CREATE PROCEDURE sp_get_user_limits(
    IN p_user_id BIGINT UNSIGNED
)
BEGIN
    SELECT 
        p.max_mindmaps,
        p.max_collaborators,
        p.max_storage_mb,
        p.features
    FROM user_subscriptions us
    INNER JOIN packages p ON us.package_id = p.id
    WHERE us.user_id = p_user_id
    AND us.status = 'active'
    AND us.end_date > NOW()
    ORDER BY us.end_date DESC
    LIMIT 1;
END //

DELIMITER ;

-- ==============================================================================
-- INDEXES FOR PERFORMANCE
-- ==============================================================================

-- Additional indexes for better query performance
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_payments_completed_at ON payments(completed_at);
CREATE INDEX idx_subscriptions_end_date ON user_subscriptions(end_date);

-- ==============================================================================
-- END OF SCHEMA
-- ==============================================================================

