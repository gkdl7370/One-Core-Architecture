package com.onecore.adapter.out.external;

/**
 * Example payload from legacy system A.
 * The external system reports water level in centimeters.
 */
public class SystemAPayload {

    private final String id;
    private final String stationCode;
    private final double waterLevelCm;

    public SystemAPayload(String id, String stationCode, double waterLevelCm) {
        this.id = id;
        this.stationCode = stationCode;
        this.waterLevelCm = waterLevelCm;
    }

    public String getId() { return id; }
    public String getStationCode() { return stationCode; }
    public double getWaterLevelCm() { return waterLevelCm; }
}
