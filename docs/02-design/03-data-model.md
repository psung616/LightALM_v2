> Owner: architect | Status: current | Last-reviewed: 2026-08-08
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

## 3. 데이터 모델 (엔티티 & DB 테이블)

공통 규칙:
- 모든 테이블의 PK는 `id BIGSERIAL PRIMARY KEY`
- 모든 테이블에 `created_at TIMESTAMP NOT NULL DEFAULT now()` 포함, 수정 가능한 테이블은 `updated_at TIMESTAMP NOT NULL DEFAULT now()` 포함
- Enum은 DB에는 `VARCHAR` + `CHECK` 제약으로 저장하고, JPA에서는 `@Enumerated(EnumType.STRING)` 사용
- 외래키는 모두 `ON DELETE CASCADE` 또는 `ON DELETE SET NULL` 중 아래 명시된 대로 적용

### 3.1 `users`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt 해시) |
| email | VARCHAR(120) | UNIQUE, NOT NULL |
| full_name | VARCHAR(100) | NOT NULL |
| system_role | VARCHAR(20) | NOT NULL, CHECK IN ('ADMIN','USER'), DEFAULT 'USER' |
| enabled | BOOLEAN | NOT NULL DEFAULT true |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

### 3.2 `projects`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_key | VARCHAR(10) | UNIQUE, NOT NULL (예: `LALM`, 대문자 3~10자) |
| name | VARCHAR(150) | NOT NULL |
| description | TEXT | NULL |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('ACTIVE','ARCHIVED'), DEFAULT 'ACTIVE' |
| issue_seq | INTEGER | NOT NULL DEFAULT 0 (이슈 키 채번용 카운터) |
| requirement_seq | INTEGER | NOT NULL DEFAULT 0 (요구사항 키 채번용 카운터) |
| test_case_seq | INTEGER | NOT NULL DEFAULT 0 (v2 확장, Phase 12 — 테스트케이스 키 채번용 카운터) |
| github_repo_owner | VARCHAR(100) | NULL |
| github_repo_name | VARCHAR(100) | NULL |
| github_access_token | VARCHAR(255) | NULL (GitHub PAT, MVP는 평문 저장 — 07-integrations.md §7.4 참고) |
| github_webhook_secret | VARCHAR(255) | NULL |
| jenkins_base_url | VARCHAR(255) | NULL |
| jenkins_job_name | VARCHAR(150) | NULL |
| jenkins_api_user | VARCHAR(100) | NULL |
| jenkins_api_token | VARCHAR(255) | NULL |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

### 3.3 `project_members`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| user_id | BIGINT | FK → users.id, ON DELETE CASCADE, NOT NULL |
| role | VARCHAR(20) | NOT NULL, CHECK IN ('PROJECT_ADMIN','MEMBER','VIEWER') |
| joined_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, user_id)

### 3.4 `requirements`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| req_key | VARCHAR(30) | UNIQUE, NOT NULL (예: `LALM-R7`) |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | NULL |
| type | VARCHAR(20) | NOT NULL, CHECK IN ('FUNCTIONAL','NON_FUNCTIONAL','BUSINESS') |
| priority | VARCHAR(20) | NOT NULL, CHECK IN ('LOW','MEDIUM','HIGH','CRITICAL'), DEFAULT 'MEDIUM' |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('DRAFT','APPROVED','IN_PROGRESS','IMPLEMENTED','VERIFIED','REJECTED'), DEFAULT 'DRAFT' |
| parent_requirement_id | BIGINT | FK → requirements.id, ON DELETE SET NULL, NULL 허용 (상위 요구사항) |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| assigned_to | BIGINT | FK → users.id, ON DELETE SET NULL, NULL 허용 |
| due_date | DATE | NULL 허용 (신규 — 개인화 대시보드의 마감 임박/기한 초과 판단 기준, 04-api.md §4.11·05-frontend.md §5.2 참고) |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

### 3.5 `issues`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| issue_key | VARCHAR(30) | UNIQUE, NOT NULL (예: `LALM-101`) |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | NULL |
| type | VARCHAR(20) | NOT NULL, CHECK IN ('BUG','TASK','STORY','IMPROVEMENT') |
| priority | VARCHAR(20) | NOT NULL, CHECK IN ('LOW','MEDIUM','HIGH','CRITICAL'), DEFAULT 'MEDIUM' |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE','CLOSED'), DEFAULT 'TODO' |
| reporter_id | BIGINT | FK → users.id, ON DELETE SET NULL |
| assignee_id | BIGINT | FK → users.id, ON DELETE SET NULL, NULL 허용 |
| due_date | DATE | NULL 허용 (신규 — 개인화 대시보드의 마감 임박/기한 초과 판단 기준, 04-api.md §4.11·05-frontend.md §5.2 참고) |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |
| resolved_at | TIMESTAMP | NULL |

### 3.6 `traceability_links`
요구사항 ↔ 이슈, 요구사항 ↔ 요구사항(참조성) 등 범용 연결 테이블. `source_type`/`target_type`은 `REQUIREMENT`, `ISSUE`, `TEST_CASE`만 허용.

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| source_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| source_id | BIGINT | NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| target_id | BIGINT | NOT NULL |
| link_type | VARCHAR(20) | NOT NULL, CHECK IN ('IMPLEMENTS','TESTS','DEPENDS_ON','RELATES_TO','DUPLICATES') |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (source_type, source_id, target_type, target_id, link_type) — 동일 링크 중복 방지

> 실제 서비스 로직에서는 `REQUIREMENT → ISSUE`(link_type='IMPLEMENTS' 또는 'TESTS') 조합만 UI에서 주로 사용하지만, 테이블 자체는 범용으로 설계한다.

> **(v2 확장)** `TEST_CASE`를 `source_type`/`target_type`에 추가한 이유: 별도의 요구사항↔테스트케이스 연결 테이블을 새로 만들지 않고, 기존 `traceability_links`에 이미 정의되어 있는 `link_type='TESTS'` 값을 그대로 재사용해 `REQUIREMENT → TEST_CASE` 링크를 표현하기 위함이다(§3.11 참고).

### 3.7 `comments`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| target_id | BIGINT | NOT NULL |
| author_id | BIGINT | FK → users.id, ON DELETE SET NULL |
| content | TEXT | NOT NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

> **(v2 확장)** `TEST_CASE`를 `target_type`에 추가해 테스트케이스도 요구사항/이슈처럼 댓글을 달 수 있게 한다.

### 3.8 `git_links` (GitHub 커밋/PR 연결)
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| target_id | BIGINT | NOT NULL |
| source | VARCHAR(20) | NOT NULL, CHECK IN ('COMMIT','PULL_REQUEST') |
| commit_sha | VARCHAR(40) | NULL |
| pr_number | INTEGER | NULL |
| pr_status | VARCHAR(20) | NULL, CHECK IN ('OPEN','MERGED','CLOSED') |
| message | TEXT | NULL (커밋 메시지 또는 PR 제목) |
| author_login | VARCHAR(100) | NULL (GitHub 사용자명) |
| url | VARCHAR(500) | NOT NULL |
| linked_at | TIMESTAMP | NOT NULL DEFAULT now() |

### 3.9 `jenkins_builds`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| target_id | BIGINT | NOT NULL |
| job_name | VARCHAR(150) | NOT NULL |
| build_number | INTEGER | NOT NULL |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('SUCCESS','FAILURE','UNSTABLE','RUNNING','ABORTED') |
| build_url | VARCHAR(500) | NOT NULL |
| triggered_by | VARCHAR(100) | NULL |
| started_at | TIMESTAMP | NULL |
| finished_at | TIMESTAMP | NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, job_name, build_number)

### 3.10 ERD 요약 (텍스트)
```
User 1---N ProjectMember N---1 Project
Project 1---N Requirement (self FK: parent_requirement_id)
Project 1---N Issue
Project 1---N TraceabilityLink (source/target = Requirement|Issue|TestCase, 다형 연관은 FK 제약 없이 애플리케이션 레벨 검증)
Project 1---N Comment (target = Requirement|Issue|TestCase)
Project 1---N GitLink (target = Requirement|Issue)
Project 1---N JenkinsBuild (target = Requirement|Issue)
Project 1---N TestCase (optional FK: requirement_id)
Project 1---N TestRun (optional FK: release_id)
TestRun 1---N TestRunResult N---1 TestCase
Project 1---N Release 1---N ReleaseItem (target = Requirement|Issue)
Project 1---N AuditLog (target = Requirement|Issue|TestCase|Release|Project|User|TraceabilityLink)
Project 1---N ApprovalRequest (target = Requirement|Issue, MVP는 Requirement만 사용)
```

---

### 3.11 `test_cases`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| requirement_id | BIGINT | FK → requirements.id, ON DELETE SET NULL, NULL 허용 |
| tc_key | VARCHAR(30) | UNIQUE, NOT NULL (예: `LALM-TC12`) |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | NULL |
| preconditions | TEXT | NULL |
| steps | TEXT | NOT NULL (번호 매긴 절차) |
| expected_result | TEXT | NOT NULL |
| priority | VARCHAR(20) | NOT NULL, CHECK IN ('LOW','MEDIUM','HIGH','CRITICAL'), DEFAULT 'MEDIUM' |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('DRAFT','READY','DEPRECATED'), DEFAULT 'DRAFT' |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

### 3.12 `test_runs`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| release_id | BIGINT | FK → releases.id, ON DELETE SET NULL, NULL 허용 |
| name | VARCHAR(150) | NOT NULL |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('PLANNED','IN_PROGRESS','COMPLETED'), DEFAULT 'PLANNED' |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| started_at | TIMESTAMP | NULL |
| completed_at | TIMESTAMP | NULL |

> **(구현 순서 메모)** Phase 12 마이그레이션(`V3__test_cases.sql`)은 `release_id` 컬럼 없이 `test_runs`를 생성한다 — `releases` 테이블이 아직 존재하지 않기 때문(Phase 13에서 생성). Phase 13 마이그레이션이 `ALTER TABLE test_runs ADD COLUMN release_id BIGINT REFERENCES releases(id) ON DELETE SET NULL;`로 뒤늦게 추가한다.

### 3.13 `test_run_results`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| test_run_id | BIGINT | FK → test_runs.id, ON DELETE CASCADE, NOT NULL |
| test_case_id | BIGINT | FK → test_cases.id, ON DELETE CASCADE, NOT NULL |
| result | VARCHAR(20) | NOT NULL, CHECK IN ('NOT_RUN','PASS','FAIL','BLOCKED','SKIPPED'), DEFAULT 'NOT_RUN' |
| actual_result | TEXT | NULL |
| executed_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| executed_at | TIMESTAMP | NULL |

UNIQUE (test_run_id, test_case_id)

### 3.14 `releases`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| version | VARCHAR(50) | NOT NULL (예: `1.2.0`) |
| name | VARCHAR(150) | NULL |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('PLANNED','IN_PROGRESS','RELEASED','ARCHIVED'), DEFAULT 'PLANNED' |
| release_date | DATE | NULL |
| description | TEXT | NULL |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, version)

### 3.15 `release_items`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| release_id | BIGINT | FK → releases.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| target_id | BIGINT | NOT NULL |
| added_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| added_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (release_id, target_type, target_id)

### 3.16 `audit_logs`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NULL 허용 (NULL이면 시스템 레벨 이벤트, 예: 사용자 관리) |
| target_type | VARCHAR(30) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE','RELEASE','PROJECT','USER','TRACEABILITY_LINK') |
| target_id | BIGINT | NOT NULL |
| action | VARCHAR(30) | NOT NULL, CHECK IN ('CREATE','UPDATE','STATUS_CHANGE','DELETE','APPROVE','REJECT') |
| field_name | VARCHAR(100) | NULL |
| old_value | TEXT | NULL |
| new_value | TEXT | NULL |
| actor_id | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

> **append-only**: 이 테이블에는 UPDATE/DELETE를 절대 실행하지 않는다. 추적 대상 엔티티(요구사항/이슈 등)가 생성/수정/상태변경/삭제되거나 승인 요청이 결정될 때마다 서비스 레이어가 이 테이블에 행을 추가만 한다.

### 3.17 `approval_requests`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') (테이블은 범용이지만 MVP는 REQUIREMENT만 실제로 사용 — 아래 참고) |
| target_id | BIGINT | NOT NULL |
| requested_status | VARCHAR(20) | NOT NULL |
| requested_by | BIGINT | FK → users.id, ON DELETE SET NULL, NOT NULL |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('PENDING','APPROVED','REJECTED','CANCELLED'), DEFAULT 'PENDING' |
| approver_id | BIGINT | FK → users.id, ON DELETE SET NULL, NULL 허용 |
| comment | TEXT | NULL |
| requested_at | TIMESTAMP | NOT NULL DEFAULT now() |
| resolved_at | TIMESTAMP | NULL |

> MVP만 놓고 보면 `target_type='REQUIREMENT'`, `requested_status='APPROVED'` 조합 하나만 실제로 생성되며(요구사항이 `DRAFT` 상태일 때만 승인 요청 가능), ISSUE 지원은 테이블 설계상 자리만 마련해둔 것으로 이번 버전에서 구현하지 않는다.
