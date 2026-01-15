package org.exp.primeapp.models.dto.response;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.exp.primeapp.models.enums.TransactionReason;
import org.exp.primeapp.models.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Inventory Transaction.
 * Product snapshot ma'lumotlarini qaytaradi.
 */
@Builder
public record InventoryTransactionResponse(
        Long id,

        // Product snapshot ma'lumotlari
        String productName,
        String productDescription,
        String productMainImageUrl,
        String productTag,
        String productStatus,
        String productColor,
        String productColorHex,
        String productCategoryName,
        String productSize,

        // Transaction ma'lumotlari
        Integer stockQuantity,
        BigDecimal unitPrice,
        Integer discountPercent,
        BigDecimal totalPrice,

        TransactionType type,
        TransactionReason reason,

        // Reference IDs
        Long productId,
        Long customerId,
        Long clientId,
        Long orderId,
        Long performedById,
        Long returnPerformedById,

        // Additional info
        String customerName,
        String clientName,
        String performedByName,
        String returnPerformedByName,

        String note,

       @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm") LocalDateTime createdAt,
       @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm") LocalDateTime returnedAt
) {}
