> Owner: architect | Status: current | Last-reviewed: 2026-08-08
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

## 4. REST API 명세

### 4.1 공통 규칙
- Base path: `/api`
- 요청/응답 포맷: JSON (`Content-Type: application/json`)
- 인증: Spring Security 세션 쿠키(`JSESSIONID`). 프론트엔드는 axios `withCredentials: true` 필수
- CSRF: Spring Security 기본 CSRF 보호 활성화, 쿠키 기반 토큰(`XSRF-TOKEN`) 발급 → 프론트는 `X-XSRF-TOKEN` 헤더로 전송 (단, `/api/webhooks/**` 경로는 CSRF 예외 처리 — 외부 시스템 호출이므로 서명 검증으로 대체)
- 에러 응답 포맷(공통):
```json
{
  "timestamp": "2026-07-26T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "title은 필수입니다.",
  "path": "/api/projects/1/requirements"
}
```
- 페이지네이션(목록 API 공통 쿼리 파라미터): `page`(0-base, default 0), `size`(default 20), `sort`(예: `createdAt,desc`)
- 목록 응답 공통 포맷:
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```
- 권한 표기: `[인증필요]` `[ADMIN]`(시스템 관리자) `[PROJECT_ADMIN+]`(해당 프로젝트 관리자 이상) `[MEMBER+]`(해당 프로젝트 멤버 이상, 즉 VIEWER 제외) `[VIEWER+]`(해당 프로젝트 멤버라면 누구나, VIEWER 포함) `[공개]`(인증 불필요, Webhook 전용)

### 4.2 인증 (Auth)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/auth/login` | `{username, password}` → 로그인, 세션 발급 | 공개 |
| POST | `/api/auth/logout` | 세션 종료 | 인증필요 |
| GET | `/api/auth/me` | 현재 로그인 사용자 정보 조회 | 인증필요 |

### 4.3 사용자 관리 (시스템 관리자 전용)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/users` | 사용자 목록 (페이지네이션) | ADMIN |
| POST | `/api/users` | 사용자 생성 `{username,password,email,fullName,systemRole}` | ADMIN |
| GET | `/api/users/{id}` | 사용자 상세 | ADMIN |
| PUT | `/api/users/{id}` | 사용자 정보 수정 | ADMIN |
| DELETE | `/api/users/{id}` | 사용자 비활성화(soft: enabled=false) | ADMIN |
| PUT | `/api/users/me/password` | 본인 비밀번호 변경 `{oldPassword,newPassword}` | 인증필요 |

### 4.4 프로젝트 (Project)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects` | 내가 속한 프로젝트 목록 (ADMIN은 전체) — 페이지네이션(`page`,`size`)만 지원, `keyword` 검색은 **미구현**(추후 추가 시 백엔드 확장 필요) | 인증필요 |
| POST | `/api/projects` | 프로젝트 생성 `{projectKey,name,description}` — 생성자는 자동으로 PROJECT_ADMIN 등록 | 인증필요 |
| GET | `/api/projects/{projectId}` | 프로젝트 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}` | 프로젝트 정보 수정 | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}` | 프로젝트 삭제 | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/integrations/github` | GitHub 연동 설정 `{repoOwner,repoName,accessToken,webhookSecret}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/integrations/jenkins` | Jenkins 연동 설정 `{baseUrl,jobName,apiUser,apiToken}` | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/members` | 멤버 목록 | VIEWER+ |
| POST | `/api/projects/{projectId}/members` | 멤버 추가 `{userId,role}` | PROJECT_ADMIN+ |
| PUT | `/api/projects/{projectId}/members/{userId}` | 멤버 역할 변경 `{role}` | PROJECT_ADMIN+ |
| DELETE | `/api/projects/{projectId}/members/{userId}` | 멤버 제거 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/dashboard/summary` | 대시보드 요약(요구사항/이슈 상태별 카운트, 최근 활동) | VIEWER+ |

### 4.5 요구사항 (Requirement)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/requirements` | 목록, 쿼리: `status,type,priority,parentId,assignedTo,keyword` | VIEWER+ |
| POST | `/api/projects/{projectId}/requirements` | 생성 `{title,description,type,priority,parentRequirementId,assignedTo,dueDate}`(`dueDate`는 신규, 선택값) | MEMBER+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}` | 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}/requirements/{reqId}` | 수정 | MEMBER+ |
| PATCH | `/api/projects/{projectId}/requirements/{reqId}/status` | 상태 변경 `{status}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/requirements/{reqId}` | 삭제 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/children` | 하위 요구사항 목록 | VIEWER+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/links` | 이 요구사항과 연결된 이슈/요구사항 목록 | VIEWER+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/traceability-tree` | **(신규)** 이 요구사항 기준 상위(조상) 체인 전체 + 하위(자손, 재귀) 요구사항 트리 + 트리의 각 노드에 연결된 이슈까지 한 번에 반환(05-frontend.md §5.4, 응답 예시는 §4.7 하단) | VIEWER+ |

### 4.6 이슈 (Issue)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/issues` | 목록, 쿼리: `status,type,priority,assigneeId,keyword` | VIEWER+ |
| POST | `/api/projects/{projectId}/issues` | 생성 `{title,description,type,priority,assigneeId,dueDate}`(`dueDate`는 신규, 선택값) | MEMBER+ |
| GET | `/api/projects/{projectId}/issues/{issueId}` | 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}/issues/{issueId}` | 수정 | MEMBER+ |
| PATCH | `/api/projects/{projectId}/issues/{issueId}/status` | 상태 변경 `{status}` (DONE 전이 시 resolved_at 자동 세팅) | MEMBER+ |
| DELETE | `/api/projects/{projectId}/issues/{issueId}` | 삭제 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/issues/{issueId}/git-links` | 연결된 커밋/PR 목록 | VIEWER+ |
| GET | `/api/projects/{projectId}/issues/{issueId}/builds` | 연결된 Jenkins 빌드 목록 | VIEWER+ |

### 4.7 추적성 (Traceability)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/traceability/matrix` | 요구사항 x 이슈 매트릭스 데이터 반환 (아래 응답 예시) | VIEWER+ |
| POST | `/api/projects/{projectId}/traceability/links` | 링크 생성 `{sourceType,sourceId,targetType,targetId,linkType}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/traceability/links/{linkId}` | 링크 삭제 | MEMBER+ |

매트릭스 응답 예시:
```json
{
  "requirements": [{"id": 1, "reqKey": "LALM-R1", "title": "로그인 기능"}],
  "issues": [{"id": 10, "issueKey": "LALM-101", "title": "로그인 API 구현"}],
  "links": [{"id": 5, "requirementId": 1, "issueId": 10, "linkType": "IMPLEMENTS"}]
}
```

**상위/하위 추적성 트리 응답 예시** (`GET /api/projects/{projectId}/requirements/{reqId}/traceability-tree`, 05-frontend.md §5.4):
```json
{
  "ancestors": [
    {"id": 1, "reqKey": "LALM-R1", "title": "로그인 기능"},
    {"id": 3, "reqKey": "LALM-R3", "title": "사용자 인증 상위 요구사항"}
  ],
  "self": {"id": 5, "reqKey": "LALM-R5", "title": "폼 로그인", "status": "IMPLEMENTED"},
  "descendants": [
    {
      "id": 8, "reqKey": "LALM-R8", "title": "로그인 실패 처리", "status": "APPROVED",
      "linkedIssues": [{"id": 10, "issueKey": "LALM-101", "title": "로그인 API 구현", "linkType": "IMPLEMENTS", "status": "IN_PROGRESS"}],
      "children": []
    }
  ]
}
```
`ancestors`는 루트까지의 조상 체인(가까운 순), `descendants`는 하위 요구사항을 재귀적으로 담되 각 노드에 직접 연결된 이슈(`linkedIssues`)도 함께 포함한다. 구현은 PostgreSQL 재귀 CTE(`WITH RECURSIVE`)로 조상/자손을 각각 조회한 뒤, 자손 트리의 각 노드마다 `traceability_links`를 조인해 `linkedIssues`를 채운다.

### 4.8 댓글 (Comment)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/comments` | 댓글 목록 (`targetType` = `requirements`\|`issues`) | VIEWER+ |
| POST | `/api/projects/{projectId}/{targetType}/{targetId}/comments` | 댓글 작성 `{content}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/comments/{commentId}` | 댓글 삭제(작성자 본인 또는 PROJECT_ADMIN+) | MEMBER+ |

### 4.9 GitHub 연동
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/{targetType}/{targetId}/git-links` | 수동 연결: 커밋 SHA 또는 PR 번호 입력 → GitHub API로 메타데이터 조회 후 저장 `{source, commitSha 또는 prNumber}` | MEMBER+ |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/git-links` | 연결 목록 | VIEWER+ |
| DELETE | `/api/projects/{projectId}/git-links/{linkId}` | 연결 삭제 | MEMBER+ |
| POST | `/api/webhooks/github/{projectId}` | GitHub Webhook 수신 엔드포인트 (`push`, `pull_request` 이벤트) — 커밋 메시지에서 `LALM-101` 형태의 이슈/요구사항 키 패턴을 정규식으로 파싱하여 자동으로 `git_links` 생성 | 공개(서명 검증) |

**GitHub 자동 연동 규칙**: 커밋 메시지 또는 PR 제목/본문에 `{PROJECT_KEY}-\d+`(이슈) 또는 `{PROJECT_KEY}-R\d+`(요구사항) 패턴이 포함되면 자동으로 해당 대상에 `git_links` 레코드를 생성한다.

**Webhook 서명 검증**: GitHub Webhook은 `X-Hub-Signature-256` 헤더에 `sha256=` prefix + HMAC-SHA256(payload, project.github_webhook_secret) 값을 담아 전송한다. 서버는 이 값을 재계산하여 일치하지 않으면 `401 Unauthorized`를 반환한다.

### 4.10 Jenkins 연동
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/jenkins/trigger` | Jenkins 빌드 트리거 `{targetType,targetId}` — Jenkins API 호출(`POST {jenkinsBaseUrl}/job/{jobName}/build`, Basic Auth: apiUser/apiToken) | MEMBER+ |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/builds` | 연결된 빌드 목록 | VIEWER+ |
| POST | `/api/webhooks/jenkins/{projectId}` | Jenkins 빌드 완료 Webhook 수신 (Jenkins Post-build Action에서 HTTP POST) — payload에 포함된 커스텀 파라미터(`targetType`,`targetId`,`jobName`,`buildNumber`,`status`,`buildUrl`)로 `jenkins_builds` 레코드 생성/갱신 | 공개(공유 시크릿 검증) |

**Jenkins Webhook 인증**: URL 쿼리 파라미터 또는 헤더로 `?token={project.github_webhook_secret 재사용 또는 별도 jenkins_webhook_secret}`을 전달받아 검증한다. (단순화를 위해 Jenkins 쪽은 헤더 `X-Jenkins-Token` 사용을 표준으로 한다.)

**Jenkins 연동 방식(권장 구성)**: Jenkins Job의 Post-build Action에 "Post build task" 또는 "HTTP Request Plugin"을 추가하여 빌드 종료 시 `POST /api/webhooks/jenkins/{projectId}`로 빌드 결과 JSON을 전송하도록 구성한다(08-dev-phases.md Phase 9에서 예시 payload 제공).

### 4.11 개인화된 대시보드 (My Dashboard) — 신규

기존 `/api/projects/{projectId}/dashboard/summary`(§4.4)는 프로젝트 하나 기준이라, 여러 프로젝트에 걸쳐 있는 사용자가 "지금 나한테 급한 게 뭔지"를 보려면 프로젝트를 하나씩 들어가봐야 한다. 이를 보완하는 프로젝트 횡단(cross-project) 개인화 대시보드 API를 추가한다.

| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/me/dashboard` | 내가 속한 모든 프로젝트를 통틀어 나에게 할당된 요구사항/이슈 현황을 집계해 반환(아래 응답 예시) | 인증필요 |

응답 예시:
```json
{
  "assignedIssuesByStatus": {"TODO": 3, "IN_PROGRESS": 2, "IN_REVIEW": 1, "DONE": 0, "CLOSED": 0},
  "assignedRequirementsByStatus": {"DRAFT": 1, "APPROVED": 2, "IN_PROGRESS": 1, "IMPLEMENTED": 0, "VERIFIED": 0, "REJECTED": 0},
  "overdue": [
    {"type": "ISSUE", "id": 10, "key": "LALM-101", "title": "로그인 API 구현", "projectKey": "LALM", "dueDate": "2026-07-20", "status": "IN_PROGRESS"}
  ],
  "dueSoon": [
    {"type": "REQUIREMENT", "id": 5, "key": "LALM-R5", "title": "폼 로그인", "projectKey": "LALM", "dueDate": "2026-08-05", "status": "APPROVED"}
  ],
  "byProject": [
    {"projectId": 1, "projectKey": "LALM", "projectName": "Light ALM", "assignedIssueCount": 6, "assignedRequirementCount": 4}
  ]
}
```
- `overdue`: `due_date < 오늘` 이면서 상태가 종료 상태(이슈의 `DONE`/`CLOSED`, 요구사항의 `IMPLEMENTED`/`VERIFIED`/`REJECTED`)가 아닌 항목.
- `dueSoon`: `due_date`가 오늘부터 7일 이내(마찬가지로 종료 상태 제외).
- `byProject`: 내가 속한 프로젝트별로 나에게 할당된 항목 개수만 요약(프로젝트 목록·바로가기용).
- 구현은 서비스 레이어에서 `project_members`로 내가 속한 프로젝트 id 목록을 먼저 구한 뒤, 그 프로젝트들 범위에서 `assignee_id`/`assigned_to`가 나인 `issues`/`requirements`를 조회해 집계한다(단일 쿼리든 여러 쿼리 조합이든 방식은 자유, DoD는 08-dev-phases.md Phase 6 참고).

---

### 4.12 테스트케이스 (Test Case)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/test-cases` | 목록, 쿼리: `requirementId,status,priority,keyword` | VIEWER+ |
| POST | `/api/projects/{projectId}/test-cases` | 생성 `{title,description,preconditions,steps,expectedResult,priority,requirementId}` | MEMBER+ |
| GET | `/api/projects/{projectId}/test-cases/{tcId}` | 상세 | VIEWER+ |
| PUT | `/api/projects/{projectId}/test-cases/{tcId}` | 수정 | MEMBER+ |
| DELETE | `/api/projects/{projectId}/test-cases/{tcId}` | 삭제 | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/requirements/{reqId}/test-cases` | 요구사항에 연결된 테스트케이스 목록 | VIEWER+ |

### 4.13 테스트 실행 (Test Run)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/test-runs` | 목록 | VIEWER+ |
| POST | `/api/projects/{projectId}/test-runs` | 생성 `{name, releaseId?}` | MEMBER+ |
| GET | `/api/projects/{projectId}/test-runs/{runId}` | 상세(포함된 테스트케이스 + 결과 목록) | VIEWER+ |
| POST | `/api/projects/{projectId}/test-runs/{runId}/cases` | 테스트런에 테스트케이스 추가 `{testCaseIds:[]}` | MEMBER+ |
| PATCH | `/api/projects/{projectId}/test-runs/{runId}/results/{testCaseId}` | 실행 결과 기록 `{result,actualResult}` | MEMBER+ |
| PATCH | `/api/projects/{projectId}/test-runs/{runId}/status` | 실행 상태 변경 (PLANNED→IN_PROGRESS→COMPLETED) | MEMBER+ |

### 4.14 릴리스 (Release)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/releases` | 목록 | VIEWER+ |
| POST | `/api/projects/{projectId}/releases` | 생성 `{version,name,releaseDate,description}` | PROJECT_ADMIN+ |
| GET | `/api/projects/{projectId}/releases/{releaseId}` | 상세(포함 항목 + 요약 통계) | VIEWER+ |
| PUT | `/api/projects/{projectId}/releases/{releaseId}` | 수정 | PROJECT_ADMIN+ |
| PATCH | `/api/projects/{projectId}/releases/{releaseId}/status` | 상태 변경 | PROJECT_ADMIN+ |
| POST | `/api/projects/{projectId}/releases/{releaseId}/items` | 항목 추가 `{targetType,targetId}` | MEMBER+ |
| DELETE | `/api/projects/{projectId}/releases/{releaseId}/items/{itemId}` | 항목 제거 | MEMBER+ |
| GET | `/api/projects/{projectId}/releases/{releaseId}/notes` | 릴리스 노트 자동 생성(포함된 요구사항/이슈를 타입별로 정리한 마크다운 텍스트 반환) | VIEWER+ |

### 4.15 변경 이력 / 감사 로그 (Audit Log)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/projects/{projectId}/audit-logs` | 프로젝트 범위 전체 조회, 쿼리: `targetType,targetId,actorId,fromDate,toDate` | PROJECT_ADMIN+ (민감정보 성격이라 조회 권한을 상향) |
| GET | `/api/projects/{projectId}/{targetType}/{targetId}/audit-logs` | 특정 대상 하나의 변경 이력만 (상세 화면 "이력" 탭용) | VIEWER+ |

생성/수정/삭제 API는 없다 — append-only이며, 서비스 레이어에서 자동 기록된다.

### 4.16 승인 워크플로우 (Approval)
| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/projects/{projectId}/requirements/{reqId}/approval-requests` | 승인 요청 생성 `{requestedStatus}` (요구사항이 DRAFT 상태일 때만 가능, 서비스 레벨 검증) | MEMBER+ |
| GET | `/api/projects/{projectId}/approval-requests` | 승인함 목록, 쿼리: `status` | PROJECT_ADMIN+ |
| PATCH | `/api/projects/{projectId}/approval-requests/{approvalId}/decision` | 승인/반려 `{decision: 'APPROVE'|'REJECT', comment}` (승인 시 대상 요구사항 status를 requestedStatus로 전이 + audit_logs에 APPROVE/REJECT 기록을 자동 생성) | PROJECT_ADMIN+ |
