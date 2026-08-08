> Owner: architect | Status: Accepted, partially extended by ADR-005 | Date: 2026-08-04 이전

# ADR-004: 단일 저장소(LightALM_v2)만 자동 커밋/push 대상으로 확정

## 맥락 (Context)
ADR-003의 사내 Git 서버 origin 전환 시도가 폐기된 이후, 새 저장소가 준비되어 더 단순한 정책이 필요했다.

## 결정 (Decision)
- 저장소(단일): `https://github.com/psung616/LightALM_v2.git` 하나만 자동 커밋/push 대상으로 하고 `origin`이 이를 가리키도록 한다
- Phase 0에서 `git init` → `git remote add origin ...` → 최초 커밋 → `git push -u origin main` 수행
- Phase 0~11 각 Phase의 DoD를 통과할 때마다 해당 Phase 변경분을 커밋하고 즉시 `origin`에 push. 커밋 메시지 형식: `Phase {N}: {phase 요약}`
- `main` 단일 브랜치에 직접 커밋(별도 브랜치 전략 없음)
- 사내 Git 서버는 당시 자동 push 대상에서 제외(저장소 준비 여부 미확인)

## 결과 (Consequences)
- 실제로 Phase 0~15까지 이 정책으로 커밋/push가 수행되어 이미 운영 중이다(08-dev-phases.md 참고)
- 이후 사내 Gitea가 준비되면서 ADR-005가 이 정책을 완전히 대체하지는 않고 **확장**한다 — `origin`은 여전히 유지되고(소스 백업용), `synology`가 추가된다

## Supersedes
ADR-003

## Extended-by
ADR-005 (사내 Gitea remote 추가 — 2026-08-08)

## 참고
- 02-architecture.md §2.5, 08-dev-phases.md
