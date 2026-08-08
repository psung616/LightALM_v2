> Owner: architect | Status: current | Last-reviewed: 2026-08-08
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

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
│       ├── pages/               # 화면 단위 컴포넌트 (05-frontend.md §5 참고)
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
| 다이어그램 렌더링 | `mermaid`(npm) — Workflow 상태 차트(05-frontend.md §5.5)를 PlantUML류 상태 다이어그램처럼 시각화하는 데 사용, 서버 없이 브라우저에서 SVG로 렌더링 |
| CI | Jenkins (빌드/테스트 파이프라인), GitHub (소스 저장소 + Webhook 트리거) |

### 2.4 실행 환경 (Local / 사내 테스트 서버) — 실제 구현 방식

`local`은 개발자 PC에서 Docker로 띄우는 기본 개발 환경이고, `test`는 사내 Synology NAS(`ondalprincess.synology.me`)에 배포하려는 공유 테스트 서버다. **최초 설계는 Spring profile(`local`/`test`) 분리를 검토했으나, 실제 구현은 그보다 단순한 "환경변수 오버라이드 단일 설정" 방식으로 진행했다.** 아래는 실제 적용된 방식이다.

| 구분 | local (개발 PC / 로컬 Docker) | 사내 테스트 서버(목표) |
|---|---|---|
| Jenkins | 프로젝트별로 앱 UI(설정 화면)에서 개별 등록 — 전역 설정 아님 | 동일. `https://jenkins.ondalprincess.synology.me/`는 "프로젝트 설정 → Jenkins 연동" 화면에 값으로만 입력됨(03-data-model.md §3.2, 04-api.md §4.4) |
| Web(배포 URL) | `http://localhost:5173`(프론트, backend/DB 포함 전체 스택 `docker compose up`으로 기동, 정상 동작 확인 완료) | `https://alm.ondalprincess.synology.me/` — **아직 우리 컨테이너로 연결되지 않음(§ 아래 경고 참고, 미완료)** |
| DB Host | `docker-compose.yml`의 `backend` 서비스가 로컬 `postgres` 컨테이너 대신 곧바로 아래 외부 DB를 사용하도록 이미 전환되어 있음 → `ondalprincess.synology.me` | 좌동 (local과 test 구분 없이 이미 이 DB 하나만 사용 중) |
| DB Port | `55432` | 좌동 |
| DB 계정 | `postgres` / `postgres` | 좌동 |
| DB 이름 | `ALM_Project` | 좌동 |

> 로컬 `postgres` 컨테이너(포트 5432, 계정 `lightalm/lightalm`, DB `lightalm`)는 `docker-compose.yml`에 **정의는 남아있지만 `backend`가 더 이상 사용하지 않는다.** 필요하면 `docker compose up -d postgres`로 별도 기동해 다른 용도로 쓸 수 있다.

> **Git 원격 저장소는 이 표에서 뺐다**: Git remote는 "local vs 사내 테스트 서버"라는 배포 환경 구분과는 다른 축(로컬 개발자 PC의 저장소 설정)이라 이 표 구조에 맞지 않는다. 실제 Git remote 운영 방식은 아래 "Git 다중 remote" 문단과 10-deployment.md 부록 D에 정리했다.

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

**Git 다중 remote(10-deployment.md 부록 D)는 실제로는 사용하지 않았다**: `git remote set-url --add --push`로 한 origin에 두 push URL을 등록하는 대신, 아래처럼 처리했다.
- `origin` = `https://git.ondalprincess.synology.me/psung616/LightALM.git` (`git remote set-url origin ...`으로 교체)
- 기존 GitHub 저장소로는 `origin`을 거치지 않고 매번 **URL을 직접 지정해 push**: `git push https://github.com/psung616/LightALM.git main`
- 이렇게 나눈 이유: 사내 Git 서버(`git.ondalprincess.synology.me`)에 저장소가 실제로 준비되어 있는지 아직 확인되지 않아, 우선 기존 GitHub에만 안전하게 반영하고 사내 서버 push는 보류했다. 10-deployment.md 부록 D의 다중 push-url 스크립트는 **참고용으로만 남겨두고, 실제로 실행하지는 않았다.**

**CORS**: profile별로 나누지 않고, `docker-compose.yml`의 `CORS_ALLOWED_ORIGINS` 환경변수 하나에 로컬(`http://localhost:5173`)과 테스트 서버 도메인(`https://alm.ondalprincess.synology.me`)을 콤마로 함께 넣는다(`SecurityConfig`가 `,`로 split해서 허용 목록에 반영, 06-auth.md §6 참고).

> ⚠️ `git.ondalprincess.synology.me`가 GitHub.com이 아닌 자체 호스팅 Git 서버(Gitea/Gogs/GitLab CE 등)인 경우, 04-api.md §4.9·07-integrations.md §7.1·08-dev-phases.md Phase 7에서 전제하는 `https://api.github.com` 기반 REST API/`X-Hub-Signature-256` Webhook 포맷이 그대로 맞지 않을 수 있다. GitHub 연동은 이 문서 기준 실제로는 **github.com을 대상으로만 구현·검증**했으며, 자체 호스팅 서버 연동은 별도 확인이 필요하다.

> ⚠️ **[이 문단은 시점상 낡았다 — CURRENT-STATE.md 확인 필요]** 사내 테스트 서버(`https://alm.ondalprincess.synology.me/`) 현재 상태 — 미완료(당시 기준): 이 도메인은 **여러 사람이 같이 쓰는 공유 서버**이고, 당시 확인 결과 아직 우리 LightALM 컨테이너로 연결되어 있지 않았다(다른 기존 애플리케이션 "FactorySolution ALM" 로그인 화면이 그 자리에 떠 있는 것을 직접 접속해 확인함). 이 문단 작성 시점 기준 실제로 End-to-End 검증까지 끝난 것은 **로컬(`http://localhost:5173`)에서 외부 사내 DB(`ondalprincess.synology.me:55432/ALM_Project`)에 붙여 정상 동작하는 것까지**였다.
>
> **이후 상태 변경**: 10-deployment.md 부록 D~E 기준으로 사내 Gitea(`synology` remote)가 준비되고 Jenkins 파이프라인이 연결되어, 이 문단이 "미완료"라고 적었던 `alm.ondalprincess.synology.me` 배포가 실제로 완료됐다(호스트 포트 8888로 서비스 중, ADR-006 참고). **이 경고 문단은 배경 설명(왜 처음엔 로컬 검증까지만 했는지)으로만 참고하고, 현재 배포 상태는 반드시 [`CURRENT-STATE.md`](../00-meta/CURRENT-STATE.md)를 기준으로 판단할 것.**

### 2.5 자동 Git 커밋/Push 정책 (신규)

위 §2.4/10-deployment.md 부록 D에 기록된 과거 이력(사내 Git 서버를 `origin`으로 쓰고 기존 GitHub엔 URL을 직접 지정해 수동 push)은 **더 이상 유효하지 않다.** 새 저장소가 준비되어 아래 정책으로 대체한다.

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

- 커밋 전 매번 `.gitignore`에 실제 운영용 민감정보(예: 진짜 프로덕션 DB 비밀번호, 실배포용 `.env`)가 걸러지는지 확인한다. 단 §2.4·07-integrations.md §7.4에서 이미 결정한 대로, 로컬/사내 테스트용 계정(`lightalm/lightalm`, `postgres/postgres` 등 테스트 전용 값)은 `docker-compose.yml`/`application.yml`에 평문으로 남겨두고 그대로 커밋 대상에 포함한다.
