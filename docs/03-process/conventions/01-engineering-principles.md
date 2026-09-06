> Owner: qa-tester | Status: current | Last-reviewed: 2026-09-06
> 상위 문서: [SPEC.md](../../00-meta/SPEC.md)

# 코딩 규약 및 구조 설계 지침

> 이 문서는 소프트웨어공학의 고전 이론을 근거로 한 프로젝트 규약이다.
> 각 규칙에는 "왜 그런가"에 해당하는 이론적 근거를 함께 적었다.
> 리포지토리 루트에 두고, AI 코딩 도구(`CLAUDE.md` / `AGENTS.md` / `.cursorrules`)의 컨텍스트로 함께 제공하면 생성되는 코드의 일관성이 크게 올라간다.

---

## 0. 이 문서의 사용법

| 상황 | 참조할 절 |
|---|---|
| 새 모듈을 만들 때 | 1, 2, 4 |
| 이름이 고민될 때 | 5, 6 |
| 함수를 쪼갤지 말지 판단할 때 | 3, 7 |
| 이미 만든 코드를 구조 변경할 때 | 8 |
| PR/커밋 직전 | 9 |

---

## 1. 근본 원리: 모듈화 (Modularity)

모든 규칙의 뿌리는 하나다. **변경의 파급 범위를 국소화한다.**

### 1.1 정보 은닉 (Information Hiding) — Parnas, 1972

> "On the Criteria To Be Used in Decomposing Systems into Modules"

모듈은 **처리 순서(flowchart)** 가 아니라 **변경될 가능성이 있는 설계 결정(design decision)** 을 기준으로 나눈다.

- ❌ 나쁜 분해: `1단계_입력받기`, `2단계_계산하기`, `3단계_출력하기` (실행 순서 기준)
- ✅ 좋은 분해: `결제수단`, `할인정책`, `영수증포맷` (변경 이유 기준)

**실천 규칙**
- 모듈의 공개 인터페이스는 "무엇을 하는가"만 노출하고, "어떻게 하는가"는 전부 숨긴다.
- 자료구조를 그대로 노출하지 않는다. 리스트를 그대로 리턴하는 대신, 그 리스트에 대한 질문에 답하는 메서드를 제공한다.
- 변경될 것 같은 것(DB 종류, 외부 API, 포맷, 정책)은 반드시 모듈 경계 뒤에 둔다.

### 1.2 응집도 (Cohesion) — Stevens, Myers & Constantine, 1974

모듈 내부 요소들이 얼마나 한 가지 목적을 향하는가. **아래로 갈수록 좋다.**

| 수준 | 이름 | 설명 | 판정 |
|---|---|---|---|
| 1 | 우연적 (Coincidental) | 아무 관계 없는 것들이 모임 (`utils.py`, `common.js`, `helpers/`) | 🚫 금지 |
| 2 | 논리적 (Logical) | 종류가 비슷해서 모임 (모든 입력 처리기를 한 파일에) | 🚫 지양 |
| 3 | 시간적 (Temporal) | 같은 시점에 실행돼서 모임 (`init()`, `shutdown()`) | ⚠️ 제한적 허용 |
| 4 | 절차적 (Procedural) | 정해진 순서로 실행됨 | ⚠️ |
| 5 | 통신적 (Communicational) | 같은 데이터를 다룸 | ✅ |
| 6 | 순차적 (Sequential) | 앞의 출력이 뒤의 입력 | ✅ |
| 7 | **기능적 (Functional)** | 단 하나의 잘 정의된 일만 수행 | ⭐ 목표 |

> **`utils`, `common`, `misc`, `helper` 라는 이름의 모듈은 이 프로젝트에서 금지한다.**
> 이름이 그것밖에 떠오르지 않는다는 것은 응집도가 1단계(우연적)라는 증거다.
> 대신 `date_formatter`, `currency_converter`, `retry_policy` 처럼 목적을 이름에 넣는다.

### 1.3 결합도 (Coupling) — 동일 논문

모듈 간 의존의 강도. **아래로 갈수록 좋다.**

| 수준 | 이름 | 설명 | 판정 |
|---|---|---|---|
| 1 | 내용 (Content) | 다른 모듈 내부를 직접 건드림 (private 접근, 몽키패칭) | 🚫 금지 |
| 2 | 공통 (Common) | 전역 변수를 공유 | 🚫 금지 |
| 3 | 외부 (External) | 외부 포맷/장치에 함께 묶임 | ⚠️ |
| 4 | 제어 (Control) | 플래그를 넘겨 상대의 동작 흐름을 지시 (`doWork(true)`) | ⚠️ 지양 |
| 5 | 스탬프 (Stamp) | 필요 없는 필드까지 든 구조체를 통째로 전달 | ✅ |
| 6 | **자료 (Data)** | 꼭 필요한 값만 파라미터로 전달 | ⭐ 목표 |

> **불리언 파라미터 금지 원칙**: `render(isPreview)` 처럼 동작을 분기시키는 플래그 인자는 제어 결합이다.
> `renderPreview()` / `renderFinal()` 두 함수로 분리한다.

### 1.4 관심사의 분리 (Separation of Concerns) — Dijkstra, 1974

한 시점에 한 관점만 본다. 비즈니스 규칙 / 입출력 / 저장 / 표현은 절대 한 함수 안에서 섞이지 않는다.

---

## 2. 객체·모듈 설계 원칙: SOLID (Robert C. Martin)

| 약자 | 원칙 | 이 프로젝트에서의 판단 기준 |
|---|---|---|
| **S** | 단일 책임 (Single Responsibility) | "이 파일이 변경되는 이유"를 적어봤을 때 **하나의 이해관계자(actor)** 만 나와야 한다. 기획 변경과 DB 스키마 변경이 같은 파일을 건드린다면 분리 대상. |
| **O** | 개방-폐쇄 (Open-Closed, Meyer 1988) | 새 기능 추가 시 기존 파일을 수정하지 않고 **새 파일 추가**로 끝나야 한다. `if type == "A" ... elif type == "B"` 가 3개 이상 늘어나면 다형성/전략 패턴으로 전환. |
| **L** | 리스코프 치환 (Liskov, 1987) | 하위 타입은 상위 타입 자리에 놓여도 **선행조건을 강화하거나 후행조건을 약화하지 않는다.** 오버라이드해놓고 `NotImplementedError`를 던진다면 상속 관계가 틀린 것. |
| **I** | 인터페이스 분리 | 클라이언트가 쓰지 않는 메서드에 의존하게 만들지 않는다. 메서드 10개짜리 인터페이스 하나보다 3개짜리 인터페이스 세 개. |
| **D** | 의존성 역전 | 상위 정책 모듈이 하위 세부사항 모듈을 직접 import 하지 않는다. **둘 다 추상(인터페이스)에 의존한다.** 도메인 코드가 `import psycopg2` 하고 있다면 위반. |

### 2.1 보조 원칙

- **DRY** (Hunt & Thomas): 중복은 "코드의 중복"이 아니라 **"지식의 중복"** 을 뜻한다. 우연히 코드가 같은 것은 합치지 않는다.
- **YAGNI**: 지금 필요 없는 확장 포인트를 미리 만들지 않는다.
- **KISS**: 두 설계가 같은 요구를 만족하면 단순한 쪽을 택한다.
- **디미터 법칙 (Law of Demeter, Lieberherr 1987)**: 메서드는 ① 자기 자신 ② 파라미터 ③ 자신이 생성한 객체 ④ 자신의 필드 — 이 넷의 메서드만 호출한다. `a.getB().getC().doSomething()` 은 위반이며, `a.doSomething()` 으로 위임한다.
- **명령-질의 분리 (CQS, Meyer)**: 함수는 **상태를 바꾸거나(명령)** **값을 반환하거나(질의)** 둘 중 하나만 한다. `getUser()` 가 내부적으로 캐시를 갱신하면 안 된다.
- **상속보다 합성 (Composition over Inheritance, GoF)**: 상속은 `is-a` 가 영구적으로 참일 때만. 코드 재사용 목적의 상속은 금지.

---

## 3. 구조 판단을 위한 정량 지표 (Metrics)

"느낌"이 아니라 숫자로 리팩터링 시점을 정한다.

| 지표 | 정의 | 임계값 | 근거 |
|---|---|---|---|
| 순환 복잡도 (Cyclomatic Complexity) | 독립 실행 경로 수 = 분기 수 + 1 | 함수당 **≤ 10** (초과 시 분리) | McCabe, 1976 |
| 함수 길이 | 실행 라인 수 | **≤ 50줄**, 권장 20줄 | 화면 한 눈 원칙 |
| 파라미터 수 | | **≤ 3** (초과 시 파라미터 객체로) | Fowler, Refactoring |
| 중첩 깊이 | if/for 중첩 | **≤ 3** | 인지 부하 |
| 파일 길이 | | **≤ 400줄** | CCP 위반 신호 |
| 팬아웃 (Fan-out, CBO) | 이 모듈이 의존하는 모듈 수 | **≤ 7** | Chidamber & Kemerer, CK 메트릭 |
| LCOM | 메서드 응집 결여도 | 낮을수록 좋음 | CK 메트릭 |
| 불안정도 I | `I = Ce / (Ca + Ce)` (Ce=나가는 의존, Ca=들어오는 의존) | 도메인 계층은 I ≈ 0, 어댑터 계층은 I ≈ 1 | Martin |

> **임계값을 넘었다고 무조건 잘못된 것은 아니다.** 다만 **넘었다면 주석이나 ADR로 이유를 남긴다.**

---

## 4. 아키텍처 규약

### 4.1 의존성 규칙 (Dependency Rule) — Clean Architecture / Hexagonal (Cockburn, 2005)

**소스코드 의존성은 항상 안쪽(추상·정책)을 향한다. 바깥쪽(구체·세부사항)을 향해서는 안 된다.**

```
┌──────────────────────────────────────────────┐
│  interfaces/   (HTTP, CLI, 스케줄러, UI)      │  ← 가장 바깥
│  ┌────────────────────────────────────────┐  │
│  │  application/  (유스케이스, 트랜잭션)    │  │
│  │  ┌──────────────────────────────────┐  │  │
│  │  │  domain/  (엔티티, 값객체, 규칙)   │  │  │  ← 가장 안쪽
│  │  │  ※ 어떤 프레임워크도 import 금지   │  │  │
│  │  └──────────────────────────────────┘  │  │
│  └────────────────────────────────────────┘  │
│  infrastructure/ (DB, 외부API, 파일, 메시지큐) │
└──────────────────────────────────────────────┘
         infrastructure → domain 의 인터페이스를 구현
```

**강제 규칙**
1. `domain/` 은 프로젝트 외부 라이브러리를 import 하지 않는다 (표준 라이브러리 제외).
2. `domain/` 은 `application/`, `infrastructure/`, `interfaces/` 를 import 하지 않는다.
3. `application/` 은 `infrastructure/` 의 **구현체가 아니라 인터페이스(포트)** 에만 의존한다.
4. 의존성 방향 위반은 린터로 자동 검사한다 (`import-linter`, `ArchUnit`, `eslint-plugin-boundaries`, `go-arch-lint`).

### 4.2 패키지 원칙 (Martin)

**응집 원칙 — 무엇을 함께 묶을 것인가**
- **CCP (공통 폐쇄)**: *함께 변경되는 것을 함께 둔다.* → 계층별 묶기보다 **기능별 묶기(Package by Feature)** 를 기본으로 한다.
- **CRP (공통 재사용)**: 함께 쓰이지 않는 것에 의존을 강요하지 않는다.
- **REP (재사용-릴리스 등가)**: 재사용의 단위 = 릴리스의 단위.

**결합 원칙 — 패키지 간 관계**
- **ADP (비순환 의존)**: **패키지 간 순환 의존을 절대 허용하지 않는다.** 순환이 생기면 인터페이스를 추출해 의존을 역전시킨다.
- **SDP (안정된 의존)**: 자주 변하는 패키지가 안정된 패키지에 의존한다. 반대 방향 금지.
- **SAP (안정된 추상화)**: 안정된 패키지일수록 추상적이어야 한다.

**디렉터리 구조 기본형 (기능별 + 계층)**

```
src/
├── domain/
│   ├── order/                 # 기능(바운디드 컨텍스트) 단위
│   │   ├── order.py           # 엔티티
│   │   ├── order_status.py    # 값 객체
│   │   ├── order_repository.py # 포트(인터페이스)만
│   │   └── discount_policy.py # 도메인 서비스
│   └── payment/
├── application/
│   └── order/
│       ├── place_order.py     # 유스케이스 1개 = 파일 1개
│       └── cancel_order.py
├── infrastructure/
│   ├── persistence/
│   │   └── sql_order_repository.py  # 포트의 구현(어댑터)
│   └── external/
│       └── toss_payment_client.py
├── interfaces/
│   ├── http/
│   │   └── order_controller.py
│   └── cli/
└── shared/                    # 진짜 범용만. 도메인 지식 금지
    ├── result.py
    └── clock.py
tests/
docs/
└── adr/                       # 아키텍처 결정 기록
```

### 4.3 아키텍처 결정 기록 (ADR) — Nygard, 2011

구조를 바꿀 때마다 `docs/adr/NNNN-제목.md` 를 남긴다. 바이브코딩에서 특히 중요하다. AI는 "왜 이렇게 했는지"를 기억하지 못하므로, 결정 근거를 파일로 남겨야 다음 세션에서 구조가 되돌아가지 않는다.

```markdown
# ADR-0007: 주문 저장소를 인터페이스로 분리

- 상태: 채택 (2026-09-04)
- 맥락: 주문 유스케이스가 ORM 모델을 직접 참조해 테스트에 DB가 필요했다.
- 결정: domain/order/order_repository 를 포트로 정의하고 infrastructure에 어댑터를 둔다.
- 결과: (+) 유스케이스 단위 테스트가 DB 없이 가능 (−) 매핑 코드 증가
- 대안: ORM 직접 사용 유지 — 테스트 속도 문제로 기각
```

---

## 5. 명명 규약 (Naming)

### 5.1 이론적 근거

**Deissenboeck & Pizka (2006), "Concise and Consistent Naming"** — 좋은 이름은 **개념 ↔ 이름 사이의 준동형사상(homomorphism)** 을 이룬다. 두 가지 규칙으로 요약된다.

1. **일관성 (Consistency)**: 하나의 이름은 하나의 개념만 가리킨다.
   → `user` 가 어떤 곳에선 로그인 계정, 어떤 곳에선 결제 주체를 뜻하면 안 된다.
2. **간결성 (Conciseness)**: 하나의 개념은 하나의 이름만 갖는다.
   → 같은 것을 `member`, `user`, `account`, `customer` 로 번갈아 부르지 않는다.

**유비쿼터스 언어 (Ubiquitous Language) — Evans, DDD 2003**: 기획서/회의/DB/코드에서 **같은 단어**를 쓴다. 아래 용어 사전을 프로젝트에 맞게 채우고, 이 표에 없는 동의어는 코드에 등장시키지 않는다.

| 도메인 개념 | 채택 용어 | 사용 금지 동의어 |
|---|---|---|
| 서비스 이용자 | `Member` | user, account, customer |
| 구매 요청 | `Order` | purchase, transaction, buy |
| (프로젝트에 맞게 추가) | | |

**스코프 길이 규칙 (Ottinger's Rules)**: 이름의 길이는 **스코프의 크기에 비례**한다.
- 3줄짜리 루프 인덱스 → `i` 허용
- 모듈 전역 상수 → `DEFAULT_CONNECTION_TIMEOUT_MS`

### 5.2 품사 규칙 (Grammar)

| 대상 | 품사 | 예시 |
|---|---|---|
| 클래스 / 타입 | **명사구** | `OrderValidator`, `PaymentGateway` |
| 함수 / 메서드 | **동사구** (동사로 시작) | `calculateTotal`, `sendReceipt` |
| 불리언 변수·함수 | **서술형** (`is/has/can/should/needs`) | `isExpired`, `hasPermission`, `canRefund` |
| 컬렉션 | **복수형** | `orders`, `activeMembers` (`orderList` ❌) |
| 인터페이스 | 능력을 나타내는 명사·형용사 | `Repository`, `Serializable` (헝가리안 `IUserRepo` ❌) |
| 상수 | 명사, 대문자 | `MAX_RETRY_COUNT` |
| 이벤트 | **과거형 동사** | `OrderPlaced`, `PaymentFailed` |
| 커맨드 | **명령형 동사** | `PlaceOrder`, `CancelOrder` |

### 5.3 동사 어휘 통일

같은 의미에 여러 동사를 섞지 않는다. 프로젝트 표준을 아래로 고정한다.

| 의미 | 채택 동사 | 금지 |
|---|---|---|
| 단건 조회 (없으면 null/Optional) | `find` | get, fetch, retrieve, load, select |
| 단건 조회 (없으면 예외) | `get` | require, mustGet |
| 다건 조회 | `findAll`, `search` | list, query, getAll |
| 생성 후 저장 | `create` | add, insert, register, make |
| 수정 | `update` | modify, edit, change, set |
| 삭제 | `delete` | remove, destroy, erase |
| 형변환 | `to` (`toDto`) | convert, as, parse, transform |
| 외부 호출 | `request`, `send` | call, do, execute |
| 검증 (실패 시 예외) | `validate` | check, verify, ensure |
| 검증 (불리언 반환) | `is`, `can` | check, validate |

### 5.4 세부 규칙

- **약어 금지**: `usr`, `cfg`, `mgr`, `svc`, `tmp` ❌ → `user`, `config`, `manager`, `service`, `temporary`. 단 업계 표준 약어(`id`, `url`, `http`, `api`, `db`, `json`)는 허용.
- **단위·타입을 이름에 명시**: `timeout` ❌ → `timeoutMs`. `size` ❌ → `sizeBytes`. `price` ❌ → `priceKrw`. (Mars Climate Orbiter 사고의 교훈)
- **시각/기간 구분**: 시각은 `~At` (`createdAt`), 기간은 `~Duration`/`~Ms`, 날짜는 `~Date`.
- **매직 넘버·문자열 금지**: 리터럴은 이름 있는 상수로 승격. `if (status == 3)` ❌ → `if (status == OrderStatus.SHIPPED)`.
- **부정형 이름 금지**: `isNotValid` ❌ → `isValid` (이중 부정 `!isNotValid` 방지).
- **타입명 중복 금지**: `userObject`, `nameString`, `orderList` ❌ (타입 시스템이 이미 알려준다).
- **컨텍스트 중복 금지**: `Order` 클래스 안의 필드는 `orderId`가 아니라 `id`.
- **인코딩 접두사 금지**: 헝가리안 표기법(`strName`, `m_count`, `_private`)은 쓰지 않는다. (Python의 `_` 접두 비공개 관례는 언어 표준이므로 예외)

### 5.5 케이스 스타일 (언어별)

| 대상 | Python | JS/TS | Java/Kotlin | Go | C# | Rust |
|---|---|---|---|---|---|---|
| 클래스/타입 | `PascalCase` | `PascalCase` | `PascalCase` | `PascalCase`(공개) | `PascalCase` | `PascalCase` |
| 함수/메서드 | `snake_case` | `camelCase` | `camelCase` | `PascalCase`/`camelCase` | `PascalCase` | `snake_case` |
| 변수 | `snake_case` | `camelCase` | `camelCase` | `camelCase` | `camelCase` | `snake_case` |
| 상수 | `UPPER_SNAKE` | `UPPER_SNAKE` | `UPPER_SNAKE` | `PascalCase` | `PascalCase` | `UPPER_SNAKE` |
| 패키지/모듈 | `lowercase` | `kebab-case` | `lowercase` | `lowercase` | `PascalCase` | `snake_case` |

> **원칙: 언어의 공식 스타일 가이드(PEP 8, Effective Go, Java Code Conventions, Rust API Guidelines)를 이 문서보다 우선한다.** 관례를 거스르는 일관성은 일관성이 아니다.

---

## 6. 파일·디렉터리 명명 규약

### 6.1 기본 규칙

1. **파일명 = 주요 export의 이름.** `OrderValidator` 클래스가 들어있는 파일은 `order_validator.py` / `OrderValidator.java` / `orderValidator.ts`.
2. **한 파일에 공개 타입 하나.** 부수적인 내부 타입은 같은 파일에 둘 수 있다.
3. **역할 접미사를 일관되게 쓴다.**

| 접미사 | 역할 | 위치 |
|---|---|---|
| `*Controller`, `*Handler`, `*Router` | 외부 요청 수신 | `interfaces/` |
| `*UseCase`, `*Service` | 유스케이스 조율, 트랜잭션 경계 | `application/` |
| `*Repository` | 영속성 포트 | 인터페이스는 `domain/`, 구현은 `infrastructure/` |
| `*Client`, `*Gateway`, `*Adapter` | 외부 시스템 연동 | `infrastructure/` |
| `*Policy`, `*Rule`, `*Spec` | 도메인 규칙 | `domain/` |
| `*Factory`, `*Builder` | 생성 책임 | 대상과 같은 계층 |
| `*Dto`, `*Request`, `*Response` | 경계 전송 객체 | `interfaces/` |
| `*Mapper` | 계층 간 변환 | `infrastructure/` 또는 `interfaces/` |
| `*Config` | 설정 | `config/` |
| `*Error`, `*Exception` | 예외 타입 | 해당 도메인 내부 |

4. **디렉터리는 기능(feature) 우선, 그 안에서 계층.** (CCP 근거) 파일 20개 미만의 소규모라면 계층 우선도 허용하되, 기능이 3개를 넘으면 기능 우선으로 전환한다.
5. **디렉터리 이름은 복수형 명사** (`orders/`, `policies/`), 단 계층 이름은 관례를 따른다 (`domain/`, `application/`).
6. **금지 디렉터리 이름**: `utils/`, `common/`, `misc/`, `etc/`, `lib/`, `helpers/`, `manager/` (응집도 1단계 신호). `shared/` 만 예외적으로 허용하되 도메인 지식이 들어가면 안 된다.
7. **파일 이름에 번호·날짜·버전 금지**: `order_v2.py`, `main_final.py`, `20260904_fix.py` ❌. 버전 관리는 Git이 한다.

### 6.2 언어별 파일명 관례

| 언어 | 소스 파일 | 테스트 파일 |
|---|---|---|
| Python | `order_validator.py` (PEP 8: 짧은 소문자 + 언더스코어) | `test_order_validator.py` |
| Java/Kotlin | `OrderValidator.java` (공개 클래스명과 반드시 일치) | `OrderValidatorTest.java` |
| TypeScript/JS | 컴포넌트 `OrderCard.tsx`, 그 외 `order-validator.ts` (kebab-case 권장, 대소문자 구분 안 하는 파일시스템 사고 방지) | `order-validator.test.ts` |
| Go | `order_validator.go`, 패키지는 소문자 한 단어 | `order_validator_test.go` |
| C# | `OrderValidator.cs` | `OrderValidatorTests.cs` |
| Rust | `order_validator.rs` | 동일 파일 내 `#[cfg(test)] mod tests` |

### 6.3 브랜치·커밋 규약

- 브랜치: `<type>/<이슈번호>-<요약-kebab>` → `feat/142-order-cancel`
- 커밋: **Conventional Commits** — `<type>(<scope>): <설명>`
  - type: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `chore`, `build`
  - 예: `refactor(order): 주문 저장소를 포트/어댑터로 분리`
- **리팩터링 커밋과 기능 커밋을 절대 섞지 않는다.** 섞이면 리뷰에서 동작 변경을 발견할 수 없다. (Fowler, "두 개의 모자")

---

## 7. 문법·코드 스타일 규약

### 7.1 함수

```python
# ❌ 나쁨: 중첩 깊이 4, 여러 관심사, 제어 결합
def process(data, is_admin, send_mail):
    if data is not None:
        if data.status == 1:
            if is_admin:
                ...
```

```python
# ✅ 좋음: 가드 절(early return)로 깊이 축소, 단일 책임
def cancel_order(order: Order) -> CancelResult:
    if order.is_already_cancelled():
        return CancelResult.already_cancelled()
    if not order.is_cancellable():
        return CancelResult.not_cancellable(order.status)

    order.cancel(clock.now())
    return CancelResult.success(order.id)
```

**규칙**
1. **가드 절 우선**: 예외/종료 조건을 앞에서 반환하고, 정상 흐름을 들여쓰기 없이 둔다.
2. **한 함수는 한 추상화 수준** (Single Level of Abstraction). 고수준 호출과 저수준 비트 연산이 한 함수에 있으면 분리.
3. **파라미터 3개 초과 시 파라미터 객체 도입** (Fowler: Introduce Parameter Object).
4. **출력 파라미터 금지.** 결과는 반환값으로만.
5. **부수효과 숨기지 않기** (CQS). 이름이 `get`인데 상태를 바꾸면 안 된다.
6. **`null` 대신 명시적 표현**: `Optional`/`Result`/합타입을 쓴다. 널 반환은 호출자 전체에 널 체크를 전염시킨다.

### 7.2 타입과 불변성

- **기본은 불변(immutable)**: `const`, `final`, `val`, `frozen dataclass`, `readonly` 를 기본값으로 두고 가변이 필요할 때만 푼다.
- **원시 타입 집착(Primitive Obsession) 회피**: 도메인 의미가 있는 값은 값 객체로. `str email` ❌ → `Email` 타입 (생성 시점에 검증이 끝나 이후 코드가 검증을 반복하지 않는다).
- **타입 힌트/정적 타입은 공개 API에 필수.**

### 7.3 에러 처리

- **예상 가능한 실패(도메인 규칙 위반)** → 반환 타입(`Result`)이나 도메인 예외로 표현.
- **예상 불가능한 실패(버그, 인프라 장애)** → 예외를 그대로 전파하고, 최상위 경계 한 곳에서만 처리.
- **빈 catch 절대 금지.** 삼키려면 이유를 주석으로 남긴다.
- **예외 메시지에 컨텍스트 포함**: `"주문 없음"` ❌ → `f"주문을 찾을 수 없음: order_id={order_id}"`.
- 도메인 예외는 도메인 언어로 (`OrderAlreadyCancelledError`), 인프라 예외를 도메인 계층으로 새어나가게 하지 않는다.

### 7.4 주석

- 코드는 **무엇을(what)** 을 설명하고, 주석은 **왜(why)** 를 설명한다.
- 주석이 필요하다고 느끼면 먼저 **이름을 바꾸거나 함수를 추출**해본다. 그래도 남는 것만 주석으로.
- 주석 처리된 코드는 즉시 삭제한다(Git이 기억한다).
- 공개 API에는 문서 주석(docstring/JSDoc/KDoc) 필수: 목적, 파라미터, 반환, 발생 예외.

### 7.5 테스트

- **테스트 이름 = 명세**: `test_주문이_이미_취소되었으면_취소에_실패한다`
- 구조는 **Given-When-Then** (AAA) 3단으로 시각적으로 구분.
- **하나의 테스트는 하나의 동작만 검증** (assert 여러 개는 같은 개념일 때만).
- 테스트 피라미드: 단위 다수 → 통합 소수 → E2E 최소.
- 도메인 계층은 **테스트 더블 없이** 순수하게 테스트 가능해야 한다. 그렇지 않다면 의존성 역전이 안 된 것이다.

---

## 8. 진행 중인 코드의 점진적 구조 변경 전략

이미 상당 부분 개발된 상태이므로, 전면 재작성(Big Bang Rewrite)은 금지한다. 아래 순서를 따른다.

### 8.1 원칙

- **교살자 무화과 패턴 (Strangler Fig, Fowler)**: 새 구조를 옆에 만들고 호출을 하나씩 옮긴 뒤, 낡은 구조를 제거한다.
- **보이스카우트 규칙 (Beck/Martin)**: 만진 파일은 처음보다 조금 더 깨끗하게 두고 나온다.
- **특성화 테스트 우선 (Characterization Test, Feathers, *Legacy Code*)**: 리팩터링 전에 **현재 동작을 그대로 고정하는 테스트**를 먼저 만든다. 그 코드가 옳은지가 아니라 **무엇을 하는지**를 기록하는 것이 목적이다.
- **이음새 (Seam)**: 코드를 편집하지 않고 동작을 바꿔 끼울 수 있는 지점을 먼저 찾는다. 대개 생성자 주입·파라미터화가 가장 싼 이음새다.

### 8.2 실행 순서 (각 단계는 독립 PR)

| 단계 | 작업 | 안전장치 |
|---|---|---|
| 1 | **측정**: 복잡도·파일 길이·의존성 그래프를 도구로 뽑아 현황을 기록 | 도구 결과를 `docs/05-history/adr/ADR-{번호}-{제목}.md` 형식으로 저장(예: `ADR-010-다형연관-검증로직-통합.md`) |
| 2 | **이름 통일** (5.3 동사표, 용어 사전 적용) — 순수 rename만 | IDE 자동 rename, 동작 변경 0 |
| 3 | **파일 이동/분할** — 로직 수정 없이 위치만 이동 | `git mv`, import 경로만 변경 |
| 4 | **순환 의존 제거** (ADP) — 인터페이스 추출로 방향 역전 | 의존성 린터를 CI에 추가 |
| 5 | **도메인 추출** — 비즈니스 규칙을 프레임워크 코드에서 분리 | 특성화 테스트 선행 |
| 6 | **포트/어댑터 도입** — DB·외부 API를 인터페이스 뒤로 | 유스케이스 단위 테스트로 검증 |
| 7 | **린트 규칙 고정** — 이 문서의 규칙을 자동화 | CI 실패로 강제 |

> **한 PR에는 위 표의 한 단계만 담는다.** 이름 변경과 로직 변경이 섞인 diff는 사람이 검토할 수 없다.

### 8.3 바이브코딩 특화 지침

AI 코딩 도구는 세션 간 기억이 없어 구조가 계속 되돌아간다. 다음을 강제한다.

1. **이 문서를 `CLAUDE.md` / `AGENTS.md` 로 리포지토리 루트에 두고 매 세션 컨텍스트로 제공한다.**
2. 요청할 때 **"어느 계층에 만들 것인지"를 먼저 지정**한다. ("도메인 계층에 `RefundPolicy` 를 추가해줘. 외부 라이브러리 import 없이.")
3. **생성된 코드는 5.3 동사표와 4.1 의존성 규칙으로 즉시 검사**한다. 이 두 가지가 가장 자주 깨진다.
4. 구조 결정은 반드시 **ADR로 남긴다.** 그래야 다음 세션의 AI가 같은 결정을 반복한다.
5. **린터·포매터·의존성 검사를 CI에 넣는다.** 문서로만 있는 규약은 지켜지지 않는다.

---

## 9. 체크리스트 (PR 제출 전)

**설계**
- [ ] 새로 만든 모듈의 이름에 `utils`, `common`, `manager`, `helper` 가 없다
- [ ] 이 모듈이 변경될 이유를 한 문장으로 말할 수 있다 (SRP)
- [ ] `domain/` 코드가 프레임워크·DB·HTTP를 import 하지 않는다 (DIP)
- [ ] 패키지 간 순환 의존이 없다 (ADP)
- [ ] 동작을 분기시키는 불리언 파라미터가 없다 (제어 결합)
- [ ] `a.getB().getC()` 형태의 체이닝이 없다 (디미터)

**명명**
- [ ] 용어 사전(5.1)에 없는 동의어를 쓰지 않았다
- [ ] 동사가 5.3 표를 따른다 (`find`/`get`/`create`/`update`/`delete`)
- [ ] 불리언은 `is/has/can/should` 로 시작한다
- [ ] 단위가 있는 값의 이름에 단위가 붙어 있다 (`timeoutMs`)
- [ ] 파일명이 주요 export 이름과 일치한다
- [ ] 매직 넘버/문자열이 상수로 승격되었다

**구현**
- [ ] 함수 순환 복잡도 ≤ 10, 파라미터 ≤ 3, 중첩 ≤ 3
- [ ] 가드 절로 정상 흐름의 들여쓰기를 제거했다
- [ ] 값을 반환하는 함수가 상태를 바꾸지 않는다 (CQS)
- [ ] 빈 catch 블록이 없고, 예외 메시지에 식별자가 들어 있다
- [ ] 주석이 "왜"를 설명한다 (무엇을 설명하는 주석은 이름으로 대체)
- [ ] 리팩터링과 기능 변경이 같은 커밋에 섞이지 않았다

---

## 10. 참고 문헌

- Parnas, D. L. (1972). *On the Criteria To Be Used in Decomposing Systems into Modules.* CACM.
- Stevens, Myers & Constantine (1974). *Structured Design.* IBM Systems Journal. (응집도/결합도)
- Dijkstra, E. W. (1974). *On the Role of Scientific Thought.* (관심사의 분리)
- McCabe, T. J. (1976). *A Complexity Measure.* IEEE TSE. (순환 복잡도)
- Meyer, B. (1988). *Object-Oriented Software Construction.* (OCP, CQS, 계약에 의한 설계)
- Liskov, B. (1987). *Data Abstraction and Hierarchy.* (LSP)
- Lieberherr & Holland (1989). *Assuring Good Style for Object-Oriented Programs.* (디미터 법칙)
- Chidamber & Kemerer (1994). *A Metrics Suite for Object Oriented Design.* (CK 메트릭)
- Gamma et al. (1994). *Design Patterns.* (합성 우선)
- Fowler, M. (1999/2018). *Refactoring.* (리팩터링 카탈로그, 코드 냄새)
- Evans, E. (2003). *Domain-Driven Design.* (유비쿼터스 언어, 바운디드 컨텍스트)
- Feathers, M. (2004). *Working Effectively with Legacy Code.* (이음새, 특성화 테스트)
- Cockburn, A. (2005). *Hexagonal Architecture.* (포트와 어댑터)
- Deissenboeck & Pizka (2006). *Concise and Consistent Naming.* SQJ.
- Martin, R. C. (2008). *Clean Code.* / (2017). *Clean Architecture.* (SOLID, 패키지 원칙)
- Nygard, M. (2011). *Documenting Architecture Decisions.* (ADR)
- Ousterhout, J. (2018). *A Philosophy of Software Design.* (깊은 모듈, 복잡도의 정의)
