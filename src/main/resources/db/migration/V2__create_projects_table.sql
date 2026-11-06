
CREATE TABLE projects (
                          id BIGINT NOT NULL AUTO_INCREMENT,

                          name VARCHAR(200) NOT NULL,
                          description VARCHAR(2000),

                          status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',

                          budget DECIMAL(15, 2),

                          currency VARCHAR(3) NOT NULL DEFAULT 'PLN',

                          start_date DATE,
                          end_date DATE,

                          organization_id BIGINT NOT NULL,

                          created_at DATETIME(6) NOT NULL,

                          PRIMARY KEY (id),

                          CONSTRAINT fk_projects_organization
                              FOREIGN KEY (organization_id)
                                  REFERENCES organizations(id),

                          CONSTRAINT chk_projects_budget
                              CHECK (budget IS NULL OR budget >= 0),

                          CONSTRAINT chk_projects_dates
                              CHECK (
                                  start_date IS NULL
                                      OR end_date IS NULL
                                      OR end_date >= start_date
                                  ),

                          INDEX idx_projects_organization_id (organization_id),

                          INDEX idx_projects_organization_status (organization_id, status)
);
