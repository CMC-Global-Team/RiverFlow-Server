-- ==============================================================================
-- FILE: V004__add_support_tickets.sql
-- ==============================================================================
-- Support Ticket System for RiverFlow
-- Description: Tables for support tickets, messages, and attachments
-- ==============================================================================

-- ==============================================================================
-- SUPPORT TICKETS TABLE
-- ==============================================================================
-- Description: Main support ticket table
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticket_number VARCHAR(20) NOT NULL UNIQUE COMMENT 'Unique ticket identifier (e.g., TKT-20231213-001)',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    
    -- Classification
    category ENUM('BUG', 'TECHNICAL_SUPPORT', 'BILLING', 'FEEDBACK', 'OTHER') NOT NULL DEFAULT 'OTHER',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
    status ENUM('NEW', 'IN_PROGRESS', 'ON_HOLD', 'WAITING_FOR_RESPONSE', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'NEW',
    
    -- Assignment
    assigned_to BIGINT NULL COMMENT 'Admin/Support staff user ID',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_ticket_number (ticket_number),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_category (category),
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- SUPPORT TICKET MESSAGES TABLE
-- ==============================================================================
-- Description: Messages/replies in support ticket thread
CREATE TABLE IF NOT EXISTS support_ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    is_internal_note BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'If true, only visible to admin/support staff',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_created_at (created_at),
    INDEX idx_internal_note (is_internal_note)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- SUPPORT TICKET ATTACHMENTS TABLE
-- ==============================================================================
-- Description: File attachments for support ticket messages
CREATE TABLE IF NOT EXISTS support_ticket_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_data LONGBLOB NOT NULL COMMENT 'Binary file data (max 10MB)',
    file_size BIGINT NOT NULL COMMENT 'File size in bytes',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (message_id) REFERENCES support_ticket_messages(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- END OF MIGRATION
-- ==============================================================================

