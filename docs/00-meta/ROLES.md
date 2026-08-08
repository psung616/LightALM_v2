> Owner: orchestrator | Status: current | Last-reviewed: 2026-08-08
> 상위 문서: [SPEC.md](SPEC.md)

## 이 문서의 목적

Light ALM 프로젝트를 "여러 역할을 가진 팀"이 진행하는 것처럼 운영하기 위한 역할 정의서다. Claude Code에서는 `.claude/agents/`에 정의된 서브에이전트가 이 역할들을 맡는다(현재 architect / developer / qa-tester 3개 구현됨 — §3 참고). 나머지 역할(requirements-analyst, devops)은 아직 전담 서브에이전트가 없고, 오케스트레이터(메인 세션)가 겸임한다.

## 1. 역할 정의

### 오케스트레이터 (Orchestrator, 메인 세션)
- **책임**: 전체 작업 조율, 어떤 요청을 어느 역할에 위임할지 판단, Phase 진행 관리, 역할 간 충돌 시 최종 판단
- **소유 문서**: SPEC.md, ROLES.md, CURRENT-STATE.md, 00-changelog.md
- **하지 않는 일**: 직접 스키마를 설계하거나 코드를 대량으로 작성하지 않는다 — 해당 성격의 작업은 위임한다

### 요구사항 분석가 (requirements-analyst) — 전담 에이전트 미생성
- **책임**: 스코프 확정/변경, 우선순위 결정, 비스코프 항목 판단
- **소유 문서**: 01-scope.md
- **산출물**: 스코프 변경안, 00-changelog.md 항목
- **다음 역할로 넘기는 조건**: 스코프가 확정되면 architect에게 설계를 요청

### 아키텍트 (architect) — `.claude/agents/architect.md`
- **책임**: 시스템 구조, 데이터 모델, API 설계, 화면 라우트 설계, 구조적 결정 기록(ADR)
- **소유 문서**: 02-architecture.md, 03-data-model.md, 04-api.md, 05-frontend.md, 06-auth.md(설계), 07-integrations.md(설계), 05-history/adr/
- **산출물**: 설계 문서 갱신, ADR
- **원칙**: 구조를 바꾸는 결정을 내릴 때는 반드시 먼저 ADR을 작성한다. 기존 V1~V7 Flyway 마이그레이션 파일은 수정하지 않는다(ADR-002, ADR-006 참고)
- **다음 역할로 넘기는 조건**: 설계가 확정되면 developer에게 구현을 위임

### 개발자 (developer: backend/frontend) — `.claude/agents/developer.md`
- **책임**: 08-dev-phases.md의 Phase 단위 구현(엔티티/API/화면), 단위 테스트 작성
- **소유 문서**: 실제 소스 코드(backend/, frontend/), 08-dev-phases.md는 참고(소유는 오케스트레이터)
- **산출물**: 동작하는 코드, Phase DoD 통과 근거
- **원칙**: 설계 문서(02~05)에 없는 내용을 임의로 추가하지 않는다. 애매하면 architect에게 확인
- **다음 역할로 넘기는 조건**: Phase DoD를 통과하면 qa-tester에게 검증을 요청

### QA · 보안 리뷰어 (qa-tester) — `.claude/agents/qa-tester.md`
- **책임**: 09-quality-testing.md 기준 테스트 작성/실행, 06-auth.md 기준 인증/인가 로직 검증, 알려진 리스크(CURRENT-STATE.md §6) 점검
- **소유 문서**: 09-quality-testing.md, 06-auth.md(검증 관점)
- **산출물**: 테스트 결과, 발견된 이슈 목록
- **원칙**: 통과 기준을 낮추지 않는다. 실패하는 테스트를 스킵 처리로 우회하지 않는다
- **다음 역할로 넘기는 조건**: 통과하면 devops에게 배포를 요청. 실패하면 developer에게 반려

### DevOps 담당 (devops) — 전담 에이전트 미생성
- **책임**: 10-deployment.md 기준 배포, CI/CD(Jenkinsfile) 관리, 환경변수/시크릿 관리
- **소유 문서**: 10-deployment.md
- **산출물**: 배포 완료, CURRENT-STATE.md 갱신
- **원칙**: `synology` remote에 push하는 것은 실제 운영 배포를 트리거하므로, 명시적 배포 의도가 있을 때만 수행한다(ADR-005 리스크 참고)
- **다음 역할로 넘기는 조건**: 배포 완료 후 CURRENT-STATE.md를 갱신하고 요구사항 분석가에게 결과를 공유

## 2. 핸드오프 요약표

| 역할 | 소유 문서 | 산출물 | 다음 역할로 넘기는 조건 |
|---|---|---|---|
| 요구사항 분석가 | 01-scope.md | 스코프 확정 | 스코프 승인 → architect |
| 아키텍트 | 02~05, ADR | 설계 문서, ADR | 설계 확정 → developer |
| 개발자 | 소스 코드 | Phase 구현 | DoD 통과 → qa-tester |
| QA·보안 | 09, 06 | 테스트 결과 | 통과 → devops / 실패 → developer |
| DevOps | 10-deployment.md | 배포 완료 | 배포 완료 → 요구사항 분석가(피드백 루프) |

## 3. Claude Code 서브에이전트 매핑 현황

| 역할 | 서브에이전트 파일 | 상태 |
|---|---|---|
| 아키텍트 | `.claude/agents/architect.md` | ✅ 구현됨 |
| 개발자 | `.claude/agents/developer.md` | ✅ 구현됨 |
| QA·보안 | `.claude/agents/qa-tester.md` | ✅ 구현됨 |
| 요구사항 분석가 | — | 미구현 (오케스트레이터가 겸임) |
| DevOps | — | 미구현 (오케스트레이터가 겸임) |

요구사항 분석가/DevOps는 작업 빈도가 낮고(스코프는 자주 안 바뀌고, 배포는 이미 파이프라인화되어 있음) 우선순위가 낮아 3개 핵심 역할부터 검증 후 필요 시 추가하기로 했다(2026-08-08 결정).

## 4. 역할 간 충돌 시 원칙

- 설계(architect)와 구현(developer)이 상충하면 **설계가 우선** — 단, developer가 설계의 결함을 발견하면 코드를 먼저 고치지 말고 architect에게 ADR로 재논의를 요청한다
- QA가 발견한 문제와 스코프(01-scope.md)가 상충하면 — 스코프 밖 버그라도 보안/데이터 무결성 문제면 요구사항 분석가 승인 없이도 즉시 수정한다(09-quality-testing.md 원칙)
- 여러 역할이 동시에 같은 문서를 고치려 하면 오케스트레이터가 순서를 조정한다
