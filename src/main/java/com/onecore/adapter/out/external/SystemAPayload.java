package com.onecore.adapter.out.external;

/**
 * 레거시 시스템 A의 원본 데이터 구조
 * cm 단위를 사용하며 JSON 스키마가 코어 도메인과 다름
 */
public class SystemAPayload {

    private String id;
    private String stationCode;
    private double waterLevelCm;  // 레거시는 cm 단위 사용

    public SystemAPayload(String id, String stationCode, double waterLevelCm) {
        this.id = id;
        this.stationCode = stationCode;
        this.waterLevelCm = waterLevelCm;
    }

    public String getId() { return id; }
    public String getStationCode() { return stationCode; }
    public double getWaterLevelCm() { return waterLevelCm; }
}