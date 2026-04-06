# 🏛 Architecture Implementation Records (ADR 예시)

본 문서는 README에 기술된 **One-Core Architecture**의 핵심 의사결정 4가지가 실제 코드(How)로 어떻게 구현되었는지 증명하는 최소 구현 예시

---

## 1. Anti-Corruption Layer (이기종 데이터 정규화)

**목적:** 외부 레거시 시스템의 파편화된 규격(`cm`, `m`, 다른 JSON 구조)이 코어 도메인으로 침투하는 것을 방어

```java
// Adapter Layer (외부 -> 내부 변환기)
@Component
public class LegacyObservationMapper {
    
    // 외부 시스템 A의 Payload (cm 단위 사용)
    public WaterObservation toDomain(SystemAPayload payload) {
        // 1. 방어적 프로그래밍: 비정상 데이터 원천 차단
        if (payload.getWaterLevelCm() < 0) {
            throw new InvalidObservationException("수위는 음수일 수 없습니다.");
        }

        // 2. 단위 정규화: cm -> 코어 표준 단위인 m로 변환
        double standardLevelMeters = payload.getWaterLevelCm() / 100.0;
        
        // 3. 순수 도메인 객체(Aggregate) 생성하여 반환
        return new WaterObservation(
            new ObservationId(payload.getId()),
            new StationId(payload.getStationCode()),
            new WaterLevel(standardLevelMeters)
        );
    }
}
```
---

## 2. Aggregate Root & Policy 추상화 (OCP 준수)

**목적:** 코어 비대화를 막기 위해 지역마다 다른 '위험 수위 경보 규칙'을 인터페이스로 분리하여 런타임에 주입

```java
// 1. 순수 도메인 (Aggregate Root) - 비즈니스 규칙의 중심
public class WaterObservation {
    private final ObservationId id;
    private final WaterLevel level;

    // 외부에서 의존성(Policy)을 주입받아 다형성 활용
    public boolean isDangerousStatus(WaterWarningPolicy policy) {
        return policy.isDangerous(this.level);
    }
}

// 2. 정책 인터페이스 (Core Domain)
public interface WaterWarningPolicy {
    boolean isDangerous(WaterLevel level);
}

// 3. 정책 구현체 (새로운 지역 추가 시 기존 코드 수정 없이 클래스만 추가)
public class UrbanAreaWarningPolicy implements WaterWarningPolicy {
    @Override
    public boolean isDangerous(WaterLevel level) {
        return level.getMeters() >= 3.5; // 도심 지역은 3.5m부터 위험
    }
}

public class RuralAreaWarningPolicy implements WaterWarningPolicy {
    @Override
    public boolean isDangerous(WaterLevel level) {
        return level.getMeters() >= 5.0; // 농어촌 지역은 5.0m부터 위험
    }
}

```
---

## 3. DB Indexing 전략 (실행 계획 기반 튜닝)

**목적:** 복잡한 수자원 데이터 조회 성능을 극대화하기 위해 실제 쿼리 패턴에 맞춘 인덱스 적용

```
-- 1. Composite Index (복합 인덱스)
-- 쿼리 패턴: WHERE station_id = ? AND observe_time BETWEEN ? AND ?
-- 카디널리티가 높은 관측소 ID를 선행 컬럼으로 배치
CREATE INDEX idx_observation_station_time 
ON water_observations (station_id, observe_time DESC);

-- 2. Partial GIN Index (부분 GIN 인덱스)
-- 목적: JSONB 확장 필드 검색 속도 향상 및 디스크 오버헤드 통제
-- 방어 전략: 특정 메타데이터가 존재하는 로우(Row)에만 인덱스 생성
CREATE INDEX idx_observation_metadata_gin 
ON water_observations USING GIN (metadata_jsonb)
WHERE metadata_jsonb ? 'sensor_error_code';

```
---

## 4. Cache-Aside Pattern (디스크 I/O 방어)

**목적:** Read-Heavy 트래픽의 디스크 접근을 차단하고 3배의 성능 향상을 끌어낸 Redis 캐싱 계층 구현

```java
// Application Layer (Use Case)
@Service
@RequiredArgsConstructor
public class ObservationQueryService {

    private final RedisCacheAdapter redisCache;
    private final ObservationRepository dbRepository;

    public ObservationDto getLatestObservation(String stationId) {
        String cacheKey = "station:latest:" + stationId;

        // 1. Cache 조회 (Redis)
        return redisCache.get(cacheKey)
            .orElseGet(() -> {
                // 2. Cache Miss 발생 시 DB에서 조회 (Fallback)
                ObservationDto dto = dbRepository.findLatestByStationId(stationId)
                    .orElseThrow(() -> new NotFoundException());
                
                // 3. DB 결과를 Cache에 적재 (TTL 5분 설정으로 정합성 유지)
                redisCache.put(cacheKey, dto, Duration.ofMinutes(5));
                return dto;
            });
    }
}

```

> 본 문서의 코드 예시는 `src/main/java/com/onecore/` 경로의 실제 구현체를 기반으로 합니다.
