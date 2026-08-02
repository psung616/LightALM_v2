# Light ALM

Jira/Azure DevOps 같은 무거운 ALM 대신, 요구사항 관리·이슈 트래킹·추적성 관리만 지원하는 경량 웹 시스템입니다.

## 기술 스택

- 백엔드: Java 21, Spring Boot 3.3.x, Spring Data JPA, Spring Security(세션 기반), Flyway, PostgreSQL 15+
- 프론트엔드: React 18 + TypeScript(Vite), react-router-dom, @tanstack/react-query, Tailwind CSS
- 빌드: 백엔드는 Maven Wrapper(`mvnw`/`mvnw.cmd`, 시스템 Maven 불필요)

## 리포지토리 구조

```
LightALM_v2/
├── backend/    # Spring Boot API 서버
├── frontend/   # React SPA
└── docker-compose.yml
```

## 로컬 개발 실행

### 1) 전체 스택을 Docker로 한 번에 기동

```bash
docker compose up --build
```

- 프론트엔드: http://localhost:5173
- 백엔드 API: http://localhost:8080/api
- PostgreSQL: localhost:5432 (DB `lightalm`, 계정 `lightalm`/`lightalm`)

`frontend` 컨테이너는 nginx로 서빙되며, `/api/**` 요청을 `frontend/nginx.conf`가 `backend:8080`으로 프록시합니다. 별도의 리버스 프록시 설정 없이 frontend 컨테이너 하나만 외부에 노출하면 됩니다.

### 2) 백엔드/프론트엔드를 각각 로컬에서 직접 실행 (개발용)

```bash
docker compose up -d postgres

cd backend
mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run        # macOS/Linux
```

```bash
cd frontend
npm install
npm run dev
```

프론트엔드는 기본적으로 `http://localhost:8080/api`를 백엔드 주소로 사용합니다(`VITE_API_BASE_URL` 환경변수로 오버라이드 가능).

### 외부 DB에 붙이고 싶을 때

`application.yml`은 profile 분리 없이 환경변수 기본값(`${DB_HOST:localhost}` 등)으로 접속 정보를 오버라이드합니다. `docker-compose.yml`의 `backend.environment` 또는 로컬 실행 시 환경변수로 `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`만 바꾸면 됩니다.

## 초기 관리자 계정

Flyway 시드 데이터(`V2__seed_admin.sql`)로 아래 계정이 자동 생성됩니다.

| 아이디 | 비밀번호 |
|---|---|
| `admin` | `admin1234` |

최초 로그인 후 반드시 비밀번호를 변경하세요(`PUT /api/users/me/password`).

## GitHub 연동 설정

1. 프로젝트 설정 → GitHub 연동 탭에서 `repoOwner`, `repoName`, Personal Access Token, Webhook Secret을 입력합니다.
2. 대상 GitHub 저장소 Settings → Webhooks에서 Payload URL을 `https://{서버주소}/api/webhooks/github/{projectId}`로, Content type을 `application/json`으로, Secret을 1번과 동일하게 등록합니다.
3. `push`, `pull_request` 이벤트를 구독하면, 커밋 메시지/PR 제목·본문에 `{PROJECT_KEY}-123`(이슈) 또는 `{PROJECT_KEY}-R123`(요구사항) 패턴이 있을 때 자동으로 연결됩니다.
4. GitHub 연동은 github.com 기준으로 구현·검증되었습니다. 자체 호스팅 Git 서버(Gitea/GitLab CE 등)는 API/Webhook 포맷이 다를 수 있어 별도 확인이 필요합니다.

## Jenkins 연동 설정

1. 프로젝트 설정 → Jenkins 연동 탭에서 `baseUrl`, `jobName`, API 사용자/토큰을 입력합니다.
2. 이슈/요구사항 상세 화면의 "빌드 트리거" 버튼은 `POST {baseUrl}/job/{jobName}/build`를 Basic Auth로 호출합니다.
3. Jenkins Job의 Post-build Action(HTTP Request Plugin 등)에서 빌드 종료 시 아래 형태로 `POST /api/webhooks/jenkins/{projectId}`를 호출하도록 구성합니다. 헤더 `X-Jenkins-Token`에 프로젝트의 GitHub Webhook Secret과 동일한 값을 담아 보내야 합니다(간소화를 위해 시크릿을 공유합니다).

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

## 테스트

```bash
cd backend
mvnw.cmd test        # 단위 테스트 (Testcontainers 통합 테스트 제외, 항상 빠르게 통과)
mvnw.cmd verify       # 단위 + 통합 테스트(*IT.java, Testcontainers PostgreSQL 필요)
```

Windows + Docker Desktop 환경에서 Testcontainers가 기본 named pipe를 찾지 못해 실패하는 경우가 있어, Testcontainers를 사용하는 통합 테스트는 `*IT.java`로 명명하고 `maven-failsafe-plugin`으로 `mvn test`(surefire)에서 제외했습니다. `mvn verify`에서만 실행됩니다.

## Git 저장소 정책

- `origin` = `https://github.com/psung616/LightALM_v2.git` 단일 저장소를 사용합니다.
- `main` 브랜치에 직접 커밋하며, Phase 0~11 각 단계 완료 시마다 커밋·push합니다.

## 보안 참고사항

- GitHub PAT, Jenkins API 토큰은 MVP 단계에서 DB 평문 저장을 허용합니다. 프로덕션 전환 시 Jasypt 등을 이용한 컬럼 암호화가 필요합니다.
- API 응답에서 토큰 필드는 항상 마스킹(`****`)되어 반환됩니다.
