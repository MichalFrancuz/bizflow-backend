-- ==========================================
-- BIZFLOW - INITIAL DATABASE SCHEMA
-- Migration: V1
-- ==========================================

-- 1. User accounts
CREATE TABLE app_users (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           email VARCHAR(254) NOT NULL,
                           password_hash VARCHAR(255) NOT NULL,
                           enabled BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at DATETIME(6) NOT NULL,

                           PRIMARY KEY (id),
                           CONSTRAINT uk_app_users_email UNIQUE (email)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- 2. Organizations
CREATE TABLE organizations (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               name VARCHAR(200) NOT NULL,
                               tax_id VARCHAR(20),
                               active BOOLEAN NOT NULL DEFAULT TRUE,
                               created_at DATETIME(6) NOT NULL,

                               PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- 3. Organization memberships
CREATE TABLE organization_members (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      organization_id BIGINT NOT NULL,
                                      app_user_id BIGINT NOT NULL,
                                      role VARCHAR(20) NOT NULL,
                                      created_at DATETIME(6) NOT NULL,

                                      PRIMARY KEY (id),

                                      CONSTRAINT uk_organization_member
                                          UNIQUE (organization_id, app_user_id),

                                      CONSTRAINT fk_members_organization
                                          FOREIGN KEY (organization_id)
                                              REFERENCES organizations(id),

                                      CONSTRAINT fk_members_app_user
                                          FOREIGN KEY (app_user_id)
                                              REFERENCES app_users(id),

                                      INDEX idx_members_app_user (app_user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- 4. Employees
CREATE TABLE employees (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           organization_id BIGINT NOT NULL,
                           name VARCHAR(100) NOT NULL,
                           familyname VARCHAR(100),
                           email VARCHAR(254),
                           phonenumber VARCHAR(30),
                           salary DECIMAL(12,2),
                           salary_currency VARCHAR(3),
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at DATETIME(6) NOT NULL,

                           PRIMARY KEY (id),

                           CONSTRAINT fk_employees_organization
                               FOREIGN KEY (organization_id)
                                   REFERENCES organizations(id),

                           INDEX idx_employees_organization (organization_id),

                           INDEX idx_employees_org_active (organization_id, active)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;