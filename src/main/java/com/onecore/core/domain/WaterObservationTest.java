package com.onecore.core.domain;

import com.onecore.adapter.out.external.LegacyObservationMapper;
import com.onecore.adapter.out.external.SystemAPayload;
import com.onecore.core.domain.aggregate.WaterObservation;
import com.onecore.core.domain.exception.InvalidObservationException;
import com.onecore.core.domain.policy.RuralAreaWarningPolicy;
import com.onecore.core.domain.policy.UrbanAreaWarningPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WaterObservationTest {

    private final LegacyObservationMapper mapper = new LegacyObservationMapper();
    private final UrbanAreaWarningPolicy urbanPolicy = new UrbanAreaWarningPolicy();
    private final RuralAreaWarningPolicy ruralPolicy = new RuralAreaWarningPolicy();

    @Test
    @DisplayName("레거시 시스템 A의 cm 단위 수위가 m로 정규화된다")
    void shouldNormalizeCmToMeters() {
        SystemAPayload payload = new SystemAPayload("obs-001", "STN-A", 350.0);

        WaterObservation observation = mapper.toDomain(payload);

        assertThat(observation.getLevel().getMeters()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("도심 지역 정책: 3.5m 이상이면 위험으로 판단한다")
    void shouldBeDangerousInUrbanAreaWhenLevelExceedsThreshold() {
        SystemAPayload payload = new SystemAPayload("obs-002", "STN-B", 360.0);
        WaterObservation observation = mapper.toDomain(payload);

        assertThat(observation.isDangerous(urbanPolicy)).isTrue();
    }

    @Test
    @DisplayName("농어촌 지역 정책: 3.5m는 안전으로 판단한다")
    void shouldBeSafeInRuralAreaWhenLevelIsBelowThreshold() {
        SystemAPayload payload = new SystemAPayload("obs-003", "STN-C", 360.0);
        WaterObservation observation = mapper.toDomain(payload);

        assertThat(observation.isDangerous(ruralPolicy)).isFalse();
    }

    @Test
    @DisplayName("음수 수위 데이터는 예외를 발생시킨다")
    void shouldThrowExceptionWhenWaterLevelIsNegative() {
        SystemAPayload invalidPayload = new SystemAPayload("obs-004", "STN-D", -10.0);

        assertThatThrownBy(() -> mapper.toDomain(invalidPayload))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("음수");
    }
}