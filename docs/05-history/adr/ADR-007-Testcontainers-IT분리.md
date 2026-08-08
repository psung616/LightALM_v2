> Owner: qa-tester | Status: Accepted | Date: Phase 11 진행 시점

# ADR-007: Testcontainers 통합 테스트를 surefire에서 분리(failsafe + *IT.java)

## 맥락 (Context)
Windows + Docker Desktop 환경에서 Testcontainers가 기본 named pipe(`npipe:////./pipe/docker_engine`)를 찾지 못해 실패하는 문제를 겪었다. Docker Desktop이 실제로 쓰는 pipe(`npipe:////./pipe/dockerDesktopLinuxEngine`)로 `DOCKER_HOST`를 지정해도 API 버전 협상 문제로 계속 실패했다.

## 결정 (Decision)
Testcontainers를 쓰는 통합 테스트는 파일명을 `*IT.java`로 짓고, `pom.xml`에 `maven-failsafe-plugin`을 추가해 `integration-test`/`verify` 골에 바인딩한다. `mvn test`(surefire, 기본 포함 패턴 `*Test.java`)는 `*IT.java`를 자동으로 제외하므로 Docker 환경 문제와 무관하게 항상 빠르게 통과한다.

## 결과 (Consequences)
- 장점: `mvn test`가 항상 안정적으로 통과 → CI 파이프라인(Jenkinsfile의 `mvnw.cmd test` 단계)이 Docker 환경 이슈로 막히지 않음
- 단점: 통합 테스트(`*IT.java`)는 `mvn verify`를 명시적으로 실행해야만 돌아간다 — CI에 `verify` 단계가 없으면 통합 테스트가 사실상 실행되지 않을 수 있음. **QA 담당은 로컬에서 주기적으로 `mvn verify`를 직접 돌려 통합 테스트가 계속 통과하는지 확인해야 한다**(자동 CI 게이트가 아님을 인지할 것)

## 참고
- 08-dev-phases.md Phase 11, 09-quality-testing.md §10
