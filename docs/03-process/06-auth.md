> Owner: architect · 검증은 qa-tester | Status: current | Last-reviewed: 2026-10-06 (ADR-016 반영)
> 상위 문서: [SPEC.md](../00-meta/SPEC.md)

## 6. 인증/인가 상세 설계

- Spring Security `SecurityFilterChain` 구성: 세션 기반 인증, `formLogin()` 커스터마이즈(로그인 성공/실패 시 JSON 응답을 위해 `AuthenticationSuccessHandler`/`AuthenticationFailureHandler` 직접 구현 — 리다이렉트 대신 200/401 JSON 반환)
- `UserDetailsService` 구현체는 `users` 테이블 조회, 비밀번호는 `BCryptPasswordEncoder` 사용
- CORS 설정(실제 구현): `application.yml`의 `light-alm.cors.allowed-origins` 프로퍼티(환경변수 `CORS_ALLOWED_ORIGINS`로 오버라이드, 기본값 `http://localhost:5173`, 02-architecture.md §2.4·10-deployment.md 부록 A 참고)를 `@ConfigurationProperties` 또는 `@Value`로 바인딩한 뒤 쉼표(`,`) 기준으로 split하여 `CorsConfigurationSource`의 허용 origin 목록에 그대로 반영한다. `allowCredentials(true)`도 함께 설정한다. 사내 테스트 서버 배포 시에는 이 환경변수 하나에 로컬 origin과 `https://alm.ondalprincess.synology.me`를 콤마로 함께 넣어 두 origin을 동시에 허용한다(10-deployment.md 부록 B의 `backend.environment.CORS_ALLOWED_ORIGINS` 참고).
- 프로젝트 단위 권한 검사는 `@PreAuthorize` 대신 서비스 레이어에서 `ProjectMemberService.requireRole(projectId, userId, minRole)` 형태의 명시적 검사 메서드로 구현(멀티 프로젝트 + 프로젝트별 역할이라는 동적 구조이므로 어노테이션보다 명시적 코드가 유지보수에 유리)
- 시스템 `ADMIN`은 모든 프로젝트에 대해 항상 `PROJECT_ADMIN` 이상 권한을 가진 것으로 취급

### 6.1 역할 체계와 화면 표시명 (ADR-015, 2026-10-05)

역할 **값**(DB/코드/API, `[MEMBER+]` 같은 권한 표기)은 그대로 두고 화면에는 표시명만 쓴다. 매핑은 `frontend/src/auth/roleDisplayNames.ts` 한 곳(GLOSSARY.md §3).

| 구분 | 값 | 화면 표시명 | 권한 요약 |
|---|---|---|---|
| 시스템 역할 | `ADMIN` | System Admin | 계정 생성/수정/비활성화, System Admin 부여·해제, 시스템 테마, **프로젝트 생성**. 모든 프로젝트에서 `PROJECT_ADMIN` 이상으로 취급(`requireRole`이 `principal.isAdmin()`이면 통과) |
| 시스템 역할 | `USER` | User | 일반 사용자. 프로젝트 권한은 각 프로젝트의 멤버 역할로 결정 |
| 프로젝트 역할 | `PROJECT_ADMIN` | Project Admin | 프로젝트 수정/삭제, GitHub/Jenkins 연동, 멤버 추가/역할 변경/제거, Configuration 4종, 요구사항/이슈/테스트케이스 **삭제**, 릴리스 생성·수정·상태 변경, 베이스라인 생성, 승인 목록·결정, 프로젝트 전체 감사 로그 |
| 프로젝트 역할 | `MEMBER` | Project Assignable | 읽기 + 쓰기: 요구사항/이슈/테스트케이스 생성·수정·상태 변경, 추적성 링크, 댓글, 테스트 실행, Git 링크, 빌드 트리거, 리뷰 사이클 생성/닫기, 승인 요청, 릴리스 항목 추가/제거, 커스텀 필드 값 저장. 설정 변경 불가(ADR-015 P2 — 현행 유지) |
| 프로젝트 역할 | `VIEWER` | Project User | 읽기 전용. **유일한 쓰기 예외**: `PATCH /api/projects/{projectId}/review-cycles/{cycleId}/participants/me` — 본인이 참여자로 지정된 리뷰 사이클에서 본인 결정/코멘트만 기록(ADR-015 P1) |

- 시스템 역할 `USER`("User")와 프로젝트 역할 `VIEWER`("Project User")는 다른 개념이다.
- 워크플로우 규칙의 `allowed_role='VIEWER'`는 `changeStatus`가 규칙을 보기 전에 `MEMBER+`를 먼저 요구하므로 실제로는 `MEMBER`와 같다. 화면에서는 새로 고를 수 없다(ADR-015 P3).

### 6.2 계정 생성·프로젝트 생성은 System Admin만 (ADR-015 D2·D6)

- **계정 생성**: `POST /api/users`(`@PreAuthorize("hasRole('ADMIN')")`)가 유일한 경로다. 공개 회원가입(`/api/auth/signup`)은 제거됐고 `SecurityConfig`의 `permitAll()`은 `/api/auth/login`, `/api/public/theme`, `/api/webhooks/**`만 남았다.
- **라이센스 게이트 없음**: 로그인 성공 처리(`JsonAuthenticationSuccessHandler`)는 항상 200 + `UserResponse`를 반환하고, 계정 생성에도 시트/만료 검사가 없다(ADR-011 이전 동작으로 복귀).
- **프로젝트 생성**: `ProjectController.create()`에 `@PreAuthorize("hasRole('ADMIN')")` — ADMIN이 아니면 `403 FORBIDDEN`(`GlobalExceptionHandler.handleAccessDenied`). 생성자(System Admin)는 `ProjectService.create()` → `ProjectMemberService.registerProjectAdmin()`으로 `PROJECT_ADMIN` 멤버 자동 등록(현행 유지). 실제 담당 Project Admin은 생성 후 멤버 API/탭으로 지정한다. 시스템 역할 검사는 컨트롤러 `@PreAuthorize`, 프로젝트 역할 검사는 서비스 `requireRole`이라는 구분을 그대로 따른다.

### 6.3 마지막 활성 System Admin 보호 (ADR-015 D3)

- 불변식: `enabled=true`이고 `system_role='ADMIN'`인 사용자가 항상 1명 이상.
- `UserService.update()`(요청이 ADMIN 해제 또는 `enabled=false`일 때)와 `UserService.deactivate()`(항상)가 대상 엔티티를 읽기 **전에** `SystemAdminRetentionPolicy.requireAnotherActiveAdmin(targetId)`를 호출한다. Policy는 `UserRepository.findEnabledAdminsForUpdate()`(`@Lock(PESSIMISTIC_WRITE)`, `... order by u.id` → `SELECT ... FOR UPDATE`)로 활성 ADMIN 행을 잠그고, 대상이 그 결과에 있고 결과 크기가 1 이하면 `LastActiveAdminRemovalException` → `400 LAST_ACTIVE_ADMIN`.
- Policy는 `@Transactional(propagation = MANDATORY)` — 락이 이어지는 변경과 같은 트랜잭션에서 유지되어야 하므로 호출자 트랜잭션이 없으면 실패한다.
- 동시성: READ COMMITTED에서 뒤에 온 트랜잭션은 락 대기 후 최신 행 버전으로 WHERE를 재평가한다. 서로를 동시에 강등하면 한쪽만 성공한다. `ORDER BY id`로 락 순서를 고정해 데드락을 막는다.
- 아이디 `admin`(V2 시드)은 특별 취급하지 않는다. 본인 요청에도 같은 규칙.
- lost update 방지(ADR-016 D3): `User` 엔티티에 Hibernate `@DynamicUpdate` — 값이 바뀐 컬럼만 UPDATE한다(예: email만 수정하면 `update users set email=?,updated_at=? where id=?`). 락 없이 실행되는 email/fullName 수정이 동시에 커밋된 `system_role`/`enabled` 변경을 옛 값으로 되돌리지 않는다. 활성 ADMIN 집합에서 빠지게 하는 쓰기는 여전히 위 락 경로만 탄다.
- 한계: `psql` 직접 UPDATE는 막지 못한다(복구 SQL은 10-deployment.md 부록 F). ~~로그인 세션의 `UserPrincipal`은 세션이 끝날 때까지 이전 권한을 유지한다~~ — **ADR-016으로 해결**(§6.4).

### 6.4 세션 사용자 상태 요청마다 재검증 (ADR-016, 2026-10-06)

- 로그인 시 `UserPrincipal`(id/username/`systemRole`/`enabled` 등)은 `HttpSessionSecurityContextRepository`로 세션의 `SecurityContext`에 저장된다. 이 값만 믿으면 ADMIN 해제·비활성화가 기존 세션에 반영되지 않으므로, `com.lightalm.security.SessionPrincipalRefreshFilter`(`OncePerRequestFilter`)를 `SecurityFilterChain`의 `AuthorizationFilter` **바로 앞**에 둔다(`SecurityConfig`: `addFilterBefore(new SessionPrincipalRefreshFilter(userRepository, authenticationEntryPoint, new HttpSessionSecurityContextRepository()), AuthorizationFilter.class)`). 스프링 빈으로 등록하지 않는다(서블릿 필터 자동 이중 등록 방지).
- 인증 principal이 `UserPrincipal`인 요청마다 `UserRepository.findById(id)` 1회:
  - 행 없음 또는 `enabled=false` → `HttpSession.invalidate()` + `SecurityContextHolder.clearContext()` + `JsonAuthenticationEntryPoint`로 **`401 UNAUTHORIZED`**(체인 중단). 이후 같은 쿠키로 오는 요청도 401(세션 없음). 재로그인은 기존대로 `DaoAuthenticationProvider`가 비활성 계정을 거부(`401 AUTHENTICATION_FAILED`)
  - `system_role`이 세션 값과 다름 → DB 값으로 새 `UserPrincipal` + authorities(`ROLE_{systemRole}`)의 `UsernamePasswordAuthenticationToken.authenticated(...)`(기존 details 유지)로 `SecurityContext` 교체 후 `saveContext()`로 세션에도 저장. 이후 `@PreAuthorize("hasRole('ADMIN')")`, 서비스의 `principal.isAdmin()`(`requireRole`), `@AuthenticationPrincipal`(`GET /api/auth/me`)이 모두 갱신된 값을 본다. 승격도 같은 방식으로 즉시 반영
  - 같음 → 그대로 통과(세션 쓰기 없음)
- 익명 요청과 `UserPrincipal`이 아닌 인증은 DB 조회 없이 통과. 단 `permitAll` 경로(예: `/api/public/theme`)라도 요청에 인증된 세션 쿠키가 실려 있으면 재검증 대상이다 — 비활성화된 사용자의 오래된 세션으로 호출하면 첫 요청은 401(세션 무효화), 이후 익명으로 200(qa-tester 2026-10-06 확인, 프론트 axios 인터셉터가 401 시 `/login`으로 보내므로 의도한 "비활성 사용자 로그아웃"과 같은 효과). webhook 호출은 쿠키를 보내지 않아 영향 없음. email/fullName 변경은 재발급 조건이 아니다.
- 프로젝트 역할은 원래 `ProjectMemberService.findRole()`이 매 요청 `project_members`를 조회하므로 이 필터와 무관하다.
- 비용: 인증된 요청마다 `users` PK SELECT 1회. 이미 실행 중인 요청에는 반영되지 않고 다음 요청부터 반영된다.
