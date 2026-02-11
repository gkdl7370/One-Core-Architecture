# One-Core Architecture
파편화된 레거시 통합 및 룰 엔진(Rule Engine) 기반 확장 전략  

전국에 흩어진 이기종 수자원 데이터 및 노후화된 시스템을 단일 도메인으로 통합하는 과정에서 마주한  
구조적 병목(Bottleneck)들을 해결하기 위한 아키텍처적 고민  **왜 특정 기술과 패턴을 선택했는지**를 기록한 엔지니어링 로그

---

## 🎯 고민사항

1. **파편화된 규격**:  
   지역마다 단위(cm / m), 평균 산출 주기(1·5·10분), JSON 스키마 구조가 모두 상이하여 단순한 매핑이나 DB Join으로는 정합성을 보장할 수 없는 상태
2. **비대해지는 코어 로직**:  
   데이터 유형과 지역이 추가될 때마다 검증 로직(If-Else)이 기하급수적으로 증가함에 따라  
   핵심 비즈니스 로직이 무엇인지 파악할 수 없는 스파게티 코드가 됨
4. **성능의 한계**:  
흩어진 데이터를 실시간으로 정규화하여 조회하려다 보니 RDBMS의 I/O 병목 발생

이 복잡도를 통제하기 위해 단순한 CRUD 구현을 멈추고 시스템의 근본적인 뼈대(Architecture)를 다시 설계하기로 결정

---

## 🧱 기술적 의사결정

### 1. 왜 하필 DDD(Domain-Driven Design)를 선택했는가?
초기에는 데이터베이스 테이블을 먼저 설계하고 로직을 맞추는 데이터 중심 설계를 고려함  
하지만 스키마와 단위가 제각각인 상황에서 DB 구조에 의존하면 외부 시스템의 변경이 코어 시스템의 붕괴로 직결된다는 것을 깨달음

* **선택**: 외부 시스템의 규격에 휘둘리지 않는 절대적인 기준 도메인(Standard Domain)이 필요  
  이를 위해 DDD를 도입하여 코어 도메인을 정의하고 외부에서 들어오는 모든 이기종 데이터는 어댑터(Adapter) 계층을 거치며  
  무조건 이 '기준 도메인' 객체로 변환되도록 강제함
  
* **결과**: 외부 시스템의 스펙이 바뀌어도 코어 비즈니스 로직은 수정할 필요가 없는 방어벽(Anti-Corruption Layer) 구축


### 2. 왜 도메인을 Aggregate로 묶고 Interface로 분리했는가?
통합 도메인을 만들었지만 지역마다 다른 '수위 경고 임계값'이나 '데이터 검증 규칙'을  
도메인 객체 안에 전부 우겨넣으면 클래스가 무한정 팽창할 위험이 존재

* **선택 (Aggregate)**: 데이터의 라이프사이클과 정합성이 함께 유지되어야 하는 단위(예: 수자원 관측소 + 관측 데이터)를  
  **Aggregate**로 묶어 외부에서는 반드시 Aggregate Root를 통해서만 상태를 변경하도록 제약(Invariant)을 검
  
* **선택 (Interface 추상화)**: 변동성이 심한 '검증 규칙'과 '정책'은 도메인 내부에 하드코딩하지 않고  
  `ValidationPolicy` 같은 **인터페이스(Interface)로 추상화**
  
* **결과**: 새로운 산출 규칙이 들어오면 기존 코드를 수정하는 대신  
  새로운 구현체만 갈아 끼우면 되는 구조(OCP 준수)를 완성하여 유지보수성을 극적으로 끌어올림


### 3. 왜 Redis 인메모리 캐시로 해결했는가?
인덱스 튜닝을 통해 DB 쿼리 성능을 극대화했음에도 런타임에 이기종 데이터를 매번 도메인 기준으로 변환하여  
실시간으로 응답하는 데에는 여전히 물리적 디스크 I/O의 한계가 존재

* **선택**: 원본 데이터의 변동 주기에 비해 조회(Read) 빈도가 압도적으로 높다는 수자원 도메인의 특성을 분석하여  
  DB 부하를 원천 차단하고 읽기 속도를 극대화하기 위해 **Redis 기반의 Cache-Aside 패턴**을 도입
  
* **결과**: 정규화된 도메인 뷰(View) 및 정책 데이터를 Redis에 적재함으로써 복잡한 연산 과정을 생략하고  
  **실시간 데이터 조회 성능을 약 3배 향상**시키는 데 성공


### 4. 조회 병목 해결: 인덱스 재설계와 GIN 인덱스
코어 도메인 통합 후, 방대한 수자원 데이터를 조회할 때 극심한 지연(Latency)이 발생

* **선택 (실행 계획 기반 Composite 인덱스)**: 기존의 단일 컬럼 인덱스들은 실제 조회 패턴과 불일치함 실제 쿼리의 실행 계획을 분석하여  
**카디널리티(Cardinality)가 높은 컬럼의 동등 조건(=) 검색이 가장 먼저 타도록 복합(Composite) 인덱스를 재구성**
  
* **선택 (JSONB/공간 데이터용 GIN 인덱스)**: B-Tree 인덱스로는 지도 공간 데이터와 JSONB 검색의 한계를 넘을 수 없어  
  해당 데이터 검색에 특화된 GIN 인덱스를 별도로 도입


#### ⚠️ Trade-off 제어 및 방어 전략
인덱스 추가로 인한 쓰기 성능 저하를 방어하기 위해 엄격한 제약을 둠
1. **Composite 인덱스 단점 방어**: INSERT/UPDATE 성능 저하 및 관리 비용 증가를 막기 위해  
   쓰기 빈도가 낮고 조회가 압도적으로 많은(Read-Heavy) 테이블에만 최소한으로 적용
2. **GIN 인덱스 단점 방어**: 막대한 디스크 사용량과 갱신 오버헤드를 제어하기 위해  
   지도 및 JSON 조회 전용 테이블로 범위를 격리하고 부분 인덱스(Partial Index)를 걸어 인덱스 크기를 최소화

---

```text
[ External Legacy / New Systems ]
        │
        ▼ (REST / gRPC Adapter)
┌───────────────────────────────────────────┐
│              One Core System              │
│  ├─ Application (Use Cases)               │
│  ├─ Domain (Aggregate, Entities)          │
│  │    └─> Interface Policies (Strategy)   │
│  └─ Rule Engine (Rules as Data)           │
└───────────────────────────────────────────┘
        │                            │
  (Cache Check)                 (DB Query)
        │                            │
        ▼                            ▼
 [ Redis Cache ]              [ PostgreSQL ]
(Cache-Aside View)       (Composite / GIN Index)
```

---

## 📦 패키지 구조(예시)

```
단순한 계층형(Layered) 구조를 탈피하고, 의존성이 바깥에서 안쪽(Core)으로만 향하도록 아키텍처를 물리적 디렉토리로 강제했습니다.

```text
one-core-architecture
├── core
│   ├── domain                     # [순수 도메인] 기술 프레임워크 의존성 제로 (POJO)
│   │   ├── aggregate              # WaterObservation (수자원 관측 데이터 Aggregate Root)
│   │   ├── policy                 # UnitConversionPolicy (단위 변환), ValidationPolicy (검증 룰 인터페이스)
│   │   └── exception              # InvalidObservationException (비즈니스 예외 격리)
│   │
│   └── application                # [유스케이스] 도메인 룰 오케스트레이션
│       ├── port                   # [인터페이스] in (IntegrateDataUseCase), out (LoadRulePort, SaveObservationPort)
│       └── service                # [구현체] WaterIntegrationService (핵심 비즈니스 흐름 제어)
│
├── adapter                        # [인프라 연동] 외부 기술 및 DB, 캐시 구현체
│   ├── in
│   │   └── web                    # [REST API] WaterObservationController (외부 데이터 수신 진입점)
│   │
│   └── out
│       ├── persistence            # [RDBMS] ObservationJpaEntity, ObservationRepositoryAdapter (복합/GIN 인덱스 튜닝)
│       ├── cache                  # [Redis] RedisObservationCacheAdapter (조회 병목 해결용 Cache-Aside 구현)
│       └── external               # [Legacy 연동] LegacySystemClient, AntiCorruptionMapper (이기종 데이터 정규화)
│
└── docs
    ├── architecture.md            # 시스템 구조도 및 데이터 정합성 보장 전략
    └── decisions.md               # ADR (샤딩 배제 이유, Redis 도입 등 기술적 의사결정 기록)
```

---
