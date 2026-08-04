> 상위 문서: [SPEC.md](SPEC.md)

## 9. 코딩 컨벤션 및 품질 기준
- 패키지 구조는 02-architecture.md §2.2를 따른다 (계층형: domain/repository/service/web/dto)
- 컨트롤러는 얇게 유지하고, 비즈니스 로직/권한 검사는 서비스 레이어에 위치시킨다
- 모든 API는 요청 DTO에 Bean Validation 어노테이션(`@NotBlank`, `@Size` 등) 적용
- 예외는 커스텀 예외 클래스(`ResourceNotFoundException`, `ForbiddenException`, `ValidationException` 등) + `@RestControllerAdvice` 전역 핸들러로 일관된 에러 응답 반환
- 프론트엔드는 화면별 폴더 구조(`pages/RequirementList/`) + API 함수는 `api/` 디렉터리에 도메인별로 분리(`api/requirement.ts`)
- TypeScript `strict: true` 유지, `any` 타입 사용 최소화
- 커밋 메시지는 이슈/요구사항 키를 포함하는 것을 권장(예: `LALM-101 feat: 이슈 상태 변경 API 구현`) — 이는 GitHub 자동 연동과도 연결되는 규칙이다

## 10. 테스트 전략
- 백엔드: 서비스 레이어 단위 테스트(JUnit5 + Mockito), 컨트롤러 통합 테스트(`@SpringBootTest`, Testcontainers PostgreSQL)
- 최소 커버리지 대상: 요구사항/이슈 CRUD, 권한 검사 로직, 채번 로직(동시성 케이스 포함), GitHub 커밋 키 파싱 정규식, Webhook 서명 검증 로직
- 프론트엔드: 핵심 컴포넌트에 대한 간단한 렌더링 테스트(Vitest + React Testing Library) — Light 버전에서는 필수는 아니나 권장
- **Testcontainers를 쓰는 통합 테스트는 파일명을 `*IT.java`로 짓고, `pom.xml`에 `maven-failsafe-plugin`을 추가해 `integration-test`/`verify` 골에 바인딩한다.** 이렇게 분리해야 `mvn test`(surefire, 기본 포함 패턴이 `*Test.java`라 `*IT.java`는 자동으로 제외됨)가 Docker 환경 문제와 무관하게 항상 빠르게 통과한다. 실제로 Windows + Docker Desktop 환경에서 Testcontainers 기본 전략이 named pipe를 못 찾는 문제를 겪었다(08-dev-phases.md Phase 11 참고).
