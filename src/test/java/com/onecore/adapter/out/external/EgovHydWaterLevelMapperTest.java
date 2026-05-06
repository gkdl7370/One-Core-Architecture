package com.onecore.adapter.out.external;

import com.onecore.core.domain.aggregate.WaterObservation;
import com.onecore.core.domain.exception.InvalidObservationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EgovHydWaterLevelMapperTest {

    private final EgovHydWaterLevelMapper mapper = new EgovHydWaterLevelMapper();

    @Test
    @DisplayName("maps anonymized eGov water-level rows into the core domain")
    void shouldMapEgovHydWaterLevelRowToDomain() {
        EgovHydWaterLevelRecord record = new EgovHydWaterLevelRecord(
                "WL-20260506-001",
                "RIVER-ST-01",
                "2026-05-06 13:00:00",
                412.0,
                "CM"
        );

        WaterObservation observation = mapper.toDomain(record);

        assertThat(observation.getId().getValue()).isEqualTo("WL-20260506-001");
        assertThat(observation.getStationId().getCode()).isEqualTo("RIVER-ST-01");
        assertThat(observation.getLevel().getMeters()).isEqualTo(4.12);
    }

    @Test
    @DisplayName("rejects unsupported legacy unit codes at the adapter boundary")
    void shouldRejectUnsupportedUnit() {
        EgovHydWaterLevelRecord record = new EgovHydWaterLevelRecord(
                "WL-20260506-002",
                "RIVER-ST-01",
                "2026-05-06 13:05:00",
                4.1,
                "FEET"
        );

        assertThatThrownBy(() -> mapper.toDomain(record))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Unsupported water level unit");
    }
}
