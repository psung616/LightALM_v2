# ADR (Architecture Decision Record) 목록

각 문서 본문에 섞여 있던 "이전엔 이랬으나 실제로는 이렇게 바뀌었다" 류의 결정 이력을 여기로 분리했다. **새로운 구조/정책 결정을 내릴 때는 반드시 여기에 번호를 이어서 ADR을 추가한 뒤 진행한다** (담당: architect).

| 번호 | 제목 | 상태 | 요약 |
|---|---|---|---|
| [ADR-001](ADR-001-환경변수-오버라이드-방식-채택.md) | 환경변수 오버라이드 방식 채택 | Accepted | Spring profile 분리 대신 `${VAR:default}` 단일 설정 |
| [ADR-002](ADR-002-사내-공용-Postgres-사용.md) | 사내 공용 Postgres 사용 | Accepted | 로컬 postgres 컨테이너 대신 NAS 공용 DB 사용 |
| [ADR-003](ADR-003-사내Git서버-origin전환-시도.md) | 사내 Git 서버 origin 전환 시도 | Superseded by ADR-004 | 실행되지 않고 폐기된 시도 (이력 기록용) |
| [ADR-004](ADR-004-단일-origin-LightALM_v2-확정.md) | 단일 origin(LightALM_v2) 확정 | Accepted, Extended by ADR-005 | GitHub 저장소 하나만 자동 push 대상 |
| [ADR-005](ADR-005-synology-remote-운영배포트리거.md) | synology remote 추가 | **Accepted (현재 유효)** | push 시 Jenkins 자동 운영 배포 트리거 (2026-08-08) |
| [ADR-006](ADR-006-Jenkinsfile-기반-운영배포-확정.md) | Jenkinsfile 기반 운영 배포 확정 | **Accepted (현재 유효)** | docker-compose 아님. 02-architecture.md §2.4의 낡은 "미완료" 경고 해소 |
| [ADR-007](ADR-007-Testcontainers-IT분리.md) | Testcontainers IT 분리 | Accepted | `*IT.java` + failsafe로 통합 테스트 분리 |

## ADR 작성 규칙
- 파일명: `ADR-{번호}-{짧은-제목}.md`
- 필수 섹션: 맥락(Context) → 결정(Decision) → 결과(Consequences)
- 이전 결정을 뒤집는 경우 반드시 `Supersedes` / `Superseded-by`를 명시하고, 이전 ADR의 Status를 `Superseded by ADR-{N}`으로 갱신한다
- 이 README의 표에도 한 줄 추가한다
