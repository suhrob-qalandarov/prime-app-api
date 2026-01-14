package org.exp.primeapp.models.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.exp.primeapp.models.enums.TransactionReason;
import org.exp.primeapp.models.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unified Request DTO for creating Inventory Transactions (both IN and OUT).
 * Replaces IncomeRequest and OutcomeRequest.
 * Product ma'lumotlari productId orqali olinadi va snapshot sifatida saqlanadi.
 */
@Builder
public record InventoryTransactionRequest(
                @NotNull(message = "Product ID kiritilishi kerak") @Positive(message = "Product ID musbat bo'lishi kerak") Long productId,

                @NotNull(message = "Transaction type kiritilishi kerak") TransactionType type,

                @NotNull(message = "Size items kiritilishi kerak") @NotEmpty(message = "Kamida bitta size item bo'lishi kerak") @Valid List<InventoryTransactionSizeItem> sizeItems,

                // Fields for IN (Income) transactions
                BigDecimal oneStockPrice,
                BigDecimal totalIncomeStockPrice,
                Boolean canSetPriceToProduct,

                // Optional fields for all transactions
                TransactionReason reason,
                Long customerId,
                Long clientId,
                Long orderId,
                Long performedById,
                String note) {
        public InventoryTransactionRequest {
                // Set defaults
                if (canSetPriceToProduct == null) {
                        canSetPriceToProduct = false;
                }
                if (reason == null) {
                        // Default reason based on type
                        reason = (type == TransactionType.IN) ? TransactionReason.PURCHASE : TransactionReason.SALE;
                }
        }
}
