
-- ==========================================
-- BIZFLOW - REFRESH TOKENS
-- Migration: V6
-- ==========================================

CREATE TABLE refresh_tokens (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                app_user_id BIGINT NOT NULL,
                                token_hash CHAR(64) NOT NULL,
                                family_id CHAR(36) NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                created_at DATETIME(6) NOT NULL,
                                revoked_at DATETIME(6) DEFAULT NULL,
                                replaced_by_token_hash CHAR(64) DEFAULT NULL,

                                PRIMARY KEY (id),

                                CONSTRAINT uk_refresh_tokens_hash
                                    UNIQUE (token_hash),

                                CONSTRAINT fk_refresh_tokens_app_user
                                    FOREIGN KEY (app_user_id)
                                        REFERENCES app_users(id)
                                        ON DELETE CASCADE,

                                INDEX idx_refresh_tokens_user (app_user_id),
                                INDEX idx_refresh_tokens_family (family_id),
                                INDEX idx_refresh_tokens_expiration (expires_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
