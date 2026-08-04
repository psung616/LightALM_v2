-- releases
CREATE TABLE releases (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    name VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    release_date DATE,
    description TEXT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_releases_project_version UNIQUE (project_id, version),
    CONSTRAINT chk_releases_status CHECK (status IN ('PLANNED','IN_PROGRESS','RELEASED','ARCHIVED'))
);

CREATE INDEX idx_releases_project_id ON releases(project_id);

-- release_items
CREATE TABLE release_items (
    id BIGSERIAL PRIMARY KEY,
    release_id BIGINT NOT NULL REFERENCES releases(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    added_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    added_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_release_items UNIQUE (release_id, target_type, target_id),
    CONSTRAINT chk_release_items_target_type CHECK (target_type IN ('REQUIREMENT','ISSUE'))
);

CREATE INDEX idx_release_items_release_id ON release_items(release_id);

-- test_runs: backfill release_id now that releases exists (deferred from Phase 12, see 03-data-model.md §3.12)
ALTER TABLE test_runs ADD COLUMN release_id BIGINT REFERENCES releases(id) ON DELETE SET NULL;
