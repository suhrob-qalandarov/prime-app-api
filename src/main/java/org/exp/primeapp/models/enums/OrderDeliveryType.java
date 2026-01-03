package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderDeliveryType {
    VIA_TAXI("POCHTA"),
    VIA_BTS("TAXI");

    private final String label;

    OrderDeliveryType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
