package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    PENDING_PAYMENT("TO'LOV KUTILMOQDA"),
    PAID("TO'LANGAN"),
    CONFIRMED("TASDIQLANGAN"),
    DELIVERING("YETKAZIB BERILAYAPTI"),
    SHIPPED("XARIDORGA BERILGAN"), /// bts(post)
    DELIVERED("YETKAZIB BERILDI"), /// taxi(yandex)
    CANCELLED("BEKOR QILINDI"),
    REFUNDED("PUL QAYTARILDI");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
