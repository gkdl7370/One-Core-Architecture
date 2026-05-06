package com.onecore.adapter.out.external;

import com.onecore.core.domain.aggregate.ObservationId;
import com.onecore.core.domain.aggregate.StationId;
import com.onecore.core.domain.aggregate.WaterLevel;
import com.onecore.core.domain.aggregate.WaterObservation;
import com.onecore.core.domain.exception.InvalidObservationException;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption layer for converting external payloads into the core domain.
 */
@Component
public class LegacyObservationMapper {

    public WaterObservation toDomain(SystemAPayload payload) {
        if (payload.getWaterLevelCm() < 0) {
            throw new InvalidObservationException(
                    "Water level must not be negative: " + payload.getWaterLevelCm()
            );
        }

        WaterLevel standardLevel = WaterLevel.fromCentimeters(payload.getWaterLevelCm());

        return new WaterObservation(
                new ObservationId(payload.getId()),
                new StationId(payload.getStationCode()),
                standardLevel
        );
    }
}
