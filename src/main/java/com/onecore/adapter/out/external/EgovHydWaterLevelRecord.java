package com.onecore.adapter.out.external;

/**
 * Anonymized row shape inspired by a legacy eGov hydrology water-level module.
 */
public class EgovHydWaterLevelRecord {

    private final String rowId;
    private final String stationCode;
    private final String observedAtText;
    private final double waterLevel;
    private final String unitCode;

    public EgovHydWaterLevelRecord(
            String rowId,
            String stationCode,
            String observedAtText,
            double waterLevel,
            String unitCode
    ) {
        this.rowId = rowId;
        this.stationCode = stationCode;
        this.observedAtText = observedAtText;
        this.waterLevel = waterLevel;
        this.unitCode = unitCode;
    }

    public String getRowId() {
        return rowId;
    }

    public String getStationCode() {
        return stationCode;
    }

    public String getObservedAtText() {
        return observedAtText;
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    public String getUnitCode() {
        return unitCode;
    }
}
