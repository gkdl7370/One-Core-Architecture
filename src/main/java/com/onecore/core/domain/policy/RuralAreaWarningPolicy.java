package com.onecore.core.domain.policy;

import com.onecore.core.domain.aggregate.WaterLevel;
import org.springframework.stereotype.Component;

/**
 * Warning policy for rural or low-density stations.
 */
@Component
public class RuralAreaWarningPolicy implements WaterWarningPolicy {

    private static final double DANGER_THRESHOLD_METERS = 5.0;

    @Override
    public boolean isDangerous(WaterLevel level) {
        return level.getMeters() >= DANGER_THRESHOLD_METERS;
    }
}
