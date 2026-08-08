# Light ALM — Claude Code 프로젝트 규칙

이 저장소는 `docs/00-meta/SPEC.md`에 정의된 명세를 기준으로 개발된다. 작업을 시작하기 전에 아래 순서로 문서를 확인한다.

## 항상 먼저 확인할 것
1. `docs/00-meta/CURRENT-STATE.md` — 지금 이 순간 유효한 사실(배포 상태, DB, Git remote 등). **개별 섹션 문서 본문과 상충하면 이 문서가 우선한다.**
2. `docs/00-meta/SPEC.md` — 전체 문서 인덱스
3. 작업 성격에 맞는 섹션 문서 (`docs/01-requirements/`, `docs/02-design/`, `docs/03-process/`, `docs/04-operations/`)

## 역할 기반 작업 방식

이 프로젝트는 여러 역할이 나눠 작업하는 것처럼 구성되어 있다. 상세는 `docs/00-meta/ROLES.md` 참고.

- 구조/스키마/API 설계 변경이 필요한 작업 → `architect` 서브에이전트에게 위임
- Phase 구현, 코드 작성 작업 → `developer` 서브에이전트에게 위임
- 테스트 작성/실행, 인증·보안 검증 작업 → `qa-tester` 서브에이전트에게 위임
- 위 셋에 해당하지 않는 요구사항/배포 관련 판단은 메인 세션(오케스트레이터)이 직접 처리한다

서브에이전트를 명시적으로 호출하지 않아도, 작업 성격이 위 역할 중 하나에 뚜렷이 해당하면 해당 서브에이전트를 사용하는 것을 우선 고려한다.

## 반드시 지킬 원칙

- **기존 Flyway 마이그레이션 파일(V1~V7)은 절대 수정하지 않는다.** 스키마 변경은 항상 새 `V{n+1}__설명.sql`을 추가한다(가능하면 `ADD COLUMN IF NOT EXISTS` 등 idempotent하게). 근거: `docs/05-history/adr/ADR-002-사내-공용-Postgres-사용.md`, `docs/04-operations/10-deployment.md` 부록 E
- **구조적 결정(스키마 변경, 아키텍처 변경, 외부 연동 방식 변경 등)을 내릴 때는 먼저 `docs/05-history/adr/`에 ADR을 작성한 뒤 진행한다.** 템플릿: `docs/05-history/adr/README.md`
- **Git push 정책**: 운영 서버(`https://alm.ondalprincess.synology.me/`)에 반영하려면 `origin`뿐 아니라 `synology` remote에도 push해야 한다. `synology` push는 즉시 운영 배포로 이어지므로 명시적 배포 의도가 있을 때만 수행한다. 최신 정책은 `docs/00-meta/CURRENT-STATE.md` §4를 확인할 것 — `docs/03-process/08-dev-phases.md`와 `docs/02-design/02-architecture.md` §2.5의 "origin에만 push" 서술은 낡은 정책이다
- **비스코프 항목을 임의로 구현하지 않는다.** `docs/01-requirements/01-scope.md` §1.3 참고
- Java 21, Maven Wrapper(`mvnw`/`mvnw.cmd`) 사용 — 시스템 Maven 설치하지 않음
- Testcontainers를 쓰는 통합 테스트는 `*IT.java`로 짓고 `mvn test`가 아닌 `mvn verify`에서만 실행되게 한다(`docs/05-history/adr/ADR-007-Testcontainers-IT분리.md`)

## 문서가 상충할 때

문서 본문에는 시간이 지나며 낡은 서술이 남아있을 수 있다("당시엔 미완료였다"는 식). 상충을 발견하면:
1. `docs/00-meta/CURRENT-STATE.md`를 우선 신뢰한다
2. 해당 섹션 문서에 상호참조/ADR 링크가 있는지 확인한다
3. 못 찾겠으면 임의로 판단하지 말고 사용자에게 확인을 요청한다 — 특히 배포/DB/보안 관련 정보는 추측하지 않는다
