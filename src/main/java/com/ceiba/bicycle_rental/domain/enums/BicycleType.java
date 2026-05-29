package com.ceiba.bicycle_rental.domain.enums;

public enum BicycleType {
    URBANA(3500),
    MONTANA(5000),
    ELECTRICA(7500);

    private final int hourlyRate;

    BicycleType(int hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public int getHourlyRate() {
        return hourlyRate;
    }
}
