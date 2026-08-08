# Light ALM — 구현 명세서 (SPEC.md)

> 이 문서는 Claude Code(AI 코딩 에이전트)가 별도 질의 없이 바로 파일 생성 및 코딩을 시작할 수 있도록 작성된 실행형 명세서다. 모든 결정 사항(스코프, 스키마, API, 화면, 개발 순서)이 확정되어 있으므로, 구현 중 모호한 부분이 있으면 이 문서의 원칙(01-scope.md §1 스코프, 09-quality-testing.md §9 컨벤션)에 따라 가장 단순한 방식으로 판단하고 진행한다.
>
> **이 문서는 여러 파일로 나뉘어 있다.** 아래 "문서 구성" 표에서 원하는 섹션을 찾아 들어가면 된다. 최근 변경 사항은 [`00-changelog.md`](../05-history/00-changelog.md)에서 확인할 수 있다.
>
> **2026-08-08 문서 체계 2차 개편**: 기존 평면 구조(`docs/00-*.md` ~ `docs/10-*.md`)를 성격별 폴더(`00-meta/`, `01-requirements/`, `02-design/`, `03-process/`, `04-operations/`, `05-history/`)로 재편했다. 각 파일의 §-번호와 파일명은 그대로 유지했으므로 기존 상호 참조(예: "05-frontend.md §5.4")는 파일명 검색으로 여전히 찾을 수 있다. 이번 개편과 함께 **역할 기반 작업 분담 체계**를 도입했다 — 상세는 [`ROLES.md`](ROLES.md) 참고. 문서마다 상단에 `Owner`/`Status`/`Last-reviewed` 메타정보를 추가했다. "지금 이 순간 유효한 사실"만 모은 요약은 [`CURRENT-STATE.md`](CURRENT-STATE.md)를 우선 참고할 것 — 개별 섹션 문서 본문에는 시간이 지나며 낡은 서술이 섞여 있을 수 있다.

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

### 메타 문서 (신규, 2026-08-08)
| 파일 | 내용 |
|---|---|
| [`ROLES.md`](ROLES.md) | 역할 기반 작업 분담 정의 — 요구사항분석가/아키텍트/개발자/QA·보안/DevOps의 소유 문서, 산출물, 핸드오프 조건 |
| [`CURRENT-STATE.md`](CURRENT-STATE.md) | 지금 이 순간 유효한 사실 요약(배포 URL, DB 접속정보, Git remote, 완료된 Phase) — 개별 문서 본문과 상충하면 이 문서가 우선 |

### 스펙 문서
| 파일 | 내용 |
|---|---|
| [`00-changelog.md`](../05-history/00-changelog.md) | 문서/스코프 변경 이력 |
| [`01-scope.md`](../01-requirements/01-scope.md) | §1 프로젝트 목적, 핵심 스코프, 비스코프, 사용자 역할 |
| [`02-architecture.md`](../02-design/02-architecture.md) | §2 아키텍처 개요, 리포지토리 구조, 기술 스택, 실행 환경, Git 정책 |
| [`03-data-model.md`](../02-design/03-data-model.md) | §3 데이터 모델(엔티티/DB 테이블/ERD) — 테스트케이스·릴리스·감사로그·승인 포함 |
| [`04-api.md`](../02-design/04-api.md) | §4 REST API 명세 — 테스트케이스·릴리스·감사로그·승인 API 포함 |
| [`05-frontend.md`](../02-design/05-frontend.md) | §5 프론트엔드 화면/라우트 — 신규 화면 포함 |
| [`06-auth.md`](../03-process/06-auth.md) | §6 인증/인가 상세 설계 |
| [`07-integrations.md`](../03-process/07-integrations.md) | §7 GitHub/Jenkins 외부 연동 상세 |
| [`08-dev-phases.md`](../03-process/08-dev-phases.md) | §8 단계별 개발 순서(Phase 0~11 완료, Phase 12~15 신규 미구현) |
| [`09-quality-testing.md`](../03-process/09-quality-testing.md) | §9 코딩 컨벤션, §10 테스트 전략 |
| [`10-deployment.md`](../04-operations/10-deployment.md) | §11 실행/배포, 부록 A~D(application.yml, docker-compose.yml, Git 원격 저장소) |

### 그 외 참고 자료
| 위치 | 내용 |
|---|---|
| [`05-history/adr/`](../05-history/adr/) | Architecture Decision Record — "이전엔 이랬으나 실제로는 이렇게 바뀌었다" 류의 결정 이력을 본문에서 분리해 여기에 기록한다. 새 결정을 내릴 때마다 번호를 이어서 추가한다 |
| [`02-design/design-tokens/linear-style-tokens.md`](../02-design/design-tokens/linear-style-tokens.md) | 프론트엔드 디자인 토큰 참고 자료(SPEC 문서 체계에는 속하지 않음) |

---

이 문서(`docs/00-meta/SPEC.md` 및 위 분할 문서들)를 Claude Code 세션에 두고 "이 SPEC.md에 따라 다음 작업을 진행해줘"라고 지시하면, 08-dev-phases.md의 순서/DoD를 기준으로 파일 생성 및 코딩을 진행한다. **Git 커밋/push 정책은 시점에 따라 바뀌었으므로 02-architecture.md §2.5를 직접 인용하지 말고, 항상 [`CURRENT-STATE.md`](CURRENT-STATE.md)의 "Git 원격 저장소" 항목을 최신 기준으로 확인할 것**(2026-08-08 기준 `origin` + `synology` 두 remote에 모두 push해야 실제 운영 배포가 트리거된다 — 10-deployment.md 부록 D·E 참고). **Java 버전은 21을 사용하고 Maven Wrapper(`mvnw`)를 포함시켜 `mvn` 대신 `mvnw`/`mvnw.cmd`로 실행할 것.**

**작업자 역할이 여러 명 있다고 가정하고 진행하는 경우**: [`ROLES.md`](ROLES.md)의 역할 정의를 따르고, `.claude/agents/`에 정의된 서브에이전트(architect, developer, qa-tester)를 해당 성격의 작업에 위임한다. 구조/스키마를 변경하는 결정을 내릴 때는 `05-history/adr/`에 ADR을 먼저 남긴 뒤 진행한다.
