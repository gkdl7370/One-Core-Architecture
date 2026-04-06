package com.onecore.adapter.out.external;

import com.onecore.core.domain.aggregate.*;
import com.onecore.core.domain.exception.InvalidObservationException;
import org.springframework.stereotype.Component;

/**
 * Anti-Corruption Layer.
 * 외부 레거시 시스템의 파편화된 규격을 코어 도메인으로 변환
 * 외부 시스템의 스펙이 바뀌어도 이 클래스만 수정
 */
@Component
public class LegacyObservationMapper {

    /**
     * 레거시 시스템 A (cm 단위 사용)를 도메인으로 변환
     */
    public WaterObservation toDomain(SystemAPayload payload) {
        if (payload.getWaterLevelCm() < 0) {
            throw new InvalidObservationException(
                    "수위는 음수일 수 없습니다: " + payload.getWaterLevelCm()
            );
        }

        // cm → m 단위 정규화 (코어 도메인은 항상 m 기준)
        WaterLevel standardLevel = WaterLevel.fromCentimeters(payload.getWaterLevelCm());

        return new WaterObservation(
                new ObservationId(payload.getId()),
                new StationId(payload.getStationCode()),
                standardLevel
        );
    }
}