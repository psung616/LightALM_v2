-- users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(120) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    system_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_system_role CHECK (system_role IN ('ADMIN','USER'))
);

-- projects
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    project_key VARCHAR(10) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issue_seq INTEGER NOT NULL DEFAULT 0,
    requirement_seq INTEGER NOT NULL DEFAULT 0,
    github_repo_owner VARCHAR(100),
    github_repo_name VARCHAR(100),
    github_access_token VARCHAR(255),
    github_webhook_secret VARCHAR(255),
    jenkins_base_url VARCHAR(255),
    jenkins_job_name VARCHAR(150),
    jenkins_api_user VARCHAR(100),
    jenkins_api_token VARCHAR(255),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_projects_project_key UNIQUE (project_key),
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE','ARCHIVED'))
);

-- project_members
CREATE TABLE project_members (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_members_role CHECK (role IN ('PROJECT_ADMIN','MEMBER','VIEWER'))
);

-- requirements
CREATE TABLE requirements (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    req_key VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    parent_requirement_id BIGINT REFERENCES requirements(id) ON DELETE SET NULL,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_to BIGINT REFERENCES users(id) ON DELETE SET NULL,
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_requirements_req_key UNIQUE (req_key),
    CONSTRAINT chk_requirements_type CHECK (type IN ('FUNCTIONAL','NON_FUNCTIONAL','BUSINESS')),
    CONSTRAINT chk_requirements_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_requirements_status CHECK (status IN ('DRAFT','APPROVED','IN_PROGRESS','IMPLEMENTED','VERIFIED','REJECTED'))
);

CREATE INDEX idx_requirements_project_id ON requirements(project_id);
CREATE INDEX idx_requirements_parent_requirement_id ON requirements(parent_requirement_id);

-- issues
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    issue_key VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    reporter_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assignee_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP,
    CONSTRAINT uq_issues_issue_key UNIQUE (issue_key),
    CONSTRAINT chk_issues_type CHECK (type IN ('BUG','TASK','STORY','IMPROVEMENT')),
    CONSTRAINT chk_issues_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_issues_status CHECK (status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE','CLOSED'))
);

CREATE INDEX idx_issues_project_id ON issues(project_id);
CREATE INDEX idx_issues_assignee_id ON issues(assignee_id);

-- traceability_links
CREATE TABLE traceability_links (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    link_type VARCHAR(20) NOT NULL,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_traceability_links UNIQUE (source_type, source_id, target_type, target_id, link_type),
    CONSTRAINT chk_traceability_links_source_type CHECK (source_type IN ('REQUIREMENT','ISSUE')),
    CONSTRAINT chk_traceability_links_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE')),
    CONSTRAINT chk_traceability_links_link_type CHECK (link_type IN ('IMPLEMENTS','TESTS','DEPENDS_ON','RELATES_TO','DUPLICATES'))
);

CREATE INDEX idx_traceability_links_project_id ON traceability_links(project_id);
CREATE INDEX idx_traceability_links_source ON traceability_links(source_type, source_id);
CREATE INDEX idx_traceability_links_target ON traceability_links(target_type, target_id);

-- comments
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    author_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_comments_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE'))
);

CREATE INDEX idx_comments_target ON comments(target_type, target_id);

-- git_links
CREATE TABLE git_links (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    source VARCHAR(20) NOT NULL,
    commit_sha VARCHAR(40),
    pr_number INTEGER,
    pr_status VARCHAR(20),
    message TEXT,
    author_login VARCHAR(100),
    url VARCHAR(500) NOT NULL,
    linked_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_git_links_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE')),
    CONSTRAINT chk_git_links_source CHECK (source IN ('COMMIT','PULL_REQUEST')),
    CONSTRAINT chk_git_links_pr_status CHECK (pr_status IS NULL OR pr_status IN ('OPEN','MERGED','CLOSED'))
);

CREATE INDEX idx_git_links_target ON git_links(target_type, target_id);
CREATE INDEX idx_git_links_project_id ON git_links(project_id);

-- jenkins_builds
CREATE TABLE jenkins_builds (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    job_name VARCHAR(150) NOT NULL,
    build_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    build_url VARCHAR(500) NOT NULL,
    triggered_by VARCHAR(100),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_jenkins_builds UNIQUE (project_id, job_name, build_number),
    CONSTRAINT chk_jenkins_builds_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE')),
    CONSTRAINT chk_jenkins_builds_status CHECK (status IN ('SUCCESS','FAILURE','UNSTABLE','RUNNING','ABORTED'))
);

CREATE INDEX idx_jenkins_builds_target ON jenkins_builds(target_type, target_id);
CREATE INDEX idx_jenkins_builds_project_id ON jenkins_builds(project_id);
