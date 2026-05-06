# One-Core Architecture

파편화된 레거시 시스템을 하나의 기준 도메인으로 통합하고, 변경이 잦은 규칙과 외부 연동을 코어 밖으로 밀어내기 위한 아키텍처 샘플입니다.

이 프로젝트는 단순 CRUD 구현보다 **왜 이런 구조가 필요한지**, **어떤 기술과 패턴을 선택했는지**, **레거시 시스템을 어떤 방식으로 점진적으로 현대화할 수 있는지**를 보여주는 포트폴리오용 엔지니어링 기록입니다.

## 문제의식

여러 레거시 시스템을 통합하다 보면 단순한 DB Join이나 Mapper 추가만으로는 해결하기 어려운 문제가 생깁니다.

1. **파편화된 규격**
   지역, 기관, 시스템마다 단위(cm/m), 관측 주기, JSON 구조, DB 컬럼명이 다릅니다. 이런 차이를 Service나 SQL에서 즉흥적으로 맞추면 정합성을 보장하기 어렵습니다.

2. **비대해지는 코어 로직**
   데이터 유형과 지역별 규칙이 늘어날수록 if-else 검증 로직이 증가합니다. 시간이 지나면 핵심 비즈니스 규칙이 어디에 있는지 파악하기 어려운 구조가 됩니다.

3. **외부 시스템 변경에 취약한 구조**
   레거시 payload, DB alias, JSP form field 이름이 코어 로직까지 침투하면 외부 시스템의 작은 변경이 내부 도메인 변경으로 번집니다.

4. **조회 성능 병목**
   서로 다른 원본 데이터를 매번 실시간으로 정규화하고 조회하면 RDBMS I/O 병목이 발생합니다. 특히 수자원/관측 도메인처럼 읽기 빈도가 높은 화면에서는 캐시와 인덱스 전략이 필요합니다.

이 복잡도를 통제하기 위해 One-Core는 시스템의 중심을 DB나 화면이 아니라 **안정적인 도메인 모델**에 둡니다.

## 핵심 방향

One-Core의 핵심 아이디어는 간단합니다.

```text
외부 시스템의 제각각인 데이터
        |
        v
Adapter / Anti-Corruption Layer
        |
        v
기준 도메인 모델
        |
        v
정책, 유스케이스, 저장소, 캐시, API
```

외부 시스템은 계속 바뀔 수 있습니다. 하지만 코어 도메인은 가능한 오래 유지되어야 합니다. 그래서 모든 외부 데이터는 코어에 들어오기 전에 반드시 어댑터를 거쳐 기준 도메인 객체로 변환됩니다.

## 기술적 의사결정

### 1. 왜 DDD를 선택했는가?

초기에는 데이터베이스 테이블을 먼저 설계하고 로직을 맞추는 데이터 중심 설계를 고려할 수 있습니다. 하지만 레거시 통합 상황에서는 DB 구조 자체가 시스템마다 다릅니다.

DB에 코어 로직을 맞추면 외부 스키마 변경이 곧 코어 시스템 변경으로 이어집니다. 그래서 One-Core는 외부 시스템의 규격에 휘둘리지 않는 **기준 도메인(Standard Domain)** 을 먼저 정의합니다.

선택:

- 핵심 개념을 `WaterObservation`, `WaterLevel`, `StationId` 같은 도메인 객체로 표현
- 외부 payload와 DB row는 어댑터에서 도메인 객체로 변환
- 코어 도메인은 Spring MVC, JSP, SQL Mapper, Redis 같은 기술 세부사항을 모르게 유지

결과:

- 외부 시스템의 단위나 payload 구조가 바뀌어도 코어 비즈니스 규칙은 유지
- 변경 지점이 Adapter로 격리됨
- 테스트 가능한 순수 Java 도메인 모델 확보

### 2. 왜 Aggregate와 Policy로 분리했는가?

통합 도메인을 만들더라도 지역마다 다른 경보 기준, 데이터 검증 규칙, 산출 정책을 도메인 객체 내부에 모두 넣으면 Aggregate가 계속 비대해집니다.

선택:

- 데이터의 정합성이 함께 유지되어야 하는 단위를 Aggregate로 묶음
- `WaterObservation`은 정규화된 관측값의 중심 객체로 유지
- 지역별 경보 기준은 `WaterWarningPolicy` 인터페이스로 분리
- 도시형, 농촌형 같은 정책은 별도 구현체로 확장

결과:

- 새로운 경보 기준이 추가되어도 기존 Aggregate 수정 최소화
- OCP에 가까운 구조 확보
- 도메인 객체는 상태와 핵심 행위에 집중하고, 변동성 높은 규칙은 Policy로 격리

### 3. 왜 Anti-Corruption Layer를 두었는가?

레거시 시스템은 필드명, 단위, 날짜 포맷, 누락값 처리 방식이 제각각입니다. 이 차이가 코어 도메인까지 들어오면 코어는 더 이상 안정적인 모델이 아니라 레거시 호환 코드가 됩니다.

선택:

- `LegacyObservationMapper`로 일반 레거시 payload를 변환
- `EgovHydWaterLevelMapper`로 익명화된 eGov 스타일 수위 관측 row를 변환
- cm/m 같은 단위 차이는 Adapter에서 meter로 정규화
- 음수 수위나 지원하지 않는 단위는 Adapter 경계에서 거부

결과:

- 코어 도메인은 항상 정규화된 값만 다룸
- 레거시별 변환 규칙을 한 곳에 모을 수 있음
- 새로운 레거시 시스템을 붙일 때 Adapter만 추가하는 방향으로 확장 가능

### 4. 왜 Cache-Aside와 인덱스 전략을 분리해서 다루는가?

관측 데이터는 조회 빈도가 높고, 특정 관측소의 최신값 또는 기간별 목록을 반복적으로 조회하는 패턴이 많습니다. 이때 DB 인덱스와 Redis 캐시는 중요하지만, 이것은 도메인 규칙이 아니라 인프라 전략입니다.

선택:

- 최신 관측값 조회는 Cache-Aside 패턴으로 확장 가능하게 설계
- DB에서는 `(station_id, observed_at)` 같은 조회 패턴 기반 복합 인덱스를 고려
- JSONB나 공간 데이터 검색은 필요 시 GIN 인덱스 같은 전용 전략을 분리 적용
- 현재 샘플 코드에서는 인프라 구현보다 도메인과 Adapter 경계를 먼저 보여줌

결과:

- 성능 최적화가 도메인 모델을 오염시키지 않음
- 캐시/DB 교체 또는 확장이 쉬움
- 포트폴리오 단계에서는 설계 의도와 확장 방향을 명확히 설명 가능

## 전체 구조

```text
[ External Legacy / New Systems ]
        |
        v
  Adapter / Mapper
        |
        v
+-----------------------------------------+
|              One-Core System            |
|                                         |
|  Application                            |
|  - Use Case orchestration               |
|  - Policy selection                     |
|                                         |
|  Core Domain                            |
|  - Aggregate                            |
|  - Value Object                         |
|  - Policy Interface                     |
|  - Domain Exception                     |
|                                         |
|  Adapter                                |
|  - External legacy mapper               |
|  - Persistence                          |
|  - Cache                                |
|  - Web/API                              |
+-----------------------------------------+
        |
        v
[ PostgreSQL / Redis / External APIs ]
```

## 패키지 구조

```text
one-core-architecture
|-- core
|   `-- domain
|       |-- aggregate              # WaterObservation, WaterLevel, StationId
|       |-- policy                 # WaterWarningPolicy, Urban/Rural policy
|       `-- exception              # InvalidObservationException
|
|-- adapter
|   `-- out
|       `-- external               # LegacyObservationMapper, EgovHydWaterLevelMapper
|
`-- docs
    `-- portfolio-scope.md         # 공개 가능한 포트폴리오 범위와 익명화 기준
```

향후 확장 시에는 다음 구조를 추가할 수 있습니다.

```text
core
`-- application
    |-- port
    `-- service

adapter
|-- in
|   `-- web
`-- out
    |-- persistence
    `-- cache
```

## 현재 코드로 확인할 수 있는 것

- 수위 관측값을 표현하는 코어 도메인 객체
- 레거시 cm 단위 payload를 표준 meter 단위로 변환하는 Mapper
- eGov 스타일 SQL Mapper row를 코어 도메인으로 변환하는 익명화 Adapter
- 지역별 경보 기준을 Policy 객체로 분리하는 방식
- 정규화, 검증, 정책 판단을 고정하는 단위 테스트

## 아직 샘플로 남겨둔 것

다음 요소는 현재 전체 구현이 아니라 설계 방향 또는 다음 확장 후보로 남겨두었습니다.

- 완성된 DB 영속성 모델
- Redis 기반 Cache-Aside 구현
- 운영 수준 REST API
- 여러 레거시 시스템을 end-to-end로 수집하는 흐름
- GeoServer 또는 공간 데이터 검색 연동

## 적용 예시: 공공 하천관리 수위 모듈 현대화

이 저장소의 예시는 실제 원본 소스코드를 공개하지 않고, 공공 하천관리 레거시 시스템에서 볼 수 있는 구조를 익명화하여 재구성한 것입니다.

기존 레거시 흐름:

```text
JSP 화면 -> Spring MVC Controller -> Service -> SQL XML Mapper -> Legacy DB Table
```

One-Core 적용 흐름:

```text
Legacy Payload / DB Row
        |
        v
EgovHydWaterLevelMapper
        |
        v
WaterObservation
        |
        v
WaterWarningPolicy
```

이 예시는 `hydWaterLevels`와 유사한 수위 관측 모듈을 대상으로 합니다. 핵심은 원본 JSP, Java, SQL XML을 복사하는 것이 아니라, 그 안에 숨어 있던 “수위 관측값 정규화”와 “경보 기준 판단”을 코어 도메인으로 끌어올리는 것입니다.

포트폴리오에서 설명할 수 있는 문장:

> eGovFrame 기반 레거시 하천관리 시스템을 분석하고, 수위 관측 모듈을 익명화된 One-Core 아키텍처 샘플로 재구성했습니다. 외부 시스템의 단위와 row 구조는 Adapter에서 흡수하고, 코어 도메인은 정규화된 관측값과 경보 정책만 다루도록 분리했습니다.

## 테스트 실행

```bash
./mvnw test
```

현재 로컬 환경에서는 Maven Wrapper 기동 오류가 있어 테스트를 실행하지 못했습니다. 코드 검증은 `git diff --check`로 문법적 diff 문제를 확인했고, Maven 실행 환경이 정리되면 `./mvnw test`로 단위 테스트를 검증할 수 있습니다.
