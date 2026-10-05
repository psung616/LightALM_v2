> Owner: architect | Status: Accepted (설계 완료, **Phase 20~23(커스텀 필드/폼 레이아웃/열거형/워크플로우) 전부 구현 완료 — ADR-012 전체 완료**. Phase 20~21은 qa-tester 검증 통과(반려 1건 수정 포함, 2026-10-05). **Phase 22(열거형)는 qa-tester 반려 2건(DEPRECATED PRIORITY 보유 항목 수정 차단, enumKey/baseEnum 불일치 집합) 수정 완료 — qa-tester 재검증 통과(2026-10-05). Phase 23(워크플로우)은 치명적 버그 반려·수정 후 qa-tester 최종 재승인 완료(2026-10-05)**) | Date: 2026-10-03
>
> **[2026-10-04 구현 각주]** developer가 Phase 20(§A 커스텀 필드)을 구현 완료했다. 실제 마이그레이션 번호, 설계 대비 차이점은 §A 본문 각주와 `docs/00-meta/CURRENT-STATE.md` §1을 참고. Phase 21(폼 레이아웃)/22(열거형)/23(워크플로우)은 아직 미구현 상태이며 이 ADR의 나머지 설계는 그대로 유효하다.
>
> **[2026-10-05 qa-tester 반려 수정 각주]** qa-tester 검증에서 "DEPRECATED 필드는 기존 값 수정까지 영구 거부"하는 과도한 구현이 반려 사유로 지적되어 수정했다(기존 값 수정은 허용, 신규 생성만 거부). 상세는 §A 본문 각주 참고.
>
> **[2026-10-05 Phase 21 구현 완료 각주]** developer가 §B(폼 레이아웃)를 구현 완료했다. 설계와의 차이점만 기록한다:
> - 실제 마이그레이션 파일은 `V14__create_form_layout_tables.sql`이다(당시 최신이 V13이었음).
> - 패키지: `com.lightalm.formlayout.{domain,repository,service,api,dto}`. 엔티티는 `FormLayout`/`FormLayoutSection`/`FormLayoutField`, `FieldSource` enum.
> - "전체 치환" 저장을 설계보다 단순하게 구현했다: PUT 요청이 오면 기존 `FormLayout`(있다면) 전체를 삭제하고 완전히 새로 생성해 저장한다(부분 업데이트/diff 로직 없음). 섹션/필드는 `FormLayout`의 애그리게잇 안에서만 생성/삭제되고 독립 Repository를 두지 않았다.
> - `custom_field_id`는 설계대로 `custom_field_definitions.id`에 대한 DB FK(`ON DELETE CASCADE`)를 걸었지만, JPA 엔티티 쪽은 패키지 경계를 유지하기 위해 연관관계가 아니라 평범한 `Long` 컬럼으로 매핑했다(ADR-012 §A의 `enumerationSetId` 처리와 동일한 패턴). 같은 프로젝트+target_type 소속 검증은 `FormLayoutService`가 `CustomFieldDefinitionRepository`를 직접 조회해서 수행한다.
> - `StandardFieldKeyRegistry`의 실제 화이트리스트(기존 Create/Update 요청 DTO를 코드에서 확인해 정함):
>   - `REQUIREMENT`: `title, description, type, priority, requirementLevel, parentRequirementId, assignedTo, dueDate`
>   - `ISSUE`: `title, description, type, priority, assigneeId, dueDate`
>   - `TEST_CASE`: `title, description, preconditions, steps, expectedResult, priority, requirementId, status`(`status`는 `UpdateTestCaseRequest`에만 실존 — 요구사항/이슈의 상태는 별도 상태변경 API로만 바뀌므로 포함하지 않았다)
> - 설계 문서에 없던 편의 필드를 응답에 추가했다: `FormLayoutFieldResponse`에 `standardFieldLabel`/`customFieldLabel`/`customFieldDataType`를 넣어 프론트가 추가 조회 없이 바로 렌더링하게 했다.
> - §B.1 "표준 필드를 완전히 숨길 수 없다" 제약은 "요청에 포함된 STANDARD 필드 중 최소 1개는 `visible=true`여야 한다"로 구체화해서 구현했다(요청에 STANDARD 필드가 하나도 없거나 전부 `visible=false`면 거부).
> - 프론트엔드: `/projects/:projectId/settings`에 "폼 레이아웃" 탭 추가(`FormLayoutSettingsTab`) — 섹션 추가/삭제/순서 이동, 필드 추가(표준/커스텀)/제거/순서 이동(상하 버튼)/노출 토글. "모든 표준 필드 키 목록"을 돌려주는 API가 없어(§B.3이 3개 엔드포인트만 정의) 프론트에 `StandardFieldKeyRegistry`와 동일한 목록을 상수로 중복 보관했다 — 백엔드 레지스트리가 바뀌면 같이 갱신해야 하는 알려진 동기화 포인트.
> - 요구사항/이슈/테스트케이스의 생성 폼(목록 화면 모달)과 수정 폼(상세 화면 "기본 정보" 섹션)에 `DynamicStandardFieldsLayout` 컴포넌트를 적용해, 레이아웃이 설정된 경우(`layout.id != null`) 섹션/순서/노출을 반영하고, 설정되지 않은 경우(`layout.id == null`, 기본 레이아웃)에는 기존과 동일한 고정 마크업(`fallback`)을 그대로 렌더링한다 — 회귀 없음을 그대로 보장하는 방식으로 구현(기존 JSX를 그대로 fallback에 복제).
> - **알려진 범위 제한**: 생성/수정 폼에서는 레이아웃에 배치된 CUSTOM(커스텀 필드) 항목을 렌더링하지 않는다. 커스텀 필드는 대상(target_id)이 있어야 값을 저장할 수 있어 생성 시점에는 적용할 수 없고(Phase 20부터의 기존 제약), 수정 화면에서도 계속 별도의 `CustomFieldsPanel`(상세 화면 전용)이 담당한다 — 설정 화면에서 CUSTOM 필드를 레이아웃에 배치하는 것 자체는 가능하고 저장/조회 API에는 정상 반영되지만, 생성/수정 폼 렌더링에는 아직 영향을 주지 않는다. **[2026-10-05 qa-tester 반려 수정]** 최초 구현은 이 제한을 ADR 각주에만 기록하고 설정 화면 UI에는 아무 표시도 하지 않아, PROJECT_ADMIN이 CUSTOM 필드를 배치하고 "저장되었습니다" 성공 피드백까지 받는데도 그 배치가 어디에도 시각적 영향을 주지 않는 문제가 있었다(qa-tester 지적: Phase 20이 `CustomFieldsPanel.tsx`에 "열거형 옵션 선택 UI는 추후 Phase 22 지원" 디스클레이머를 단 선례를 Phase 21이 따르지 않음). 기능 자체(저장/조회 API)는 그대로 두고, `FormLayoutSettingsTab.tsx`에 Phase 20과 같은 톤의 명시적 디스클레이머를 추가했다 — 이미 배치된 CUSTOM 필드 행에 "(생성/수정 화면에는 아직 미반영)" 태그, "+ 커스텀 필드 추가" 드롭다운 아래에 "커스텀 필드 배치는 저장/조회에는 정상 반영되지만, 생성/수정 화면에는 아직 반영되지 않습니다 — 추후 지원 예정" 안내문. 렌더링 자체를 구현하지는 않았으므로(범위 제한은 해결이 아니라 명시로 처리) 여전히 CUSTOM 필드 배치는 설정 화면 밖에서는 무동작이다 — `docs/00-meta/CURRENT-STATE.md` §6에 리스크로 기록.
> - 단위 테스트 6개(`FormLayoutServiceTest`) 추가, 전체 `mvnw test` 99개 통과(위 qa-tester 반려 수정은 프론트엔드 전용이라 백엔드 테스트는 변경 없이 그대로 99개 통과). 로컬 docker-compose 스택(실제 Postgres)에 마이그레이션을 적용해 API 전체를 curl로 end-to-end 수동 검증했다(기본 레이아웃 반환, 전체 치환 저장/재조회, standardFieldKey 화이트리스트 밖 값 거부, 표준 필드 전부 숨김 거부, 다른 프로젝트/다른 target_type 소속 custom_field_id 거부).
>
> **[2026-10-05 Phase 22 구현 완료 각주]** developer가 §C(열거형)를 설계 그대로 구현했다. §C.1의 범위 제한(PRIORITY 확장 + 커스텀 필드 전용 선택지 관리만 포함, REQUIREMENT_STATUS/ISSUE_STATUS/TEST_CASE_STATUS는 서비스 레이어가 명시적으로 거부)을 그대로 지켰다. 설계와의 차이점 및 구현 각주:
> - 실제 마이그레이션 파일은 `V15__create_enumeration_tables.sql`이다(당시 최신이 V14였음). 이 파일 하나에 `project_enumeration_sets`/`project_enumeration_values` 생성, priority CHECK 제약 제거, `custom_field_definitions.enumeration_set_id` 고아값 정리+FK 추가를 모두 담았다(§C.3이 예고한 순서 그대로).
> - `V1__init.sql` 원문을 직접 확인해 실제 제약명을 정확히 썼다: `chk_requirements_priority`, `chk_issues_priority`(V1), `chk_test_cases_priority`(V3) — 전부 `DROP CONSTRAINT IF EXISTS`로 제거했고 `status` 컬럼 CHECK(`chk_requirements_status`/`chk_issues_status`/`chk_test_cases_status`)는 그대로 유지했다(§C.1, 상태값 확장은 범위 밖).
> - `custom_field_definitions.enumeration_set_id` 고아값 정리: `UPDATE ... SET enumeration_set_id = NULL WHERE NOT EXISTS (SELECT 1 FROM project_enumeration_sets ...)`를 FK 추가 전에 실행했다. 로컬 docker-compose 스택(기존 운영과 분리된 로컬 Postgres 볼륨)에서 적용 시점 기준 고아값은 0건이었다(`SELECT count(*) FROM custom_field_definitions WHERE enumeration_set_id IS NOT NULL` → 0) — 즉 이번 환경에서는 정리할 레코드가 실제로 없었지만, 문장 자체는 운영 DB(사내 공용 Postgres, 실제 Phase 20 사용 이력이 있을 수 있는 환경)에 적용될 때를 대비해 그대로 포함해뒀다. FK는 `fk_custom_field_definitions_enumeration_set FOREIGN KEY (enumeration_set_id) REFERENCES project_enumeration_sets(id) ON DELETE SET NULL`로 걸었다.
> - 패키지: `com.lightalm.enumeration.{domain,repository,service,api,dto}`. 엔티티는 `ProjectEnumerationSet`/`ProjectEnumerationValue`, enum은 `BaseEnumType`/`EnumerationValueStatus`. `@Setter` 없음, 상태 변경은 `updateLabelAndOrder()`/`deprecate()` 도메인 메서드로만.
> - `is_system_default=true` 값의 삭제 거부는 엔티티가 아니라 서비스(`EnumerationSetService.deprecateValue`)가 호출 전에 판단한다(`CustomFieldDefinition`과 다른 지점 — 이 엔티티는 상태 전이 메서드만 제공하고 거부 판단 책임은 서비스에 둔 것이 설계 문서 §C.2의 "라벨/순서만 수정 가능, 삭제 금지"를 API 레이어(PUT은 항상 허용, DELETE만 거부)와 일치시키기 쉬웠다). 라벨/순서 수정(`updateValue`)은 `is_system_default` 여부와 무관하게 항상 허용된다 — API 표(§C.4) 괄호 안 "is_system_default=true면 거부"는 DELETE에만 해당하고 PUT에는 적용되지 않는다고 판단했다(§C.2 스키마 설명과 developer 작업지시서 둘 다 "삭제 거부"로만 명시했기 때문).
> - `EnumerationValueValidator.requireValidValue(projectId, enumKey, value)`를 신설해 `RequirementService`/`IssueService`/`TestCaseService`의 create/update 전부에서 priority 값을 저장하기 직전에 호출하도록 리팩터링했다. 프로젝트가 해당 enumKey 집합을 만들지 않았으면 기존 Java `Priority` enum(LOW/MEDIUM/HIGH/CRITICAL)으로 fallback 검증한다 — PRIORITY 외 enumKey는 fallback이 없어 집합 미생성 시 전부 거부된다(지금은 PRIORITY만 fallback이 정의돼 있다는 뜻이고, 커스텀 필드 전용 열거형은 이 validator의 대상이 아니다).
> - **엔티티/DTO의 `priority` 타입을 기존 Java enum `Priority`에서 `String`으로 전면 변경했다** — ADR §C.3이 명시한 대로 DB CHECK 제거만으로는 부족했다. Jackson이 JSON 본문을 `Priority` 고정 enum으로 역직렬화하면 애당초 "BLOCKER" 같은 새 값이 요청 단계에서 400으로 막혀 PRIORITY 확장 기능 자체가 동작할 수 없었기 때문이다(이 refactor는 설계 문서에 문자 그대로 적혀 있지 않았지만, "기존 priority 검증 경로 전체를 validator로 통과시켜라"는 작업지시의 전제 조건으로 판단해 developer가 범위에 포함했다). 영향 범위: `Requirement`/`Issue`/`TestCase` 엔티티의 `priority` 컬럼 매핑(`@Enumerated` 제거, `String` + 기존 기본값은 `Priority.MEDIUM.name()`), `Create*Request`/`Update*Request`/`*Response` DTO 6개, 목록 조회 컨트롤러의 `priority` 쿼리 파라미터 3곳. 기존 Java `Priority` enum 자체는 삭제하지 않고 "집합 미생성 시 fallback 검증 소스"와 "자동 시드 소스"로 계속 사용한다.
> - API는 설계 문서(§C.4) 그대로: `GET/POST .../config/enumerations`, `POST .../config/enumerations/{id}/values`, `PUT/DELETE .../config/enumerations/{id}/values/{valueId}`, `GET .../enumerations/{enumKey}/values`. 마지막 조회 API는 집합이 없는 프로젝트의 `enumKey=PRIORITY` 요청에 대해 `id=null`인 응답으로 기존 Java enum 4개 값을 그대로 돌려준다(프론트가 집합 유무와 무관하게 동일한 모양의 응답을 받을 수 있게 한 설계 외 세부 선택).
> - **알려진 범위 제한(의도적, 작업 지시서 명시)**: 프론트엔드 "열거형" 탭(§C.5)과, 요구사항/이슈/테스트케이스 생성·수정 화면에서 PRIORITY 드롭다운을 동적 값 목록으로 렌더링하는 작업은 이번 범위에 포함하지 않았다 — developer 작업 지시서가 DB/백엔드/리팩터링/테스트만 명시했고 프론트엔드 섹션이 없었다. 따라서 PROJECT_ADMIN이 API로 PRIORITY 값을 확장해도 현재 프론트 화면(고정된 LOW/MEDIUM/HIGH/CRITICAL 드롭다운)에는 아직 반영되지 않는다 — API는 정상 동작하지만 화면에서 확장된 값을 선택할 UI가 없다. 커스텀 필드 SINGLE_SELECT/MULTI_SELECT 선택지를 이 열거형 집합과 연결해 드롭다운으로 렌더링하는 것도 마찬가지로 미구현이다(Phase 20 각주의 "자유 텍스트 입력" 임시 구현이 그대로 유지됨). `docs/00-meta/CURRENT-STATE.md` §6에 리스크로 기록.
> - 단위 테스트 18개 추가(`EnumerationSetServiceTest` 12개, `EnumerationValueValidatorTest` 6개) + 기존 `RequirementServiceTest`/`IssueServiceTest`/`TestCaseServiceTest`에 validator 연동/예외 전파 검증 테스트 3개 추가, 전체 `mvnw test` 120개 통과(기존 99개 + 신규 21개). 로컬 docker-compose 스택(실제 Postgres)에 V15까지 마이그레이션을 적용해 API 전체를 curl로 end-to-end 수동 검증했다(REQUIREMENT_STATUS 등 금지된 baseEnum 거부, PRIORITY 집합 생성 시 4개 시스템 기본값 자동 시드, 집합 미생성 프로젝트의 기본 Priority enum fallback 검증 및 회귀 없음, 커스텀 값 추가 후 요구사항/이슈/테스트케이스 생성 API가 새 값을 허용, is_system_default 값 삭제 거부/라벨 수정 허용, 값 비활성화 후 재사용 거부, enumKey 중복 생성 거부).
>
> **[2026-10-05 qa-tester 반려 및 수정 각주 — Phase 22, 2건]**
> 1. **(Medium) 폐기된 PRIORITY 값을 가진 항목은 다른 필드 수정까지 400으로 막혔다.** 재현: PRIORITY에 BLOCKER 추가 → 이슈를 BLOCKER로 생성 → BLOCKER 값 DELETE(DEPRECATED) → 같은 이슈의 제목만 PUT → `400 "유효하지 않은 PRIORITY 값입니다: BLOCKER"`. 원인: `IssueService`/`RequirementService`/`TestCaseService`의 update가 priority를 바꾸지 않아도 `requireValidValue`로 ACTIVE 여부를 검사했다. 수정: `EnumerationValueValidator.requireValidValueForChange(projectId, enumKey, currentValue, newValue)`를 신설하고 세 서비스의 update 경로가 이를 쓰도록 바꿨다 — 값이 기존과 같으면 검증을 생략하고, 다른 값으로 바꿀 때만 ACTIVE를 요구한다. create 경로는 그대로 `requireValidValue`(폐기 값으로 신규 생성 불가). §A(Phase 20) 커스텀 필드의 DEPRECATED 처리와 같은 원칙("폐기 = 새로 고를 수 없음"이지 기존 보유 항목 잠금이 아님). 다른 값에서 폐기된 값으로 되돌리는 것은 여전히 400.
> 2. **(Medium) `enumKey=PRIORITY` + `baseEnum` 생략 집합이 프로젝트 전체의 priority 쓰기를 막았다.** 재현: `POST .../config/enumerations {"enumKey":"PRIORITY","name":"x"}` → 201(값 0개) → 이후 그 프로젝트의 항목 생성/수정이 기본값 MEDIUM 포함 전부 400. 집합 삭제 API가 없어 복구가 곤란했다. 원인: validator는 `enumKey`로 집합을 찾고, 기본값 시드는 `baseEnum=PRIORITY`일 때만 해 두 판별 기준이 어긋났다. 수정: `EnumerationSetService.create()`가 **`enumKey=PRIORITY` ⇔ `baseEnum=PRIORITY`**를 강제한다(어느 방향으로든 어긋나면 400). **이 방식을 고른 이유**: validator의 조회 기준을 baseEnum으로 바꾸는 방안보다 변경 지점이 하나(생성 경로)로 작고, "PRIORITY 집합은 항상 기본값 4개가 시드된 상태로만 존재한다"는 불변식을 생성 시점에 보장하므로 validator·시드·조회 API(`GET .../enumerations/{enumKey}/values`)가 모두 기존 코드 그대로 일관되게 동작한다. 스키마 변경(V19) 불필요. 한계: 이 수정 이전에 이미 만들어진 불일치 집합(예: 로컬 검증 DB의 프로젝트 14)은 자동 정리되지 않는다 — 집합 삭제 API가 없으므로 생기면 DB에서 직접 정리해야 한다.
> - 단위 테스트 추가: `EnumerationValueValidatorTest` +2, `EnumerationSetServiceTest` +2, `IssueServiceTest`/`RequirementServiceTest`/`TestCaseServiceTest` 각 +1(같은 작업에서 `GlobalExceptionHandlerTest` +2 포함 `mvnw test` 199개 통과). 로컬 docker-compose Postgres + `mvnw spring-boot:run` + curl로 qa-tester 재현 시나리오 재확인: 이슈 13(BLOCKER, 폐기됨)의 제목만 수정 → 200(priority BLOCKER 유지), BLOCKER를 명시해 재전송 → 200, HIGH로 변경 → 200, 다시 BLOCKER로 변경 → 400, BLOCKER로 신규 생성 → 400, 요구사항/테스트케이스(로컬 DB에서 priority를 BLOCKER로 직접 설정해 재현)의 제목만 수정 → 200. `enumKey=PRIORITY`+baseEnum 생략 → 400, `MY_PRIORITY`+`baseEnum=PRIORITY` → 400(집합 생성 안 됨, 이후 이슈 생성 201), `SEVERITY`+baseEnum 생략 → 201, `PRIORITY`+`PRIORITY` → 201(기본값 4개 시드).

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

> **[2026-10-04 구현 완료 각주]** developer가 아래 설계를 거의 그대로 구현했다. 실제 코드와의 차이점만 기록한다:
> - 실제 마이그레이션 파일은 `V13__create_custom_field_tables.sql`이다(당시 최신이 V12였음).
> - `enumeration_set_id`는 설계대로 FK 제약 없이 `BIGINT NULL` 컬럼으로만 추가했다(§C의 `project_enumeration_sets`가 아직 없으므로). Phase 22 구현 시 그 테이블을 만든 뒤 `ALTER TABLE custom_field_definitions ADD CONSTRAINT fk_custom_field_definitions_enumeration_set FOREIGN KEY (enumeration_set_id) REFERENCES project_enumeration_sets(id) ON DELETE SET NULL;`을 추가해야 한다 — 마이그레이션 파일 상단에도 동일 주석을 남겼다.
> - 패키지: `com.lightalm.customfield.{domain,repository,service,api,dto}`.
> - 값 저장 시 서비스 레이어가 `dataType`별 형식을 검증한다(NUMBER는 `Double.parseDouble`, DATE는 ISO `LocalDate.parse`, BOOLEAN은 `true`/`false` 문자열, MULTI_SELECT는 JSON 배열 문자열 여부를 `ObjectMapper`로 검증). 이 부분은 설계 문서에 "애플리케이션 레벨에서 파싱/검증"이라고만 적혀 있었고 구체적 규칙은 없었으므로 developer가 정했다 — SINGLE_SELECT/TEXT는 추가 형식 검증 없음(자유 문자열).
> - ~~설계에는 명시되지 않았던 보강 규칙 1개를 추가했다: DEPRECATED 상태인 필드에는 새 값을 저장할 수 없다~~ — **[2026-10-05 qa-tester 반려 수정]** 최초 구현은 이 규칙을 "기존 값이 있든 없든 무조건 거부"로 과하게 적용해, 이미 값이 입력된 DEPRECATED 필드의 오타 수정 같은 정당한 수정까지 막는 버그가 있었다(qa-tester 발견). ADR 원문 "비활성 필드는 생성/수정 폼에서는 숨기지만, 값이 있는 상세 조회 화면에서는 읽기 전용으로 노출한다"는 "폼에 새 입력 옵션으로 내놓지 않는다"는 뜻일 뿐 "저장 API 자체를 영구 잠근다"는 뜻이 아니므로, 다음으로 수정했다: **해당 (field_id, target_type, target_id) 조합에 기존 `CustomFieldValue` 행이 없는 신규 생성만 거부**하고, 이미 값이 존재하는 경우의 수정(`changeValue()`)은 필드 상태(ACTIVE/DEPRECATED)와 무관하게 항상 허용한다. 즉 "읽기 전용"이 아니라 "새 옵션으로는 제공하지 않지만 기존 값은 계속 편집 가능"이 맞는 동작이다.
> - `GET .../custom-field-values` 응답은 "해당 target_type의 ACTIVE 필드 전체(값 없으면 value=null) + 가지고 있는 값이 존재하는 DEPRECATED 필드"의 합집합이다. ACTIVE인데 아직 값이 없는 필드도 포함시킨 이유는 생성/수정 폼이 이 엔드포인트 하나로 초기값(빈 값 포함)을 채울 수 있게 하기 위함이며, 이 또한 설계 문서에 구체적으로 명시되지 않았던 세부 구현 선택이다.
> - 프론트엔드: SINGLE_SELECT/MULTI_SELECT는 Phase 22(열거형)가 없어 선택 UI를 만들 수 없으므로, 이번 Phase에서는 둘 다 자유 텍스트 입력(MULTI_SELECT는 쉼표 구분 입력 → JSON 배열로 변환)으로 임시 구현했다. 커스텀 필드 렌더링은 "생성/수정 화면"이 아니라 요구사항/이슈/테스트케이스의 **상세 화면**(표준 필드 "기본 정보" 섹션 바로 뒤)에 추가했다 — 세 엔티티 모두 별도의 "생성 전용" 화면 없이 상세 화면에서 인라인 편집하는 기존 구조이기 때문.
> - DTO는 Summary/Detail을 분리했으나(`CustomFieldDefinitionSummaryResponse`/`CustomFieldDefinitionDetailResponse`), 값 응답(`CustomFieldValueResponse`)은 대상 하나당 필드 값 리스트 하나뿐이라 분리할 대상이 없어 단일 타입으로 유지했다(파일 내 주석으로 사유 기록).
> - 단위 테스트 12개(`CustomFieldDefinitionServiceTest` 5개, `CustomFieldValueServiceTest` 7개) 추가, 전체 `mvnw test` 93개 통과(qa-tester 반려 수정 후 재검증 포함). 로컬 docker-compose 스택(실제 Postgres)에 마이그레이션을 적용해 API 전체를 curl로 end-to-end 수동 검증했다(생성/중복거부/활성목록/소프트삭제/값 저장·조회/존재하지않는 대상 차단/MULTI_SELECT JSON 검증/DEPRECATED 필드의 기존 값 수정 허용·신규 생성 거부).
> - `enumeration_set_id`는 Phase 22 전까지 FK/존재 검증이 전혀 없어 어떤 값(존재하지 않는 id 포함)이든 그대로 저장된다 — qa-tester가 지적한 리스크로 `docs/00-meta/CURRENT-STATE.md` §6에 기록했다. Phase 22가 FK 제약을 추가하기 전에 기존에 저장된 고아 값을 먼저 정리해야 한다.
> - **[qa-tester 권고 반영, 2026-10-05]** Phase 22가 DB에 `fk_custom_field_definitions_enumeration_set` FK를 추가한 뒤에도 `CustomFieldDefinitionService.create()`가 `enumerationSetId`를 애플리케이션 레벨에서 사전 검증하지 않아, 존재하지 않는 값을 보내면 FK violation이 그대로 터져 `500 INTERNAL_ERROR`로 응답되는 문제가 있었다(qa-tester 2026-10-05 발견). `FormLayoutService`의 `customFieldId` 소속 검증과 동일한 패턴으로, `create()`에 `enumerationSetId != null`이면 `ProjectEnumerationSetRepository.existsByIdAndProjectId(id, projectId)`로 존재/프로젝트 소속을 먼저 확인해 실패 시 `ValidationException`(400)으로 바꿨다. `UpdateCustomFieldDefinitionRequest`에는 원래부터 `enumerationSetId` 필드가 없어(생성 후 불변) 이 수정은 `create()` 경로에만 적용된다. 단위 테스트 2개(`create_withEnumerationSetIdBelongingToSameProject_savesSuccessfully`, `create_whenEnumerationSetIdDoesNotBelongToProject_throwsValidationExceptionNotFkViolation`) 추가.



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

> **서비스 레이어 제약(DB 제약이 아님)**: `base_enum`이 `REQUIREMENT_STATUS`/`ISSUE_STATUS`/`TEST_CASE_STATUS`인 집합의 생성은 이번 버전에서 `EnumerationSetService`가 거부한다(§C.1 제외 범위). `base_enum='PRIORITY'`와 `base_enum=NULL`(커스텀 필드 전용)만 생성 가능. **[2026-10-05 qa-tester 반려 수정]** 추가로 `enum_key='PRIORITY'` ⇔ `base_enum='PRIORITY'`를 생성 시점에 강제한다(어긋나면 400) — 상세는 문서 상단 "Phase 22 qa-tester 반려 및 수정 각주" 2번.

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

> **[2026-10-05 Phase 23 구현 완료 각주 — ADR-012 전체 완료]** developer가 §D를 설계 그대로 구현했다. 설계와의 차이점만 기록한다:
> - 실제 마이그레이션 파일은 `V17__create_workflow_transition_rules.sql`이다(당시 최신이 V16이었음).
> - 패키지: `com.lightalm.workflow.{domain,repository,service,api,dto}`. 엔티티는 `WorkflowTransitionRule` 하나뿐이고(§D.2 그대로), 생성/삭제만 가능해 도메인 상태 전이 메서드가 필요 없어 `@Setter` 없이 불변 필드만 둔다.
> - 서비스 레이어를 Command/Query와 Policy로 분리했다(`docs/CLAUDE.md`의 "만능 서비스 금지" 원칙): `WorkflowTransitionRuleService`가 설정화면 CRUD(PROJECT_ADMIN+)·다음 상태 조회(VIEWER+)의 권한 검사/입력 검증/DTO 매핑을 맡고, `WorkflowTransitionPolicy`가 실제 전이 허용/거부 판단과 "다음 상태 목록 + 자유 전이 모드 여부" 순수 조회만 담당한다. 작업 지시서가 명시한 클래스는 `WorkflowTransitionPolicy` 하나였지만, CRUD까지 그 클래스에 넣으면 책임이 섞여 `WorkflowTransitionRuleService`를 추가로 분리했다.
> - `WorkflowTransitionPolicy.requireAllowedTransition(...)`의 실제 시그니처는 작업 지시서의 `actorUserId`(단순 ID) 대신 `UserPrincipal actor`를 받는다 — 시스템 ADMIN 여부(`actor.isAdmin()`) 판단에 `UserPrincipal`이 필요하고, `ProjectMemberService.requireRole(projectId, principal, minRole)` 등 기존 코드 전체가 이미 이 패턴(ID가 아니라 `UserPrincipal`을 관통시키는 방식)을 쓰고 있어 일관성을 위해 그대로 따랐다.
> - `fromStatus == toStatus`(자기 자신으로의 "전이")는 규칙 조회 전에 조건 없이 항상 허용하고 반환한다(DB CHECK `from_status <> to_status`는 저장되는 규칙 자체에 대한 제약이고, 실행 시점에 같은 상태로 "전이"를 요청하는 것은 애초에 전이가 아니라는 판단).
> - `GET .../workflow-rules/{targetType}/{fromStatus}` 응답은 설계 문서에 구체적인 바디 형식이 없어 `{freeTransitionMode: boolean, nextStatuses: string[]}`로 정의했다 — §5.5 Workflow 차트가 "자유 전이 모드"와 "화이트리스트 모드"에서 서로 다른 툴팁 문구를 보여줘야 해서(§D.5) 단순 배열만으로는 그 구분을 표현할 수 없었다. 자유 전이 모드일 때 `nextStatuses`는 해당 target_type의 전체 상태 값 중 `fromStatus`를 제외한 목록이다(REQUIREMENT는 `RequirementStatus` 6개 중 5개, ISSUE는 `IssueStatus` 5개 중 4개).
> - 기존 상태 변경 경로 3곳에 훅을 걸었다: `RequirementService.changeStatus`(기존 `DRAFT→APPROVED` 하드 차단 로직은 그대로 두고 그 아래에 훅 추가), `IssueService.changeStatus`, 그리고 **`DRAFT→APPROVED`의 실제 실행 지점인 `ApprovalService.decide()`의 `APPROVE` 분기**(이 메서드가 승인 요청을 승인 처리하면서 요구사항 상태를 직접 바꾸는 지점이다 — `RequirementService.changeStatus`는 이 전이를 영구히 거부하므로 거기엔 걸 필요가 없다).
> - 단위 테스트 20개 추가(최초 구현 시점): `WorkflowTransitionPolicyTest` 9개(자유 전이 회귀, 화이트리스트 거부, allowedRole 거부, 시스템 ADMIN/PROJECT_ADMIN lock-out 방지 예외, nextStatuses 자유/화이트리스트 모드), `WorkflowTransitionRuleServiceTest` 9개(CRUD 권한/입력 검증), 기존 `RequirementServiceTest`/`IssueServiceTest`/`ApprovalServiceTest`에 정책 거부 시 상태 불변·예외 전파를 확인하는 회귀 테스트 1개씩 추가. 이후 아래 버그 수정에서 4개가 더 추가됐다(총 161개).
> - 프론트엔드: `/projects/:projectId/settings`에 "워크플로우" 탭(`WorkflowRuleSettingsTab`, GLOSSARY §6 명명 그대로) 추가 — target_type(요구사항/이슈) 선택 → 상태 전이 매트릭스(행=from, 열=to) 체크박스 + 체크된 셀의 `allowedRole` 인라인 select. PUT 엔드포인트가 없어(§D.4) 역할 변경은 "삭제 후 새 역할로 재생성"으로 구현했다. 규칙이 없으면 "현재 자유 전이 모드입니다" 배너를 보여준다.
> - §D.5가 지적한 `WorkflowChart.tsx`(실제 파일명, GLOSSARY §6의 `StatusWorkflowChart`는 개념명이고 실제 컴포넌트 파일/함수명은 기존부터 `WorkflowChart`였다 — 이 불일치는 이번 작업 범위에서 새로 만들지 않았으므로 그대로 둔다)를 갱신했다: 기존 호출부(`RequirementDetailPage`/`IssueDetailPage`/`ProjectDashboardPage`)가 선택적 `projectId`/`targetType` prop을 추가로 넘기면, 마운트 시 그 프로젝트의 각 상태 노드에 대해 `GET .../workflow-rules/{targetType}/{fromStatus}`를 호출해 전부 자유 모드면 기존 고정 다이어그램을, 하나라도 화이트리스트 모드면 그 화살표만 그리는 동적 다이어그램으로 전환한다. 두 prop을 생략하면(이번 변경 전 호출부와 동일한 모양) 항상 기존 고정 다이어그램을 그려 회귀가 없다 — 실제로 3개 호출부 모두 새 prop을 추가했으므로 지금은 전부 동적 경로를 탄다.
>
> **[2026-10-05 qa-tester 반려 및 수정 각주 — 치명적 버그]** qa-tester가 로컬 docker-compose 실제 Postgres + 실제 API로 항목 1~8을 검증한 결과, 항목 6(`DRAFT→APPROVED` AND 조건)에서 치명적 버그를 발견해 반려했다.
> - **버그**: `ApprovalService.decide()`의 `APPROVE` 분기가 `WorkflowTransitionPolicy.requireAllowedTransition(...)`(원래 설계한 그 메서드)을 그대로 호출했는데, 이 메서드는 "시스템 `ADMIN`과 해당 프로젝트의 `PROJECT_ADMIN`은 화이트리스트와 무관하게 항상 통과"하는 lock-out 방지 우회를 갖고 있다. 그런데 `decide()`는 호출 전에 이미 `projectMemberService.requireRole(projectId, principal, PROJECT_ADMIN)`으로 **PROJECT_ADMIN 이상만 호출 가능**하도록 제한돼 있고, `ProjectRole`에는 PROJECT_ADMIN보다 높은 역할이 없다. 즉 `decide()`에 도달할 수 있는 모든 실제 호출자가 예외 없이 그 우회 조건에 걸려, 추가한 워크플로우 체크가 **실제 호출 경로상 단 한 번도 거부를 발생시킬 수 없는 죽은 코드**였다 — `DRAFT→APPROVED`가 화이트리스트에 없어도 PROJECT_ADMIN이 승인하면 그대로 200 성공, 요구사항이 APPROVED로 바뀌는 버그. 최초 구현 시 작성한 `ApprovalServiceTest`의 회귀 테스트는 `WorkflowTransitionPolicy`를 Mockito로 완전히 모킹해 `doThrow`로 예외를 강제 주입했기 때문에, `requireRole(PROJECT_ADMIN)` 선행 검사와 정책의 PROJECT_ADMIN 우회 로직 사이의 실제 상호작용을 전혀 검증하지 못했다(Mockito 통과와 실제 동작이 다른 사례).
> - **수정**: `WorkflowTransitionPolicy`에 lock-out 우회가 없는 변형 `requireRegisteredTransition(projectId, targetType, fromStatus, toStatus)`(actor 파라미터 자체가 없음)을 추가하고, `ApprovalService.decide()`가 `requireAllowedTransition` 대신 이 메서드를 호출하도록 바꿨다. 자유 전이 모드(규칙 미등록)면 그대로 허용하고, 화이트리스트 모드면 등록된 (from,to) 조합만 허용하며, allowedRole 검사는 하지 않는다(호출자가 `decide()`에 의해 이미 PROJECT_ADMIN 이상임이 보장되므로 항상 충족). 이 호출 지점에는 애초에 lock-out 시나리오가 없다는 qa-tester의 지적을 그대로 반영했다 — 승인이 막혀도 요구사항은 DRAFT로 남을 뿐이고, PROJECT_ADMIN은 `config/workflow-rules` API로 스스로 규칙을 추가해 풀 수 있는 별도 경로가 있다. `requireAllowedTransition`(lock-out 우회 포함)은 `RequirementService.changeStatus`/`IssueService.changeStatus` 경로에는 그대로 쓴다 — qa-tester가 항목 1~5,7,8은 전부 PASS로 확인했으므로 그 경로는 손대지 않았다.
> - **재검증 방법(Mockito가 아니라 qa-tester와 동일한 방식)**: 로컬 docker-compose 실제 Postgres(V17까지 마이그레이션 적용됨)에 `mvnw spring-boot:run`으로 수정된 코드를 실제로 구동하고, 신규 프로젝트에 시스템 ADMIN이 아닌 실제 PROJECT_ADMIN 멤버 계정으로 로그인해 curl로 재현했다: (1) `IN_PROGRESS→VERIFIED`만 등록하고 `DRAFT→APPROVED`는 등록하지 않은 화이트리스트 모드에서 MEMBER가 승인 요청을 생성하고 PROJECT_ADMIN이 `PATCH .../approval-requests/{id}/decision {"decision":"APPROVE"}`를 호출 → **`400 VALIDATION_ERROR`로 거부됨을 확인**(수정 전에는 이 경로가 `200`을 반환했을 것). 거부 후 DB를 직접 조회해 요구사항 상태가 여전히 `DRAFT`, 승인 요청 상태가 여전히 `PENDING`임을 확인(전체 롤백). (2) 같은 프로젝트에 `DRAFT→APPROVED` 규칙을 추가 등록한 뒤 같은 승인 요청을 다시 decide → `200`으로 성공, 요구사항이 `APPROVED`로 바뀜을 DB에서 확인. (3) 규칙을 아예 등록하지 않은 별도 프로젝트에서는 decide가 그대로 `200`으로 성공(자유 전이 회귀 재확인). 검증에 사용한 임시 프로젝트/사용자(`WFBUG`/`WFBUG2`, `bugfix_padmin`/`bugfix_member`)는 검증 후 DB에서 직접 삭제해 정리했다.
> - 단위 테스트에도 `WorkflowTransitionPolicyTest`에 `requireRegisteredTransition` 전용 회귀 테스트 4개를 추가했다(규칙 없음/자기자신/등록된 조합 허용/미등록 조합 거부 — actor·role 파라미터 자체가 없어 구조적으로 lock-out 우회가 불가능함을 보장). `ApprovalServiceTest`의 기존 2개 테스트는 `requireRegisteredTransition` 호출로 갱신했다. 전체 `mvnw test` **161개 통과**(이전 157개 + 신규 4개).

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
