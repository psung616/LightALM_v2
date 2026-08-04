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

## 부록 D. Git 원격 저장소 — 현재 정책 (02-architecture.md §2.5)

> 이전 버전의 이 부록은 "사내 Git 서버를 origin으로 쓰고 기존 GitHub엔 URL을 직접 지정해 수동 push"하는 과거 이력을 기록하고 있었다. **`https://github.com/psung616/LightALM_v2` 저장소가 새로 준비되면서 아래 정책으로 대체한다.** 과거 이력은 더 이상 유효하지 않으니 아래만 따른다.

**현재 사용(자동)**: `origin` = `https://github.com/psung616/LightALM_v2.git` 하나만 쓰고, Phase 0~11 각 Phase DoD 통과 시마다 자동으로 커밋 후 이 저장소에 push한다(02-architecture.md §2.5, 08-dev-phases.md Phase 0 참고).

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
