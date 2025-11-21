ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role ENUM('admin','user') NOT NULL DEFAULT 'user',
    ADD COLUMN IF NOT EXISTS credit BIGINT UNSIGNED NOT NULL DEFAULT 0,
    ADD INDEX idx_role (role);

CREATE TABLE IF NOT EXISTS credit_topup_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    amount BIGINT UNSIGNED NOT NULL,
    status ENUM('pending','paid','expired','cancelled') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    INDEX idx_code (code),
    INDEX idx_user_status (user_id, status),
    CONSTRAINT fk_topup_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
