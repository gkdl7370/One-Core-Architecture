package com.onecore.core.domain.aggregate;

import com.onecore.core.domain.policy.WaterWarningPolicy;

/**
 * 수자원 관측 데이터 Aggregate Root.
 * 외부 시스템의 변경이 이 도메인에 영향을 주지 않도록
 * Anti-Corruption Layer(Adapter)를 통해서만 생성
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

    /**
     * 위험 수위 판단 책임을 Policy로 위임
     * 지역마다 다른 경보 기준을 OCP 원칙에 따라 확장
     */
    public boolean isDangerous(WaterWarningPolicy policy) {
        return policy.isDangerous(this.level);
    }

    public ObservationId getId() { return id; }
    public StationId getStationId() { return stationId; }
    public WaterLevel getLevel() { return level; }
}