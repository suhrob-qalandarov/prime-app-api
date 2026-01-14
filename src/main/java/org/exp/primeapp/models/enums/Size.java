package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Size {
    SMALL("KICHIK"), MEDIUM("O'RTACHA"), BIG("KATTA"),
    S("S"), M("M"), L("L"), XL("XL"), XXL("XXL"), XXXL("XXXL"),
    SIZE_39("39"), SIZE_40("40"), SIZE_41("41"), SIZE_42("42"), SIZE_43("43"), SIZE_44("44"), SIZE_45("45"),
    SIZE_28("28"), SIZE_29("29"), SIZE_30("30"), SIZE_31("31"), SIZE_32("32"), SIZE_33("33"), SIZE_34("34"), SIZE_35("35"), SIZE_36("36");

    private final String label;

    Size(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @JsonValue
    public String getValue() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Size fromLabel(String label) {
        for (Size size : Size.values()) {
            if (size.label.equals(label)) {
                return size;
            }
        }
        throw new IllegalArgumentException("Invalid size label: " + label);
    }
}
