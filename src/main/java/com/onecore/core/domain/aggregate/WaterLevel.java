package com.onecore.core.domain.aggregate;

import com.onecore.core.domain.exception.InvalidObservationException;

public class WaterLevel {

    private final double meters;

    public WaterLevel(double meters) {
        if (meters < 0) {
            throw new InvalidObservationException(
                    "Water level must not be negative: " + meters
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
