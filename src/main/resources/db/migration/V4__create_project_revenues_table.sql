
CREATE TABLE project_revenues (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  category VARCHAR(30) NOT NULL,
                                  description VARCHAR(1000) NOT NULL,
                                  amount DECIMAL(15, 2) NOT NULL,
                                  currency VARCHAR(3) NOT NULL DEFAULT 'PLN',
                                  revenue_date DATE NOT NULL,
                                  project_id BIGINT NOT NULL,
                                  created_at DATETIME(6) NOT NULL,

                                  PRIMARY KEY (id),

                                  CONSTRAINT fk_project_revenues_project
                                      FOREIGN KEY (project_id)
                                          REFERENCES projects(id),

                                  CONSTRAINT chk_project_revenues_amount
                                      CHECK (amount > 0),

                                  INDEX idx_project_revenues_project_id (project_id),
                                  INDEX idx_project_revenues_project_date (project_id, revenue_date)
);
