> Owner: architect | Status: Accepted | Date: 2026-09-04

# ADR-009: projectKey 검증 제약 완화 (대문자 전용 → 대문자+숫자+언더바, 10자 → 20자)

## 맥락 (Context)

`projectKey`는 프로젝트 생성 시 사용자가 직접 입력하는 값으로, 이슈/요구사항 키의 접두사(`{PROJECT_KEY}-123`, `{PROJECT_KEY}-R45`)로 쓰인다. 현재 제약은 다음 네 곳에 중복 정의돼 있다.

- DB: `V1__init.sql` — `project_key VARCHAR(10) NOT NULL` + `uq_projects_project_key` UNIQUE
- Entity: `Project.java` — `@Column(name = "project_key", length = 10)`
- DTO 검증: `CreateProjectRequest.java` — `@Pattern(regexp = "^[A-Z]{3,10}$")`
- 프론트: `ProjectNewPage.tsx` — `maxLength=10`, 라벨 "프로젝트 키 (대문자 3~10자)"

제품 오너로부터 다음 요구사항이 들어왔다.
1. 대문자만 허용하던 것을 완화해 숫자와 언더바(`_`)도 허용한다(파일명 규칙과 유사하게).
2. 최대 길이를 10자에서 20자로 확장한다.
3. 완화가 SQL Injection 위험을 만들지 않아야 한다.

### SQL Injection 관련 검토
`@Pattern` 화이트리스트 정규식은 애초에 SQL Injection 방어 수단이 아니다. 이 프로젝트는 JPA/Hibernate의 파라미터 바인딩(PreparedStatement)을 통해 모든 쿼리 파라미터를 처리하며, `projectKey` 값을 SQL 문자열에 직접 이어붙이는 코드는 존재하지 않는다. 즉 SQL Injection은 "입력값이 무엇이든" 파라미터 바인딩 계층에서 근본적으로 방어되고 있고, `@Pattern`은 "식별자스러운 값만 받겠다"는 입력 검증(가독성·일관성·키 포맷 통일) 목적일 뿐이다. 따라서 이번 정규식 완화는 SQL Injection 위험을 새로 만들지 않는다.

또한 `IssueKeyParser.parse()`(`backend/src/main/java/com/lightalm/integration/github/IssueKeyParser.java`)는 GitHub 커밋 메시지에서 이슈 키를 추출할 때 `Pattern.quote(projectKey)`로 `projectKey`를 정규식 리터럴로 이스케이프한 뒤 매칭한다. `projectKey`에 어떤 문자(정규식 특수문자 포함)가 들어가도 안전하게 리터럴 취급되므로, 이번 완화로 이 로직이 깨지지 않는다.

## 결정 (Decision)

### 새 정규식
```
^[A-Z][A-Z0-9_]{2,19}$
```
- 반드시 **대문자 알파벳으로 시작**한다. 프로젝트 키가 이슈 키 접두사(`KEY-123`)로 쓰이므로, 숫자나 언더바로 시작하면 `123_-45`처럼 가독성이 떨어지고 순수 숫자 키와 혼동될 여지가 있다. 첫 글자를 문자로 고정해 식별자로서의 일관성을 유지한다.
- 두 번째 글자부터는 대문자/숫자/언더바(`[A-Z0-9_]`)를 자유롭게 허용한다. 언더바의 위치(선두/후행/연속)를 별도로 제한하지 않는다 — 파일명 규칙과 유사하게 가되, 과도한 규칙 추가로 검증 로직이 복잡해지는 것을 피한다.
- 전체 길이는 3~20자(`{2,19}`는 첫 글자 이후 2~19자, 총 3~20자)로, 기존 3~10자 하한을 유지하고 상한만 20자로 확장한다.

예: `LALM`, `PROJECT_2`, `MY_APP_V2`, `A2Z_TEAM_BACKEND_01` 모두 허용. `1ABC`, `_ABC`, `AB`(2자), 21자 이상은 거부.

### 변경 범위 (구현은 developer에게 위임, 이 ADR에는 설계만 확정)
| 영역 | 파일 | 변경 |
|---|---|---|
| DB | `backend/src/main/resources/db/migration/V8__widen_project_key.sql` (신규) | `ALTER TABLE projects ALTER COLUMN project_key TYPE VARCHAR(20);` — 기존 V1~V7은 수정하지 않음(idempotent하게 `DO $$ ... $$` 블록으로 감싸는 것을 권장하되, `ALTER COLUMN TYPE`은 자체적으로 재실행 시 오류가 나지 않으므로 단순 `ALTER`도 무방) |
| Entity | `Project.java` | `@Column(length = 10)` → `length = 20` |
| DTO 검증 | `CreateProjectRequest.java` | `@Pattern(regexp = "^[A-Z]{3,10}$", ...)` → `@Pattern(regexp = "^[A-Z][A-Z0-9_]{2,19}$", message = "projectKey는 대문자로 시작하는 3~20자(대문자/숫자/언더바)여야 합니다.")` |
| 프론트 | `ProjectNewPage.tsx` | `maxLength=10` → `20`, 라벨 "프로젝트 키 (대문자 3~10자)" → "프로젝트 키 (대문자로 시작, 대문자/숫자/언더바 3~20자)", 클라이언트 측 정규식 검증이 있다면 동일 패턴으로 동기화 |
| 설계 문서 | `docs/02-design/03-data-model.md` §3.2 | 이 ADR과 함께 갱신(완료) |

### 기존 기능 영향 검토
- **DB 유니크 제약(`uq_projects_project_key`)**: 컬럼 타입/길이만 넓어질 뿐 UNIQUE 인덱스 정의 자체는 영향 없음. 기존 데이터(대문자 3~10자)는 새 제약을 그대로 만족하므로 마이그레이션 시 데이터 손실이나 제약 위반이 발생하지 않음(하위 호환).
- **`IssueKeyParser`**: 위 SQL Injection 검토 항목에서 확인한 대로 `Pattern.quote()` 사용으로 영향 없음.
- **기존에 생성된 프로젝트 키 값**: 모두 새 정규식(`^[A-Z][A-Z0-9_]{2,19}$`)의 부분집합(대문자 3~10자)이므로 재검증 없이 계속 유효함.

## 결과 (Consequences)
- 장점: 사용자가 팀/제품 이름을 더 자연스럽게 반영한 프로젝트 키를 만들 수 있음(예: `TEAM_A`, `APP_V2`). SQL Injection 방어는 파라미터 바인딩 계층에서 이미 이뤄지고 있어 이번 완화로 보안 저하 없음.
- 단점/리스크: 이슈 키(`{PROJECT_KEY}-123`)가 길어질 수 있어(`MY_APP_V2-123` 등) UI에서 뱃지/라벨 표시 시 줄바꿈·말줄임 처리가 필요할 수 있음 — 프론트 구현 시 developer가 확인할 것.
- 기존 V1~V7 마이그레이션은 수정하지 않고 `V8__widen_project_key.sql`을 신규 추가하는 방식으로 하위 호환을 유지한다.

## 참고
- `docs/02-design/03-data-model.md` §3.2
- `backend/src/main/java/com/lightalm/integration/github/IssueKeyParser.java`
- `docs/05-history/adr/ADR-002-사내-공용-Postgres-사용.md`(Flyway 마이그레이션 정책)
