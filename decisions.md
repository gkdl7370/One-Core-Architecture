# 아키텍처 의사결정

## ADR-001: 코어를 eGovFrame과 분리한다

공공 Java 레거시 애플리케이션에서는 비즈니스 규칙이 Spring MVC Controller, JSP 화면 처리, SQL Mapper 결과 구조와 섞여 있는 경우가 많습니다. One-Core는 도메인 모델을 순수 Java 코드로 유지하여 웹 컨테이너를 띄우지 않고도 테스트할 수 있도록 합니다.

결정:

- 지속적으로 유지되어야 하는 규칙은 `core.domain`에 둔다.
- 레거시 payload 변환은 `adapter.out.external`에 둔다.
- SQL Mapper의 row 이름이나 DB 컬럼명이 도메인으로 새어 들어오지 않게 한다.

## ADR-002: Anti-Corruption Mapper를 둔다

레거시 수위 모듈은 row id, 관측소 코드, 관측 시각 문자열, 수위 값, 단위 코드 같은 필드를 노출할 수 있습니다. 코어는 원본 row가 cm를 썼는지, m를 썼는지, Oracle alias를 썼는지, JSP form field 이름을 썼는지 알 필요가 없습니다.

결정:

- 모든 레거시 데이터 형태는 `WaterObservation`으로 변환한다.
- 잘못된 값은 어댑터 경계에서 거부한다.
- 수위 값은 Aggregate에 도달하기 전에 meter 단위로 정규화한다.

## ADR-003: 경보 규칙은 Policy로 모델링한다

경보 기준은 관측소 유형과 운영 맥락에 따라 달라질 수 있습니다. 이 규칙을 Aggregate 내부에 하드코딩하면 새로운 지역 유형이 추가될 때마다 Aggregate가 계속 변경됩니다.

결정:

- `WaterWarningPolicy`를 정의한다.
- 도시형/농촌형 같은 관측소 유형별 정책 구현체를 둔다.
- Application Service가 상황에 맞는 Policy를 선택하도록 한다.

## ADR-004: 캐시와 인덱스는 어댑터 관심사로 둔다

관측 화면은 조회가 많기 때문에 Cache-Aside 전략과 DB 인덱스 튜닝이 유용합니다. 하지만 이것은 코어 도메인 규칙이 아닙니다.

결정:

- Redis와 DB 인덱스 선택은 문서나 어댑터 계층에서 다룬다.
- 현재 단계에서는 `WaterObservation`에 영속성 annotation을 붙이지 않는다.
- `(station_id, observed_at)` 기반 복합 인덱스 같은 내용은 코어 코드가 아니라 마이그레이션 가이드로 설명한다.
