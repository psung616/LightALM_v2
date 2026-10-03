> Owner: architect | Status: Accepted (설계 완료, 구현 전) | Date: 2026-10-03

# ADR-012: 프로젝트별 Configuration 영역 — 커스텀 필드 / 폼 레이아웃 / 열거형 / 워크플로우 전이 규칙 (비스코프 재검토)

## 맥락 (Context)

제품 오너가 업계 상용 ALM 툴들이 흔히 제공하는 **"프로젝트별로 세밀하게 커스터마이징 가능한 설정(Configuration) 영역"**을 요청했다. `PROJECT_ADMIN`이 자신이 관리하는 프로젝트 안에서 직접 설정할 수 있는 기능으로 다음 4가지를 지정했고, "하나로 뭉뚱그리지 말고 기능 단위로 쪼개서" 설계하도록 명시했다.

1. 필드(Field) 설정 — 요구사항/이슈/테스트케이스에 프로젝트별 커스텀 필드 추가/편집/삭제
2. 폼 레이아웃(Form Layout) 설정 — 생성/수정 화면의 필드 노출 순서/그룹을 프로젝트별로 구성
3. 열거형(Enumeration) 설정 — 상태값/우선순위값 등 Enum성 값 목록을 프로젝트별로 추가/편집
4. 워크플로우(Workflow) 설정 — 상태 전이 규칙(어떤 상태에서 어떤 상태로 전이 가능한지, 조건/권한)을 프로젝트별로 구성

### 명시적 비스코프와의 정면 충돌 — 반드시 짚어야 하는 지점

`01-scope.md` §1.3은 다음 두 항목을 **이미 명시적으로 금지**하고 있고, 그 섹션 서두는 "Claude는 이 항목들에 대한 기능을 임의로 추가하지 않는다"고 못박고 있다.

- "커스텀 워크플로우 엔진(상태 전이는 고정된 Enum 기반)"
- "커스텀 필드(사용자 정의 필드 추가 기능)"

이번 ADR의 1번(필드)과 4번(워크플로우) 요청은 위 두 금지 항목을 **정면으로 재검토하자는 것**이다. 이는 Claude가 스스로 판단해 비스코프 결정을 뒤집은 것이 아니라, **제품 오너가 직접 이 두 비스코프 항목의 재검토를 명시적으로 요청**했기 때문에 ADR로 다루는 것임을 분명히 기록한다. `ROLES.md`의 원칙상 스코프 변경은 requirements-analyst(이 프로젝트에서는 오케스트레이터가 겸임) 영역이지만, 구조 설계가 선행되어야 승인 여부를 판단할 수 있으므로 architect가 먼저 설계안을 ADR로 제시하고, 제품 오너 승인 후 `01-scope.md` §1.2/§1.3이 갱신되는 순서를 따른다(ADR-008이 v3 확장 때 쓴 것과 동일한 절차).

### 기존 "좁은 예외" 선례와의 관계
`01-scope.md` §1.3은 이미 "커스텀 워크플로우 엔진 제외" 옆에 **괄호로 예외를 하나 두고 있다** — "§1.2의 승인 워크플로우는 `요구사항 DRAFT→APPROVED` 전이 하나만 게이팅하는 좁은 예외이며, 범용 워크플로우 엔진이 아니다." 이번 ADR의 4번(워크플로우 설정)도 같은 패턴의 **두 번째 좁은 예외**로 설계한다. 즉:

> **이 ADR은 범용 워크플로우 엔진을 만드는 것이 아니다.** "이 상태에서 저 상태로 전이할 수 있는가(참/거짓) + 최소 역할"만 표현하는 **상태 전이 화이트리스트 매트릭스**를 프로젝트별로 설정 가능하게 하는 수준으로 한정한다. 조건 분기(필드 값에 따른 분기), 전이 시 자동 액션(알림/필드 자동 변경 등), 승인자 체인 같은 범용 워크플로우 엔진의 요소는 포함하지 않는다.

필드(1번)도 마찬가지로 "완전히 자유로운 스키마 설계 도구"가 아니라, 정해진 데이터 타입 목록 안에서 프로젝트별 추가 속성 몇 개를 붙이는 수준으로 범위를 한정한다(§1 상세 참고).

### 참고 원칙
`02-competitive-reference.md`/ADR-008의 저작권 준수 원칙을 동일하게 적용한다. "프로젝트 단위로 필드/레이아웃/열거형/워크플로우를 설정한다"는 **개념**만 참고하고, 특정 제품의 화면/파일 포맷/내부 구현은 참고하지 않으며 어떤 문서에도 특정 제품명을 적지 않는다. 아래 4개 기능명은 업계에서 흔히 쓰이는 일반 용어(필드/레이아웃/열거형/워크플로우)로만 서술했다.

### 이 ADR이 끝난 뒤에도 남는 선행 조건
이 ADR은 **설계만 확정**한다. 구현에 들어가기 전에 반드시:
1. 제품 오너가 이 설계를 승인한다.
2. requirements-analyst(오케스트레이터)가 `01-scope.md` §1.3에서 "커스텀 워크플로우 엔진 제외"와 "커스텀 필드 제외" 두 항목의 비스코프 지정을 해제(또는 이 ADR이 정의한 좁은 범위로 재한정하는 문구로 수정)하고, §1.2에 네 기능을 핵심 스코프로 추가한다.
3. architect가 `03-data-model.md`/`04-api.md`/`05-frontend.md`/`GLOSSARY.md`에 해당 절을 반영한다.

이번 작업 범위에는 위 세 가지를 포함하지 않는다(ADR-011과 동일한 절차).

---

## 결정 (Decision)

4개 기능은 서로 느슨하게 연결되지만(필드 정의 → 폼 레이아웃이 그 필드를 참조, 열거형 → 필드의 선택지), **독립적으로 구현/배포 가능하도록 분리**한다. ADR-008이 v3 확장을 Phase 16~19로 쪼갠 것과 동일한 방식으로, 아래 순서를 권장 구현 순서로 제안한다(08-dev-phases.md 본문 갱신은 이 ADR의 범위 밖이므로 번호는 참고용 제안이다).

| 제안 Phase | 기능 | 선행 의존성 |
|---|---|---|
| Phase 20 | A. 커스텀 필드 | 없음 (독립적으로 먼저 구현 가능) |
| Phase 21 | B. 폼 레이아웃 | A에 의존(레이아웃이 커스텀 필드를 배치 대상으로 참조) |
| Phase 22 | C. 열거형 | A와 병행 가능(커스텀 필드의 SELECT 옵션 공급자) — A보다 먼저 구현해도 무방 |
| Phase 23 | D. 워크플로우 전이 규칙 | 없음(A/B/C와 독립) |

공통 전제:
- 네 기능 모두 `project_members.role = PROJECT_ADMIN`(또는 시스템 `ADMIN`)만 쓰기(CRUD) 가능하다. 기존 `ProjectMemberService.requireRole(projectId, userId, PROJECT_ADMIN)` 패턴을 그대로 재사용하고(06-auth.md), 새 권한 검사 로직을 만들지 않는다.
- 설정 대상(target_type)은 기존 `TargetType`(REQUIREMENT/ISSUE/TEST_CASE) 또는 그 부분집합을 재사용한다. 새 TargetType 값을 추가하지 않는다.
- 네 기능 모두 "읽기(화면 렌더링용 조회)"는 `VIEWER+`(프로젝트 멤버 전원), "쓰기(설정 변경)"는 `PROJECT_ADMIN+`로 구분한다.

---

### A. 커스텀 필드 (Custom Field) 설정

#### A.1 범위
요구사항/이슈/테스트케이스에 프로젝트별로 몇 개의 **추가 속성**을 붙이는 기능으로 한정한다. 아래는 포함하지 않는다(과설계 방지, "완전 자유 스키마 도구"가 아님을 명확히 하기 위함):
- 필드 간 의존 관계(필드 A의 값에 따라 필드 B가 보이거나 필수가 되는 조건부 로직)
- 수식/계산 필드
- 다른 프로젝트/다른 target_type과 필드를 공유하는 기능(필드는 항상 `프로젝트 + target_type` 단위로 독립)

지원 데이터 타입: `TEXT`, `NUMBER`, `DATE`, `BOOLEAN`, `SINGLE_SELECT`, `MULTI_SELECT`. 뒤의 두 타입은 아래 C(열거형)에서 정의한 "커스텀 필드 전용 열거형 집합"을 선택지로 참조한다.

#### A.2 설계 방식 — EAV(Entity-Attribute-Value) 패턴
프로젝트마다 실제 테이블에 `ALTER TABLE ... ADD COLUMN`을 동적으로 실행하는 방식(동적 DDL)은 선택하지 않았다. 이유: (1) 동적 DDL은 Flyway 기반 마이그레이션 이력 관리와 충돌하고, (2) 프로젝트마다 테이블 구조가 달라지면 JPA 엔티티 매핑이 불가능해진다. 대신 **필드 정의 테이블 + 값 테이블을 분리하는 EAV 패턴**(여러 데이터 모델링 교재에 나오는 일반적인 기법, 특정 제품 고유 기법이 아님)을 쓴다. 트레이드오프는 "결과" 섹션에 기록한다.

#### A.3 DB 스키마

**`custom_field_definitions`** — 필드 정의
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| field_key | VARCHAR(50) | NOT NULL (식별자, 예: `severity_custom`, 영숫자/언더바) |
| label | VARCHAR(100) | NOT NULL (화면 표시명) |
| data_type | VARCHAR(20) | NOT NULL, CHECK IN ('TEXT','NUMBER','DATE','BOOLEAN','SINGLE_SELECT','MULTI_SELECT') |
| enumeration_set_id | BIGINT | FK → project_enumeration_sets.id, ON DELETE SET NULL, NULL 허용(SINGLE_SELECT/MULTI_SELECT일 때만 사용 — §C) |
| required | BOOLEAN | NOT NULL DEFAULT false |
| default_value | TEXT | NULL |
| display_order | INTEGER | NOT NULL DEFAULT 0 |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('ACTIVE','DEPRECATED'), DEFAULT 'ACTIVE' |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, target_type, field_key)

> **"삭제"는 하드 삭제가 아니라 소프트 비활성(`status='DEPRECATED'`)이다.** 이미 값이 입력된 레코드(`custom_field_values`)를 보존하기 위함. 비활성 필드는 생성/수정 폼에서는 숨기지만, 기존 값이 있는 상세 조회 화면에서는 읽기 전용으로 계속 노출한다.
> **`data_type`과 `field_key`는 생성 후 변경 불가**(API에서 수정 대상 필드에서 제외) — 이미 저장된 값과 타입이 불일치하는 상황을 원천 차단.

**`custom_field_values`** — 필드 값 (대상별 1행)
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| field_id | BIGINT | FK → custom_field_definitions.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL (정의와 동일한 값을 중복 저장 — 다형 연관 검증을 `PolymorphicTargetValidator`로 통일하기 위함, ADR-010 패턴) |
| target_id | BIGINT | NOT NULL |
| value | TEXT | NULL (TEXT/NUMBER/DATE/BOOLEAN은 문자열 직렬화, MULTI_SELECT는 JSON 배열 문자열로 저장) |
| updated_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (field_id, target_type, target_id)

> 값 조회/저장 시 대상(target_type/target_id) 존재·프로젝트 소속 검증은 신규 로직을 만들지 않고 기존 `PolymorphicTargetValidator`(`com.lightalm.service.support`, ADR-010)를 재사용한다.

#### A.4 API (요약)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/custom-fields?targetType=` | 필드 정의 목록(ACTIVE+DEPRECATED, 설정화면용) | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/custom-fields` | 필드 생성 `{targetType,fieldKey,label,dataType,enumerationSetId?,required,defaultValue}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/config/custom-fields/{fieldId}` | 필드 수정(`label`/`required`/`defaultValue`/`displayOrder`만, `dataType`/`fieldKey` 불변) | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}/config/custom-fields/{fieldId}` | 소프트 삭제(`status=DEPRECATED`) | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/custom-fields?targetType=` | 활성 필드 정의만(생성/수정 폼 렌더링용) | VIEWER+ |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/custom-field-values` | 대상의 커스텀 필드 값 전체 조회 | VIEWER+ |
| PUT | `/api/projects/{projectId}/{targetType}/{targetId}/custom-field-values` | 값 일괄 저장 `{values:[{fieldId,value}]}` | MEMBER+ |

#### A.5 화면
기존 `/projects/:projectId/settings`(탭: 일반 정보/멤버 관리/GitHub 연동/Jenkins 연동)에 **"필드" 탭을 추가**한다. target_type(요구사항/이슈/테스트케이스) 선택 후 필드 목록 테이블(필드명/타입/필수여부/상태) + 추가/수정/비활성화. 요구사항/이슈/테스트케이스의 생성/수정/상세 화면은 활성 커스텀 필드를 표준 필드 뒤에 추가로 렌더링한다(배치 순서/그룹은 B에서 다룬다).

---

### B. 폼 레이아웃 (Form Layout) 설정

#### B.1 범위
"생성/수정 화면에서 어떤 필드를 어떤 순서/그룹으로 보여줄지"만 다룬다. **프로젝트+target_type당 레이아웃은 정확히 1개만 지원**한다(사용자가 여러 레이아웃을 만들어 전환하는 기능은 제외 — 과설계 방지, Light 철학 유지). 표준 필드(title/description/priority/status/assignedTo/dueDate 등 기존 고정 필드)는 완전히 숨길 수 없다(최소한 읽기 전용으로는 노출) — 폼 자체가 깨지는 것을 방지하기 위한 서비스 레이어 검증.

#### B.2 DB 스키마

**`form_layouts`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE','TEST_CASE') |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, target_type) — 프로젝트+target_type당 1개 고정

**`form_layout_sections`** (그룹)
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| form_layout_id | BIGINT | FK → form_layouts.id, ON DELETE CASCADE, NOT NULL |
| title | VARCHAR(100) | NOT NULL (예: "기본 정보", "일정") |
| display_order | INTEGER | NOT NULL DEFAULT 0 |

**`form_layout_fields`** (섹션 안의 필드 배치)
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| section_id | BIGINT | FK → form_layout_sections.id, ON DELETE CASCADE, NOT NULL |
| field_source | VARCHAR(20) | NOT NULL, CHECK IN ('STANDARD','CUSTOM') |
| standard_field_key | VARCHAR(50) | NULL(`STANDARD`일 때만 사용 — 허용값은 서비스 레이어 화이트리스트로 검증: `title`,`description`,`priority`,`status`,`assignedTo`/`assigneeId`,`dueDate` 등 target_type별로 실제 존재하는 표준 필드만) |
| custom_field_id | BIGINT | FK → custom_field_definitions.id, ON DELETE CASCADE, NULL 허용(`CUSTOM`일 때만 사용) |
| display_order | INTEGER | NOT NULL DEFAULT 0 |
| visible | BOOLEAN | NOT NULL DEFAULT true |
| CHECK | | `(field_source='STANDARD' AND standard_field_key IS NOT NULL AND custom_field_id IS NULL) OR (field_source='CUSTOM' AND custom_field_id IS NOT NULL AND standard_field_key IS NULL)` |

> `standard_field_key`의 유효값 집합을 DB CHECK로 걸지 않은 이유: target_type마다(요구사항/이슈/테스트케이스) 표준 필드 집합이 다르고, 여러 Java 클래스에 흩어져 있어 DB 레벨에서 단일 CHECK로 표현하기 어렵다. 대신 서비스 레이어의 `StandardFieldKeyRegistry`(신규, target_type별 허용 키 목록을 코드로 관리)가 생성/수정 시 검증한다.

#### B.3 API (요약)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/form-layouts/{targetType}` | 레이아웃 조회(섹션+필드 트리, 설정화면용) | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/config/form-layouts/{targetType}` | 레이아웃 전체 치환(섹션/필드 순서 일괄 저장) | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/form-layouts/{targetType}` | 폼 렌더링용 조회 | VIEWER+ |

레이아웃이 아직 한 번도 설정되지 않은 프로젝트+target_type은 "기본 레이아웃"(표준 필드를 코드에 정의된 기본 순서로, 섹션 없이 일렬로)으로 동작한다 — 즉 **설정하지 않으면 기존과 동일한 화면**이 그대로 보인다(하위 호환).

#### B.4 화면
`/projects/:projectId/settings`에 "폼 레이아웃" 탭 추가. target_type 선택 → 섹션 추가/삭제, 섹션 내 필드 드래그 순서 변경, 필드 노출 여부 토글.

---

### C. 열거형 (Enumeration) 설정

#### C.1 범위 — 반드시 지켜야 하는 제한
제품 오너 요청 예시("기본 제공 PRIORITY 외에 프로젝트 전용 값 추가")는 글자 그대로 읽으면 **기존 고정 Java enum(`Priority`) 자체를 프로젝트별로 확장**하자는 것이다. 이를 위해서는 `requirements.priority`/`issues.priority`/`test_cases.priority` 컬럼에 걸린 DB CHECK 제약(`CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL'))`)을 완화해야 한다 — 컬럼 단위 제약이라 **한 프로젝트라도 PRIORITY를 확장하면 전체 프로젝트에 대해 이 컬럼의 DB 레벨 안전장치가 느슨해진다**는 트레이드오프가 생긴다(§"결과" 참고).

따라서 이번 설계는 두 단계로 나눈다.

- **지금 포함하는 범위**: (1) `PRIORITY`의 프로젝트별 값 확장(DB CHECK 완화 + 애플리케이션 레벨 검증으로 이전), (2) 커스텀 필드(A)의 `SINGLE_SELECT`/`MULTI_SELECT` 전용 선택지 목록 관리.
- **지금 포함하지 않는 범위(명시적 제외)**: `RequirementStatus`/`IssueStatus`/`TestCaseStatus`의 값 자체를 프로젝트별로 추가/삭제하는 것. 상태값은 승인 게이트(`approval_requests`가 특정 상태 전이를 전제로 함), 대시보드 집계, 감사 로그 등 여러 곳이 고정된 상태 값 집합을 전제로 코드가 작성돼 있어, 값 집합 자체를 동적으로 만들면 영향 범위를 전부 재검토해야 한다. **상태의 "전이 규칙"을 프로젝트별로 제한하는 것은 D(워크플로우)에서 다루지만, 상태 "값" 자체의 추가는 이번 ADR 범위 밖이며 별도 ADR이 필요하다.** 이 제한을 두기 위해 `base_enum` 컬럼의 CHECK는 스키마상 4개 값을 모두 허용하도록 느슨하게 정의해두되(향후 확장 여지), 서비스 레이어가 지금은 `PRIORITY` 외 값으로의 집합 생성을 거부한다.

#### C.2 DB 스키마

**`project_enumeration_sets`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| enum_key | VARCHAR(50) | NOT NULL (예: `PRIORITY`, 또는 커스텀 필드 전용이면 `SEVERITY` 같은 임의 키) |
| base_enum | VARCHAR(30) | NULL 허용, CHECK IN ('PRIORITY','REQUIREMENT_STATUS','ISSUE_STATUS','TEST_CASE_STATUS') — 이 집합이 기존 고정 Java enum 중 무엇을 확장하는지. NULL이면 커스텀 필드 전용(기존 enum과 무관한 새 열거형) |
| name | VARCHAR(100) | NOT NULL (표시명) |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (project_id, enum_key)

> **서비스 레이어 제약(DB 제약이 아님)**: `base_enum`이 `REQUIREMENT_STATUS`/`ISSUE_STATUS`/`TEST_CASE_STATUS`인 집합의 생성은 이번 버전에서 `EnumerationSetService`가 거부한다(§C.1 제외 범위). `base_enum='PRIORITY'`와 `base_enum=NULL`(커스텀 필드 전용)만 생성 가능.

**`project_enumeration_values`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| enumeration_set_id | BIGINT | FK → project_enumeration_sets.id, ON DELETE CASCADE, NOT NULL |
| value_key | VARCHAR(50) | NOT NULL (코드에서 쓰이는 값, 예: `BLOCKER`) |
| label | VARCHAR(100) | NOT NULL (화면 표시 라벨) |
| display_order | INTEGER | NOT NULL DEFAULT 0 |
| is_system_default | BOOLEAN | NOT NULL DEFAULT false (true면 기존 고정 Java enum 값을 그대로 미러링한 행 — 삭제 금지, 라벨/순서만 수정 가능) |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('ACTIVE','DEPRECATED'), DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (enumeration_set_id, value_key)

#### C.3 기존 테이블 영향 — 반드시 검토할 하위 호환성 이슈
`base_enum='PRIORITY'` 집합을 프로젝트가 처음 만드는 순간, `requirements.priority`/`issues.priority`/`test_cases.priority`의 기존 CHECK 제약(`chk_...`형, V1__init.sql에 정의)을 **완화**해야 한다. 컬럼 제약은 프로젝트 단위로 걸 수 없으므로(한 컬럼, 모든 프로젝트 공유), 이 기능이 구현되는 순간부터 다음과 같이 검증 책임이 이동한다.

| 이전(현재) | 이후(이 ADR 구현 시) |
|---|---|
| DB CHECK 제약이 `priority` 값의 최종 방어선 | DB CHECK 제약 제거, `EnumerationValueValidator.requireValidValue(projectId, "PRIORITY", value)`(신규, `com.lightalm.enumeration`)가 유일한 검증 지점 |
| — | 프로젝트가 `PRIORITY` 집합을 만들지 않았으면(기본 상태) 이 validator는 기존 Java `Priority` enum 값으로 검증 — **즉 커스터마이징하지 않은 프로젝트는 동작이 전혀 바뀌지 않는다** |
| — | 프로젝트가 `PRIORITY` 집합을 만들면(최초 생성 시 LOW/MEDIUM/HIGH/CRITICAL 4개가 `is_system_default=true`로 자동 시드됨) 그 프로젝트는 `project_enumeration_values`의 ACTIVE 값 목록으로 검증 |

이 설계는 ADR-009(`projectKey` 정규식 완화 시 "SQL Injection은 파라미터 바인딩으로 이미 방어되고 `@Pattern`은 입력 검증일 뿐"이라고 책임 소재를 구분했던 것)와 같은 방식으로, **"DB가 막아준다"는 안전망이 "애플리케이션이 막아준다"로 이동하는 것임을 명시적으로 인지**하고 진행한다(리스크는 "결과" 섹션에 기록).

마이그레이션 영향(참고용, 실제 파일은 생성하지 않음):
```sql
-- (제안, 번호는 구현 시점 확정) 기존 chk_requirements_priority 등을 제거
ALTER TABLE requirements DROP CONSTRAINT IF EXISTS chk_requirements_priority;
ALTER TABLE issues DROP CONSTRAINT IF EXISTS chk_issues_priority;
ALTER TABLE test_cases DROP CONSTRAINT IF EXISTS chk_test_cases_priority;
-- status 컬럼의 CHECK 제약은 그대로 유지한다 — §C.1에서 상태값 확장은 이번 범위에서 제외했기 때문
```
> 실제 제약명(`chk_requirements_priority` 등)은 구현 시점에 `V1__init.sql` 원문을 확인해 정확히 맞춘다. 이 ADR은 명칭 추정치만 제공한다.

#### C.4 API (요약)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/enumerations` | 집합 목록 | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/enumerations` | 집합 생성 `{enumKey,baseEnum?,name}` — `baseEnum`은 `PRIORITY` 또는 생략만 허용 | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/enumerations/{id}/values` | 값 추가 `{valueKey,label,displayOrder}` | PROJECT_ADMIN+ |
| PUT/DELETE | `/api/projects/{projectId}/config/enumerations/{id}/values/{valueId}` | 값 수정/소프트삭제(`is_system_default=true`면 거부) | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/enumerations/{enumKey}/values` | 폼 렌더링용 활성 값 목록(커스텀 필드 SELECT 옵션, PRIORITY 드롭다운 등) | VIEWER+ |

#### C.5 화면
`/projects/:projectId/settings`에 "열거형" 탭 추가. `PRIORITY` 확장 집합(있다면)과 커스텀 필드용 집합 목록을 함께 보여주고, 각 집합의 값 목록을 추가/수정/비활성화.

---

### D. 워크플로우 (상태 전이 규칙) 설정

#### D.1 범위 — "범용 엔진이 아니라 전이 매트릭스 설정"
대상은 `REQUIREMENT`/`ISSUE` 두 target_type만(기존 `release_items`/`approval_requests`/`git_links`/`jenkins_builds`가 이미 이 두 타입만 다루는 패턴과 일치, GLOSSARY §2 그룹 2). `TEST_CASE`(`DRAFT`/`READY`/`DEPRECATED` 3상태, 이미 단순함)는 이번 범위에서 제외한다.

지원하는 것: "상태 A에서 상태 B로 전이 가능한가(참/거짓)" + "전이에 필요한 최소 프로젝트 역할". 지원하지 않는 것(범용 워크플로우 엔진과의 경계선): 필드 값에 따른 조건 분기, 전이 시 자동 액션(알림·필드 자동 변경), 다단계 승인자 체인(이건 이미 §1.2 승인 워크플로우/§1.2 v3 리뷰 사이클이 각자의 좁은 범위에서 다룬다 — 중복 구현하지 않음).

#### D.2 DB 스키마

**`workflow_transition_rules`**
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| from_status | VARCHAR(20) | NOT NULL (해당 target_type의 Java enum 값 중 하나 — DB CHECK 아님, 서비스 레이어 화이트리스트 검증. `RequirementStatus`/`IssueStatus`가 서로 다른 enum이라 공용 CHECK로 표현 불가) |
| to_status | VARCHAR(20) | NOT NULL |
| allowed_role | VARCHAR(20) | NULL 허용, CHECK IN ('PROJECT_ADMIN','MEMBER','VIEWER') (NULL이면 기존과 동일하게 `MEMBER+`) |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| CHECK | | `from_status <> to_status` (자기 자신으로의 "전이"는 의미 없음) |

UNIQUE (project_id, target_type, from_status, to_status)

#### D.3 적용(enforcement) 로직 — 보수적 기본값(deny가 아니라 "미설정 시 자유 전이")
기존 동작을 깨지 않는 것이 최우선이므로, 다음 규칙을 둔다.

- 프로젝트가 특정 `target_type`에 대해 **규칙을 하나도 등록하지 않았다면** → 지금과 동일하게 **모든 상태 간 자유 전이**(`01-scope.md` §1.3 "그 외 모든 상태 전이는 여전히 자유 전이" 원칙 그대로 유지). 즉 **이 기능을 쓰지 않는 프로젝트는 100% 하위 호환**.
- 규칙을 **하나 이상 등록하면** 그 프로젝트의 그 target_type은 **화이트리스트 모드**로 전환되어, 등록된 (from, to) 조합만 허용하고 나머지는 거부한다(기본 거부).
- 기존 승인 게이트(§3.17 `approval_requests`, `요구사항 DRAFT→APPROVED`)와는 **AND 조건**으로 공존한다: 워크플로우 규칙이 활성화된 프로젝트에서도 `DRAFT→APPROVED` 전이는 (1) 먼저 승인 요청이 `APPROVED`로 결정되어 있어야 하고, (2) 그 다음 워크플로우 화이트리스트에도 그 전이가 등록돼 있어야 한다. 둘 중 하나라도 막으면 전이는 거부된다.
- 시스템 `ADMIN`과 해당 프로젝트의 `PROJECT_ADMIN`은 **화이트리스트 규칙과 무관하게 항상 모든 전이가 허용된다** — 관리자가 자신이 설정한 규칙 때문에 스스로 잠기는 상황(lock-out)을 방지하기 위한 명시적 예외(ADR-011의 라이센스 ADMIN 로그인 예외와 동일한 설계 사유).

신규 정책 클래스 `WorkflowTransitionPolicy.requireAllowedTransition(projectId, targetType, fromStatus, toStatus, actorUserId)`가 기존 `PATCH .../status` 처리 경로에 훅으로 들어간다. 내부적으로 `ProjectMemberService.requireRole(...)`을 재사용해 `allowed_role` 조건을 검사한다.

#### D.4 API (요약)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/config/workflow-rules?targetType=` | 규칙 목록(없으면 "자유 전이 모드"임을 함께 응답) | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/config/workflow-rules` | 규칙 추가 `{targetType,fromStatus,toStatus,allowedRole?}` | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}/config/workflow-rules/{ruleId}` | 규칙 삭제(전부 삭제하면 그 target_type은 다시 자유 전이 모드로 복귀) | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/workflow-rules/{targetType}/{fromStatus}` | 특정 상태에서 전이 가능한 다음 상태 목록(화면의 "상태 변경" 드롭다운 구성용) | VIEWER+ |

#### D.5 기존 설계 문서와의 충돌 지점 — 반영 필요
`05-frontend.md` §5.5(상태 Workflow 차트)는 현재 다음과 같이 서술돼 있다: "01-scope.md §1.3에서 커스텀 워크플로우 엔진은 명시적으로 스코프 밖이다 ... 여기서 추가하는 Workflow 차트는 고정되어 있는 상태 목록을 다이어그램으로 보여주기만 하는 순수 시각화 컴포넌트다." 이 ADR이 승인되면 이 서술은 더 이상 정확하지 않다 — 해당 차트는 이제 "설정된 화이트리스트가 있으면 그 화이트리스트를, 없으면 기존처럼 전체 자유 전이 다이어그램을" 보여주도록 갱신해야 한다. 이 반영은 이번 ADR 범위에 포함하지 않지만, 구현 전 architect가 05-frontend.md §5.5를 갱신할 때 반드시 함께 처리해야 할 항목으로 기록한다.

#### D.6 화면
`/projects/:projectId/settings`에 "워크플로우" 탭 추가. target_type 선택 → 상태 전이 매트릭스(행=from, 열=to) 체크박스 UI + 각 셀의 `allowed_role` 선택. 규칙이 하나도 없으면 "현재 자유 전이 모드입니다" 안내 배너 표시.

---

## 결과 (Consequences)

### 장점
- 네 기능이 독립 테이블/독립 API/독립 화면 탭으로 분리되어 있어, Phase 20~23 중 일부만 먼저 구현해도 나머지에 영향이 없다.
- 커스터마이징하지 않는 프로젝트(대다수일 것으로 예상)는 기존 동작이 전혀 바뀌지 않는다 — 커스텀 필드/폼 레이아웃/워크플로우 규칙 모두 "미설정 시 기존과 동일" 원칙을 지켰다.
- 기존 다형 연관 검증(`PolymorphicTargetValidator`, ADR-010)과 권한 검사(`ProjectMemberService.requireRole`, 06-auth.md) 패턴을 그대로 재사용해 새 인프라를 만들지 않았다.

### 단점 / 리스크
- **DB 안전망 약화(C. 열거형)**: `PRIORITY` 확장 기능이 활성화되는 순간부터 `requirements`/`issues`/`test_cases`의 `priority` 컬럼은 DB CHECK 제약을 잃고 애플리케이션 레벨 검증(`EnumerationValueValidator`)에만 의존한다. 이 validator를 호출하지 않는 코드 경로(예: 향후 추가되는 bulk import, 관리 스크립트 등)가 생기면 잘못된 값이 그대로 저장될 수 있다 — 구현 시 이 validator를 **모든** priority 쓰기 경로(생성/수정 API, 테스트, 향후 import 기능)에 빠짐없이 적용해야 한다는 점을 developer에게 명시적으로 전달해야 한다.
- **EAV 패턴의 일반적 트레이드오프(A. 커스텀 필드)**: `custom_field_values.value`가 `TEXT`이므로 DB 레벨 타입 검증(숫자/날짜 형식 등)이 없다 — 전부 애플리케이션 레벨에서 `data_type`에 맞게 파싱/검증해야 한다. 또한 커스텀 필드 기준 검색/정렬/집계는 표준 컬럼보다 느리고 쿼리가 복잡해진다(이번 설계는 목록 화면의 커스텀 필드 검색/정렬 기능까지는 포함하지 않는다 — 필요해지면 별도 ADR).
- **워크플로우 화이트리스트 모드의 운영 리스크(D)**: `PROJECT_ADMIN`이 규칙을 설정하다가 특정 상태로 전이할 방법을 실수로 모두 막아버리면(예: `IN_PROGRESS`에서 나가는 규칙을 하나도 안 만듦) 해당 상태에 멈춘 항목들이 고착된다. `PROJECT_ADMIN`/시스템 `ADMIN` 예외로 복구 경로는 열어뒀지만(§D.3), 일반 `MEMBER`는 영향을 받을 수 있다는 점을 화면에 명확히 경고 문구로 안내해야 한다.
- **범위를 넘어서는 추가 요청 가능성**: 상태값(Status) 자체의 프로젝트별 확장은 이번 ADR에서 의도적으로 제외했다(§C.1). 제품 오너가 이후 이것까지 요청하면, 승인 게이트/대시보드/감사 로그 등 상태값에 의존하는 기존 로직 전체를 다시 검토하는 별도 ADR이 필요하다 — 지금 설계에 끼워넣지 않았다.
- **`01-scope.md` 미반영 상태**: 이 ADR은 설계 제안이며, **구현 전에 반드시 `01-scope.md` §1.3에서 "커스텀 워크플로우 엔진 제외"와 "커스텀 필드 제외" 두 항목의 비스코프 지정을 해제(또는 이 ADR이 정의한 좁은 범위로 재한정하는 문구로 교체)하고, §1.2에 네 기능을 핵심 스코프로 추가해야 한다.** 이번 작업에서는 `01-scope.md` 파일 자체를 수정하지 않았다(제품 오너 승인 전이므로).
- `03-data-model.md`/`04-api.md`/`05-frontend.md`/`GLOSSARY.md`도 아직 갱신하지 않았다 — 승인 후 architect가 별도로 반영한다(ADR-011과 동일한 절차).

### developer에게 위임할 구현 범위 (설계 승인 + 스코프 반영 후)
1. **Phase 20 (커스텀 필드)**: `com.lightalm.customfield` 패키지 — `CustomFieldDefinition`/`CustomFieldValue` 엔티티(신규 코드이므로 `@Setter` 금지, 도메인 동사 메서드 사용), Repository, `CustomFieldDefinitionService`/`CustomFieldValueService`, Controller, DTO. `custom_field_definitions`/`custom_field_values` 마이그레이션.
2. **Phase 21 (폼 레이아웃)**: `com.lightalm.formlayout` 패키지 — `FormLayout`/`FormLayoutSection`/`FormLayoutField` 엔티티, `StandardFieldKeyRegistry`(target_type별 표준 필드 화이트리스트), Controller/DTO. `form_layouts`/`form_layout_sections`/`form_layout_fields` 마이그레이션.
3. **Phase 22 (열거형)**: `com.lightalm.enumeration` 패키지 — `ProjectEnumerationSet`/`ProjectEnumerationValue` 엔티티, `EnumerationValueValidator`(PRIORITY 검증의 새 단일 진입점), 기존 `priority` CHECK 제약 제거 마이그레이션(제약명은 구현 시점에 `V1__init.sql` 원문 확인 후 정확히 지정). **이 Phase 구현 시 반드시 기존 요구사항/이슈/테스트케이스 생성·수정 경로 전체에서 `priority` 값 검증이 새 validator를 거치도록 리팩터링해야 한다 — 빠뜨리면 DB 안전망 없이 잘못된 값이 저장될 수 있다.**
4. **Phase 23 (워크플로우 전이 규칙)**: `com.lightalm.workflow` 패키지 — `WorkflowTransitionRule` 엔티티, `WorkflowTransitionPolicy`, 기존 `PATCH .../requirements/{id}/status`·`PATCH .../issues/{id}/status` 처리 경로에 정책 훅 추가(기존 approval_requests 게이트와의 AND 조건 순서 포함).
5. 프론트: `/projects/:projectId/settings`에 탭 4개 추가(필드/폼 레이아웃/열거형/워크플로우), 요구사항/이슈/테스트케이스 생성·수정 폼이 폼 레이아웃 설정을 반영해 동적으로 렌더링되도록 수정.
6. qa-tester 검증 포인트: (a) 미설정 프로젝트의 기존 동작 무변화 회귀 테스트, (b) PRIORITY 확장 후 DB 제약 없이도 잘못된 값이 애플리케이션 레벨에서 차단되는지, (c) 워크플로우 화이트리스트 모드에서 PROJECT_ADMIN/시스템 ADMIN 예외가 실제로 모든 전이를 통과시키는지, (d) 승인 게이트 + 워크플로우 규칙 AND 조건이 올바르게 동작하는지(둘 중 하나만 통과해도 거부되는지), (e) 커스텀 필드 소프트 삭제 후 기존 값이 상세 화면에서 읽기 전용으로 유지되는지.

---

## 참고
- `docs/01-requirements/01-scope.md` §1.2, §1.3, §1.4
- `docs/01-requirements/02-competitive-reference.md`
- `docs/03-process/06-auth.md`(`ProjectMemberService.requireRole`)
- `docs/00-meta/GLOSSARY.md` §2(다형 연관 패턴), §3(Enum 값), §5(동사 규칙) — 승인 시 `CustomFieldDefinition` 등 신규 개념 행 추가 필요
- `docs/05-history/adr/ADR-008-v3-경쟁ALM툴-참고-스코프확장.md`(스코프 확장 ADR 절차, Phase 분할 선례)
- `docs/05-history/adr/ADR-009-projectKey-제약완화.md`(DB 제약 → 애플리케이션 검증 책임 이전 선례)
- `docs/05-history/adr/ADR-010-다형연관-검증로직-통합.md`(`PolymorphicTargetValidator` 재사용)
- `docs/05-history/adr/ADR-011-회원가입-및-라이센스관리-스코프확장.md`(동일한 "ADMIN lock-out 방지 예외" 설계 사유, 동일한 스코프 확장 절차)
- `backend/src/main/resources/db/migration/V1__init.sql`(기존 CHECK 제약명 확인 근거)
