package com.onecore.core.domain;

import com.onecore.adapter.out.external.LegacyObservationMapper;
import com.onecore.adapter.out.external.SystemAPayload;
import com.onecore.core.domain.aggregate.WaterObservation;
import com.onecore.core.domain.exception.InvalidObservationException;
import com.onecore.core.domain.policy.RuralAreaWarningPolicy;
import com.onecore.core.domain.policy.UrbanAreaWarningPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterObservationTest {

    private final LegacyObservationMapper mapper = new LegacyObservationMapper();
    private final UrbanAreaWarningPolicy urbanPolicy = new UrbanAreaWarningPolicy();
    private final RuralAreaWarningPolicy ruralPolicy = new RuralAreaWarningPolicy();

    @Test
    @DisplayName("normalizes legacy centimeter payloads into meters")
    void shouldNormalizeCmToMeters() {
        SystemAPayload payload = new SystemAPayload("obs-001", "STN-A", 350.0);

        WaterObservation observation = mapper.toDomain(payload);

        assertThat(observation.getLevel().getMeters()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("marks urban observations as dangerous at 3.5m or higher")
    void shouldBeDangerousInUrbanAreaWhenLevelExceedsThreshold() {
        SystemAPayload payload = new SystemAPayload("obs-002", "STN-B", 360.0);
        WaterObservation observation = mapper.toDomain(payload);

        assertThat(observation.isDangerous(urbanPolicy)).isTrue();
    }

    @Test
    @DisplayName("keeps rural observations safe below the rural threshold")
    void shouldBeSafeInRuralAreaWhenLevelIsBelowThreshold() {
        SystemAPayload payload = new SystemAPayload("obs-003", "STN-C", 360.0);
        WaterObservation observation = mapper.toDomain(payload);

        assertThat(observation.isDangerous(ruralPolicy)).isFalse();
    }

    @Test
    @DisplayName("rejects negative water-level payloads")
    void shouldThrowExceptionWhenWaterLevelIsNegative() {
        SystemAPayload invalidPayload = new SystemAPayload("obs-004", "STN-D", -10.0);

        assertThatThrownBy(() -> mapper.toDomain(invalidPayload))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("negative");
    }
}
