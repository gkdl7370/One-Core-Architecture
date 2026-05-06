package com.onecore.core.domain.policy;

import com.onecore.core.domain.aggregate.WaterLevel;
import org.springframework.stereotype.Component;

/**
 * Warning policy for dense urban stations.
 */
@Component
public class UrbanAreaWarningPolicy implements WaterWarningPolicy {

    private static final double DANGER_THRESHOLD_METERS = 3.5;

    @Override
    public boolean isDangerous(WaterLevel level) {
        return level.getMeters() >= DANGER_THRESHOLD_METERS;
    }
}
