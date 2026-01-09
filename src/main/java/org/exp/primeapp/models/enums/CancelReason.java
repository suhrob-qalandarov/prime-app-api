package org.exp.primeapp.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static CancelReason fromValue(String value) {
        // First try to match by name (e.g., "OUT_OF_STOCK")
        for (CancelReason reason : CancelReason.values()) {
            if (reason.name().equals(value)) {
                return reason;
            }
        }
        // Then try to match by label (e.g., "MIQDOR YETARLI EMAS")
        for (CancelReason reason : CancelReason.values()) {
            if (reason.label.equals(value)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown CancelReason: " + value);
    }
}
