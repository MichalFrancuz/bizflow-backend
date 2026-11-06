
CREATE TABLE time_entries (
                              id BIGINT NOT NULL AUTO_INCREMENT,

                              project_id BIGINT NOT NULL,
                              employee_id BIGINT NOT NULL,

                              description VARCHAR(1000) NOT NULL,
                              work_date DATE NOT NULL,

                              hours DECIMAL(8, 2) NOT NULL,
                              hourly_rate DECIMAL(12, 2) NOT NULL,

                              currency VARCHAR(3) NOT NULL DEFAULT 'PLN',

                              created_at DATETIME(6) NOT NULL,

                              PRIMARY KEY (id),

                              CONSTRAINT fk_time_entries_project
                                  FOREIGN KEY (project_id)
                                      REFERENCES projects(id),

                              CONSTRAINT fk_time_entries_employee
                                  FOREIGN KEY (employee_id)
                                      REFERENCES employees(id),

                              CONSTRAINT chk_time_entries_hours
                                  CHECK (hours > 0),

                              CONSTRAINT chk_time_entries_hourly_rate
                                  CHECK (hourly_rate >= 0),

                              INDEX idx_time_entries_project (project_id),
                              INDEX idx_time_entries_employee (employee_id),
                              INDEX idx_time_entries_project_date (project_id, work_date)
);
