package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountStatus {
    ACTIVE("FAOL"),
    INACTIVE("NOFAOL"),
    DELETED("O'CHIRILGAN"),
    BLOCKED("BLOKLANGAN");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
