> Owner: architect | Status: Accepted | Date: 2026-08-08 | **[2026-08-08 정정: Decision 범위를 "운영 배포만"으로 수정 — 아래 참고]**

# ADR-002: 운영 배포는 로컬 postgres 컨테이너 대신 사내 공용 NAS Postgres 사용

## 맥락 (Context)
`docker-compose.yml`에는 `postgres:15` 서비스(포트 5432, 계정 `lightalm/lightalm`, DB `lightalm`)가 정의되어 있다. 이와 별개로 사내 Synology NAS에 이미 운영 중인 공용 PostgreSQL(`ondalprincess.synology.me:55432`)이 존재한다.

## 결정 (Decision)
**운영 배포(Jenkinsfile)** 는 `backend` 컨테이너 실행 시 로컬 `postgres` 컨테이너 대신 외부 DB(`ondalprincess.synology.me:55432/ALM_Project`, 계정 `postgres/postgres`)를 환경변수로 직접 주입한다(10-deployment.md 부록 E).
**로컬 개발(`docker-compose.yml`)은 이 결정의 대상이 아니다** — `backend` 서비스는 지금도 같은 compose의 로컬 `postgres` 컨테이너(`lightalm/lightalm`)를 그대로 사용한다(02-architecture.md §2.4 정정, 10-deployment.md 부록 B). 로컬에서 외부 DB에 붙여보고 싶을 때는 그때그때 환경변수를 바꿔서 실행할 수 있을 뿐, 기본값이 바뀐 것은 아니다.

> 이 ADR은 최초 작성 시 "`docker-compose.yml`의 `backend`가 이미 외부 DB로 전환되어 있다"고 잘못 기록했었다. 실제 파일을 확인해 위와 같이 정정했다(로컬/운영 구분).

## 근거
- KPI 집계 등 사내의 다른 도구가 이미 같은 공용 DB(`ALM_Project`)를 보고 있어서, 운영 배포에서는 별도 DB를 새로 만들지 않고 의도적으로 공용 DB를 그대로 사용하기로 함(10-deployment.md 부록 E 참고)
- 로컬 개발까지 공용 DB를 공유하면 개발 중 실수로 운영/KPI 데이터를 오염시킬 위험이 있어, 로컬은 격리된 자체 DB를 유지하기로 함

## 결과 (Consequences)
- 장점: 로컬 개발은 격리되어 안전하고, 운영 배포는 별도 DB 프로비저닝 없이 사내 공용 DB를 즉시 재사용
- 단점: **공용 DB이므로 스키마 변경(마이그레이션) 시 다른 도구에 영향을 줄 수 있음** — 기존 V1~V7 마이그레이션 파일을 수정하지 않는다는 규칙(10-deployment.md 부록 E)이 이 리스크에서 비롯됨
- 단점: 계정(`postgres/postgres`)이 Jenkinsfile에 평문으로 들어있음 — 운영 전환 시 Jenkins Credentials로 이전 필요(10-deployment.md 부록 E)

## 참고
- 02-architecture.md §2.4, 10-deployment.md 부록 B·E
