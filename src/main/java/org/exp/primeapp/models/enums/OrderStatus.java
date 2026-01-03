package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    PENDING_PAYMENT("TO'LOV KUTILMOQDA"),
    PENDING("TASDIQLASH KUTILMOQDA"),
    PAID("TO'LANGAN"),
    CONFIRMED("YETKAZIB BERILAYAPTI"),
    SHIPPED("XARIDORGA BERILGAN"),
    DELIVERED("YETKAZIB BERILDI"),
    CANCELLED("BEKOR QILINDI");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
