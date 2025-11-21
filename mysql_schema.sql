-- ==============================================================================
-- FILE: mysql_schema.sql
-- ==============================================================================
-- MySQL Database Schema for Mindmap Online Real-time System
-- Version: 2.0 (Optimized)
-- Description: User management, authentication, AI workflows
-- ==============================================================================

-- Create database
CREATE DATABASE IF NOT EXISTS mindmap_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

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
    avatar VARCHAR(500) NULL COMMENT 'URL to user avatar (deprecated - use avatar_data instead)',
    avatar_data LONGBLOB NULL COMMENT 'Avatar image binary data (BLOB)',
    avatar_mime_type VARCHAR(50) NULL COMMENT 'MIME type of avatar image (e.g., image/png, image/jpeg)',
    status ENUM('active', 'suspended', 'deleted') NOT NULL DEFAULT 'active',
    role ENUM('admin', 'user') NOT NULL DEFAULT 'user',
    
    -- OAuth fields
    oauth_provider ENUM('email', 'google', 'github', 'facebook') NOT NULL DEFAULT 'email',
    oauth_id VARCHAR(255) NULL COMMENT 'ID from OAuth provider',
    
    -- Email verification
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMP NULL,
    
    -- User preferences
    preferred_language VARCHAR(10) DEFAULT 'en' COMMENT 'Language code (en, vi, etc.)',
    timezone VARCHAR(50) DEFAULT 'UTC',
    theme ENUM('light', 'dark', 'auto') DEFAULT 'light',
    credit BIGINT UNSIGNED NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    
    INDEX idx_email (email),
    INDEX idx_oauth (oauth_provider, oauth_id),
    INDEX idx_status (status),
    INDEX idx_role (role),
    INDEX idx_created_at (created_at),
    INDEX idx_avatar_mime_type (avatar_mime_type)
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
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
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
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
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
    INDEX idx_expires (expires_at),
    INDEX idx_revoked (is_revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- AI WORKFLOW CATEGORIES TABLE
-- ==============================================================================
-- Description: Categories for AI workflows
CREATE TABLE ai_workflow_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NULL,
    icon VARCHAR(100) NULL COMMENT 'Icon name or emoji',
    color VARCHAR(7) NULL COMMENT 'Hex color code',
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_slug (slug),
    INDEX idx_active (is_active),
    INDEX idx_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- AI WORKFLOWS TABLE
-- ==============================================================================
-- Description: Store AI workflow templates (50 workflows for employee development)
CREATE TABLE ai_workflows (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT UNSIGNED NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NULL,
    
    -- Workflow configuration
    prompt_template TEXT NOT NULL COMMENT 'AI prompt template with variables',
    input_schema JSON NULL COMMENT 'Schema for required inputs: {"field": "type"}',
    output_format ENUM('text', 'json', 'mindmap', 'list') DEFAULT 'mindmap',
    
    -- Metadata
    tags JSON NULL COMMENT 'Array of tags for search',
    difficulty_level ENUM('beginner', 'intermediate', 'advanced') DEFAULT 'beginner',
    estimated_time INT NULL COMMENT 'Estimated time in minutes',
    
    -- Usage tracking
    usage_count BIGINT UNSIGNED DEFAULT 0,
    rating_average DECIMAL(3, 2) DEFAULT 0.00 COMMENT 'Average rating 0-5',
    rating_count INT UNSIGNED DEFAULT 0,
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (category_id) REFERENCES ai_workflow_categories(id) ON DELETE SET NULL,
    INDEX idx_slug (slug),
    INDEX idx_category (category_id),
    INDEX idx_active (is_active),
    INDEX idx_featured (is_featured),
    INDEX idx_usage (usage_count),
    INDEX idx_rating (rating_average)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- USER WORKFLOW HISTORY TABLE
-- ==============================================================================
-- Description: Track user's AI workflow usage
CREATE TABLE user_workflow_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    workflow_id BIGINT UNSIGNED NOT NULL,
    
    -- Input/Output
    input_data JSON NULL COMMENT 'User inputs for the workflow',
    output_data JSON NULL COMMENT 'Generated output',
    
    -- Performance
    execution_time_ms INT NULL COMMENT 'Execution time in milliseconds',
    token_count INT NULL COMMENT 'AI tokens used',
    
    -- Feedback
    rating TINYINT NULL COMMENT 'User rating 1-5',
    feedback TEXT NULL,
    
    -- Associated mindmap
    mindmap_id VARCHAR(50) NULL COMMENT 'MongoDB mindmap ID if created',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (workflow_id) REFERENCES ai_workflows(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_created_at (created_at),
    INDEX idx_mindmap_id (mindmap_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- SAAS PLATFORM INTEGRATIONS TABLE
-- ==============================================================================
-- Description: Store SaaS platform integration configurations
CREATE TABLE saas_integrations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    platform_name VARCHAR(100) NOT NULL COMMENT 'e.g., Slack, Teams, Notion, etc.',
    
    -- Integration config
    config JSON NULL COMMENT 'Platform-specific configuration',
    access_token TEXT NULL COMMENT 'Encrypted access token',
    refresh_token TEXT NULL COMMENT 'Encrypted refresh token',
    token_expires_at TIMESTAMP NULL,
    
    -- Status
    status ENUM('active', 'inactive', 'error') DEFAULT 'active',
    last_sync_at TIMESTAMP NULL,
    error_message TEXT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_platform (platform_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- USER ACTIVITIES TABLE
-- ==============================================================================
-- Description: Track user activities for analytics and audit
CREATE TABLE user_activities (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    activity_type VARCHAR(100) NOT NULL COMMENT 'e.g., login, logout, mindmap.create',
    entity_type VARCHAR(50) NULL COMMENT 'e.g., mindmap, workflow',
    entity_id VARCHAR(100) NULL COMMENT 'ID of the entity (can be MongoDB ID)',
    
    -- Request info
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    
    -- Details
    details JSON NULL COMMENT 'Additional context about the activity',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_activity_type (activity_type),
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
    setting_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON') NOT NULL DEFAULT 'STRING',
    description TEXT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Can be accessed by frontend',
    updated_by BIGINT UNSIGNED NULL COMMENT 'User who last updated this setting',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_setting_key (setting_key),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- NOTIFICATIONS TABLE
-- ==============================================================================
-- Description: Store user notifications
CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    type VARCHAR(100) NOT NULL COMMENT 'e.g., collaboration_invite, comment_mention',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    
    -- Related entity
    entity_type VARCHAR(50) NULL COMMENT 'e.g., mindmap, comment',
    entity_id VARCHAR(100) NULL,
    
    -- Actions
    action_url VARCHAR(500) NULL COMMENT 'URL to navigate when clicked',
    action_label VARCHAR(100) NULL,
    
    -- Status
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- INITIAL DATA
-- ==============================================================================

-- Insert AI Workflow Categories
INSERT INTO ai_workflow_categories (name, slug, description, icon, color, display_order) VALUES
('Phát triển kỹ năng', 'skill-development', 'Workflows về phát triển kỹ năng cá nhân', '🎯', '#4A90E2', 1),
('Quản lý thời gian', 'time-management', 'Workflows về quản lý thời gian hiệu quả', '⏰', '#F5A623', 2),
('Lãnh đạo & Quản lý', 'leadership-management', 'Workflows về kỹ năng lãnh đạo', '👔', '#7B61FF', 3),
('Giao tiếp & Hợp tác', 'communication-collaboration', 'Workflows về giao tiếp và làm việc nhóm', '💬', '#50E3C2', 4),
('Sáng tạo & Đổi mới', 'creativity-innovation', 'Workflows về tư duy sáng tạo', '💡', '#F8E71C', 5),
('Sức khỏe & Cân bằng', 'health-balance', 'Workflows về sức khỏe và work-life balance', '🧘', '#BD10E0', 6),
('Nghề nghiệp & Phát triển', 'career-growth', 'Workflows về phát triển sự nghiệp', '📈', '#B8E986', 7),
('Học tập & Tư duy', 'learning-thinking', 'Workflows về phương pháp học tập', '📚', '#FF6B6B', 8);

-- Insert Sample AI Workflows (một số ví dụ, bạn có thể thêm 50 workflows)
INSERT INTO ai_workflows (category_id, name, slug, description, prompt_template, input_schema, output_format, tags, difficulty_level, estimated_time, is_featured) VALUES
-- Skill Development
(1, 'Lập kế hoạch phát triển kỹ năng', 'skill-development-plan', 'Tạo roadmap phát triển kỹ năng cụ thể cho bản thân', 
'Tạo một mindmap chi tiết về kế hoạch phát triển kỹ năng {{skill_name}} trong {{timeframe}}. Bao gồm: 1) Đánh giá năng lực hiện tại, 2) Mục tiêu cụ thể, 3) Các bước học tập, 4) Tài nguyên cần thiết, 5) Cách đo lường tiến độ.',
'{"skill_name": "string", "timeframe": "string", "current_level": "string"}',
'mindmap', '["kỹ năng", "phát triển", "học tập"]', 'beginner', 15, TRUE),

(1, 'Đánh giá SWOT cá nhân', 'personal-swot-analysis', 'Phân tích điểm mạnh, điểm yếu, cơ hội và thách thức', 
'Tạo mindmap phân tích SWOT cá nhân cho {{job_role}}. Bao gồm: Strengths (điểm mạnh), Weaknesses (điểm yếu), Opportunities (cơ hội), Threats (thách thức). Đưa ra ít nhất 4-5 điểm cho mỗi mục.',
'{"job_role": "string", "industry": "string"}',
'mindmap', '["swot", "tự đánh giá", "phát triển"]', 'intermediate', 20, TRUE),

-- Time Management
(2, 'Ma trận Eisenhower', 'eisenhower-matrix', 'Sắp xếp công việc theo độ ưu tiên', 
'Tạo mindmap Ma trận Eisenhower để phân loại công việc. Chia thành 4 nhóm: 1) Quan trọng & Khẩn cấp, 2) Quan trọng & Không khẩn cấp, 3) Không quan trọng & Khẩn cấp, 4) Không quan trọng & Không khẩn cấp. Gợi ý cách xử lý mỗi nhóm.',
'{"tasks": "array"}',
'mindmap', '["quản lý thời gian", "ưu tiên", "hiệu quả"]', 'beginner', 10, TRUE),

(2, 'Kế hoạch tuần hiệu quả', 'weekly-planning', 'Lập kế hoạch tuần làm việc', 
'Tạo mindmap kế hoạch tuần làm việc cho {{week_goal}}. Bao gồm: Mục tiêu tuần, Phân bổ thời gian theo ngày, Thời gian deep work, Thời gian nghỉ ngơi, Đánh giá cuối tuần.',
'{"week_goal": "string", "work_hours_per_day": "number"}',
'mindmap', '["kế hoạch", "tuần", "năng suất"]', 'beginner', 15, FALSE),

-- Leadership
(3, 'Kỹ năng lãnh đạo 360°', '360-leadership-skills', 'Phát triển kỹ năng lãnh đạo toàn diện', 
'Tạo mindmap về kỹ năng lãnh đạo 360° bao gồm: Self-leadership (tự lãnh đạo), Leading up (lãnh đạo cấp trên), Leading across (lãnh đạo đồng nghiệp), Leading down (lãnh đạo cấp dưới). Chi tiết các kỹ năng cần thiết cho từng hướng.',
'{"leadership_level": "string", "team_size": "number"}',
'mindmap', '["lãnh đạo", "quản lý", "kỹ năng"]', 'advanced', 25, TRUE),

-- Communication
(4, 'Kỹ năng trình bày hiệu quả', 'effective-presentation', 'Cải thiện kỹ năng thuyết trình', 
'Tạo mindmap về kỹ năng trình bày cho chủ đề {{presentation_topic}}. Bao gồm: Chuẩn bị nội dung, Cấu trúc bài thuyết trình, Kỹ thuật truyền đạt, Xử lý câu hỏi, Ngôn ngữ cơ thể.',
'{"presentation_topic": "string", "audience_type": "string", "duration_minutes": "number"}',
'mindmap', '["thuyết trình", "giao tiếp", "kỹ năng mềm"]', 'intermediate', 20, FALSE),

-- Creativity
(5, 'Tư duy sáng tạo Design Thinking', 'design-thinking-process', 'Áp dụng quy trình Design Thinking', 
'Tạo mindmap quy trình Design Thinking cho vấn đề {{problem_statement}}. Bao gồm 5 giai đoạn: Empathize (đồng cảm), Define (định nghĩa), Ideate (ý tưởng), Prototype (nguyên mẫu), Test (thử nghiệm).',
'{"problem_statement": "string", "target_users": "string"}',
'mindmap', '["sáng tạo", "design thinking", "đổi mới"]', 'advanced', 30, TRUE),

-- Health & Balance
(6, 'Work-Life Balance', 'work-life-balance', 'Cân bằng công việc và cuộc sống', 
'Tạo mindmap về cân bằng công việc và cuộc sống. Bao gồm: Thiết lập ranh giới, Quản lý năng lượng, Chăm sóc sức khỏe, Thời gian gia đình, Sở thích cá nhân, Thiền và mindfulness.',
'{"current_situation": "string", "goals": "string"}',
'mindmap', '["cân bằng", "sức khỏe", "hạnh phúc"]', 'beginner', 15, FALSE),

-- Career Growth
(7, 'Lộ trình sự nghiệp 5 năm', '5-year-career-roadmap', 'Vạch ra lộ trình phát triển sự nghiệp', 
'Tạo mindmap lộ trình sự nghiệp 5 năm từ vị trí {{current_position}} đến {{target_position}}. Bao gồm: Năm 1-5 với mục tiêu cụ thể, Kỹ năng cần học, Kinh nghiệm cần tích lũy, Mạng lưới quan hệ, Chứng chỉ/Bằng cấp.',
'{"current_position": "string", "target_position": "string", "industry": "string"}',
'mindmap', '["sự nghiệp", "phát triển", "kế hoạch"]', 'intermediate', 25, TRUE),

-- Learning
(8, 'Phương pháp học Feynman', 'feynman-learning-technique', 'Học hiệu quả với kỹ thuật Feynman', 
'Tạo mindmap áp dụng phương pháp học Feynman cho chủ đề {{learning_topic}}. Bao gồm: 1) Chọn khái niệm, 2) Giải thích đơn giản, 3) Xác định khoảng trống kiến thức, 4) Đơn giản hóa và sử dụng ẩn dụ.',
'{"learning_topic": "string", "difficulty_level": "string"}',
'mindmap', '["học tập", "phương pháp", "hiệu quả"]', 'intermediate', 20, FALSE);

-- Insert system settings
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public) VALUES
('site_name', 'RiverFlow Mindmap', 'STRING', 'Website name', TRUE),
('max_upload_size_mb', '10', 'NUMBER', 'Maximum file upload size in MB', FALSE),
('email_verification_required', 'true', 'BOOLEAN', 'Require email verification for new users', FALSE),
('maintenance_mode', 'false', 'BOOLEAN', 'Enable maintenance mode', TRUE),
('max_mindmaps_per_user', '100', 'NUMBER', 'Maximum mindmaps per user (0 = unlimited)', FALSE),
('max_collaborators_per_mindmap', '10', 'NUMBER', 'Maximum collaborators per mindmap', FALSE),
('enable_ai_features', 'true', 'BOOLEAN', 'Enable AI workflow features', TRUE),
('ai_daily_limit_per_user', '20', 'NUMBER', 'Daily AI workflow usage limit per user', FALSE);

-- ==============================================================================
-- VIEWS FOR COMMON QUERIES
-- ==============================================================================

-- View: User summary
CREATE VIEW v_user_summary AS
SELECT 
    u.id,
    u.email,
    u.full_name,
    u.avatar,
    u.status,
    u.role,
    u.credit,
    u.oauth_provider,
    u.email_verified,
    u.preferred_language,
    u.timezone,
    u.theme,
    u.last_login_at,
    u.created_at
FROM users u
WHERE u.status = 'active';

-- View: AI Workflow usage statistics
CREATE VIEW v_workflow_stats AS
SELECT 
    w.id AS workflow_id,
    w.name AS workflow_name,
    w.slug,
    c.name AS category_name,
    w.usage_count,
    w.rating_average,
    w.rating_count,
    w.difficulty_level,
    w.is_featured,
    COUNT(DISTINCT uwh.user_id) AS unique_users,
    AVG(uwh.execution_time_ms) AS avg_execution_time_ms
FROM ai_workflows w
LEFT JOIN ai_workflow_categories c ON w.category_id = c.id
LEFT JOIN user_workflow_history uwh ON w.id = uwh.workflow_id
WHERE w.is_active = TRUE
GROUP BY w.id, w.name, w.slug, c.name, w.usage_count, w.rating_average, w.rating_count, w.difficulty_level, w.is_featured;

-- ==============================================================================
-- STORED PROCEDURES
-- ==============================================================================

DELIMITER //

-- Procedure: Get user's unread notification count
CREATE PROCEDURE sp_get_unread_notification_count(
    IN p_user_id BIGINT UNSIGNED,
    OUT p_count INT
)
BEGIN
    SELECT COUNT(*) INTO p_count
    FROM notifications
    WHERE user_id = p_user_id
    AND is_read = FALSE;
END //

-- Procedure: Mark all notifications as read
CREATE PROCEDURE sp_mark_all_notifications_read(
    IN p_user_id BIGINT UNSIGNED
)
BEGIN
    UPDATE notifications
    SET is_read = TRUE, read_at = NOW()
    WHERE user_id = p_user_id
    AND is_read = FALSE;
END //

-- Procedure: Get popular AI workflows
CREATE PROCEDURE sp_get_popular_workflows(
    IN p_limit INT,
    IN p_category_id BIGINT UNSIGNED
)
BEGIN
    IF p_category_id IS NULL THEN
        SELECT * FROM ai_workflows
        WHERE is_active = TRUE
        ORDER BY usage_count DESC, rating_average DESC
        LIMIT p_limit;
    ELSE
        SELECT * FROM ai_workflows
        WHERE is_active = TRUE
        AND category_id = p_category_id
        ORDER BY usage_count DESC, rating_average DESC
        LIMIT p_limit;
    END IF;
END //

-- Procedure: Record AI workflow usage
CREATE PROCEDURE sp_record_workflow_usage(
    IN p_user_id BIGINT UNSIGNED,
    IN p_workflow_id BIGINT UNSIGNED,
    IN p_input_data JSON,
    IN p_output_data JSON,
    IN p_execution_time_ms INT,
    IN p_mindmap_id VARCHAR(50)
)
BEGIN
    -- Insert usage record
    INSERT INTO user_workflow_history (
        user_id, workflow_id, input_data, output_data, 
        execution_time_ms, mindmap_id
    ) VALUES (
        p_user_id, p_workflow_id, p_input_data, p_output_data,
        p_execution_time_ms, p_mindmap_id
    );
    
    -- Update workflow usage count
    UPDATE ai_workflows
    SET usage_count = usage_count + 1
    WHERE id = p_workflow_id;
END //

-- Procedure: Rate AI workflow
CREATE PROCEDURE sp_rate_workflow(
    IN p_history_id BIGINT UNSIGNED,
    IN p_rating TINYINT,
    IN p_feedback TEXT
)
BEGIN
    DECLARE v_workflow_id BIGINT UNSIGNED;
    
    -- Update the history record
    UPDATE user_workflow_history
    SET rating = p_rating, feedback = p_feedback
    WHERE id = p_history_id;
    
    -- Get workflow_id
    SELECT workflow_id INTO v_workflow_id
    FROM user_workflow_history
    WHERE id = p_history_id;
    
    -- Recalculate workflow rating
    UPDATE ai_workflows w
    SET 
        rating_count = (
            SELECT COUNT(*) 
            FROM user_workflow_history 
            WHERE workflow_id = v_workflow_id AND rating IS NOT NULL
        ),
        rating_average = (
            SELECT AVG(rating) 
            FROM user_workflow_history 
            WHERE workflow_id = v_workflow_id AND rating IS NOT NULL
        )
    WHERE w.id = v_workflow_id;
END //

DELIMITER ;

-- ==============================================================================
-- INDEXES FOR PERFORMANCE
-- ==============================================================================

-- Additional indexes for better query performance
CREATE INDEX idx_users_last_login ON users(last_login_at);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_workflow_history_created ON user_workflow_history(created_at);

-- ==============================================================================
-- END OF SCHEMA
-- ==============================================================================
