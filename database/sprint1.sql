-- Sprint 1 schema: authentication, authorization and user administration only.
-- No product, order, inventory or other post-Sprint-1 business tables are created.

USE sales_warehouse;

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = '+07:00';

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NULL,
    password_hash VARCHAR(100) NOT NULL,
    password_must_change BOOLEAN NOT NULL DEFAULT TRUE,
    status ENUM('ACTIVE', 'LOCKED', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    lock_reason VARCHAR(500) NULL,
    locked_at DATETIME(6) NULL,
    locked_by_user_id BIGINT UNSIGNED NULL,
    password_changed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_full_name (full_name),
    KEY idx_users_phone (phone),
    KEY idx_users_status (status),
    KEY idx_users_locked_by (locked_by_user_id),
    CONSTRAINT fk_users_locked_by
        FOREIGN KEY (locked_by_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT chk_users_username_length
        CHECK (CHAR_LENGTH(username) BETWEEN 3 AND 50),
    CONSTRAINT chk_users_password_hash
        CHECK (CHAR_LENGTH(password_hash) >= 59),
    CONSTRAINT chk_users_lock_reason
        CHECK (
            (status = 'LOCKED' AND lock_reason IS NOT NULL AND CHAR_LENGTH(TRIM(lock_reason)) > 0)
            OR (status <> 'LOCKED' AND lock_reason IS NULL)
        )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS roles (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS permissions (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Absence of a row means denied. Backend authorization must default to deny.
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id SMALLINT UNSIGNED NOT NULL,
    permission_id SMALLINT UNSIGNED NOT NULL,
    granted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    granted_by_user_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permissions_permission (permission_id),
    KEY idx_role_permissions_granted_by (granted_by_user_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_granted_by
        FOREIGN KEY (granted_by_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id SMALLINT UNSIGNED NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by_user_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_roles_role (role_id),
    KEY idx_user_roles_assigned_by (assigned_by_user_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS warehouses (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouses_code (code),
    KEY idx_warehouses_name (name)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS territories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_territories_code (code),
    KEY idx_territories_name (name)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Backend must assign at least one warehouse before assigning a warehouse role.
CREATE TABLE IF NOT EXISTS user_warehouses (
    user_id BIGINT UNSIGNED NOT NULL,
    warehouse_id BIGINT UNSIGNED NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by_user_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (user_id, warehouse_id),
    KEY idx_user_warehouses_warehouse (warehouse_id),
    KEY idx_user_warehouses_assigned_by (assigned_by_user_id),
    CONSTRAINT fk_user_warehouses_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_user_warehouses_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_user_warehouses_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_territories (
    user_id BIGINT UNSIGNED NOT NULL,
    territory_id BIGINT UNSIGNED NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by_user_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (user_id, territory_id),
    KEY idx_user_territories_territory (territory_id),
    KEY idx_user_territories_assigned_by (assigned_by_user_id),
    CONSTRAINT fk_user_territories_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_user_territories_territory
        FOREIGN KEY (territory_id) REFERENCES territories (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_user_territories_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Tracks the consecutive failure counter and the 15-minute temporary lock.
CREATE TABLE IF NOT EXISTS user_login_security (
    user_id BIGINT UNSIGNED NOT NULL,
    consecutive_failed_attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    last_failed_at DATETIME(6) NULL,
    temporary_locked_until DATETIME(6) NULL,
    last_successful_login_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    KEY idx_login_security_locked_until (temporary_locked_until),
    CONSTRAINT fk_login_security_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT chk_login_security_attempts
        CHECK (consecutive_failed_attempts <= 5)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Only a SHA-256 (or stronger) token hash is stored, never the emailed token.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    requested_ip VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_token_hash (token_hash),
    KEY idx_password_reset_user_state (user_id, used_at, expires_at),
    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT chk_password_reset_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_password_reset_used_at
        CHECK (used_at IS NULL OR used_at >= created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Logout, password change and account locking revoke sessions by setting revoked_at.
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    session_token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_accessed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    revoke_reason VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sessions_token_hash (session_token_hash),
    KEY idx_user_sessions_user_state (user_id, revoked_at, expires_at),
    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT chk_user_sessions_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_user_sessions_revocation
        CHECK (
            (revoked_at IS NULL AND revoke_reason IS NULL)
            OR (revoked_at IS NOT NULL AND revoke_reason IS NOT NULL AND CHAR_LENGTH(TRIM(revoke_reason)) > 0)
        )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT UNSIGNED NULL,
    target_user_id BIGINT UNSIGNED NULL,
    action_code VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    details JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_actor_created (actor_user_id, created_at),
    KEY idx_audit_target_created (target_user_id, created_at),
    KEY idx_audit_action_created (action_code, created_at),
    CONSTRAINT fk_audit_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT fk_audit_target
        FOREIGN KEY (target_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT IGNORE INTO roles (code, name, description)
VALUES
    ('ADMIN', 'Administrator', 'Quản trị tài khoản, vai trò và trạng thái người dùng.'),
    ('SALES_REP', 'Sales Representative', 'Nhân viên kinh doanh.'),
    ('SALES_MANAGER', 'Sales Manager', 'Quản lý kinh doanh; vai trò duy nhất được xem giá vốn và biên lợi nhuận.'),
    ('WAREHOUSE_STAFF', 'Warehouse Staff', 'Nhân viên kho; phải được gắn ít nhất một kho.'),
    ('WAREHOUSE_MANAGER', 'Warehouse Manager', 'Quản lý kho; phải được gắn ít nhất một kho.'),
    ('ACCOUNTANT', 'Accountant', 'Kế toán.'),
    ('DEALER', 'Dealer', 'Đại lý.');

INSERT IGNORE INTO permissions (code, name, description)
VALUES
    ('USER_VIEW', 'View users', 'Xem, tìm kiếm, lọc và phân trang danh sách người dùng.'),
    ('USER_CREATE', 'Create users', 'Tạo tài khoản với mật khẩu tạm.'),
    ('USER_UPDATE', 'Update users', 'Cập nhật thông tin tài khoản.'),
    ('USER_LOCK', 'Lock or unlock users', 'Khóa/mở khóa tài khoản và thu hồi session.'),
    ('USER_ROLE_MANAGE', 'Manage user roles', 'Gán hoặc gỡ vai trò của người dùng.'),
    ('COST_MARGIN_VIEW', 'View cost and margin', 'Xem giá vốn và biên lợi nhuận.');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'USER_VIEW',
    'USER_CREATE',
    'USER_UPDATE',
    'USER_LOCK',
    'USER_ROLE_MANAGE'
)
WHERE r.code = 'ADMIN';

-- Deliberately granted only to SALES_MANAGER, including exclusion of ADMIN.
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'COST_MARGIN_VIEW'
WHERE r.code = 'SALES_MANAGER';

-- Test login username: admin. Supply the test password outside version control.
-- The password is stored as BCrypt cost 12 and marked temporary.
INSERT IGNORE INTO users (
    username,
    email,
    full_name,
    phone,
    password_hash,
    password_must_change,
    status
)
VALUES (
    'admin',
    'admin@saleswarehouse.local',
    'Sprint 1 Administrator',
    '0900000000',
    '$2a$12$80DX0n9VVnCPjtjUWgQV5OqdzSa0MlCcmSYlXj9sJxq.SzP5JlHAC',
    TRUE,
    'ACTIVE'
);

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE u.username = 'admin';

INSERT IGNORE INTO user_login_security (user_id)
SELECT id
FROM users
WHERE username = 'admin';

COMMIT;
