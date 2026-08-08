> Owner: architect | Status: Accepted (현재 유효) | Date: 2026-08-08 (ADR-005와 같은 시점)

# ADR-006: 운영 배포는 docker-compose가 아닌 Jenkinsfile(순수 docker build/run)로 수행

## 맥락 (Context) — 문서 간 모순 발견
문서 재편 작업 중 두 문서가 서로 다른 배포 상태를 서술하고 있는 것을 발견했다.
- **02-architecture.md §2.4**: "`https://alm.ondalprincess.synology.me/`는 아직 우리 컨테이너로 연결되지 않음(미완료). DSM 리버스 프록시 설정은 더 이상 손대지 않기로 했다."
- **10-deployment.md 부록 E**: Jenkinsfile이 `docker build`/`docker run`으로 `lightalm-backend`/`lightalm-frontend` 이미지를 빌드해 호스트 포트 8888로 기동하고, 기존 리버스 프록시가 이미 이 포트를 `alm.ondalprincess.synology.me`로 연결해둔 상태라 실제로 서비스되고 있다고 서술한다.

두 서술은 시점이 다른 것으로 판단된다. 02-architecture.md §2.4는 "DSM 리버스 프록시를 우리가 직접 설정해야 한다"고 전제하던 **이전 시점**의 기록이고, 10-deployment.md 부록 E는 그 이후 **"기존 리버스 프록시가 이미 8888 포트를 보고 있다"는 사실이 확인된 이후**의 최신 기록으로 보인다(부록 D의 2026-08-08 Gitea 연동과 같은 배포 파이프라인 정비 작업의 일부).

## 결정 (Decision)
10-deployment.md 부록 E를 **현재 유효한 배포 방식**으로 확정한다.
- 운영 배포는 `docker-compose.yml`(로컬 개발용)을 쓰지 않고, 레포 루트의 `Jenkinsfile`이 순수 `docker build`/`docker run`으로 수행한다(Jenkins 에이전트에 docker compose가 설치되어 있지 않기 때문)
- 흐름: `git push synology main` → Gitea webhook → Jenkins `ALM_Pipeline` 자동 실행 → 1~3분 내 `https://alm.ondalprincess.synology.me/`에 반영
- DSM 리버스 프록시는 이미 8888 포트를 해당 도메인에 연결해둔 상태이므로 **더 이상 건드릴 필요가 없다** — 02-architecture.md §2.4의 "DSM 설정을 더 이상 손대지 않기로 했다"는 결정은 결과적으로 옳았다(다른 이유로 해결됐을 뿐)

## 결과 (Consequences)
- 02-architecture.md §2.4의 "미완료" 경고 문단은 **더 이상 현재 상태를 반영하지 않는다.** 해당 문단에 이 ADR을 가리키는 상호참조를 추가해두었다
- 앞으로 배포 상태에 대한 질문은 이 ADR + CURRENT-STATE.md + 10-deployment.md 부록 E를 우선 참고할 것
- 기존 마이그레이션 파일(V1~V7)은 절대 수정하지 않는다는 규칙이 이 배포 방식과 결합되어 있음(체크섬 검증이 `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`로 우회 중이기 때문) — ADR-002 참고

## 참고
- 10-deployment.md 부록 E, 02-architecture.md §2.4
