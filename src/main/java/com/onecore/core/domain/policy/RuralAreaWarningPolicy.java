package com.onecore.core.domain.policy;

import com.onecore.core.domain.aggregate.WaterLevel;
import org.springframework.stereotype.Component;

/**
 * 농어촌 지역 경보 정책
 * 농경지 특성상 5.0m 초과 시 위험으로 판단
 */
@Component
public class RuralAreaWarningPolicy implements WaterWarningPolicy {

    private static final double DANGER_THRESHOLD_METERS = 5.0;

    @Override
    public boolean isDangerous(WaterLevel level) {
        return level.getMeters() >= DANGER_THRESHOLD_METERS;
    }
}