package org.exp.primeapp.models.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.exp.primeapp.models.enums.Size;

import java.math.BigDecimal;

/**
 * Size item for inventory transactions (both IN and OUT)
 * For IN (income): includes sellingPrice
 * For OUT (outcome): sellingPrice is not used
 */
@Builder
public record InventoryTransactionSizeItem(
        @NotNull(message = "Size kiritilishi kerak") Size size,

        @NotNull(message = "Quantity kiritilishi kerak") @Positive(message = "Quantity musbat bo'lishi kerak") Integer quantity,

        // Optional: faqat IN transaction uchun ishlatiladi
        BigDecimal sellingPrice) {
}
