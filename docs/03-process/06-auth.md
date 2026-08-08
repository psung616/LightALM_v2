> Owner: architect · 검증은 qa-tester | Status: current | Last-reviewed: 2026-08-08
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

## 6. 인증/인가 상세 설계

- Spring Security `SecurityFilterChain` 구성: 세션 기반 인증, `formLogin()` 커스터마이즈(로그인 성공/실패 시 JSON 응답을 위해 `AuthenticationSuccessHandler`/`AuthenticationFailureHandler` 직접 구현 — 리다이렉트 대신 200/401 JSON 반환)
- `UserDetailsService` 구현체는 `users` 테이블 조회, 비밀번호는 `BCryptPasswordEncoder` 사용
- CORS 설정(실제 구현): `application.yml`의 `light-alm.cors.allowed-origins` 프로퍼티(환경변수 `CORS_ALLOWED_ORIGINS`로 오버라이드, 기본값 `http://localhost:5173`, 02-architecture.md §2.4·10-deployment.md 부록 A 참고)를 `@ConfigurationProperties` 또는 `@Value`로 바인딩한 뒤 쉼표(`,`) 기준으로 split하여 `CorsConfigurationSource`의 허용 origin 목록에 그대로 반영한다. `allowCredentials(true)`도 함께 설정한다. 사내 테스트 서버 배포 시에는 이 환경변수 하나에 로컬 origin과 `https://alm.ondalprincess.synology.me`를 콤마로 함께 넣어 두 origin을 동시에 허용한다(10-deployment.md 부록 B의 `backend.environment.CORS_ALLOWED_ORIGINS` 참고).
- 프로젝트 단위 권한 검사는 `@PreAuthorize` 대신 서비스 레이어에서 `ProjectMemberService.requireRole(projectId, userId, minRole)` 형태의 명시적 검사 메서드로 구현(멀티 프로젝트 + 프로젝트별 역할이라는 동적 구조이므로 어노테이션보다 명시적 코드가 유지보수에 유리)
- 시스템 `ADMIN`은 모든 프로젝트에 대해 항상 `PROJECT_ADMIN` 이상 권한을 가진 것으로 취급
