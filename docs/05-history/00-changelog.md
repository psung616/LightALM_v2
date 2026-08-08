> Owner: orchestrator | Status: current | Last-reviewed: 2026-08-08
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

## 변경 이력

### 2026-08-08 (2) — v3 스코프 확장
- **업계 상용 ALM 툴에서 흔한 기능 카테고리를 참고하여 4개 영역 추가**(01-scope.md §1.2 v3 항목 참고): 리뷰 사이클+베이스라인, 위험 관리, 요구사항 문서 뷰+변형 관리, 대시보드 위젯+리포트 내보내기. 데이터 모델(03-data-model.md §3.18~3.24), API(04-api.md §4.17~4.21), 화면(05-frontend.md §5.11~5.15), 개발 단계(08-dev-phases.md Phase 16~19)에 상세 설계 추가. **아직 구현되지 않았다** — Phase 0~15까지만 완료된 상태다.
- **저작권 준수 원칙 문서 신설**: `docs/01-requirements/02-competitive-reference.md` — 참고한 것/참고하지 않은 것을 명시하고, 기능명을 원 제품과 다르게 재정의한 매핑표 포함
- ~~**ADR-008 작성**~~: 문서 인계 과정에서 파일이 유실되어 존재하지 않는다(2026-08-08 정정) — 위 결정의 맥락/근거를 기록한 ADR은 착수 전 다시 작성 필요

### 2026-08-08
- **문서 구조 2차 개편 — 성격별 폴더 분리**: 평면 구조(`docs/00-*.md` ~ `docs/10-*.md`)를 `docs/00-meta/`, `docs/01-requirements/`, `docs/02-design/`, `docs/03-process/`, `docs/04-operations/`, `docs/05-history/`로 재편했다. 파일명·§-번호는 유지. 각 문서 상단에 `Owner`/`Status`/`Last-reviewed` 메타정보를 추가했다.
- **역할 기반 작업 분담 체계 도입**: 요구사항분석가/아키텍트/개발자/QA·보안 리뷰어/DevOps 5개 역할을 정의(00-meta/ROLES.md 신규). 이 중 architect·developer·qa-tester 3개는 Claude Code 서브에이전트(`.claude/agents/`)로 구현했다. 요구사항분석가·DevOps는 작업 빈도가 낮아 우선 오케스트레이터가 겸임하기로 함.
- **ADR(Architecture Decision Record) 도입**: 본문에 흩어져 있던 "이전엔 이랬으나 실제로는 이렇게 바뀌었다" 류의 결정 이력을 `docs/05-history/adr/`로 분리했다(ADR-001~007). 새 구조적 결정은 앞으로 ADR로 먼저 기록한 뒤 진행한다.
- **CURRENT-STATE.md 신규 도입**: "지금 이 순간 유효한 사실"만 모은 요약 문서를 추가했다. 문서 재편 과정에서 **02-architecture.md §2.4의 사내 테스트 서버 배포 상태 서술이 10-deployment.md 부록 E와 실제로 상충**하고 있음을 발견했다(§2.4는 "미완료"라고 되어 있으나 부록 E는 실제 배포 완료를 서술) — ADR-006으로 정리하고 §2.4에 상호참조를 추가했다.
- **`DESIGN-linear_app.md` 위치 정정**: SPEC 문서 체계에 속하지 않는 프론트엔드 디자인 토큰 참고 자료였음을 확인, `docs/02-design/design-tokens/linear-style-tokens.md`로 이동.

### 2026-08-04
- **문서 구조 개편**: 단일 `SPEC.md`(약 1,050줄)를 관리하기 쉽도록 `docs/` 아래 섹션별 파일로 분할했다. `SPEC.md` 자체는 짧은 인덱스 문서로 남고, 실제 내용은 `docs/01-scope.md` ~ `docs/10-deployment.md`에 있다. 각 파일의 §-번호는 원본과 동일하게 유지했다.
- **v2 스코프 확장 — ALM 기능 4종 추가**: 아래 4개 영역을 명시적 비스코프에서 핵심 스코프로 이동했다(01-scope.md §1.2/§1.3 참고). 데이터 모델(03-data-model.md §3.11~3.17), API(04-api.md §4.12~4.16), 화면(05-frontend.md §5.6~5.10), 개발 단계(08-dev-phases.md Phase 12~15)에 상세 설계를 추가했다. **아직 구현되지 않았다** — Phase 0~11(기존 Light ALM MVP)만 완료된 상태다.
  - 테스트케이스 관리 (Test Case Management)
  - 릴리스/버전 관리 (Release/Version Management)
  - 변경 이력/감사 로그 (Audit Log)
  - 승인 워크플로우 (요구사항 `DRAFT→APPROVED` 전이 1건에 한정된 좁은 승인 게이트 — 범용 워크플로우 엔진 아님)
