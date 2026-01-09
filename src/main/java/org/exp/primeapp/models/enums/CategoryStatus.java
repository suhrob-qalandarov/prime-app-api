package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CategoryStatus {
    CREATED("Yaratilgan"),
    ACTIVE("Faol"),
    INACTIVE("Nofaol");

    private final String label;

    CategoryStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
