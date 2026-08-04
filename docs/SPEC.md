# Light ALM — 구현 명세서 (SPEC.md)

> 이 문서는 Claude Code(AI 코딩 에이전트)가 별도 질의 없이 바로 파일 생성 및 코딩을 시작할 수 있도록 작성된 실행형 명세서다. 모든 결정 사항(스코프, 스키마, API, 화면, 개발 순서)이 확정되어 있으므로, 구현 중 모호한 부분이 있으면 이 문서의 원칙(01-scope.md §1 스코프, 09-quality-testing.md §9 컨벤션)에 따라 가장 단순한 방식으로 판단하고 진행한다.
>
> **이 문서는 여러 파일로 나뉘어 있다.** 아래 "문서 구성" 표에서 원하는 섹션을 찾아 들어가면 된다. 최근 변경 사항은 [`00-changelog.md`](00-changelog.md)에서 확인할 수 있다.

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

## 문서 구성

| 파일 | 내용 |
|---|---|
| [`00-changelog.md`](00-changelog.md) | 문서/스코프 변경 이력 |
| [`01-scope.md`](01-scope.md) | §1 프로젝트 목적, 핵심 스코프, 비스코프, 사용자 역할 |
| [`02-architecture.md`](02-architecture.md) | §2 아키텍처 개요, 리포지토리 구조, 기술 스택, 실행 환경, Git 정책 |
| [`03-data-model.md`](03-data-model.md) | §3 데이터 모델(엔티티/DB 테이블/ERD) — 테스트케이스·릴리스·감사로그·승인 포함 |
| [`04-api.md`](04-api.md) | §4 REST API 명세 — 테스트케이스·릴리스·감사로그·승인 API 포함 |
| [`05-frontend.md`](05-frontend.md) | §5 프론트엔드 화면/라우트 — 신규 화면 포함 |
| [`06-auth.md`](06-auth.md) | §6 인증/인가 상세 설계 |
| [`07-integrations.md`](07-integrations.md) | §7 GitHub/Jenkins 외부 연동 상세 |
| [`08-dev-phases.md`](08-dev-phases.md) | §8 단계별 개발 순서(Phase 0~11 완료, Phase 12~15 신규 미구현) |
| [`09-quality-testing.md`](09-quality-testing.md) | §9 코딩 컨벤션, §10 테스트 전략 |
| [`10-deployment.md`](10-deployment.md) | §11 실행/배포, 부록 A~D(application.yml, docker-compose.yml, Git 원격 저장소) |

---

이 문서(`docs/SPEC.md` 및 위 분할 문서들)를 Claude Code 세션에 두고 "이 SPEC.md에 따라 Phase 0부터 순서대로 구현해줘"라고 지시하면, 08-dev-phases.md의 순서대로 파일 생성 및 코딩을 진행하며, 02-architecture.md §2.5 정책에 따라 각 Phase마다 `https://github.com/psung616/LightALM_v2`에 자동으로 커밋·push한다. **단, Java 버전은 21을 사용하고 Maven Wrapper(`mvnw`)를 포함시켜 `mvn` 대신 `mvnw`/`mvnw.cmd`로 실행할 것.**
