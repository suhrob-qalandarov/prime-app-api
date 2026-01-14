package org.exp.primeapp.models.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.Map;

/**
 * Enhanced response for inventory transactions with statistics
 */
@Builder
public record InventoryTransactionPageResponse(
        // Page data
        Page<InventoryTransactionResponse> transactions,

        // Statistics
        Long totalTransactionsCount,
        Long inTransactionsCount,
        Long outTransactionsCount,
        Long returningCount,
        Map<String, Long> productTagCounts // Key: tag name, Value: count
) {
}
