# Light ALM — AI 코딩 세션 공통 지침

작업 전에 아래 문서를 먼저 읽어줘:

- `docs/00-meta/CURRENT-STATE.md` — 지금 이 순간 유효한 사실 (개별 문서와 상충하면 이 문서가 우선)
- `docs/00-meta/GLOSSARY.md` — 용어 사전. 표에 없는 동의어 사용 금지
- `docs/03-process/conventions/02-naming-spring-react.md` — 파일 명명 규칙
- `docs/03-process/11-structure-migration-plan.md` §2 — 목표 패키지 구조
- `docs/00-meta/ROLES.md` — 역할 분담. 성격에 맞는 서브에이전트에 위임할 것

---

## 신규 코드 규칙

1. 새 파일은 기능별 패키지(`com.lightalm.{기능}/api|domain|repository|service`)에 만든다. 기존 계층형(`service/`, `dto/`, `web/`)에 추가하지 않는다.
2. 파일명은 **"한정어 + 개념 + 역할 접미사"** 형식. 파일명을 읽고 "어떤 ~?" "무엇을 하는 ~?" 라는 질문이 남으면 안 된다.
3. 만능 서비스 금지. 책임별로 분리한다 (Command / Query / Policy).
4. 신규 엔티티에 `@Setter` 금지. 상태 변경은 도메인 동사 메서드로만 (`approve()`, `close()`).
5. Response DTO는 Summary(목록) / Detail(단건)로 분리한다. 하나를 겸용하지 않는다.
6. `Util` / `Common` / `Helper` / `Manager` / `Impl` 접미사 금지.
7. Lombok은 `@Getter`, `@RequiredArgsConstructor`, `@Builder` 만. `@Data`, `@Setter`, `@AllArgsConstructor` 금지.
8. 생성자 주입만 사용. `@Autowired` 필드 주입 금지.
9. `@Transactional` 은 service 계층에만.
10. 동작을 분기시키는 boolean 파라미터 금지 — 메서드를 나눈다.

## 다형 연관

`(target_type, target_id)` 대상 검증은 **`PolymorphicTargetValidator` 를 재사용한다.** 새로 만들지 않는다 (ADR-010).
새로운 `TargetType` 값을 추가할 때는 이 클래스의 switch 분기와 `GLOSSARY.md §3` 을 함께 갱신한다.

## Flyway

- 기존 마이그레이션 파일은 **절대 수정하지 않는다** (ADR-002, ADR-006). 운영 DB에 이미 적용된 이력이 있다.
- 새 마이그레이션은 기존 순번을 이어간다. 날짜 접두 형식으로 바꾸지 않는다.
- 패턴: `V{n}__{동사}_{대상}.sql` (소문자 snake_case, 동사로 시작)

## Git / 배포

- `origin`(GitHub) = 소스 백업. push해도 자동화 없음.
- `synology`(사내 Gitea) = **운영 배포 트리거.** push하면 Jenkins가 자동으로 운영에 반영한다.
- **`synology` 에는 내가 명시적으로 요청할 때만 push한다.** (ADR-005 리스크)
- 커밋은 한 종류의 변경만 담는다. rename과 로직 변경을 섞지 않는다.

## 프로세스

- 구조를 바꾸는 결정은 코드보다 **ADR을 먼저 쓴다.** `docs/05-history/adr/`
- 스코프에 없는 기능을 임의로 추가하지 않는다 (`01-scope.md` §1.3 비스코프).
- 테스트가 실패하면 `@Disabled` 나 스킵으로 우회하지 않는다 (`09-quality-testing.md` 원칙).
- DoD 통과 판단은 qa-tester가 한다. developer가 건너뛰지 않는다.
- **구현이 끝나면 같은 작업 안에서 문서를 동기화한다.** 소스 코드 없이 `docs/`만으로 프로젝트를 재구성할 수 있어야 한다는 게 기준이다. developer는 Phase/기능 구현을 마치면 해당 ADR의 Status를 "구현 완료"로 갱신하고, 설계와 다르게 구현한 부분(실제 마이그레이션 번호, 변경된 필드/엔드포인트명 등)을 architect에게 알려 `03-data-model.md`/`04-api.md`/`05-frontend.md`에 반영되게 한다. "나중에 몰아서 문서화"는 금지 — 문서 갱신을 다음 작업으로 미루지 않는다.

---

## 기존 코드에 대한 예외

**Phase 0~15 코드는 위 4번(`@Setter`)과 5번(DTO 분리)을 따르지 않는다.**
엔티티 16개 전부 `@Setter` 를 쓰고 있고, `XxxResponse` 하나가 목록·상세·생성응답에 겸용되고 있다.
이는 **의도적으로 미뤄둔 것**이다. 지시 없이 고치지 마.

새 구조는 Phase 16 이후 신규 기능에서 먼저 검증한 뒤, 별도 계획에 따라 기존 코드로 확대한다
(`docs/03-process/11-structure-migration-plan.md` §5).
