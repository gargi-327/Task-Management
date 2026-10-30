-- Portable MySQL-style reference schema. JPA creates the runtime schema in development.
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    start_date DATE,
    due_date DATE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_id BIGINT NOT NULL,
    CONSTRAINT fk_projects_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT chk_project_dates CHECK (due_date IS NULL OR start_date IS NULL OR due_date >= start_date)
);
CREATE INDEX idx_projects_archived ON projects(archived);
CREATE INDEX idx_projects_created_by ON projects(created_by_id);
CREATE INDEX idx_projects_dates ON projects(start_date, due_date);

CREATE TABLE project_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    membership_role VARCHAR(30) NOT NULL,
    CONSTRAINT fk_members_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_project_member UNIQUE (project_id, user_id)
);
CREATE INDEX idx_project_members_user ON project_members(user_id);

CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    assignee_id BIGINT,
    created_by_id BIGINT NOT NULL,
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
    CONSTRAINT fk_tasks_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT chk_task_dates CHECK (due_date >= start_date)
);
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_assignee_status ON tasks(assignee_id, status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_priority ON tasks(priority);
