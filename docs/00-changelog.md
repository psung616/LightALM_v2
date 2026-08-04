> 상위 문서: [SPEC.md](SPEC.md)

## 변경 이력

### 2026-08-04
- **문서 구조 개편**: 단일 `SPEC.md`(약 1,050줄)를 관리하기 쉽도록 `docs/` 아래 섹션별 파일로 분할했다. `SPEC.md` 자체는 짧은 인덱스 문서로 남고, 실제 내용은 `docs/01-scope.md` ~ `docs/10-deployment.md`에 있다. 각 파일의 §-번호는 원본과 동일하게 유지했다.
- **v2 스코프 확장 — ALM 기능 4종 추가**: 아래 4개 영역을 명시적 비스코프에서 핵심 스코프로 이동했다(01-scope.md §1.2/§1.3 참고). 데이터 모델(03-data-model.md §3.11~3.17), API(04-api.md §4.12~4.16), 화면(05-frontend.md §5.6~5.10), 개발 단계(08-dev-phases.md Phase 12~15)에 상세 설계를 추가했다. **아직 구현되지 않았다** — Phase 0~11(기존 Light ALM MVP)만 완료된 상태다.
  - 테스트케이스 관리 (Test Case Management)
  - 릴리스/버전 관리 (Release/Version Management)
  - 변경 이력/감사 로그 (Audit Log)
  - 승인 워크플로우 (요구사항 `DRAFT→APPROVED` 전이 1건에 한정된 좁은 승인 게이트 — 범용 워크플로우 엔진 아님)
