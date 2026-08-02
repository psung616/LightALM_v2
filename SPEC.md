# Light ALM — 구현 명세서 (SPEC.md)

> 이 문서는 Claude Code(AI 코딩 에이전트)가 별도 질의 없이 바로 파일 생성 및 코딩을 시작할 수 있도록 작성된 실행형 명세서다. 모든 결정 사항(스코프, 스키마, API, 화면, 개발 순서)이 확정되어 있으므로, 구현 중 모호한 부분이 있으면 이 문서의 원칙(§1 스코프, §9 컨벤션)에 따라 가장 단순한 방식으로 판단하고 진행한다.

---

## 0. 프로젝트 메타 정보

| 항목 | 값 |
|---|---|
| 프로젝트명 | Light ALM |
| 목적 | Jira/Azure DevOps 등 복잡한 ALM 대신, 요구사항 관리·이슈 트래킹·기본 추적성 관리만 지원하는 경량 웹 시스템 |
| 백엔드 | Java 21 (Maven Wrapper 사용, 시스템 Maven 설치 불필요), Spring Boot 3.3.x |
| DB | PostgreSQL 15+ |
| 프론트엔드 | React 18 + TypeScript (Vite), 별도 SPA로 백엔드 REST API 호출 |
| 인증 | 폼 기반 세션 로그인 (Spring Security, 서버 세션 쿠키) |
| 외부 연동 | GitHub API/Webhook, Jenkins API/Webhook |
| 프로젝트 구조 | 멀티 프로젝트 지원 (하나의 시스템에서 여러 프로젝트를 생성/관리) |
| VCS/CI | GitHub, Jenkins |

---

## 1. 프로젝트 목적 및 핵심 스코프

### 1.1 목적
중소 규모 개발팀이 무거운 ALM 툴 없이도 다음 세 가지 핵심 기능만으로 소프트웨어 개발 생명주기를 관리할 수 있게 한다.

1. **요구사항 관리** — 요구사항 등록/분류/상태 추적, 상위-하위 요구사항 계층 구조
2. **이슈 트래킹** — 버그/작업/스토리 단위 이슈 생성, 담당자 지정, 상태 전이
3. **추적성 관리(Traceability)** — 요구사항 ↔ 이슈 간 연결, 커밋/PR/빌드까지 연결하여 "요구사항이 실제로 어떤 코드/빌드로 구현·검증되었는지" 추적

### 1.2 핵심 스코프 (반드시 구현)
- 사용자 인증/인가 (로그인, 로그아웃, 역할 기반 권한)
- 멀티 프로젝트 생성/관리 및 프로젝트 멤버 관리
- 요구사항 CRUD + 계층 구조(상위/하위)
- 이슈 CRUD + 상태 전이(칸반 스타일)
- 요구사항 ↔ 이슈 추적성 링크 및 매트릭스 뷰
- **상위/하위 추적성 트리 뷰**(§5.4) — 요구사항 조상 체인 + 하위 요구사항·연결된 이슈까지 한 화면에서 재귀적으로 탐색
- **요구사항/이슈 상태 Workflow 차트**(§5.5) — 고정된 상태 흐름을 다이어그램으로 시각화(전이 규칙을 강제하는 워크플로우 엔진이 아니라 읽기 전용 시각화, §1.3 참고)
- 댓글(요구사항/이슈 공용)
- GitHub 연동: 커밋/PR을 요구사항·이슈에 연결, Webhook으로 자동 반영
- Jenkins 연동: 빌드 결과를 이슈에 연결, Webhook으로 자동 반영
- 대시보드(프로젝트 요약 통계)
- **개인화된 대시보드**(§4.11, §5.2 "내 작업") — 여러 프로젝트에 걸쳐 나에게 할당된 항목을 한 화면에서 상태별/마감일 기준으로 모아보기

### 1.3 명시적 비스코프 (Light 버전에서 제외)
아래 항목은 이번 버전에서 **구현하지 않는다.** Claude는 이 항목들에 대한 기능을 임의로 추가하지 않는다.
- 스프린트/애자일 보드(백로그, 스프린트 계획, 번다운 차트)
- 커스텀 워크플로우 엔진(상태 전이는 고정된 Enum 기반)
- 커스텀 필드(사용자 정의 필드 추가 기능)
- 알림/이메일 발송, 실시간 웹소켓 알림
- 파일 첨부(첨부파일 업로드 기능은 제외, 텍스트/링크로만 참조)
- 다국어(i18n) — 한국어 단일 언어로 구현
- SSO/OAuth 로그인 — 자체 폼 로그인만 지원
- 세밀한 권한 매트릭스(프로젝트 역할은 3단계: PROJECT_ADMIN / MEMBER / VIEWER로 단순화)
- 감사 로그(Audit Log) 고도화 — 이번 버전은 생략(추후 확장 가능하도록 테이블만 대비하지 않음)

### 1.4 사용자 역할
- **시스템 역할(User.role)**: `ADMIN`(시스템 전체 관리자, 사용자 관리 가능), `USER`(일반 사용자)
- **프로젝트 역할(ProjectMember.role)**: `PROJECT_ADMIN`(프로젝트 설정/멤버 관리), `MEMBER`(요구사항/이슈 생성·수정), `VIEWER`(읽기 전용)

---

## 2. 아키텍처 개요

### 2.1 시스템 구성
```
[React SPA (Vite, TS)]  --REST API (JSON, 쿠키 세션)-->  [Spring Boot Backend]  --JPA-->  [PostgreSQL]
                                                                |    |
                                                     GitHub API |    | Jenkins API
                                                                v    v
                                                        [GitHub Repo]  [Jenkins Server]
                                                                |            |
                                              Webhook(push/PR)  |            | Webhook(build result)
                                                                v            v
                                                   [POST /api/webhooks/github/{projectId}]
                                                   [POST /api/webhooks/jenkins/{projectId}]
```

### 2.2 리포지토리 구조
모노레포로 구성하며 최상위에 `backend/`, `frontend/` 두 디렉터리를 둔다.

```
light-alm/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/lightalm/
│       ├── LightAlmApplication.java
│       ├── config/            # SecurityConfig, CorsConfig, WebConfig, JacksonConfig
│       ├── domain/             # JPA Entity
│       ├── repository/         # Spring Data JPA Repository
│       ├── dto/                 # Request/Response DTO
│       ├── service/            # 비즈니스 로직
│       ├── web/                # @RestController
│       ├── security/            # UserDetailsService, 인증 관련
│       ├── integration/
│       │   ├── github/         # GitHub API 클라이언트, Webhook 서명 검증
│       │   └── jenkins/        # Jenkins API 클라이언트, Webhook 처리
│       └── exception/          # 예외 클래스, GlobalExceptionHandler
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/       # Flyway 마이그레이션 SQL (V1__init.sql ...)
├── frontend/
│   ├── package.json
│   └── src/
│       ├── main.tsx
│       ├── api/                # axios 인스턴스, API 함수 (project.ts, requirement.ts ...)
│       ├── auth/                # AuthContext, ProtectedRoute
│       ├── pages/               # 화면 단위 컴포넌트 (§5 참고)
│       ├── components/         # 공용 컴포넌트
│       ├── types/               # TypeScript 타입 정의
│       └── router.tsx
├── docker-compose.yml           # postgres + backend + frontend
└── README.md
```

### 2.3 기술 스택 상세
| 영역 | 선택 |
|---|---|
| 언어/런타임 | Java 21(실제 사용 버전), Node 20 |
| 백엔드 프레임워크 | Spring Boot 3.3.x |
| 웹 계층 | spring-boot-starter-web |
| 인증 | spring-boot-starter-security (세션 기반, `HttpSession`) |
| ORM | spring-boot-starter-data-jpa (Hibernate) |
| DB | PostgreSQL 15+, 드라이버 `org.postgresql:postgresql` |
| 마이그레이션 | Flyway (`flyway-core`, `flyway-database-postgresql`) |
| 검증 | spring-boot-starter-validation |
| 코드 간결화 | Lombok |
| 빌드 도구 | Maven **Wrapper**(`mvnw`/`mvnw.cmd`) — 시스템에 Maven을 설치하지 않고, 프로젝트에 wrapper를 포함시켜 `mvn` 대신 `mvnw`로 실행 |
| HTTP 클라이언트(외부 연동) | Spring `RestClient` 또는 `WebClient` (GitHub/Jenkins API 호출) |
| 프론트엔드 빌드 | Vite + React 18 + TypeScript |
| 라우팅 | react-router-dom v6 |
| 서버 상태 관리 | @tanstack/react-query |
| HTTP 클라이언트(프론트) | axios (`withCredentials: true`) |
| 스타일링 | Tailwind CSS |
| CI | Jenkins (빌드/테스트 파이프라인), GitHub (소스 저장소 + Webhook 트리거) |

### 2.4 실행 환경 (Local / 사내 테스트 서버) — 실제 구현 방식

`local`은 개발자 PC에서 Docker로 띄우는 기본 개발 환경이고, `test`는 사내 Synology NAS(`ondalprincess.synology.me`)에 배포하려는 공유 테스트 서버다. **최초 설계는 Spring profile(`local`/`test`) 분리를 검토했으나, 실제 구현은 그보다 단순한 "환경변수 오버라이드 단일 설정" 방식으로 진행했다.** 아래는 실제 적용된 방식이다.

| 구분 | local (개발 PC / 로컬 Docker) | 사내 테스트 서버(목표) |
|---|---|---|
| Jenkins | 프로젝트별로 앱 UI(설정 화면)에서 개별 등록 — 전역 설정 아님 | 동일. `https://jenkins.ondalprincess.synology.me/`는 "프로젝트 설정 → Jenkins 연동" 화면에 값으로만 입력됨(§3.2, §4.4) |
| Web(배포 URL) | `http://localhost:5173`(프론트, backend/DB 포함 전체 스택 `docker compose up`으로 기동, 정상 동작 확인 완료) | `https://alm.ondalprincess.synology.me/` — **아직 우리 컨테이너로 연결되지 않음(§ 아래 경고 참고, 미완료)** |
| DB Host | `docker-compose.yml`의 `backend` 서비스가 로컬 `postgres` 컨테이너 대신 곧바로 아래 외부 DB를 사용하도록 이미 전환되어 있음 → `ondalprincess.synology.me` | 좌동 (local과 test 구분 없이 이미 이 DB 하나만 사용 중) |
| DB Port | `55432` | 좌동 |
| DB 계정 | `postgres` / `postgres` | 좌동 |
| DB 이름 | `ALM_Project` | 좌동 |

> 로컬 `postgres` 컨테이너(포트 5432, 계정 `lightalm/lightalm`, DB `lightalm`)는 `docker-compose.yml`에 **정의는 남아있지만 `backend`가 더 이상 사용하지 않는다.** 필요하면 `docker compose up -d postgres`로 별도 기동해 다른 용도로 쓸 수 있다.

> **Git 원격 저장소는 이 표에서 뺐다**: Git remote는 "local vs 사내 테스트 서버"라는 배포 환경 구분과는 다른 축(로컬 개발자 PC의 저장소 설정)이라 이 표 구조에 맞지 않는다. 실제 Git remote 운영 방식은 아래 "Git 다중 remote" 문단과 부록 D에 정리했다.

**환경변수 오버라이드 방식(Spring profile 미사용)**: `application-local.yml`/`application-test.yml`을 만들지 않고, `backend/src/main/resources/application.yml` 하나에 아래처럼 환경변수 기본값을 넣어 어떤 값이든 실행 시 오버라이드 가능하게 했다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:lightalm}
    username: ${DB_USER:lightalm}
    password: ${DB_PASSWORD:lightalm}
server:
  port: ${SERVER_PORT:8080}
light-alm:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
```

`docker-compose.yml`도 `local`/`test`용으로 파일을 나누지 않고 **단일 파일**을 유지하며, `backend` 서비스의 environment 블록에 실제 값을 직접 넣는다.

```yaml
  backend:
    build: ./backend
    environment:
      DB_HOST: ondalprincess.synology.me
      DB_PORT: 55432
      DB_NAME: ALM_Project
      DB_USER: postgres
      DB_PASSWORD: postgres
      CORS_ALLOWED_ORIGINS: https://alm.ondalprincess.synology.me,http://localhost:5173
```

**프론트엔드 → 백엔드 API 경로(`.env.local`/`.env.test` 미사용)**: 프론트엔드 환경 파일 분리 대신 **Docker 빌드 시점 ARG**로 처리한다.

```dockerfile
# frontend/Dockerfile
ARG VITE_API_BASE_URL=http://localhost:8080/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build
```

```yaml
# docker-compose.yml
  frontend:
    build:
      context: ./frontend
      args:
        VITE_API_BASE_URL: /api   # 상대 경로 — 접속 도메인의 /api를 그대로 호출
```

axios `baseURL`은 `import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'`로 참조한다(`frontend/src/api/client.ts`).

**⚠️ 반드시 필요한 nginx `/api` 프록시(실제로 겪은 장애와 해결책)**: `VITE_API_BASE_URL=/api`(상대 경로)로 빌드한 상태에서 **외부 리버스 프록시 없이 frontend 컨테이너에 직접 접속하면**, `/api/**` 요청이 nginx의 SPA 폴백(`try_files $uri $uri/ /index.html`)에 걸려 **JSON 대신 `index.html`이 그대로 응답**된다. axios는 이를 조용히 문자열로 반환하고, 프론트엔드 코드가 이를 정상 JSON(`{content:[...]}`)처럼 다루려다 렌더링 중 `TypeError: Cannot read properties of undefined (reading 'length')`로 크래시하여 **화면이 완전히 하얗게 빈다.** (실제로 `docker compose up`으로 `http://localhost:5173`에 접속했을 때 이 증상을 겪었다.)

해결책: `frontend/nginx.conf`에 `/api/`를 backend 컨테이너로 직접 프록시하는 블록을 추가한다.

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

이렇게 하면 frontend 컨테이너 **하나만으로 API 호출이 완결**되며, 외부(DSM) 리버스 프록시 유무와 무관하게 항상 정상 동작한다. 따라서 사내 테스트 서버 배포 시 DSM 리버스 프록시는 도메인 전체를 frontend 컨테이너(80번 포트) **하나에만** 연결하면 되고, `/api`를 위한 별도 규칙은 필요 없다.

**Git 다중 remote(부록 D)는 실제로는 사용하지 않았다**: `git remote set-url --add --push`로 한 origin에 두 push URL을 등록하는 대신, 아래처럼 처리했다.
- `origin` = `https://git.ondalprincess.synology.me/psung616/LightALM.git` (`git remote set-url origin ...`으로 교체)
- 기존 GitHub 저장소로는 `origin`을 거치지 않고 매번 **URL을 직접 지정해 push**: `git push https://github.com/psung616/LightALM.git main`
- 이렇게 나눈 이유: 사내 Git 서버(`git.ondalprincess.synology.me`)에 저장소가 실제로 준비되어 있는지 아직 확인되지 않아, 우선 기존 GitHub에만 안전하게 반영하고 사내 서버 push는 보류했다. 부록 D의 다중 push-url 스크립트는 **참고용으로만 남겨두고, 실제로 실행하지는 않았다.**

**CORS**: profile별로 나누지 않고, `docker-compose.yml`의 `CORS_ALLOWED_ORIGINS` 환경변수 하나에 로컬(`http://localhost:5173`)과 테스트 서버 도메인(`https://alm.ondalprincess.synology.me`)을 콤마로 함께 넣는다(`SecurityConfig`가 `,`로 split해서 허용 목록에 반영, §6 참고).

> ⚠️ `git.ondalprincess.synology.me`가 GitHub.com이 아닌 자체 호스팅 Git 서버(Gitea/Gogs/GitLab CE 등)인 경우, §4.9·§7.1·Phase 7에서 전제하는 `https://api.github.com` 기반 REST API/`X-Hub-Signature-256` Webhook 포맷이 그대로 맞지 않을 수 있다. GitHub 연동은 이 문서 기준 실제로는 **github.com을 대상으로만 구현·검증**했으며, 자체 호스팅 서버 연동은 별도 확인이 필요하다.

> ⚠️ **사내 테스트 서버(`https://alm.ondalprincess.synology.me/`) 현재 상태 — 미완료**: 이 도메인은 **여러 사람이 같이 쓰는 공유 서버**이고, 확인 결과 아직 우리 LightALM 컨테이너로 연결되어 있지 않다(다른 기존 애플리케이션 "FactorySolution ALM" 로그인 화면이 그 자리에 떠 있는 것을 직접 접속해 확인함). 사용자 요청에 따라 DSM 리버스 프록시 설정은 **더 이상 손대지 않기로 했다.** 이 문서 기준 실제로 End-to-End 검증까지 끝난 것은 **로컬(`http://localhost:5173`)에서 외부 사내 DB(`ondalprincess.synology.me:55432/ALM_Project`)에 붙여 정상 동작하는 것까지**이며, `alm.ondalprincess.synology.me` 도메인 자체의 리버스 프록시 연결/배포 검증은 이후 별도로 진행해야 한다(DSM 제어판 → 로그인 포털 → 고급 → 역방향 프록시에서 해당 도메인을 frontend 컨테이너로 연결).

### 2.5 자동 Git 커밋/Push 정책 (신규)

위 §2.4/부록 D에 기록된 과거 이력(사내 Git 서버를 `origin`으로 쓰고 기존 GitHub엔 URL을 직접 지정해 수동 push)은 **더 이상 유효하지 않다.** 새 저장소가 준비되어 아래 정책으로 대체한다.

- **저장소(단일)**: `https://github.com/psung616/LightALM_v2.git` 하나만 자동 커밋/push 대상으로 한다. `origin`이 이 저장소를 가리키도록 한다.
- **Phase 0**에서 `git init` → `git remote add origin https://github.com/psung616/LightALM_v2.git` → 최초 커밋(`Phase 0: 프로젝트 초기화`) → `git push -u origin main`까지 수행한다.
- **커밋/push 시점**: Phase 0~11 각 Phase의 DoD를 통과할 때마다, 해당 Phase에서 변경된 파일을 커밋하고 곧바로 `origin`(LightALM_v2)에 push한다. 커밋 메시지 형식: `Phase {N}: {phase 요약}` (예: `Phase 4: 요구사항 CRUD 구현`).
- **브랜치**: `main` 단일 브랜치에 직접 커밋한다(Light 버전 스코프에서는 별도 브랜치 전략을 쓰지 않는다).
- **사내 Git 서버는 현재 자동 push 대상에서 제외**한다. 저장소 준비 여부가 아직 확인되지 않았기 때문이다(§2.4 참고). 나중에 준비가 확인되면 아래 주석을 해제해서 추가 remote로 등록하고 각 Phase 커밋 시 함께 push하면 된다.

```bash
# --- 사내 Git 서버 연동 (현재 비활성 — 저장소 준비 확인 후 아래 주석을 해제할 것) ---
# git remote add company https://git.ondalprincess.synology.me/psung616/LightALM.git
# git push company main
```

- 커밋 전 매번 `.gitignore`에 실제 운영용 민감정보(예: 진짜 프로덕션 DB 비밀번호, 실배포용 `.env`)가 걸러지는지 확인한다. 단 §2.4·§7.4에서 이미 결정한 대로, 로컬/사내 테스트용 계정(`lightalm/lightalm`, `postgres/postgres` 등 테스트 전용 값)은 `docker-compose.yml`/`application.yml`에 평문으로 남겨두고 그대로 커밋 대상에 포함한다.

---

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
| github_repo_owner | VARCHAR(100) | NULL |
| github_repo_name | VARCHAR(100) | NULL |
| github_access_token | VARCHAR(255) | NULL (GitHub PAT, MVP는 평문 저장 — §7.4 참고) |
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
| due_date | DATE | NULL 허용 (신규 — 개인화 대시보드의 마감 임박/기한 초과 판단 기준, §4.11·§5.2 참고) |
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
| due_date | DATE | NULL 허용 (신규 — 개인화 대시보드의 마감 임박/기한 초과 판단 기준, §4.11·§5.2 참고) |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMP | NOT NULL DEFAULT now() |
| resolved_at | TIMESTAMP | NULL |

### 3.6 `traceability_links`
요구사항 ↔ 이슈, 요구사항 ↔ 요구사항(참조성) 등 범용 연결 테이블. `source_type`/`target_type`은 `REQUIREMENT` 또는 `ISSUE`만 허용.

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| source_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| source_id | BIGINT | NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| target_id | BIGINT | NOT NULL |
| link_type | VARCHAR(20) | NOT NULL, CHECK IN ('IMPLEMENTS','TESTS','DEPENDS_ON','RELATES_TO','DUPLICATES') |
| created_by | BIGINT | FK → users.id, ON DELETE SET NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

UNIQUE (source_type, source_id, target_type, target_id, link_type) — 동일 링크 중복 방지

> 실제 서비스 로직에서는 `REQUIREMENT → ISSUE`(link_type='IMPLEMENTS' 또는 'TESTS') 조합만 UI에서 주로 사용하지만, 테이블 자체는 범용으로 설계한다.

### 3.7 `comments`
| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| project_id | BIGINT | FK → projects.id, ON DELETE CASCADE, NOT NULL |
| target_type | VARCHAR(20) | NOT NULL, CHECK IN ('REQUIREMENT','ISSUE') |
| target_id | BIGINT | NOT NULL |
| author_id | BIGINT | FK → users.id, ON DELETE SET NULL |
| content | TEXT | NOT NULL |
| created_at | TIMESTAMP | NOT NULL DEFAULT now() |

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
Project 1---N TraceabilityLink (source/target = Requirement|Issue, 다형 연관은 FK 제약 없이 애플리케이션 레벨 검증)
Project 1---N Comment (target = Requirement|Issue)
Project 1---N GitLink (target = Requirement|Issue)
Project 1---N JenkinsBuild (target = Requirement|Issue)
```

---

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
| GET | `/api/projects/{projectId}/requirements/{reqId}/traceability-tree` | **(신규)** 이 요구사항 기준 상위(조상) 체인 전체 + 하위(자손, 재귀) 요구사항 트리 + 트리의 각 노드에 연결된 이슈까지 한 번에 반환(§5.4, 응답 예시는 §4.7 하단) | VIEWER+ |

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

**상위/하위 추적성 트리 응답 예시** (`GET /api/projects/{projectId}/requirements/{reqId}/traceability-tree`, §5.4):
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

**Jenkins 연동 방식(권장 구성)**: Jenkins Job의 Post-build Action에 "Post build task" 또는 "HTTP Request Plugin"을 추가하여 빌드 종료 시 `POST /api/webhooks/jenkins/{projectId}`로 빌드 결과 JSON을 전송하도록 구성한다(문서 §8 Phase 9에서 예시 payload 제공).

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
- 구현은 서비스 레이어에서 `project_members`로 내가 속한 프로젝트 id 목록을 먼저 구한 뒤, 그 프로젝트들 범위에서 `assignee_id`/`assigned_to`가 나인 `issues`/`requirements`를 조회해 집계한다(단일 쿼리든 여러 쿼리 조합이든 방식은 자유, DoD는 §8 Phase 6 참고).

---

## 5. 프론트엔드 주요 화면 단위

### 5.1 라우트 테이블
| 경로 | 화면 | 인증 |
|---|---|---|
| `/login` | 로그인 | 비인증 전용 |
| `/` | 프로젝트 목록 (내가 속한 프로젝트) | 인증필요 |
| `/projects/new` | 프로젝트 생성 | 인증필요 |
| `/projects/:projectId` | 프로젝트 대시보드 | 인증필요 |
| `/projects/:projectId/requirements` | 요구사항 목록 | 인증필요 |
| `/projects/:projectId/requirements/:reqId` | 요구사항 상세 | 인증필요 |
| `/projects/:projectId/issues` | 이슈 목록(칸반/테이블 토글) | 인증필요 |
| `/projects/:projectId/issues/:issueId` | 이슈 상세 | 인증필요 |
| `/projects/:projectId/traceability` | 추적성 매트릭스 | 인증필요 |
| `/projects/:projectId/settings` | 프로젝트 설정(멤버, GitHub/Jenkins 연동) | 인증필요(PROJECT_ADMIN+) |
| `/admin/users` | 사용자 관리 | 인증필요(ADMIN) |
| `/my-tasks` | 개인화된 대시보드 — 전체 프로젝트에 걸쳐 내게 할당된 이슈/요구사항 모아보기(신규: 마감 임박/기한 초과 포함, §4.11) | 인증필요 |

### 5.2 화면별 상세

**로그인 (`/login`)**
- 아이디/비밀번호 입력 폼, 로그인 실패 시 에러 메시지
- 성공 시 `/`로 리다이렉트, AuthContext에 사용자 정보 저장

**프로젝트 목록 (`/`)**
- 카드 목록(프로젝트명, 키, 상태 뱃지) — 내가 접근 권한을 가진 프로젝트만 표시(아래 "프로젝트 노출 권한" 참고)
- "새 프로젝트" 버튼 → `/projects/new`
- (미구현) 키워드 검색창 — 최초 설계에는 있었으나 이번 구현 범위에는 포함하지 않았다. 추가하려면 §4.4의 `GET /api/projects?keyword=` 쿼리 파라미터부터 백엔드에 구현해야 한다.

**프로젝트 노출 권한**
- 시스템 `USER`: 자신이 `project_members`에 등록된(즉 `PROJECT_ADMIN`/`MEMBER`/`VIEWER` 중 하나라도 부여된) 프로젝트만 목록·검색 결과에 노출된다. 멤버로 등록되지 않은 프로젝트는 목록에 아예 나타나지 않으며, 상세 URL(`/projects/:projectId`)로 직접 접근해도 서비스 레이어의 `requireRole` 검사(§6)에 걸려 접근이 거부된다.
- 시스템 `ADMIN`: 예외적으로 프로젝트 멤버 여부와 무관하게 전체 프로젝트가 목록·검색에 노출된다(§4.4).
- 특정 사용자에게 프로젝트 노출 권한을 주는 방법 = 해당 프로젝트의 `프로젝트 설정 → 멤버 관리` 탭(`/projects/:projectId/settings`, PROJECT_ADMIN+ 전용, §5.2)에서 멤버로 추가하는 것이다. 시스템 ADMIN은 모든 프로젝트에 대해 PROJECT_ADMIN+ 취급되므로(§6) 어떤 프로젝트든 이 화면에서 멤버를 추가/제거할 수 있다 — 별도의 "전역 프로젝트 권한 관리" 화면은 Light 버전 스코프에 추가하지 않는다.

**프로젝트 대시보드 (`/projects/:projectId`)**
- 요구사항 상태별 카운트 위젯(도넛/막대), 이슈 상태별 카운트 위젯
- 최근 생성/수정된 요구사항·이슈 리스트(최근 10건)
- 좌측 사이드바 네비게이션(요구사항/이슈/추적성/설정)
- 요구사항/이슈 각각 상태별 카운트를 Workflow 차트(§5.5) 형태(단계별 흐름 + 각 단계 건수)로도 볼 수 있는 토글 제공

**요구사항 목록 (`/projects/:projectId/requirements`)**
- 테이블 뷰: 키, 제목, 유형, 우선순위, 상태, 담당자, 상위 요구사항
- 필터: 상태, 유형, 우선순위, 담당자, 키워드 검색
- 계층 구조는 들여쓰기(tree-table) 또는 상위 요구사항 컬럼으로 표시
- "새 요구사항" 모달/사이드패널 생성

**요구사항 상세 (`/projects/:projectId/requirements/:reqId`)**
- 기본 정보(제목/설명/유형/우선순위/상태) 인라인 편집
- 상태 옆에 현재 상태를 표시하는 Workflow 차트 미니 위젯(§5.5)
- 하위 요구사항 목록 — "추적성 트리로 보기" 링크 클릭 시 §5.4의 상위/하위 추적성 트리 뷰로 전환
- 연결된 이슈 목록(추적성 링크) + "이슈 연결" 버튼
- 댓글 스레드
- GitHub 커밋/PR 연결 목록

**이슈 목록 (`/projects/:projectId/issues`)**
- 기본: 칸반 보드(컬럼 = TODO/IN_PROGRESS/IN_REVIEW/DONE/CLOSED, 드래그로 상태 변경)
- 토글: 테이블 뷰(필터/정렬 가능)
- 필터: 상태, 유형, 우선순위, 담당자, 키워드

**이슈 상세 (`/projects/:projectId/issues/:issueId`)**
- 기본 정보 인라인 편집, 상태 변경 드롭다운
- 상태 옆에 현재 상태를 표시하는 Workflow 차트 미니 위젯(§5.5)
- 연결된 요구사항 목록(추적성 링크)
- GitHub 커밋/PR 연결 목록
- Jenkins 빌드 이력 목록(상태 뱃지: SUCCESS/FAILURE/…) + "빌드 트리거" 버튼
- 댓글 스레드

**추적성 매트릭스 (`/projects/:projectId/traceability`)**
- 상단 토글: "매트릭스 뷰"(기본) / "트리 뷰"(§5.4)
- 매트릭스 뷰: 행: 요구사항, 열: 이슈(또는 반대) 형태의 그리드, 셀 클릭으로 링크 생성/해제 토글
- 커버리지 요약(연결되지 않은 요구사항 수 하이라이트)

**프로젝트 설정 (`/projects/:projectId/settings`)**
- 탭: 일반 정보 / 멤버 관리 / GitHub 연동 / Jenkins 연동
- GitHub 탭: repoOwner, repoName, accessToken(마스킹), webhookSecret, Webhook URL 안내 문구 표시
- Jenkins 탭: baseUrl, jobName, apiUser, apiToken(마스킹), Webhook 설정 안내 문구 표시

**사용자 관리 (`/admin/users`)**
- 사용자 테이블(아이디/이메일/이름/시스템 역할/활성화 여부)
- 생성/수정 모달, 비활성화 토글

**내 작업 (`/my-tasks`) — 개인화된 대시보드로 확장(신규)**
- `GET /api/me/dashboard`(§4.11) 하나로 화면을 구성한다. 로그인 직후에도 바로 봐도 유용하도록, 원한다면 로그인 후 첫 화면을 `/`(프로젝트 목록) 대신 이 화면으로 바꾸는 것도 고려할 수 있지만, 이번 버전은 기존대로 `/`을 유지하고 `/my-tasks`는 헤더 링크로 바로 갈 수 있게만 한다(§5.3).
- 상단: 나에게 할당된 이슈 상태별 카운트 위젯 + 요구사항 상태별 카운트 위젯(§5.5 Workflow 차트 스타일 재사용 가능)
- **마감 임박(7일 이내) / 기한 초과** 항목을 각각 별도 리스트로 강조 표시(기한 초과는 빨간색 계열로 눈에 띄게). 각 항목 클릭 시 해당 프로젝트의 상세 화면으로 이동
- 프로젝트별 담당 항목 수 요약(카드 형태) — 클릭 시 해당 프로젝트로 이동
- 기존처럼 "나에게 할당된 이슈 목록 / 요구사항 목록"을 탭으로 나눠 테이블로도 볼 수 있게 유지(전체 목록 확인용)

### 5.3 공통 레이아웃 & 홈 네비게이션 — 실제 구현 방식

최초 설계는 인증 필요 라우트 전체(`/`, `/projects/**`, `/admin/users`, `/my-tasks`)를 감싸는 **단일** `AppLayout` 컴포넌트를 상정했지만, 실제 구현은 **레이아웃을 두 개로 분리**했다(기능적으로는 동등하다).

- **`TopNavbar`** (`/`, `/my-tasks`, `/admin/users`에서 사용): 좌측 "Light ALM" 로고(클릭 시 `/`로 이동, 홈 버튼 역할) + "내 작업"(`/my-tasks`) 링크 + "사용자 관리"(`/admin/users`, 시스템 `ADMIN`에게만 조건부 노출) 링크. 우측에 로그인한 사용자명과 로그아웃 버튼. **별도의 "프로젝트 목록" 텍스트 링크는 없고, 로고 클릭이 그 역할을 겸한다**(최초 설계는 로고와 별개로 "① 프로젝트 목록" 링크도 두는 안이었으나 실제로는 로고 하나로 통합).
- **`ProjectLayout`** (`/projects/:projectId/**`에서 사용): 좌측 사이드바 최상단에 "← 프로젝트 목록" 링크(클릭 시 `/`) + 현재 프로젝트명/키, 그 아래 대시보드/요구사항/이슈/추적성/설정 내비게이션, 하단에 사용자명/로그아웃. 이 부분은 최초 설계와 동일하게 구현됨.
- **세션 복원 및 새로고침 대응**: `AuthContext`(`frontend/src/auth/AuthContext.tsx`)는 마운트 시 `loading` state를 `true`로 시작하고 `GET /api/auth/me`를 호출, 응답(성공/실패 모두) 후 `loading`을 `false`로 내린다. `ProtectedRoute`/`GuestOnlyRoute`는 `loading === true`인 동안 전체 화면 로딩 인디케이터만 보여주고, 완료 후에만 인증 여부에 따라 원래 경로 렌더링 또는 `/login` 리다이렉트를 수행한다 — **이 부분은 최초 설계대로 정확히 구현되어 있고 실제로 문제가 없었다.**
- 다만 세션 확인 요청이 네트워크 오류로 실패한 경우 "다시 시도" 버튼이 있는 에러 화면을 보여주는 것은 **미구현**이다(현재는 실패 시 조용히 `user = null` 처리하여 `/login`으로 보냄).

**⚠️ 실제로 발생했던 "화면이 완전히 하얗게 비는" 버그의 진짜 원인은 위 세션 복원 로직이 아니었다.** `npm run dev` 개발 서버에서는 전혀 재현되지 않았고, **오직 `docker compose`로 띄운 프로덕션(nginx) 빌드에서만** 발생했다. 원인은 §2.4에 정리한 대로 `/api` 요청이 nginx SPA 폴백에 걸려 `index.html`을 JSON처럼 파싱하려다 크래시한 것이었다(`frontend/nginx.conf`의 `/api/` 프록시 블록으로 해결). 향후 유사 증상이 재현되면 AuthContext보다 **"백엔드 API 응답이 실제로 JSON으로 오고 있는가(네트워크 탭/curl로 Content-Type 확인)"부터 먼저 의심할 것.**

### 5.4 상위/하위 추적성 트리 뷰 (신규 — Polarion 스타일 다단계 추적성)

기존 §4.7 매트릭스 뷰는 "요구사항 x 이슈"를 평면 그리드로 보여주는 데는 적합하지만, 요구사항이 여러 단계로 상위/하위 계층을 이루고 각 단계마다 연결된 이슈가 있는 경우 전체 구조를 한눈에 보기 어렵다. Polarion의 다단계 추적성(Linked Work Items 트리)을 참고해 아래 트리 뷰를 추가한다.

- **진입 경로**: ① 요구사항 상세 화면(`/projects/:projectId/requirements/:reqId`)의 "추적성 트리로 보기" 링크, ② 추적성 매트릭스 화면(`/projects/:projectId/traceability`) 상단의 "트리 뷰" 토글. 두 경로 모두 같은 컴포넌트를 재사용한다.
- **표시 내용**: `GET .../requirements/{reqId}/traceability-tree`(§4.5, §4.7) 응답을 기준으로,
  - 화면 상단에 **조상 체인**을 루트부터 가로 breadcrumb 형태로 표시(`LALM-R1 › LALM-R3 › LALM-R5(현재)`), 각 항목 클릭 시 그 요구사항 기준 트리로 이동.
  - 화면 본문에 **현재 요구사항을 루트로 하는 하위 트리**를 들여쓰기된 트리 구조로 표시. 각 요구사항 노드는 키/제목/상태 뱃지를 보여주고, 그 노드에 직접 연결된 이슈가 있으면 자식처럼 들여써서 이슈 키/제목/상태를 함께 표시(요구사항과 이슈를 시각적으로 구분되는 아이콘/색상으로 구별).
  - 트리 노드를 접고 펼 수 있어야 하며(대규모 트리 대비), 기본은 2단계까지 펼친 상태로 시작한다.
- **커버리지 표시**: 하위 트리를 순회했을 때 이슈가 하나도 연결되지 않은 요구사항 노드는 §5.2 매트릭스 뷰와 동일하게 하이라이트(예: 노란색 경고 아이콘)해서 "구현되지 않은 요구사항"을 트리에서도 바로 식별할 수 있게 한다.
- **범위**: 이번 버전은 요구사항↔요구사항(상위/하위), 요구사항↔이슈(`IMPLEMENTS`/`TESTS` 등) 관계만 트리에 담는다. 이슈↔이슈 연결이나 커밋/빌드까지 트리에 포함하는 것은 이번 스코프 밖(필요해지면 추후 확장).

### 5.5 요구사항/이슈 상태 Workflow 차트 (신규 — 읽기 전용 시각화)

§1.3에서 "커스텀 워크플로우 엔진"은 명시적으로 스코프 밖이다. 즉 상태 전이 규칙을 프로젝트마다 다르게 설정하거나, 특정 상태에서만 다음 상태로 넘어갈 수 있게 강제하는 기능은 만들지 않는다. 여기서 추가하는 "Workflow 차트"는 그런 엔진이 아니라, **§3.4/§3.5에 이미 고정되어 있는 상태 목록을 다이어그램으로 보여주기만 하는 순수 시각화 컴포넌트**다 — 데이터베이스 스키마나 상태 변경 API(`PATCH .../status`)는 전혀 바뀌지 않는다.

- **요구사항 Workflow 차트**: `DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → VERIFIED`를 기본 흐름(가로로 나열된 박스 + 화살표)으로 그리고, `REJECTED`는 흐름 아래쪽에 별도 노드로 두고 다른 모든 상태에서 점선 화살표로 연결해 "어디서든 REJECTED로 갈 수 있음"을 표현한다.
- **이슈 Workflow 차트**: `TODO → IN_PROGRESS → IN_REVIEW → DONE → CLOSED`를 동일한 방식으로 가로 흐름으로 그린다.
- **표시 위치**:
  - 요구사항/이슈 상세 화면: 현재 항목의 상태에 해당하는 노드를 강조(테두리/색상)한 미니 위젯으로 표시(§5.2).
  - 프로젝트 대시보드: 각 단계 노드 아래에 해당 상태의 항목 개수를 함께 표시하는 "퍼널형" 차트로 확장해서, 기존 도넛/막대 위젯과 토글로 전환 가능하게 한다(§5.2).
- **구현 방식**: 상태 목록/순서는 프론트엔드에 §3.4·§3.5 CHECK 제약과 동일한 순서로 하드코딩한다(백엔드에 별도 "워크플로우 정의" API를 만들지 않는다). 노드/화살표 렌더링은 별도 다이어그램 라이브러리 없이 CSS flex + SVG 화살표로 구현 가능한 수준이며, 필요시 가벼운 라이브러리(예: 단순 SVG 컴포넌트)를 써도 된다.
- **주의**: 이 차트는 "일반적으로 이런 순서로 진행된다"는 안내일 뿐, 실제로는 `PATCH .../status`가 임의의 상태값으로 즉시 변경을 허용한다(전이 순서를 막는 로직 없음, §7 관련 없음). 화면에는 이 점을 짧은 툴팁("참고용 흐름도이며 상태는 자유롭게 변경할 수 있습니다")으로 안내한다.

---

## 6. 인증/인가 상세 설계

- Spring Security `SecurityFilterChain` 구성: 세션 기반 인증, `formLogin()` 커스터마이즈(로그인 성공/실패 시 JSON 응답을 위해 `AuthenticationSuccessHandler`/`AuthenticationFailureHandler` 직접 구현 — 리다이렉트 대신 200/401 JSON 반환)
- `UserDetailsService` 구현체는 `users` 테이블 조회, 비밀번호는 `BCryptPasswordEncoder` 사용
- CORS 설정(실제 구현): `application.yml`의 `light-alm.cors.allowed-origins` 프로퍼티(환경변수 `CORS_ALLOWED_ORIGINS`로 오버라이드, 기본값 `http://localhost:5173`, §2.4·부록 A 참고)를 `@ConfigurationProperties` 또는 `@Value`로 바인딩한 뒤 쉼표(`,`) 기준으로 split하여 `CorsConfigurationSource`의 허용 origin 목록에 그대로 반영한다. `allowCredentials(true)`도 함께 설정한다. 사내 테스트 서버 배포 시에는 이 환경변수 하나에 로컬 origin과 `https://alm.ondalprincess.synology.me`를 콤마로 함께 넣어 두 origin을 동시에 허용한다(부록 B의 `backend.environment.CORS_ALLOWED_ORIGINS` 참고).
- 프로젝트 단위 권한 검사는 `@PreAuthorize` 대신 서비스 레이어에서 `ProjectMemberService.requireRole(projectId, userId, minRole)` 형태의 명시적 검사 메서드로 구현(멀티 프로젝트 + 프로젝트별 역할이라는 동적 구조이므로 어노테이션보다 명시적 코드가 유지보수에 유리)
- 시스템 `ADMIN`은 모든 프로젝트에 대해 항상 `PROJECT_ADMIN` 이상 권한을 가진 것으로 취급

---

## 7. 외부 연동 상세 (GitHub / Jenkins)

### 7.1 GitHub 연동 흐름
1. 프로젝트 설정 화면에서 `repoOwner`, `repoName`, Personal Access Token, Webhook Secret 입력
2. 백엔드는 GitHub REST API(`https://api.github.com`)를 `RestClient`로 호출하여 커밋/PR 메타데이터 조회
3. 사용자가 수동으로 커밋 SHA/PR 번호를 입력해 연결하거나, GitHub Webhook이 push/PR 이벤트를 `/api/webhooks/github/{projectId}`로 전송하면 커밋 메시지에서 이슈/요구사항 키 패턴을 파싱해 자동 연결
4. 사용자는 GitHub 저장소 Settings → Webhooks에서 Payload URL을 `https://{서버주소}/api/webhooks/github/{projectId}`로, Secret을 프로젝트 설정과 동일하게 등록해야 한다(README에 안내 문구 포함)

### 7.2 Jenkins 연동 흐름
1. 프로젝트 설정 화면에서 `baseUrl`, `jobName`, API 사용자/토큰 입력
2. "빌드 트리거" 버튼 클릭 시 백엔드가 Jenkins API(`POST {baseUrl}/job/{jobName}/build`, Basic Auth)를 호출
3. Jenkins Job의 Post-build Action에서 빌드 종료 시 아래 형태의 JSON을 `/api/webhooks/jenkins/{projectId}`로 POST하도록 구성(HTTP Request Plugin 또는 curl 스크립트 사용):
```json
{
  "targetType": "ISSUE",
  "targetId": 10,
  "jobName": "light-alm-backend",
  "buildNumber": 42,
  "status": "SUCCESS",
  "buildUrl": "https://jenkins.example.com/job/light-alm-backend/42/",
  "triggeredBy": "jenkins-user"
}
```
4. 헤더 `X-Jenkins-Token`으로 프로젝트별 시크릿을 검증한 뒤 `jenkins_builds` 테이블에 upsert

### 7.3 재시도/에러 처리
- 외부 API 호출 실패(네트워크 오류, 401 등)는 사용자에게 명확한 에러 메시지로 변환하여 반환(`GITHUB_API_ERROR`, `JENKINS_API_ERROR` 등 에러 코드 사용)
- 별도의 재시도 큐/비동기 처리는 Light 버전 스코프 밖(동기 호출 + 실패 시 에러 응답으로 충분)

### 7.4 보안 참고사항
- GitHub PAT, Jenkins API 토큰은 MVP 단계에서 DB 평문 저장을 허용하되, 코드 주석과 README에 "프로덕션 전환 시 Jasypt 등으로 암호화 필요"를 TODO로 명시한다.
- API 응답(GET 프로젝트 상세 등)에서 토큰 필드는 마스킹(`****`) 처리하여 반환한다.

---

## 8. 단계별 개발 순서 (Claude 구현 지침)

**중요**: 각 Phase는 이전 Phase가 컴파일/실행되는 상태에서 완료된 것으로 간주하고 진행한다. 각 Phase 종료 시 "완료 조건(Definition of Done)"을 스스로 점검한다. **DoD를 통과하면 그 즉시 §2.5의 자동 Git 커밋/Push 정책에 따라 커밋 후 `origin`(`https://github.com/psung616/LightALM_v2.git`)에 push한다.** 이 커밋/push는 별도 확인 없이 각 Phase마다 자동으로 수행한다.

### Phase 0 — 프로젝트 초기화
1. `light-alm/` 루트에 `backend/`(Spring Initializr 구조 수동 생성 또는 `spring init` 사용: Web, Security, Data JPA, PostgreSQL Driver, Validation, Lombok, Flyway), `frontend/`(Vite React-TS 템플릿) 생성
2. `docker-compose.yml` 작성: `postgres:15` 서비스(포트 5432, DB명 `lightalm`, 계정 `lightalm/lightalm`)
3. `backend/src/main/resources/application.yml` 하나만 작성한다(§2.4, 부록 A 참고). profile 분리 없이 `${DB_HOST:localhost}` 형태의 환경변수 기본값으로 DB 접속정보를 오버라이드 가능하게 하고, JPA `ddl-auto: validate`, Flyway를 활성화한다. Maven은 시스템에 설치하지 않고 **Maven Wrapper(`mvnw`/`mvnw.cmd`)를 프로젝트에 포함**시켜, 이후 모든 빌드/실행 명령은 `mvn` 대신 `mvnw`/`mvnw.cmd`로 실행한다.
4. `git init` → `.gitignore` 작성(§2.2 구조 기준: `target/`, `node_modules/`, `dist/`, `.env` 등) → `git remote add origin https://github.com/psung616/LightALM_v2.git` → 최초 커밋(`Phase 0: 프로젝트 초기화`) → `git push -u origin main`(§2.5 참고)
5. **DoD**: `docker compose up -d postgres` 후 `mvnw.cmd spring-boot:run`(Windows)으로 백엔드가 에러 없이 기동, `npm run dev`로 프론트가 기동. `git remote -v`에 `origin`이 `LightALM_v2` 저장소로 잡혀 있고 최초 커밋이 push되어 있음을 확인

### Phase 1 — DB 스키마 (Flyway) & JPA 엔티티
1. `db/migration/V1__init.sql`에 §3의 모든 테이블 DDL 작성(제약조건 포함)
2. §3의 모든 엔티티 클래스 작성(Lombok `@Getter/@Setter/@Builder`, 연관관계는 지연 로딩 `FetchType.LAZY` 기본)
3. **DoD**: 애플리케이션 기동 시 Flyway 마이그레이션이 성공적으로 적용됨

### Phase 2 — 인증/인가 & 사용자 관리
1. `SecurityConfig` 작성(세션 기반, JSON 로그인 성공/실패 핸들러, CORS)
2. `CustomUserDetailsService`, 로그인/로그아웃/`me` API 구현
3. 사용자 CRUD API 구현(관리자 전용), 최초 관리자 계정을 Flyway 시드 데이터(`V2__seed_admin.sql`)로 삽입(예: `admin/admin1234`, 최초 로그인 후 변경 권장 안내)
4. **DoD**: Postman/curl로 로그인 → 세션 쿠키로 `/api/auth/me` 호출 성공

### Phase 3 — 프로젝트 & 멤버 관리
1. Project, ProjectMember 엔티티/리포지토리/서비스/컨트롤러 구현
2. 프로젝트 키/이슈·요구사항 시퀀스 채번 로직(동시성 고려: `SELECT ... FOR UPDATE` 또는 DB 시퀀스 활용)
3. `ProjectMemberService.requireRole()` 공통 권한 검사 유틸 구현
4. **DoD**: 프로젝트 생성 → 생성자가 자동 PROJECT_ADMIN으로 등록되는지 확인

### Phase 4 — 요구사항 (Requirement)
1. CRUD API + 계층 구조(상위/하위) 조회 API 구현. 요청/응답 DTO에 `dueDate`(신규, §3.4) 포함
2. `req_key` 자동 채번(`{projectKey}-R{seq}`)
3. **DoD**: 요구사항 생성/수정/삭제/계층 조회 API 전부 동작, 단위 테스트 작성

### Phase 5 — 이슈 (Issue)
1. CRUD API + 상태 변경 API(`PATCH .../status`) 구현. 요청/응답 DTO에 `dueDate`(신규, §3.5) 포함
2. `issue_key` 자동 채번(`{projectKey}-{seq}`)
3. **DoD**: 이슈 생성/수정/삭제/상태변경 API 전부 동작, 단위 테스트 작성

### Phase 6 — 추적성 & 댓글
1. TraceabilityLink CRUD + 매트릭스 조회 API 구현
2. **(신규)** 상위/하위 추적성 트리 API 구현: `GET .../requirements/{reqId}/traceability-tree`(§4.5, §4.7, §5.4) — PostgreSQL 재귀 CTE(`WITH RECURSIVE`)로 조상 체인과 자손 트리를 각각 조회한 뒤, 자손 트리 각 노드에 `traceability_links`를 조인해 `linkedIssues`를 채운다
3. **(신규)** 개인화 대시보드 API 구현: `GET /api/me/dashboard`(§4.11) — `project_members` 기준 내가 속한 프로젝트 범위에서 나에게 할당된 요구사항/이슈를 상태별 집계 + `dueDate` 기준 overdue/dueSoon 목록 + 프로젝트별 카운트로 반환
4. Comment CRUD 구현(다형 target_type/target_id 처리 공통 로직)
5. **DoD**: 요구사항-이슈 링크 생성 후 매트릭스 API에서 정상 반환 확인. 3단계 이상 깊이의 요구사항 계층(조부모-부모-자식)을 만들어 `traceability-tree` API가 조상 체인과 자손 트리를 정확히 반환하는지 확인. 서로 다른 두 프로젝트에 동일 사용자를 멤버로 넣고 각각 항목을 할당한 뒤 `/api/me/dashboard`가 두 프로젝트 항목을 모두 집계하는지 확인, `dueDate`를 과거/7일 이내/먼 미래로 나눠 넣고 `overdue`/`dueSoon` 분류가 맞는지 확인

### Phase 7 — GitHub 연동
1. GitHub API 클라이언트(`GithubApiClient`) 구현: 커밋 조회(`GET /repos/{owner}/{repo}/commits/{sha}`), PR 조회(`GET /repos/{owner}/{repo}/pulls/{number}`)
2. 수동 연결 API + Webhook 수신 API 구현(HMAC-SHA256 서명 검증 포함)
3. 커밋 메시지/PR 제목에서 `{PROJECT_KEY}-\d+` 패턴 정규식 파싱 로직 구현
4. **DoD**: 테스트용 GitHub 저장소로 실제 Webhook 호출(또는 curl로 모의 payload 전송) 시 `git_links` 레코드 생성 확인

### Phase 8 — Jenkins 연동
1. Jenkins API 클라이언트 구현: 빌드 트리거(Basic Auth POST)
2. Webhook 수신 API 구현(`X-Jenkins-Token` 검증), `jenkins_builds` upsert 로직
3. **DoD**: curl로 모의 Jenkins webhook payload 전송 시 빌드 레코드 생성/갱신 확인

### Phase 9 — 프론트엔드 기반 구축
1. Vite 프로젝트에 react-router-dom, axios, @tanstack/react-query, Tailwind 설치/설정
2. axios 인스턴스(`withCredentials: true`, 401 응답 시 `/login`으로 리다이렉트하는 인터셉터) 구성
3. `AuthContext` + `ProtectedRoute` 구현 — 앱 마운트 시 `GET /api/auth/me`로 세션 확인, 확인 완료 전(`isLoading`)에는 라우트 대신 전체 화면 로딩 표시, 완료 후 인증 여부에 따라 원래 경로 렌더링 또는 `/login` 리다이렉트(§5.3 참고)
4. 공통 레이아웃(§5.3) 구현: `TopNavbar`(로고=홈 버튼, 내 작업/사용자 관리 링크, `/`·`/my-tasks`·`/admin/users`에서 사용)와 `ProjectLayout`(사이드바 + "← 프로젝트 목록" 링크, `/projects/:projectId/**`에서 사용) 두 컴포넌트로 구현한다. §5.1 라우트 테이블의 인증 필요 라우트는 경로에 맞는 레이아웃으로 감싼다
5. **DoD**: 로그인 → 프로젝트 목록 화면까지 라우팅 정상 동작. `/`을 포함한 임의의 화면에서 새로고침(F5)해도 로그인 상태가 유지된 채 해당 화면이 다시 정상적으로 나타나야 하며, 어느 화면에서든 헤더의 홈 버튼(로고) 또는 사이드바의 "← 프로젝트 목록"을 클릭하면 `/`로 이동해야 한다

### Phase 10 — 프론트엔드 화면 구현 (아래 순서 권장)
1. 로그인 화면
2. 프로젝트 목록/생성 화면
3. 프로젝트 대시보드
4. 요구사항 목록/상세 화면 — 상세 화면에 Workflow 차트 미니 위젯(§5.5)과 "추적성 트리로 보기" 링크(§5.4) 포함
5. 이슈 목록(칸반+테이블)/상세 화면 — 상세 화면에 Workflow 차트 미니 위젯(§5.5) 포함
6. 추적성 매트릭스 화면(매트릭스 뷰 + 트리 뷰 토글, §5.4)
7. 프로젝트 설정 화면(멤버, GitHub/Jenkins 연동)
8. 사용자 관리 화면(관리자)
9. 내 작업 화면(개인화된 대시보드, §4.11·§5.2) — 마감 임박/기한 초과 위젯 포함
10. **DoD**: 각 화면에서 대응하는 API가 정상 호출되고 로딩/에러 상태가 처리됨

### Phase 11 — 통합 점검 및 마무리
1. **`docker-compose.yml` 단일 파일**에 backend/frontend 서비스를 추가한다(부록 C의 `docker-compose.test.yml` 분리안은 실제로는 채택하지 않음 — 부록 B가 곧 로컬/테스트 겸용이다). `frontend`는 빌드 시 `VITE_API_BASE_URL` 등 build args를 받고, `frontend/nginx.conf`가 `/api/`를 `backend:8080`으로 프록시하도록 구성한다(§2.4 참고, 필수).
2. README.md 작성: 로컬/사내 테스트 서버 실행 방법(§2.4, 부록 A~B), 초기 관리자 계정, GitHub/Jenkins Webhook 설정 가이드, Git 저장소 정책(§2.5·부록 D — `origin`은 `https://github.com/psung616/LightALM_v2`, 사내 Git 서버는 현재 비활성)을 안내한다.
3. 백엔드 단위 테스트(서비스 레이어 위주) 및 통합 테스트(컨트롤러, `@SpringBootTest` + Testcontainers 권장) 최소 커버리지 확보. **Windows + Docker Desktop 환경에서 Testcontainers가 기본 named pipe(`npipe:////./pipe/docker_engine`)를 찾지 못해 실패할 수 있음** — Docker Desktop이 실제로 쓰는 pipe(`npipe:////./pipe/dockerDesktopLinuxEngine`, `docker context inspect`로 확인)를 `DOCKER_HOST`로 지정해도 API 버전 협상 문제로 계속 실패하는 경우가 있었다. 이 경우 Testcontainers를 쓰는 통합 테스트는 파일명을 `*IT.java`로 짓고 `maven-failsafe-plugin`을 추가해 `mvn test`(surefire, 기본 포함 패턴 `*Test.java`)에서는 제외하고 `mvn verify`(failsafe)에서만 실행되도록 분리한다. 이렇게 하면 `mvn test`는 항상 빠르고 안정적으로 통과한다.
4. **DoD**: `docker compose up --build`로 전체 스택(postgres + backend + frontend) 기동 → 브라우저에서 로그인부터 요구사항/이슈/추적성·GitHub/Jenkins 연동 데이터 표시까지 End-to-End 시나리오 수동 검증 완료. (GitHub/Jenkins 연동 자체의 실제 API 호출/Webhook 왕복 검증은 Phase 7·8 단계에서 curl 모의 payload로 이미 별도 완료했다는 전제)

---

## 9. 코딩 컨벤션 및 품질 기준
- 패키지 구조는 §2.2를 따른다 (계층형: domain/repository/service/web/dto)
- 컨트롤러는 얇게 유지하고, 비즈니스 로직/권한 검사는 서비스 레이어에 위치시킨다
- 모든 API는 요청 DTO에 Bean Validation 어노테이션(`@NotBlank`, `@Size` 등) 적용
- 예외는 커스텀 예외 클래스(`ResourceNotFoundException`, `ForbiddenException`, `ValidationException` 등) + `@RestControllerAdvice` 전역 핸들러로 일관된 에러 응답 반환
- 프론트엔드는 화면별 폴더 구조(`pages/RequirementList/`) + API 함수는 `api/` 디렉터리에 도메인별로 분리(`api/requirement.ts`)
- TypeScript `strict: true` 유지, `any` 타입 사용 최소화
- 커밋 메시지는 이슈/요구사항 키를 포함하는 것을 권장(예: `LALM-101 feat: 이슈 상태 변경 API 구현`) — 이는 GitHub 자동 연동과도 연결되는 규칙이다

## 10. 테스트 전략
- 백엔드: 서비스 레이어 단위 테스트(JUnit5 + Mockito), 컨트롤러 통합 테스트(`@SpringBootTest`, Testcontainers PostgreSQL)
- 최소 커버리지 대상: 요구사항/이슈 CRUD, 권한 검사 로직, 채번 로직(동시성 케이스 포함), GitHub 커밋 키 파싱 정규식, Webhook 서명 검증 로직
- 프론트엔드: 핵심 컴포넌트에 대한 간단한 렌더링 테스트(Vitest + React Testing Library) — Light 버전에서는 필수는 아니나 권장
- **Testcontainers를 쓰는 통합 테스트는 파일명을 `*IT.java`로 짓고, `pom.xml`에 `maven-failsafe-plugin`을 추가해 `integration-test`/`verify` 골에 바인딩한다.** 이렇게 분리해야 `mvn test`(surefire, 기본 포함 패턴이 `*Test.java`라 `*IT.java`는 자동으로 제외됨)가 Docker 환경 문제와 무관하게 항상 빠르게 통과한다. 실제로 Windows + Docker Desktop 환경에서 Testcontainers 기본 전략이 named pipe를 못 찾는 문제를 겪었다(§8 Phase 11 참고).

## 11. 실행/배포
- 로컬 개발: `docker compose up -d postgres` → 백엔드는 `mvnw.cmd spring-boot:run`(Windows) / `./mvnw spring-boot:run`(macOS·Linux), 프론트는 `npm run dev`. Maven을 시스템에 설치하지 않아도 되도록 **Maven Wrapper(`mvnw`)를 프로젝트에 포함**한다.
- 전체 스택(로컬/사내 테스트 서버 공용): `docker compose up --build` (postgres + backend + frontend 이미지 빌드 포함, 부록 B 참고). DB를 외부(사내 NAS)로 돌리고 싶으면 `backend` 서비스의 `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` 환경변수만 바꾸면 되고, 별도의 `docker-compose.test.yml` 파일은 두지 않는다.
- Jenkins CI 파이프라인 예시 단계: `checkout` → `mvnw.cmd test`(백엔드, `*IT.java` 통합 테스트는 제외됨) → `npm ci && npm run build` (프론트) → `docker build` → (선택) 배포 → Post-build Action에서 §7.2의 Webhook payload 전송

---

## 부록 A. `application.yml` 예시 (실제 구현 — profile 분리 없음)

`application.yml` 하나만 두고, 모든 접속 정보를 환경변수 기본값(`${VAR:default}`)으로 오버라이드 가능하게 한다. `application-local.yml`/`application-test.yml`은 만들지 않는다.

```yaml
spring:
  application:
    name: light-alm-backend
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:lightalm}
    username: ${DB_USER:lightalm}
    password: ${DB_PASSWORD:lightalm}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
server:
  port: ${SERVER_PORT:8080}
light-alm:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
```

실행 예시(로컬, 기본값 그대로 사용):
```bash
cd backend
mvnw.cmd spring-boot:run
```

사내 테스트 DB에 붙여 로컬에서 직접 실행하고 싶을 때(도커 없이):
```bash
# Windows PowerShell 예시
$env:DB_HOST="ondalprincess.synology.me"; $env:DB_PORT="55432"; $env:DB_NAME="ALM_Project"; $env:DB_USER="postgres"; $env:DB_PASSWORD="postgres"
mvnw.cmd spring-boot:run
```

> 위 사내 DB 계정(`postgres/postgres`)은 테스트 전용이라는 전제로 문서에 남겨두지만, 실제로는 `docker-compose.yml`에 평문으로 들어가 있다(§2.4). 운영 전환 시 계정/비밀번호를 교체하고 커밋에서 제외하는 방식으로 바꿔야 한다.

## 부록 B. `docker-compose.yml` 골격 (로컬/사내 테스트 서버 겸용, 실제 사용 중인 구조)

로컬용/테스트서버용으로 파일을 나누지 않고 **이 파일 하나만 유지**한다. `postgres` 서비스는 정의는 남겨두되(다른 용도로 필요 시 `docker compose up -d postgres`), `backend`는 기본적으로 사내 외부 DB를 바라보도록 되어 있다. 순수 로컬 DB로 되돌리려면 `backend.environment`의 `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`를 `postgres`/`5432`/`lightalm`/`lightalm`/`lightalm`로 바꾸면 된다.

```yaml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: lightalm
      POSTGRES_USER: lightalm
      POSTGRES_PASSWORD: lightalm
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U lightalm -d lightalm"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build: ./backend
    environment:
      DB_HOST: ondalprincess.synology.me
      DB_PORT: 55432
      DB_NAME: ALM_Project
      DB_USER: postgres
      DB_PASSWORD: postgres
      CORS_ALLOWED_ORIGINS: https://alm.ondalprincess.synology.me,http://localhost:5173
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./frontend
      args:
        VITE_API_BASE_URL: /api   # 상대 경로. frontend/nginx.conf가 /api를 backend:8080으로 프록시(§2.4, 필수)
    depends_on:
      - backend
    ports:
      - "5173:80"

volumes:
  pgdata:
```

`frontend/nginx.conf`(반드시 함께 존재해야 함, §2.4 참고):
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

배포: `docker compose up --build -d`

## 부록 C. `docker-compose.test.yml` — 실제로는 채택하지 않음

최초 설계는 사내 테스트 서버 전용 compose 파일을 별도로 두는 안이었으나, 실제로는 **부록 B의 단일 `docker-compose.yml`이 로컬/테스트 서버를 겸한다**(환경변수 값만 바뀔 뿐 파일 자체는 하나). 이 부록은 "다른 설계를 고려했었다"는 기록으로만 남겨두고, 새로 구현할 때는 부록 B를 기준으로 삼는다.

## 부록 D. Git 원격 저장소 — 현재 정책 (§2.5)

> 이전 버전의 이 부록은 "사내 Git 서버를 origin으로 쓰고 기존 GitHub엔 URL을 직접 지정해 수동 push"하는 과거 이력을 기록하고 있었다. **`https://github.com/psung616/LightALM_v2` 저장소가 새로 준비되면서 아래 정책으로 대체한다.** 과거 이력은 더 이상 유효하지 않으니 아래만 따른다.

**현재 사용(자동)**: `origin` = `https://github.com/psung616/LightALM_v2.git` 하나만 쓰고, Phase 0~11 각 Phase DoD 통과 시마다 자동으로 커밋 후 이 저장소에 push한다(§2.5, §8 Phase 0 참고).

```bash
git remote add origin https://github.com/psung616/LightALM_v2.git
git push -u origin main
```

**사내 Git 서버 — 현재 비활성(주석 처리, 준비 확인 후 사용)**: 사내 Synology Git 서버(`git.ondalprincess.synology.me`)에 저장소가 실제로 준비됐는지 아직 확인되지 않아, 지금은 자동 push 대상에서 제외한다. 아래 스크립트는 준비가 확인된 뒤 주석을 해제해서 `company`라는 별도 remote로 추가하고 사용한다.

```bash
# --- 사내 Git 서버 연동 (현재 비활성) ---
# git remote add company https://git.ondalprincess.synology.me/psung616/LightALM.git
# git push company main
```

두 저장소에 항상 동시에 반영하고 싶어지면(현재는 사용하지 않음), 아래처럼 `origin`에 push URL을 두 개 등록하는 방법도 가능하다.

```bash
git remote set-url --add --push origin https://github.com/psung616/LightALM_v2.git
git remote set-url --add --push origin https://git.ondalprincess.synology.me/psung616/LightALM.git

git push origin main   # 두 저장소에 동시 반영
```

---

이 문서(SPEC.md)를 Claude Code 세션의 루트 디렉터리에 두고 "이 SPEC.md에 따라 Phase 0부터 순서대로 구현해줘"라고 지시하면, 위 순서대로 파일 생성 및 코딩을 진행하며, §2.5 정책에 따라 각 Phase마다 `https://github.com/psung616/LightALM_v2`에 자동으로 커밋·push한다. **단, Java 버전은 21을 사용하고 Maven Wrapper(`mvnw`)를 포함시켜 `mvn` 대신 `mvnw`/`mvnw.cmd`로 실행할 것.**
