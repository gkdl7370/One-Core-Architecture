package com.onecore.core.domain.aggregate;

import com.onecore.core.domain.exception.InvalidObservationException;

public class WaterLevel {

    private final double meters;

    public WaterLevel(double meters) {
        if (meters < 0) {
            throw new InvalidObservationException(
                    "수위는 음수일 수 없습니다: " + meters
            );
        }
        this.meters = meters;
    }

    public static WaterLevel fromCentimeters(double cm) {
        return new WaterLevel(cm / 100.0);
    }

    public double getMeters() {
        return meters;
    }

    @Override
    public String toString() {
        return meters + "m";
    }
}