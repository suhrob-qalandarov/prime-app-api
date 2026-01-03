package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CancelReason {
    OUT_OF_STOCK("MIQDOR YETARLI EMAS"),
    USER_CANCELLED("FOYDALANUVCHI BEKOR QILDI"),
    PAYMENT_NOT_CONFIRMED("TO'LOV QABULLANMAGAN"),
    ADMIN_REJECTED("ADMIN BEKOR QILDI");

    private final String label;

    CancelReason(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
