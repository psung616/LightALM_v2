> Owner: orchestrator (전체 세션이 매 Phase 종료 시 갱신) | Status: current | Last-reviewed: 2026-08-08
> 상위 문서: [SPEC.md](SPEC.md)

## 이 문서의 목적

개별 섹션 문서(01-scope.md ~ 10-deployment.md)에는 시간이 지나며 "당시엔 이랬으나 나중에 바뀐" 서술이 본문에 섞여 있을 수 있다(예: 배포 상태, Git remote 정책). **이 문서는 지금 이 순간 유효한 사실만 담고, 상충하는 서술이 있으면 이 문서를 우선한다.** 이 문서를 갱신할 때는 근거가 된 원본 문서/ADR을 함께 표기한다.

작업을 시작하기 전 이 문서를 먼저 읽고, 필요하면 관련 섹션 문서로 들어가 상세를 확인하는 순서를 권장한다.

---

## 1. 구현 진행 상태

| 항목 | 상태 |
|---|---|
| Phase 0~11 (Light ALM MVP: 인증, 프로젝트, 요구사항, 이슈, 추적성, GitHub/Jenkins 연동, 프론트엔드) | ✅ 완료, 운영 중 |
| Phase 12 (테스트케이스 & 테스트 실행) | ✅ 완료 (2026-08-04) |
| Phase 13 (릴리스/버전 관리) | ✅ 완료 (2026-08-04) |
| Phase 14 (변경 이력/감사 로그) | ✅ 완료 (2026-08-04) |
| Phase 15 (승인 워크플로우) | ✅ 완료 (2026-08-04) |
| Phase 16~19 (v3: 리뷰 사이클+베이스라인 / 위험 관리 / 문서 뷰+변형 관리 / 대시보드 위젯+리포트 내보내기) | 🔲 설계 완료, **구현 전** (2026-08-08 설계) |

근거: 08-dev-phases.md

## 2. 배포 상태

- **운영 서비스**: `https://alm.ondalprincess.synology.me/` — **실제로 서비스 중** (사내 Gitea `synology` remote push → Jenkins `ALM_Pipeline` 자동 배포, 호스트 포트 8888)
- **로컬 개발**: `http://localhost:5173`(프론트) / `http://localhost:8080`(백엔드) — `docker compose up --build`로 기동, 검증 완료
- ⚠️ 02-architecture.md §2.4에는 이 도메인이 "아직 연결되지 않음(미완료)"이라는 문단이 남아있는데, 이는 **낡은 서술**이다. ADR-006 참고.

근거: 10-deployment.md 부록 E, ADR-006

## 3. DB 접속 정보

| 항목 | 값 |
|---|---|
| Host | `ondalprincess.synology.me` |
| Port | `55432` |
| DB | `ALM_Project` |
| 계정 | `postgres` / `postgres` |
| 비고 | 로컬 개발용 자체 DB가 아니라 **사내 공용 Postgres**를 그대로 씀(KPI 집계 등 다른 도구가 같은 DB를 봄). 로컬/사내 테스트 서버 구분 없이 이 DB 하나만 사용 중 |

근거: 02-architecture.md §2.4, 10-deployment.md 부록 B·E, ADR-002

## 4. Git 원격 저장소 — **반드시 최신 기준으로 확인**

| remote | URL | 용도 |
|---|---|---|
| `origin` | `https://github.com/psung616/LightALM_v2.git` | 소스 백업. push해도 자동 배포 없음 |
| `synology` | `https://git.ondalprincess.synology.me/FactorySolution/ALM_Repository` | **운영 배포 트리거.** push하면 Jenkins가 자동 배포 |

**운영 서버에 반영하려면 반드시 `synology`에도 push해야 한다.** `origin`에만 push하면 배포되지 않는다.

⚠️ 08-dev-phases.md와 02-architecture.md §2.5 본문에는 "각 Phase마다 `origin`에만 push한다"는 더 이전 정책이 그대로 남아있다. **이는 ADR-005(2026-08-08)로 대체됐다** — 지금부터 Phase/기능을 추가로 구현할 때는 `origin` + `synology` 둘 다 push해야 실제 서비스에 반영된다.

근거: 10-deployment.md 부록 D(2026-08-08 갱신), ADR-004, ADR-005

## 5. 계정 정보

| 항목 | 값 |
|---|---|
| 최초 관리자 | `admin` / `admin1234` (V2__seed_admin.sql 시드값, 로그인 후 변경 권장) |
| Jenkins | `https://jenkins.ondalprincess.synology.me/job/ALM_Pipeline/` |

## 6. 알려진 리스크 / TODO (아직 해결 안 됨)

- GitHub PAT, Jenkins API 토큰이 DB에 평문 저장됨 — 운영 전환 시 암호화 필요(07-integrations.md §7.4)
- DB 계정(`postgres/postgres`)이 `docker-compose.yml`/Jenkinsfile에 평문으로 커밋되어 있음 — Jenkins Credentials로 이전 필요(10-deployment.md 부록 E)
- Flyway `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`로 체크섬 검증을 우회 중 — 스키마 드리프트를 놓칠 수 있는 상태
- `synology` push가 곧바로 운영 배포로 이어지는데 별도의 배포 승인/검토 게이트가 없음(ADR-005 리스크 항목 참고)
- `synology` remote 저장소명(`ALM_Repository`)이 GitHub 저장소명(`LightALM_v2`)과 달라 혼동 가능성 있음

## 갱신 이력
- 2026-08-08: 문서 구조 2차 개편과 함께 최초 작성. ADR-001~007 정리 및 02-architecture.md §2.4 낡은 경고에 상호참조 추가
