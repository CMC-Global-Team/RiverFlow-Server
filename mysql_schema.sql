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
-- CURRENCIES TABLE
-- ==============================================================================
-- Description: Supported currencies for multi-currency support
CREATE TABLE currencies (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE COMMENT 'ISO 4217 currency code (USD, EUR, VND, etc.)',
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL COMMENT 'Currency symbol ($, €, ₫, etc.)',
    decimal_places TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT 'Number of decimal places',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_code (code),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- EXCHANGE RATES TABLE
-- ==============================================================================
-- Description: Exchange rates between currencies (relative to base currency USD)
CREATE TABLE exchange_rates (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    from_currency_id INT UNSIGNED NOT NULL,
    to_currency_id INT UNSIGNED NOT NULL,
    rate DECIMAL(20, 8) NOT NULL COMMENT 'Exchange rate (1 from_currency = rate to_currency)',
    source VARCHAR(100) NULL COMMENT 'Rate source (API provider, manual, etc.)',
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP NULL COMMENT 'NULL means currently active',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (from_currency_id) REFERENCES currencies(id) ON DELETE RESTRICT,
    FOREIGN KEY (to_currency_id) REFERENCES currencies(id) ON DELETE RESTRICT,
    INDEX idx_currencies (from_currency_id, to_currency_id),
    INDEX idx_active (is_active, valid_from, valid_until),
    INDEX idx_valid_dates (valid_from, valid_until),
    UNIQUE KEY unique_active_rate (from_currency_id, to_currency_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    
    -- User preferences
    preferred_currency_id INT UNSIGNED NULL COMMENT 'User preferred currency for display',
    preferred_language VARCHAR(10) DEFAULT 'en' COMMENT 'Language code (en, vi, etc.)',
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    
    FOREIGN KEY (preferred_currency_id) REFERENCES currencies(id) ON DELETE SET NULL,
    INDEX idx_email (email),
    INDEX idx_oauth (oauth_provider, oauth_id),
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_preferred_currency (preferred_currency_id)
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
    
    -- Pricing (base price for reference, actual prices in package_prices table)
    base_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Base price in USD for reference',
    base_currency_id INT UNSIGNED NOT NULL COMMENT 'Base currency (usually USD)',
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
    
    FOREIGN KEY (base_currency_id) REFERENCES currencies(id) ON DELETE RESTRICT,
    INDEX idx_slug (slug),
    INDEX idx_active (is_active),
    INDEX idx_base_currency (base_currency_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PACKAGE PRICES TABLE
-- ==============================================================================
-- Description: Multi-currency pricing for packages
CREATE TABLE package_prices (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT UNSIGNED NOT NULL,
    currency_id INT UNSIGNED NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    
    -- Optional promotional pricing
    promotional_price DECIMAL(10, 2) NULL COMMENT 'Discounted price if promotion active',
    promotion_start_date TIMESTAMP NULL,
    promotion_end_date TIMESTAMP NULL,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE CASCADE,
    FOREIGN KEY (currency_id) REFERENCES currencies(id) ON DELETE RESTRICT,
    UNIQUE KEY unique_package_currency (package_id, currency_id),
    INDEX idx_package_currency (package_id, currency_id),
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
    currency_id INT UNSIGNED NOT NULL COMMENT 'Currency used for payment',
    original_amount DECIMAL(10, 2) NULL COMMENT 'Original amount if currency conversion occurred',
    original_currency_id INT UNSIGNED NULL COMMENT 'Original currency before conversion',
    exchange_rate DECIMAL(20, 8) NULL COMMENT 'Exchange rate used at time of payment',
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
    FOREIGN KEY (currency_id) REFERENCES currencies(id) ON DELETE RESTRICT,
    FOREIGN KEY (original_currency_id) REFERENCES currencies(id) ON DELETE RESTRICT,
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_status (payment_status),
    INDEX idx_created_at (created_at),
    INDEX idx_currency (currency_id)
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

-- Insert supported currencies
INSERT INTO currencies (code, name, symbol, decimal_places, is_active, display_order) VALUES
('USD', 'US Dollar', '$', 2, TRUE, 1),
('EUR', 'Euro', '€', 2, TRUE, 2),
('GBP', 'British Pound', '£', 2, TRUE, 3),
('JPY', 'Japanese Yen', '¥', 0, TRUE, 4),
('VND', 'Vietnamese Dong', '₫', 0, TRUE, 5),
('SGD', 'Singapore Dollar', 'S$', 2, TRUE, 6),
('AUD', 'Australian Dollar', 'A$', 2, TRUE, 7),
('CAD', 'Canadian Dollar', 'C$', 2, TRUE, 8),
('CNY', 'Chinese Yuan', '¥', 2, TRUE, 9),
('INR', 'Indian Rupee', '₹', 2, TRUE, 10),
('THB', 'Thai Baht', '฿', 2, TRUE, 11),
('MYR', 'Malaysian Ringgit', 'RM', 2, TRUE, 12);

-- Insert exchange rates (base: USD)
-- Note: These are example rates. In production, update regularly from API
INSERT INTO exchange_rates (from_currency_id, to_currency_id, rate, source, is_active) VALUES
-- USD to other currencies
(1, 1, 1.00000000, 'base', TRUE),          -- USD to USD
(1, 2, 0.92000000, 'manual', TRUE),        -- USD to EUR
(1, 3, 0.79000000, 'manual', TRUE),        -- USD to GBP
(1, 4, 149.50000000, 'manual', TRUE),      -- USD to JPY
(1, 5, 24500.00000000, 'manual', TRUE),    -- USD to VND
(1, 6, 1.35000000, 'manual', TRUE),        -- USD to SGD
(1, 7, 1.52000000, 'manual', TRUE),        -- USD to AUD
(1, 8, 1.37000000, 'manual', TRUE),        -- USD to CAD
(1, 9, 7.24000000, 'manual', TRUE),        -- USD to CNY
(1, 10, 83.12000000, 'manual', TRUE),      -- USD to INR
(1, 11, 35.75000000, 'manual', TRUE),      -- USD to THB
(1, 12, 4.72000000, 'manual', TRUE);       -- USD to MYR

-- Insert default admin user (password: Admin@123 - hash generated with bcrypt)
-- Set preferred currency to USD
INSERT INTO users (email, password_hash, full_name, role, email_verified, email_verified_at, preferred_currency_id) VALUES
('admin@mindmap.com', '$2b$10$rGHcY6Y6KdGj5pXZOXK4xOGQjqZ.x5m3tV8HxNQXJNc9LYvL6LY8i', 'System Administrator', 'admin', TRUE, NOW(), 1);

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
INSERT INTO packages (name, description, slug, base_price, base_currency_id, duration_days, max_mindmaps, max_collaborators, max_storage_mb, features, is_active, display_order) VALUES
-- Free Plan
('Free', 'Perfect for getting started', 'free', 0.00, 1, 365, 3, 2, 50, 
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
('Pro', 'For professionals and small teams', 'pro', 9.99, 1, 30, 50, 10, 500,
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
('Enterprise', 'For large organizations', 'enterprise', 49.99, 1, 30, 0, 0, 0,
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

-- Insert package prices in multiple currencies
-- Free package (ID: 1) - always 0 in all currencies
INSERT INTO package_prices (package_id, currency_id, price, is_active) VALUES
(1, 1, 0.00, TRUE),    -- USD
(1, 2, 0.00, TRUE),    -- EUR
(1, 3, 0.00, TRUE),    -- GBP
(1, 4, 0.00, TRUE),    -- JPY
(1, 5, 0.00, TRUE),    -- VND
(1, 6, 0.00, TRUE),    -- SGD
(1, 7, 0.00, TRUE),    -- AUD
(1, 8, 0.00, TRUE),    -- CAD
(1, 9, 0.00, TRUE),    -- CNY
(1, 10, 0.00, TRUE),   -- INR
(1, 11, 0.00, TRUE),   -- THB
(1, 12, 0.00, TRUE);   -- MYR

-- Pro package (ID: 2) - $9.99 base price
INSERT INTO package_prices (package_id, currency_id, price, is_active) VALUES
(2, 1, 9.99, TRUE),      -- USD
(2, 2, 9.19, TRUE),      -- EUR (≈ $9.99)
(2, 3, 7.89, TRUE),      -- GBP
(2, 4, 1490, TRUE),      -- JPY (no decimals)
(2, 5, 245000, TRUE),    -- VND (no decimals)
(2, 6, 13.49, TRUE),     -- SGD
(2, 7, 15.18, TRUE),     -- AUD
(2, 8, 13.68, TRUE),     -- CAD
(2, 9, 72.33, TRUE),     -- CNY
(2, 10, 830, TRUE),      -- INR
(2, 11, 357, TRUE),      -- THB
(2, 12, 47.15, TRUE);    -- MYR

-- Enterprise package (ID: 3) - $49.99 base price
INSERT INTO package_prices (package_id, currency_id, price, is_active) VALUES
(3, 1, 49.99, TRUE),     -- USD
(3, 2, 45.99, TRUE),     -- EUR
(3, 3, 39.49, TRUE),     -- GBP
(3, 4, 7470, TRUE),      -- JPY
(3, 5, 1225000, TRUE),   -- VND
(3, 6, 67.48, TRUE),     -- SGD
(3, 7, 75.98, TRUE),     -- AUD
(3, 8, 68.48, TRUE),     -- CAD
(3, 9, 362, TRUE),       -- CNY
(3, 10, 4155, TRUE),     -- INR
(3, 11, 1787, TRUE),     -- THB
(3, 12, 235.95, TRUE);   -- MYR

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
    c.code AS currency_code,
    c.symbol AS currency_symbol,
    p.original_amount,
    oc.code AS original_currency_code,
    p.exchange_rate,
    p.payment_method,
    p.payment_status,
    p.transaction_id,
    p.created_at,
    p.completed_at
FROM payments p
INNER JOIN users u ON p.user_id = u.id
INNER JOIN packages pkg ON p.package_id = pkg.id
INNER JOIN currencies c ON p.currency_id = c.id
LEFT JOIN currencies oc ON p.original_currency_id = oc.id;

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

-- Procedure: Convert amount between currencies
CREATE PROCEDURE sp_convert_currency(
    IN p_amount DECIMAL(10, 2),
    IN p_from_currency_id INT UNSIGNED,
    IN p_to_currency_id INT UNSIGNED,
    OUT p_converted_amount DECIMAL(10, 2),
    OUT p_exchange_rate DECIMAL(20, 8)
)
BEGIN
    DECLARE v_rate DECIMAL(20, 8);
    
    -- If same currency, no conversion needed
    IF p_from_currency_id = p_to_currency_id THEN
        SET p_converted_amount = p_amount;
        SET p_exchange_rate = 1.00000000;
    ELSE
        -- Get exchange rate
        SELECT rate INTO v_rate
        FROM exchange_rates
        WHERE from_currency_id = p_from_currency_id
        AND to_currency_id = p_to_currency_id
        AND is_active = TRUE
        AND (valid_until IS NULL OR valid_until > NOW())
        ORDER BY valid_from DESC
        LIMIT 1;
        
        IF v_rate IS NOT NULL THEN
            SET p_converted_amount = p_amount * v_rate;
            SET p_exchange_rate = v_rate;
        ELSE
            -- Try reverse conversion (to -> from) and invert
            SELECT 1 / rate INTO v_rate
            FROM exchange_rates
            WHERE from_currency_id = p_to_currency_id
            AND to_currency_id = p_from_currency_id
            AND is_active = TRUE
            AND (valid_until IS NULL OR valid_until > NOW())
            ORDER BY valid_from DESC
            LIMIT 1;
            
            IF v_rate IS NOT NULL THEN
                SET p_converted_amount = p_amount * v_rate;
                SET p_exchange_rate = v_rate;
            ELSE
                -- No exchange rate found, return NULL
                SET p_converted_amount = NULL;
                SET p_exchange_rate = NULL;
            END IF;
        END IF;
    END IF;
END //

-- Procedure: Get package price in specific currency
CREATE PROCEDURE sp_get_package_price(
    IN p_package_id BIGINT UNSIGNED,
    IN p_currency_id INT UNSIGNED,
    OUT p_price DECIMAL(10, 2),
    OUT p_promotional_price DECIMAL(10, 2),
    OUT p_has_promotion BOOLEAN
)
BEGIN
    DECLARE v_promo_start TIMESTAMP;
    DECLARE v_promo_end TIMESTAMP;
    DECLARE v_promo_price DECIMAL(10, 2);
    
    -- Get price for specific currency
    SELECT 
        price,
        promotional_price,
        promotion_start_date,
        promotion_end_date
    INTO 
        p_price,
        v_promo_price,
        v_promo_start,
        v_promo_end
    FROM package_prices
    WHERE package_id = p_package_id
    AND currency_id = p_currency_id
    AND is_active = TRUE
    LIMIT 1;
    
    -- Check if promotion is active
    IF v_promo_price IS NOT NULL 
       AND (v_promo_start IS NULL OR v_promo_start <= NOW())
       AND (v_promo_end IS NULL OR v_promo_end >= NOW()) THEN
        SET p_promotional_price = v_promo_price;
        SET p_has_promotion = TRUE;
    ELSE
        SET p_promotional_price = NULL;
        SET p_has_promotion = FALSE;
    END IF;
END //

-- Procedure: Get active exchange rate between currencies
CREATE PROCEDURE sp_get_exchange_rate(
    IN p_from_currency_id INT UNSIGNED,
    IN p_to_currency_id INT UNSIGNED,
    OUT p_rate DECIMAL(20, 8)
)
BEGIN
    -- Get direct exchange rate
    SELECT rate INTO p_rate
    FROM exchange_rates
    WHERE from_currency_id = p_from_currency_id
    AND to_currency_id = p_to_currency_id
    AND is_active = TRUE
    AND (valid_until IS NULL OR valid_until > NOW())
    ORDER BY valid_from DESC
    LIMIT 1;
    
    -- If no direct rate, try reverse and invert
    IF p_rate IS NULL THEN
        SELECT 1 / rate INTO p_rate
        FROM exchange_rates
        WHERE from_currency_id = p_to_currency_id
        AND to_currency_id = p_from_currency_id
        AND is_active = TRUE
        AND (valid_until IS NULL OR valid_until > NOW())
        ORDER BY valid_from DESC
        LIMIT 1;
    END IF;
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

