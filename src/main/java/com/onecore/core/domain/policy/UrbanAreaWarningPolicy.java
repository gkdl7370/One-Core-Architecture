package com.onecore.core.domain.policy;

import com.onecore.core.domain.aggregate.WaterLevel;
import org.springframework.stereotype.Component;

/**
 * 도심 지역 경보 정책
 * 인프라 밀집으로 3.5m 초과 시 위험으로 판단
 */
@Component
public class UrbanAreaWarningPolicy implements WaterWarningPolicy {

    private static final double DANGER_THRESHOLD_METERS = 3.5;

    @Override
    public boolean isDangerous(WaterLevel level) {
        return level.getMeters() >= DANGER_THRESHOLD_METERS;
    }
}