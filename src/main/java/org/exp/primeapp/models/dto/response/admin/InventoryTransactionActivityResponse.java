package org.exp.primeapp.models.dto.response.admin;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record InventoryTransactionActivityResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        String categoryName,
        Integer stockQuantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        BigDecimal sellingPrice,
        Boolean isCalculated,
        String adminUserName,
        String createdAt, // Format: dd.MM.yyyy HH:mm
        String activityType // "INCOME" or "OUTCOME"
) {
}
