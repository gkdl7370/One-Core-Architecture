package com.onecore.adapter.out.external;

import com.onecore.core.domain.aggregate.ObservationId;
import com.onecore.core.domain.aggregate.StationId;
import com.onecore.core.domain.aggregate.WaterLevel;
import com.onecore.core.domain.aggregate.WaterObservation;
import com.onecore.core.domain.exception.InvalidObservationException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Anti-corruption mapper for an anonymized eGov-style water-level row.
 */
@Component
public class EgovHydWaterLevelMapper {

    public WaterObservation toDomain(EgovHydWaterLevelRecord record) {
        return new WaterObservation(
                new ObservationId(record.getRowId()),
                new StationId(record.getStationCode()),
                normalizeLevel(record.getWaterLevel(), record.getUnitCode())
        );
    }

    private WaterLevel normalizeLevel(double value, String unitCode) {
        if (unitCode == null || unitCode.isBlank()) {
            throw new InvalidObservationException("Water level unit must not be blank.");
        }

        String normalizedUnit = unitCode.trim().toUpperCase(Locale.ROOT);
        if ("CM".equals(normalizedUnit)) {
            return WaterLevel.fromCentimeters(value);
        }
        if ("M".equals(normalizedUnit) || "METER".equals(normalizedUnit)) {
            return new WaterLevel(value);
        }

        throw new InvalidObservationException("Unsupported water level unit: " + unitCode);
    }
}
