> 상위 문서: [SPEC.md](SPEC.md)

## 11. 실행/배포
- 로컬 개발: `docker compose up -d postgres` → 백엔드는 `mvnw.cmd spring-boot:run`(Windows) / `./mvnw spring-boot:run`(macOS·Linux), 프론트는 `npm run dev`. Maven을 시스템에 설치하지 않아도 되도록 **Maven Wrapper(`mvnw`)를 프로젝트에 포함**한다.
- 전체 스택(로컬/사내 테스트 서버 공용): `docker compose up --build` (postgres + backend + frontend 이미지 빌드 포함, 부록 B 참고). DB를 외부(사내 NAS)로 돌리고 싶으면 `backend` 서비스의 `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` 환경변수만 바꾸면 되고, 별도의 `docker-compose.test.yml` 파일은 두지 않는다.
- Jenkins CI 파이프라인 예시 단계: `checkout` → `mvnw.cmd test`(백엔드, `*IT.java` 통합 테스트는 제외됨) → `npm ci && npm run build` (프론트) → `docker build` → (선택) 배포 → Post-build Action에서 07-integrations.md §7.2의 Webhook payload 전송

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

> 위 사내 DB 계정(`postgres/postgres`)은 테스트 전용이라는 전제로 문서에 남겨두지만, 실제로는 `docker-compose.yml`에 평문으로 들어가 있다(02-architecture.md §2.4). 운영 전환 시 계정/비밀번호를 교체하고 커밋에서 제외하는 방식으로 바꿔야 한다.

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
        VITE_API_BASE_URL: /api   # 상대 경로. frontend/nginx.conf가 /api를 backend:8080으로 프록시(02-architecture.md §2.4, 필수)
    depends_on:
      - backend
    ports:
      - "5173:80"

volumes:
  pgdata:
```

`frontend/nginx.conf`(반드시 함께 존재해야 함, 02-architecture.md §2.4 참고):
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

## 부록 D. Git 원격 저장소 — 현재 정책 (2026-08-08 갱신)

> 이 부록은 이전에 "사내 Git 서버가 아직 준비 안 됨"이라고 기록했었다. **2026-08-08부로 사내 Gitea가 실제로 준비되어 `synology`라는 이름의 두 번째 remote로 연동됐고, 여기 push하면 Jenkins가 자동으로 운영 배포까지 한다.** 아래가 현재 유효한 정책이다.

| remote 이름 | URL | 용도 |
|---|---|---|
| `origin` | `https://github.com/psung616/LightALM_v2.git` | 소스 백업/개인 저장소. push해도 아무 자동화 없음 |
| `synology` | `https://git.ondalprincess.synology.me/FactorySolution/ALM_Repository` | **운영 배포 트리거.** 사내 Gitea, push하면 webhook으로 Jenkins `ALM_Pipeline`이 자동 실행되어 `https://alm.ondalprincess.synology.me/`에 반영됨 |

```bash
git remote add origin https://github.com/psung616/LightALM_v2.git
git remote add synology https://git.ondalprincess.synology.me/FactorySolution/ALM_Repository

# 배포하려면 반드시 synology에도 push
git push origin main
git push synology main
```

인증은 Git Credential Manager(GCM)가 처음 push할 때 브라우저 OAuth 창을 띄워서 처리한다(사내 Gitea 계정으로 승인). 두 저장소에 항상 동시에 반영하고 싶으면 `origin`에 push URL을 두 개 등록하는 방법도 가능하다.

```bash
git remote set-url --add --push origin https://github.com/psung616/LightALM_v2.git
git remote set-url --add --push origin https://git.ondalprincess.synology.me/FactorySolution/ALM_Repository

git push origin main   # 두 저장소에 동시 반영
```

## 부록 E. 실제 운영 배포 — Synology Jenkins (docker-compose 아님)

> **중요**: 부록 B의 `docker-compose.yml`은 로컬 개발용으로만 쓰인다. 운영(`alm.ondalprincess.synology.me`) 배포는 이 compose 파일을 쓰지 않고, 레포 루트의 **`Jenkinsfile`**이 순수 `docker build`/`docker run`으로 별도 수행한다. 이유: Jenkins 에이전트에 `docker compose`(v2)도 `docker-compose`(v1)도 설치돼 있지 않음.

**흐름**: `git push synology main` → Gitea webhook → Jenkins `ALM_Pipeline` 자동 실행 → 약 1~3분 내 `https://alm.ondalprincess.synology.me/`에 반영.

파이프라인 단계(`Jenkinsfile`):
1. **Checkout** — Gitea에서 최신 커밋 가져옴
2. **Build Images** — `docker build`로 `lightalm-backend`(Spring Boot jar), `lightalm-frontend`(React 빌드 + nginx) 이미지 생성. 소스 변경 없는 레이어는 캐시돼서 보통 수십 초 내 끝남
3. **Deploy** — 기존 `lightalm-backend`/`lightalm-frontend`(및 과거 잔재인 `factorysolution-alm`, `lightalm-postgres`) 컨테이너 정리 → 전용 브리지 네트워크 `lightalm-net`에 `lightalm-backend`(alias `backend`, DB/CORS 환경변수 주입) 기동 → 부팅 시 Flyway가 새 마이그레이션을 자동 적용 → `lightalm-frontend`를 호스트 포트 **8888**로 기동(기존 리버스 프록시가 이 포트를 `alm.ondalprincess.synology.me`로 이미 연결해둔 상태라 DSM 설정은 안 건드림)

접속/계정 정보:
- 서비스: `https://alm.ondalprincess.synology.me/` (프록시 → 호스트 8888 → `lightalm-frontend` 컨테이너 → nginx가 `/api`만 `lightalm-backend:8080`으로 프록시)
- 최초 관리자 계정: `admin` / `admin1234` (`V2__seed_admin.sql` 시드값, 로그인 후 변경 권장)
- DB: `ondalprincess.synology.me:55432` / db `ALM_Project` / `postgres`/`postgres` — **로컬 개발용 자체 DB가 아니라 사내에서 이미 쓰던 공용 Postgres**를 그대로 씀 (KPI 집계 등 다른 도구가 같은 DB를 보고 있어서 의도적으로 이렇게 함)
- Jenkins: `https://jenkins.ondalprincess.synology.me/job/ALM_Pipeline/` — 빌드/배포 로그 확인은 여기서 `Console Output`

배포 시 반드시 지켜야 할 것:
- **기존 마이그레이션 파일(V1~V7)은 절대 수정하지 않는다.** 이미 `ALM_Project` DB에 적용된 이력이 있어서, 파일을 고치면 체크섬이 다시 어긋나거나(현재는 `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`로 우회 중) 실제 스키마와 또 어긋날 수 있다. 스키마를 바꿔야 하면 항상 새 `V{n+1}__설명.sql`을 추가한다(가능하면 `ADD COLUMN IF NOT EXISTS` 등 idempotent하게).
- Jenkinsfile에 DB 계정(`postgres`/`postgres`)이 평문으로 들어있다 — 사내 전용 저장소이긴 하지만 운영 전환 시 Jenkins Credentials로 옮기는 게 안전하다.
- 배포 실패 시: Jenkins 해당 빌드의 Console Output에서 `docker build`/`docker run` 단계 오류 확인, 또는 컨테이너가 뜨자마자 죽는 경우 Jenkinsfile Deploy 스테이지 마지막에 `docker logs lightalm-backend` 한 줄을 임시로 추가해서 원인을 본다(현재는 `docker ps` 상태 체크만 남겨둠).
