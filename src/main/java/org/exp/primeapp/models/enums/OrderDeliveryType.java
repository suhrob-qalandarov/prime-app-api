package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderDeliveryType {
    VIA_TAXI("TAXI"),
    VIA_BTS("POCHTA");

    private final String label;

    OrderDeliveryType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static OrderDeliveryType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (OrderDeliveryType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.getLabel().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unknown enum type " + value + ", Allowed values are " + java.util.Arrays.toString(values()));
    }
}
