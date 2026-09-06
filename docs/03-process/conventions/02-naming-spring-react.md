> Owner: qa-tester | Status: current | Last-reviewed: 2026-09-06
> 상위 문서: [SPEC.md](../../00-meta/SPEC.md)

# 명명·구조 규약 (Spring Boot + React)

> `CONVENTIONS.md`(이론편)의 스택 적용판.
> 핵심 목표: **파일명만 보고 "무엇에 대한, 어떤 역할의 코드인지"를 열어보지 않고 알 수 있게 한다.**
> 리포지토리 루트에 두고 AI 코딩 도구 컨텍스트(`CLAUDE.md`)로 함께 제공한다.

---

## 1. 명명의 대전제

### 1.1 이름은 설계의 진단 도구다

> Ousterhout(*A Philosophy of Software Design*, 2018): **"이름을 짧고 정확하게 지을 수 없다면, 그 모듈의 책임이 흐릿하다는 뜻이다."**

`Requirement.java` 라는 이름밖에 떠오르지 않는다면, 그 클래스가 **여러 개념을 한꺼번에 담고 있다**는 신호다.
이름을 구체화하려 시도하는 과정이 곧 응집도를 높이는 설계 작업이다.

### 1.2 명명 공식

```
[한정어(Qualifier)] + [도메인 개념(Concept)] + [역할 접미사(Role Suffix)]
     어떤?                 무엇에 대한?              무슨 역할?
```

| 예시 | 한정어 | 도메인 개념 | 역할 접미사 |
|---|---|---|---|
| `ProjectKeySequenceAllocator` | ProjectKey | Sequence | Allocator |
| `PolymorphicTargetValidator` | PolymorphicTarget | Target | Validator |
| `RequirementApprovalPolicy` | Approval | Requirement | Policy |
| `RequirementTraceabilityMatrix` | Traceability | Requirement | Matrix |
| `ApproveRequirementService` | Approve | Requirement | Service |
| `RequirementAlreadyApprovedException` | AlreadyApproved | Requirement | Exception |

> 상세 명명은 Service / Controller / DTO 에 적용한다.
> 엔티티는 대상이 아니다. requirements.type 은 FUNCTIONAL/NON_FUNCTIONAL/BUSINESS
> 컬럼 값이므로 RequirementType enum 이 맞고, 클래스를 나누면 스키마와 어긋난다.

### 1.3 "어떤?" 테스트 (필수 통과 조건)

파일명을 소리 내어 읽고 **"어떤 ~ ?" 또는 "무엇을 하는 ~ ?" 라는 질문이 떠오르면 이름이 미완성**이다.

| 이름 | 떠오르는 질문 | 수정 |
|---|---|---|
| `RequirementResponse.java`(목록·상세 겸용) | 어떤 용도의 응답? 목록? 상세? | `RequirementSummaryResponse.java` / `RequirementDetailResponse.java` |
| `RequirementService.java` | 요구사항에 대해 **무엇을** 하는? | `ApproveRequirementService.java` |
| `Validator.java` | 무엇을 검증하는? | `RequirementDuplicationValidator.java` |
| `Status.java` | 무엇의 상태? | `RequirementReviewStatus.java` |
| `Table.tsx` | 무엇을 보여주는 표? | `RequirementTraceabilityTable.tsx` |
| `Modal.tsx` | 무슨 모달? | `RequirementApprovalConfirmModal.tsx` |
| `handleClick` | 무엇을 하는 클릭? | `handleApproveButtonClick` |
| `data`, `info`, `item` | (전부 실격) | `requirementSummary`, `approverProfile` |

### 1.4 길이 규칙

- **2~4 단어**를 표준으로 한다. 1단어는 대개 정보 부족, 5단어 이상은 책임 과다 신호.
- **5단어를 넘으면 이름을 줄이지 말고 클래스를 쪼갠다.** `RequirementApprovalNotificationEmailTemplateBuilder` → `ApprovalNotificationEmail`(값) + `ApprovalNotificationEmailBuilder`(생성).
- **패키지가 주는 컨텍스트를 클래스명에 중복하지 않는다.** 단, **도메인 접두어는 유지**한다. Java 클래스명은 import 문과 스택트레이스에서 패키지 없이 단독으로 읽히기 때문이다.
  - `requirement/domain/Policy.java` ❌ (전역적으로 무의미)
  - `requirement/domain/RequirementApprovalPolicy.java` ✅
  - `requirement/domain/RequirementDomainRequirementPolicy.java` ❌ (계층명 중복)

### 1.5 금지 단어

| 금지 | 이유 | 대안 |
|---|---|---|
| `Util`, `Common`, `Helper`, `Misc`, `Etc` | 우연적 응집(응집도 1단계)의 자백 | 목적을 이름에: `DateRangeCalculator` |
| `Manager`, `Processor`, `Handler`(단독) | 무엇을 관리/처리하는지 없음 | `RequirementLifecycleCoordinator` |
| `Data`, `Info`, `Detail`(단독) | 모든 객체가 데이터다 | `RequirementDetailResponse` |
| `~Impl` | 구현이 하나뿐이면 인터페이스가 불필요, 여럿이면 기술명을 써야 함 | `JpaRequirementRepository`, `InMemoryRequirementRepository` |
| `Base~`, `Abstract~`(무분별한) | 상속 재사용 유혹 | 합성으로 전환 |
| `~1`, `~2`, `~New`, `~Old`, `~V2`, `~Final` | Git이 버전을 관리한다 | 의미로 구분: `LegacyRequirementImporter` |
| 약어 `Req`, `Mgr`, `Svc`, `Cfg`, `Btn` | 검색 불가, 해독 비용 | 전체 단어 |

---

## 2. 도메인 용어 사전 (Ubiquitous Language)

> Evans, *DDD*(2003). 코드·DB·기획서·API에서 **같은 개념은 반드시 같은 단어**로 부른다.
> 아래 표를 프로젝트에 맞게 채우고, **표에 없는 동의어는 코드에 등장시키지 않는다.**

| 도메인 개념 | 채택 용어 | 금지 동의어 | 비고 |
|---|---|---|---|
| (예) 서비스 이용자 | `Member` | user, account, customer | 인증 주체는 `Principal`로 구분 |
| (예) 요구사항 | `Requirement` | spec, feature, story | 하위 분류는 §1.2 한정어로 |
| (예) 요구사항 변경 요청 | `ChangeRequest` | modification, revision | |
| | | | |

**한정어 사전** — 같은 개념의 하위 종류를 구분하는 형용사도 고정한다.

| 축 | 한정어 후보 |
|---|---|
| 요구사항 종류 | `Functional`, `NonFunctional`, `Constraint` |
| 상태 시점 | `Draft`, `Reviewed`, `Approved`, `Deprecated` |
| 조회 목적 | `Summary`(목록용), `Detail`(단건용), `Row`(테이블용) |

---

## 3. Spring Boot: 패키지 구조

### 3.1 기능 우선(Package by Feature) + 헥사고날

> 근거: CCP(공통 폐쇄 원칙) — 함께 변경되는 것을 함께 둔다. / Cockburn, *Ports and Adapters*(2005).
> 구조 참고: Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*.

```
com.example.reqms
├── requirement/                      ← 기능(바운디드 컨텍스트) 단위
│   ├── domain/                       ← 프레임워크 import 금지 (POJO만)
│   │   ├── Requirement.java                       (RequirementType type 필드로 종류 구분 — 서브클래스 분리 아님)
│   │   ├── RequirementId.java                     (값 객체)
│   │   ├── RequirementPriority.java               (값 객체)
│   │   ├── RequirementReviewStatus.java           (enum)
│   │   ├── RequirementApprovalPolicy.java         (도메인 서비스)
│   │   ├── RequirementTraceabilityLink.java
│   │   ├── RequirementApprovedEvent.java          (도메인 이벤트)
│   │   └── RequirementAlreadyApprovedException.java
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── RegisterRequirementUseCase.java
│   │   │   │   ├── ApproveRequirementUseCase.java
│   │   │   │   └── RegisterRequirementCommand.java
│   │   │   └── out/
│   │   │       ├── LoadRequirementPort.java
│   │   │       ├── SaveRequirementPort.java
│   │   │       └── SendApprovalNotificationPort.java
│   │   ├── RegisterRequirementService.java        ← 유스케이스 1개 = 파일 1개
│   │   ├── ApproveRequirementService.java
│   │   └── SearchRequirementService.java
│   └── adapter/
│       ├── in/web/
│       │   ├── RequirementRegistrationController.java
│       │   ├── RequirementApprovalController.java
│       │   ├── RequirementSearchController.java
│       │   └── dto/
│       │       ├── RegisterRequirementRequest.java
│       │       ├── RequirementSummaryResponse.java
│       │       └── RequirementDetailResponse.java
│       └── out/
│           ├── persistence/
│           │   ├── RequirementJpaEntity.java
│           │   ├── RequirementSpringDataRepository.java
│           │   ├── RequirementPersistenceAdapter.java   ← 포트 구현
│           │   └── RequirementPersistenceMapper.java
│           └── notification/
│               └── EmailApprovalNotificationAdapter.java
├── changerequest/                    ← 다음 기능도 같은 형태로 반복
├── common/  ✗ 만들지 않는다
└── shared/                           ← 도메인 지식 없는 것만
    ├── time/BusinessClock.java
    └── error/ApiErrorResponse.java
```

**강제 규칙**
1. `domain/` 은 `org.springframework`, `jakarta.persistence`, `com.fasterxml` 를 import 하지 않는다.
2. `RequirementJpaEntity` 와 도메인 엔티티 `Requirement` 는 **별개 클래스**다. `@Entity` 를 도메인에 붙이지 않는다.
3. 기능 패키지끼리 직접 참조하지 않는다. 필요하면 `port/out` 인터페이스나 도메인 이벤트로 통신한다.
4. 패키지 간 순환 의존 금지(ADP) — ArchUnit으로 검사(§7).

### 3.2 파일명 규칙표 (Java)

| 대상 | 패턴 | ❌ 나쁜 예 | ✅ 좋은 예 |
|---|---|---|---|
| 애그리거트 루트/엔티티 | `[한정어]+개념` | `Requirement` | `FunctionalRequirement` |
| 값 객체 | `개념+속성` | `Priority` | `RequirementPriority` |
| enum | `개념+분류축` | `Status` | `RequirementReviewStatus` |
| 도메인 서비스/정책 | `개념+관심사+Policy/Rule/Calculator` | `RequirementService` | `RequirementApprovalPolicy` |
| 도메인 이벤트 | `개념+과거분사+Event` | `RequirementEvent` | `RequirementApprovedEvent` |
| 인바운드 포트 | `동사+개념+UseCase` | `RequirementUseCase` | `ApproveRequirementUseCase` |
| 커맨드 객체 | `동사+개념+Command` | `RequirementDto` | `RegisterRequirementCommand` |
| 조회 조건 | `개념+Query` / `+SearchCondition` | `SearchDto` | `RequirementSearchCondition` |
| 유스케이스 구현 | `동사+개념+Service` | `RequirementServiceImpl` | `ApproveRequirementService` |
| 아웃바운드 포트 | `동사+대상+Port` | `RequirementRepository`(포트 위치일 때만 허용) | `LoadRequirementPort`, `SaveRequirementPort` |
| 영속성 어댑터 | `개념+PersistenceAdapter` | `RequirementRepositoryImpl` | `RequirementPersistenceAdapter` |
| JPA 엔티티 | `개념+JpaEntity` | `Requirement`(도메인과 충돌) | `RequirementJpaEntity` |
| Spring Data 리포지토리 | `개념+SpringDataRepository` | `RequirementRepository`(모호) | `RequirementSpringDataRepository` |
| 매퍼 | `개념+계층+Mapper` | `Mapper` | `RequirementPersistenceMapper` |
| 외부 연동 클라이언트 | `벤더+대상+Client` | `ApiClient` | `SlackNotificationClient` |
| 컨트롤러 | `개념+관심사+Controller` | `RequirementController`(비대해짐) | `RequirementApprovalController` |
| 요청 DTO | `동사+개념+Request` | `RequirementDto` | `RegisterRequirementRequest` |
| 응답 DTO | `개념+용도+Response` | `RequirementDto` | `RequirementSummaryResponse` |
| 예외 | `개념+위반상황+Exception` | `RequirementException` | `RequirementAlreadyApprovedException` |
| 설정 클래스 | `범위+관심사+Config` | `Config` | `RequirementSecurityConfig` |
| 프로퍼티 바인딩 | `범위+Properties` | `AppProperties` | `RequirementApprovalProperties` |
| 스케줄러 | `동사+대상+Scheduler`/`Job` | `Batch` | `ExpireDraftRequirementScheduler` |
| 테스트 | `대상+Test` / 통합 `+IntegrationTest` | `Test1` | `ApproveRequirementServiceTest` |

> **`RequirementController` 하나에 등록·수정·승인·검색을 다 넣지 않는다.**
> 컨트롤러는 **유스케이스 그룹 단위**로 쪼갠다(`RequirementRegistrationController`, `RequirementApprovalController`). SRP에서 말하는 "변경 이유"가 각각 다르기 때문이다.

### 3.3 응답 DTO는 용도별로 분리한다

> 근거: CRP(공통 재사용 원칙) — 함께 쓰이지 않는 것에 의존을 강요하지 않는다.

```java
// ❌ 만능 DTO: 목록 조회에도 30개 필드가 딸려온다
public record RequirementDto(...30 fields...) {}

// ✅ 용도별 분리
public record RequirementSummaryResponse(Long id, String title, RequirementReviewStatus status) {}
public record RequirementDetailResponse(Long id, String title, String description,
                                        List<TraceabilityLinkResponse> links, ...) {}
```

### 3.4 메서드·변수 명명

| 대상 | 규칙 | 예 |
|---|---|---|
| 조회(없으면 Optional) | `find~` | `findByRequirementId` |
| 조회(없으면 예외) | `get~` | `getApprovedRequirement` |
| 다건 | `findAll~`, `search~` | `searchByReviewStatus` |
| 생성 | `create~`, `register~` | `registerFunctionalRequirement` |
| 상태 변경 | 도메인 동사 그대로 | `approve()`, `deprecate()` (`setStatus()` ❌) |
| 불리언 | `is/has/can/should` | `isApprovable()`, `hasTraceabilityLink()` |
| 변환 | `to~`, `from~` | `toDomain()`, `fromJpaEntity()` |
| 단위 포함 | 접미사 필수 | `approvalTimeoutMillis`, `fileSizeBytes` |
| 시각/기간 | `~At` / `~Duration` | `approvedAt`, `reviewDuration` |

> **엔티티에 `setStatus(3)` 같은 세터를 두지 않는다.** 도메인 동사 메서드(`approve(Approver approver, Instant now)`)로만 상태를 바꾼다. 세터는 불변 조건 검증을 우회시켜 도메인 규칙을 무력화한다.

### 3.5 그 외 자원 명명

| 대상 | 규칙 | 예 |
|---|---|---|
| REST 경로 | 복수 명사 + kebab-case, 동사 금지 | `POST /api/v1/requirements/{id}/approval` |
| DB 테이블 | `snake_case` 복수 | `functional_requirements` |
| DB 컬럼 | `snake_case`, 불리언은 `is_` | `is_approved`, `approved_at` |
| Flyway 마이그레이션 | `V{yyyyMMdd}_{seq}__{동사}_{대상}.sql` | `V20260904_01__create_requirement_approval_table.sql` |
| 프로파일 | `application-{env}.yml` | `application-local.yml` |
| 설정 키 | kebab-case 계층 | `requirement.approval.timeout-millis` |

---

## 4. React: 디렉터리 구조

```
src/
├── app/                              ← 앱 전역 설정 (라우터, 프로바이더)
│   ├── AppRouter.tsx
│   └── QueryClientProvider.tsx
├── features/                         ← 기능 단위 (백엔드 패키지와 이름 일치)
│   └── requirement/
│       ├── api/
│       │   ├── requirementApi.ts
│       │   ├── useApproveRequirementMutation.ts
│       │   └── useRequirementDetailQuery.ts
│       ├── model/
│       │   ├── requirementReviewStatus.ts
│       │   └── requirement.types.ts
│       ├── hooks/
│       │   └── useRequirementFilter.ts
│       ├── components/
│       │   ├── RequirementSummaryCard.tsx
│       │   ├── RequirementApprovalConfirmDialog.tsx
│       │   ├── RequirementTraceabilityMatrix.tsx
│       │   └── RequirementReviewStatusBadge.tsx
│       └── pages/
│           ├── RequirementListPage.tsx
│           └── RequirementDetailPage.tsx
├── shared/                           ← 도메인 지식 없는 재사용 요소만
│   ├── ui/  (Button.tsx, DataTable.tsx)
│   ├── lib/ (formatBusinessDate.ts)
│   └── api/ (httpClient.ts)
└── types/
```

**규칙**
1. **`features/` 하위 기능 이름을 백엔드 패키지 이름과 동일하게 맞춘다** (`requirement`, `changerequest`). 유비쿼터스 언어를 프론트/백엔드가 공유한다.
2. **`features/A` 가 `features/B` 를 직접 import 하지 않는다.** 공통이 필요하면 `shared/` 로 올린다.
3. `shared/ui` 에는 도메인 단어가 등장하지 않는다. `shared/ui/RequirementButton.tsx` ❌.
4. **`index.tsx` 배럴 파일을 남발하지 않는다.** 에디터 탭과 스택트레이스가 전부 `index`가 되어 파일명의 정보가 사라진다. 기능 폴더의 공개 API용으로 `index.ts` 하나만 허용한다.

### 4.1 파일명 규칙표 (React/TS)

| 대상 | 패턴 | ❌ | ✅ |
|---|---|---|---|
| 페이지 | `개념+용도+Page.tsx` | `Detail.tsx` | `RequirementDetailPage.tsx` |
| 도메인 컴포넌트 | `개념+역할+컴포넌트유형.tsx` | `Card.tsx` | `RequirementSummaryCard.tsx` |
| 목록/표 | `개념+Table/List.tsx` | `Table.tsx` | `RequirementTraceabilityTable.tsx` |
| 모달/다이얼로그 | `개념+행동+Dialog.tsx` | `Modal.tsx` | `RequirementApprovalConfirmDialog.tsx` |
| 폼 | `동사+개념+Form.tsx` | `Form.tsx` | `RegisterRequirementForm.tsx` |
| 뱃지/칩 | `개념+속성+Badge.tsx` | `Badge.tsx` | `RequirementReviewStatusBadge.tsx` |
| 공용 UI | 도메인 없는 일반명.tsx | — | `shared/ui/DataTable.tsx` |
| 커스텀 훅 | `use+동사+개념.ts` | `useData.ts` | `useApproveRequirement.ts` |
| 조회 훅 | `use+개념+Query.ts` | `useFetch.ts` | `useRequirementDetailQuery.ts` |
| 변경 훅 | `use+동사+개념+Mutation.ts` | `usePost.ts` | `useApproveRequirementMutation.ts` |
| API 모듈 | `개념+Api.ts` (camelCase) | `api.ts` | `requirementApi.ts` |
| 타입 정의 | `개념.types.ts` | `types.ts` | `requirement.types.ts` |
| 상수 | `개념+Constants.ts` | `constants.ts` | `requirementStatusLabels.ts` |
| 스토어 | `개념+Store.ts` | `store.ts` | `requirementFilterStore.ts` |
| 테스트 | `대상.test.tsx` | | `RequirementSummaryCard.test.tsx` |

> **케이스 규칙**: 컴포넌트 파일은 `PascalCase.tsx`(컴포넌트명과 일치), 그 외 모듈은 `camelCase.ts`. 프로젝트 내에서 둘 중 하나로 통일하고 섞지 않는다.

### 4.2 컴포넌트 내부 명명

```tsx
// props 타입: 컴포넌트명 + Props
interface RequirementApprovalConfirmDialogProps {
  requirementId: RequirementId;
  isOpen: boolean;                    // 불리언은 is/has/can
  hasPendingReview: boolean;
  onApproveConfirm: (comment: string) => void;   // prop은 on~
  onCancel: () => void;
}

export function RequirementApprovalConfirmDialog({ ... }: RequirementApprovalConfirmDialogProps) {
  // 내부 핸들러는 handle~ + 대상 + 이벤트
  const handleApproveButtonClick = () => { ... };
  const handleCommentInputChange = (e) => { ... };
}
```

| 대상 | 규칙 | 예 |
|---|---|---|
| props 타입 | `컴포넌트명+Props` | `RequirementSummaryCardProps` |
| 콜백 prop | `on+대상+사건` | `onApproveConfirm` |
| 내부 핸들러 | `handle+대상+이벤트` | `handleApproveButtonClick` |
| 불리언 상태 | `is/has/can/should` | `isApprovalPending` |
| 배열 상태 | 복수형 | `selectedRequirementIds` |
| 로딩/에러 | `is~Loading`, `~Error` | `isRequirementLoading` |

---

## 5. 문법·코드 스타일

### 5.1 Java

- **불변 우선**: DTO·값 객체는 `record`, 컬렉션은 `List.copyOf()` 로 방어적 복사. 필드는 `final`.
- **생성자 주입만 사용**: `@Autowired` 필드 주입 금지. 의존이 5개를 넘으면 SRP 위반 신호.
- **Lombok 제한**: `@Getter`, `@RequiredArgsConstructor`, `@Builder` 만 허용. **`@Data`, `@Setter`, `@AllArgsConstructor` 금지**(불변 조건 파괴, `equals/hashCode` 오작동).
- **가드 절 우선**: 예외 조건을 먼저 반환하고 정상 흐름의 들여쓰기를 없앤다. 중첩 3단계 이하.
- **`Optional`은 반환 타입에만.** 필드나 파라미터에 쓰지 않는다.
- **불리언 파라미터 금지**: `approve(true)` ❌ → `approve()` / `approveWithOverride()` 로 분리(제어 결합 제거).
- **파라미터 4개 이상이면 커맨드 객체로 묶는다.**
- **예외는 도메인 언어로**: 인프라 예외(`SQLException`, `FeignException`)를 도메인/애플리케이션 계층으로 흘려보내지 않고 어댑터에서 변환한다.
- **`@Transactional` 은 `application` 계층에만.** 컨트롤러와 도메인에는 붙이지 않는다.

```java
// ✅ 예: 유스케이스 구현
@Service
@RequiredArgsConstructor
public class ApproveRequirementService implements ApproveRequirementUseCase {

    private final LoadRequirementPort loadRequirementPort;
    private final SaveRequirementPort saveRequirementPort;
    private final RequirementApprovalPolicy approvalPolicy;

    @Override
    @Transactional
    public RequirementId approve(ApproveRequirementCommand command) {
        FunctionalRequirement requirement =
                loadRequirementPort.loadById(command.requirementId());

        if (requirement.isApproved()) {
            throw new RequirementAlreadyApprovedException(command.requirementId());
        }
        approvalPolicy.verifyApprovable(requirement, command.approver());

        requirement.approve(command.approver(), command.approvedAt());
        return saveRequirementPort.save(requirement);
    }
}
```

### 5.2 TypeScript / React

- **`any` 금지**, `unknown` + 타입 가드. `strict: true`.
- **`type` vs `interface`**: props와 객체 형태는 `interface`, 유니온·유틸리티는 `type`. 프로젝트 내 일관 유지.
- **함수 컴포넌트 + 명명 함수 선언** (`export function X()`). 익명 화살표 default export 금지(스택트레이스에 이름이 안 남는다).
- **컴포넌트 1개당 파일 1개.** 200줄을 넘으면 분해 검토.
- **서버 상태와 클라이언트 상태 분리**: 서버 데이터는 React Query, UI 상태만 로컬/전역 스토어.
- **`useEffect` 로 파생 상태를 만들지 않는다.** 렌더 중 계산하거나 `useMemo`.
- **매직 문자열 금지**: 상태값은 백엔드 enum과 1:1 대응하는 상수 유니온으로.

```ts
// requirementReviewStatus.ts — 백엔드 RequirementReviewStatus.java 와 값 동기화
export const REQUIREMENT_REVIEW_STATUS = {
  DRAFT: 'DRAFT',
  UNDER_REVIEW: 'UNDER_REVIEW',
  APPROVED: 'APPROVED',
  DEPRECATED: 'DEPRECATED',
} as const;

export type RequirementReviewStatus =
  (typeof REQUIREMENT_REVIEW_STATUS)[keyof typeof REQUIREMENT_REVIEW_STATUS];
```

### 5.3 테스트 명명

```java
@Test
@DisplayName("이미 승인된 요구사항을 다시 승인하면 RequirementAlreadyApprovedException이 발생한다")
void approve_throwsException_whenRequirementIsAlreadyApproved() { ... }
```

- 메서드명: `대상_기대결과_조건` (영문), `@DisplayName` 은 한글 문장으로 명세를 서술.
- 구조는 given-when-then 3단으로 빈 줄 구분.
- 도메인/유스케이스 테스트는 `@SpringBootTest` 없이 순수 JUnit으로 돌아가야 한다. 안 된다면 의존성 역전이 덜 된 것이다.
- React: `RequirementSummaryCard.test.tsx`, 쿼리는 `getByRole` 우선(구현 세부 결합 회피).

---

## 6. 현 코드베이스 이전 절차

한 PR에 한 단계만 담는다. 이름 변경과 로직 변경을 섞지 않는다.

| 단계 | 작업 | 검증 |
|---|---|---|
| 1 | 용어 사전(§2) 확정 — 팀 합의 후 `docs/00-meta/GLOSSARY.md` 커밋 | 코드 변경 없음 |
| 2 | **클래스 rename만** — IDE 자동 리팩터링. `Requirement` → `FunctionalRequirement` 등 §1.3 테스트 통과시키기 | 컴파일 + 기존 테스트 그린 |
| 3 | **비대한 Controller/Service 분할** — `RequirementService` → `RegisterRequirementService` + `ApproveRequirementService` | 동작 변경 0 |
| 4 | **패키지 이동** — 계층 우선 → 기능 우선 (`git mv`, import만 변경) | ArchUnit 기준선 추가 |
| 5 | **도메인 분리** — `@Entity` 클래스에서 비즈니스 규칙을 POJO 도메인으로 추출, `RequirementJpaEntity` 신설 | 특성화 테스트 선행 작성 |
| 6 | **포트/어댑터 도입** — `RequirementPersistenceAdapter` 로 감싸기 | 유스케이스 단위 테스트 DB 없이 통과 |
| 7 | **React `features/` 재편** — 백엔드 기능명과 동기화, 파일명 §4.1 적용 | ESLint boundaries 규칙 추가 |
| 8 | **규칙 자동화** (§7) | CI 실패로 강제 |

> **5단계 전에 특성화 테스트를 반드시 먼저 만든다** (Feathers, *Working Effectively with Legacy Code*). 현재 동작이 옳은지가 아니라, 무엇을 하는지를 고정하는 것이 목적이다.

---

## 7. 규칙 자동화 (문서보다 CI가 강하다)

### 7.1 ArchUnit — 의존성 방향 + 명명 규칙 강제

```java
@AnalyzeClasses(packages = "com.example.reqms", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRuleTest {

    @ArchTest
    static final ArchRule 도메인은_스프링에_의존하지_않는다 =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule 패키지_순환_의존_금지 =
        slices().matching("com.example.reqms.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule 영속성_어댑터_명명 =
        classes().that().resideInAPackage("..adapter.out.persistence..")
            .and().areAnnotatedWith(Component.class)
            .should().haveSimpleNameEndingWith("PersistenceAdapter");

    @ArchTest
    static final ArchRule 유스케이스_구현체_명명 =
        classes().that().implement(
                JavaClass.Predicates.simpleNameEndingWith("UseCase"))
            .should().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule Impl_접미사_금지 =
        noClasses().should().haveSimpleNameEndingWith("Impl");

    @ArchTest
    static final ArchRule 유틸성_패키지_금지 =
        noClasses().should().resideInAnyPackage("..util..", "..common..", "..helper..");

    @ArchTest
    static final ArchRule 컨트롤러는_유스케이스만_호출한다 =
        noClasses().that().resideInAPackage("..adapter.in.web..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");
}
```

### 7.2 ESLint — 프론트 경계 + 파일명

```js
// eslint.config.js (일부)
{
  plugins: { boundaries, 'check-file': checkFile },
  settings: {
    'boundaries/elements': [
      { type: 'app',     pattern: 'src/app/*' },
      { type: 'feature', pattern: 'src/features/*', capture: ['featureName'] },
      { type: 'shared',  pattern: 'src/shared/*' },
    ],
  },
  rules: {
    'boundaries/element-types': ['error', {
      default: 'disallow',
      rules: [
        { from: 'app',     allow: ['feature', 'shared'] },
        // 기능끼리 직접 참조 금지 (같은 기능 내부만 허용)
        { from: 'feature', allow: [['feature', { featureName: '${from.featureName}' }], 'shared'] },
        { from: 'shared',  allow: ['shared'] },
      ],
    }],
    'check-file/filename-naming-convention': ['error', {
      'src/**/components/**/*.tsx': 'PASCAL_CASE',
      'src/**/pages/**/*.tsx': 'PASCAL_CASE',
      'src/**/{api,hooks,model}/**/*.ts': 'CAMEL_CASE',
    }],
    'check-file/folder-naming-convention': ['error', { 'src/features/*': 'KEBAB_CASE' }],
  },
}
```

### 7.3 그 외

- **Checkstyle / Spotless**: 포맷·import 순서 자동화. Naver Hackday Java 컨벤션 또는 Google Java Format 채택.
- **Sonar / PMD**: 순환 복잡도 10 초과, 메서드 50줄 초과, 파라미터 4개 초과를 CI 실패로.
- **Husky + lint-staged**: 커밋 전 ESLint/Prettier.
- **PR 템플릿**: `CONVENTIONS.md` §9 체크리스트를 그대로 붙여둔다.

---

## 8. 바이브코딩용 프롬프트 규격

AI는 세션 간 기억이 없어 구조가 반복적으로 되돌아간다. 요청 시 아래 4가지를 항상 명시한다.

```
[기능]     requirement
[계층]     application (port/in 인터페이스 + 구현 Service)
[제약]     domain 패키지는 Spring/JPA import 금지, 생성자 주입만, Lombok은 @RequiredArgsConstructor만
[파일명]   ApproveRequirementUseCase.java, ApproveRequirementService.java
           (한정어+개념+역할 접미사 형식 유지, ~Impl 금지)
```

**생성된 코드에서 가장 자주 깨지는 3가지 — 매번 확인할 것**
1. 도메인 클래스에 `@Entity`/`@Column` 이 붙어 있는가 (계층 혼합)
2. 클래스명이 `~ServiceImpl`, `~Util`, `~Manager` 로 나왔는가
3. Controller 하나에 모든 유스케이스가 몰려 있는가

---

## 9. 빠른 참조: 나쁜 이름 → 좋은 이름

| ❌ | ✅ | 근거 |
|---|---|---|
| `Requirement.java` | `FunctionalRequirement.java` | 한정어 누락 |
| `RequirementService.java` | `ApproveRequirementService.java` | 동사 누락, SRP |
| `RequirementServiceImpl.java` | `ApproveRequirementService.java` | Impl 안티패턴 |
| `RequirementRepository.java`(구현체) | `RequirementPersistenceAdapter.java` | 기술/역할 명시 |
| `RequirementDto.java` | `RegisterRequirementRequest.java` / `RequirementSummaryResponse.java` | 방향·용도 구분, CRP |
| `Status.java` | `RequirementReviewStatus.java` | 무엇의 상태인지 |
| `RequirementUtil.java` | `RequirementIdGenerator.java` | 우연적 응집 제거 |
| `RequirementException.java` | `RequirementAlreadyApprovedException.java` | 위반 상황 명시 |
| `Config.java` | `RequirementSecurityConfig.java` | 범위 명시 |
| `Modal.tsx` | `RequirementApprovalConfirmDialog.tsx` | 무슨 모달인지 |
| `Table.tsx` | `RequirementTraceabilityTable.tsx` | 무엇의 표인지 |
| `useData.ts` | `useRequirementDetailQuery.ts` | 대상·성격 명시 |
| `api.ts` | `requirementApi.ts` | 도메인 명시 |
| `handleClick` | `handleApproveButtonClick` | 대상 명시 |
| `list`, `data`, `item` | `approvedRequirements`, `requirementSummary` | 정보 없음 |
