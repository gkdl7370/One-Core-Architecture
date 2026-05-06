package com.onecore.core.domain.policy;

import com.onecore.core.domain.aggregate.WaterLevel;

/**
 * Strategy interface for station-specific water warning rules.
 */
public interface WaterWarningPolicy {
    boolean isDangerous(WaterLevel level);
}
