> Owner: architect | Status: current | Last-reviewed: 2026-10-05
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
| project_key | VARCHAR(20) | UNIQUE, NOT NULL (예: `LALM`, `TEAM_A_V2`. 대문자로 시작하는 대문자/숫자/언더바 3~20자, 정규식 `^[A-Z][A-Z0-9_]{2,19}$`. ADR-009로 대문자 전용 3~10자에서 완화됨) |
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
| requirement_level | VARCHAR(10) | NOT NULL, CHECK IN ('PRD','SRS'), DEFAULT 'SRS' (신규 — ADR-013. `type`(성격 분류)과 다른 축으로, 문서 레벨(제품 수준 PRD vs 소프트웨어 수준 SRS)만 구분. 기존 데이터는 전부 `'SRS'`로 백필. `parent_requirement_id`와의 상하 관계는 강제하지 않음 — 사용자 판단에 맡김) |
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
| source_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') (`TEST_CASE`는 v2 확장 `V3__test_cases.sql`에서 이미 추가됨, Phase 12 — 아래 정정 각주 참고) |
| source_id | BIGINT | NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| target_id | BIGINT | NOT NULL |
| link_type | VARCHAR(20) | NOT NULL, CHECK IN ('IMPLEMENTS','TESTS','DEPENDS_ON','RELATES_TO','DUPLICATES') |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (source_type, source_id, target_type, target_id, link_type) — 동일 링크 중복 방지

> 실제 서비스 로직에서는 `REQUIREMENT → ISSUE`(link_type='IMPLEMENTS' 또는 'TESTS') 조합만 UI에서 주로 사용하지만, 테이블 자체는 범용으로 설계한다.

> **(v2 확장)** `TEST_CASE`를 `source_type`/`target_type`에 추가한 이유: 별도의 요구사항↔테스트케이스 연결 테이블을 새로 만들지 않고, 기존 `traceability_links`에 이미 정의되어 있는 `link_type='TESTS'` 값을 그대로 재사용해 `REQUIREMENT → TEST_CASE` 링크를 표현하기 위함이다(§3.11 참고).

> **(ADR-013, 2026-10-03 — 2026-10-04 정정) `source_type`/`target_type` 대칭성 — 실제로는 이미 Phase 12부터 대칭이었다**: ADR-013은 원래 "`source_type`은 지금까지 `REQUIREMENT`/`ISSUE`만 허용해 테스트케이스를 출발점으로 하는 링크를 만들 수 없었다"고 전제하고 이를 바로잡는 신규 마이그레이션을 설계했으나, 이는 **`V1__init.sql`만 조사하고 그 뒤에 적용된 `V3__test_cases.sql`(Phase 12, 2026-08-04, 63~65행)을 놓친 조사 오류였다.** 실제로는 `V3__test_cases.sql`이 이미 `chk_traceability_links_source_type`을 `('REQUIREMENT','ISSUE','TEST_CASE')`로 넓혀뒀고, 바로 위 표의 `source_type` 행과 이 섹션 맨 위 "`source_type`/`target_type`은 `REQUIREMENT`, `ISSUE`, `TEST_CASE`만 허용"이라는 서술은 ADR-013 이전부터 **이미 정확했다.** ADR-013 구현 과정에서 만들어진 `V12__widen_traceability_links_source_type.sql`은 동일한 제약을 다시 적용하는 **no-op**이었다(해롭지는 않음). 상세 정정 내용은 `ADR-013` 본문의 2026-10-04 정정 각주 참고. 새 `TargetType` 값을 추가한 적은 원래도 없었다 — `PolymorphicTargetValidator`(ADR-010)는 이 건과 무관하게 변경되지 않는다.

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

다형 연관(target_type/target_id) 존재/소속 검증은 Comment/GitLink/JenkinsBuild/Release/TraceabilityLink 5곳에서 공용 컴포넌트 `PolymorphicTargetValidator`(`com.lightalm.service.support`)로 통합됐다. 대상이 존재하지 않거나 다른 프로젝트 소속이면 항상 `ResourceNotFoundException`(404)으로 통일해 크로스 테넌트 존재 노출을 막는다. 상세: ADR-010.
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

---

## v3 확장 (2026-08-08, 01-scope.md §1.2 v3 항목, 아직 미구현 — ADR-008 참고)

> 아래 §3.18~§3.24는 02-competitive-reference.md에서 참고 배경과 저작권 준수 원칙을 먼저 확인할 것. 기능명은 모두 프로젝트 자체 용어로 재정의했으며 원 제품의 UI/스키마를 그대로 옮긴 것이 아니다.

### 3.18 `review_cycles`
승인 워크플로우(§3.17, approval_requests)와는 별개의 기능이다. approval_requests는 `DRAFT→APPROVED` 전이 1건을 게이팅하는 좁은 승인 게이트이고, review_cycles는 상태 전이와 무관하게 여러 검토자의 의견을 수집·기록하는 용도다.

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| target_id | BIGINT | NOT NULL |
| name | VARCHAR(150) | NOT NULL |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('OPEN','CLOSED'), DEFAULT 'OPEN' |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| closed_at | TIMESTAMP | NULL |

### 3.19 `review_participants`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| review_cycle_id | BIGINT | FK → review_cycles.id, ON DELETE CASCADE, NOT NULL |
| user_id | BIGINT | FK → users.id, ON DELETE CASCADE, NOT NULL |
| decision | VARCHAR(20) | NOT NULL, CHECK IN ('PENDING','APPROVE','REJECT','COMMENT_ONLY'), DEFAULT 'PENDING' |
| comment | TEXT | NULL |
| decided_at | TIMESTAMP | NULL |

UNIQUE (review_cycle_id, user_id)

> **범용 워크플로우 엔진이 아님을 명시**: review_participants의 decision은 기록·표시 용도이며, 서비스 레이어가 이 값을 근거로 target(요구사항/이슈)의 status를 자동으로 바꾸는 로직은 만들지 않는다(01-scope.md §1.3 원칙 유지). 상태를 바꾸려면 여전히 기존 `PATCH .../status`(또는 승인 워크플로우 §3.17)를 사용자가 직접 호출해야 한다.

### 3.20 `baselines`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| name | VARCHAR(150) | NOT NULL |
| description | TEXT | NULL |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

### 3.21 `baseline_items`
베이스라인 생성 시점의 요구사항/이슈/테스트케이스 필드 값을 JSON으로 그대로 얼려서 저장한다(스냅샷). 이후 원본이 바뀌어도 이 값은 변하지 않는다.

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| baseline_id | BIGINT | FK → baselines.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| target_id | BIGINT | NOT NULL |
| snapshot | JSONB | NOT NULL (베이스라인 생성 시점의 주요 필드 스냅샷 — title/description/status/priority 등) |
| captured_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (baseline_id, target_type, target_id)

> **비교(diff) 기능**: 별도 테이블 없이, 조회 시점에 baseline_items.snapshot과 원본 테이블의 현재 값을 서비스 레이어에서 필드 단위로 비교해 변경분을 계산해서 반환한다(04-api.md §4.18).

### 3.22 `risks`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| risk_key | VARCHAR(30) | UNIQUE, NOT NULL (예: `LALM-RISK3`) |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | NULL |
| likelihood | VARCHAR(20) | NOT NULL, CHECK IN ('LOW','MEDIUM','HIGH'), DEFAULT 'MEDIUM' |
| impact | VARCHAR(20) | NOT NULL, CHECK IN ('LOW','MEDIUM','HIGH'), DEFAULT 'MEDIUM' |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('OPEN','MITIGATED','ACCEPTED','CLOSED'), DEFAULT 'OPEN' |
| mitigation_plan | TEXT | NULL |
| owner_id | BIGINT | FK → users.id, ON DELETE SET NULL, NULL 허용 |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

> **간이 점수화**: `likelihood`/`impact` 각각 LOW=1/MEDIUM=2/HIGH=3으로 매핑해 `risk_score = likelihood × impact`(1~9)를 API 응답 시 계산값으로 내려준다 — 별도 컬럼으로 저장하지 않는다(값이 바뀌면 항상 최신 재계산). 정식 FMEA 방법론(발생도/심각도/검출도 3축 등)은 구현하지 않는다(01-scope.md §1.3 v3 비스코프 참고).
> **위험을 요구사항/이슈에 연결하는 방법**: 별도 링크 테이블을 새로 만들지 않고, 기존 §3.6 `traceability_links`의 `source_type`/`target_type` CHECK 제약에 `'RISK'`를 추가해 재사용한다(§3.11에서 `TEST_CASE`를 추가했던 것과 동일한 패턴). 새 마이그레이션에서 `ALTER TABLE traceability_links DROP CONSTRAINT ...; ALTER TABLE traceability_links ADD CONSTRAINT ... CHECK (source_type IN ('REQUIREMENT','ISSUE','TEST_CASE','RISK'))`처럼 처리한다(기존 V1~V7 파일은 수정하지 않고 새 마이그레이션에서 제약만 갱신, ADR-002 원칙 준수).

### 3.23 `variants` / `requirement_variants`
| 컬럼 (variants) | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| variant_key | VARCHAR(30) | NOT NULL (예: `STANDARD`, `PREMIUM`) |
| name | VARCHAR(150) | NOT NULL |
| description | TEXT | NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, variant_key)

| 컬럼 (requirement_variants) | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| requirement_id | BIGINT | FK → requirements.id, ON DELETE CASCADE, NOT NULL |
| variant_id | BIGINT | FK → variants.id, ON DELETE CASCADE, NOT NULL |
| applicability | VARCHAR(20) | NOT NULL, CHECK IN ('INCLUDED','EXCLUDED','MODIFIED'), DEFAULT 'INCLUDED' |
| note | TEXT | NULL (MODIFIED인 경우 이 변형에서 무엇이 다른지 짧게 서술) |

UNIQUE (requirement_id, variant_id)

> **요구사항 문서 뷰와의 관계**: 별도 테이블을 추가하지 않고, 기존 `requirements.parent_requirement_id`(§3.4)로 이미 존재하는 상위/하위 계층을 그대로 활용해 문서 목차처럼 정렬해서 보여준다. 다만 형제 요구사항 간 표시 순서를 사용자가 지정할 수 있어야 하므로, `requirements` 테이블에 컬럼을 하나 추가한다: `order_index INTEGER NOT NULL DEFAULT 0`(같은 부모를 가진 요구사항끼리 이 값 기준 오름차순 정렬. 새 마이그레이션에서 `ALTER TABLE requirements ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0` 추가, 기존 파일 수정 금지).

### 3.24 `dashboard_widget_configs`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK → users.id, ON DELETE CASCADE, NOT NULL |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NULL 허용(NULL이면 "내 작업" 개인화 대시보드용, 값이 있으면 특정 프로젝트 대시보드용) |
| widget_type | VARCHAR(50) | NOT NULL (예: `STATUS_DONUT`, `DUE_SOON_LIST`, `WORKFLOW_FUNNEL`, `RISK_HEATMAP`) |
| position | INTEGER | NOT NULL DEFAULT 0 |
| config | JSONB | NULL (위젯별 옵션, 예: 표시할 상태 필터) |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

> 리포트 내보내기(Excel/PDF)는 별도 테이블이 필요 없다 — 조회 시점에 서비스 레이어가 실시간으로 생성해서 스트리밍 응답한다(04-api.md §4.21).

### ERD 요약 추가분
```
Project 1---N ReviewCycle (target = Requirement|Issue) 1---N ReviewParticipant N---1 User
Project 1---N Baseline 1---N BaselineItem (target = Requirement|Issue|TestCase, 스냅샷 JSONB)
Project 1---N Risk (traceability_links를 통해 Requirement|Issue와 연결, source_type/target_type에 'RISK' 추가)
Project 1---N Variant 1---N RequirementVariant N---1 Requirement
User 1---N DashboardWidgetConfig (optional FK: project_id)
```

---

## v4 확장 (2026-10-03~05, 01-scope.md §1.2 v4 항목, 아직 미구현 — ADR-011·ADR-012·ADR-014)

> 아래 §3.25~§3.29는 ADR-011(회원가입+라이센스 관리)·ADR-012(프로젝트 Configuration 영역)에서 설계가 확정된 내용을 그대로 옮긴 것이다. §3.30은 ADR-014(시스템 테마 설정)에서 확정된 내용이다. 두 ADR 본문의 §2.3("업로드→파싱→저장→적용" 흐름)·§A~§D(커스텀 필드/폼 레이아웃/열거형/워크플로우) 서술이 각 테이블의 1차 근거이며, §3.30은 ADR-014 §2~§3이 근거다.

### 3.25 `licenses` (ADR-011)
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| license_key | VARCHAR(100) | UNIQUE, NOT NULL |
| organization_name | VARCHAR(150) | NOT NULL |
| license_type | VARCHAR(20) | NOT NULL, CHECK IN ('TRIAL','STANDARD','ENTERPRISE') |
| seat_limit | INTEGER | NOT NULL, CHECK (seat_limit > 0) |
| issued_at | TIMESTAMP | NOT NULL |
| expires_at | TIMESTAMP | NULL 허용(무기한 라이센스) |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('ACTIVE','SUPERSEDED','REVOKED'), DEFAULT 'ACTIVE' |
| raw_file_name | VARCHAR(255) | NOT NULL (업로드 원본 파일명, 감사용) |
| raw_payload | TEXT | NOT NULL (업로드된 라이센스 파일 원문 전체 — 재검증/감사용. 일반 첨부파일 저장소가 아니라 이 설정 파일 하나만을 위한 컬럼, ADR-011 맥락 참고) |
| signature_valid | BOOLEAN | NOT NULL DEFAULT true (서명 검증 실패 건은 저장하지 않으므로 사실상 항상 true — 감사 추적용 플래그) |
| uploaded_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| uploaded_at | TIMESTAMP | NOT NULL DEFAULT now() |

`ACTIVE` 상태는 항상 최대 1건만 존재해야 하므로 부분 유니크 인덱스로 강제한다.
```sql
CREATE UNIQUE INDEX uq_licenses_single_active ON licenses(status) WHERE status = 'ACTIVE';
```

> **`users` 테이블은 변경 없음.** `username`/`password`/`email`/`full_name`/`system_role`/`enabled`이 이미 회원가입(self-signup)에 필요한 컬럼을 모두 제공한다(§3.1 참고). 회원가입은 `systemRole`을 항상 `USER`로, `enabled`을 항상 `true`로 서버가 고정해서 저장하며 새 컬럼이 필요 없다.
> `audit_logs.target_type`(§3.16) CHECK 제약에 `'LICENSE'`를 추가해 라이센스 업로드도 감사 로그 대상에 포함한다(기존 `chk_audit_logs_target_type` 제약을 `DROP`→재생성, ADR-011 §3.3).

### 3.26 `custom_field_definitions` / `custom_field_values` (ADR-012 §A)
프로젝트가 요구사항/이슈/테스트케이스에 붙이는 추가 속성. EAV(Entity-Attribute-Value) 패턴으로, 프로젝트마다 실제 테이블에 동적 `ALTER TABLE`을 실행하지 않는다.

**`custom_field_definitions`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| field_key | VARCHAR(50) | NOT NULL (식별자, 예: `severity_custom`) |
| label | VARCHAR(100) | NOT NULL |
| data_type | VARCHAR(20) | NOT NULL, CHECK IN ('TEXT','NUMBER','DATE','BOOLEAN','SINGLE_SELECT','MULTI_SELECT') |
| enumeration_set_id | BIGINT | FK → project_enumeration_sets.id(§3.28), ON DELETE SET NULL, NULL 허용(SINGLE_SELECT/MULTI_SELECT일 때만 사용) |
| required | BOOLEAN | NOT NULL DEFAULT false |
| default_value | TEXT | NULL |
| display_order | INTEGER | NOT NULL DEFAULT 0 |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('ACTIVE','DEPRECATED'), DEFAULT 'ACTIVE' (하드 삭제 없음 — 소프트 비활성) |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, target_type, field_key)

**`custom_field_values`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| field_id | BIGINT | FK → custom_field_definitions.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL (정의와 동일 값을 중복 저장 — `PolymorphicTargetValidator` 재사용을 위함, ADR-010 패턴) |
| target_id | BIGINT | NOT NULL |
| value | TEXT | NULL (TEXT/NUMBER/DATE/BOOLEAN은 문자열 직렬화, MULTI_SELECT는 JSON 배열 문자열) |
| updated_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (field_id, target_type, target_id)

### 3.27 `form_layouts` / `form_layout_sections` / `form_layout_fields` (ADR-012 §B)
프로젝트+target_type당 레이아웃 1개만 지원(다중 레이아웃 전환 기능 제외).

**`form_layouts`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, target_type)

**`form_layout_sections`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| form_layout_id | BIGINT | FK → form_layouts.id, ON DELETE CASCADE, NOT NULL |
| title | VARCHAR(100) | NOT NULL (예: "기본 정보") |
| display_order | INTEGER | NOT NULL DEFAULT 0 |

**`form_layout_fields`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| section_id | BIGINT | FK → form_layout_sections.id, ON DELETE CASCADE, NOT NULL |
| field_source | VARCHAR(20) | NOT NULL, CHECK IN ('STANDARD','CUSTOM') |
| standard_field_key | VARCHAR(50) | NULL(`STANDARD`일 때만. 유효값은 서비스 레이어 `StandardFieldKeyRegistry`가 target_type별로 화이트리스트 검증 — DB CHECK로는 표현하지 않음) |
| custom_field_id | BIGINT | FK → custom_field_definitions.id(§3.26), ON DELETE CASCADE, NULL 허용(`CUSTOM`일 때만) |
| display_order | INTEGER | NOT NULL DEFAULT 0 |
| visible | BOOLEAN | NOT NULL DEFAULT true |

CHECK: `(field_source='STANDARD' AND standard_field_key IS NOT NULL AND custom_field_id IS NULL) OR (field_source='CUSTOM' AND custom_field_id IS NOT NULL AND standard_field_key IS NULL)`

> 레이아웃이 없는 프로젝트+target_type은 표준 필드를 코드 기본 순서로, 섹션 없이 보여준다 — 즉 설정하지 않으면 기존과 동일한 화면(하위 호환).

### 3.28 `project_enumeration_sets` / `project_enumeration_values` (ADR-012 §C)
**`project_enumeration_sets`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| enum_key | VARCHAR(50) | NOT NULL (예: `PRIORITY`, 또는 커스텀 필드 전용 `SEVERITY` 등) |
| base_enum | VARCHAR(30) | NULL 허용, CHECK IN ('PRIORITY','REQUIREMENT_STATUS','ISSUE_STATUS','TEST_CASE_STATUS') — NULL이면 커스텀 필드 전용(기존 enum과 무관) |
| name | VARCHAR(100) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, enum_key)

> **서비스 레이어 제약(DB 제약 아님)**: `base_enum IN ('REQUIREMENT_STATUS','ISSUE_STATUS','TEST_CASE_STATUS')`인 집합의 생성은 현재 `EnumerationSetService`가 거부한다 — 상태(Status) 값 자체의 확장은 이번 범위에서 제외(ADR-012 §C.1, `01-scope.md` §1.3 v4 비스코프 참고). `base_enum='PRIORITY'`와 `base_enum=NULL`만 생성 가능.

**`project_enumeration_values`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| enumeration_set_id | BIGINT | FK → project_enumeration_sets.id, ON DELETE CASCADE, NOT NULL |
| value_key | VARCHAR(50) | NOT NULL (예: `BLOCKER`) |
| label | VARCHAR(100) | NOT NULL |
| display_order | INTEGER | NOT NULL DEFAULT 0 |
| is_system_default | BOOLEAN | NOT NULL DEFAULT false (true면 기존 고정 Java enum 값을 미러링한 행 — 삭제 금지, 라벨/순서만 수정 가능) |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('ACTIVE','DEPRECATED'), DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (enumeration_set_id, value_key)

> **기존 테이블 영향(하위 호환성 — 반드시 확인)**: 프로젝트가 `base_enum='PRIORITY'` 집합을 최초 생성하는 순간, `requirements.priority`/`issues.priority`/`test_cases.priority`(§3.4·§3.5·§3.11)에 걸린 기존 CHECK 제약(`chk_..._priority`류, `V1__init.sql`/`V3__test_cases.sql` 유래, 정확한 제약명은 구현 시점에 원문 확인)을 제거하고, 검증 책임을 신규 `EnumerationValueValidator.requireValidValue(projectId, "PRIORITY", value)`(애플리케이션 레벨, 단일 진입점)로 옮긴다. 컬럼 단위 제약이라 **한 프로젝트라도 PRIORITY를 확장하면 전체 프로젝트에 대해 이 컬럼의 DB 레벨 안전장치가 느슨해진다.** `PRIORITY` 집합을 만들지 않은 프로젝트는 이 validator가 기존 Java `Priority` enum 값으로 검증하므로 동작은 바뀌지 않는다. 상태(`status`) 컬럼의 CHECK 제약은 그대로 유지한다(위 서비스 레이어 제약 참고).

### 3.29 `workflow_transition_rules` (ADR-012 §D)
대상은 `REQUIREMENT`/`ISSUE`만(`TEST_CASE`는 이미 상태가 단순해 범위에서 제외, GLOSSARY §2 그룹 2 패턴과 동일).

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| from_status | VARCHAR(20) | NOT NULL (해당 target_type의 Java enum 값 중 하나 — `RequirementStatus`/`IssueStatus`가 서로 다른 enum이라 공용 DB CHECK로 표현 불가, 서비스 레이어 화이트리스트로 검증) |
| to_status | VARCHAR(20) | NOT NULL |
| allowed_role | VARCHAR(20) | NULL 허용, CHECK IN ('PROJECT_ADMIN','MEMBER','VIEWER') (NULL이면 기존과 동일하게 `MEMBER+`) |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

CHECK: `from_status <> to_status`
UNIQUE (project_id, target_type, from_status, to_status)

> **적용 규칙(보수적 기본값)**: 프로젝트가 특정 target_type에 규칙을 하나도 등록하지 않으면 기존과 동일하게 모든 상태 간 자유 전이(하위 호환). 하나 이상 등록하면 그 프로젝트의 그 target_type은 화이트리스트 모드로 전환되어 등록된 (from,to) 조합만 허용한다. 기존 승인 게이트(§3.17 `approval_requests`, `DRAFT→APPROVED`)와는 AND 조건으로 공존한다. 시스템 `ADMIN`과 해당 프로젝트의 `PROJECT_ADMIN`은 화이트리스트와 무관하게 항상 모든 전이가 허용된다(lock-out 방지, ADR-011의 라이센스 ADMIN 로그인 예외와 동일한 설계 사유).

### 3.30 `system_theme_settings` (ADR-014)
시스템 전역 색상 템플릿(브랜드 컬러 프리셋) 설정. **항상 정확히 1행만 존재하는 단일 행 테이블**이다 — 프로젝트별 설정이 아니므로 `project_id` 컬럼이 없다(라이트/다크 모드는 서버에 저장하지 않음, ADR-014 §1 참고 — 개인 브라우저 `localStorage` 선호로만 처리하고 이 테이블에는 컬럼 자체가 없다).

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGINT | PK, `DEFAULT 1`, `CHECK (id = 1)` — PK 유니크 제약과 이 체크로 행이 항상 0개 또는 1개임을 DB 레벨에서 강제(licenses의 "ACTIVE 1건" 부분 유니크 인덱스와 다른, 처음부터 단일 행 전용인 더 단순한 패턴) |
| color_preset | VARCHAR(20) | NOT NULL, `DEFAULT 'DEFAULT'`, CHECK IN ('DEFAULT','RED','BLUE','GREEN','PURPLE') |
| updated_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

각 `color_preset` 값이 실제로 가리키는 색상 토큰 4종(`primary`/`primary-hover`/`primary-focus`/`brand-secure`)의 구체적인 hex 값은 DB에 저장하지 않고 프론트엔드 `index.css`에 코드로 고정한다(ADR-014 §2·§5.1 참고) — 이 테이블은 "지금 어떤 프리셋이 선택돼 있는가"라는 상태값 하나만 책임진다.

마이그레이션 시드: 이 테이블이 생성되는 즉시 `id=1, color_preset='DEFAULT'` 행을 1건 넣어, 기능 배포 직후에도 조회 API가 항상 유효한 응답을 반환하고 기존 운영 화면 색상이 바뀌지 않도록 한다(하위 호환성, ADR-002/006의 idempotent 마이그레이션 원칙과 함께 적용).

> `audit_logs` 연동은 하지 않는다(ADR-014 §4 — 단일 설정의 현재값 변경이 `AuditTargetType`에 추가할 만큼 중요한 변경 이력은 아니라고 판단, 필요해지면 별도 ADR). `updated_by`/`updated_at`은 "마지막으로 누가 바꿨는지"만 보여주는 최소 추적 정보다.

### ERD 요약 추가분 (v4)
```
License (단독 테이블, project_id 없음 — 시스템 전역 1건의 ACTIVE)
Project 1---N CustomFieldDefinition 1---N CustomFieldValue (target = Requirement|Issue|TestCase)
Project 1---N FormLayout(target_type당 1개) 1---N FormLayoutSection 1---N FormLayoutField (optional FK: custom_field_id)
Project 1---N ProjectEnumerationSet 1---N ProjectEnumerationValue
Project 1---N WorkflowTransitionRule (target = Requirement|Issue)
SystemThemeSettings (단독 테이블, project_id 없음 — 시스템 전역 1행 고정, PK=1)
```
