package com.onecore.core.domain.aggregate;

import com.onecore.core.domain.policy.WaterWarningPolicy;

/**
 * Aggregate root for normalized water observation data.
 * Legacy payload changes are absorbed by adapter mappers before reaching this class.
 */
public class WaterObservation {

    private final ObservationId id;
    private final StationId stationId;
    private final WaterLevel level;

    public WaterObservation(ObservationId id, StationId stationId, WaterLevel level) {
        this.id = id;
        this.stationId = stationId;
        this.level = level;
    }

    public boolean isDangerous(WaterWarningPolicy policy) {
        return policy.isDangerous(this.level);
    }

    public ObservationId getId() {
        return id;
    }

    public StationId getStationId() {
        return stationId;
    }

    public WaterLevel getLevel() {
        return level;
    }
}
